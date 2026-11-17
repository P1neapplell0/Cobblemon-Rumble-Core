# 旧核心模型修复迁移

对照 IncandescentVerityCore 的 `civ_assets`（`3e6c298`、`13a5cad`），按当前 Cobblemon 1.8.4、Mega Showdown 1.3.3 和 Lost Lore 3.2.0 的资源重新适配：

| 形态 | 处理 |
| --- | --- |
| 永恒花花叶蒂 | 内置 `Model Fixes` 资源包在安装 Mega Showdown 时启用。Resolver 优先级为 5，高于 ATM 的 3/4，低于 Ange 等特殊形态的 7/99。普通、异色及 Mega 统一使用对应的 Mega Showdown 模型、姿态和贴图，去掉 CCC 遗留的非 Mega 贴图叠层。Mega 的叫声和受击动画清理了已不存在的骨骼轨道。 |
| 舞步形态美洛耶塔 | Mega Showdown 的悬浮和睡眠动画只保留新版模型拥有的骨骼轨道。如果 CCC 2.1 的旧模型与旧动画同时生效，则恢复这套动画自身的舞步形态前缀，而不是拿新版动画给旧模型用。 |
| Mega 暴飞龙 | 使用 Mega Showdown 的新版模型、贴图和定位点。若旧 resolver 没有姿态定义，则补上现存的 Mega 姿态；新版待机、行走和飞行动画去掉旧骨骼轨道。 |
| 铠甲超梦 | Lost Lore 的铠甲和 Mega 铠甲模型已经带有各自的完整贴图。只移除这两个模型仍继承的旧 E19 `armored` 叠层，保留 Lost Lore 的自发光层。 |
| 异色裙儿小姐 | Cobblemon 1.8.4 的贴图与旧修复在所有可见像素上相同，沿用上游。 |

模型补丁保留现有资源包顺序和模型、贴图。骑乘修复另外提取了旧核心的座位定位点，并适配新版座位格式，详见 [骑乘座位兼容](riding-fixes.md)。普通与 Mega 烈空坐继续分别使用现有 Café 和 Cqs 修复。

Café 烈空坐不是从旧核心搬过来的。当前组合里，Café 的月华烈空坐使用原版骨架，但 Mega Showdown 提供了同名、对应另一套骨架的动画组。`mixin/cobblemon/rayquaza/` 从 Cobblemon 本体资源中把原版动画注册到独立动画组，只给 Café 那条 resolver 换用核心内置的 poser。模型、贴图仍来自 Café，Mega 形态不受这一代码补丁影响；因此不需要为它们全局调整资源包顺序。
