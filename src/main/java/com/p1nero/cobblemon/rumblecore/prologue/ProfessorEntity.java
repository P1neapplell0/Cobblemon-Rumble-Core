package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.shop.ProfessorCatalogue;
import com.p1nero.cobblemon.rumblecore.shop.RecyclingCatalogue;
import fr.harmex.cobbledollars.common.world.item.trading.CobbleDollarsShopHolder;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProfessorEntity extends PathfinderMob implements CobbleDollarsShopHolder {
    public static final double TALK_DISTANCE = 4.0;
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CobblemonRumbleCore.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ProfessorEntity>> TYPE =
            TYPES.register("professor", () -> EntityType.Builder.of(ProfessorEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(8).updateInterval(2).build("cobblemon_rumble_core:professor"));

    private ShopRef shopRef;
    private BankRef bankRef;
    private Set<Player> tradingPlayers = new HashSet<>();

    @Override public ShopRef getShopRef() {
        if (shopRef == null) shopRef = new ShopRef.Inline(ProfessorCatalogue.create());
        return shopRef;
    }
    @Override public BankRef getBankRef() {
        if (bankRef == null) bankRef = new BankRef.Inline(RecyclingCatalogue.create(getShopRef().getShop()));
        return bankRef;
    }
    @Override public void setShopRef(ShopRef value) { shopRef = value; }
    @Override public void setBankRef(BankRef value) { bankRef = value; }
    @Override public UUID getMerchantUUID() { return getUUID(); }
    @Override public void setMerchantUUID(UUID value) { /* Native menus use the entity's stable UUID. */ }
    @Override public Set<Player> getTradingPlayers() { return tradingPlayers; }
    @Override public void setTradingPlayers(Set<Player> players) { tradingPlayers = players; }

    public ProfessorEntity(EntityType<? extends ProfessorEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) ->
                event.put(TYPE.get(), createMobAttributes().build()));
    }

    @Override protected float tickHeadTurn(float movementYaw, float limbSwing) {
        // For a stationary actor the tracked rotation, not the direction of movement, drives the body.
        setYBodyRot(getYRot());
        return limbSwing;
    }

    @Override public boolean isPushable() { return false; }

    @Override public boolean canBeLeashed() { return false; }

    @Override protected Component getTypeName() { return Component.translatable("entity.cobblemon_rumble_core.professor"); }
}
