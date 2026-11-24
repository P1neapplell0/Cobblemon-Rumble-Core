package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.client.BattleCamera;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.johnn.realtimecobblemon.network.SmashPayloads;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeHealthBar;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.johnn.realtimecobblemon.client.SmashHud", remap = false)
abstract class ArenaKnockoutHud {
    @Unique private static final RealtimeHealthBar rumble$ownHp = new RealtimeHealthBar();
    @Unique private static final RealtimeHealthBar rumble$enemyHp = new RealtimeHealthBar();

    @Shadow private static void tile(GuiGraphics graphics, float x, float y, float partial, boolean reversed,
                                     Pokemon pokemon, int level, int maxHp, float health, boolean flat, String status) {
        throw new AssertionError();
    }

    @WrapOperation(method = "ownTile", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/client/SmashHud;tile(Lnet/minecraft/client/gui/GuiGraphics;FFFZLcom/cobblemon/mod/common/pokemon/Pokemon;IIFZLjava/lang/String;)V"))
    private static void rumble$animateOwn(GuiGraphics graphics, float x, float y, float partial, boolean reversed,
                                          Pokemon pokemon, int level, int maxHp, float hp, boolean flat, String status,
                                          Operation<Void> original) {
        if (rumble$body(Minecraft.getInstance().player) instanceof PokemonEntity body) {
            hp = ((ArenaKnockoutVisual) body).rumble$isKnockedOut() ? 0 : hp;
            RealtimeHealthBar.painting = rumble$ownHp.sample(body, hp, maxHp);
        }
        try {
            original.call(graphics, x, y, partial, reversed, pokemon, level, maxHp, hp, flat, status);
        } finally {
            RealtimeHealthBar.painting = null;
        }
    }

    @Inject(method = "targetTile", at = @At("HEAD"), cancellable = true)
    private static void rumble$lockedTarget(GuiGraphics graphics, SmashPayloads.Hud hud, float partial, CallbackInfo ci) {
        ci.cancel();
        var world = Minecraft.getInstance().level;
        if (world == null) return;
        Entity body = rumble$body(BattleCamera.target());
        Entity recent = hud.targetId() >= 0 ? rumble$body(world.getEntity(hud.targetId())) : null;
        if (!(body instanceof PokemonEntity)) body = recent;
        if (!(body instanceof PokemonEntity pokemon) || pokemon.isRemoved()) {
            rumble$enemyHp.clear();
            return;
        }
        boolean ko = ((ArenaKnockoutVisual) pokemon).rumble$isKnockedOut();
        float hp = ko ? 0 : pokemon.getHealth();
        int level = body == recent && hud.targetLevel() > 0 ? hud.targetLevel() : pokemon.getPokemon().getLevel();
        var status = pokemon.getPokemon().getStatus();
        RealtimeHealthBar.painting = rumble$enemyHp.sample(pokemon, hp, pokemon.getMaxHealth());
        try {
            tile(graphics, graphics.guiWidth() - 152f, 10, partial, true, pokemon.getPokemon(), level, 1,
                    hp / Math.max(1, pokemon.getMaxHealth()), false,
                    body == recent ? hud.targetStatus() : status == null ? "" : status.getStatus().getShowdownName());
        } finally {
            RealtimeHealthBar.painting = null;
        }
    }

    @Unique private static Entity rumble$body(Entity entity) {
        if (entity instanceof Player player) {
            Integer id = RealtimeHudControl.rumble$controlledId(player.getId());
            if (id != null) return player.level().getEntity(id);
        }
        return entity;
    }
}

@Mixin(value = ClientSmash.class, remap = false)
interface RealtimeHudControl {
    @Invoker("controlledId")
    static Integer rumble$controlledId(int playerId) { throw new AssertionError(); }
}
