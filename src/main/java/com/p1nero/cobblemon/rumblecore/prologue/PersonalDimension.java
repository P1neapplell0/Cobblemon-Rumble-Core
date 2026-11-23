package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import java.util.List;
import java.util.function.Supplier;
import leaf.soulhome.registry.DimensionRegistry;
import leaf.soulhome.registry.ItemsRegistry;
import leaf.soulhome.utils.DimensionHelper;
import leaf.soulhome.utils.ResourceLocationHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestCompletionPolicy;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.questmarker.api.MarkSpec;
import org.arcadia.arc_quest.questmarker.api.MarkableObject;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class PersonalDimension {
    public static final ResourceLocation BRIEFING = GymStory.secondGymQuest();
    public static final ResourceLocation ID = PrologueWorld.id("personal_dimension");
    public static final int COMPLETE_BIT = 1 << 29, FIRST_VISIT_BIT = 1 << 28;
    public static final String BRIEFING_ROUTE = "space_research";
    static final String ABOUT_ROUTE = "space_ingot";
    private static final String DELIVER = "deliver_ingot", ENTER = "enter_home", KEY = "rumble.personal_dimension.";
    private static final String SEEDED = KEY + "seeded";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonRumbleCore.MOD_ID);
    private static final Supplier<Item> INGOT = ITEMS.register("space_time_ingot", () -> new Item(new Item.Properties()));

    private PersonalDimension() {}

    public static void registerItems(IEventBus bus) { ITEMS.register(bus); }

    public static PhaseBuilder briefingPhase() { return phase(BRIEFING_ROUTE).trackingMarker(PrologueStory.PROFESSOR_MARK); }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var pos = PrologueWorld.ANCHOR;
        event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE).themeColor(SquadQuest.SIDE_THEME)
                .cannotAbandon().icon(INGOT.get()).displayName(QuestText.translatable(KEY + "title"))
                .description(QuestText.translatable(KEY + "description")).requiresQuest(BRIEFING.toString())
                .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(ENTER)
                .phase(phase(DELIVER).trackingMarker(PrologueStory.PROFESSOR_MARK).thenGoTo(ENTER))
                .phase(phase(ENTER).trackingMarker(MarkSpec.translated(ENTER, TrialWaystones.HALL_NAME,
                        new MarkableObject.DimensionPos(PrologueWorld.GYMS, pos.getX(), pos.getY() + 2, pos.getZ())))).build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
    }

    private static PhaseBuilder phase(String phase) {
        return PhaseBuilder.create(phase).displayName(QuestText.translatable(KEY + phase))
                .objective(ObjectiveBuilder.custom(PrologueWorld.id("personal_dimension/" + phase), 1)
                        .display(QuestText.translatable(KEY + phase + ".objective")));
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var briefing = PrologueStory.tree(BRIEFING_ROUTE).repeatable(true);
        briefing.node("start").say(text("start", DialogueText.DialogueArg.playerName()), BRIEFING + "/start").delay(5500).autoNext("material");
        briefing.node("material").say(text("material"), BRIEFING + "/material").delay(4200).autoNext("hint");
        briefing.node("hint").say(text("hint"), BRIEFING + "/hint").choice("done", text("end"), choice -> choice
                .addEvents((p, npc) -> { if (PrologueStory.atDialogueProfessor(p)) finishBriefing(p); }).close());
        event.register(briefing.build());

        var about = PrologueStory.tree(ABOUT_ROUTE).repeatable(true);
        about.node("ask").say(DialogueText.of((p, npc) -> Component.translatable(KEY + (ingot(p) == null ? "hint" : "found"))), ID + "/ask")
                .choice("submit", text("submit"), c -> c.conditionIf((p, npc) -> trackedDelivery(p) && ingot(p) != null)
                        .addEvents((p, npc) -> {
                            if (submit(p)) PrologueStory.after(p, () -> open(p, "space_ready"));
                        }).close())
                .choice("later", text("end"), c -> c.close());
        event.register(about.build());
        var ready = PrologueStory.tree("space_ready").repeatable(true);
        for (int i = 1; i <= 3; i++) ready.node("wait" + i).say(DialogueText.literal("…"), ID + "/wait" + i)
                .delay(650).autoNext(i == 3 ? "ready" : "wait" + (i + 1));
        ready.node("ready").say(text("ready", DialogueText.DialogueArg.playerName()), ID + "/ready")
                .choice("done", text("end"), c -> c.close());
        event.register(ready.build());
    }

    static boolean briefingActive(ServerPlayer player) { return GymStory.phaseActive(player, BRIEFING, BRIEFING_ROUTE); }

    private static void finishBriefing(ServerPlayer player) {
        if (!briefingActive(player)) return;
        QuestProgressHandler.incrementObjective(player, BRIEFING.toString(), BRIEFING_ROUTE, 0, 1);
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!data.isQuestCompleted(BRIEFING.toString())) return;
        GymStory.acceptNext(player);
        QuestProgressHandler.acceptQuest(player, ID.toString());
        player.awardRecipesByKey(List.of(PrologueWorld.id("space_time_ingot")));
        seedCrystals(player);
    }

    private static void seedCrystals(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (data.hasFlag(SEEDED)) return;
        var stack = RewardNotice.stack("beyonddimensions:shattered_space_time_crystallization", 4);
        var net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net != null && net.isOwner(player)) {
            var remainder = net.getUnifiedStorage().insert(new ItemStackKey(stack.copyWithCount(1)), stack.getCount(), false);
            stack.setCount((int) remainder.amount());
        }
        if (!stack.isEmpty()) RewardNotice.grant(player, stack);
        data.setFlag(SEEDED);
    }

    static boolean trackedDelivery(ServerPlayer player) {
        return phaseActive(player, DELIVER) && ID.toString().equals(ArcQuestPlayerManager.getOrCreate(player).getTrackedQuestId());
    }

    static void open(ServerPlayer player, String route) {
        var doctor = PrologueStory.nearby(player);
        if (doctor != null) PrologueStory.open(player, doctor, route);
    }

    private static ItemStack ingot(ServerPlayer player) {
        return player.getInventory().items.stream().filter(stack -> stack.is(INGOT.get())).findFirst().orElse(null);
    }

    private static boolean submit(ServerPlayer player) {
        if (!PrologueStory.atDialogueProfessor(player) || !trackedDelivery(player)) return false;
        var stack = ingot(player);
        if (stack == null) return false;
        stack.shrink(1);
        player.getInventory().setChanged();
        QuestProgressHandler.incrementObjective(player, ID.toString(), DELIVER, 0, 1);
        PhoneState.sync(player);
        return phaseActive(player, ENTER);
    }

    static boolean phaseActive(ServerPlayer player, String phase) {
        var quest = ArcQuestPlayerManager.getOrCreate(player).getActiveQuest(ID.toString());
        return quest != null && quest.isPhaseActive(phase);
    }

    public static int travelBits(ServerPlayer player) {
        return ArcQuestPlayerManager.getOrCreate(player).isQuestCompleted(ID.toString()) ? COMPLETE_BIT
                : phaseActive(player, ENTER) ? FIRST_VISIT_BIT : 0;
    }

    public static boolean allowKeyTravel(ServerPlayer player) {
        if (DimensionHelper.isInSoulDimension(player) || travelBits(player) == COMPLETE_BIT) return true;
        player.displayClientMessage(Component.translatable(KEY + "key_locked"), true);
        return false;
    }

    private static ResourceKey<Level> home(ServerPlayer player) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocationHelper.prefix(player.getUUID().toString()));
    }

    static boolean isOwnHome(ServerPlayer player, ResourceKey<Level> dimension) {
        return dimension.equals(home(player));
    }

    static boolean enter(ServerPlayer player) {
        if (travelBits(player) == 0 || player.level().dimension().equals(home(player))) return false;
        if (!DimensionHelper.isInSoulDimension(player)) {
            DimensionHelper.FlipDimension(player, player.server, List.of(player), player.getUUID());
        } else {
            // Switching rooms must not overwrite the original return position.
            var level = player.server.getLevel(home(player));
            if (level == null) level = DimensionRegistry.createSoulDimension(player.server, home(player), player.getUUID().toString());
            if (level != null) player.teleportTo(level, .5, DimensionHelper.FLOOR_LEVEL + 2, .5, player.getYRot(), player.getXRot());
        }
        return player.level().dimension().equals(home(player));
    }

    @SubscribeEvent
    public static void arrived(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getTo().equals(home(player)) || !phaseActive(player, ENTER)) return;
        QuestProgressHandler.incrementObjective(player, ID.toString(), ENTER, 0, 1);
        var key = new ItemStack(ItemsRegistry.SOUL_KEY.get());
        RewardNotice.grant(player, key);
        player.sendSystemMessage(RewardNotice.component(key));
        player.sendSystemMessage(Component.translatable(KEY + "return", Component.keybind("key.use")));
        PhoneState.sync(player);
    }

    static DialogueText text(String key, DialogueText.DialogueArg... args) { return DialogueText.translatable(KEY + key, args); }
}
