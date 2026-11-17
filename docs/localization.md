# 内置模组汉化

- `src/main/resources/packs/mod_translations` 通过 NeoForge 作为客户端内置资源包自动加载。该包只有 `assets/*/lang/`，不改变玩法数据，也不覆盖动画、模型或纹理。
- 旧译文来自 `CobblemonEbonyPhantomCore`（IncandescentVerityCore）的 `src/main/resources/packs/i18n` 和 `src/main/resources/packs/zh_translation`。按当前依赖保留译文，冲突键沿用旧顺序，以 `zh_translation` 覆盖 `i18n`。独立的 Legendary Monuments CN 兼容和授权见项目 README。
- Real-Time Cobblemon `1.0.0-alpha.1` 的原语言键统一收录到 `assets/realtimecobblemon/lang/zh_cn.json`。没有上游语言键的界面标签、战斗消息、作业及命令提示由 `compat/realtime/localization` 适配；新增键以 `realtimecobblemon.rumble.` 为前缀，英文原文及回退在 `src/main/resources/realtime-localization/en_us.json`。战斗消息由接收方客户端按自己的语言显示，招式名复用 Cobblemon 译文。
- 升级 Real-Time Cobblemon 时，对照上游 `en_us.json` 和目标显示方法复核。动态提示的英文模板按原顺序使用 `%s`，中文可用 `%1$s`、`%2$s` 调整语序。命令语法、配置名以及日志不纳入游戏内汉化。
