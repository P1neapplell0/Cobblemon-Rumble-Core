package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.guide.RumbleGuides;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog.Actor;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog.Encounter;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog.Gym;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.p1nero.cobblemon.rumblecore.prologue.ChampionStory;
import com.p1nero.cobblemon.rumblecore.prologue.CinemaQuest;
import com.p1nero.cobblemon.rumblecore.prologue.FirstBadgeVisit;
import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import com.p1nero.cobblemon.rumblecore.prologue.HallQuestMarkers;
import com.p1nero.cobblemon.rumblecore.prologue.LegendaryCommission;
import com.p1nero.cobblemon.rumblecore.prologue.MegaIntroduction;
import com.p1nero.cobblemon.rumblecore.prologue.PersonalDimension;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueStory;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.cobblemon.rumblecore.prologue.SafariChapter;
import com.p1nero.cobblemon.rumblecore.prologue.SimulatorTestQuest;
import com.p1nero.cobblemon.rumblecore.prologue.TeraChapter;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueContext;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import org.arcadia.arc_quest.guide.runtime.GuideUnlockService;
import org.arcadia.arc_quest.quest.api.ICondition;
import org.arcadia.arc_quest.quest.api.IReward;
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
public final class GymStory {
    private static final String GARDENIA_GIFT = "rumble.gardenia.gift";
    private static final String GARDENIA_DONE = "rumble.gardenia.done";
    private static final String PREFIX = "rumble.gym.", ARRIVE = "arrive", DEBRIEF = "leader_debrief";
    private static final ResourceLocation TRAINER_MODEL = PrologueWorld.id("gym_trainer");
    private static final Map<ServerPlayer, Pending> PENDING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Result> RESULTS = new WeakHashMap<>();
    private static final List<BlockPos> ENTRANCES = List.of(new BlockPos(-5, 52, -139), new BlockPos(-98, 47, -114),
            new BlockPos(-145, 52, 4), new BlockPos(-116, 57, 116), new BlockPos(-5, 50, 138),
            new BlockPos(102, 55, 129), new BlockPos(125, 61, 10), new BlockPos(92, 56, -107));
    private static final List<String> BADGES = List.of("rock", "grass", "ghost", "ice", "mountain", "fire", "dark", "thunder");
    private static final int[] LEADER_PAYOUTS = {4_000, 8_000, 12_000, 16_000, 20_000, 30_000, 40_000, 60_000};
    private record Pending(Encounter encounter, int speaker, int expires) {}
    private record Result(Encounter encounter, UUID speaker, boolean won, boolean rewarded, int earliest, int expires) {}

    private GymStory() {}

    private static ResourceLocation quest(Gym gym) { return PrologueWorld.id("trial_" + gym.path()); }
    private static String prerequisite(Gym gym) {
        return switch (gym) {
            case ROCK -> PrologueStory.ID;
            default -> quest(Gym.values()[gym.ordinal() - 1]).toString();
        };
    }
    public static boolean isGymQuest(String id) {
        for (var gym : Gym.values()) if (quest(gym).toString().equals(id)) return true;
        return false;
    }
    public static ResourceLocation firstGymQuest() { return quest(Gym.ROCK); }
    public static ResourceLocation secondGymQuest() { return quest(Gym.GRASS); }
    public static ResourceLocation thirdGymQuest() { return quest(Gym.GHOST); }
    public static ResourceLocation fourthGymQuest() { return quest(Gym.ICE); }
    public static ResourceLocation fifthGymQuest() { return quest(Gym.GROUND); }
    public static int badgeCount(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        int badges = 0;
        for (Gym gym : Gym.values()) if (data.hasFlag(flag(GymCatalog.leader(gym)))) badges++;
        return badges;
    }
    private static String flag(Encounter encounter) { return PREFIX + encounter.id() + ".cleared"; }
    private static String tree(Actor actor) { return PrologueWorld.id("gym/" + actor.id()).toString(); }
    private static String resultTree(Actor actor, String outcome) { return tree(actor) + "/" + outcome; }

