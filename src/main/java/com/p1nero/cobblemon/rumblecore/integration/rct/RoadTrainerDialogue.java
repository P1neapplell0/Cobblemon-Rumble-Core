package com.p1nero.cobblemon.rumblecore.integration.rct;

import com.gitlab.srcmc.rctapi.api.util.Text;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueContext;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;

/** Presents RCT's own localized replies without changing how RCT selects them. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class RoadTrainerDialogue {
    private static final String ID = PrologueWorld.id("road_trainer/reply").toString();
    private static final Map<ServerPlayer, Reply> PENDING = new WeakHashMap<>();
    private record Reply(TrainerMob trainer, Component line, int expires) {}

    private RoadTrainerDialogue() {}

    public static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var tree = DialogueTreeBuilder.create(ID).repeatable(true)
                .npc(DialogueText.of(ctx -> ctx.npc() == null ? Component.empty() : ctx.npc().getDisplayName()));
        tree.node("reply").say(DialogueText.of(ctx -> {
            var session = DialogueSessionManager.INSTANCE.getSession(ctx.player());
            if (session == null || !session.getContext().has("rctLine")) return Component.empty();
            var line = Component.Serializer.fromJson(session.getContext().getString("rctLine"), ctx.player().registryAccess());
            return line == null ? Component.empty() : line;
        }), ID + "/reply").choice("done", DialogueText.translatable("gui.done"), choice -> choice.close());
        event.register(tree.build());
    }

    public static boolean route(TrainerMob trainer, ServerPlayer player, Function<String, Text[]> lookup, String[] keys) {
        for (String key : keys) {
            Text[] lines = lookup.apply(key);
            if (lines == null) continue;
            if (lines.length == 0) return false;
            // The arena introduction takes over as soon as a challenge starts.
            if ("on_battle_start".equals(key) && ArenaMatch.inArena(player) != null) return true;
            Component line = lines[player.getRandom().nextInt(lines.length)].getComponent();
            PENDING.put(player, new Reply(trainer, line, player.server.getTickCount() + 200));
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Reply reply = PENDING.get(player);
        if (reply == null) return;
        if (!reply.trainer().isAlive() || player.server.getTickCount() >= reply.expires()) {
            PENDING.remove(player);
            return;
        }
        var manager = DialogueSessionManager.INSTANCE;
        if (player.level() != reply.trainer().level() || ArenaMatch.inArena(player) != null || Smash.active(player)
                || player.distanceToSqr(reply.trainer()) > 64 || player.containerMenu != player.inventoryMenu
                || manager.isInDialogue(player)) return;
        var dialogue = new DialogueContext().put("rctLine", Component.Serializer.toJson(reply.line(), player.registryAccess()));
        if (manager.startDialogue(player, reply.trainer(), ID, dialogue) != null) PENDING.remove(player, reply);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PENDING.remove(player);
    }
}
