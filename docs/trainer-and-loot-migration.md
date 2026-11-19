# 训练师与 Lootr 迁移

## 训练师与模拟机

- CobbleBash 0.2.0、CobbleBash Screen 0.1.3、RCT 0.19.2-beta、RCTAPI 0.16.2-beta、CobbleBadges、Curios、Architectury 和 Cobblemon NPC ArcQuest Compat 1.1.0 随整合包单独安装。版本和来源见[本地依赖清单](development-dependencies.tsv)。模拟宇宙使用原生菜单，主城入口保留剧情解锁条件。
- RCT 的 1,559 份合法化训练师数据直接来自 Incandescent Verity Core 的 `packs/civ_data/data/rctmod/trainers`。Rumble 将它作为必需、顶部优先级的数据包加载，路径为 `packs/legacy_trainer_teams`。
- 旧核心的 CobbleBash 0.1.5 JAR 与公开发布文件的 SHA-512 相同。旧 Screen 0.1.2 的内置数据包覆盖了 59 份道馆文本，配队与 0.1.5 相同。新版采用 CobbleBash 0.2.0 的 59 份分档道馆数据。隔离服加载现有数据包后，334 套队伍的 1,626 个成员均通过 RCTAPI 转换和当前 Cobblemon 形态招式表、特性列表的校验。
- `modpack_override/config/rctmod-server.toml` 仅在主世界生成训练师，每位玩家最多 2 名，全服最多 16 名。生成检查间隔 600–1,800 tick，脱离玩家视野 2,400 tick 后清理。关闭路过强制对战和训练师协会随机生成。落点搜索只访问已加载区块，试炼殿堂也拦截 RCT 的实体加入与训练师生成器。
- RCT 和 CobbleBash 保留原交互入口并接入 Rumble 实时战斗副本，胜利后复用各模组进度和奖励流程。场地和宝可币规则见 [主题竞技场](battle-arenas.md)。

## 地图奖励

Lootr 已恢复。42 个旧箱子的坐标、UUID 和奖励表引用均已在隔离服务器逐箱实载核对。其中 21 个使用 `my_loot:cobblemon_relic_coins`，12 个使用 `my_loot:cobblemon_potions_pokeballs`，9 个使用 `my_loot:cobblemon_ability_ev_items`。旧核心的 7 张 `my_loot` 奖励表都已迁移，地图继续从 `modpack_override/config` 部署。装饰改动见[逐项替换表](hall-block-replacements.md)。
