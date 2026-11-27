package com.p1nero.cobblemon.rumblecore.phone;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PhoneItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonRumbleCore.MOD_ID);
    private static final Supplier<Item> BLANK = ITEMS.register("sd_card", () -> new Item(new Item.Properties().stacksTo(1)));
    private static final EnumMap<PhoneApp, Supplier<AppCard>> CARDS = new EnumMap<>(PhoneApp.class);

    static {
        for (PhoneApp app : PhoneApp.values()) if (!app.builtIn()) CARDS.put(app, ITEMS.register(app.itemId(), () -> new AppCard(app)));
    }

    private PhoneItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(PhoneItems::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) return;
        addCreativeItems(event::accept);
    }

    /** Shared by the vanilla tools tab and the mod's dedicated creative tab. */
    public static void addCreativeItems(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BLANK.get()));
        CARDS.values().forEach(card -> output.accept(new ItemStack(card.get())));
    }

    private static final class AppCard extends Item {
        private final PhoneApp app;
        private AppCard(PhoneApp app) { super(new Properties().stacksTo(1)); this.app = app; }

        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer serverPlayer) {
                if (!PhoneState.install(serverPlayer, app)) return InteractionResultHolder.fail(stack);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }
}
