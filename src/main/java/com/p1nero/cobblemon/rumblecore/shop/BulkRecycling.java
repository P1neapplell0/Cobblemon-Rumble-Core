package com.p1nero.cobblemon.rumblecore.shop;

import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorShop;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import fr.harmex.cobbledollars.common.api.bank.Bank;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import fr.harmex.cobbledollars.common.world.inventory.BankMenu;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Quote and settle against the open bank on the server thread. No items or prices come from the client. */
final class BulkRecycling {
    private static final Map<BankMenu, Plan> QUOTES = new WeakHashMap<>();
    private static int nextQuote;

    private BulkRecycling() {}

    static void handle(ServerPlayer player, RecyclingPackets.Request request) {
        if (!(player.containerMenu instanceof BankMenu menu) || menu.containerId != request.menuId()
                || !(menu.getCobbleMerchant() instanceof ProfessorEntity doctor) || !ProfessorShop.canTrade(player, doctor)) {
            reply(player, request.menuId(), 0, "unavailable", BigInteger.ZERO, BigInteger.ZERO);
            return;
        }
        Plan approved = QUOTES.remove(menu);
        if (request.quote() != 0 && (approved == null || request.quote() != approved.id())) {
            reply(player, menu.containerId, 0, "stale", BigInteger.ZERO, BigInteger.ZERO);
            return;
        }
        var selection = request.quote() == 0 ? Set.copyOf(request.selection()) : approved.selection();
        Plan fresh = plan(player, menu.getBankRef().getBank(), selection);
        if (request.quote() == 0 || !approved.matches(fresh)) {
            boolean empty = fresh.inventory().isEmpty() && fresh.cloud().isEmpty();
            if (!empty) QUOTES.put(menu, fresh);
            reply(player, menu.containerId, empty ? 0 : fresh.id(), empty ? "nothing" : request.quote() == 0 ? "quote" : "changed",
                    fresh.inventoryValue(), fresh.storageValue());
            return;
        }
        var inventory = player.getInventory();
        BigInteger inventoryValue = BigInteger.ZERO;
        for (var line : fresh.inventory()) {
            int removed = inventory.removeItem(line.slot(), line.count()).getCount();
            inventoryValue = inventoryValue.add(line.price().multiply(BigInteger.valueOf(removed)));
        }
        BigInteger storageValue = BigInteger.ZERO;
        if (fresh.storage() != null) for (var line : fresh.cloud()) {
            var removed = fresh.storage().extract(line.key(), line.count(), false, false);
            storageValue = storageValue.add(line.price().multiply(BigInteger.valueOf(removed.amount())));
        }
        var total = inventoryValue.add(storageValue);
        if (total.signum() > 0) {
            PlayerExtensionKt.setCobbleDollars(player, PlayerExtensionKt.getCobbleDollars(player).add(total));
            inventory.setChanged();
            menu.broadcastChanges();
        }
        reply(player, menu.containerId, 0, "sold", inventoryValue, storageValue);
    }

    private static Plan plan(ServerPlayer player, Bank bank, Set<ResourceLocation> selection) {
        List<InventoryLine> inventory = new ArrayList<>();
        var slots = player.getInventory().items;
        for (int i = 0; i < slots.size(); i++) {
            var stack = slots.get(i);
            var price = price(bank, stack, selection);
            if (price.signum() > 0) inventory.add(new InventoryLine(i, stack.getItem(), stack.getCount(), price));
        }
        var net = DimensionsNet.getPrimaryNetFromPlayer(player);
        var storage = net != null && net.isOwner(player) ? net.getUnifiedStorage() : null;
        List<StorageLine> cloud = new ArrayList<>();
        if (storage != null) for (var entry : storage.getStorage()) {
            if (!(entry.key() instanceof ItemStackKey key) || entry.amount() <= 0) continue;
            var price = price(bank, key.getReadOnlyStack(), selection);
            if (price.signum() <= 0) continue;
            var available = storage.extract(key, entry.amount(), true, false);
            if (available.amount() > 0 && key.equals(available.key())) cloud.add(new StorageLine(key, available.amount(), price));
        }
        if (++nextQuote <= 0) nextQuote = 1;
        cloud.sort(Comparator.comparing(line -> BuiltInRegistries.ITEM.getKey(line.key().getReadOnlyStack().getItem())));
        return new Plan(nextQuote, selection, List.copyOf(inventory), storage, List.copyOf(cloud));
    }

    private static BigInteger price(Bank bank, ItemStack stack, Set<ResourceLocation> selection) {
        return stack.isEmpty() || !selection.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                || !ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance())
                ? BigInteger.ZERO : bank.getPrice(stack);
    }

    private static void reply(ServerPlayer player, int menu, int quote, String status, BigInteger bag, BigInteger cloud) {
        PacketDistributor.sendToPlayer(player, new RecyclingPackets.Result(menu, quote, status, bag.toString(), cloud.toString()));
    }

    private record InventoryLine(int slot, Item item, int count, BigInteger price) {}
    private record StorageLine(ItemStackKey key, long count, BigInteger price) {}
    private record Plan(int id, Set<ResourceLocation> selection, List<InventoryLine> inventory, UnifiedStorage storage, List<StorageLine> cloud) {
        boolean matches(Plan other) { return storage == other.storage && inventory.equals(other.inventory) && cloud.equals(other.cloud); }
        BigInteger inventoryValue() {
            return inventory.stream().map(line -> line.price().multiply(BigInteger.valueOf(line.count())))
                    .reduce(BigInteger.ZERO, BigInteger::add);
        }
        BigInteger storageValue() {
            return cloud.stream().map(line -> line.price().multiply(BigInteger.valueOf(line.count())))
                    .reduce(BigInteger.ZERO, BigInteger::add);
        }
    }
}
