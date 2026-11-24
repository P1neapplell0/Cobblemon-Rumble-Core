package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.client.BattleCamera;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleNameplates;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleOrbit;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeReleaseAim;
import net.neoforged.neoforge.network.PacketDistributor;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.johnn.realtimecobblemon.client.BattleCamera", remap = false)
abstract class StoryActorLockOn {
    @Shadow private static int lockId;
    @Shadow static void lock(LivingEntity entity) { throw new AssertionError(); }
    @Shadow static List<LivingEntity> candidates(LocalPlayer player) { throw new AssertionError(); }
    @Shadow static boolean canSee(LocalPlayer player, Entity target) { throw new AssertionError(); }

    @Unique private static java.util.UUID rumble$lockedTrainer;
    @Unique private static int rumble$lockedBody = -1;
    @Unique private static int rumble$syncedLock = -1;
    @Unique private static int rumble$syncedBody = -1;

    @Inject(method = "onTick", at = @At("HEAD"))
    private static void rumble$clearLockOutsideArena(ClientTickEvent.Post event, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || !ClientSmash.active() || !rumble$instance(player)) rumble$clearTrainerLock();
    }

    @WrapOperation(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private static boolean rumble$cancelPendingLockOnManualInput(KeyMapping mapping, Operation<Boolean> original) {
        boolean clicked = original.call(mapping);
        if (clicked && mapping == BattleCamera.LOCK) rumble$clearTrainerLock();
        return clicked;
    }

    @Unique private static void rumble$clearTrainerLock() {
        rumble$lockedTrainer = null;
        rumble$lockedBody = -1;
    }

    @Inject(method = "onTurn", at = @At("HEAD"), cancellable = true)
    private static void rumble$normalMouseSensitivity(CalculatePlayerTurnEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "angleTo", at = @At("HEAD"), cancellable = true)
    private static void rumble$scoreFromOrbit(LocalPlayer player, Entity target, CallbackInfoReturnable<Double> cir) {
        if (ClientSmash.active()) cir.setReturnValue(BattleOrbit.angleTo(player, target));
    }

    // After manual key handling, before RTC validates the lock and turns the combat aim toward it.
    @Inject(method = "onTick", at = @At(value = "INVOKE", ordinal = 2,
            target = "Lcom/johnn/realtimecobblemon/client/BattleCamera;target()Lnet/minecraft/world/entity/LivingEntity;"))
    private static void rumble$retarget(ClientTickEvent.Post event, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        var enemies = BattleNameplates.opponents(player);
        if (enemies != null && rumble$instance(player)) {
            if (lockId != rumble$lockedBody && lockId >= 0) {
                rumble$lockedTrainer = enemies.players().stream().filter(p -> p.body() == lockId).map(p -> p.player()).findFirst().orElse(null);
                rumble$lockedBody = lockId;
            }
            if (rumble$lockedTrainer != null) {
                var nextBody = enemies.players().stream().filter(p -> p.player().equals(rumble$lockedTrainer)).findFirst().orElse(null);
                if (nextBody == null) rumble$clearTrainerLock();
                else {
                    var previous = BattleCamera.target();
                    boolean switching = nextBody.body() != rumble$lockedBody
                            || previous instanceof PokemonEntity mon && mon.getPokemon().isFainted();
                    if (switching) {
                        if (nextBody.body() != rumble$lockedBody
                                && player.level().getEntity(nextBody.body()) instanceof LivingEntity body && rumble$usable(player, body)) {
                            lock(body);
                            rumble$lockedBody = body.getId();
                        }
                        return;
                    }
                    // The client can see a recall before the server's opponent update arrives.
                    if (previous == null) return;
                }
            }
        }
        if (lockId < 0) return;
        var target = BattleCamera.target();
        BattleOrbit.updateLock(player, true);
        if (mc.screen != null) return;
        double retention = rumble$instance(player) ? 72 : 40;
        boolean lost = target == null || !rumble$usable(player, target) || target.distanceToSqr(player) > retention * retention;
        if (!lost && !BattleOrbit.wantsSwitch()) return;

        LivingEntity next = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates(player)) {
            if (!(candidate instanceof PokemonEntity) || candidate.getId() == lockId) continue;
            double distance = candidate.distanceTo(player);
            if (distance > 28) continue;
            double angle = BattleOrbit.angleTo(player, candidate, !lost);
            // While deliberately turning away, only switch to an opponent in the new view.
            if ((!lost && angle > 34) || !canSee(player, candidate)) continue;
            double score = distance + .35 * angle;
            if (score < best || score == best && next != null && candidate.getId() < next.getId()) {
                best = score;
                next = candidate;
            }
        }
        if (lost || next != null) {
            lock(next);
            BattleOrbit.switched();
        }
    }

    @Unique
    private static boolean rumble$instance(LocalPlayer player) {
        return InstanceManager.isInstanceDimension(player.level().dimension());
    }

    @ModifyConstant(method = "keep", constant = @Constant(doubleValue = 40.0))
    private static double rumble$retainArenaRange(double original, LocalPlayer player, LivingEntity target) {
        return rumble$instance(player) ? 72.0 : original;
    }

    @Inject(method = "onTick", at = @At("RETURN"))
    private static void rumble$releaseOrbit(ClientTickEvent.Post event, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            BattleOrbit.reset();
            rumble$syncedLock = rumble$syncedBody = -1;
        }
        else {
            var target = ClientSmash.active() ? BattleCamera.target() : null;
            BattleOrbit.updateLock(player, target != null);
            int aim = target == null ? -1 : target.getId();
            Integer controlled = ClientSmash.controlledEntity();
            int body = controlled == null ? -1 : controlled;
            if (ClientSmash.active() && (aim != rumble$syncedLock || body != rumble$syncedBody)) {
                PacketDistributor.sendToServer(new RealtimeReleaseAim(aim));
            }
            rumble$syncedLock = aim;
            rumble$syncedBody = ClientSmash.active() ? body : -1;
            BattleOrbit.tickIntent();
        }
    }

    @Inject(method = "candidates", at = @At("RETURN"), cancellable = true)
    private static void rumble$filter(LocalPlayer player, CallbackInfoReturnable<List<LivingEntity>> cir) {
        var filtered = new ArrayList<>(cir.getReturnValue());
        filtered.removeIf(target -> !rumble$usable(player, target));
        cir.setReturnValue(filtered);
    }

    @Inject(method = "keep", at = @At("HEAD"), cancellable = true)
    private static void rumble$release(LocalPlayer player, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target != null && !rumble$usable(player, target)) cir.setReturnValue(false);
    }

    @Unique private static boolean rumble$usable(LocalPlayer player, LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && !target.isInvisibleTo(player) && !target.isSpectator()
                && !BattleNameplates.allied(target)
                && !FixedStoryActors.protectedFromCombat(target)
                && !(target instanceof PokemonEntity mon && (((ArenaKnockoutVisual) mon).rumble$isKnockedOut()
                        || mon.getPokemon().isFainted() || WildCapture.stunned(mon)));
    }
}
