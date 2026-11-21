package com.p1nero.cobblemon.rumblecore.client.menu;

import com.cobblemon.mod.common.client.render.models.blockbench.TexturedModel;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationAdapter;
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Bone;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.ModList;

/** Title-owned instances of the original models, textures, and clips selected in the HTML preview. */
record TitleRig(PokemonPosableModel model, ResourceLocation skin, ResourceLocation[] glow,
                BedrockAnimation idle, BedrockAnimation move, BedrockAnimation blink, Set<String> aspects) {
    private static final String POKEMON = "assets/cobblemon/bedrock/pokemon/";
    private static final com.google.gson.Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BedrockAnimation.class, BedrockAnimationAdapter.INSTANCE).create();

    static TitleRig load(int dex, String species, boolean flying) throws IOException {
        String animationGroup = species.equals("salamence") ? "salamence_mega" : species;
        String folder = String.format(java.util.Locale.ROOT, "%04d_%s", dex, species);
        String modelName = switch (species) {
            case "charizard" -> "charizard_mega_x";
            case "salamence" -> "salamencemega";
            case "pikachu", "scizor", "garchomp", "blaziken" -> species + "_male";
            default -> species;
        };
        String modelMod = switch (species) {
            case "charizard", "salamence" -> "mega_showdown";
            default -> "cobblemon";
        };
        String animationMod = species.equals("salamence") ? modelMod : "cobblemon";
        String geometry = read(modelMod, "models/" + folder + "/" + modelName + ".geo.json");
        var geo = Objects.requireNonNull(TexturedModel.Companion.from(geometry), modelName);
        var root = geo.create().bakeRoot();
        var model = new PokemonPosableModel((Bone) (Object) root);
        // Greninja's bb_main is a sibling of the animated greninja root.
        for (var bone : geo.getGeometry().getFirst().getBones()) {
            if (bone.getParent() == null) model.registerChildWithAllChildren(root, bone.getName());
        }

        String animations = read(animationMod, "animations/" + folder + "/" + animationGroup + ".animation.json");
        var clips = JsonParser.parseString(animations).getAsJsonObject().getAsJsonObject("animations");
        String idleName = flying ? "air_idle" : "battle_idle";
        var idle = clip(clips, animationGroup, idleName);
        if (idle == null || idle.getBoneTimelines().keySet().stream().noneMatch(model.getRelevantPartsByName()::containsKey)) {
            throw new IOException("No matching bones for " + animationGroup + "." + idleName + " in " + modelName);
        }
        var move = switch (species) {
            case "charizard" -> clip(clips, species, "air_battle_cry");
            case "pikachu" -> clip(clips, species, "special");
            default -> null;
        };
        String skin = switch (species) {
            case "charizard" -> "megacharizardx";
            case "salamence" -> "salamence_mega";
            case "ceruledge" -> "ceruledge_1";
            default -> species;
        };
        var glow = switch (species) {
            case "charizard" -> new ResourceLocation[]{texture(modelMod, folder, "x_glow"), texture(modelMod, folder, "x_glow2"),
                    texture(modelMod, folder, "x_glow3"), texture(modelMod, folder, "x_glow4")};
            case "ceruledge" -> java.util.stream.IntStream.rangeClosed(1, 8)
                    .mapToObj(i -> texture(modelMod, folder, "ceruledge_emissive_" + i)).toArray(ResourceLocation[]::new);
            default -> new ResourceLocation[0];
        };
        Set<String> aspects = switch (species) {
            case "charizard" -> Set.of("mega_x");
            case "salamence" -> Set.of("mega");
            default -> Set.of("male");
        };
        var blink = species.equals("charizard") ? null : clip(clips, animationGroup, "blink");
        return new TitleRig(model, texture(modelMod, folder, skin), glow, idle, move, blink, aspects);
    }

    ResourceLocation glowAt(float seconds) {
        return glow.length == 0 ? null : glow[(int) (seconds * 10) % glow.length];
    }

    private static BedrockAnimation clip(JsonObject clips, String species, String name) {
        String key = "animation." + species + "." + name;
        var clip = GSON.fromJson(clips.get(key), BedrockAnimation.class);
        if (clip != null) clip.setName(key);
        return clip;
    }

    private static ResourceLocation texture(String mod, String folder, String name) {
        String path = folder + "/" + name + ".png";
        var id = ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, "title/pokemon/" + path);
        var textures = Minecraft.getInstance().getTextureManager();
        if (textures.getTexture(id, null) == null) {
            // The title model and its UVs belong to the same JAR as this texture.
            var source = resource(mod, "assets/cobblemon/textures/pokemon/" + path);
            textures.register(id, new SimpleTexture(id) {
                @Override protected TextureImage getTextureImage(ResourceManager manager) {
                    try (var input = Files.newInputStream(source)) {
                        return new TextureImage(null, NativeImage.read(input));
                    } catch (IOException e) {
                        return new TextureImage(e);
                    }
                }
            });
        }
        return id;
    }

    private static String read(String mod, String path) throws IOException {
        return Files.readString(resource(mod, POKEMON + path));
    }

    private static Path resource(String mod, String path) {
        return ModList.get().getModFileById(mod).getFile().findResource(path);
    }
}
