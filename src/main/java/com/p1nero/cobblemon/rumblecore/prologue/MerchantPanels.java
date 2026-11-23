package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.LegendaryDirectoryScreen;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.MerchantOddsScreen;
import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials;
import com.p1nero.cobblemon.rumblecore.shop.WanderingCatalogue;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

/** Sends a bounded server-side snapshot; C2S requests still recheck the live merchant and quest. */
public final class MerchantPanels {
    private MerchantPanels() {}

    private static TrialMerchantEntity nearby(ServerPlayer player) {
        return FixedStoryActors.find(player.serverLevel(), FixedStoryActors.MERCHANT) instanceof TrialMerchantEntity merchant
                && WanderingMerchantQuest.at(player, merchant) ? merchant : null;
    }

    public static void openLegendary(ServerPlayer player) {
        if (!LegendaryCommission.unlocked(player) || nearby(player) == null) return;
        var data = ArcQuestPlayerManager.getOrCreate(player);
        var rows = new ListTag();
        for (var trial : LegendaryTrials.all()) {
            var row = new CompoundTag();
            row.putString("id", trial.id());
            String quest = trial.quest().toString();
            row.putByte("state", (byte) (!trial.supported() ? -1 : data.isQuestCompleted(quest) ? 2 : data.isQuestActive(quest) ? 1 : 0));
            rows.add(row);
        }
        PacketDistributor.sendToPlayer(player, new Directory(withRows(rows)));
    }

    private static void select(ServerPlayer player, String id) {
        var trial = LegendaryTrials.get(id);
        if (trial == null || !trial.supported() || !LegendaryCommission.unlocked(player) || nearby(player) == null) return;
        var current = InstanceApi.find(player).orElse(null);
        if (current != null && !current.getDefinition().id().equals(trial.template())) {
            player.displayClientMessage(Component.translatable("rumble.legendary.other_room"), true);
            return;
        }
        try {
            LegendaryCommission.acceptTrial(player, trial);
            InstanceNetwork.openLobby(player, trial.template());
        } catch (RuntimeException e) { InstanceManager.error(player, e); }
    }

    public static void openOdds(ServerPlayer player) {
        var merchant = nearby(player);
        if (merchant == null || !WanderingMerchantQuest.canTrade(player, merchant)) return;
        var rows = new ListTag();
        for (var entry : WanderingCatalogue.odds()) {
            var row = new CompoundTag();
            row.putString("item", entry.item());
            row.putInt("price", entry.price());
            row.putDouble("fraction", entry.fraction());
            rows.add(row);
        }
        PacketDistributor.sendToPlayer(player, new Odds(withRows(rows)));
    }

    private static CompoundTag withRows(ListTag rows) {
        var data = new CompoundTag();
        data.put("rows", rows);
        return data;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Directory.TYPE, Directory.CODEC, (packet, ctx) -> LegendaryDirectoryScreen.open(packet.data()));
        registrar.playToClient(Odds.TYPE, Odds.CODEC, (packet, ctx) -> MerchantOddsScreen.open(packet.data()));
        registrar.playToServer(Select.TYPE, Select.CODEC, (packet, ctx) -> select((ServerPlayer) ctx.player(), packet.id()));
    }

    public record Directory(CompoundTag data) implements CustomPacketPayload {
        static final Type<Directory> TYPE = new Type<>(PrologueWorld.id("legendary_directory"));
        static final StreamCodec<RegistryFriendlyByteBuf, Directory> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Directory::data, Directory::new);
        public Type<Directory> type() { return TYPE; }
    }

    public record Select(String id) implements CustomPacketPayload {
        static final Type<Select> TYPE = new Type<>(PrologueWorld.id("legendary_select"));
        static final StreamCodec<RegistryFriendlyByteBuf, Select> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Select::id, Select::new);
        public Type<Select> type() { return TYPE; }
    }

    public record Odds(CompoundTag data) implements CustomPacketPayload {
        static final Type<Odds> TYPE = new Type<>(PrologueWorld.id("merchant_odds"));
        static final StreamCodec<RegistryFriendlyByteBuf, Odds> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Odds::data, Odds::new);
        public Type<Odds> type() { return TYPE; }
    }
}
