package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.gui.ProfileTransformType;
import com.cobblemon.mod.common.client.render.SpriteType;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.entity.PoseType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;

/** Reuses a pose per species/form, while callers determine the frame layout. */
public final class TrialPortraits {
    private record Key(ResourceLocation species, Set<String> aspects) {}
    private final Map<Key, FloatingState> poses = new HashMap<>();

    public boolean draw(GuiGraphics g, ResourceLocation species, Set<String> aspects, int x, int y, int size, float tick) {
        return draw(g, species, aspects, x, y, size, tick, false);
    }

    public boolean draw(GuiGraphics g, ResourceLocation species, Set<String> aspects, int x, int y, int size, float tick, boolean mirror) {
        if (PokemonSpecies.getByIdentifier(species) == null) return false;
        var pose = pose(species, aspects);
        g.enableScissor(x, y, x + size, y + size);
        g.pose().pushPose();
        try {
            // Cobblemon's profile transform expects a top-center anchor. Profile sprites
            // are drawn from that same anchor as a two-unit quad without the model transform.
            boolean sprite = VaryingModelRepository.INSTANCE.getSprite(species, pose, SpriteType.PROFILE) != null;
            g.flush();
            g.pose().translate(x + size / 2F, y + (sprite ? 0 : size / 26F), 25);
            float scale = sprite ? size / 2F : size * (11.25F / 26F);
            if (sprite && mirror) g.pose().scale(-1, 1, 1);
            PokemonGuiUtilsKt.drawProfilePokemon(species, g.pose(), new Quaternionf().rotationXYZ(.227F, mirror ? -.611F : .611F, 0),
                    PoseType.PROFILE, pose, tick, scale, ProfileTransformType.PROFILE, false, false,
                    1, 1, 1, 1, 0, 0, 15);
            g.flush();
            return true;
        } finally {
            g.pose().popPose();
            g.disableScissor();
        }
    }

    /** Same 21 px crop and head renderer as the Cobblemon party HUD, not its full-body profile. */
    public boolean drawFace(GuiGraphics g, ResourceLocation species, Set<String> aspects,
                            int x, int y, int size, float tick, boolean reversed) {
        var model = PokemonSpecies.getByIdentifier(species);
        if (model == null || size <= 0) return false;
        float scale = size / 21F;
        g.enableScissor(x, y, x + size, y + size);
        g.pose().pushPose();
        try {
            g.flush();
            g.pose().translate(x + (reversed ? 11.5F : 9.5F) * scale, y - 12 * scale, 25);
            g.pose().scale(scale, scale, 1);
            GuiUtilsKt.drawPosablePortrait(species, g.pose(), 13, model.getStandardForm().getBaseScale(),
                    reversed, pose(species, aspects), tick);
            g.flush();
            return true;
        } finally {
            g.pose().popPose();
            g.disableScissor();
        }
    }

    private FloatingState pose(ResourceLocation species, Set<String> aspects) {
        return poses.computeIfAbsent(new Key(species, Set.copyOf(aspects)), key -> {
            var state = new FloatingState();
            state.setCurrentAspects(key.aspects());
            return state;
        });
    }

    public boolean draw(GuiGraphics g, CompoundTag icon, int x, int y, int size, float tick) {
        var species = ResourceLocation.tryParse(icon.getString("species"));
        if (species == null) return false;
        var rawAspects = icon.getString("aspects");
        var aspects = rawAspects.isEmpty() ? Set.<String>of() : Set.copyOf(List.of(rawAspects.split(",")));
        return draw(g, species, aspects, x, y, size, tick);
    }

    public void render(GuiGraphics g, ResourceLocation species, Set<String> aspects, int x, int y, int size, float tick) {
        if (!draw(g, species, aspects, x, y, size, tick))
            g.renderItem(Items.ENDER_EYE.getDefaultInstance(), x + (size - 16) / 2, y + (size - 16) / 2);
    }
}
