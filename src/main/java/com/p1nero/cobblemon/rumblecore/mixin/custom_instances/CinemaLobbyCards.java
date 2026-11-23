package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.gym.CinemaTrials;
import com.p1nero.cobblemon.rumblecore.prologue.client.CinemaCatalogScreen;
import com.p1nero.cobblemon.rumblecore.prologue.client.CinemaPartyRoster;
import com.p1nero.cobblemon.rumblecore.prologue.client.CinemaRoleNotice;
import com.p1nero.misc.custom_instances.client.screen.FantasyScreen;
import com.p1nero.misc.custom_instances.client.screen.LobbyScreen;
import com.p1nero.misc.custom_instances.client.widget.PartyRosterWidget;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LobbyScreen.class, remap = false)
abstract class CinemaLobbyCards extends FantasyScreen {
    @Shadow @Final private InstanceDefinition definition;
    @Shadow @Final private UUID token;
    @Shadow private CompoundTag party;
    @Shadow private boolean canKick() { throw new AssertionError(); }
    @Unique private final TrialPortraits rumble$cinemaPortraits = new TrialPortraits();

    private CinemaLobbyCards(Component title) { super(title); }

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void rumble$renderCinemaTitle(GuiGraphics g, CallbackInfo ci) {
        var trial = CinemaTrials.Trial.fromTemplate(definition.id());
        if (trial == null) return;
        var featured = trial.sides().stream().map(List::getFirst).toList();
        int size = 20, gap = 2, portraitsWidth = featured.size() * (size + gap) - gap;
        var label = Language.getInstance().getVisualOrder(font.substrByWidth(title, Math.max(1, panelWidth - portraitsWidth - 22)));
        int x = (width - portraitsWidth - 7 - font.width(label)) / 2;
        for (int i = 0; i < featured.size(); i++) {
            var cast = featured.get(i);
            int px = x + i * (size + gap), py = top + 5;
            OreTheme.well(g, px, py, size, size);
            if (!rumble$cinemaPortraits.drawFace(g, cast.speciesId(), cast.aspects(), px + 1, py + 1, size - 2, 0, i > 0))
                rumble$cinemaPortraits.render(g, cast.speciesId(), cast.aspects(), px + 1, py + 1, size - 2, 0);
        }
        g.drawString(font, label, x + portraitsWidth + 7, top + 11, OreTheme.TEXT, false);
        ci.cancel();
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", ordinal = 0, target =
            "Lcom/p1nero/misc/custom_instances/client/screen/LobbyScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    private GuiEventListener rumble$cinemaRoster(GuiEventListener original) {
        if (!(original instanceof PartyRosterWidget roster) || !party.contains("rumbleCinema") || minecraft.player == null) return original;
        var trial = CinemaTrials.Trial.fromTemplate(definition.id());
        if (trial == null) return original;
        return new CinemaPartyRoster(roster.getX(), roster.getY(), roster.getWidth(), party, trial, rumble$cinemaPortraits,
                minecraft.player.getUUID(), canKick(), () -> CinemaCatalogScreen.chooseRole(this, trial, party), member -> {
                    var request = new CompoundTag();
                    request.putUUID("session", party.getUUID("session"));
                    request.putUUID("member", member);
                    InstanceNetwork.request("kick", token, request);
                });
    }

    @Inject(method = "lambda$init$3", at = @At("HEAD"), cancellable = true)
    private void rumble$checkReadyRole(boolean alreadyReady, Button button, CallbackInfo ci) {
        if (alreadyReady || !party.contains("rumbleCinema") || minecraft.player == null) return;
        var selections = party.getCompound("rumbleCinema").getCompound("selections");
        var viewer = minecraft.player.getUUID().toString();
        String reason = !selections.contains(viewer) ? "role_required"
                : selections.getCompound(viewer).getBoolean("conflict") ? "role_conflict" : null;
        if (reason == null) return;
        var trial = CinemaTrials.Trial.fromTemplate(definition.id());
        if (trial == null) return;
        minecraft.setScreen(new CinemaRoleNotice((LobbyScreen) (Object) this, party.getUUID("session"),
                Component.translatable("rumble.cinema." + reason), () -> CinemaCatalogScreen.chooseRole(this, trial, party)));
        ci.cancel();
    }
}
