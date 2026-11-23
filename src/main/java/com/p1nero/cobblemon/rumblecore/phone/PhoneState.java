package com.p1nero.cobblemon.rumblecore.phone;

import com.nbp.cobblemon_smartphone.item.SmartphoneItem;
import com.nbp.cobblemon_smartphone.util.SmartphoneHelper;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.prologue.PersonalDimension;
import com.p1nero.cobblemon.rumblecore.prologue.TrialWaystones;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhoneState {
    private static final String KEY = CobblemonRumbleCore.MOD_ID + ":phone";
    private static final ResourceLocation PHONE_ITEM = ResourceLocation.fromNamespaceAndPath("cobblemon_smartphone", "red_smartphone");
    private PhoneState() {}

    private static CompoundTag root(Player player) {
        // NeoForge copies PlayerPersisted on death; the grant and app permissions follow the player, not a phone item.
        return nested(nested(player.getPersistentData(), Player.PERSISTED_NBT_TAG), KEY);
    }

    private static CompoundTag nested(CompoundTag parent, String key) {
        CompoundTag value = parent.getCompound(key);
        parent.put(key, value);
        return value;
    }

    public static boolean install(ServerPlayer player, PhoneApp app) {
        if (!app.available()) {
            player.displayClientMessage(Component.translatable("rumble.phone.unavailable", app.title()), true);
            return false;
        }
        CompoundTag data = root(player);
        if (unlocked(player, app)) {
            player.displayClientMessage(Component.translatable("rumble.phone.already", app.title()), true);
            return false;
        }
        data.putBoolean(app.key(), true);
        sync(player);
        player.sendSystemMessage(Component.translatable("rumble.phone.installed", app.title()));
        return true;
    }

    public static boolean allow(ServerPlayer player, PhoneApp app) {
        if (unlocked(player, app) && hasPhone(player)) return true;
        player.displayClientMessage(Component.translatable("rumble.phone.locked", app.title()), true);
        return false;
    }

    public static boolean unlocked(ServerPlayer player, PhoneApp app) {
        return app.available() && (app.builtIn() || app == PhoneApp.WAYSTONE || app == PhoneApp.CLOUD && StarterStorage.granted(player)
                || root(player).getBoolean(app.key()));
    }

    public static boolean hasPhone(ServerPlayer player) {
        // Query the server player's own slots; the smartphone right-click context is client-global.
        if (player.getOffhandItem().getItem() instanceof SmartphoneItem
                || player.getInventory().items.stream().anyMatch(stack -> stack.getItem() instanceof SmartphoneItem)) return true;
        var compat = SmartphoneHelper.INSTANCE.getGetSmartphoneImpl();
        ItemStack stack = compat == null ? null : compat.invoke(player);
        return stack != null && stack.getItem() instanceof SmartphoneItem;
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = root(player);
        if (!data.getBoolean("starter_phone") && BuiltInRegistries.ITEM.containsKey(PHONE_ITEM)) {
            player.getInventory().placeItemBackInInventory(new ItemStack(BuiltInRegistries.ITEM.get(PHONE_ITEM)));
            data.putBoolean("starter_phone", true);
        }
        StarterStorage.grant(player);
        sync(player);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void sync(ServerPlayer player) {
        int mask = (TrialWaystones.travelUnlocked(player) ? TrialWaystones.STORY_TRAVEL_BIT : 0)
                | PersonalDimension.travelBits(player);
        for (PhoneApp app : PhoneApp.values()) if (unlocked(player, app)) mask |= app.bit();
        PacketDistributor.sendToPlayer(player, new PhoneSync(mask));
    }
}
