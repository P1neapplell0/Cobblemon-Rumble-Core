package com.p1nero.cobblemon.rumblecore.mixin;

import com.p1nero.cobblemon.rumblecore.mixin.legendarymonuments.MonumentTextSitesPlugin;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.localization.RealtimeTextSitesPlugin;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * 统一配置的加载入口：只负责可选模组门控及两个汉化字节码适配器的精确分派。
 * 具体 Mixin 继续按目标 mod id 和功能分包，服务端与客户端划分由 JSON 维护。
 */
public final class RumbleMixinPlugin implements IMixinConfigPlugin {
    private static final String ROOT = "com.p1nero.cobblemon.rumblecore.mixin.";
    private static final String MONUMENTS = ROOT + "legendarymonuments.";
    private static final String REALTIME_TEXT = ROOT + "realtimecobblemon.localization.";
    private final MonumentTextSitesPlugin monumentText = new MonumentTextSitesPlugin();
    private final RealtimeTextSitesPlugin realtimeText = new RealtimeTextSitesPlugin();

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals(ROOT + "cobbledollars.TrialMerchantInteraction")) {
            return FMLLoader.getLoadingModList().getModFileById("cobbledollars_villagers_overhaul_rca") != null;
        }
        if (mixinClassName.endsWith(".quark.HideConfigButton")) {
            return FMLLoader.getLoadingModList().getModFileById("quark") != null;
        }
        if (mixinClassName.endsWith(".zeta.HideConfigButton")) {
            return FMLLoader.getLoadingModList().getModFileById("zeta") != null;
        }
        // Mixin 初始化早于常规 ModList；使用加载阶段列表，保留原 requiredMods 的可选门控。
        if (mixinClassName.startsWith(ROOT + "fsweep.")) {
            return FMLLoader.getLoadingModList().getModFileById("fsweep") != null;
        }
        if (mixinClassName.startsWith(ROOT + "jade.")) {
            return FMLLoader.getLoadingModList().getModFileById("jade") != null;
        }
        if (mixinClassName.startsWith(ROOT + "jei.")) {
            return FMLLoader.getLoadingModList().getModFileById("jei") != null;
        }
        if (mixinClassName.startsWith(ROOT + "wildfire_gender.")) {
            return FMLLoader.getLoadingModList().getModFileById("wildfire_gender") != null;
        }
        return !mixinClassName.startsWith(MONUMENTS)
                || FMLLoader.getLoadingModList().getModFileById("legendarymonuments") != null;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
        // 旧适配器会重写目标类中的显示调用，不能对无关 Mixin 或同名后缀进行广泛匹配。
        if (mixinClassName.equals(MONUMENTS + "CommonTextSites")
                || mixinClassName.equals(MONUMENTS + "ClientTextSites")) {
            monumentText.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        } else if (mixinClassName.equals(REALTIME_TEXT + "CommonTextSites")
                || mixinClassName.equals(REALTIME_TEXT + "ClientTextSites")) {
            realtimeText.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass,
                                   String mixinClassName, IMixinInfo mixinInfo) {}
}
