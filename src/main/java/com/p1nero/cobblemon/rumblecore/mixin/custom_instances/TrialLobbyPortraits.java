package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials;
import com.p1nero.misc.custom_instances.client.screen.FantasyScreen;
import com.p1nero.misc.custom_instances.client.screen.LobbyScreen;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LobbyScreen.class, remap = false)
abstract class TrialLobbyPortraits extends FantasyScreen {
    @Shadow @Final private InstanceDefinition definition;
    @Unique private final TrialPortraits rumble$portraits = new TrialPortraits();

    @Unique private CompoundTag rumble$megaPortrait;

    private TrialLobbyPortraits(Component title) { super(title); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rumble$readPortrait(CompoundTag data, CallbackInfo ci) {
        if (data.contains("rumbleMegaPortrait")) rumble$megaPortrait = data.getCompound("rumbleMegaPortrait");
    }

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void rumble$renderRoster(GuiGraphics graphics, CallbackInfo ci) {
        var trial = LegendaryTrials.template(definition.id());
        if (trial == null) {
            if (rumble$megaPortrait != null) {
                int x = Math.max(left + 9, (width - font.width(title) - 28) / 2);
                OreTheme.well(graphics, x, top + 3, 24, 24);
                if (rumble$portraits.draw(graphics, rumble$megaPortrait, x, top + 3, 24, 0)) {
                    graphics.drawString(font, title, x + 28, top + 11, OreTheme.TEXT, false);
                    ci.cancel();
                }
            }
            return;
        }
        int size = 24, x = left + panelWidth - 9 - size, y = top + 4;
        var name = font.substrByWidth(title, Math.max(12, x - left - 15)).getString();
        graphics.drawString(font, name, left + 9, top + 5, OreTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("rumble.legendary.boss_stats"), left + 9, top + 17, OreTheme.COINS, false);
        var foe = trial.foe();
        OreTheme.well(graphics, x, y, size, size);
        rumble$portraits.render(graphics, foe.speciesId(), foe.aspects(), x, y, size, 0);
        ci.cancel();
    }
}
