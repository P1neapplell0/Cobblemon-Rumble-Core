# 内置模组汉化

- `src/main/resources/packs/mod_translations` 通过 NeoForge 作为客户端内置资源包自动加载。该包只有 `assets/*/lang/`，不改变玩法数据，也不覆盖动画、模型或纹理。
- 旧译文来自 `CobblemonEbonyPhantomCore`（IncandescentVerityCore）的 `src/main/resources/packs/i18n` 和 `src/main/resources/packs/zh_translation`。按当前依赖保留译文，冲突键沿用旧顺序，以 `zh_translation` 覆盖 `i18n`。独立的 Legendary Monuments CN 兼容和授权见项目 README。
- Real-Time Cobblemon `1.0.0-alpha.1` 的原语言键统一收录到 `assets/realtimecobblemon/lang/zh_cn.json`。没有上游语言键的界面标签、战斗消息、作业及命令提示由 `compat/realtime/localization` 适配；新增键以 `realtimecobblemon.rumble.` 为前缀，英文原文及回退在 `src/main/resources/realtime-localization/en_us.json`。战斗消息由接收方客户端按自己的语言显示，招式名复用 Cobblemon 译文。
- 升级 Real-Time Cobblemon 时，对照上游 `en_us.json` 和目标显示方法复核。动态提示的英文模板按原顺序使用 `%s`，中文可用 `%1$s`、`%2$s` 调整语序。命令语法、配置名以及日志不纳入游戏内汉化。

## Cobblemon Additions 4.3.0 NPC

- `mod_translations/assets/bca/lang` 保留旧项目 `i18n` 的中英文 NPC 名称和对话译文。上游 NPC 名称已经是语言键，不迁移旧项目里把名字写死为中文的 NPC 定义。
- 旧项目 `pokeverie_data` 中的 25 个对话覆盖单独装入 `packs/bca_dialogues`，仅在检测到 Additions 时由服务端自动加载。台词、选项和固定说话人改为语言键；保留上游的选项行为、页面跳转、治疗和战斗脚本。联机时服务端与客户端都需要安装核心。
- `q.npc.name` 在对话中原本会由服务端解析为纯文本。核心保留这种独立名称表达式的可翻译组件，使对话标题与 NPC 名牌使用同一语言。结构里写死的乔伊与商店店员名称在实体进入世界时转换为翻译键，新旧存档均适用。世界生成、商品、队伍及战斗规则未改动。
