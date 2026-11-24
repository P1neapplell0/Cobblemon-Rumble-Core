package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.misc.custom_instances.storage.CompletionStore;
import com.p1nero.misc.custom_instances.storage.record.RunRecord;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CompletionStore.class, remap = false)
abstract class SoloBestRecords {
    private static boolean rumble$solo(RunRecord run) {
        return run != null && run.startingTeam().size() == 1 && run.participants().size() == 1;
    }

    @ModifyExpressionValue(method = "lambda$recordRun$15", at = @At(value = "INVOKE", target =
            "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object rumble$ignoreLegacyBest(Object previous) {
        return previous instanceof RunRecord run && rumble$solo(run) ? previous : null;
    }

    @WrapOperation(method = "lambda$recordRun$15", at = @At(value = "INVOKE", target =
            "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object rumble$writeOnlySolo(Map<?, ?> map, Object player, Object record, Operation<Object> original) {
        return record instanceof RunRecord run && rumble$solo(run) ? original.call(map, player, record) : null;
    }

    @Inject(method = "personalBest", at = @At("RETURN"), cancellable = true)
    private void rumble$soloBest(String id, UUID player, CallbackInfoReturnable<RunRecord> cir) {
        if (!rumble$solo(cir.getReturnValue())) cir.setReturnValue(null);
    }

    @ModifyExpressionValue(method = "leaderboard", at = @At(value = "INVOKE", ordinal = 1, target =
            "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private Set<Map.Entry<UUID, RunRecord>> rumble$filterBeforeRankLimit(Set<Map.Entry<UUID, RunRecord>> rows) {
        return rows.stream().filter(row -> rumble$solo(row.getValue())).collect(Collectors.toSet());
    }
}
