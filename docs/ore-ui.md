# Ore UI 界面维护

- `client/ui/ore/OreTheme.java`：颜色、边框、按钮深度、焦点与选中态。全局换风格从这里改。
- `client/ui/ore/NativePanels.java`：CobbleDollars 购买、出售，以及 Waystones 选址窗口的排版。槽位和按钮热区仍由原模组维护。
- `client/ui/ore/RecyclingScreen.java`：分类回收价目表、预览与确认的客户端状态。
- `mixin/cobbledollars/` 和 `mixin/waystones/`：把原生界面挂到上面的渲染方法；不在这些类里定义主题色。
- `mixin/jei/WaystoneSelectionOverlay.java`：仅在石碑选址页隐藏 JEI 叠层；其它容器仍使用正常的 JEI 布局。
- `shop/`：目录、旧回收表和服务端批量结算。`prologue/ProfessorShop.java` 管博士入口；`prologue/TrialWaystones.java` 管剧情传送。
- `client/ui/ore/LegendaryDirectoryScreen.java`：传说之路的地域过滤、搜索、卡片和完整阵容预览。`client/ui/ore/TrialPortraits.java` 共用 Cobblemon 形态头像；`mixin/custom_instances/TrialLobbyPortraits.java` 在神兽和 Mega 房间标题区绘制形态头像。
- `mixin/custom_instances/MegaCatalogSearch.java` 为 Mega 目录增加搜索，继续使用 Custom Instances 的选关和房间流程。
- `client/ui/ore/MerchantOddsScreen.java`：黑市刷新概率公示。定价和概率由 `shop/WanderingCatalogue.java` 维护，客户端只渲染服务端快照。
- `client/ui/ore/TutorialPulse.java`：按钮发光扩散效果。第五馆模拟机试运行把它挂到横排模拟宇宙图标、岩石馆卡片和原生确认按钮上。
