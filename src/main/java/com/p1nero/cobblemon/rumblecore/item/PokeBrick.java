package com.p1nero.cobblemon.rumblecore.item;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PokeBrick extends Item {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonRumbleCore.MOD_ID);
    public static final DeferredItem<PokeBrick> ITEM = ITEMS.registerItem("pokebrick", PokeBrick::new);

    private PokeBrick(Properties properties) {
        super(properties);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(PokeBrick::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) event.accept(ITEM);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".description").withStyle(ChatFormatting.GRAY));
    }
}
