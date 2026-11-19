package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.github.yajatkaul.mega_showdown.components.MegaShowdownDataComponents;
import com.github.yajatkaul.mega_showdown.config.MegaShowdownConfig;
import com.github.yajatkaul.mega_showdown.item.custom.tera.LikosPendant;
import com.github.yajatkaul.mega_showdown.utils.PokemonBehaviourHelper;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.MegaCombat;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotReference;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MayAnklet {
    private static final String SUMMON = "rumble.may_anklet";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonRumbleCore.MOD_ID);
    public static final java.util.function.Supplier<Item> ITEM = ITEMS.register("may_anklet",
            () -> MegaCombat.available() ? new Charging(new Item.Properties().stacksTo(1)) : new Item(new Item.Properties().stacksTo(1)));
    private static final java.util.function.Supplier<Item> AWAKENED = ITEMS.register("may_anklet_awakened", () -> new Awakened(new Item.Properties().stacksTo(1)));

    private MayAnklet() {}
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(MayAnklet::setup);
    }

    private static void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        if (MegaCombat.available()) event.enqueueWork(Charging::registerAccessory);
    }

    private static final class Charging extends LikosPendant {
        Charging(Properties properties) { super(properties); }

        static void registerAccessory() {
            AccessoriesAPI.registerAccessory(ITEM.get(), new Accessory() {
                @Override public void tick(ItemStack stack, SlotReference slot) {
                    var wearer = slot.entity();
                    stack.inventoryTick(wearer.level(), wearer, -1, false);
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("rumble.may_anklet.charging"));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
            if (level.isClientSide || stack.isEmpty()) return;
            var component = MegaShowdownDataComponents.LIKO_PENDANT_TICK_COMPONENT.get();
            int remaining = stack.getOrDefault(component, MegaShowdownConfig.likoPendentDuration) - 1;
            if (remaining > 0) {
                stack.set(component, remaining);
                if (remaining % 20 == 0) stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(ticksToTime(remaining)))));
                return;
            }
            var awakened = AWAKENED.get().getDefaultInstance();
            if (holder instanceof Player player) {
                var inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (inventory.getItem(i) != stack) continue;
                    inventory.setItem(i, awakened);
                    stack.setCount(0);
                    ready(player);
                    return;
                }
            }
            if (holder instanceof LivingEntity living) {
                var accessories = AccessoriesCapability.get(living);
                if (accessories != null) for (var container : accessories.getContainers().values()) {
                    var inv = container.getAccessories();
                    for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i) == stack) {
                        inv.setItem(i, ItemStack.EMPTY);
                        container.markChanged();
                        break;
                    }
                }
            }
            stack.setCount(0);
            if (holder instanceof Player p) {
                p.getInventory().add(awakened);
                if (!awakened.isEmpty()) p.drop(awakened, false);
                ready(p);
            } else holder.spawnAtLocation(awakened);
        }

        private static void ready(Player player) {
            player.getInventory().setChanged();
            player.displayClientMessage(Component.translatable("rumble.may_anklet.ready"), false);
        }
    }

    private static final class Awakened extends Item {
        Awakened(Properties properties) { super(properties); }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("rumble.may_anklet.summon"));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(level instanceof ServerLevel server) || !MegaCombat.available()
                    || PrologueWorld.blocksAmbientSpawns(level) || InstanceApi.find(server).isPresent()) return InteractionResultHolder.fail(stack);
            var entity = PokemonProperties.Companion.parse("terapagos").createEntity(level);
            var pokemon = entity.getPokemon();
            pokemon.getPersistentData().putBoolean(SUMMON, true);
            for (Stats stat : List.of(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED)) pokemon.getIvs().set(stat, 31);
            pokemon.setTeraType(TeraTypes.getSTELLAR());
            pokemon.heal();
            if (player.getRandom().nextFloat() * Cobblemon.config.getShinyRate() < 1) pokemon.setShiny(true);
            entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
            double yaw = Math.toRadians(player.getYRot());
            entity.moveTo(player.getX() - Math.sin(yaw) * 2, player.getY(), player.getZ() + Math.cos(yaw) * 2, player.getYRot(), 0);
            entity.setNoAi(true);
            if (!level.addFreshEntity(entity)) return InteractionResultHolder.fail(stack);
            stack.shrink(1);
            PokemonBehaviourHelper.Companion.snowStormPartileSpawner(entity, ResourceLocation.parse("cobblemon:pendant_effect"), List.of("target"), null, null);
            BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("mega_showdown:terapagos_spawn")).ifPresent(sound ->
                    level.playSound(null, entity.blockPosition(), sound, SoundSource.PLAYERS, 0.6F, 1));
            return InteractionResultHolder.consume(stack);
        }
    }

    public static boolean guaranteesCapture(PokemonEntity entity) {
        var pokemon = entity.getPokemon();
        return !entity.level().isClientSide && pokemon.isWild() && pokemon.getPersistentData().getBoolean(SUMMON)
                && pokemon.getSpecies().getResourceIdentifier().equals(ResourceLocation.parse("cobblemon:terapagos"))
                && UncatchableProperty.INSTANCE.isCatchable(entity);
    }
}
