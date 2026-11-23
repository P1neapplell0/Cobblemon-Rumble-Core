package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.gym.CinemaTrials;
import com.p1nero.cobblemon.rumblecore.prologue.client.CinemaCatalogScreen;
import com.p1nero.misc.custom_instances.client.screen.FantasyScreen;
import com.p1nero.misc.custom_instances.client.screen.LobbyScreen;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LobbyScreen.class, remap = false)
abstract class CinemaLobbyCards extends FantasyScreen {
    @Shadow @Final private InstanceDefinition definition;
    @Shadow private CompoundTag party;
    @Shadow private int menu;

    private CinemaLobbyCards(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$roleButton(CallbackInfo ci) {
        if (menu != 0 || !party.getString("state").equals("WAITING") || !party.contains("rumbleCinema")) return;
        var trial = CinemaTrials.Trial.fromTemplate(definition.id());
        if (trial == null) return;
        var roles = party.getCompound("rumbleCinema");
        int side = roles.getInt("side"), slot = roles.getInt("slot");
        boolean selected = side >= 0 && slot >= 0;
        var label = selected ? Component.translatable("rumble.cinema.change_role", trial.sides().get(side).get(slot).name())
                : Component.translatable("rumble.cinema.choose_role");
        addRenderableWidget(OreTheme.action(left + 16, top + 149, panelWidth - 32, 20, label, !selected,
                clicked -> CinemaCatalogScreen.chooseRole(this, trial, party)));
    }
}
