package com.p1nero.cobblemon.rumblecore;

import com.p1nero.cobblemon.rumblecore.item.PokeBrick;
import com.p1nero.cobblemon.rumblecore.phone.PhoneItems;
import com.p1nero.cobblemon.rumblecore.prologue.MayMegaDevice;
import com.p1nero.cobblemon.rumblecore.prologue.PersonalDimension;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The discoverable creative inventory for items supplied by Rumble Core. */
public final class RumbleCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CobblemonRumbleCore.MOD_ID);
    static {
        TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon_rumble_core.main"))
                    .icon(() -> new ItemStack(PokeBrick.ITEM.get()))
                    .displayItems((parameters, output) -> addItems(output::accept))
                    .build());
    }

    private RumbleCreativeTab() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private static void addItems(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PokeBrick.ITEM.get()));
        output.accept(new ItemStack(MayMegaDevice.ITEM.get()));
        output.accept(new ItemStack(PersonalDimension.INGOT.get()));
        PhoneItems.addCreativeItems(output);
    }
}
