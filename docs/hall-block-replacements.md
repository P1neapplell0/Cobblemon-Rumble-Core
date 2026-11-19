# 试炼殿堂地图方块替换

主城和教学场使用 `modpack_override/config` 中的区块模板。表中括号是各模板内的替换格数。`lootr:lootr_chest` 的原方块、坐标、容器 UUID 和奖励表引用均保留。

| 原方块 ID | 主城替代方块（格） | 教学场替代方块（格） |
| --- | --- | --- |
| `cobblefurnies:black_chair` | `quark:black_stool` (1) | — |
| `cobblefurnies:black_dark_chair` | `quark:black_stool` (2) | — |
| `cobblefurnies:black_sink` | `minecraft:cauldron` (2) | — |
| `cobblefurnies:black_sofa` | `minecraft:polished_blackstone_stairs` (15) | — |
| `cobblefurnies:black_stove` | `minecraft:smoker` (2) | — |
| `cobblefurnies:light_blue_sink` | `minecraft:cauldron` (1) | — |
| `cobblefurnies:light_blue_sofa` | `minecraft:prismarine_stairs` (10) | — |
| `cobblefurnies:light_blue_stove` | `minecraft:smoker` (1) | — |
| `cobblefurnies:light_toilet` | `minecraft:cauldron` (1) | — |
| `cobblefurnies:purple_dark_chair` | `quark:purple_stool` (6) | — |
| `cobblefurnies:purple_sink` | `minecraft:cauldron` (2) | — |
| `cobblefurnies:purple_sofa` | `minecraft:purpur_stairs` (5) | — |
| `cobblefurnies:red_dark_chair` | `quark:red_stool` (2) | — |
| `cobblefurnies:red_sofa` | `minecraft:brick_stairs` (9) | — |
| `cobblefurnies:spruce_cabinet` | `minecraft:barrel` (8) | — |
| `cobblefurnies:white_sofa` | `minecraft:quartz_stairs` (24) | — |
| `cobblefurnies:white_stool` | `quark:white_stool` (8) | — |
| `cobblefurnies:yellow_cabinetry` | `minecraft:yellow_terracotta` (1) | — |
| `cobblefurnies:yellow_chair` | `quark:yellow_stool` (9) | — |
| `cobblefurnies:yellow_sink` | `minecraft:cauldron` (3) | — |
| `cobblefurnies:yellow_sofa` | `minecraft:smooth_sandstone_stairs` (19) | — |
| `cobblefurnies:yellow_stove` | `minecraft:smoker` (2) | — |
| `create:red_table_cloth` | `minecraft:red_carpet` (61) | — |
| `supplementaries:blackboard` | `minecraft:black_concrete` (14) | — |
| `supplementaries:candle_holder_black` | `minecraft:black_candle` (7) | — |
| `supplementaries:candle_holder_light_gray` | `minecraft:light_gray_candle` (19) | — |
| `supplementaries:candle_holder_purple` | `minecraft:purple_candle` (6) | — |
| `supplementaries:candle_holder_yellow` | `minecraft:yellow_candle` (9) | — |
| `supplementaries:clock_block` | `minecraft:birch_planks` (1) | — |
| `supplementaries:deepslate_lamp` | `quark:blaze_lantern` (39) | — |
| `supplementaries:fire_pit` | `minecraft:campfire` (5) | — |
| `supplementaries:flower_box` | `minecraft:potted_pink_tulip` (33)，`minecraft:potted_poppy` (7) | — |
| `supplementaries:globe` | `minecraft:stripped_jungle_wood` (1) | — |
| `supplementaries:globe_sepia` | `minecraft:birch_planks` (1) | — |
| `supplementaries:goblet` | `minecraft:stripped_jungle_wood` (1) | — |
| `supplementaries:jar` | `minecraft:decorated_pot` (4) | — |
| `supplementaries:pedestal` | `minecraft:chiseled_quartz_block` (5) | — |
| `supplementaries:planter` | `minecraft:rooted_dirt` (41) | — |
| `supplementaries:statue` | `minecraft:sandstone` (1) | — |
| `supplementaries:stone_lamp` | `minecraft:sea_lantern` (11) | — |
| `supplementaries:way_sign` | `minecraft:nether_brick_fence` (1) | — |

花箱按原 NBT 的花种保留颜色，牡丹换粉红郁金香，玫瑰丛换虞美人。种植箱换缠根泥土以支撑现有植物。挂钟、地球仪、酒杯和雕像使用近邻建筑材料。路牌使用原 `Mimic` 记录里的下界砖栅栏。

替换脚本为 `scripts/replace-hall-decor.py`。输入原区块模板和实载方块注册表即可复现。
