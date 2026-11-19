package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.MegaTrials;
import com.p1nero.cobblemon.rumblecore.prologue.client.MegaSimulatorScreen;
import com.p1nero.cobblebashscreen.simulator.TrainingSimulatorMenu;
import com.p1nero.misc.custom_instances.api.InstanceEntrance;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class MegaSimulator {
    public static final BlockPos POS = PrologueWorld.ANCHOR.south(5);
    private static final List<BlockPos> TERMINALS = List.of(POS, PrologueWorld.ANCHOR.north(5));
    public static final String UNIVERSE_UNLOCKED = "rumble.simulator.universe_unlocked";
    private static final String KEY = "rumble.mega.simulator.";
    public static final int MEGA = 0, UNIVERSE = 1, SAFARI = 2;
    private static final ResourceLocation BASH_BLOCK = ResourceLocation.parse("cobblebash:training_simulator");
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CobblemonRumbleCore.MOD_ID);
    private static final java.util.function.Supplier<Block> TERMINAL = BLOCKS.register("mega_simulator",
            () -> new Block(BlockBehaviour.Properties.of().strength(5).lightLevel(state -> 5)));

    private MegaSimulator() { }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        modBus.addListener(MegaSimulator::packets);
    }

    private static boolean isTerminal(Level level, BlockPos pos) {
        if (!PrologueWorld.GYMS.equals(level.dimension()) || !level.hasChunkAt(pos)) return false;
        Block block = level.getBlockState(pos).getBlock();
        return block == TERMINAL.get() || BASH_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static BlockPos nearestTerminal(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return null;
        return TERMINALS.stream().filter(pos -> pos.closerToCenterThan(player.position(), 8) && isTerminal(player.level(), pos))
                .min(java.util.Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(player.position())))
                .orElse(null);
    }

    public static boolean atTerminal(ServerPlayer player) { return nearestTerminal(player) != null; }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        var clicked = event.getPos();
        var base = TERMINALS.contains(clicked) ? clicked : clicked.below();
        if (!TERMINALS.contains(base) || !isTerminal(event.getLevel(), base)) return;
        if (!clicked.equals(base) && !BASH_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(clicked).getBlock()))) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && atTerminal(player)) {
            PacketDistributor.sendToPlayer(player, new Open(MegaTrials.unlocked(player), universeUnlocked(player), SafariChapter.unlocked(player)));
        }
    }

    static void unlockUniverse(ServerPlayer player) {
        ArcQuestPlayerManager.getOrCreate(player).setFlag(UNIVERSE_UNLOCKED);
    }

    private static boolean universeUnlocked(ServerPlayer player) {
        return ArcQuestPlayerManager.getOrCreate(player).hasFlag(UNIVERSE_UNLOCKED);
    }

    private static void choose(ServerPlayer player, int selection) {
        var terminal = nearestTerminal(player);
        if (terminal == null) return;
        if (selection == MEGA && MegaTrials.unlocked(player)) {
            new Entrance(player.serverLevel(), terminal).openInstanceCatalog(player);
        } else if (selection == UNIVERSE && universeUnlocked(player)) {
            openUniverse(player);
        } else if (selection == SAFARI && SafariChapter.unlocked(player)) {
            SafariChapter.openAdmission(player);
        } else if (selection >= MEGA && selection <= SAFARI) {
            player.displayClientMessage(Component.translatable(KEY + "locked"), true);
        }
    }

    private static void openUniverse(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new TrainingSimulatorMenu(id, inventory),
                Component.translatable("container.cobblebash.training_simulator")));
    }

    private record Entrance(ServerLevel level, BlockPos terminal) implements InstanceEntrance {
        @Override public List<String> getTemplateIds() { return MegaTrials.ids(); }
        @Override public void addTemplateId(String id) { throw new UnsupportedOperationException(); }
        @Override public void removeTemplateId(String id) { throw new UnsupportedOperationException(); }
        @Override public Level entranceLevel() { return level; }
        @Override public Vec3 entrancePosition() { return Vec3.atCenterOf(terminal); }
        @Override public boolean isEntranceValid() { return isTerminal(level, terminal); }
        @Override public boolean canUseEntrance(ServerPlayer player) { return level == player.level() && player.isAlive() && !player.isSpectator()
                && terminal.closerToCenterThan(player.position(), 8) && isEntranceValid() && MegaTrials.unlocked(player); }
        @Override public String catalogTitle() { return "{rumble.mega.simulator.mega}"; }
        @Override public void writeCatalogIcon(String template, CompoundTag icon) {
            var trial = MegaTrials.get(template);
            if (trial == null) return;
            icon.putString("renderer", MegaTrials.PORTRAIT.toString());
            icon.putString("species", "cobblemon:" + trial.species());
            icon.putString("aspects", String.join(",", trial.aspects()));
        }
    }

    public record Open(boolean mega, boolean universe, boolean safari) implements CustomPacketPayload {
        static final Type<Open> TYPE = new Type<>(PrologueWorld.id("simulator_open"));
        static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Open::mega, ByteBufCodecs.BOOL, Open::universe, ByteBufCodecs.BOOL, Open::safari, Open::new);
        @Override public Type<Open> type() { return TYPE; }
    }

    public record Select(int selection) implements CustomPacketPayload {
        static final Type<Select> TYPE = new Type<>(PrologueWorld.id("simulator_select"));
        static final StreamCodec<RegistryFriendlyByteBuf, Select> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Select::selection, Select::new);
        @Override public Type<Select> type() { return TYPE; }
    }

    private static void packets(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Open.TYPE, Open.CODEC, (packet, ctx) -> MegaSimulatorScreen.open(packet));
        registrar.playToServer(Select.TYPE, Select.CODEC, (packet, ctx) -> choose((ServerPlayer) ctx.player(), packet.selection()));
    }
}
