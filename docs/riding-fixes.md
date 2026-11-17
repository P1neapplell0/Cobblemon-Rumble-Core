# 骑乘座位兼容（Cobblemon 1.8.4）

## 肯泰罗问题的来源

当前启用的 `Cobblemon Champions Addition [v2.1.0]-ZA.zip` 覆盖了风速狗、肯泰罗及其三个帕底亚形态的物种数据。座位仍是旧版 `offset/poseOffsets` 格式；1.8.4 的 `Seat` 只读取 `locator`、`condition`、`poseAnimations` 和 `handsBusy`。旧偏移被忽略，缺少 `locator` 的座位反序列化为空名称，无法对应模型上的 `seat_1`。

旧核心的 `ride_fix_data` 中也存在这套旧格式，因此照搬数据整包不能解决新版问题。旧核心的 `ride_fix_resource` 提供了许多座位定位点，但此前没有迁入。

## 当前处理

- `compat/riding/LegacyRidingData` 在物种和物种追加数据反序列化前，将带有旧偏移字段、且没有定位点名称的座位转换为 `seat_1`、`seat_2` 等。递归处理形态；保留骑乘行为、速度、体力、条件及其它物种设置。已有现代定位点的座位不变。
- `client/riding/RidingLocatorRepairs` 在模型工厂读取资源时补齐缺失座位。使用当前资源包顺序选中的模型，补丁保留原资源来源与元数据。
- 从旧核心提取的 `seat_locators.json` 仅包含座位骨骼的父级、枢轴、旋转和定位点，以及用于核对骨架的祖先变换。`seat` 名称转换为兼容的 `seat_1` 别名。已有同名定位点优先；骨架变换不匹配时跳过旧坐标。
- `seat_locators_extra.json` 单独维护旧包也缺少的座位：炽焰咆哮虎换装、甲贺忍蛙及相关模型、Mega／超极巨耿鬼。它们同样带有骨架核对信息。坐标使用 Bedrock 模型单位，挂在躯干／胸部骨骼上，以跟随模型动画。
- 两个 Mixin 位于 `mixin/cobblemon/riding/`；数据转换同时用于服务端与客户端，模型补丁只在客户端运行。补丁在资源加载时执行，不增加每帧模型扫描。

## 查出的旧格式数据

| 来源 | 宝可梦／形态 | 旧座位定义数 |
| --- | --- | ---: |
| Champions Addition | 风速狗、肯泰罗、帕底亚斗战种／火炽种／水澜种 | 5 |
| CCC 2.1 | 帝牙卢卡、帕路奇亚及起源形态、骑拉帝纳及起源形态、伊裴尔塔尔、故勒顿、密勒顿 | 8 |
| 终极解放 | 红莲铠骑、火焰鸡、苍炎刃鬼、鳃鱼龙、耿鬼、甲贺忍蛙、固拉多、炽焰咆哮虎、班基拉斯 | 9 |

形态继承会让一条基础物种定义同时影响多个 Mega、换装或特殊形态，因此模型检查需展开形态，不能只数这 22 条 JSON 定义。

## 维护

旧核心定位点清单可以重新提取：

```sh
python3 scripts/extract-riding-locators.py \
  '/path/to/CobblemonEbonyPhantomCore/src/main/resources/packs/ride_fix_resource' \
  src/main/resources/assets/cobblemon_rumble_core/riding/seat_locators.json
```

`seat_locators_extra.json` 是针对当前骨架补做的坐标，独立于提取结果。更新模型后，应核对选中的 resolver、座位名称和祖先变换，再实际检查上下骑及站立／移动时的乘客位置。

## 验证（2026-11-17）

使用当前全部开发模组与资源包，在 `.gradle/rumble-audit/client-game` 的新建隔离世界验证：

- 展开上述物种的骑乘形态后，53 个形态的座位均能对应当前实际加载模型上的定位点；服务端全部 299 个座位的定位点名称均非空。
- 肯泰罗四种形态、风速狗、固拉多、甲贺忍蛙、帝牙卢卡、骑拉帝纳、帕路奇亚、伊裴尔塔尔、故勒顿、密勒顿共 13 个案例，均通过正常骑乘入口、服务端座位分配、客户端乘客位置同步及下骑检查。
- 肯泰罗四种形态另加风速狗、甲贺忍蛙，使用实际前进键骑乘移动，位移均超过 2 米；检查移动中的乘客位置，并人工核对肯泰罗四种形态的第三人称截图。
- 多座位编号、重复迁移、保留现代座位属性、已有定位点优先和骨架变换不匹配时跳过均通过检查。旧核心座位元数据可以由提取脚本一致重建。

测试日志位于 `.gradle/rumble-audit/riding-client.log` 与 `riding-movement-client.log`。53 个形态是本次受旧数据影响的检查范围；其它新资源包替换模型后仍需按上述方法核对。
