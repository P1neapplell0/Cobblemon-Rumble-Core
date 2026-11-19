# ArcQuest 指南清单

均位于「基础」分类。在核心中注册，由对应任务阶段解锁。截图来自隔离 NeoForge 客户端的英文界面，裁去了聊天、任务 HUD 和其他无关区域；正文与按键名随客户端设置变化。

| 分组 | 指南 | ID | 游戏截图 |
| --- | --- | --- | --- |
| 入门操作 | 打开宝可梦详情与物品栏 | `cobblemon_rumble_core:pokemon_inventory` | [详情与携带物槽](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/pokemon-inventory.png) |
| 入门操作 | 云仓库 | `cobblemon_rumble_core:cloud_storage` | [云仓库](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/cloud-storage.png) |
| 入门操作 | 虚拟牧场 | `cobblemon_rumble_core:virtual_pasture` | [27 格产物栏](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/virtual-pasture.png) |
| 战斗基础 | 更换宝可梦招式 | `cobblemon_rumble_core:pokemon_moves` | [招式替换列表](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/pokemon-moves.png) |
| 战斗基础 | 实时战斗 | `cobblemon_rumble_core:realtime_combat` | [战斗视角与四个招式槽](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/realtime-combat.png) |
| 战斗基础 | 给予携带物 | `cobblemon_rumble_core:pokemon_held_items` | [互动轮盘](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/held-items.png) |
| 战斗基础 | 努力值训练 | `cobblemon_rumble_core:effort_values` | [EV 页、储备点数和分配滑条](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/effort-values.png) |
| 战斗基础 | 招式学习机 | `cobblemon_rumble_core:technical_machines` | [杰尼龟学会冰冻光束](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/technical-machines.png) |
| 战斗基础 | Mega 进化 | `cobblemon_rumble_core:mega_evolution` | — |
| 战斗基础 | 太晶化与太晶碎块 | `cobblemon_rumble_core:terastallization` | — |
| 战斗基础 | 训练模拟机：模拟宇宙 | `cobblemon_rumble_core:training_simulator` | — |

当前宝可梦的物品入口是详情中的战斗携带物／外观饰品槽。给予战斗携带物使用潜行右键打开的互动轮盘。

虚拟牧场指南在第二馆首胜后与菜种确认领取赠礼时解锁。

努力值指南在完成「大菠博士的补货委托」后解锁。实时击倒野生宝可梦会结算其努力值产出，力量道具使结算多 8 点；整合包的 CobbleEffortVR 将这些点数加入储备池，玩家可在详情界面的 EV 页分配。

招式学习机指南随第四馆首胜的冰冻光束学习机解锁。

Mega 进化指南在第三馆后的介绍对白结束时解锁；太晶化指南在第五馆后向坂木展示太晶珠时解锁。两篇都使用实时战斗键位。模拟宇宙指南随「来测！」的汇报阶段完成解锁。

可在测试世界用 `/arcquest guide grant @s cobblemon_rumble_core:pokemon_moves` 发给自己，再用 `/arcquest guide open @s cobblemon_rumble_core:pokemon_moves` 打开。代码入口为 `guide/RumbleGuides.java`；新增内容同步维护本清单和中英文语言键。

## 新指南检查项

1. 读 ArcQuest 的指南开发文档及旧核心同类指南，再确认实际模组的 UI、操作和前置条件。以玩家手里真正拿到的注册物品作图标，把指南注册与任务解锁分开。
2. 标题、摘要、正文都提供 `en_us` 和 `zh_cn`；用 `Component.keybind` 配合 `%1$s` 等参数，不写死默认键位。
3. 用隔离 NeoForge 客户端切到 `en_us`，操作到真实界面再截 PNG。只裁留对应控件、标题和必要上下文，排除 HUD、聊天、JEI 和任务弹窗。用 `imageContained` 按原比例展示，检查读得清关键操作。
4. 用实际任务领取和 `/arcquest guide open` 分别验证一次。多物品的奖励提示用 `RewardNotice`，读物品的注册翻译名和实际数量。

任务和 NPC 的制作清单见 [ArcQuest 剧情规范](arcquest-story.md)。
