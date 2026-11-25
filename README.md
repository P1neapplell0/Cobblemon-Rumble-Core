# Cobblemon Rumble Core

面向 Real-Time Cobblemon 的 **Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21** 开发工程。NeoForge 21.1.248 复用本机缓存，也满足 XaeroClaims、Structure Rewind Watch 的最低版本要求。

## 开发手册

继续开发主线、支线、剧情 NPC 或副本前，先读 [剧情与玩法开发手册](docs/development-handbook.md)。

## 开发环境

1. 在 IntelliJ 中按 Gradle 项目导入，将 Gradle JVM 设为 Java 21，然后重载 Gradle。
2. 按[本地依赖清单](docs/development-dependencies.tsv)放置 JAR 和 ZIP。`libs/` 已被 Git 忽略；`flatDir` 会自动加载顶层 JAR（不解析传递依赖）。核心用到 API 的模组（如 Cobblemon、Real-Time Cobblemon、ArcQuest、Smartphone 和 CobbleDollars）同时进入编译类路径，其余按运行时模组加载。
3. 运行以下任务，或使用 IDEA 重载后生成的 NeoForge 运行配置：

   ```sh
   ./gradlew build
   ./gradlew runClient  # runs/neoforge-client
   ./gradlew runServer  # runs/neoforge-server；首次启动需确认 Minecraft EULA
   ```

发布时只需分发 `build/libs/cobblemon-rumble-core-<版本>.jar`，其中已包含早期窗口和 Core 的 Jar-in-Jar 依赖。升级时移除旧版 Core 和独立的 `rumble-early-display` JAR。

## 整合包发布目录

把 [modpack_override](modpack_override/) 的内容合并到 Minecraft 实例根目录。`config/` 存放整合包配置，`resourcepacks/` 由游戏识别，`options.txt` 给出客户端资源包的启用顺序。Core 自动从 `global_packs/datapacks/` 加载全局数据包，`global_packs/both/` 同时供服务端数据和客户端资源加载。双用途 ZIP 在发布目录中只存一份。新世界会发现并自动启用全局数据包，现有世界可用 `/datapack list` 查看启用状态。`optional_datapacks/` 供手动安装。

开发实例同步覆盖目录后也会走相同的加载路径。未同步时，Gradle 启动会回退到 `libs/packs/`。运行 `./gradlew syncRunOverrides` 可同步三个本地测试目录。`-PrumblePacks=false` 可停用 Core 对外部目录的发现，游戏原生 `resourcepacks/` 中的包可在资源包界面停用。客户端和专用服务器默认最大堆均为 `6G`，可分别用 `-PrumbleClientHeap=8G` 或 `-PrumbleServerHeap=8G` 覆盖。专用服务器可加 `-PrumbleServerWorld=rumble-smoke` 验证新世界。

`HumanEraVillagersIllagers` 的村民模型依赖客户端的 Entity Model Features（EMF）和 Entity Texture Features（ETF）。制作客户端整合包时，两者和该资源包都需一并附带；不装这两个客户端模组时，应关闭该资源包。

Simple Player Data Manager 和 VirtualLoot 0.4.1 随核心通过 Jar-in-Jar 分发。升级时，服务端和客户端都移除独立的 VirtualLoot JAR。本地依赖齐备时，构建产物还附带三项 MIT 模组（Cobbreeding、FSweep Stack Fix、MSD Special Move Fix），三项均已在旧 IncandescentVerityCore 的成品中内置。外置数据包、资源包和其他模组仍单独加载。详见[精简和内置记录](docs/dependency-prune-2026-11-16.md)。

传说遗迹汉化沿用 Legendary Monuments CN（LMCN，作者：下次一定能出货）的授权译文和[原许可证](src/main/resources/META-INF/licenses/lmcn.txt)。文本适配和蕾冠王对话由核心重新实现，随 Legendary Monuments 自动启用，不读写 `lmcn.json`。部署时移除独立 LMCN JAR。

核心 JAR 随附[模组汉化包](docs/localization.md)：迁移了当前仍使用的旧核心译文，并补充 Real-Time Cobblemon 的设置、HUD、战斗及牧场提示。进入游戏后使用「简体中文」即可，无需另外启用汉化包。

发布目录的 `options.txt` 按低到高排列资源包：`file/CqsAnimation.zip` 在 `mod_resources` 后，让 Mega 烈空坐的模型、贴图和动画由同一个包提供；旧版 E19 图标包放在 `mod_resources` 前，避免覆盖 Cobblemon 1.8.4 的模型索引。旧整合包的 Café 版本为烈空坐模型单独使用一组动画；当前版本复用了 `rayquaza` 组，与 Mega Showdown 的另一套骨骼冲突。核心为 Café 的模型单独绑定 Cobblemon 原版动画，不需要改资源包顺序或 ZIP。

旧 IncandescentVerityCore 的宝可梦模型补丁已按当前依赖适配，见[模型修复记录](docs/model-fixes.md)。

实时挑战、私有竞技场、晕眩捕捉、应用 SD 卡与钱包改动见[实时玩法与兼容维护](docs/realtime-gameplay.md)。ArcQuest 指南及实机截图见[指南清单](docs/arcquest-guides.md)，主城部署、序章模拟战、八馆解锁和 NPC 坐标见[序章部署说明](docs/prologue.md)。

第三馆后的 Mega 模拟配队和奖励见[配队表](modpack_override/config/cobblemon_rumble_core/mega_challenges.tsv)，八馆、Mega、路人和模拟馆的场地分配与宝可币规则见[主题竞技场](docs/battle-arenas.md)。部署时使用本地重编译的 Custom Instances 1.0.0，并随整合包分发 `modpack_override/` 的内容。CobbleBash、CobbleBash Screen、RCT 和依赖已恢复，模拟宇宙入口由后续剧情解锁。训练师队伍、生成配置和 Lootr 奖励迁移见[恢复记录](docs/trainer-and-loot-migration.md)。

## 依赖范围与验证

- 基础组合为 Real-Time Cobblemon `1.0.0-alpha.1`、Cobblemon `1.8.4`、Mega Showdown `1.3.3+1.8.4+1.21.1-release`。Real-Time 的 JAR 有 Mega Showdown 和 Accessories 兼容逻辑。其余模组来自 `CobblemonEbonyPhantomCore` 的开发依赖及这些依赖和外置包所需的库，不包含参考核心本身、其源码、剧情包和原声。
- 移除了 Fabric Loader、Loom、CobbleAction 和旧 Fabric 运行依赖。个别 NeoForge 模组内嵌的 Forgified Fabric API 桥接组件随各自 JAR 加载。
- 未启用 Pokémon Ultra Space、Raid Dens、Rider’s Call 和 Auto-Battle。内嵌的 VirtualLoot 0.4.1 用于虚拟牧场奖励，Cobblemon Cinematics 1.4.0 用于徽章演出，Structure Rewind Watch 1.0.0 提供神秘商人出售的结构重制怀表。
- 启动验证覆盖客户端创建并进入单人世界、专用服务器建档和数据包加载。已在独立开发客户端验证实时对战中文设置界面、能力变化提示，以及 Mega 烈空坐释放「画龙点睛」。部分第三方内容仍有非致命的数据警告，例如 Legendary Monuments 中引用旧物品 ID 的战利品表，以及部分旧刷怪预设。

清单中的 `CobblemonEbonyPhantomCore/` 路径相对于两个项目的共同父目录；`maven:` 条目是可从参考项目的 Gradle 缓存找到的坐标。
