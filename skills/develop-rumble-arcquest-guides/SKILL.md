---
name: develop-rumble-arcquest-guides
description: 在 Cobblemon Rumble Core 中创建和验证 ArcQuest 教学指南、分类、截图和后续任务阶段发放。
---

1. 先读 `docs/arcquest-guides.md` 和 `guide/RumbleGuides.java`。参照旧核心的入门／战斗分组；注册指南和确定解锁时机是两件事，未指定时不新增发放钩子。
2. 在 `ArcQuestRegistrationEvent.Guide` 中用 `GuideBuilder` / `GuidePageBuilder` 注册，保持 `cobblemon_rumble_core:<path>` ID 不变。选用注册表中已存在的图标，分类用 `GuideCategory.BASICS`，先注册 `GuideGroupDefinition` 再分配指南。
3. 标题、摘要和正文写入 `assets/cobblemon_rumble_core/lang/{zh_cn,en_us}.json`。用 `GuideText.Arg` 包装 `Component.keybind`，多个变量用 `%1$s`、`%2$s`，不要硬编码当前键位。
4. 用实际隔离开发客户端取图；不要操作原存档。PNG 放在 `assets/cobblemon_rumble_core/textures/guide/`，用对应宽高比裁切，保证缩到指南尺寸时关键控件仍可辨认。别用模组文件中并不存在的功能写教学。
5. `JAVA_HOME=$(/usr/libexec/java_home -v21) ./gradlew --offline build` 后在测试世界用 `/arcquest guide grant @s <id>` 和 `/arcquest guide open @s <id>` 验证注册、键位、图文与分页；同步更新指南清单。现有指南的解锁时机仍留给后续任务设计。
