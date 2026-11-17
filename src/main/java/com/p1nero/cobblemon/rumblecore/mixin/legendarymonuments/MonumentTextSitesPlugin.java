package com.p1nero.cobblemon.rumblecore.mixin.legendarymonuments;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Redirect just the listed upstream call sites, including lambda bodies and synthetic methods. */
public final class MonumentTextSitesPlugin implements IMixinConfigPlugin {
    private static final String PREFIX = "com/p1nero/cobblemon/rumblecore/compat/legendarymonuments/localization/";
    private static final String STRING = "Ljava/lang/String;";
    private static final String OBJECTS = "[Ljava/lang/Object;";
    private static final String MUTABLE = "Lnet/minecraft/network/chat/MutableComponent;";
    private static final String FONT = "net/minecraft/client/gui/Font";
    private static final String GUI = "net/minecraft/client/gui/GuiGraphics";
    private static final String DRAW = "(L" + FONT + ";" + STRING + "III";
    private static final Set<String> COMPONENT_FACTORIES = Set.of(
            "literal(" + STRING + ")" + MUTABLE,
            "translatable(" + STRING + OBJECTS + ")" + MUTABLE,
            "translatableWithFallback(" + STRING + STRING + OBJECTS + ")" + MUTABLE);

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!mixin.endsWith("TextSites")) return;
        boolean client = mixin.endsWith("ClientTextSites");
        for (var method : node.methods) {
            for (var insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) continue;
                String signature = call.name + call.desc;
                if (call.owner.equals("net/minecraft/network/chat/Component") && COMPONENT_FACTORIES.contains(signature)) {
                    redirect(call, "MonumentText", "");
                } else if (client && call.owner.equals(GUI) && (signature.equals("drawString" + DRAW + ")I")
                        || signature.equals("drawString" + DRAW + "Z)I") || signature.equals("drawCenteredString" + DRAW + ")V"))) {
                    redirect(call, "MonumentGuiText", "L" + GUI + ";");
                } else if (client && call.owner.equals(FONT) && signature.equals("width(" + STRING + ")I")) {
                    redirect(call, "MonumentGuiText", "L" + FONT + ";");
                }
            }
        }
    }

    private static void redirect(MethodInsnNode call, String bridge, String receiver) {
        call.setOpcode(Opcodes.INVOKESTATIC);
        call.owner = PREFIX + bridge;
        call.itf = false;
        call.desc = "(" + receiver + call.desc.substring(1);
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
