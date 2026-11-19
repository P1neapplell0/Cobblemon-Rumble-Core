# Mixin 配置与分包

统一入口为 src/main/resources/rumblecore.mixins.json，基础包为
com.p1nero.cobblemon.rumblecore.mixin。清单使用相对于基础包的类名，
先按目标模组 ID、再按功能子包组织；通用注入放 mixins，客户端注入放 client。
不要按功能继续拆出独立配置，也不要把客户端注入放进通用列表。

## 加载与汉化适配

- neoforge.mods.toml 只注册这一个配置。
- RumbleMixinPlugin 在 Mixin 加载阶段使用 FMLLoader 的加载列表检查可选
  Legendary Monuments；没有安装时跳过 legendarymonuments 包的注入。
  其余目标模组为当前核心的必需依赖，由 NeoForge 依赖声明保证。
- 两套现有汉化字节码适配器保留原实现，由统一插件仅对各自的
  CommonTextSites / ClientTextSites 完整类名分派 postApply。
  不能把任一汉化适配器直接用于整个统一配置，以免改写其他目标类。
- defaultRequire=1 保证必需注入失效时及时报错；有意允许缺失的注入应在
  注解上显式设置 require=0。覆盖方法要求声明 @Overwrite。

合并时核对旧配置的完整类名与客户端分类，确保清单不遗漏、不重复。
新增 Mixin 时同时更新该配置，并验证专用服务器不加载客户端类。
