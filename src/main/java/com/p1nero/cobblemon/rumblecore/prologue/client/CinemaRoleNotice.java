package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.p1nero.misc.custom_instances.client.screen.LobbyScreen;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class CinemaRoleNotice extends ConfirmScreen {
    private final LobbyScreen lobby;
    private final UUID room;

    public CinemaRoleNotice(LobbyScreen lobby, UUID room, Component reason, Runnable choose) {
        super(confirmed -> {
            if (confirmed) choose.run();
            else Minecraft.getInstance().setScreen(lobby);
        }, Component.translatable("rumble.cinema.choose_role"), reason,
                Component.translatable("rumble.cinema.choose_role"), Component.translatable("gui.back"));
        this.lobby = lobby;
        this.room = room;
    }

    public boolean roomUpdate(String kind, CompoundTag data) {
        if (kind.equals("close")) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (!kind.equals("rooms")) return false;
        lobby.updateRooms(data);
        var party = data.getCompound("party");
        if (!party.hasUUID("session") || !room.equals(party.getUUID("session")) || !party.getString("state").equals("WAITING")) {
            Minecraft.getInstance().setScreen(lobby);
        }
        return true;
    }
}
