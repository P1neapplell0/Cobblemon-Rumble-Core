package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog.Gym;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.EntityTeleportResult;
import net.blay09.mods.waystones.api.MutableWaystone;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneOrigin;
import net.blay09.mods.waystones.api.WaystoneTypes;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.error.WaystoneTeleportError;
import net.blay09.mods.waystones.api.event.WaystoneInitializedEvent;
import net.blay09.mods.waystones.api.event.WaystoneTeleportEntityEvent;
import net.blay09.mods.waystones.block.ModBlocks;
import net.blay09.mods.waystones.block.WaystoneBlockBase;
import net.blay09.mods.waystones.block.entity.WaystoneBlockEntityBase;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WaystoneManagerImpl;
import net.blay09.mods.waystones.core.WaystoneSyncManager;
import net.blay09.mods.waystones.item.ModItems;
import net.blay09.mods.waystones.menu.WaystoneSelectionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class TrialWaystones {
    public static final int STORY_TRAVEL_BIT = 1 << 30;
    public static final String HALL_NAME = "rumble.waystones.hall";
    public static final String OVERWORLD_NAME = "rumble.waystones.overworld";
    public static final String LOCKED = "rumble.waystones.locked";
    private static final BlockPos BASE = PrologueWorld.ANCHOR;
    private static final BlockPos TOP = BASE.above();
    private static final Map<BlockPos, Gym> GYM_STONES = Map.of(
            new BlockPos(-9, 52, -139), Gym.ROCK, new BlockPos(-108, 47, -107), Gym.GRASS,
            new BlockPos(-144, 52, 7), Gym.GHOST, new BlockPos(-121, 57, 116), Gym.ICE,
            new BlockPos(-5, 49, 134), Gym.GROUND, new BlockPos(107, 55, 125), Gym.FIRE,
            new BlockPos(125, 61, 5), Gym.DARK, new BlockPos(87, 56, -106), Gym.ELECTRIC);

    private TrialWaystones() {}

    public static void initialize() {
        Balm.getEvents().onEvent(WaystoneInitializedEvent.class, event -> nameGymStone(ServerLifecycleHooks.getCurrentServer(), event.getWaystone()));
        Balm.getEvents().onEvent(WaystoneTeleportEntityEvent.Pre.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            boolean toHall = event.getTargetLevel().dimension() == PrologueWorld.GYMS;
            boolean locked = !travelUnlocked(player) && (player.level().dimension() == PrologueWorld.GYMS || toHall);
            if (locked || toHall && !GymStory.allowsArrival(player, BlockPos.containing(event.getTargetPosition()))) {
                event.overrideResult(EntityTeleportResult.failed(player, event.getOriginalDestination(),
                        new WaystoneTeleportError(Component.translatable(locked ? LOCKED : "rumble.gym.locked"))));
            }
        });
    }

    private static BlockPos fixedBase(BlockPos pos) {
        if (pos.equals(BASE) || GYM_STONES.containsKey(pos)) return pos;
        BlockPos below = pos.below();
        return below.equals(BASE) || GYM_STONES.containsKey(below) ? below : null;
    }

    private static boolean isFixedRecord(Waystone stone, BlockPos base) {
        return stone.getDimension().equals(PrologueWorld.GYMS) && WaystoneTypes.WAYSTONE.equals(stone.getWaystoneType())
                && !stone.isTransient() && (base.equals(stone.getPos()) || base.above().equals(stone.getPos()));
    }

    private static List<Waystone> storedStones(ServerLevel level, BlockPos base) {
        return WaystoneManagerImpl.get(level.getServer()).getWaystones().filter(stone -> isFixedRecord(stone, base))
                .sorted(Comparator.<Waystone, Boolean>comparing(stone -> !base.equals(stone.getPos()))
                        .thenComparing(Waystone::getWaystoneUid))
                .toList();
    }

    /** Look up the identity before Waystones generates a new UUID for a freshly loaded block entity. */
    public static Waystone restoreFixedStone(ServerLevel level, BlockPos pos, UUID preferred) {
        if (!level.dimension().equals(PrologueWorld.GYMS) || !level.getBlockState(pos).is(ModBlocks.waystone)) return null;
        var base = fixedBase(pos);
        if (base == null) return null;
        var manager = WaystoneManagerImpl.get(level.getServer());
        var stone = preferred == null ? null : manager.getWaystoneById(preferred).filter(s -> isFixedRecord(s, base)).orElse(null);
        if (stone == null) stone = storedStones(level, base).stream().findFirst().orElse(null);
        if (stone != null) nameGymStone(level.getServer(), stone);
        return stone;
    }

    private static void nameGymStone(MinecraftServer server, Waystone stone) {
        if (server == null || !stone.getDimension().equals(PrologueWorld.GYMS)
                || !WaystoneTypes.WAYSTONE.equals(stone.getWaystoneType()) || !(stone instanceof MutableWaystone mutable)) return;
        var base = fixedBase(stone.getPos());
        if (base == null) return;
        var name = Component.translatable(base.equals(BASE) ? HALL_NAME : "waystones.incandescentveritycore.gym." + GYM_STONES.get(base).path());
        if (name.equals(stone.getName()) && base.equals(stone.getPos())) return;
        mutable.setName(name);
        mutable.setPos(base);
        WaystoneManagerImpl.get(server).updateWaystone(stone);
        WaystoneSyncManager.sendWaystoneUpdateToAll(server, stone);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void repairActivatedStones(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var hall = player.server.getLevel(PrologueWorld.GYMS);
        if (hall == null) return;
        var data = PlayerWaystoneManager.getPlayerWaystoneData(player.level());
        boolean changed = false;
        var bases = PlayerWaystoneManager.getActivatedWaystones(player).stream()
                .filter(stone -> stone.getDimension().equals(PrologueWorld.GYMS))
                .map(stone -> fixedBase(stone.getPos())).filter(Objects::nonNull).distinct().toList();
        for (var base : bases) {
            var records = storedStones(hall, base);
            if (records.size() < 2) continue;
            if (!(hall.getBlockEntity(base) instanceof WaystoneBlockEntityBase lower)) continue;
            var canonical = lower.getWaystone();
            if (!canonical.isValid() || !isFixedRecord(canonical, base)) continue;
            for (var duplicate : records) {
                if (canonical.getWaystoneUid().equals(duplicate.getWaystoneUid()) || !data.isWaystoneActivated(player, duplicate)) continue;
                // Repair the saved activation without replaying announcements or creating another minimap marker.
                if (!data.isWaystoneActivated(player, canonical)) data.activateWaystone(player, canonical);
                data.deactivateWaystone(player, duplicate);
                changed = true;
            }
        }
        if (changed) WaystoneSyncManager.sendActivatedWaystones(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void nameGymOnUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.serverLevel().dimension() != PrologueWorld.GYMS) return;
        var pos = event.getPos();
        var base = GYM_STONES.containsKey(pos) ? pos : pos.below();
        if (GYM_STONES.containsKey(base)) WaystonesAPI.getWaystoneAt(player.serverLevel(), base)
                .ifPresent(stone -> nameGymStone(player.server, stone));
    }

    static void install(ServerLevel level) {
        var base = ModBlocks.waystone.defaultBlockState().setValue(WaystoneBlockBase.FACING, Direction.WEST)
                .setValue(WaystoneBlockBase.ORIGIN, WaystoneOrigin.UNKNOWN);
        if (!level.getBlockState(BASE).is(ModBlocks.waystone) || !level.getBlockState(TOP).is(ModBlocks.waystone)) {
            level.setBlock(BASE, base.setValue(WaystoneBlockBase.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
            level.setBlock(TOP, base.setValue(WaystoneBlockBase.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        }
        if (level.getBlockEntity(BASE) instanceof WaystoneBlockEntityBase lower
                && level.getBlockEntity(TOP) instanceof WaystoneBlockEntityBase upper) {
            lower.initializeWaystone(level, null, WaystoneOrigin.UNKNOWN);
            nameGymStone(level.getServer(), lower.getWaystone());
            if (!upper.getWaystone().isValid() || !upper.getWaystone().getWaystoneUid().equals(lower.getWaystone().getWaystoneUid())) {
                upper.initializeFromBase(lower);
            }
        }
    }

    public static boolean travelUnlocked(ServerPlayer player) { return SquadQuest.travelUnlocked(player); }

    private static boolean anchor(Level level, BlockPos pos) {
        return level.dimension() == PrologueWorld.GYMS && (pos.equals(BASE) || pos.equals(TOP));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !anchor(event.getLevel(), event.getPos())) return;
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
        if (!travelUnlocked(player)) {
            player.displayClientMessage(Component.translatable(LOCKED), true);
        } else if (!canTravel(player)) {
            player.displayClientMessage(Component.translatable("rumble.waystones.busy"), true);
        } else {
            if (!(player.serverLevel().getBlockEntity(BASE) instanceof WaystoneBlockEntityBase existing)
                    || !existing.getWaystone().isValidInLevel(player.serverLevel())) install(player.serverLevel());
            if (!(player.serverLevel().getBlockEntity(BASE) instanceof WaystoneBlockEntityBase stone)
                    || !stone.getWaystone().isValidInLevel(player.serverLevel())) {
                player.displayClientMessage(Component.translatable("rumble.waystones.unavailable"), true);
                return;
            }
            // Open the native list immediately, not the first-use naming/attunement flow.
            if (!PlayerWaystoneManager.isWaystoneActivated(player, stone.getWaystone())) {
                PlayerWaystoneManager.activateWaystone(player, stone.getWaystone());
            }
            stone.getSelectionMenuProvider(player).ifPresentOrElse(
                    menu -> Balm.getNetworking().openGui(player, menu),
                    () -> player.displayClientMessage(Component.translatable("rumble.waystones.unavailable"), true));
        }
    }

    @SubscribeEvent
    public static void protectAnchor(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && anchor(level, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void protectPlacement(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != PrologueWorld.GYMS) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isCreative()) return;
            // Placement restores the held stack after the canceled event returns.
            player.server.tell(new TickTask(player.server.getTickCount(), player.containerMenu::broadcastFullState));
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void protectAnchor(ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension() == PrologueWorld.GYMS) event.getAffectedBlocks().removeIf(pos -> anchor(event.getLevel(), pos));
    }

    private static boolean canTravel(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isVehicle()
                && !Smash.active(player) && BattleRegistry.getBattleByParticipatingPlayer(player) == null
                && !InstanceManager.isInstanceDimension(player.level().dimension()) && !PrologueTraining.launching(player)
                && !DialogueSessionManager.INSTANCE.isInDialogue(player);
    }

    private static boolean validSource(ServerPlayer player, WaystoneSelectionMenu menu) {
        Waystone source = menu.getWaystoneFrom();
        if (source != null) {
            return source.getDimension() == player.level().dimension() && source.isValidInLevel(player.serverLevel())
                    && player.distanceToSqr(source.getPos().getCenter()) <= 36;
        }
        return menu.getWarpItem().is(ModItems.warpStone) && (PhoneState.hasPhone(player)
                || player.getMainHandItem().is(ModItems.warpStone) || player.getOffhandItem().is(ModItems.warpStone));
    }

    static void travel(ServerPlayer player, int menuId, boolean toHall) {
        if (!validTravelMenu(player, menuId)) return;
        ServerLevel level = toHall ? player.server.getLevel(PrologueWorld.GYMS) : player.server.overworld();
        if (level == null || level == player.level()) return;
        BlockPos pos = safeArrival(player, level, toHall ? PrologueWorld.ARRIVAL : VillageArrival.get(level));
        if (pos == null && !toHall) pos = safeArrival(player, level, level.getSharedSpawnPos());
        if (pos == null) {
            player.displayClientMessage(Component.translatable("rumble.waystones.no_safe_arrival"), true);
            return;
        }
        if (player.teleportTo(level, pos.getX() + .5, pos.getY(), pos.getZ() + .5, Set.<RelativeMovement>of(),
                toHall ? -135f : level.getSharedSpawnAngle(), 0f)) {
            player.closeContainer();
            player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .65f, 1.25f);
            SquadQuest.arrived(player);
        }
    }

    static void travelHome(ServerPlayer player, int menuId) {
        if (validTravelMenu(player, menuId) && PersonalDimension.enter(player)) {
            player.closeContainer();
            player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .65f, 1.25f);
        }
    }

    private static boolean validTravelMenu(ServerPlayer player, int menuId) {
        return travelUnlocked(player) && canTravel(player) && player.containerMenu instanceof WaystoneSelectionMenu menu
                && menu.containerId == menuId && menu.stillValid(player) && validSource(player, menu);
    }

    private static BlockPos safeArrival(ServerPlayer player, ServerLevel level, BlockPos origin) {
        if (safe(player, level, origin)) return origin;
        for (BlockPos column : BlockPos.spiralAround(origin, 5, Direction.EAST, Direction.SOUTH)) {
            BlockPos local = column.immutable();
            if (safe(player, level, local)) return local;
            if (!level.getWorldBorder().isWithinBounds(local)) continue;
            level.getChunkAt(local);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, local);
            if (safe(player, level, surface)) return surface;
        }
        return null;
    }

    private static boolean safe(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos.below())
                || level.isOutsideBuildHeight(pos.above())) return false;
        var supportPos = pos.below();
        var support = level.getBlockState(supportPos);
        return support.isFaceSturdy(level, supportPos, Direction.UP)
                && !support.is(Blocks.MAGMA_BLOCK) && !support.is(Blocks.CAMPFIRE) && !support.is(Blocks.SOUL_CAMPFIRE)
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                && level.noCollision(player, new AABB(pos.getX() + .2, pos.getY(), pos.getZ() + .2,
                        pos.getX() + .8, pos.getY() + 1.8, pos.getZ() + .8));
    }
}
