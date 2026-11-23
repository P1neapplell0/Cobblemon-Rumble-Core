package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.block.BerryBlock;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import com.p1nero.misc.simple_player_data_manager.api.PlayerDataRegister;
import com.p1nero.misc.simple_player_data_manager.api.SyncMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestGroupDefinition;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.network.QuestSyncCoordinator;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class DailyCommissions {
    private static final int THEME_COLOR = 0x5AD7FF;
    private static final String KEY = "rumble.daily.", PHASE = "commission";
    private static final List<String> TYPES = List.of("normal", "fire", "water", "electric", "grass", "ice", "fighting",
            "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");
    private static final List<String> KINDS = List.of("defeat", "capture", "fish", "harvest", "plant", "mine", "juice", "home");
    private static final com.p1nero.misc.simple_player_data_manager.api.LongData DAY = PlayerDataRegister.create(CobblemonRumbleCore.MOD_ID)
            .longData("daily_commission_day", Long.MIN_VALUE, SyncMode.SERVER_ONLY);
    private static final com.p1nero.misc.simple_player_data_manager.api.StringData ASSIGNED = PlayerDataRegister.create(CobblemonRumbleCore.MOD_ID)
            .stringData("daily_commission", "", SyncMode.SERVER_ONLY);

    private DailyCommissions() {}

    public static void initialize() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(e -> {
            if (!e.getPlayer().getUUID().equals(e.getPokemon().getOwnerUUID())) return;
            pokemon(e.getPlayer(), "capture", e.getPokemon());
            SafariChapter.captured(e.getPlayer(), e.getPokeBallEntity().level());
        });
        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe(e -> {
            if (e.getBobber().getOwner() instanceof ServerPlayer p) progress(p, "fish");
        });
        CobblemonEvents.BERRY_HARVEST.subscribe(e -> {
            if (!e.getDrops().isEmpty()) progress(e.getPlayer(), "harvest");
        });
    }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var group = PrologueWorld.id("daily_commissions");
        event.registerGroup(new QuestGroupDefinition(group,
                Component.translatable("rumble.quest_group.daily_commissions").withColor(THEME_COLOR), 4, THEME_COLOR));
        int order = 2000;
        for (int i = 0; i < KINDS.size(); i++) {
            String kind = KINDS.get(i);
            for (String type : i < 2 ? TYPES : List.of("")) {
                String variant = variant(kind, type);
                var id = PrologueWorld.id("daily/" + variant);
                var args = type.isEmpty() ? new QuestText.Arg[0] : new QuestText.Arg[]{QuestText.Arg.constant(Component.translatable("cobblemon.type." + type))};
                var objective = QuestText.translatable(KEY + kind + ".objective", args);
                var builder = SquadQuest.sound(QuestBuilder.create(id)).category(QuestCategory.ADVENTURE).sortOrder(order++)
                        .displayName(QuestText.translatable(KEY + kind + ".title", args)).description(objective)
                        .icon(Items.WRITABLE_BOOK).themeColor(THEME_COLOR).cannotAbandon().canBeAutoTrack(false)
                        .requiresQuest(PrologueStory.ID)
                        .unlockCondition((player, quests, flags, variables) -> player != null
                                && variant.equals(ASSIGNED.get(player))
                                && DAY.get(player) == currentDay(player))
                        .phase(PhaseBuilder.create(PHASE).displayName(objective)
                                .objective(ObjectiveBuilder.custom(id, 1).display(objective))
                                .reward(new CobbleDollarReward(i < 3 ? 12_000 : 10_000))
                                .reward(new ItemReward(i < 3 ? com.cobblemon.mod.common.CobblemonItems.POKE_BALL : Items.BONE_MEAL, 3)));
                event.register(builder.build());
                event.assignQuestToGroup(id, group);
            }
        }
    }

    private static String variant(String kind, String type) { return type.isEmpty() ? kind : kind + "/" + type; }

    private static long currentDay(ServerPlayer player) {
        return Math.floorDiv(player.server.overworld().getDayTime(), 24_000L);
    }

    static String refresh(ServerPlayer player) {
        if (!PrologueStory.completed(player)) return "";
        long day = currentDay(player);
        String prior = ASSIGNED.get(player);
        if (day <= DAY.get(player)) return prior;
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!prior.isEmpty()) data.resetQuest(PrologueWorld.id("daily/" + prior).toString());
        int limit = PersonalDimension.travelBits(player) == PersonalDimension.COMPLETE_BIT ? KINDS.size() : KINDS.size() - 1;
        String kind = KINDS.get(player.getRandom().nextInt(limit));
        String selected = variant(kind, kind.equals("defeat") || kind.equals("capture") ? TYPES.get(player.getRandom().nextInt(TYPES.size())) : "");
        ASSIGNED.set(player, selected);
        DAY.set(player, day);
        QuestProgressHandler.acceptQuest(player, PrologueWorld.id("daily/" + selected).toString());
        QuestSyncCoordinator.persistAndSyncIfChanged(player, data);
        return selected;
    }

    private static void progress(ServerPlayer p, String variant) {
        if (variant.equals(refresh(p))) award(p, variant);
    }

    private static void award(ServerPlayer p, String variant) {
        QuestProgressHandler.incrementObjective(p, PrologueWorld.id("daily/" + variant).toString(), PHASE, 0, 1);
    }

    public static void pokemon(ServerPlayer p, String kind, Pokemon pokemon) {
        String selected = refresh(p);
        if (!selected.startsWith(kind + "/")) return;
        for (var type : pokemon.getTypes()) {
            if (selected.equals(variant(kind, type.getName().toLowerCase(Locale.ROOT)))) {
                award(p, selected);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) refresh(p);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void planted(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()
                && p.level().dimension() != PrologueWorld.GYMS && e.getPlacedBlock().getBlock() instanceof BerryBlock) progress(p, "plant");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mined(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || p.isCreative() || !p.hasCorrectToolForDrops(e.getState())) return;
        var id = BuiltInRegistries.BLOCK.getKey(e.getState().getBlock());
        if (id.getNamespace().equals("cobblemon") && id.getPath().matches("(black_|sky_)?tumblestone_cluster")) progress(p, "mine");
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getCrafting().is(com.cobblemon.mod.common.CobblemonItems.BERRY_JUICE)) progress(p, "juice");
    }

    @SubscribeEvent
    public static void arrived(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && PersonalDimension.isOwnHome(p, e.getTo())) progress(p, "home");
    }
}