    private static int payout(Encounter encounter) {
        int base = LEADER_PAYOUTS[encounter.gym().ordinal()];
        // A pair of attendants shares one encounter and pays out once.
        return encounter.leader() ? base : base / 4;
    }

    private static ResourceLocation badge(Gym gym) {
        return ResourceLocation.fromNamespaceAndPath("badgebox", BADGES.get(gym.ordinal()) + "_badge");
    }

    private static PhaseBuilder followup(Gym gym) {
        return switch (gym) {
            case ROCK -> FirstBadgeVisit.phase();
            case GRASS -> PersonalDimension.briefingPhase();
            case GHOST -> MegaIntroduction.phase();
            case ICE -> SafariChapter.briefing();
            default -> null;
        };
    }

    public static ResourceLocation sixthGymQuest() { return quest(Gym.FIRE); }
    public static ResourceLocation seventhGymQuest() { return quest(Gym.DARK); }

    public static boolean phaseActive(ServerPlayer player, ResourceLocation quest, String phase) {
        var active = ArcQuestPlayerManager.getOrCreate(player).getActiveQuest(quest.toString());
        return active != null && active.isPhaseActive(phase);
    }

    public static void registerQuests(ArcQuestRegistrationEvent.Quest event) {
        for (Gym gym : Gym.values()) {
            var id = quest(gym);
            var encounters = GymCatalog.forGym(gym);
            var leader = encounters.getLast();
            var followup = followup(gym);
            boolean debriefRequired = followup != null || gym == Gym.GROUND || gym == Gym.DARK;
            String followupId = switch (gym) {
                case ROCK -> FirstBadgeVisit.ROUTE;
                case GRASS -> PersonalDimension.BRIEFING_ROUTE;
                case GHOST -> MegaIntroduction.ROUTE;
                case ICE -> SafariChapter.ROUTE;
                case GROUND, DARK -> DEBRIEF;
                default -> leader.id();
            };
            var entrance = ENTRANCES.get(gym.ordinal());
            var arrival = PhaseBuilder.create(ARRIVE).displayName(QuestText.translatable(PREFIX + "stage.arrive"))
                    .objective(ObjectiveBuilder.custom(PrologueWorld.id("gym/" + gym.path() + "/arrive"), 1)
                            .display(QuestText.translatable(PREFIX + "stage.arrive.objective")))
                    .trackingMarker(marker(gym.path(), PREFIX + gym.path() + ".title", entrance));
            var builder = QuestBuilder.create(id).category(QuestCategory.ARCHON).sortOrder(gym.ordinal() + 1)
                    .displayName(QuestText.translatable(PREFIX + gym.path() + ".title"))
                    .description(QuestText.translatable(PREFIX + gym.path() + ".description"))
                    .icon(Items.COMPASS).themeColor(ChatFormatting.GOLD).cannotAbandon()
                    .requiresQuest(prerequisite(gym))
                    .chapterStartSound(CobblemonSounds.LEVELUP_START).chapterCompleteSound(CobblemonSounds.LEVELUP)
                    .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(followupId);
            for (var e : encounters) if (!e.leader()) arrival.thenGoTo(e.id());
            builder.phase(arrival);
            ICondition attendantsCleared = null;
            for (var e : encounters) {
                var first = e.actors().getFirst();
                Component name = e.actors().size() == 1 ? first.name() : Component.translatable(PREFIX + "duo", first.name(), e.actors().getLast().name());
                var args = QuestText.Arg.constant(name);
                var phase = PhaseBuilder.create(e.id()).displayName(QuestText.translatable(PREFIX + "stage." + (e.leader() ? "leader" : "attendant"), args))
                        .trackingMarker(HallQuestMarkers.actor(e.id(), first.key(), first.id(), first.pos()))
                        .objective(ObjectiveBuilder.custom(PrologueWorld.id("gym/" + e.id()), 1)
                                .display(QuestText.translatable(PREFIX + "stage.challenge.objective", args)));
                if (e.leader()) {
                    phase.enterWhen(attendantsCleared, false).setFlagOnComplete(flag(e))
                            .reward(new BadgeReward(badge(gym))).reward(new CobbleDollarReward(payout(e)));
                    if (debriefRequired) phase.thenGoTo(DEBRIEF);
                } else {
                    phase.setFlagOnComplete(flag(e)).reward(new CobbleDollarReward(payout(e))).thenGoTo(leader.id());
                    ICondition defeated = ICondition.flagSet(flag(e));
                    attendantsCleared = attendantsCleared == null ? defeated : attendantsCleared.and(defeated);
                }
                builder.phase(phase);
            }
            if (debriefRequired) {
                var actor = leader.actors().getFirst();
                var debrief = PhaseBuilder.create(DEBRIEF).displayName(QuestText.translatable(PREFIX + "stage.debrief",
                                QuestText.Arg.constant(actor.name())))
                        .objective(ObjectiveBuilder.custom(PrologueWorld.id("gym/" + gym.path() + "/debrief"), 1)
                                .display(QuestText.translatable(PREFIX + "stage.debrief", QuestText.Arg.constant(actor.name()))))
                        .trackingMarker(HallQuestMarkers.actor(DEBRIEF, actor.key(), actor.id(), actor.pos()));
                if (followup != null) debrief.thenGoTo(followupId);
                builder.phase(debrief);
                if (followup != null) builder.phase(followup);
            }
            event.register(builder.build());
            event.assignQuestToGroup(id, PrologueWorld.id("champion_road"));
        }
    }

