package com.p1nero.cobblemon.rumblecore.guide;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.guide.api.GuideCategory;
import org.arcadia.arc_quest.guide.api.GuideGroupDefinition;
import org.arcadia.arc_quest.guide.api.GuideText;
import org.arcadia.arc_quest.guide.builder.GuideBuilder;
import org.arcadia.arc_quest.guide.builder.GuidePageBuilder;
import org.arcadia.arc_quest.guide.runtime.GuideUnlockService;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;

/** Registration is independent of unlock timing; quest phases can grant these stable IDs later. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class RumbleGuides {
    private static final ResourceLocation START = id("getting_started");
    private static final ResourceLocation COMBAT = id("battle_foundations");
    private static final List<ResourceLocation> PARTNER_GUIDES = List.of(id("pokemon_inventory"), id("pokemon_moves"), id("pokemon_evolution"));
    public static final ResourceLocation REALTIME = id("realtime_combat");
    public static final ResourceLocation EFFORT_VALUES = id("effort_values");
    public static final ResourceLocation CLOUD_STORAGE = id("cloud_storage");
    public static final ResourceLocation MEGA = id("mega_evolution");
    public static final ResourceLocation TERA = id("terastallization");
    public static final ResourceLocation SIMULATOR = id("training_simulator");
    public static final ResourceLocation VIRTUAL_PASTURE = id("virtual_pasture");

    public static PhaseBuilder partnerGrants(PhaseBuilder phase) {
        PARTNER_GUIDES.forEach(phase::grantGuideOnComplete);
        return phase;
    }

    public static void grantPartnerGuides(ServerPlayer player) { new GuideUnlockService().grantAll(player, PARTNER_GUIDES); }
    public static void grantRealtimeGuide(ServerPlayer player) { new GuideUnlockService().grant(player, REALTIME); }

    private RumbleGuides() {}

    @SubscribeEvent
    public static void register(ArcQuestRegistrationEvent.Guide event) {
        event.registerGroup(new GuideGroupDefinition(START, Component.translatable("rumble.guide.group.getting_started"), 0, 0x57C7A2));
        event.registerGroup(new GuideGroupDefinition(COMBAT, Component.translatable("rumble.guide.group.battle_foundations"), 10, 0xF7A658));
        add(event, START, "cloud_storage", "cloud-storage", "beyonddimensions:net_terminal_item", 5, "key.beyonddimensions.open_gui");
        add(event, START, "virtual_pasture", "virtual-pasture", "virtualloot:virtual_pasture", 35, "key.use", "key.sneak");
        add(event, START, "pokemon_inventory", "pokemon-inventory", "cobblemon:pokedex_red", 10,
                "key.cobblemon.summary", "key.realtimecobblemon.summary");
        add(event, COMBAT, "pokemon_moves", "pokemon-moves", "minecraft:book", 20, "key.cobblemon.summary");
        add(event, START, "pokemon_evolution", null, "cobblemon:leaf_stone", 30, "key.cobblemon.summary");
        add(event, COMBAT, "realtime_combat", "realtime-combat", "cobblemon:poke_ball", 30,
                "key.cobblemon.throwpartypokemon", "key.attack", "key.realtimecobblemon.dodge", "key.realtimecobblemon.lock_on", "key.realtimecobblemon.smash",
                "key.hotbar.1", "key.hotbar.2", "key.hotbar.3", "key.hotbar.4", "key.use");
        add(event, COMBAT, "pokemon_held_items", "held-items", "cobblemon:leftovers", 40, "key.realtimecobblemon.smash", "key.sneak", "key.use");
        add(event, COMBAT, "effort_values", "effort-values", "cobblemon:power_weight", 50, "key.cobblemon.summary");
        add(event, COMBAT, "mega_evolution", null, "mega_showdown:mega_bracelet", 60, "key.cobblemon_rumble_core.use_held");
        add(event, COMBAT, "terastallization", null, "mega_showdown:tera_orb", 65, "key.cobblemon_rumble_core.tera", "key.use");
        add(event, COMBAT, "training_simulator", null, "cobblebash:training_simulator", 70, "key.use");
        add(event, COMBAT, "technical_machines", "technical-machines", "cobblemon:technical_machine", 55,
                "key.cobblemon.throwpartypokemon", "key.use", "key.cobblemon.summary");
    }

    private static void add(ArcQuestRegistrationEvent.Guide event, ResourceLocation group, String path, String screenshot,
                            String item, int order, String... keys) {
        String prefix = "rumble.guide." + path;
        GuideText.Arg[] args = Arrays.stream(keys).map(key -> GuideText.Arg.of((player, context) -> Component.keybind(key)))
                .toArray(GuideText.Arg[]::new);
        var page = GuidePageBuilder.create();
        if (screenshot == null) {
            page.none();
        } else {
            int height = switch (screenshot) {
                case "pokemon-inventory", "pokemon-moves" -> 112;
                case "effort-values" -> 120;
                case "cloud-storage" -> 219;
                case "virtual-pasture" -> 92;
                case "held-items" -> 222;
                case "technical-machines" -> 155;
                default -> 126;
            };
            page.imageContained(id("textures/guide/" + screenshot + ".png"), 224, height);
        }
        page.description(GuideText.translatable(prefix + ".body", args));
        var guide = GuideBuilder.create(id(path)).category(GuideCategory.BASICS)
                .title(GuideText.translatable(prefix + ".title")).summary(GuideText.translatable(prefix + ".summary"))
                .icon(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item))).sortOrder(order).page(page).build();
        event.register(guide);
        event.assignGuideToGroup(id(path), group);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, path); }
}
