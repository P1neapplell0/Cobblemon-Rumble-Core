package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.misc.custom_instances.storage.record.RunRecord;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RunRecord.class, remap = false)
abstract class TeamDpsRecords {
    @Inject(method = "summaryFor", at = @At("HEAD"), cancellable = true)
    private void rumble$keepTeamSamples(UUID player, CallbackInfoReturnable<RunRecord> cir) {
        var run = (RunRecord) (Object) this;
        if (player == null && run.completeTeam() && run.battleReport() != null && run.battleReport().players().size() <= 6)
            cir.setReturnValue(run);
    }
}
