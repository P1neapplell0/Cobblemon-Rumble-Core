package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.Cobblemon;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.MegaCombat;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.gym.GymTeamPokemon;
import com.p1nero.cobblemon.rumblecore.gym.MegaTrials;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import com.p1nero.misc.custom_instances.api.InstanceEvent;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueContext;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestCompletionPolicy;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questmarker.api.MarkSpec;
import org.arcadia.arc_quest.questmarker.api.MarkableObject;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class MegaTrainingQuest {
    private static final String KEY = "rumble.mega.training.";
    private static final String CRAFT = "craft_device", TRIAL = "clear_simulation", LESSON = "battle_may", FAREWELL = "farewell";
    public static final String TREE = PrologueWorld.id("story/mega_training").toString();
    private static final ResourceLocation ID = PrologueWorld.id("mega_training");
    private static final int REWARD = 20_000;
    private static final GymCatalog.Actor MAY = new GymCatalog.Actor("may", MegaIntroduction.MAY_POS, GymTeamPokemon::may);

    private MegaTrainingQuest() {}

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var pos = MegaSimulator.POS;
        var may = MegaIntroduction.MAY_POS;
        var terminal = MarkSpec.translated("mega_terminal", "rumble.mega.simulator.title",
                new MarkableObject.DimensionPos(PrologueWorld.GYMS, pos.getX(), pos.getY() + 1, pos.getZ()));
        var actor = MarkSpec.translated("mega_may", MegaIntroduction.NAME,
                new MarkableObject.DimensionPos(PrologueWorld.GYMS, may.getX(), may.getY() + 2, may.getZ()));
        event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE)
                .sortOrder(3).themeColor(SquadQuest.SIDE_THEME).icon(Items.AMETHYST_SHARD).cannotAbandon().canBeAutoTrack(false)
                .requiresQuest(GymStory.thirdGymQuest()).displayName(QuestText.translatable(KEY + "title"))
                .description(QuestText.translatable(KEY + "description"))
                .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(FAREWELL)
                .phase(phase(CRAFT).thenGoTo(TRIAL)).phase(phase(TRIAL).trackingMarker(terminal).thenGoTo(LESSON))
                .phase(phase(LESSON).trackingMarker(actor).thenGoTo(FAREWELL)).phase(phase(FAREWELL).trackingMarker(actor))
                .reward(new CobbleDollarReward(REWARD)).reward(new ItemReward(MayMegaDevice.ITEM.get(), 1)).build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
    }

    private static PhaseBuilder phase(String name) {
        var text = QuestText.translatable(KEY + "phase." + name);
        return PhaseBuilder.create(name).displayName(text).objective(ObjectiveBuilder.custom(ID.withSuffix("/" + name), 1).display(text));
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var ask = tree("lesson");
        line(ask, "ask", DialogueText.DialogueArg.playerName()).choice("yes", text("got_it"), c -> c.goTo("explain"));
        line(ask, "explain").choice("ready", text("ready"), c -> c.addEvents((p, ignored) -> {
            if (atDialogueMay(p) && active(p, LESSON)) PrologueStory.after(p, () -> start(p));
        }).close()).choice("later", text("later"), c -> c.close());
        event.register(ask.build());

        var farewell = tree("farewell");
        line(farewell, "congrats").delay(3550).autoNext("goodbye");
        line(farewell, "goodbye", DialogueText.DialogueArg.playerName()).delay(4100).autoNext("coins");
        farewell.node("coins").say(DialogueText.translatable("rumble.gym.reward.coins", DialogueText.DialogueArg.of(ctx -> REWARD)),
                TREE + "/coins").delay(1550).autoNext("anklet");
        farewell.node("anklet").say(RewardNotice.items(MayMegaDevice.ITEM.get().getDefaultInstance()), TREE + "/anklet")
                .choice("done", DialogueText.translatable("gui.done"), c -> c.addEvents((p, ignored) -> {
                    if (atDialogueMay(p)) advance(p, FAREWELL);
                }).close());
        event.register(farewell.build());

        var idle = tree("idle");
        line(idle, "craft_hint").choice("done", DialogueText.translatable("gui.done"), c -> c.close());
        event.register(idle.build());
        var trial = tree("trial");
        line(trial, "trial_hint").choice("done", DialogueText.translatable("gui.done"), c -> c.close());
        event.register(trial.build());
    }

    private static DialogueTreeBuilder tree(String route) {
        return DialogueTreeBuilder.create(TREE + "/" + route).npc(DialogueText.translatable(MegaIntroduction.NAME)).repeatable(true);
    }

    private static DialogueTreeBuilder line(DialogueTreeBuilder tree, String node, DialogueText.DialogueArg... args) {
        return tree.node(node).say(text(node, args), TREE + "/" + node);
    }

    private static DialogueText text(String line, DialogueText.DialogueArg... args) { return DialogueText.translatable(KEY + line, args); }

    static void accept(ServerPlayer player) {
        if (MegaCombat.available()) QuestProgressHandler.acceptQuest(player, ID.toString());
    }

    private static boolean active(ServerPlayer player, String phase) { return GymStory.phaseActive(player, ID, phase); }

    static boolean visible(ServerPlayer player) {
        return active(player, CRAFT) || active(player, TRIAL) || active(player, LESSON) || active(player, FAREWELL);
    }

    static boolean talk(ServerPlayer player, Entity actor) {
        String route = active(player, FAREWELL) ? "farewell" : active(player, LESSON) ? "lesson"
                : active(player, TRIAL) ? "trial" : active(player, CRAFT) ? "idle" : null;
        if (route == null) return false;
        DialogueSessionManager.INSTANCE.startDialogue(player, actor, TREE + "/" + route, new DialogueContext());
        return true;
    }

    private static boolean atDialogueMay(ServerPlayer player) {
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        return session != null && MegaIntroduction.atMay(player, session.getEntity());
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player, CRAFT) || !MegaCombat.isDevice(event.getCrafting())) return;
        var matrix = event.getInventory();
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            if (BuiltInRegistries.ITEM.getKey(matrix.getItem(i).getItem()).toString().equals(MegaIntroduction.KEYSTONE)) {
                advance(player, CRAFT);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void completed(InstanceEvent.Completed event) {
        var session = event.getSession();
        if (MegaTrials.get(session.getDefinition().id()) == null) return;
        var players = session.getLevel().getServer().getPlayerList();
        for (UUID id : event.getPlayers()) {
            var player = players.getPlayer(id);
            if (player != null) advance(player, TRIAL);
        }
    }

    private static void start(ServerPlayer player) {
        if (!active(player, LESSON) || !MegaIntroduction.atMay(player, MegaIntroduction.actor(player))) return;
        if (!MegaCombat.equipped(player)) { hint(player, "equip_device"); return; }
        for (var pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
            if (pokemon.isFainted() || pokemon.getTetheringId() != null || !MegaCombat.hasStone(pokemon)) continue;
            var lead = pokemon.getUuid();
            ArenaMatch.challengeStory(player, new ArenaMatch.StoryChallenge() {
                @Override public List<GymCatalog.Actor> actors() { return List.of(MAY); }
                @Override public boolean eligible(ServerPlayer p) { return active(p, LESSON) && MegaCombat.equipped(p); }
                @Override public UUID megaLessonLead() { return lead; }
                @Override public void result(ServerPlayer p, boolean won) { if (won) advance(p, LESSON); }
            });
            return;
        }
        hint(player, "equip_stone");
    }

    private static void hint(ServerPlayer player, String key) { player.displayClientMessage(Component.translatable(KEY + key), true); }

    private static void advance(ServerPlayer player, String phase) {
        if (active(player, phase)) QuestProgressHandler.incrementObjective(player, ID.toString(), phase, 0, 1);
    }
}
