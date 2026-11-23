package com.p1nero.cobblemon.rumblecore.prologue;

import com.github.yajatkaul.mega_showdown.item.custom.gimmick.MegaBracelet;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** May's anklet uses Mega Showdown's Mega accessory behavior. */
public final class MayMegaDevice {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonRumbleCore.MOD_ID);
    public static final java.util.function.Supplier<Item> ITEM = ITEMS.register("may_mega_device",
            () -> new MegaBracelet(new Item.Properties().stacksTo(1)));

    private MayMegaDevice() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
