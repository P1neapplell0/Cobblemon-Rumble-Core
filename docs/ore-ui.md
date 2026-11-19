# Ore UI 界面维护

- `client/ui/ore/OreTheme.java`：颜色、边框、按钮深度、焦点与选中态。全局换风格从这里改。
- `client/ui/ore/NativePanels.java`：CobbleDollars 购买、出售，以及 Waystones 选址窗口的排版。槽位和按钮热区仍由原模组维护。
- `client/ui/ore/RecyclingScreen.java`：分类回收价目表、预览与确认的客户端状态。
- `mixin/cobbledollars/` 和 `mixin/waystones/`：把原生界面挂到上面的渲染方法；不在这些类里定义主题色。
- `mixin/jei/WaystoneSelectionOverlay.java`：仅在石碑选址页隐藏 JEI 叠层；其它容器仍使用正常的 JEI 布局。
- `shop/`：目录、旧回收表和服务端批量结算。`prologue/ProfessorShop.java` 管博士入口；`prologue/TrialWaystones.java` 管剧情传送。
