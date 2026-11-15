# Cobblemon Rumble Core

面向 Real-Time Cobblemon 的 **Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21** 开发工程。NeoForge 21.1.248 复用本机缓存，也满足 XaeroClaims、Structure Rewind Watch 的最低版本要求。

## 开发环境

1. 在 IntelliJ 中按 Gradle 项目导入，将 Gradle JVM 设为 Java 21，然后重载 Gradle。
2. 按[本地依赖清单](docs/development-dependencies.tsv)放置 JAR 和 ZIP。`libs/` 已被 Git 忽略；`flatDir` 会自动加载顶层 JAR（不解析传递依赖）。Cobblemon 和 Real-Time Cobblemon 同时进入编译类路径，其余按运行时模组加载。
3. 运行以下任务，或使用 IDEA 重载后生成的 NeoForge 运行配置：

   ```sh
   ./gradlew build
   ./gradlew runClient  # runs/neoforge-client
   ./gradlew runServer  # runs/neoforge-server；首次启动需确认 Minecraft EULA
   ```

`libs/packs/datapacks/` 为数据包，`libs/packs/resourcepacks/` 为资源包，`libs/packs/both/` 两端都可发现。新世界会自动发现并加载外置数据包；现有世界可用 `/datapack list` 查看，按需启用。资源包在客户端「选项 → 资源包」启用。可加 `-PrumblePacks=false` 排查外置包冲突。JAR 构建不包含这些模组或包；NeoForge 使用独立运行目录，原 `run/` 存档保留。

## 依赖范围与验证

- 基础组合为 Real-Time Cobblemon `1.0.0-alpha.1`、Cobblemon `1.8.4`、Mega Showdown `1.3.3+1.8.4+1.21.1-release`。Real-Time 的 JAR 有 Mega Showdown 和 Accessories 兼容逻辑。其余模组来自 `CobblemonEbonyPhantomCore` 的开发依赖及这些依赖和外置包所需的库，不包含参考核心本身、其源码、剧情包和原声。
- 移除了 Fabric Loader、Loom、CobbleAction 和旧 Fabric 运行依赖。个别 NeoForge 模组内嵌的 Forgified Fabric API 桥接组件随各自 JAR 加载。
- 暂未启用 Pokémon Ultra Space `1.0.5`（在 Raid Dens `0.14.3` 上触发 `NoSuchFieldError: SET_RAID_BOSS`），以及只声明支持 Cobblemon 1.7 的 VirtualLoot、Rider’s Call、Dreamweaving Prism Ball、Auto-Battle。按当前开发范围不导入 Cobblemon Cinematics。
- 启动验证覆盖客户端资源加载、服务端新世界及数据包加载，不等于逐个验证附属模组的玩法或实测实时战斗。部分第三方内容仍有非致命的数据警告，例如 Yuushya 的可选 Create 配方、Legendary Monuments 中引用旧物品 ID 的战利品表，以及部分旧刷怪预设。

清单中的 `CobblemonEbonyPhantomCore/` 路径相对于两个项目的共同父目录；`maven:` 条目是可从参考项目的 Gradle 缓存找到的坐标。BotanyPots Cobblemon Integrations 使用本地修正版：在 `data/botanypots/function/load.mcfunction` 中将两处无效的 `"color":"lime"` 改为 `"color":"green"`。
