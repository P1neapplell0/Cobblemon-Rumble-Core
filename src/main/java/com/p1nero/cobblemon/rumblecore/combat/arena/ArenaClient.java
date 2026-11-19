package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.net.messages.client.PlayerInteractOptionsPacket;
import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.battles.ShowdownPokemon;
import com.cobblemon.mod.common.battles.ShowdownSide;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattleActor;
import com.cobblemon.mod.common.client.battle.SingleActionRequest;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ArenaClient {
    private static ClientBattle buildingRoster;
    private static ResourceLocation arenaDimension;

    private ArenaClient() {}

    public static Screen challenge(PlayerInteractOptionsPacket packet) {
        return new ConfirmScreen(accept -> {
            if (accept) new BattleChallengePacket(packet.getNumericTargetId(), packet.getSelectedPokemonId(),
                    BattleFormat.Companion.getGEN_9_SINGLES()).sendToServer();
            Minecraft.getInstance().setScreen(null);
        }, Component.translatable("rumble.arena.challenge"), Component.translatable("rumble.arena.challenge_hint"),
                Component.translatable("rumble.arena.send"), Component.translatable("gui.cancel"));
    }

    static void invite(ArenaPackets.Invite packet) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen instanceof Choose) return;
        var previous = mc.screen;
        mc.setScreen(new ConfirmScreen(accept -> {
            PacketDistributor.sendToServer(new ArenaPackets.Reply(packet.invitation(), accept));
            mc.setScreen(previous);
        }, Component.translatable("rumble.arena.challenge"), Component.translatable("rumble.arena.invited", packet.challenger()),
                Component.translatable("rumble.arena.accept"), Component.translatable("gui.cancel")));
    }

    static void roster(ArenaPackets.Switch packet) {
        var mc = Minecraft.getInstance();
        if (packet.roster().isEmpty()) {
            if (mc.screen instanceof Choose choose && choose.packet.match().equals(packet.match())) mc.setScreen(null);
        } else {
            mc.setScreen(new Choose(packet));
        }
    }

    public static boolean choosing() { return Minecraft.getInstance().screen instanceof Choose; }

    static void prepare(ArenaPackets.Prepare packet) { arenaDimension = packet.dimension(); }

    public static boolean isArena() {
        var level = Minecraft.getInstance().level;
        return level != null && level.dimension().location().equals(arenaDimension);
    }

    /** Only the two native widget constructors see this data; no global Showdown battle is installed. */
    public static ClientBattle buildingRoster() { return buildingRoster; }

    public static boolean choose(BattleGUI gui, UUID pokemon) {
        if (!(Minecraft.getInstance().screen instanceof Choose screen) || screen.selection == null || screen.selection.getBattleGUI() != gui) return false;
        if (!screen.submitted) for (var option : screen.packet.roster()) {
            if (!option.pokemon().getUuid().equals(pokemon) || option.pokemon().isFainted()) continue;
            screen.submitted = true;
            PacketDistributor.sendToServer(new ArenaPackets.Pick(screen.packet.match(), screen.packet.round(), option.index()));
            break;
        }
        return true;
    }

    private static final class Choose extends Screen {
        private final ArenaPackets.Switch packet;
        private BattleSwitchPokemonSelection selection;
        private boolean submitted;

        Choose(ArenaPackets.Switch packet) {
            super(Component.translatable("rumble.arena.switch"));
            this.packet = packet;
        }

        @Override protected void init() {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var battle = new ClientBattle(packet.match(), BattleFormat.Companion.getGEN_9_SINGLES());
            var actor = new ClientBattleActor("p1", player.getDisplayName().copy(), player.getUUID(), ActorType.PLAYER);
            actor.setSide(battle.getSide1());
            battle.getSide1().getActors().add(actor);
            actor.setPokemon(packet.roster().stream().map(ArenaPackets.Option::pokemon).toList());
            var active = new ActiveClientBattlePokemon(actor, null);
            actor.getActivePokemon().add(active);
            var side = new ShowdownSide();
            side.setPokemon(actor.getPokemon().stream().map(mon -> {
                var entry = new ShowdownPokemon();
                entry.setDetails(mon.getSpecies().getName() + ", " + mon.getUuid());
                entry.setCondition(mon.getCurrentHealth() + "/" + mon.getMaxHealth() + (mon.isFainted() ? " fnt" : ""));
                return entry;
            }).toList());
            buildingRoster = battle;
            try {
                var gui = new BattleGUI();
                gui.setOpacity(1);
                selection = addRenderableWidget(new BattleSwitchPokemonSelection(gui,
                        new SingleActionRequest(active, side, null, true, false)));
            } finally {
                buildingRoster = null;
            }
        }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (selection != null) selection.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override public void onClose() {}
        @Override public boolean shouldCloseOnEsc() { return false; }
        @Override public boolean isPauseScreen() { return false; }
    }
}
