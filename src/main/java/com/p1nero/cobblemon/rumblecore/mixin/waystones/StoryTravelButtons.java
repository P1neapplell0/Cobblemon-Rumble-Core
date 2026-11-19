package com.p1nero.cobblemon.rumblecore.mixin.waystones;

import com.p1nero.cobblemon.rumblecore.client.phone.ClientPhoneState;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.NativePanels;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets;
import com.p1nero.cobblemon.rumblecore.prologue.PersonalDimension;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.cobblemon.rumblecore.prologue.TrialWaystones;
import net.blay09.mods.waystones.client.gui.screen.WaystoneContainerScreen;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.blay09.mods.waystones.client.gui.widget.ManageWaystonesButton;
import net.blay09.mods.waystones.client.gui.widget.WaystoneGroupFilterButton;
import net.blay09.mods.waystones.menu.WaystoneSelectionMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WaystoneSelectionScreenBase.class, remap = false)
public abstract class StoryTravelButtons extends WaystoneContainerScreen<WaystoneSelectionMenu> {
    @Shadow private int headerY;

    protected StoryTravelButtons(WaystoneSelectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void rumble$panel(GuiGraphics g, float partialTick, int mx, int my, CallbackInfo ci) {
        NativePanels.waystones(g, leftPos, topPos, imageWidth, imageHeight);
    }

    @Inject(method = "updateList", at = @At("TAIL"))
    private void rumble$insetHeader(CallbackInfo ci) {
        // Searching and changing filters reset the native offset before rebuilding the list.
        headerY = NativePanels.WAYSTONE_TITLE_Y;
    }

    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 20, ordinal = 0))
    private int rumble$headerSpacing(int vanillaSpacing) {
        return NativePanels.WAYSTONE_HEADER_LINE_SPACING;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$addDestinations(CallbackInfo ci) {
        for (var child : children()) {
            if (child instanceof Button button && (button instanceof ManageWaystonesButton || button instanceof WaystoneGroupFilterButton)) {
                button.setX(leftPos + NativePanels.WAYSTONE_SIDE_INSET);
            }
        }
        if (minecraft == null || minecraft.player == null) return;
        int y = topPos + imageHeight - 23;
        var player = minecraft.player;
        boolean home = ClientPhoneState.hasBit(player, PersonalDimension.COMPLETE_BIT);
        boolean first = ClientPhoneState.hasBit(player, PersonalDimension.FIRST_VISIT_BIT);
        int available = imageWidth - (home || first ? 24 : 20);
        int w = available / (home || first ? 3 : 2);
        boolean unlocked = ClientPhoneState.hasBit(player, TrialWaystones.STORY_TRAVEL_BIT);
        rumble$destination(TrialWaystones.OVERWORLD_NAME, false, leftPos + 8, y, w, unlocked, player.level().dimension() == Level.OVERWORLD);
        rumble$destination(TrialWaystones.HALL_NAME, true, leftPos + 12 + w, y, home || first ? w : available - w, unlocked, player.level().dimension() == PrologueWorld.GYMS);
        if (home || first) {
            var button = OreTheme.action(leftPos + 16 + 2 * w, y, available - 2 * w, 18,
                    Component.translatable("rumble.waystones." + (home ? "personal" : "personal_first")), true,
                    pressed -> PacketDistributor.sendToServer(new ProloguePackets.PersonalTravel(menu.containerId)));
            button.active = unlocked && !player.level().dimension().location().equals(
                    leaf.soulhome.utils.ResourceLocationHelper.prefix(player.getUUID().toString()));
            addRenderableWidget(button);
        }
    }

    @Unique
    private void rumble$destination(String label, boolean toHall, int x, int y, int width, boolean unlocked, boolean here) {
        Button button = OreTheme.action(x, y, width, 18, Component.translatable(label), true,
                pressed -> PacketDistributor.sendToServer(new ProloguePackets.StoryTravel(menu.containerId, toHall)));
        button.active = unlocked && !here;
        if (!button.active) button.setTooltip(Tooltip.create(Component.translatable(unlocked ? "rumble.waystones.already_here" : TrialWaystones.LOCKED)));
        addRenderableWidget(button);
    }
}
