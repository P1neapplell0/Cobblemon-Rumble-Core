package com.p1nero.cobblemon.rumblecore.prologue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;

public final class ProfessorStockMessage {
    private static final String ID = "rumble_professor_stock";
    private static final ChatSender PROFESSOR = new ChatSender(UUID.nameUUIDFromBytes(ID.getBytes(StandardCharsets.UTF_8)),
            "entity.cobblemon_rumble_core.professor", PrologueWorld.id("textures/entity/npc/prof_p1nero.png"), SenderType.NPC);
    private static final String CINEMA_ID = "rumble_cinema_intro";
    private static final String SIMULATOR_ID = "rumble_simulator_test";
    private static final String MAY_ID = "rumble_may_introduction";
    private static final String SAFARI_ID = "rumble_safari_invitation";
    private static final ChatSender MAY = new ChatSender(UUID.nameUUIDFromBytes(MAY_ID.getBytes(StandardCharsets.UTF_8)),
            MegaIntroduction.NAME, PrologueWorld.id("textures/entity/npc/xiao_yao.png"), SenderType.NPC);

    private ProfessorStockMessage() {}

    public static void register() {
        new NPCDialog.Builder(ID, PROFESSOR, true)
                .sendMessage(player -> Component.translatable("rumble.first_badge.sms", player.getDisplayName()),
                        List.of("rumble.first_badge.sms_reply")).build();
        new NPCDialog.Builder(SAFARI_ID, PROFESSOR, true)
                .sendMessage(player -> Component.translatable("rumble.safari.sms", player.getDisplayName()),
                        List.of("rumble.safari.sms_reply")).build();
        new NPCDialog.Builder(CINEMA_ID, PROFESSOR, true)
                .sendMessage(player -> Component.translatable("rumble.cinema.sms", player.getDisplayName()),
                        List.of("rumble.cinema.sms_reply")).build();
        new NPCDialog.Builder(SIMULATOR_ID, PROFESSOR, true)
                .sendMessage(player -> Component.translatable("rumble.simulator_test.sms_hello", player.getDisplayName()), List.of())
                .sendMessage(player -> Component.translatable("rumble.simulator_test.sms_test"), List.of("rumble.simulator_test.sms_what"))
                .sendMessage(player -> Component.translatable("rumble.simulator_test.sms_software"), List.of("rumble.simulator_test.sms_later")).build();
        new NPCDialog.Builder(MAY_ID, MAY, true)
                .sendMessage(player -> Component.translatable("rumble.mega.sms.hello"), List.of("rumble.mega.sms.who"))
                .sendMessage(player -> Component.translatable("rumble.mega.sms.name"), List.of("rumble.mega.sms.daughter"))
                .sendMessage(player -> Component.translatable("rumble.mega.sms.keystone"), List.of("rumble.mega.sms.reply")).build();
    }

    static void sendCinema(ServerPlayer player) {
        var dialogue = NPCDialogRegister.getNPCDialog(CINEMA_ID);
        if (dialogue != null) NPCDialogManager.getInstance().addNPCDialogQuest(PROFESSOR.getUuid(), player, dialogue, true);
    }

    static void sendSimulator(ServerPlayer player) {
        var dialogue = NPCDialogRegister.getNPCDialog(SIMULATOR_ID);
        if (dialogue != null) NPCDialogManager.getInstance().addNPCDialogQuest(PROFESSOR.getUuid(), player, dialogue, true);
    }

    static void sendMay(ServerPlayer player) {
        var dialogue = NPCDialogRegister.getNPCDialog(MAY_ID);
        if (dialogue != null) NPCDialogManager.getInstance().addNPCDialogQuest(MAY.getUuid(), player, dialogue, true);
    }

    static void send(ServerPlayer player) {
        var dialogue = NPCDialogRegister.getNPCDialog(ID);
        if (dialogue != null) NPCDialogManager.getInstance().addNPCDialogQuest(PROFESSOR.getUuid(), player, dialogue, true);
    }

    static void sendSafari(ServerPlayer player) {
        var dialogue = NPCDialogRegister.getNPCDialog(SAFARI_ID);
        if (dialogue != null) NPCDialogManager.getInstance().addNPCDialogQuest(PROFESSOR.getUuid(), player, dialogue, true);
    }
}
