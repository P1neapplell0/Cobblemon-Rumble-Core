package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.p1nero.misc.custom_instances.client.screen.FantasyScreen;
import com.p1nero.misc.custom_instances.client.screen.GateCatalogScreen;
import com.p1nero.misc.custom_instances.client.scroll.SmoothScroll;
import com.p1nero.misc.custom_instances.util.CustomText;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filters the native catalog rows, keeping their selection tokens and button behavior. */
@Mixin(value = GateCatalogScreen.class, remap = false)
abstract class MegaCatalogSearch extends FantasyScreen {
    @Shadow @Final private ListTag entries;
    @Shadow @Final private SmoothScroll scroll;
    @Unique private ListTag rumble$allEntries;
    @Unique private EditBox rumble$search;

    private MegaCatalogSearch(Component title) { super(title); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rumble$rememberMegaEntries(CompoundTag data, CallbackInfo ci) {
        if ("{rumble.mega.simulator.mega}".equals(data.getString("title"))) {
            rumble$allEntries = entries.copy();
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$addSearch(CallbackInfo ci) {
        if (rumble$allEntries == null) return;
        if (rumble$search == null) {
            var label = Component.translatable("rumble.mega.catalog.search");
            rumble$search = new EditBox(font, 0, 0, 0, 18, label);
            rumble$search.setMaxLength(64);
            rumble$search.setHint(label);
            rumble$search.setResponder(this::rumble$filter);
        }
        rumble$search.setX(left + 16);
        rumble$search.setY(top + 186);
        rumble$search.setWidth(panelWidth - 32);
        addRenderableWidget(rumble$search);
    }

    @ModifyConstant(method = "renderContents", constant = @Constant(intValue = 196))
    private int rumble$moveCount(int original) {
        return rumble$allEntries == null ? original : 207;
    }

    @Inject(method = "renderContents", at = @At("HEAD"), cancellable = true)
    private void rumble$emptySearch(GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTick, CallbackInfo ci) {
        if (rumble$search == null || rumble$search.getValue().isBlank() || !entries.isEmpty()) return;
        graphics.drawCenteredString(font, Component.translatable("rumble.mega.catalog.empty"), width / 2, top + 96, 0xFFD2B77D);
        ci.cancel();
    }

    @Unique private void rumble$filter(String query) {
        String[] terms = rumble$normalize(query).strip().split("\\s+");
        entries.clear();
        for (int i = 0; i < rumble$allEntries.size(); i++) {
            var entry = rumble$allEntries.getCompound(i);
            var icon = entry.getCompound("customIcon");
            var speciesId = ResourceLocation.tryParse(icon.getString("species"));
            var species = speciesId == null ? null : PokemonSpecies.getByIdentifier(speciesId);
            String name = CustomText.parse(entry.getString("name")).getString();
            String localized = species == null ? "" : species.getTranslatedName().getString();
            String description = name + " " + localized + " " + entry.getString("id") + " "
                    + icon.getString("species") + " " + icon.getString("aspects");
            String searchable = description.toLowerCase(Locale.ROOT) + " " + rumble$normalize(description);
            boolean matches = true;
            for (String term : terms) {
                if (!searchable.contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches) entries.add(entry);
        }
        scroll.reset();
        rebuildWidgets();
        setFocused(rumble$search);
    }

    @Unique private static String rumble$normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[_,:-]+", " ");
    }
}
