package com.p1nero.cobblemon.rumblecore.guide;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.guide.api.GuideCategory;
import org.arcadia.arc_quest.guide.api.GuideGroupDefinition;
import org.arcadia.arc_quest.guide.api.GuideText;
import org.arcadia.arc_quest.guide.builder.GuideBuilder;
import org.arcadia.arc_quest.guide.builder.GuidePageBuilder;

/** Registration is independent of unlock timing; quest phases can grant these stable IDs later. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class RumbleGuides {
    private static final ResourceLocation START = id("getting_started");
    private static final ResourceLocation COMBAT = id("battle_foundations");
    private RumbleGuides() {}

    @SubscribeEvent
    public static void register(ArcQuestRegistrationEvent.Guide event) {
        event.registerGroup(new GuideGroupDefinition(START, Component.translatable("rumble.guide.group.getting_started"), 0, 0x57C7A2));
        event.registerGroup(new GuideGroupDefinition(COMBAT, Component.translatable("rumble.guide.group.battle_foundations"), 10, 0xF7A658));
        add(event, START, "pokemon_inventory", "pokemon-inventory", "cobblemon:pokedex_red", 10,
                "key.cobblemon.summary", "key.realtimecobblemon.summary");
        add(event, COMBAT, "pokemon_moves", "pokemon-moves", "minecraft:book", 20, "key.cobblemon.summary");
        add(event, COMBAT, "realtime_combat", "realtime-combat", "cobblemon:poke_ball", 30,
                "key.cobblemon.throwpartypokemon", "key.attack", "key.realtimecobblemon.dodge", "key.realtimecobblemon.lock_on", "key.realtimecobblemon.smash");
        add(event, COMBAT, "pokemon_held_items", "held-items", "cobblemon:leftovers", 40, "key.realtimecobblemon.smash", "key.sneak", "key.use");
    }

    private static void add(ArcQuestRegistrationEvent.Guide event, ResourceLocation group, String path, String screenshot,
                            String item, int order, String... keys) {
        String prefix = "rumble.guide." + path;
        GuideText.Arg[] args = java.util.Arrays.stream(keys).map(key -> GuideText.Arg.of((player, context) -> Component.keybind(key)))
                .toArray(GuideText.Arg[]::new);
        var page = GuidePageBuilder.create().image(id("textures/guide/" + screenshot + ".png"), 224, 126)
                .description(GuideText.translatable(prefix + ".body", args));
        var guide = GuideBuilder.create(id(path)).category(GuideCategory.BASICS)
                .title(GuideText.translatable(prefix + ".title")).summary(GuideText.translatable(prefix + ".summary"))
                .icon(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item))).sortOrder(order).page(page).build();
        event.register(guide);
        event.assignGuideToGroup(id(path), group);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, path); }
}
