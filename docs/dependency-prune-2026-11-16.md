# NeoForge 开发环境精简（2026-11-16）

## 本次移除

| 类别 | 模组或包 |
| --- | --- |
| 点名移除 | Extra Move Animations、Cobblemon Raid Dens、Cobblemon Size Variation、Fight or Flight、SimpleTMs、SoulHome、TLC、YUNG's Better End Island / Ocean Monuments / Nether Fortresses、Cobblemon Move Tutor、Yuushya、Cobblemon Alphas、CobbleFurnies、BotanyPots、Asymmetric Battles、CobbleBash、Cobblemon Battle Tower、BerryPouch、Cobblemon Ultra Wormholes、Black/White Battle Music、Charta、Cobblemon Charms、CobbleBadges |
| **连带删除：依赖上述模组** | `cobblebash_screen` 硬依赖 `cobblebash`。`cobblebash` 本身硬依赖 `cobblebadges`；`cobblemonraiddens` 本身硬依赖 `asymmetricbattles`，两条链的两端都已在点名名单中。 |
| 连带删除：点名模组使用的库 | `bookshelf`、`prickle`（BotanyPots）；`matthiesen_core`（Move Tutor）；`yungsapi`（三个 YUNG 建筑）；`wild_battle_api`（Battle Tower、Ultra Wormholes）。 |
| 配套包 | `BotanyPots - Cobblemon Integrations.zip`、`CobblemonRaidDenDesigns-0.5_RELEASE.zip`、`berrypouch-interface.zip`。 |

Create 本体原本就没装；Ponder/Flywheel 由保留的 Arc-Quest 使用。保留 BadgeBox；Curios 仍用于 Accessories Compatibility Layer、Artifacts 与 Cobblemon Smartphone 的可选集成；RCT API 仍由 RCT Mod 与 Run & Bun AI 使用。Cobbreeding 对 Size Variation、Cobblemon Trials Edition 对 Fight or Flight 的兼容不构成硬依赖。

保留 `ATM x MSD [v5.0].zip` 和 `CR - Biome Expanded Spawns.zip`。前者在 `data/cobblemon/spawn_detail_presets/{nether_structures,ocean_monument}.json` 中移除了 YUNG 结构，只留原版结构。后者在 `data/cobblemon/tags/worldgen/structure/{nether_structures,ocean_monuments}.json` 中移除了可选 YUNG 条目，并在 `data/cobblemon/spawn_pool_world/` 的 Klink、Klang、Klinklang 文件中移除了仅通过 Create 方块匹配的刷怪分支。移出和修改前的原件备份在 `.gradle/migration/pruned-2026-11-16/`。

## 内置与独立加载

- 核心构建通过 NeoForge Jar-in-Jar 内置 `Cobbreeding`、`FSweep Stack Fix` 和 `MSD Special Move Fix`（上游标注 MIT）。开发环境仍从 `libs/` 各加载一次。内嵌副本保留原模组描述、资源及已有许可证，只补充 JVM 模块名；打包没有合并这些模组的类或资源。部署核心 JAR 时移除这三个模组的独立副本；它们需要的 Cobblemon、FSweep 等仍单独安装。
- 对照旧 `incandescentveritycore-1.0.0.jar` 的 `META-INF/jarjar/metadata.json`，上面三个 MIT 模组和 LMCN 都在旧版成品的内置清单里。未扩大内置范围，也不恢复已删或不兼容的模组。
- `lmcn` 原先外置。经作者「下次一定能出货」授权，核心保留译文、署名和原 ARR 许可证，重新实现传说遗迹的文本适配和蕾冠王对话。核心不读写 `lmcn.json`，也不保留全局玩家/NPC 会话表。Legendary Monuments 和 Cobblemon 仍独立加载；原 LMCN JAR 已移到 `.gradle/migration/integrated-lmcn/`，部署时也需移除独立副本。
- `XaeroClaims`、`CobbleEffortVR`、`FormTips`、`Hoverboards Pocket` 虽在参考项目 `libs-mit`，但当前 JAR 标为 ARR，保留外置。`rbrctai` 的 JAR 标为 MIT，但发布项目标为 AGPL，未确认该版本授权前保留外置。VirtualLoot 和旧版 Matthiesen Lib/API 没有恢复。

## 验证

- 旧客户端日志最后停在 Raid Dens 注册结构，没有记录 `OutOfMemoryError`，不足以判定卡住的原因。
- 精简后使用默认 `6G`：开发客户端在独立测试目录走创建世界界面，进入新单人世界并生成 `level.dat`；专用服务器使用新世界名也完成初始化。
- 用构建产物替代开发核心输出，并移除三个内嵌模组的外置副本后，NeoForge 仍发现三者，服务端启动到 `Done` 并保存新世界。该测试没有启动官方启动器。
- LMCN 集成后，简体中文开发客户端验证了「召唤急冻鸟」及脚印提示的「代拉基翁」，成功构造蕾冠王的纯台词和带选项对话，并创建、进入了新的单人世界。另用仅启用核心的专用服务器确认，缺少 Legendary Monuments 时也能建档启动。

## 烈空坐客户端渲染

- `crash-2026-11-16_20.20.24-client.txt` 的首个错误是 `LevelRenderer.renderLevel` 在实体阶段结束时抛出的 `Pose stack not empty`，不是 OOM。报告未记录实际释放的招式，也没有指向某个资源文件。
- Real-Time Cobblemon 在 `RenderLivingEvent.Pre` 入栈，并在 `Post` 出栈。如果后续监听器取消 `Pre`，NeoForge 不会发 `Post`。核心在取消路径清理遗留的实时战斗渲染状态并恢复栈，不改变正常渲染，也保留原版栈检查。隔离客户端用烈空坐和真实的 Real-Time Cobblemon 变形回调触发该取消路径，验证栈已恢复。
- 参考 `CobblemonEbonyPhantomCore/modpack_override/options.txt`，仅提高 `CqsAnimation.zip` 的优先级（放在 `mod_resources` 之后）。ZIP 与参考项目一致。未照搬整套顺序：旧 E19 小地图图标包高于模组资源时，会引用 Cobblemon 1.8.4 已不存在的 `sudowoodo.geo`，导致资源重载回退。独立客户端确认 Mega 烈空坐的 poser 由 `CqsAnimation.zip` 提供，并注册 `dragonascent`。
- 独立客户端也确认模型、resolver 和 `dragon_ascent` 动画文件来自 Cqs；通过实时战斗引擎让 Mega 烈空坐释放画龙点睛，招式进入冷却，客户端随后继续运行。该专项未重现旧存档中触发崩溃时的完整战斗状态。
