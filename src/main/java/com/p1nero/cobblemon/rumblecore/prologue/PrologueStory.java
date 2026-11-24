package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.guide.RumbleGuides;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.integration.rct.RoadTrainerDialogue;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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
import org.arcadia.arc_quest.questmarker.api.MarkSpec;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class PrologueStory {
    public static final String ID = PrologueWorld.id("first_spark").toString();
    static final String WELCOME = "welcome", PARTNER = "partner", BRIEFING = "briefing", DRILLS = "drills", SPAR = "spar";
    private static final Map<ServerPlayer, Runnable> AFTER_DIALOGUE = new WeakHashMap<>();
    private static final Set<ServerPlayer> FAREWELL_OFFERED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String FAREWELL = "farewell";
    public static final String FAREWELL_TREE = PrologueWorld.id("prologue/farewell").toString();
    private static final String SPEAKER = "entity.cobblemon_rumble_core.professor";
    private static final String DIALOGUE_KEY = "rumble.prologue.dialogue.";
    static final MarkSpec PROFESSOR_MARK = HallQuestMarkers.actor("professor", SPEAKER,
            FixedStoryActors.PROFESSOR, PrologueWorld.PROFESSOR);

    private PrologueStory() {}

    @EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Content {
        @SubscribeEvent
        public static void registerQuest(ArcQuestRegistrationEvent.Quest event) {
            event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ARCHON).sortOrder(0)
                    .displayName(QuestText.translatable("rumble.prologue.title"))
                    .description(QuestText.translatable("rumble.prologue.description"))
                    .cannotAbandon().themeColor(ChatFormatting.GOLD)
                    .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(FAREWELL)
                    .phase(phase(WELCOME, "key.cobblemon_rumble_core.talk").trackingMarker(PROFESSOR_MARK).thenGoTo(PARTNER))
                    .phase(RumbleGuides.partnerGrants(phase(PARTNER)).trackingMarker(PROFESSOR_MARK).thenGoTo(BRIEFING))
                    .phase(phase(BRIEFING, "key.cobblemon_rumble_core.talk").trackingMarker(PROFESSOR_MARK).thenGoTo(DRILLS))
                    .phase(phase(DRILLS).thenGoTo(SPAR))
                    .phase(phase(SPAR).grantGuideOnComplete(RumbleGuides.REALTIME).thenGoTo(FAREWELL))
                    .phase(phase(FAREWELL, "key.cobblemon_rumble_core.talk").trackingMarker(PROFESSOR_MARK)).build());
            SquadQuest.register(event);
            SquadQuest.group(event, "champion_road", QuestCategory.ARCHON, 0, ResourceLocation.parse(ID));
            SquadQuest.group(event, "adventure_quests", QuestCategory.ADVENTURE, 2, SquadQuest.ID);
            TrainerGrowth.register(event);
            SimulatorGrowth.register(event);
            GymStory.registerQuests(event);
            ProfessorRestock.register(event);
            PersonalDimension.register(event);
            MegaTrainingQuest.register(event);
            SimulatorTestQuest.register(event);
            CinemaQuest.register(event);
            ChampionStory.register(event);
            TeraChapter.register(event);
            DailyCommissions.register(event);
            SafariChapter.register(event);
            WanderingMerchantQuest.register(event);
            LegendaryCommission.register(event);
        }

        @SubscribeEvent
        public static void registerDialogue(ArcQuestRegistrationEvent.Dialogue event) {
            GymStory.registerDialogue(event);
            RoadTrainerDialogue.register(event);
            FirstBadgeVisit.register(event);
            ProfessorRestock.register(event);
            PersonalDimension.register(event);
            MegaIntroduction.register(event);
            MegaTrainingQuest.register(event);
            SimulatorTestQuest.register(event);
            CinemaQuest.register(event);
            ChampionStory.register(event);
            TeraChapter.register(event);
            SafariChapter.register(event);
            WanderingMerchantQuest.register(event);
            LegendaryCommission.register(event);
            var intro = tree(WELCOME);
            for (int i = 1; i <= 9; i++) {
                String node = "intro_" + i;
                line(intro, node).delay(i <= 2 ? 1000 : 1250).autoNext(i < 9 ? "intro_" + (i + 1) : "intro_bonds");
            }
            line(intro, "intro_bonds").delay(4000).autoNext("name");
            line(intro, "name").choice("your_name", text("your_name", DialogueText.DialogueArg.playerName()),
                    choice -> choice.goTo("trainer"));
            line(intro, "trainer").delay(1300).autoNext("realtime");
            line(intro, "realtime").delay(1900).autoNext("choose");
            intro.node("choose").say(text("choose", DialogueText.DialogueArg.playerName()), rId("choose"))
                    .choice("select", text("select"), choice -> choice.addEvents((player, npc) -> {
                        if (!atDialogueProfessor(player)) return;
                        finish(player, WELCOME);
                        after(player, () -> chooseStarter(player));
                    }).close());
            event.register(intro.build());

            var partner = tree(PARTNER);
            line(partner, "choose_again").choice("select", text("select"), choice -> choice
                    .addEvents((player, npc) -> { if (atDialogueProfessor(player)) after(player, () -> chooseStarter(player)); }).close());
            event.register(partner.build());

            var farewell = tree(FAREWELL);
            farewell.node(FAREWELL).say(text(FAREWELL, DialogueText.DialogueArg.playerName()), rId(FAREWELL))
                    .choice("master", text("ask_master"), choice -> choice.goTo("hall"));
            line(farewell, "hall").delay(8600).autoNext("partners");
            line(farewell, "partners").delay(3000).autoNext("overworld");
            line(farewell, "overworld").choice("world", text("ask_world"), choice -> choice.goTo("wild"));
            line(farewell, "wild").delay(2000).autoNext("strength");
            line(farewell, "strength").delay(4200).autoNext("stunned");
            line(farewell, "stunned").delay(4000).autoNext("catch");
            line(farewell, "catch").delay(4000).autoNext("easy");
            line(farewell, "easy").choice("easy", text("ask_easy"), choice -> choice.goTo("facilities"));
            line(farewell, "facilities").delay(2900).autoNext("healer");
            line(farewell, "healer").delay(4700).autoNext("storage_pc");
            line(farewell, "storage_pc").choice("simulator", text("ask_simulator"), choice -> choice.goTo("simulator"));
            line(farewell, "simulator").delay(5300).autoNext("simulator_frontier");
            line(farewell, "simulator_frontier").delay(4600).autoNext("simulator_multiplayer");
            line(farewell, "simulator_multiplayer").delay(3500).autoNext("depart");
            line(farewell, "depart").choice("depart", text("set_out"), choice -> choice.addEvents((player, npc) -> {
                if (atDialogueProfessor(player)) finishFarewell(player);
            }).goTo("received_catch_kit"));
            farewell.node("received_catch_kit").say(RewardNotice.items(catchKit()), rId("received_catch_kit"))
                    .choice("done", DialogueText.translatable("gui.done"), choice -> choice.close());
            event.register(farewell.build());

            var idle = tree("done").repeatable(true);
            idle.node("done").say(DialogueText.of(ctx -> professorGreeting(ctx.player())), rId("done"))
                    .choice("space_ingot", DialogueText.of(ctx -> Component.translatable("rumble.personal_dimension.about")
                                    .withStyle(ChatFormatting.GOLD)), choice -> choice
                            .conditionIf((p, npc) -> PersonalDimension.trackedDelivery(p))
                            .addEvents((p, npc) -> { if (atDialogueProfessor(p)) after(p, () -> PersonalDimension.open(p, PersonalDimension.ABOUT_ROUTE)); }).close())
                    .choice("restock", ProfessorRestock.option(), choice -> choice
                            .conditionIf((p, npc) -> ProfessorRestock.active(p))
                            .addEvents((p, npc) -> { if (atDialogueProfessor(p)) after(p, () -> ProfessorRestock.open(p)); }).close())
                    .choice("champion_rematch", ChampionStory.gold("rematch"), choice -> choice
                            .conditionIf((p, npc) -> ChampionStory.completed(p))
                            .addEvents((p, npc) -> ChampionStory.challenge(p)).close())
                    .choice("hall_of_fame", DialogueText.translatable("rumble.champion.hall_title"), choice -> choice
                            .conditionIf((p, npc) -> ChampionStory.completed(p))
                            .addEvents((p, npc) -> ChampionStory.rankings(p)).close())
                    .choice("trade", FirstBadgeVisit.text("trade"), choice -> choice
                            .conditionIf((p, npc) -> ArcQuestPlayerManager.getOrCreate(p).hasFlag(ProfessorShop.UNLOCKED))
                            .addEvents((p, npc) -> ProfessorShop.afterDialogue(p, false)).close())
                    .choice("sell", DialogueText.translatable("rumble.bank_prices.sell"), choice -> choice
                            .conditionIf((p, npc) -> ArcQuestPlayerManager.getOrCreate(p).hasFlag(ProfessorShop.UNLOCKED))
                            .addEvents((p, npc) -> ProfessorShop.afterDialogue(p, true)).close())
                    .choice("bye", text("bye"), choice -> choice.close());
            event.register(idle.build());

            for (String route : new String[] {BRIEFING, "retry"}) {
                var dialogue = tree(route).repeatable(true);
                dialogue.node(route).say(text(route, DialogueText.DialogueArg.playerName()), rId(route))
                        .choice("continue", text("begin"), choice -> choice
                                .addEvents((player, npc) -> {
                                    if (atDialogueProfessor(player)) after(player, () -> PrologueTraining.launch(player));
                                }).close());
                event.register(dialogue.build());
            }
        }
    }

    private static PhaseBuilder phase(String phase, String... keys) {
        String prefix = "rumble.prologue.phase." + phase;
        var args = Arrays.stream(keys).map(key -> QuestText.Arg.constant(Component.keybind(key)))
                .toArray(QuestText.Arg[]::new);
        return PhaseBuilder.create(phase).displayName(QuestText.translatable(prefix))
                .objective(ObjectiveBuilder.custom(PrologueWorld.id("prologue/" + phase), 1)
                        .display(QuestText.translatable(prefix + ".objective", args)));
    }

    static DialogueTreeBuilder tree(String path) { return DialogueTreeBuilder.create(rId(path)).npc(DialogueText.translatable(SPEAKER)); }
    static String rId(String path) { return PrologueWorld.id("prologue/" + path).toString(); }
    private static DialogueText text(String path, DialogueText.DialogueArg... args) { return DialogueText.translatable(DIALOGUE_KEY + path, args); }
    private static DialogueTreeBuilder line(DialogueTreeBuilder tree, String path) { return tree.node(path).say(text(path), rId(path)); }

    static String phase(ServerPlayer player) {
        var quest = ArcQuestPlayerManager.getOrCreate(player).getActiveQuest(ID);
        return quest == null ? "" : quest.getCurrentPhaseId();
    }

    static void finish(ServerPlayer player, String phase) {
        if (phase.equals(phase(player))) QuestProgressHandler.incrementObjective(player, ID, phase, 0, 1);
    }

    private static void finishFarewell(ServerPlayer player) {
        if (!FAREWELL.equals(phase(player))) return;
        finish(player, FAREWELL);
        if (!SquadQuest.travelUnlocked(player)) return;
        RewardNotice.grant(player, catchKit());
        PhoneState.sync(player);
        acceptOpeningQuests(player);
        OpeningTutorial.start(player);
    }

    private static void acceptOpeningQuests(ServerPlayer player) {
        GymStory.acceptNext(player);
        TrainerGrowth.accept(player);
        DailyCommissions.refresh(player);
        // The last accepted quest takes focus; leave the player with their first exploration objective.
        SquadQuest.accept(player);
    }

    private static ItemStack[] catchKit() {
        return new ItemStack[] {new ItemStack(CobblemonItems.POKEDEX_RED), new ItemStack(CobblemonItems.ULTRA_BALL, 30)};
    }

    static boolean completed(ServerPlayer player) { return ArcQuestPlayerManager.getOrCreate(player).isQuestCompleted(ID); }
    public static boolean starterAllowed(ServerPlayer player) { return PARTNER.equals(phase(player)) || completed(player); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!data.isQuestActive(ID) && !data.isQuestCompleted(ID)) QuestProgressHandler.acceptQuest(player, ID);
        var starter = starterData(player);
        if (!starter.getStarterSelected()) setStarterLocked(player, !starterAllowed(player));
        if (SquadQuest.travelUnlocked(player)) {
            acceptOpeningQuests(player);
            FirstBadgeVisit.acceptFollowups(player);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        if (player.tickCount % 20 == 0) TrainerGrowth.update(player);
        // Initial position is applied once, after login/recovery hooks have restored the player.
        if (!PrologueWorld.ARRIVED.get(player) && PrologueWorld.arrive(player)) {
            player.setRespawnPosition(player.serverLevel().dimension(), PrologueWorld.ARRIVAL, -135, true, false);
            PrologueWorld.ARRIVED.markTrue(player);
        }
        Runnable next = AFTER_DIALOGUE.get(player);
        if (next != null && !DialogueSessionManager.INSTANCE.isInDialogue(player)) {
            AFTER_DIALOGUE.remove(player);
            next.run();
        }
        if (PARTNER.equals(phase(player)) && starterData(player).getStarterSelected()) {
            // The native chosen event is fired before Cobblemon commits the Pokémon to the party.
            finish(player, PARTNER);
            ProfessorEntity doctor = nearby(player);
            if (doctor != null && !DialogueSessionManager.INSTANCE.isInDialogue(player)) open(player, doctor, BRIEFING);
        }
        if (player.tickCount % 10 != 0 || player.level().dimension() != PrologueWorld.GYMS
                || PrologueTraining.launching(player) || !FAREWELL.equals(phase(player))) return;
        if (player.containerMenu == player.inventoryMenu
                && !DialogueSessionManager.INSTANCE.isInDialogue(player) && !FAREWELL_OFFERED.contains(player)) {
            ProfessorEntity doctor = nearby(player);
            if (doctor != null && FAREWELL_OFFERED.add(player)) open(player, doctor, FAREWELL);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AFTER_DIALOGUE.remove(player);
            FAREWELL_OFFERED.remove(player);
        }
    }

    static boolean at(ServerPlayer player, Entity npc) {
        return npc instanceof ProfessorEntity && FixedStoryActors.PROFESSOR.equals(FixedStoryActors.role(npc)) && npc.isAlive() && npc.level() == player.level()
                && player.isAlive() && !player.isSpectator() && player.serverLevel().dimension().equals(PrologueWorld.GYMS)
                && player.distanceToSqr(npc) <= ProfessorEntity.TALK_DISTANCE * ProfessorEntity.TALK_DISTANCE && player.hasLineOfSight(npc);
    }

    static boolean atDialogueProfessor(ServerPlayer player) {
        // ArcQuest's LambdaAction does not populate its NPC argument from the current session.
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        return session != null && at(player, session.getEntity());
    }

    static ProfessorEntity nearby(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(ProfessorEntity.class,
                player.getBoundingBox().inflate(ProfessorEntity.TALK_DISTANCE), doctor -> at(player, doctor))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    static void talk(ServerPlayer player, int id) {
        if (DialogueSessionManager.INSTANCE.isInDialogue(player) || PrologueTraining.launching(player)) return;
        var entity = player.serverLevel().getEntity(id);
        if (!DialogueTarget.aimedAt(player, entity)) return;
        if (WanderingMerchantQuest.talk(player, entity)) return;
        if (MegaIntroduction.talk(player, entity)) return;
        if (GymStory.talk(player, entity)) return;
        if (!(entity instanceof ProfessorEntity doctor) || !at(player, doctor)) return;
        String phase = phase(player);
        String championRoute = ChampionStory.trackedRoute(player);
        String route = switch (phase) {
            case WELCOME, PARTNER, BRIEFING, FAREWELL -> phase;
            case DRILLS, SPAR -> "retry";
            default -> championRoute != null ? championRoute
                    : FirstBadgeVisit.active(player) ? FirstBadgeVisit.ROUTE : PersonalDimension.briefingActive(player)
                    ? PersonalDimension.BRIEFING_ROUTE : SafariChapter.active(player) ? SafariChapter.ROUTE : completed(player) ? "done" : null;
        };
        if ("done".equals(route)) {
            var simulator = SimulatorTestQuest.professorRoute(player);
            championRoute = ChampionStory.activeRoute(player);
            if (championRoute != null) route = championRoute;
            else if (CinemaQuest.meeting(player)) route = CinemaQuest.ROUTE;
            else if (simulator != null) route = simulator;
        }
        if (FAREWELL.equals(route)) FAREWELL_OFFERED.add(player);
        if (route != null) open(player, doctor, route);
    }

    static void open(ServerPlayer player, ProfessorEntity doctor, String route) {
        DialogueSessionManager.INSTANCE.startDialogue(player, doctor, rId(route), new DialogueContext());
    }

    private static Component professorGreeting(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        String tracked = data.getTrackedQuestId();
        String stage;
        if (ChampionStory.completed(player)) stage = "champion";
        else if (CinemaQuest.ID.toString().equals(tracked) && CinemaQuest.testing(player)) stage = "cinema_test";
        else if (PersonalDimension.trackedDelivery(player)) stage = "space_ingot";
        else if (ProfessorRestock.ID.toString().equals(tracked) && ProfessorRestock.active(player)) stage = "restock";
        else if (data.isQuestCompleted(GymStory.eighthGymQuest().toString())) stage = "champion_ready";
        else if (data.isQuestCompleted(GymStory.seventhGymQuest().toString())) stage = "final_badge";
        else if (data.isQuestCompleted(GymStory.sixthGymQuest().toString())) stage = "legendary";
        else if (data.isQuestCompleted(GymStory.fifthGymQuest().toString())) stage = "sixth";
        else if (data.isQuestCompleted(GymStory.fourthGymQuest().toString())) stage = "fifth";
        else if (data.isQuestCompleted(GymStory.thirdGymQuest().toString())) stage = "fourth";
        else if (data.isQuestCompleted(GymStory.secondGymQuest().toString())) stage = "third";
        else if (data.isQuestCompleted(GymStory.firstGymQuest().toString())) stage = "second";
        else if (data.isQuestCompleted(SquadQuest.ID.toString())) stage = "first_gym";
        else stage = "first_team";
        return Component.translatable(DIALOGUE_KEY + "idle." + stage, player.getGameProfile().getName());
    }

    static void after(ServerPlayer player, Runnable action) { AFTER_DIALOGUE.put(player, action); }

    private static void chooseStarter(ServerPlayer player) {
        if (!PARTNER.equals(phase(player)) || nearby(player) == null || starterData(player).getStarterSelected()) return;
        setStarterLocked(player, false);
        Cobblemon.INSTANCE.getStarterHandler().requestStarterChoice(player);
    }

    private static GeneralPlayerData starterData(ServerPlayer player) {
        return Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player);
    }

    private static void setStarterLocked(ServerPlayer player, boolean locked) {
        var data = starterData(player);
        if (data.getStarterLocked() == locked) return;
        data.setStarterLocked(locked);
        Cobblemon.INSTANCE.getPlayerDataManager().saveSingle(data, PlayerInstancedDataStoreTypes.INSTANCE.getGENERAL());
        data.sendToPlayer(player);
    }
}