    private static MarkSpec marker(String id, String key, BlockPos at) {
        return MarkSpec.translated(id, key, new MarkableObject.DimensionPos(PrologueWorld.GYMS, at.getX(), at.getY() + 2, at.getZ()));
    }

    private record BadgeReward(ResourceLocation id) implements IReward {
        @Override public void grant(ServerPlayer player) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
                var stack = new ItemStack(item);
                if (!player.addItem(stack)) player.drop(stack, false);
            });
        }
        @Override public String describe() { return BuiltInRegistries.ITEM.getOptional(id).map(i -> i.getDescription().getString()).orElse(""); }
    }

    private static DialogueText trainerLine(Actor actor, String suffix) {
        return DialogueText.translatable(actor.key() + suffix, DialogueText.DialogueArg.playerName());
    }

    public static void registerDialogue(ArcQuestRegistrationEvent.Dialogue event) {
        for (var encounter : GymCatalog.ALL) for (var speaker : encounter.actors()) {
            var id = tree(speaker);
            var builder = DialogueTreeBuilder.create(id).npc(DialogueText.translatable(speaker.key())).repeatable(true);
            var others = encounter.actors().stream().filter(actor -> actor != speaker).toList();
            builder.node("start").say(trainerLine(speaker, ".intro"), id + "/start");
            if (!others.isEmpty()) {
                var second = others.getFirst();
                builder.delay(2900).autoNext("partner").node("partner").speaker(DialogueText.translatable(second.key()))
                        .say(trainerLine(second, ".intro"), id + "/partner");
            }
            builder.choice("battle", DialogueText.translatable(PREFIX + "choice.challenge"), choice -> choice.addEvents((player, ignored) -> {
                var session = DialogueSessionManager.INSTANCE.getSession(player);
                if (session == null) return;
                var npc = session.getEntity();
                var match = matching(player, npc);
                if (match == encounter && canChallenge(player, encounter)) {
                    PENDING.put(player, new Pending(encounter, npc.getId(), player.server.getTickCount() + 100));
                }
            }).close()).choice("later", DialogueText.translatable(PREFIX + "choice.later"), choice -> choice.close());
            event.register(builder.build());
            var idle = DialogueTreeBuilder.create(id + "/idle").npc(DialogueText.translatable(speaker.key())).repeatable(true);
            idle.node("start").say(DialogueText.of(ctx -> Component.translatable((String) ctx.vars().getOrDefault("idleKey", PREFIX + "idle.wait"))), id + "/idle/start")
                    .choice("done", DialogueText.translatable("gui.done"), choice -> choice.close());
            event.register(idle.build());
            if (encounter.leader()) {
                var rematch = DialogueTreeBuilder.create(id + "/rematch").npc(DialogueText.translatable(speaker.key())).repeatable(true);
                rematch.node("start").say(DialogueText.translatable(speaker.key() + ".rematch"), id + "/rematch/start")
                        .choice("battle", DialogueText.translatable(PREFIX + "choice.rematch"), choice -> choice.addEvents((player, ignored) -> {
                            var session = DialogueSessionManager.INSTANCE.getSession(player);
                            var npc = session == null ? null : session.getEntity();
                            if (matching(player, npc) == encounter && canChallenge(player, encounter))
                                PENDING.put(player, new Pending(encounter, npc.getId(), player.server.getTickCount() + 100));
                        }).close());
                if (encounter.gym() == Gym.GROUND) TeraChapter.addChoices(rematch);
                else rematch.choice("later", DialogueText.translatable(PREFIX + "choice.later"), choice -> choice.close());
                event.register(rematch.build());
            }
            for (String outcome : List.of("win", "win_reward", "lose")) {
                var result = resultTree(speaker, outcome);
                var suffix = outcome.equals("lose") ? ".lose" : ".win";
                var lines = DialogueTreeBuilder.create(result).npc(DialogueText.translatable(speaker.key())).repeatable(true);
                lines.node("start").say(trainerLine(speaker, suffix), result + "/start");
                if (!others.isEmpty()) {
                    var second = others.getFirst();
                    lines.delay(2800).autoNext("partner").node("partner").speaker(DialogueText.translatable(second.key()))
                            .say(trainerLine(second, suffix), result + "/partner");
                }
                if (outcome.equals("win_reward")) {
                    lines.delay(2400).autoNext("coins").node("coins").say(DialogueText.translatable(
                            PREFIX + "reward.coins", DialogueText.DialogueArg.of(context -> payout(encounter))), result + "/coins");
                    if (encounter.leader()) {
                        lines.delay(1900).autoNext("badge").node("badge").say(RewardNotice.items(
                                RewardNotice.stack(badge(encounter.gym()).toString(), 1)), result + "/badge");
                    }
                }
                if (outcome.equals("win_reward") && gardenia(encounter)) {
                    lines.delay(2000).autoNext("gift_intro");
                    giftDialogue(lines, encounter, result);
                } else if (outcome.equals("win_reward") && encounter.leader() && encounter.gym() == Gym.ICE) {
                    lines.delay(1900).autoNext("stop");
                    SafariChapter.iceGift(lines, result, p -> finishDebrief(p, encounter));
                } else if (outcome.equals("win_reward") && encounter.leader() && encounter.gym() == Gym.GROUND) {
                    lines.delay(1900).autoNext("paldea").node("paldea").say(TeraChapter.text("paldea"), result + "/paldea")
                            .delay(3300).autoNext("show_orb").node("show_orb").say(TeraChapter.text("show_orb"), result + "/show_orb")
                            .choice("done", DialogueText.translatable("gui.done"), c -> c.addEvents((p, npc) -> finishDebrief(p, encounter)).close());
                } else lines.choice("done", DialogueText.translatable("gui.done"), choice -> choice
                        .addEvents((p, npc) -> { if (outcome.equals("win_reward")) finishDebrief(p, encounter); }).close());
                event.register(lines.build());
            }
            if (gardenia(encounter)) {
                var idGift = tree(speaker) + "/gift";
                var lines = DialogueTreeBuilder.create(idGift).npc(DialogueText.translatable(speaker.key())).repeatable(true);
                giftDialogue(lines, encounter, idGift);
                event.register(lines.build());
                var instructions = DialogueTreeBuilder.create(idGift + "/instructions").npc(DialogueText.translatable(speaker.key())).repeatable(true);
                giftInstructions(instructions, encounter, idGift + "/instructions");
                event.register(instructions.build());
            }
        }
    }

    private static boolean gardenia(Encounter encounter) { return encounter.leader() && encounter.gym() == Gym.GRASS; }

    private static void giftDialogue(DialogueTreeBuilder lines, Encounter encounter, String id) {
        lines.node("gift_intro").say(DialogueText.translatable("rumble.gardenia.intro"), id + "/gift_intro").delay(3800).autoNext("gift_offer");
        lines.node("gift_offer").say(DialogueText.translatable("rumble.gardenia.offer"), id + "/gift_offer")
                .choice("receive", DialogueText.translatable("rumble.first_badge.take"), choice -> choice
                        .conditionIf((p, npc) -> eligibleGift(p, encounter) && !ArcQuestPlayerManager.getOrCreate(p).hasFlag(GARDENIA_GIFT))
                        .addEvents((p, npc) -> giveGardeniaGift(p, encounter)).goTo("gift_received"));
        lines.node("gift_received").say(RewardNotice.items(gardeniaRewards()), id + "/gift_received")
                .delay(3100).autoNext("healing");
        giftInstructions(lines, encounter, id);
    }

    private static void giftInstructions(DialogueTreeBuilder lines, Encounter encounter, String id) {
        lines.node("healing").say(DialogueText.translatable("rumble.gardenia.healing",
                DialogueText.DialogueArg.of(ctx -> Component.keybind("key.use"))), id + "/healing").delay(6250).autoNext("pasture");
        lines.node("pasture").say(DialogueText.translatable("rumble.gardenia.pasture",
                DialogueText.DialogueArg.of(ctx -> Component.keybind("key.use")),
                DialogueText.DialogueArg.of(ctx -> Component.keybind("key.sneak"))), id + "/pasture").delay(6100).autoNext("visit");
        lines.node("visit").say(DialogueText.translatable("rumble.gardenia.visit"), id + "/visit")
                .choice("done", DialogueText.translatable("gui.done"), choice -> choice.addEvents((p, npc) -> {
                    var data = ArcQuestPlayerManager.getOrCreate(p);
                    if (!eligibleGift(p, encounter) || !data.hasFlag(GARDENIA_GIFT)) return;
                    data.setFlag(GARDENIA_DONE);
                    finishDebrief(p, encounter);
                }).close());
    }

    private static ItemStack[] gardeniaRewards() {
        return new ItemStack[]{RewardNotice.stack(PrologueWorld.id(PhoneApp.HEALING.itemId()).toString(), 1),
                RewardNotice.stack("virtualloot:virtual_pasture", 1)};
    }

    private static boolean eligibleGift(ServerPlayer player, Encounter encounter) {
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        return session != null && matching(player, session.getEntity()) == encounter
                && phaseActive(player, secondGymQuest(), DEBRIEF);
    }

    private static void finishDebrief(ServerPlayer player, Encounter encounter) {
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        if (session == null || matching(player, session.getEntity()) != encounter
                || !phaseActive(player, quest(encounter.gym()), DEBRIEF)) return;
        if (encounter.leader() && encounter.gym() == Gym.ICE && phaseActive(player, fourthGymQuest(), DEBRIEF)) {
            RewardNotice.grant(player, SafariChapter.tm());
            new GuideUnlockService().grant(player, PrologueWorld.id("technical_machines"));
        }
        QuestProgressHandler.incrementObjective(player, quest(encounter.gym()).toString(), DEBRIEF, 0, 1);
        if (encounter.leader() && encounter.gym() == Gym.DARK
                && ArcQuestPlayerManager.getOrCreate(player).isQuestCompleted(seventhGymQuest().toString())) {
            acceptNext(player);
            CinemaQuest.offer(player);
        }
        if (encounter.leader() && encounter.gym() == Gym.GROUND && !phaseActive(player, fifthGymQuest(), DEBRIEF)) {
            acceptNext(player);
            TeraChapter.offer(player);
            SimulatorTestQuest.offer(player);
        }
    }

    private static void giveGardeniaGift(ServerPlayer player, Encounter encounter) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!eligibleGift(player, encounter) || data.hasFlag(GARDENIA_GIFT)) return;
        RewardNotice.grant(player, gardeniaRewards());
        new GuideUnlockService().grant(player, RumbleGuides.VIRTUAL_PASTURE);
        data.setFlag(GARDENIA_GIFT);
    }

    public static boolean talkable(Entity entity) {
        return entity instanceof NPCEntity npc && PrologueWorld.GYMS.equals(npc.level().dimension())
                && TRAINER_MODEL.equals(npc.getEntityData().get(NPCEntity.Companion.getRESOURCE_IDENTIFIER()));
    }

    public static boolean isDialogueActor(Entity speaker, NPCEntity actor, boolean partner) {
        if (!talkable(speaker) || !talkable(actor)) return false;
        if (!partner) return speaker == actor;
        if (speaker == actor || speaker.distanceToSqr(actor) > 36) return false;
        var speakerAspects = ((NPCEntity) speaker).getAspects();
        return GymCatalog.ALL.stream().filter(e -> e.actors().size() > 1
                        && e.actors().stream().anyMatch(a -> speakerAspects.contains(a.aspect())))
                .anyMatch(e -> e.actors().stream().anyMatch(a -> actor.getAspects().contains(a.aspect())));
    }

    private static Encounter matching(ServerPlayer player, Entity entity) {
        if (entity == null || !entity.isAlive() || entity.level() != player.level() || !talkable(entity)
                || player.isSpectator() || !player.isAlive() || !player.hasLineOfSight(entity)
                || player.distanceToSqr(entity) > ProfessorEntity.TALK_DISTANCE * ProfessorEntity.TALK_DISTANCE) return null;
        String id = FixedStoryActors.role(entity);
        return GymCatalog.ALL.stream().filter(e -> e.actors().stream().anyMatch(a -> a.id().equals(id))).findFirst().orElse(null);
    }

    public static boolean talk(ServerPlayer player, Entity entity) {
        var encounter = matching(player, entity);
        if (encounter == null) return false;
        String actorId = FixedStoryActors.role(entity);
        var actor = encounter.actors().stream().filter(a -> a.id().equals(actorId)).findFirst().orElseThrow();
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!canChallenge(player, encounter)) {
            String key = data.isQuestCompleted(quest(encounter.gym()).toString()) ? "hall_cleared"
                    : data.hasFlag(flag(encounter)) ? "keep_going" : encounter.leader() && unlocked(player, encounter.gym()) ? "gym_leader_wait" : "wait";
            DialogueSessionManager.INSTANCE.startDialogue(player, entity, tree(actor) + "/idle",
                    new DialogueContext().put("idleKey", PREFIX + "idle." + key));
            return true;
        }
        if (!ArenaMatch.available(player)) {
            player.displayClientMessage(Component.translatable(PREFIX + "busy"), true);
            return true;
        }
        if (encounter.leader() && phaseActive(player, quest(encounter.gym()), DEBRIEF)) {
            String route = gardenia(encounter) && data.hasFlag(GARDENIA_GIFT)
                    ? tree(actor) + "/gift/instructions" : resultTree(actor, "win_reward");
            DialogueSessionManager.INSTANCE.startDialogue(player, entity, route, new DialogueContext());
            return true;
        }
        DialogueSessionManager.INSTANCE.startDialogue(player, entity,
                tree(actor) + (active(player, encounter) ? "" : "/rematch"), new DialogueContext());
        return true;
    }

    public static boolean canChallenge(ServerPlayer player, Encounter encounter) {
        return active(player, encounter) || encounter.leader()
                && ArcQuestPlayerManager.getOrCreate(player).hasFlag(flag(encounter));
    }

    public static boolean atTrainer(ServerPlayer player, UUID speaker, Encounter encounter) {
        return matching(player, player.serverLevel().getEntity(speaker)) == encounter;
    }

    @SubscribeEvent
    public static void preventNativeInteraction(PlayerInteractEvent.EntityInteract event) {
        if (blocksNativeInteraction(event.getTarget())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preventNativeInteraction(PlayerInteractEvent.EntityInteractSpecific event) {
        if (blocksNativeInteraction(event.getTarget())) event.setCanceled(true);
    }

    private static boolean blocksNativeInteraction(Entity target) {
        return target instanceof NPCEntity npc && (talkable(npc) || npc.level() instanceof ServerLevel level && ArenaMatch.isArena(level));
    }

    public static boolean active(ServerPlayer player, Encounter encounter) {
        var data = ArcQuestPlayerManager.get(player);
        var current = data == null ? null : data.getActiveQuest(quest(encounter.gym()).toString());
        return current != null && current.isPhaseActive(encounter.id());
    }

    private static boolean unlocked(ServerPlayer player, Gym gym) {
        var data = ArcQuestPlayerManager.get(player);
        return data != null && data.isQuestCompleted(prerequisite(gym));
    }

    public static boolean allowsArrival(ServerPlayer player, BlockPos pos) {
        var route = section(pos);
        return route == null || player.isCreative() || unlocked(player, route);
    }

    public static void acceptNext(ServerPlayer player) {
        var data = ArcQuestPlayerManager.get(player);
        if (data == null) return;
        for (var gym : Gym.values()) {
            var id = quest(gym).toString();
            if (!unlocked(player, gym) || data.isQuestCompleted(id)) continue;
            if (!data.isQuestActive(id)) QuestProgressHandler.acceptQuest(player, id);
            return;
        }
    }

    public static ResourceLocation eighthGymQuest() { return quest(Gym.ELECTRIC); }

    private static boolean advanceVictory(ServerPlayer player, Encounter encounter) {
        if (!active(player, encounter)) return false;
        var questId = quest(encounter.gym()).toString();
        QuestProgressHandler.incrementObjective(player, questId, encounter.id(), 0, 1);
        var data = ArcQuestPlayerManager.getOrCreate(player);
        boolean rewarded = data.hasFlag(flag(encounter));
        acceptNext(player);
        if (encounter.leader() && encounter.gym() == Gym.FIRE && data.isQuestCompleted(sixthGymQuest().toString()))
            LegendaryCommission.offer(player);
        if (encounter.leader() && encounter.gym() == Gym.ELECTRIC && data.isQuestCompleted(eighthGymQuest().toString()))
            ChampionStory.offer(player);
        return rewarded;
    }

    public static void battleResult(ServerPlayer player, Encounter encounter, UUID speaker, boolean won) {
        boolean rewarded = won && advanceVictory(player, encounter);
        if (speaker == null || player.serverLevel().dimension() != PrologueWorld.GYMS) return;
        int now = player.server.getTickCount();
        RESULTS.put(player, new Result(encounter, speaker, won, rewarded, now + 12, now + 160));
    }

    private static void openResult(ServerPlayer player) {
        var result = RESULTS.get(player);
        if (result == null) return;
        int now = player.server.getTickCount();
        if (now >= result.expires() || player.serverLevel().dimension() != PrologueWorld.GYMS) {
            RESULTS.remove(player);
            return;
        }
        if (now < result.earliest() || DialogueSessionManager.INSTANCE.isInDialogue(player)) return;
        var npc = player.serverLevel().getEntity(result.speaker());
        if (matching(player, npc) != result.encounter()) return;
        var tag = FixedStoryActors.role(npc);
        result.encounter().actors().stream().filter(actor -> actor.id().equals(tag)).findFirst().ifPresent(actor -> {
            RESULTS.remove(player);
            String outcome = result.rewarded() ? "win_reward" : result.won() ? "win" : "lose";
            DialogueSessionManager.INSTANCE.startDialogue(player, npc, resultTree(actor, outcome), new DialogueContext());
        });
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        var pending = PENDING.get(player);
        if (pending != null && (player.server.getTickCount() >= pending.expires() || !DialogueSessionManager.INSTANCE.isInDialogue(player))) {
            PENDING.remove(player);
            if (player.server.getTickCount() < pending.expires() && canChallenge(player, pending.encounter())
                    && matching(player, player.serverLevel().getEntity(pending.speaker())) == pending.encounter()) {
                ArenaMatch.challengeTrainer(player, pending.encounter(), player.serverLevel().getEntity(pending.speaker()).getUUID());
            }
        }
        openResult(player);
        if (player.tickCount % 10 == 0 && !RESULTS.containsKey(player)
                && !DialogueSessionManager.INSTANCE.isInDialogue(player)) FirstBadgeVisit.notifyPlayer(player);
        if (!PrologueWorld.GYMS.equals(player.level().dimension()) || player.isSpectator()) return;
        var route = section(player.blockPosition());
        if (route != null && !player.isCreative() && !unlocked(player, route)) {
            DialogueSessionManager.INSTANCE.endDialogue(player);
            PrologueWorld.arrive(player);
            player.displayClientMessage(Component.translatable(PREFIX + "locked"), true);
            return;
        }
        if (player.tickCount % 20 != 0) return;
        acceptNext(player);
        if (route == null) return;
        var entrance = ENTRANCES.get(route.ordinal());
        if (player.blockPosition().distSqr(entrance) <= 22 * 22) {
            QuestProgressHandler.incrementObjective(player, quest(route).toString(), ARRIVE, 0, 1);
        }
    }

    // Same octagon as the hub spawn plaza; indices follow the roads counter-clockwise from north.
    static Gym section(BlockPos position) {
        int x = position.getX() - PrologueWorld.ANCHOR.getX(), z = position.getZ() - PrologueWorld.ANCHOR.getZ();
        int dx = Math.abs(x), dz = Math.abs(z);
        if (dx <= 42 && dz <= 46 && dx + dz <= 65) return null;
        return Gym.values()[Math.floorMod((int) Math.round(Math.atan2(-x, -z) / (Math.PI / 4)), 8)];
    }

    public static List<Actor> hallActors() {
        return GymCatalog.ALL.stream().flatMap(encounter -> encounter.actors().stream()).toList();
    }

    public static NPCEntity createHallTrainer(ServerLevel level, Actor actor) {
        var at = actor.pos();
        // Try the catalog block first; some speakers stood over props missing from the migrated map.
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                var column = at.offset(dx, 0, dz);
                if ((column.getX() >> 4) != (at.getX() >> 4) || (column.getZ() >> 4) != (at.getZ() >> 4)) continue;
                for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3, 4}) {
                    var foot = column.above(dy);
                    if (!level.getBlockState(foot.below()).getCollisionShape(level, foot.below()).isEmpty()
                            && level.getBlockState(foot).getCollisionShape(level, foot).isEmpty()
                            && level.getBlockState(foot.above()).getCollisionShape(level, foot.above()).isEmpty()) {
                        var npc = newTrainer(level, actor, Vec3.atBottomCenterOf(foot), 0);
                        faceHallCourt(npc, actor.id());
                        return npc;
                    }
                }
            }
        }
        return null;
    }

    public static void faceHallCourt(NPCEntity npc, String actorId) {
        var gym = GymCatalog.actorGym(actorId);
        if (gym == null) return;
        double dx = gym.courtX + .5 - npc.getX(), dz = gym.courtZ + .5 - npc.getZ();
        if (dx * dx + dz * dz < .01) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        npc.setYRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
        npc.yRotO = npc.yBodyRotO = npc.yHeadRotO = yaw;
        npc.getLookControl().setLookAt(gym.courtX + .5, npc.getEyeY(), gym.courtZ + .5);
    }

    public static NPCEntity trainerModel(Level level, String aspect) {
        var npc = new NPCEntity(level);
        var type = NPCClasses.INSTANCE.getByIdentifier(TRAINER_MODEL);
        if (type == null) throw new IllegalStateException("Missing Cobblemon NPC class: " + TRAINER_MODEL);
        npc.setNpc(type);
        // A detached client preview never receives the server’s tracked model identifier.
        if (level.isClientSide()) npc.getEntityData().set(NPCEntity.Companion.getRESOURCE_IDENTIFIER(), type.getResourceIdentifier());
        npc.getAppliedAspects().add(aspect);
        npc.updateAspects();
        return npc;
    }

    public static NPCEntity newTrainer(ServerLevel level, Actor actor, Vec3 location, float yaw) {
        var npc = trainerModel(level, actor.aspect());
        npc.setCustomName(actor.name());
        npc.setCustomNameVisible(false);
        npc.setHideNameTag(true);
        npc.setInvulnerable(true);
        npc.setMovable(false);
        npc.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        npc.getLookControl().setLookAt(location.x - 2 * Math.sin(Math.toRadians(yaw)), location.y + 1.4,
                location.z + 2 * Math.cos(Math.toRadians(yaw)));
        npc.moveTo(location.x, location.y, location.z, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        return npc;
    }
}
