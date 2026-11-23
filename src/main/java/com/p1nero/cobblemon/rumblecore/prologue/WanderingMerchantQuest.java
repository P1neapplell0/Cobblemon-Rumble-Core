package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueContext;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

public final class WanderingMerchantQuest {
    static final BlockPos POS = PrologueWorld.ANCHOR.south(2);
    static final String NAME = "entity.cobblemon_rumble_core.trial_merchant";
    private static final String KEY = "rumble.merchant.", PHASE = "bring_tea";
    static final ResourceLocation ID = PrologueWorld.id("wandering_merchant");

    private WanderingMerchantQuest() { }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var objective = QuestText.translatable(KEY + "objective");
        event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE).sortOrder(5)
                .displayName(QuestText.translatable(NAME)).description(objective).themeColor(SquadQuest.SIDE_THEME)
                .icon(Items.EMERALD).cannotAbandon().requiresQuest(GymStory.fourthGymQuest())
                .phase(PhaseBuilder.create(PHASE).displayName(objective).objective(ObjectiveBuilder.custom(ID, 1).display(objective))
                        .trackingMarker(HallQuestMarkers.actor(PHASE, NAME, FixedStoryActors.MERCHANT, POS))).build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
    }

    static void accept(ServerPlayer player) { QuestProgressHandler.acceptQuest(player, ID.toString()); }

    static boolean visible(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        return LegendaryCommission.meeting(player) || LegendaryCommission.unlocked(player)
                || data.isQuestCompleted(ID.toString()) || data.isQuestActive(ID.toString()) && ID.toString().equals(data.getTrackedQuestId())
                || session != null && FixedStoryActors.MERCHANT.equals(FixedStoryActors.role(session.getEntity()));
    }

    public static boolean canTrade(ServerPlayer player, TrialMerchantEntity trader) {
        return at(player, trader) && ArcQuestPlayerManager.getOrCreate(player).isQuestCompleted(ID.toString());
    }

    static boolean at(ServerPlayer player, Entity trader) {
        return trader == FixedStoryActors.find(player.serverLevel(), FixedStoryActors.MERCHANT) && trader != null && trader.isAlive()
                && player.isAlive() && !player.isSpectator() && player.distanceToSqr(trader) <= 16 && player.hasLineOfSight(trader);
    }

    static boolean talk(ServerPlayer player, Entity trader) {
        if (!(trader instanceof TrialMerchantEntity merchant) || !at(player, trader) || !visible(player)) return false;
        player.playNotifySound(SoundEvents.WANDERING_TRADER_YES, SoundSource.NEUTRAL, 0.65F, 1F);
        if (LegendaryCommission.meeting(player)) { LegendaryCommission.talk(player, merchant); return true; }
        if (LegendaryCommission.unlocked(player) && !canTrade(player, merchant)
                && !GymStory.phaseActive(player, ID, PHASE)) {
            DialogueSessionManager.INSTANCE.startDialogue(player, trader, tree("legendary_stock"), new DialogueContext());
            return true;
        }
        DialogueSessionManager.INSTANCE.startDialogue(player, trader, tree(canTrade(player, (TrialMerchantEntity) trader) ? "trade" : "quest"), new DialogueContext());
        return true;
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        String id = tree("quest");
        var quest = DialogueTreeBuilder.create(id).npc(DialogueText.translatable(NAME)).repeatable(true);
        quest.node("thirsty").say(text("thirsty"), id + "/thirsty").choice("who", text("who"), c -> c.goTo("who"))
                .choice("legendary", DialogueText.translatable("rumble.legendary.browse"),
                        c -> c.conditionIf((p, npc) -> LegendaryCommission.unlocked(p))
                                .addEvents((p, npc) -> PrologueStory.after(p, () -> MerchantPanels.openLegendary(p))).close());
        quest.node("who").say(text("introduce"), id + "/who").choice("tea", DialogueText.of(ctx ->
                Component.translatable(KEY + "tea").withStyle(ChatFormatting.GOLD)), c -> c.goTo("check"));
        quest.node("check").say(DialogueText.of(ctx -> Component.translatable(KEY + (hasTea(ctx.player(), false) ? "ready" : "unknown"))), id + "/check")
                .choice("submit", text("submit"), c -> c.conditionIf((p, npc) -> hasTea(p, false)).addEvents((p, npc) -> deliver(p)).goTo("delivered"))
                .choice("recipe", text("recipe"), c -> c.goTo("guide"));
        quest.node("guide").say(text("guide"), id + "/guide").delay(3400).autoNext("wish");
        quest.node("wish").say(text("wish"), id + "/wish").choice("leave", text("leave"), c -> c.close());
        quest.node("delivered").say(DialogueText.of(ctx -> Component.translatable(KEY +
                (ArcQuestPlayerManager.getOrCreate(ctx.player()).isQuestCompleted(ID.toString()) ? "thanks" : "wish"))), id + "/delivered")
                .delay(2700).autoNext("stock");
        stock(quest, id);
        event.register(quest.build());

        id = tree("trade");
        var trade = DialogueTreeBuilder.create(id).npc(DialogueText.translatable(NAME)).repeatable(true);
        stock(trade, id);
        event.register(trade.build());

        id = tree("legendary_stock");
        var legendary = DialogueTreeBuilder.create(id).npc(DialogueText.translatable(NAME)).repeatable(true);
        stock(legendary, id);
        event.register(legendary.build());
    }

    private static void stock(DialogueTreeBuilder tree, String id) {
        tree.node("stock").say(text("stock"), id + "/stock")
                .choice("trade", text("trade"), c -> c.conditionIf((p, npc) -> npc instanceof TrialMerchantEntity trader && canTrade(p, trader))
                        .addEvents((p, npc) -> openShopAfterDialogue(p)).close())
                .choice("odds", text("odds"), c -> c.conditionIf((p, npc) -> npc instanceof TrialMerchantEntity trader && canTrade(p, trader))
                        .addEvents((p, npc) -> PrologueStory.after(p, () -> MerchantPanels.openOdds(p))).close())
                .choice("legendary", DialogueText.translatable("rumble.legendary.browse"),
                        c -> c.conditionIf((p, npc) -> LegendaryCommission.unlocked(p))
                                .addEvents((p, npc) -> PrologueStory.after(p, () -> MerchantPanels.openLegendary(p))).close())
                .choice("leave", text("leave"), c -> c.close());
    }

    private static TrialMerchantEntity dialogueTrader(ServerPlayer player) {
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        if (session != null && session.getEntity() instanceof TrialMerchantEntity trader && canTrade(player, trader)) return trader;
        return null;
    }

    private static void openShopAfterDialogue(ServerPlayer player) {
        var trader = dialogueTrader(player);
        if (trader == null) return;
        PrologueStory.after(player, () -> {
            if (!canTrade(player, trader)) return;
            PlayerExtensionKt.openShop(player, trader);
            trader.getTradingPlayers().add(player);
        });
    }

    private static void deliver(ServerPlayer player) {
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        if (session == null || !at(player, session.getEntity()) || !visible(player)
                || !GymStory.phaseActive(player, ID, PHASE) || !hasTea(player, true)) return;
        QuestProgressHandler.incrementObjective(player, ID.toString(), PHASE, 0, 1);
    }

    /** Bag first, then the player's own cloud; do not withdraw from a shared network. */
    private static boolean hasTea(ServerPlayer player, boolean take) {
        var tea = RewardNotice.stack("cobblemon:sinister_tea", 1);
        for (var slot : player.getInventory().items) {
            if (!slot.is(tea.getItem())) continue;
            if (take) { slot.shrink(1); player.getInventory().setChanged(); }
            return true;
        }
        var net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null || !net.isOwner(player)) return false;
        var storage = net.getUnifiedStorage();
        for (var entry : storage.getStorage()) {
            if (entry.key() instanceof ItemStackKey key && key.getReadOnlyStack().is(tea.getItem())
                    && storage.extract(key, 1, !take, false).amount() == 1) return true;
        }
        return false;
    }

    private static String tree(String suffix) { return PrologueWorld.id("story/merchant/" + suffix).toString(); }
    private static DialogueText text(String suffix) { return DialogueText.translatable(KEY + suffix); }
}
