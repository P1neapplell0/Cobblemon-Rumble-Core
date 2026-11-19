package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.prologue.client.PrologueClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;

@Mixin(value = ClientSmash.class, remap = false)
abstract class PrologueCombatKeys {
    @Redirect(method = "onTickPre", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetMouseButton(JI)I"), require = 2)
    private static int rumble$respectCombatBinding(long window, int button) {
        var options = Minecraft.getInstance().options;
        boolean attack = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean held = PrologueClient.allow(attack ? Cue.HEAVY : Cue.GUARD)
                && PrologueClient.held(window, attack ? options.keyAttack : options.keyUse);
        return held ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
    }

    @Redirect(method = "onTickPre", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private static boolean rumble$focusMoveBinding(KeyMapping key) { return PrologueClient.allow(Cue.MOVE) && key.isDown(); }
}
