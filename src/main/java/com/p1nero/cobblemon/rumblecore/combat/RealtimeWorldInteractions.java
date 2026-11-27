package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Player-cast terrain and vanilla fire reactions; never runs in protected encounter dimensions. */
public final class RealtimeWorldInteractions {
    private RealtimeWorldInteractions() {}

    public static void onMoveHit(Smash.Session source, MoveInfo move, LivingEntity target) {
        if (!allowed(source) || target instanceof PokemonEntity || target.level() != source.player.level()
                || target instanceof ServerPlayer p && Smash.active(p)) return;
        if ("fire".equals(move.type()) && target.isAlive() && !target.fireImmune())
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
        else if ("water".equals(move.type())) target.clearFire();
    }

    public static void melee(Smash.Session source, MoveInfo move, Vec3 facing) {
        var from = source.pokemon.getBoundingBox().getCenter();
        trace(source, move, from, from.add(facing.normalize().scale(Math.min(5, 2 + source.pokemon.getBbWidth() / 2))), true);
    }

    public static boolean trace(Smash.Session source, MoveInfo move, Vec3 from, Vec3 to, boolean physical) {
        if (!allowed(source) || move.isStatus()) return false;
        String type = move.type();
        boolean elemental = "fire".equals(type) || "ice".equals(type) || "water".equals(type);
        if (!elemental && !physical) return false;
        var level = source.player.serverLevel();
        // Cap long beams and stop before an unloaded chunk rather than asking clip() to load it.
        var delta = to.subtract(from);
        if (delta.lengthSqr() > 144) to = from.add(delta.normalize().scale(12));
        for (int i = 0, steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) * 2)); i <= steps; i++) {
            if (!level.hasChunkAt(BlockPos.containing(from.lerp(to, (double) i / steps)))) return false;
        }
        // SOURCE_ONLY misses flowing water and the shallow water layer commonly used
        // as a surface. Ice moves should interact with both source and flowing water;
        // fire still uses a solid-block trace so it keeps its existing behaviour.
        var hit = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE,
                "ice".equals(type) ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, source.pokemon));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        var player = source.player;
        if ("fire".equals(type)) {
            if (state.is(Blocks.ICE)) return replace(level, player, pos, level.dimensionType().ultraWarm()
                    ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), hit.getDirection());
            var next = pos.relative(hit.getDirection());
            if (state.isFlammable(level, pos, hit.getDirection()) && level.hasChunkAt(next) && level.isEmptyBlock(next)) {
                var fire = net.minecraft.world.level.block.BaseFireBlock.getState(level, next);
                if (fire.canSurvive(level, next)) return replace(level, player, next, fire, hit.getDirection());
            }
        } else if ("ice".equals(type) && state.is(Blocks.WATER)
                && level.isEmptyBlock(pos.above())) {
            if (replace(level, player, pos, Blocks.FROSTED_ICE.defaultBlockState(), hit.getDirection())) {
                level.scheduleTick(pos, Blocks.FROSTED_ICE, 40);
                return true;
            }
        } else if ("water".equals(type) && state.is(BlockTags.FIRE)) {
            return replace(level, player, pos, Blocks.AIR.defaultBlockState(), hit.getDirection());
        }
        return physical && (state.is(BlockTags.LEAVES) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.COBWEB))
                && replace(level, player, pos, Blocks.AIR.defaultBlockState(), hit.getDirection());
    }

    private static boolean replace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState next, Direction face) {
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayBuild() || player.isSpectator()
                || !level.getWorldBorder().isWithinBounds(pos)) return false;
        var current = level.getBlockState(pos);
        if (current == next || current.hasBlockEntity() || current.getDestroySpeed(level, pos) < 0) return false;
        if (!current.isAir() && NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, current, player)).isCanceled()) return false;
        if (next.isAir()) return level.destroyBlock(pos, false, player);
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, next, 0)) return false;
        if (EventHooks.onBlockPlace(player, snapshot, face)) {
            snapshot.restore();
            return false;
        }
        level.markAndNotifyBlock(pos, level.getChunkAt(pos), current, next, 3, 512);
        return true;
    }

    private static boolean allowed(Smash.Session source) {
        var level = source.player.serverLevel();
        return source.pokemon.level() == level && Smash.session(source.player) == source
                && !PrologueWorld.GYMS.equals(level.dimension()) && !InstanceManager.isInstanceDimension(level.dimension())
                && !ArenaMatch.isArena(level) && !com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining.isTrainingLevel(level);
    }
}
