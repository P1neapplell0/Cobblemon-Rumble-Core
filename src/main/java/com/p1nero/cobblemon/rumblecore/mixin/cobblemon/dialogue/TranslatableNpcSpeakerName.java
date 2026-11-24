package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.dialogue;

import com.cobblemon.mod.common.api.dialogue.ActiveDialogue;
import com.cobblemon.mod.common.api.dialogue.ExpressionLikeDialogueText;
import com.cobblemon.mod.common.api.molang.ExpressionLike;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ExpressionLikeDialogueText.class, remap = false)
abstract class TranslatableNpcSpeakerName {
    @Shadow @Final private ExpressionLike expression;

    @Inject(method = "invoke(Lcom/cobblemon/mod/common/api/dialogue/ActiveDialogue;)Lnet/minecraft/network/chat/MutableComponent;",
            at = @At("HEAD"), cancellable = true)
    private void rumble$preserveNpcNameTranslation(ActiveDialogue dialogue, CallbackInfoReturnable<MutableComponent> cir) {
        if (dialogue.getNpc() == null || !"q.npc.name".equals(expression.getString())) return;
        var name = dialogue.getNpc().getName();
        // Do not resolve a translatable speaker name in the server's language.
        if (name.getContents() instanceof TranslatableContents) cir.setReturnValue(name.copy());
    }
}
