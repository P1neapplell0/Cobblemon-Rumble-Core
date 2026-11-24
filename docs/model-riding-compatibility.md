# 模型、姿势与骑乘兼容性

## 资源包优先级

甲贺忍蛙的两个第三方 poser 分别位于 `(Cobblemon) Animated Pokemon.zip` 的 `bedrock/pokemon/posers/greninja.json` 和 `CCCwLegendSpawns_2.1.zip` 的 `bedrock/pokemon/posers/0658_greninja/greninja.json`。Minecraft 按**完整资源路径**叠加，Cobblemon 1.8.4 却按**命名空间和文件名**注册 poser；所以不同路径的这两个文件都可被读到，然后争用 `cobblemon:greninja`。它们都指向原版普通模型没有的两块手里剑，单纯交换两个 ZIP 的顺序不能解掉这个组合。

内置 `pose_fixes` 用独立的 `cobblemon_rumble_core:greninja_base` 保存同版本原版姿势。只对 `cobblemon:greninja.geo` 搭配旧共享 poser 时改用它；普通服装模型继续用原来的姿势，Ash 本体用 Ash 姿势。给 Ash 形态套上 Hero Style 或 Winter 普通模型时，使用与该模型配套的普通姿势。旧文件中缺失的两块手里剑仍有 Mixin 兜底。

其他定点修复：乌波帕底亚礼服模型的根骨与原生 poser 的名字对齐；哲尔尼亚斯补无形态标签时的默认 resolver；梦幻肩骑和烈箭鹰 Unite 起跳动画中过早截断的表达式只回退为已有的常量项。默认客户端包顺序为 `Cobblemon: regionbiasforms` → `Mega Showdown: regionbiasmsd` → `Rumble: pose_fixes`。手动维护 `options.txt` 的旧客户端需要补开前者，后者引用了前者的阿罗拉皮卡丘和皮丘 poser。

## 旧核心骑乘包

- `ride_fix_resource` 的座位信息已通过 `scripts/extract-riding-locators.py` 抽取，不装整包。核心只在活动模型的骨名、父链、枢轴和旋转匹配、且原模型确实缺该 locator 时补坐标；不替换几何、材质或已有座位。
- `ride_fix_data` 包含完整物种定义和旧 `offset` / `poseOffsets` 字段，直接覆盖会连基础属性、招式、特性、进化等一起覆盖，并改变本包可骑物种和形态。现有迁移层只转换仍在加载的旧座位字段，既不引入旧物种定义，也不以旧包调整当前坐骑资格。
- 本次只新增已核对骨链的定点座位：超梦 Boundary、老翁龙新年皮肤、伽勒尔火焰鸟、超极巨钢铠鸦、超级暴飞龙。水箭龟普通和超极巨模型复用各自已有的次座 `seat_water_1`，给它补 `seat_2` 别名。

## 本次运行检查

使用 `modpack_override` 的客户端和服务端资源顺序新建本地世界，读取实际同步的物种并构建形态与已声明外观对应的模型/poser，逐一检查乘客 locator。覆盖 1,025 个实装物种、1,507 个形态（其中 228 个含座位）、2,292 组模型姿势和 375 个座位引用。修复后未再复现姿势构建异常，16 个缺失座位引用降至 6 个：

| 仍缺的座位 | 原因 |
| --- | --- |
| 超级水箭龟 `seat_2` | 当前模型上的 `seat_water_1` 与主座几乎重合，不能直接套用普通形态的做法。 |
| 原始盖欧卡 `seat_2`–`seat_5` | 当前旧动画模型只有驾驶座；Mega Showdown 自带五座模型，但骨架不同，不能把坐标原样搬过去。 |
| 骑拉帝纳 Distortion 外观 `seat_1` | CobbleSafari 的独立模型没有骑乘点，与一般形态的骨架不同。 |

这轮检查验证加载、姿势构建及座位引用，不等于所有形态、姿势和乘客一起实骑时都不会穿模。新玩家首次传送后读取的重生维度和坐标已确认为试炼殿堂 `(-16, 50, 0)`；后续调用返回殿堂不会覆盖已有重生点。
