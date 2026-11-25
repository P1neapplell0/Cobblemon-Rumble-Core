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
        } else if (mc.screen instanceof Choose choose && choose.packet.match().equals(packet.match())
                && choose.packet.round() == packet.round() && packet.gymDeathsLeft() >= 0
                && choose.gymDeathsLeft != packet.gymDeathsLeft()) {
            choose.gymDeathsLeft = packet.gymDeathsLeft();
        } else {
            mc.setScreen(new Choose(packet));
        }
    }

    public static boolean choosing() { return Minecraft.getInstance().screen instanceof Choose; }

    static void gymReport(ArenaPackets.GymReport packet) {
        com.p1nero.cobblemon.rumblecore.client.combat.TeamDpsScreen.preview(packet.dimension(), packet.report());
    }

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
        private int gymDeathsLeft;

        Choose(ArenaPackets.Switch packet) {
            super(Component.translatable("rumble.arena.switch"));
            this.packet = packet;
            this.gymDeathsLeft = packet.gymDeathsLeft();
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

        private int firstTileY() { return selection.getSlotPosition(0).getSecond().intValue(); }

        private int rosterOffset() {
            return selection == null || gymDeathsLeft < 0 ? 0 : Math.max(0, 35 - firstTileY());
        }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (selection == null) return;
            int offset = rosterOffset();
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, offset, 0);
            selection.render(graphics, mouseX, mouseY - offset, partialTick);
            pose.popPose();
            if (gymDeathsLeft < 0) return;

            var title = Component.translatable("rumble.gym.deaths_left", gymDeathsLeft).withStyle(net.minecraft.ChatFormatting.BOLD);
            int textWidth = font.width(title);
            float scale = Math.min(2F, (width - 28F) / Math.max(1, textWidth));
            int halfWidth = (int) Math.ceil(textWidth * scale / 2) + 8;
            int x = width / 2, tileY = firstTileY() + offset;
            int top = tileY - (int) Math.ceil(font.lineHeight * scale) - 13;
            int color = gymDeathsLeft == 1 ? 0xFFFFAE75 : 0xFFFFE6A1;
            graphics.fill(x - halfWidth, top, x + halfWidth, tileY - 6, 0xE819202D);
            graphics.fill(x - halfWidth, tileY - 7, x + halfWidth, tileY - 6, color);
            pose.pushPose();
            pose.translate(x, top + 3, 0);
            pose.scale(scale, scale, 1);
            graphics.drawCenteredString(font, title, 0, 0, color);
            pose.popPose();
        }

        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY - rosterOffset(), button);
        }

        @Override public void onClose() {
            if (!shouldCloseOnEsc()) return;
            PacketDistributor.sendToServer(new ArenaPackets.Pick(packet.match(), packet.round(), -1));
            super.onClose();
        }
        @Override public boolean shouldCloseOnEsc() { return packet.round() == ArenaPackets.WILD_ROUND; }
        @Override public boolean isPauseScreen() { return false; }
    }
}
