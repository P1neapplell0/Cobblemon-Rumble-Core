# ArcQuest 指南清单

均位于「基础」分类。在核心中注册，不设自动解锁；以后由任务阶段引用下表的稳定 ID。截图来自隔离 NeoForge 客户端，游戏显示语言为简体中文；正文和按键提示随客户端语言、按键绑定变化。

| 分组 | 指南 | ID | 游戏截图 |
| --- | --- | --- | --- |
| 入门操作 | 打开宝可梦详情与物品栏 | `cobblemon_rumble_core:pokemon_inventory` | [详情与携带物槽](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/pokemon-inventory.png) |
| 战斗基础 | 更换宝可梦招式 | `cobblemon_rumble_core:pokemon_moves` | [招式替换列表](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/pokemon-moves.png) |
| 战斗基础 | 实时战斗 | `cobblemon_rumble_core:realtime_combat` | [战斗视角与四个招式槽](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/realtime-combat.png) |
| 战斗基础 | 给予携带物 | `cobblemon_rumble_core:pokemon_held_items` | [互动轮盘](../src/main/resources/assets/cobblemon_rumble_core/textures/guide/held-items.png) |

当前宝可梦的物品入口是详情中的战斗携带物／外观饰品槽。给予战斗携带物使用潜行右键打开的互动轮盘。

可在测试世界用 `/arcquest guide grant @s cobblemon_rumble_core:pokemon_moves` 发给自己，再用 `/arcquest guide open @s cobblemon_rumble_core:pokemon_moves` 打开。代码入口为 `guide/RumbleGuides.java`；新增内容同步维护本清单和中英文语言键。
