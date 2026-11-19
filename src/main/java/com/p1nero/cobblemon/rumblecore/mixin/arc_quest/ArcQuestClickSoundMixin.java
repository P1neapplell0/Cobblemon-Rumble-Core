package com.p1nero.cobblemon.rumblecore.mixin.arc_quest;

import com.cobblemon.mod.common.CobblemonSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces ArcQuest's vanilla GUI click and plays the Cobblemon sound at half volume. */
@Mixin(targets = {
        "org.arcadia.arc_quest.client.config.ArcQuestModConfigScreen",
        "org.arcadia.arc_quest.client.config.ArcQuestTextConfigScreen",
        "org.arcadia.arc_quest.client.hud.dialogue.DialogueScreen",
        "org.arcadia.arc_quest.client.hud.gacha.GachaPreviewPanel",
        "org.arcadia.arc_quest.client.hud.gacha.GachaResultRenderer",
        "org.arcadia.arc_quest.client.hud.gacha.GachaRollerPanel",
        "org.arcadia.arc_quest.client.hud.guide.GuideListScreen",
        "org.arcadia.arc_quest.client.hud.ponder.EmbeddedPonderScenePanel",
        "org.arcadia.arc_quest.client.hud.quest.history.QuestHistoryPanel",
        "org.arcadia.arc_quest.client.hud.quest.journal.QuestJournalScreen",
        "org.arcadia.arc_quest.client.hud.quest.journal.detail.JournalDetailParallelPhase",
        "org.arcadia.arc_quest.client.hud.quest.offer.QuestOfferPanel",
        "org.arcadia.arc_quest.client.hud.quest.ponder.QuestIntelPanel",
        "org.arcadia.arc_quest.client.hud.quest.trackingmenu.QuestTrackingMenuScreen",
        "org.arcadia.arc_quest.client.hud.quest.splash.QuestSplashRenderer",
        "org.arcadia.arc_quest.client.hud.shop.AbstractTradeScreen",
        "org.arcadia.arc_quest.client.hud.shop.TradeTooltipRenderer"
}, remap = false)
public abstract class ArcQuestClickSoundMixin {

    private static final float CLICK_VOLUME_MULTIPLIER = 0.5F;
    private static final float DEFAULT_UI_VOLUME = 0.25F;

    @Redirect(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/sounds/SoundEvents;UI_BUTTON_CLICK:Lnet/minecraft/core/Holder$Reference;"),
            require = 0,
            remap = false)
    private static Holder.Reference<SoundEvent> rumble$useCobblemonClickSound() {
        return BuiltInRegistries.SOUND_EVENT
                .getHolder(CobblemonSounds.GUI_CLICK.getLocation())
                .orElseThrow();
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forUI(Lnet/minecraft/core/Holder;F)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"),
            require = 0,
            remap = false)
    private static SimpleSoundInstance rumble$quieterHolderClick(Holder<SoundEvent> sound, float pitch) {
        SoundEvent event = sound.value();
        return isCobblemonClick(event)
                ? SimpleSoundInstance.forUI(event, pitch,
                DEFAULT_UI_VOLUME * CLICK_VOLUME_MULTIPLIER)
                : SimpleSoundInstance.forUI(sound, pitch);
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forUI(Lnet/minecraft/sounds/SoundEvent;F)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"),
            require = 0,
            remap = false)
    private static SimpleSoundInstance rumble$quieterEventClick(SoundEvent sound, float pitch) {
        return isCobblemonClick(sound)
                ? SimpleSoundInstance.forUI(sound, pitch,
                DEFAULT_UI_VOLUME * CLICK_VOLUME_MULTIPLIER)
                : SimpleSoundInstance.forUI(sound, pitch);
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forUI(Lnet/minecraft/sounds/SoundEvent;FF)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"),
            require = 0,
            remap = false)
    private static SimpleSoundInstance rumble$quieterEventClick(
            SoundEvent sound, float pitch, float volume) {
        return SimpleSoundInstance.forUI(sound, pitch,
                isCobblemonClick(sound) ? volume * CLICK_VOLUME_MULTIPLIER : volume);
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"),
            require = 0,
            remap = false)
    private static void rumble$playQuieterLocalClick(
            LocalPlayer player, SoundEvent sound, float volume, float pitch) {
        player.playSound(sound,
                isCobblemonClick(sound) ? volume * CLICK_VOLUME_MULTIPLIER : volume,
                pitch);
    }

    private static boolean isCobblemonClick(SoundEvent sound) {
        return CobblemonSounds.GUI_CLICK.getLocation().equals(sound.getLocation());
    }
}
