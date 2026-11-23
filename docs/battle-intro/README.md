# 对战入场

- HTML：运行 `python3 scripts/build-battle-intro.py`，双击 `build/battle-intro/index.html`。模型使用 Cobblemon 的 Java 骨骼轴映射。
- 游戏：PvP 房间、电影模式传说对决、Mega 副本在 Custom Instances 提交场地复制任务时开始演出。场地未就绪时保留 VS 画面；就绪后切到开战。画面使用真实玩家皮肤和 Cobblemon 形态。
- 渲染：`ArenaIntroOverlay` 控制时间轴和模型，`ArenaIntroStage` 负责单层背景着色器。着色器载入失败时回退到普通渐变。无额外 UI 模组依赖。
