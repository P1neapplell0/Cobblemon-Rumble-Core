package com.p1nero.cobblemon.rumblecore.mixin.waystones;

import com.p1nero.cobblemon.rumblecore.prologue.TrialWaystones;
import java.util.UUID;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneOrigin;
import net.blay09.mods.waystones.block.entity.WaystoneBlockEntityBase;
import net.blay09.mods.waystones.core.WaystoneProxy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WaystoneBlockEntityBase.class, remap = false)
public abstract class FixedStoneIdentity {
    @Shadow private Waystone waystone;
    @Shadow private UUID waystoneUid;

    @Inject(method = "loadBackingWaystone", at = @At("HEAD"))
    private void rumble$restoreOnLoad(CallbackInfoReturnable<Waystone> cir) {
        var entity = (BlockEntity) (Object) this;
        if (entity.getLevel() instanceof ServerLevel level) rumble$restore(level, entity);
    }

    // Hall setup can call this before Waystones has resolved the block entity's saved UUID.
    @Inject(method = "initializeWaystone", at = @At("HEAD"))
    private void rumble$beforeNewId(ServerLevelAccessor level, LivingEntity owner, WaystoneOrigin origin, CallbackInfo ci) {
        if (!waystone.isValid()) rumble$restore(level.getLevel(), (BlockEntity) (Object) this);
    }

    @Unique
    private void rumble$restore(ServerLevel level, BlockEntity entity) {
        UUID preferred = waystone.isValid() ? waystone.getWaystoneUid() : waystoneUid;
        var restored = TrialWaystones.restoreFixedStone(level, entity.getBlockPos(), preferred);
        if (restored == null) return;
        var id = restored.getWaystoneUid();
        // A proxy keeps the upper half from moving the registry entry away from the base.
        this.waystone = new WaystoneProxy(level.getServer(), id);
        if (!id.equals(waystoneUid)) {
            this.waystoneUid = id;
            entity.setChanged();
        }
    }
}
