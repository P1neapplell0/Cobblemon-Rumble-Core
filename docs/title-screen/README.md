# Cobblemon Rumble 对战封面

运行 `python3 scripts/build-title-screen.py`，然后打开 `build/title-screen/index.html`。构建器从 `libs/` 读取 Cobblemon 和 Mega Showdown 的模型、贴图、动画和音效，生成可离线打开的 HTML。首次构建会下载并校验与对战入场共用的 Three.js。

左侧是皮卡丘、路卡利欧、巨钳螳螂、魔幻假面喵、战舞郎和超级暴飞龙。右侧是甲贺忍蛙、烈咬陆鲨、火焰鸡、苍炎刃鬼、闪焰王牌和超级喷火龙 X。两队各有五只地面角色，超级暴飞龙与超级喷火龙 X 在左右上方飞行待机。皮卡丘蓄电时播放叫声和十万伏特音效。超级喷火龙 X 间歇播放空中吼叫。赛场和字标由 `gpt-image-2.6-flare` 生成。

单人和多人入口固定显示英文。语言选项会同步更新所有工具图标的悬浮提示和键盘焦点提示。确认退出会停止 HTML 预览的动画与声音，点击「返回标题界面」可以恢复。

## 游戏内实装

运行 `./gradlew runClient` 可同时预览早期加载页和标题页。标题的字标、图标与竞技场来自 HTML 设计，宝可梦由 Cobblemon 实时渲染。重新导出菜单贴图或跑步图集时，先构建 HTML，再用 `node scripts/export-title-art.mjs <Chrome 调试端口>` 更新 `game/`。

启动、世界加载和标准设置菜单共用 `game/arena-blur.png`。更新阵容后，可在 16:9 的 HTML 预览中隐藏 `.menu`，截取只有竞技场和对阵宝可梦的画面，再用 `python3 scripts/prepare-title-brand.py --backdrop <截图>` 重新生成背景。

`./gradlew assemble` 会生成单文件发布包 `build/libs/cobblemon-rumble-core-<版本>.jar`，直接放到实例的 `mods/`。升级时请先移除旧的独立 Core 和 `rumble-early-display` JAR。

发布包先加载早期窗口服务，再把内嵌 Core 写入实例的 `.rumble-cache/core/`，交给 NeoForge 正常扫描；Core 原有的 Jar-in-Jar 依赖仍由 NeoForge 解析。早期窗口会把 FML 的默认提供器切到 `rumbleearlywindow`，并尊重已选的其他提供器。卸载后可在实例的 `config/fml.toml` 把 `earlyWindowProvider` 改回 `fmlearlywindow`。

Discord 图标默认指向整合包社群 `https://discord.gg/UfVF8b2GFk`，可用 JVM 参数 `-Drumble.discordUrl=邀请地址` 覆盖。

窗口使用方可梦球图标，macOS 通过游戏的 Dock 图标入口设置，Windows 和 Linux X11 通过 GLFW 设置。原生 Wayland 的任务栏图标由桌面应用入口决定。

`python3 scripts/prepare-title-brand.py [Image 2.6 绿幕原图]` 可重新提取透明字标并导出各平台窗口图标，需要 Pillow 和 NumPy。
