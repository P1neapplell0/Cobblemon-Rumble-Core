package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.SmashConfig;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.Defense;
import com.johnn.realtimecobblemon.moves.Kits;
import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import com.p1nero.misc.custom_instances.api.ExitReason;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.misc.custom_instances.api.InstanceEvent;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;

/** Arena runtime is owned by Custom Instances; ArcQuest owns saved story progress. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class PrologueTraining {
    public static final String TEMPLATE = "rumble_prologue";
    private static final String TRAINING_PIKACHU = "rumble_training_pikachu";
    private static final Vec3 TARGET = new Vec3(-11.5, 50, 0.5);
    private static final Vec3 WEST_MARK = new Vec3(-18.5, 50, 0.5);
    private static final Map<ServerPlayer, Integer> LAUNCH = new HashMap<>();

    private PrologueTraining() {}

    public static void initialize() { InstanceApi.registerSessionFactory(TEMPLATE, Drill::new); }

    private static final class Drill extends InstanceSession {
        Drill(UUID id, InstanceDefinition definition, ResourceKey<Level> dimension, UUID owner, String password) {
            super(id, definition, dimension, owner, password);
        }

        Cue cue = Cue.LOAD;
        PokemonEntity pikachu;
        Pokemon partner;
        Vec3 previousPosition;
        double walked;
        boolean jumped, previousOnGround;
        int readyAt;
        int guardRetryAt;
        int guardLaunchAt;
        TrainingBolt bolt;
        boolean wonPending;
        int knockoutUntil;
        int pauseUntil;
        int nextAt;
        int loadDeadline;
        boolean ready;
        boolean resumeSpar;
        boolean authorizedWin;
        int moves;

        @Override public boolean showDefaultResult() { return false; }
        @Override public boolean showDefaultBattleReport() { return false; }
        @Override public boolean showStatusHud() { return false; }
        @Override public int resultDelayTicks() { return 78; }

        @Override protected void onTick(InstanceManager manager) {
            ServerPlayer player = manager.getServer().getPlayerList().getPlayer(getOwner());
            if (player != null && player.serverLevel() == getLevel()) tickDrill(this, player);
        }
    }

    static boolean launching(ServerPlayer player) { return LAUNCH.containsKey(player) || drillMember(player) != null; }

    static void launch(ServerPlayer player) {
        String phase = PrologueStory.phase(player);
        if (launching(player) || !canLaunch(player, phase)) return;
        LAUNCH.put(player, player.server.getTickCount() + 14);
        ProloguePackets.show(player, Cue.TRANSFER, -1, false, 0);
    }

    private static boolean canLaunch(ServerPlayer player, String phase) {
        return (phase.equals(PrologueStory.BRIEFING) || phase.equals(PrologueStory.DRILLS) || phase.equals(PrologueStory.SPAR))
                && player.isAlive() && player.serverLevel().dimension().equals(PrologueWorld.GYMS)
                && PrologueStory.nearby(player) != null && !Smash.active(player);
    }

    @SubscribeEvent
    public static void tickLaunches(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer launchAt = LAUNCH.get(player);
        if (launchAt == null || player.server.getTickCount() < launchAt) return;
        // Keep the token through the synchronous BeforeJoin callback.
        try {
            if (!canLaunch(player, PrologueStory.phase(player))) throw new IllegalStateException("Left the professor before transfer");
            InstanceManager manager = InstanceManager.get(player.server);
            if (manager == null || manager.forPlayer(player) != null) throw new IllegalStateException("Already in an instance");
            UUID id = manager.create(player, TEMPLATE, "");
            manager.setReady(player, id, true);
            manager.start(player, TEMPLATE);
        } catch (RuntimeException failure) {
            Drill drill = drillMember(player);
            if (drill != null) InstanceManager.get(player.server).end(drill, ExitReason.ERROR, true);
            ProloguePackets.show(player, Cue.NONE, -1, false, 0);
            player.displayClientMessage(Component.translatable("rumble.prologue.transfer_failed"), false);
            LogUtils.getLogger().warn("Could not start the prologue instance for {}", player.getScoreboardName(), failure);
        } finally {
            LAUNCH.remove(player, launchAt);
        }
    }

    @SubscribeEvent
    public static void beforeJoin(InstanceEvent.BeforeJoin event) {
        if (event.getSession() instanceof Drill && (!event.getSession().getOwner().equals(event.getPlayer().getUUID())
                || !LAUNCH.containsKey(event.getPlayer()))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void started(InstanceEvent.Started event) {
        if (!(event.getSession() instanceof Drill drill)) return;
        ServerLevel level = drill.getLevel();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(drill.getOwner());
        if (player == null) return;
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), -90, 10);
        drill.loadDeadline = player.server.getTickCount() + 600;
        drill.resumeSpar = PrologueStory.SPAR.equals(PrologueStory.phase(player));
        if (!spawnPikachu(drill)) throw new IllegalStateException("Could not create the training Pokémon");
        PrologueStory.finish(player, PrologueStory.BRIEFING);
        sync(drill, player);
    }

    private static boolean outsideTrainingBounds(PokemonEntity pikachu) {
        if (pikachu.getY() < TARGET.y - 10 || pikachu.getY() > TARGET.y + 12) return true;
        double dx = pikachu.getX() - TARGET.x;
        double dz = pikachu.getZ() - TARGET.z;
        return dx * dx + dz * dz > 18 * 18;
    }

    private static boolean spawnPikachu(Drill drill) {
        ServerLevel level = drill.getLevel();
        if (drill.pikachu != null && !drill.pikachu.isRemoved()) drill.pikachu.discard();
        var mon = PokemonProperties.Companion.parse("pikachu level=15 hp_iv=31 uncatchable=true moves=tackle,thundershock")
                .createEntity(level);
        mon.addTag(TRAINING_PIKACHU);
        mon.setPersistenceRequired();
        mon.setNoAi(true);
        mon.moveTo(TARGET.x, TARGET.y, TARGET.z, 90, 0);
        drill.pikachu = mon;
        return level.addFreshEntity(mon);
    }

    static void clientReady(ServerPlayer player, int target) {
        Drill drill = active(player);
        if (drill != null && drill.cue == Cue.LOAD && drill.pikachu.getId() == target) step(drill, player, Cue.CHALLENGE);
    }

    private static void tickDrill(Drill drill, ServerPlayer player) {
        int now = player.server.getTickCount();
        if (drill.cue == Cue.LOAD) {
            if (now >= drill.loadDeadline) InstanceManager.get(player.server).end(drill, ExitReason.ERROR, true);
            return;
        }
        if (drill.cue == Cue.CLEAR) return;
        // RTC's KO impulse can carry the training decoy outside the small tutorial
        // arena.  Keep the lesson recoverable without adding a physical barrier that
        // would interfere with the player's movement and camera.
        if (!drill.wonPending && drill.pikachu != null && outsideTrainingBounds(drill.pikachu)) {
            drill.pikachu.teleportTo(TARGET.x, TARGET.y, TARGET.z);
            drill.pikachu.setDeltaMovement(Vec3.ZERO);
            drill.pikachu.hurtMarked = true;
            drill.pikachu.setNoGravity(true);
            drill.pikachu.noPhysics = false;
        }
        // A finishing hit can come from inside the RTC decoy/projectile iterator.
        // End the fight from the instance tick, not in the middle of RTC's damage callback.
        if (drill.wonPending) {
            if (drill.knockoutUntil == 0) {
                int duration = FrontierKnockout.CUE_TICKS;
                var actors = new ArrayList<Integer>();
                actors.add(player.getId());
                actors.add(drill.pikachu.getId());
                var battle = Smash.session(player);
                if (battle != null) actors.add(battle.pokemon.getId());
                PacketDistributor.sendToPlayer(player, new FrontierKnockout.Cue(
                        drill.getDimension().location(), duration, drill.pikachu.getId(), battle == null ? -1 : battle.pokemon.getId(), actors));
                drill.knockoutUntil = now + duration;
            }
            if (now < drill.knockoutUntil) return;
            drill.authorizedWin = true;
            try { if (InstanceApi.win(drill.getLevel())) drill.wonPending = false; }
            finally { drill.authorizedWin = false; }
            return;
        }
        if (!drill.ready && now >= drill.readyAt && drill.nextAt == 0) {
            drill.ready = true;
            sync(drill, player);
        }
        Smash.Session battle = Smash.session(player);
        // A confirmed exit deliberately has no RTC session until the re-entry input succeeds.
        boolean waitingOutside = drill.cue == Cue.MODE_EXIT && drill.nextAt != 0
                || drill.cue == Cue.MODE_REENTER && drill.nextAt == 0;
        PokemonEntity partnerEntity = !waitingOutside || drill.partner == null ? null : drill.partner.getEntity();
        boolean lostPartner = waitingOutside && (partnerEntity == null || !partnerEntity.isAlive() || partnerEntity.level() != drill.getLevel());
        if (drill.pikachu == null || !drill.pikachu.isAlive() || lostPartner
                || battle == null && drill.cue != Cue.CHALLENGE && !waitingOutside) {
            if (battle != null) Smash.end(player, null, true);
            // A fainted partner or interrupted mode restarts practice in the same private room.
            if (drill.partner != null) drill.partner.heal();
            drill.partner = null;
            drill.resumeSpar = drill.cue == Cue.SPAR;
            drill.moves = 0;
            if (!spawnPikachu(drill)) {
                InstanceManager.get(player.server).end(drill, ExitReason.ERROR, true);
                return;
            }
            Vec3 entry = Vec3.atBottomCenterOf(drill.getSpawn());
            player.connection.teleport(entry.x, entry.y, entry.z, -90, 10);
            step(drill, player, Cue.CHALLENGE);
            return;
        }
        if (drill.nextAt != 0) {
            if (now >= drill.nextAt) advance(drill, player);
            return;
        }
        if (!drill.ready || battle == null) return;
        if (drill.cue == Cue.MOVEMENT) {
            Vec3 position = player.position();
            Vec3 delta = position.subtract(drill.previousPosition);
            double distance = delta.horizontalDistance();
            if (distance > .005 && distance <= 1.25) drill.walked += distance;
            if (drill.previousOnGround && !player.onGround() && delta.y > .1) drill.jumped = true;
            drill.previousOnGround = player.onGround();
            drill.previousPosition = position;
            if (drill.walked >= 3.5 && drill.jumped && player.onGround()) success(drill, player, 32);
        } else if (drill.cue == Cue.DODGE || drill.cue.defensive()) {
            tickGuard(drill, battle, now);
        }
    }

    private static void tickGuard(Drill drill, Smash.Session battle, int now) {
        ServerLevel level = drill.getLevel();
        if (drill.bolt != null) {
            var result = drill.bolt.tick(level, battle);
            if (result == TrainingBolt.Result.FLYING) return;
            drill.bolt = null;
            boolean passed = switch (drill.cue) {
                case DODGE -> result == TrainingBolt.Result.DODGED;
                case GUARD -> result == TrainingBolt.Result.GUARDED || result == TrainingBolt.Result.PARRIED;
                case PARRY -> result == TrainingBolt.Result.PARRIED;
                default -> false;
            };
            if (passed) {
                success(drill, battle.player, 44);
                return;
            }
            if (drill.cue == Cue.PARRY && result == TrainingBolt.Result.GUARDED) {
                battle.player.displayClientMessage(Component.translatable("rumble.prologue.lesson.parry.retry"), true);
            }
            if (drill.cue == Cue.DODGE) {
                battle.player.displayClientMessage(Component.translatable("rumble.prologue.lesson.dodge.retry"), true);
            }
            drill.guardRetryAt = now + 36;
            return;
        }

        PokemonEntity mon = drill.pikachu;
        if (drill.guardLaunchAt != 0) {
            if (now >= drill.guardLaunchAt) {
                drill.guardLaunchAt = 0;
                drill.bolt = new TrainingBolt(level, mon, battle, drill.cue == Cue.DODGE);
            }
            return;
        }
        if (now < drill.guardRetryAt) return;
        Vec3 aim = battle.player.getBoundingBox().getCenter();
        Vec3 origin = TrainingBolt.origin(mon);
        if (aim.distanceToSqr(origin) > 100 || level.clip(new ClipContext(origin, aim, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mon)).getType() != HitResult.Type.MISS) return;
        if (aim.distanceToSqr(origin) < 6.25) {
            // Give a player who is right next to Pikachu room to see the projectile coming.
            Vec3 mark = aim.distanceToSqr(TARGET) > aim.distanceToSqr(WEST_MARK) ? TARGET : WEST_MARK;
            mon.setPos(mark.x, mark.y, mark.z);
        }
        var toward = aim.subtract(mon.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
        mon.setYRot(yaw);
        mon.setYBodyRot(yaw);
        mon.setYHeadRot(yaw);
        TrainingBolt.burst(level, TrainingBolt.origin(mon), 12);
        level.playSound(null, mon, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, .75F, 1.6F);
        drill.guardLaunchAt = now + 20;
    }

    private static void advance(Drill drill, ServerPlayer player) {
        switch (drill.cue) {
            case CHALLENGE -> {
                if (drill.resumeSpar) startSpar(drill, player);
                else step(drill, player, Cue.MODE_EXIT);
            }
            case MODE_EXIT -> step(drill, player, Cue.MODE_REENTER);
            case MODE_REENTER -> {
                drill.previousPosition = player.position();
                drill.previousOnGround = player.onGround();
                drill.walked = 0;
                drill.jumped = false;
                step(drill, player, Cue.MOVEMENT);
            }
            case MOVEMENT -> step(drill, player, Cue.LOCK);
            case LOCK -> step(drill, player, Cue.ATTACK);
            case ATTACK -> step(drill, player, Cue.HEAVY);
            case HEAVY -> step(drill, player, Cue.DODGE);
            case DODGE -> step(drill, player, Cue.GUARD);
            case GUARD, PARRY -> {
                Defense.block(player, false);
                step(drill, player, drill.cue == Cue.GUARD ? Cue.PARRY : Cue.MOVE);
            }
            case MOVE -> {
                PrologueStory.finish(player, PrologueStory.DRILLS);
                startSpar(drill, player);
            }
            default -> { }
        }
    }

    private static void startSpar(Drill drill, ServerPlayer player) {
        drill.pikachu.getPokemon().heal();
        drill.pikachu.setHealth(drill.pikachu.getMaxHealth());
        drill.pikachu.setNoAi(false);
        Brawl.train(drill.pikachu, Brawl.Difficulty.EASY);
        Brawl.provoke(Smash.session(player), drill.pikachu);
        step(drill, player, Cue.SPAR);
    }

    private static void step(Drill drill, ServerPlayer player, Cue cue) {
        drill.cue = cue;
        drill.nextAt = 0;
        int now = player.server.getTickCount();
        boolean combatFocus = cue == Cue.LOCK || cue == Cue.ATTACK || cue == Cue.HEAVY || cue == Cue.DODGE || cue.defensive() || cue == Cue.MOVE;
        drill.pauseUntil = now + (combatFocus ? 9 : 0);
        boolean switchFocus = cue == Cue.MODE_EXIT || cue == Cue.MODE_REENTER;
        drill.readyAt = now + (combatFocus ? 32 : cue == Cue.CHALLENGE || switchFocus ? 24 : 0);
        drill.ready = now >= drill.readyAt;
        drill.bolt = null;
        drill.guardRetryAt = drill.readyAt + 16;
        drill.guardLaunchAt = 0;
        sync(drill, player);
    }

    private static void success(Drill drill, ServerPlayer player, int ticks) {
        drill.ready = false;
        drill.nextAt = player.server.getTickCount() + Math.max(33, ticks);
        sync(drill, player);
    }

    private static void sync(Drill drill, ServerPlayer player) {
        ProloguePackets.show(player, drill.cue, drill.pikachu == null ? -1 : drill.pikachu.getId(), drill.ready, drill.moves, drill.nextAt != 0);
    }

    private static Drill drillMember(ServerPlayer player) {
        return InstanceApi.find(player).orElse(null) instanceof Drill drill ? drill : null;
    }

    private static Drill active(ServerPlayer player) {
        Drill drill = drillMember(player);
        return drill != null && drill.getLevel() == player.serverLevel() && drill.getState() == InstanceSession.State.ACTIVE ? drill : null;
    }

    public static boolean isTrainingLevel(ServerLevel level) {
        return InstanceApi.find(level).orElse(null) instanceof Drill;
    }

    /** Runs from the ServerLevel tick injection, never through the server-wide tick-rate manager. */
    public static boolean skipLevelTick(ServerLevel level) {
        if (!(InstanceApi.find(level).orElse(null) instanceof Drill drill) || !drill.cue.focus()) return false;
        int tick = level.getServer().getTickCount();
        if (tick < drill.pauseUntil) return true;
        // Slow the demonstration in flight only. Practising movement and RTC charge timing stay at full speed.
        return drill.cue.defensive() && drill.bolt != null && (tick & 1) != 0;
    }

    public static boolean allowChallenge(ServerPlayer player, PokemonEntity target) {
        Drill drill = active(player);
        return drill == null || drill.cue == Cue.CHALLENGE && drill.ready && drill.pikachu == target;
    }

    public static void challenged(ServerPlayer player, PokemonEntity target) {
        Drill drill = active(player);
        Smash.Session session = Smash.session(player);
        if (drill == null || drill.cue != Cue.CHALLENGE || drill.pikachu != target || session == null) return;
        drill.partner = session.pokemon.getPokemon();
        drill.moves = Math.min(4, Kits.moves(drill.partner).size());
        success(drill, player, 16);
    }

    public static boolean restrict(ServerPlayer player) {
        Drill drill = active(player);
        return LAUNCH.containsKey(player) || drill != null && drill.cue != Cue.SPAR;
    }

    public static boolean allowToggle(ServerPlayer player, Cue action) {
        Drill drill = active(player);
        return drill == null ? !LAUNCH.containsKey(player) : drill.cue == Cue.SPAR || drill.cue == action && drill.ready;
    }

    public static boolean allowCombat(ServerPlayer player, Cue action) {
        Drill drill = active(player);
        return drill == null || drill.cue == Cue.SPAR || drill.cue.accepts(action) && drill.ready;
    }

    static void locked(ServerPlayer player, int id) {
        // Target lock is an RTC client-side camera action. Match its acknowledgement to this private drill.
        Drill drill = active(player);
        if (drill == null || drill.cue != Cue.LOCK || !drill.ready || drill.pikachu.getId() != id
                || !drill.pikachu.isAlive() || player.distanceToSqr(drill.pikachu) > 18 * 18 || Smash.session(player) == null) return;
        success(drill, player, 36);
    }

    public static boolean allowProvoke(Smash.Session battle, Entity target) {
        Drill drill = active(battle.player);
        return drill == null || drill.pikachu != target || drill.cue == Cue.SPAR;
    }

    public static void performed(ServerPlayer player, Cue action, int recovery) {
        Drill drill = active(player);
        if (drill != null && drill.cue == action && drill.ready) success(drill, player, recovery);
    }

    public static int heavyRecovery() { return Math.max(18, SmashConfig.get(SmashConfig.HEAVY_RECOVERY) + 2); }

    @SubscribeEvent
    public static void blockVanillaAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && restrict(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockVanillaUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && restrict(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && restrict(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockEntityUse(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && restrict(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockEntityUseAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && restrict(player)) event.setCanceled(true);
    }

    public static boolean target(Entity entity) { return entity.getTags().contains(TRAINING_PIKACHU); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectDrills(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !(InstanceApi.find(level).orElse(null) instanceof Drill drill)) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(drill.getOwner());
        if (event.getEntity() == drill.pikachu) {
            Smash.Session battle = player == null ? null : Smash.session(player);
            if (drill.cue != Cue.SPAR || battle == null || event.getSource().getEntity() != battle.pokemon
                    && event.getSource().getEntity() != player) event.setCanceled(true);
        } else if (event.getEntity() == player && drill.cue != Cue.SPAR) event.setCanceled(true);
    }

    public static void knockout(Smash.Session battle, PokemonEntity target) {
        Drill drill = active(battle.player);
        if (drill == null || drill.cue != Cue.SPAR || drill.pikachu != target
                || target.isAlive() && !target.getPokemon().isFainted()) return;
        drill.wonPending = true;
    }

    @SubscribeEvent
    public static void victory(InstanceEvent.Victory event) {
        if (event.getSession() instanceof Drill drill && !drill.authorizedWin) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void complete(InstanceEvent.Completed event) {
        if (!(event.getSession() instanceof Drill drill)) return;
        ServerPlayer player = drill.getLevel().getServer().getPlayerList().getPlayer(drill.getOwner());
        if (player == null || !event.getPlayers().contains(player.getUUID())) return;
        step(drill, player, Cue.CLEAR);
        PrologueStory.finish(player, PrologueStory.SPAR);
        if (drill.partner != null) drill.partner.heal();
        if (Smash.active(player)) Smash.end(player, null, false);
    }

    @SubscribeEvent
    public static void left(InstanceEvent.Left event) {
        if (!(event.getSession() instanceof Drill drill)) return;
        if (drill.partner != null) drill.partner.heal();
        ProloguePackets.show(event.getPlayer(), Cue.NONE, -1, false, 0);
        if (event.getReason() == ExitReason.ERROR) {
            event.getPlayer().displayClientMessage(Component.translatable("rumble.prologue.transfer_failed"), false);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LAUNCH.remove(player);
    }
}
