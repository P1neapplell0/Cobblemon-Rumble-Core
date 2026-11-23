package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.Cobblemon;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTheme;
import com.p1nero.cobblemon.rumblecore.combat.arena.TeraCombat;
import com.p1nero.cobblemon.rumblecore.guide.RumbleGuides;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.gym.GymTeamPokemon;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.dialogue.DialogueEndedEvent;
import org.arcadia.arc_quest.api.event.dialogue.DialogueNodeStartedEvent;
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

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class TeraChapter {
    private static final String KEY = "rumble.tera.";
    public static final String TREE = PrologueWorld.id("story/tera_training").toString();
    private static final String REMATCH = PrologueWorld.id("gym/ground_leader/rematch").toString();
    private static final ResourceLocation ID = PrologueWorld.id("paldean_power");
    private static final String SHOW = "show_orb", LESSON = "train_tera", DEBRIEF = "report_to_giovanni";
    private static final int COINS = 40_000;
    private static final String POUCH = "mega_showdown:tera_pouch_black", SHARD = "mega_showdown:dark_tera_shard";
    private static final GymCatalog.Actor GIOVANNI = GymCatalog.leader(GymCatalog.Gym.GROUND).actors().getFirst();
    static final GymCatalog.Actor CASSIDY = new GymCatalog.Actor("tera_cassidy", GIOVANNI.pos().north(2), GymTeamPokemon::cassidy);
    static final GymCatalog.Actor BUTCH = new GymCatalog.Actor("tera_butch", GIOVANNI.pos().south(2), GymTeamPokemon::butch);
    private static final List<GymCatalog.Actor> TEAM_ROCKET = List.of(CASSIDY, BUTCH);
    public static final String CASSIDY_NAME = CASSIDY.key(), BUTCH_NAME = BUTCH.key();
    private record Scene(UUID session) {}
    private static final Map<ServerPlayer, Scene> AUDIENCE = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> RETURN_AT = new WeakHashMap<>();
    private static final Map<ServerPlayer, Launch> LAUNCH = new WeakHashMap<>();
    private record Launch(UUID speaker, int expires) {}

    private TeraChapter() {}

    public static void offer(ServerPlayer player) {
        if (TeraCombat.available()) QuestProgressHandler.acceptQuest(player, ID.toString());
    }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var mark = HallQuestMarkers.actor("tera_giovanni", GIOVANNI.key(), GIOVANNI.id(), GIOVANNI.pos());
        event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE).themeColor(SquadQuest.SIDE_THEME)
                .sortOrder(6).icon(RewardNotice.stack(TeraCombat.ORB.toString(), 1).getItem()).cannotAbandon()
                .requiresQuest(GymStory.fifthGymQuest()).displayName(QuestText.translatable(KEY + "title"))
                .description(QuestText.translatable(KEY + "description"))
                .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(DEBRIEF)
                .phase(phase(SHOW).trackingMarker(mark).grantGuideOnComplete(RumbleGuides.TERA).thenGoTo(LESSON))
                .phase(phase(LESSON).trackingMarker(mark).thenGoTo(DEBRIEF)).phase(phase(DEBRIEF).trackingMarker(mark))
                .reward(new CobbleDollarReward(COINS)).reward(new ItemReward(RewardNotice.stack(POUCH, 1).getItem(), 1))
                .reward(new ItemReward(RewardNotice.stack(SHARD, 1).getItem(), 50)).build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
    }

    private static PhaseBuilder phase(String name) {
        return PhaseBuilder.create(name).displayName(QuestText.translatable(KEY + "phase." + name))
                .objective(ObjectiveBuilder.custom(ID.withSuffix("/" + name), 1).display(QuestText.translatable(KEY + "objective." + name)));
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var outro = tree("victory");
        addDebrief(outro);
        event.register(outro.build());
    }

    public static void addChoices(DialogueTreeBuilder rematch) {
        var about = DialogueText.of(ctx -> Component.translatable(KEY + "about").withStyle(net.minecraft.ChatFormatting.GOLD));
        rematch.choice("tera_missing", about, c -> c.conditionIf((p, npc) -> canAsk(p, npc)
                        && active(p, SHOW) && !hasOrb(p)).goTo("missing"))
                .choice("tera", about, c -> c.conditionIf((p, npc) -> canAsk(p, npc)
                                && (active(p, LESSON) || active(p, SHOW) && hasOrb(p)))
                        .addEvents((p, ignored) -> advance(p, SHOW)).goTo("recognize"))
                .choice("tera_debrief", about, c -> c.conditionIf((p, npc) -> canAsk(p, npc)
                        && active(p, DEBRIEF)).goTo("duo_retreat"))
                .choice("later", DialogueText.translatable("rumble.gym.choice.later"), c -> c.close());
        say(rematch, "missing").choice("done", DialogueText.translatable("gui.done"), c -> c.close());
        addLesson(rematch);
        addDebrief(rematch);
    }

    private static boolean hasOrb(ServerPlayer player) {
        return TeraCombat.equipped(player) || player.getInventory().items.stream().anyMatch(TeraCombat::isDevice);
    }

    private static void addLesson(DialogueTreeBuilder intro) {
        say(intro, "recognize").delay(1700).autoNext("explain");
        say(intro, "explain", DialogueText.DialogueArg.of(ctx -> Component.keybind(TeraCombat.BINDING)))
                .delay(5500).autoNext("check");
        say(intro, "check").choice("equip", text("equip"), c -> c.conditionIf((p, npc) -> !TeraCombat.equipped(p)).close())
                .choice("ready", text("ready"), c -> c.conditionIf((p, npc) -> TeraCombat.equipped(p)).goTo("introduce"));
        say(intro, "introduce").choice("who", text("who"), c -> c.goTo("cassidy_1"));
        var motto = List.of("cassidy_1", "butch_1", "cassidy_2", "butch_2", "cassidy_3", "butch_3",
                "cassidy_4", "cassidy_5", "butch_4", "cassidy_6", "butch_5");
        for (int i = 0; i < motto.size() - 1; i++) {
            var node = motto.get(i);
            intro.node(node).speaker(DialogueText.translatable(node.startsWith("cassidy_") ? CASSIDY_NAME : BUTCH_NAME))
                    .say(text(node), TREE + "/" + node).delay(i == 7 || i == 8 ? 1500 : 2900).autoNext(motto.get(i + 1));
        }
        intro.node("butch_5").speaker(DialogueText.translatable(BUTCH_NAME)).say(text("butch_5"), TREE + "/butch_5")
                .choice("battle", text("battle"), c -> c.addEvents((p, ignored) -> {
                    var session = DialogueSessionManager.INSTANCE.getSession(p);
                    var speaker = session == null ? null : session.getEntity();
                    if (atGiovanni(p, speaker)) LAUNCH.put(p, new Launch(speaker.getUUID(), p.server.getTickCount() + 100));
                    else tell(p, "start_unavailable");
                }).close()).choice("later", DialogueText.translatable("rumble.gym.choice.later"), c -> c.close());
    }

    private static void addDebrief(DialogueTreeBuilder outro) {
        outro.node("duo_retreat").speaker(text("duo")).say(text("duo_retreat"), TREE + "/duo_retreat")
                .delay(2300).autoNext("congrats");
        say(outro, "congrats").delay(3000).autoNext("farewell");
        say(outro, "farewell").delay(2900).autoNext("coins");
        outro.node("coins").say(DialogueText.translatable("rumble.gym.reward.coins", DialogueText.DialogueArg.of(ctx -> COINS)),
                TREE + "/coins").delay(1600).autoNext("items");
        outro.node("items").say(RewardNotice.items(RewardNotice.stack(POUCH, 1), RewardNotice.stack(SHARD, 50)), TREE + "/items")
                .choice("done", DialogueText.translatable("gui.done"), c -> c.addEvents((p, ignored) -> {
                    advance(p, DEBRIEF);
                }).close());
    }

    private static DialogueTreeBuilder tree(String route) {
        return DialogueTreeBuilder.create(TREE + "/" + route).npc(DialogueText.translatable(GIOVANNI.key())).repeatable(true);
    }
    private static DialogueTreeBuilder say(DialogueTreeBuilder tree, String node, DialogueText.DialogueArg... args) {
        return tree.node(node).say(text(node, args), TREE + "/" + node);
    }
    public static DialogueText text(String line, DialogueText.DialogueArg... args) { return DialogueText.translatable(KEY + line, args); }

    public static boolean canAsk(ServerPlayer p, Entity npc) {
        return TeraCombat.available() && atGiovanni(p, npc) && (active(p, SHOW) || active(p, LESSON) || active(p, DEBRIEF));
    }


    private static void start(ServerPlayer p, Launch launch) {
        if (!atGiovanni(p, p.serverLevel().getEntity(launch.speaker())) || !ArenaMatch.available(p)) {
            tell(p, "start_unavailable");
            return;
        }
        if (!TeraCombat.equipped(p)) {
            tell(p, "need_equipped");
            return;
        }
        // Reconcile a pending handover before validating the deferred launch.
        if (active(p, SHOW)) advance(p, SHOW);
        if (!active(p, LESSON)) {
            tell(p, "start_unavailable");
            return;
        }
        for (var mon : Cobblemon.INSTANCE.getStorage().getParty(p).toGappyList()) {
            if (mon == null || mon.isFainted() || mon.getTetheringId() != null || mon.getTeraType() == null) continue;
            var lead = mon.getUuid();
            boolean started = ArenaMatch.challengeStory(p, new ArenaMatch.StoryChallenge() {
                @Override public List<GymCatalog.Actor> actors() { return TEAM_ROCKET; }
                @Override public UUID teraLessonLead() { return lead; }
                @Override public ArenaTheme arena(ServerPlayer player) { return ArenaTheme.gym(GymCatalog.Gym.GROUND.path()); }
                @Override public boolean eligible(ServerPlayer player) { return active(player, LESSON) && TeraCombat.equipped(player); }
                @Override public void result(ServerPlayer player, boolean won) {
                    if (!won) return;
                    advance(player, LESSON);
                    if (active(player, DEBRIEF)) RETURN_AT.put(player, player.server.getTickCount() + 10);
                }
            });
            if (!started) tell(p, "start_unavailable");
            return;
        }
        tell(p, "need_partner");
    }

    private static void tell(ServerPlayer p, String key) { p.displayClientMessage(Component.translatable(KEY + key), true); }

    @SubscribeEvent
    public static void beat(DialogueNodeStartedEvent event) {
        if (!sceneDialogue(event.getDialogueId())) return;
        var p = event.getPlayer();
        if ("congrats".equals(event.getNodeId())) {
            if (AUDIENCE.remove(p) != null) smoke(p, true);
        } else if ("cassidy_1".equals(event.getNodeId()) || "duo_retreat".equals(event.getNodeId())) {
            var session = DialogueSessionManager.INSTANCE.getSession(p);
            if (session != null && atGiovanni(p, event.getNpc()) && AUDIENCE.putIfAbsent(p, new Scene(session.getSessionId())) == null
                    && "cassidy_1".equals(event.getNodeId())) smoke(p, false);
        }
    }

    static boolean visible(ServerPlayer p) {
        var scene = AUDIENCE.get(p);
        var session = DialogueSessionManager.INSTANCE.getSession(p);
        return scene != null && session != null && scene.session().equals(session.getSessionId()) && atGiovanni(p, session.getEntity());
    }

    private static void smoke(ServerPlayer p, boolean retreat) {
        var level = p.serverLevel();
        for (var def : TEAM_ROCKET) {
            var actor = FixedStoryActors.find(level, def.id());
            if (actor != null) level.sendParticles(p, ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    actor.getX(), actor.getY() + .6, actor.getZ(), 15, .32, .33, .32, .024);
        }
        p.playNotifySound(SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, .52f, retreat ? .83f : 1.15f);
    }

    @SubscribeEvent
    public static void dialogueEnded(DialogueEndedEvent event) {
        if (sceneDialogue(event.getDialogueId())) AUDIENCE.remove(event.getPlayer());
    }

    public static boolean sceneDialogue(String id) { return REMATCH.equals(id) || id.startsWith(TREE + "/"); }

    @SubscribeEvent
    public static void returnToGiovanni(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer at = RETURN_AT.get(player);
        int now = player.server.getTickCount();
        var launch = LAUNCH.get(player);
        if (launch != null) {
            if (now >= launch.expires()) {
                LAUNCH.remove(player);
                tell(player, "start_unavailable");
            } else if (!DialogueSessionManager.INSTANCE.isInDialogue(player)) {
                LAUNCH.remove(player);
                start(player, launch);
            }
        }
        if (at == null || now < at) return;
        if (!active(player, DEBRIEF) || now >= at + 160) { RETURN_AT.remove(player); return; }
        var actor = FixedStoryActors.find(player.serverLevel(), GIOVANNI.id());
        if (atGiovanni(player, actor) && !DialogueSessionManager.INSTANCE.isInDialogue(player)) {
            RETURN_AT.remove(player);
            DialogueSessionManager.INSTANCE.startDialogue(player, actor, TREE + "/victory", new DialogueContext());
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { AUDIENCE.remove(p); RETURN_AT.remove(p); LAUNCH.remove(p); }
    }

    private static boolean atGiovanni(ServerPlayer p, Entity npc) {
        return npc != null && npc.isAlive() && npc.level() == p.level() && PrologueWorld.GYMS.equals(p.level().dimension())
                && GIOVANNI.id().equals(FixedStoryActors.role(npc)) && p.distanceToSqr(npc) <= 8 * 8;
    }
    private static boolean active(ServerPlayer p, String phase) { return GymStory.phaseActive(p, ID, phase); }
    private static void advance(ServerPlayer p, String phase) {
        if (active(p, phase)) QuestProgressHandler.incrementObjective(p, ID.toString(), phase, 0, 1);
    }
}
