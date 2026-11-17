package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.localization;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Redirect display calls, not string constants, so the underlying game logic and payloads are unchanged. */
public final class RealtimeTextSitesPlugin implements IMixinConfigPlugin {
    private static final String BRIDGE = "com/p1nero/cobblemon/rumblecore/compat/realtime/localization/";
    private static final String CHAT = "net/minecraft/network/chat/";
    private static final String STRING = "Ljava/lang/String;";
    private static final String MUTABLE = "L" + CHAT + "MutableComponent;";
    private static final String FROM_STRING = "(" + STRING + ")" + MUTABLE;
    private static final String RTC = "com/johnn/realtimecobblemon/";

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        boolean client = mixin.endsWith("ClientTextSites");
        for (var method : node.methods) {
            for (var insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) continue;
                if (call.owner.equals(CHAT + "Component") && call.name.equals("literal") && call.desc.equals(FROM_STRING)) {
                    if (node.name.endsWith("/ClashHud") && method.name.equals("tile")) call.name = "moveName";
                    else if (node.name.endsWith("/WorldText")) call.name = "popup";
                    call.owner = BRIDGE + "RealtimeText";
                    call.itf = false;
                } else if (!client && call.owner.equals(CHAT + "MutableComponent") && call.name.equals("append") && call.desc.equals(FROM_STRING)) {
                    call.owner = BRIDGE + "RealtimeText";
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.desc = "(" + MUTABLE + STRING + ")" + MUTABLE;
                } else if (client && call.owner.equals(RTC + "client/Fonts") && call.desc.equals(FROM_STRING)) {
                    call.owner = BRIDGE + "RealtimeHudText";
                }
            }
        }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
