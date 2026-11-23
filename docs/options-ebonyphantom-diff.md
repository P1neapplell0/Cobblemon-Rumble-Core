# EbonyPhantom → Rumble：缺失的非原版默认设置

基准是本机 macOS 上干净的 Minecraft Java 1.21.1 客户端，首次载入完成后，在未修改设置、未进入世界时调用原版保存方法生成 `options.txt`。按键名匹配：Rumble 已有的键不列，即使两包的值不同。

## 原版已有、EbonyPhantom 改过、Rumble 没有（14 项）

| 键 | 原版首次启动值 | EbonyPhantom |
| --- | --- | --- |
| `enableVsync` | `true` | `false` |
| `forceUnicodeFont` | `false` | `true` |
| `gamma` | `0.5` | `1.0` |
| `maxFps` | `120` | `260` |
| `operatorItemsTab` | `false` | `true` |
| `darkMojangStudiosBackground` | `false` | `true` |
| `narratorHotkey` | `true` | `false` |
| `incompatibleResourcePacks` | `[]` | `["mod/pokeverie:packs/pokeverie_assets","file/CR - Biome Expanded Spawns.zip","file/BotanyPots - Cobblemon Integrations.zip"]` |
| `advancedItemTooltips` | `false` | `true` |
| `skipMultiplayerWarning` | `false` | `true` |
| `syncChunkWrites` | `false` | `true` |
| `onboardAccessibility` | `true` | `false` |
| `soundCategory_music` | `1.0` | `0.15` |
| `modelPart_cape` | `true` | `false` |

- `syncChunkWrites` 原版默认值按平台决定：macOS 为 `false`，Windows 为 `true`。
- `onboardAccessibility` 是首次启动无障碍引导标志；完成引导后原版自己会写 `false`。
- `incompatibleResourcePacks` 记录参考包自己的资源包兼容性确认，不建议照搬。

## 原版没有这些键，Rumble 也没有（142 项）

下面是参考包多出的模组键位。它们不是「改过的原版选项」；原版没有可比较的默认值，部分是模组自身的默认键或已解绑的键。整包迁移时只选已安装且要保留的模组键位。

```text
key_key.fancytoasts.config_menu:key.keyboard.unknown
key_lambdynlights.key.toggle_fps_dynamic_lighting:key.keyboard.unknown
key_key.modernfix.config:key.keyboard.unknown
key_iris.keybind.reload:key.keyboard.unknown
key_iris.keybind.toggleShaders:key.keyboard.unknown
key_iris.keybind.shaderPackSelection:key.keyboard.unknown
key_iris.keybind.wireframe:key.keyboard.unknown
key_quark.keybind.back:key.mouse.4
key_supplementaries.keybind.quiver:key.keyboard.unknown
key_key.geo_mod.open_config:key.keyboard.unknown
key_key.cobblemon.summary:key.keyboard.c
key_key.cobblemon.downshiftparty:key.keyboard.down
key_key.cobblemon.upshiftparty:key.keyboard.up
key_key.cobblemon.throwpartypokemon:key.keyboard.r
key_key.cobblemon.ridingfreelook:key.keyboard.left.alt
key_key.constructionwand.wand_option:key.keyboard.left.control
key_key.jei.toggleOverlay:key.keyboard.o:CONTROL
key_key.jei.focusSearch:key.keyboard.f:CONTROL
key_key.jei.bookmark:key.keyboard.a
key_key.jei.showRecipe:key.keyboard.r
key_key.jei.showRecipe2:key.mouse.left
key_key.jei.showUses:key.keyboard.u
key_key.jei.showUses2:key.mouse.right
key_key.jei.transferRecipeBookmark:key.mouse.left:SHIFT
key_key.jei.maxTransferRecipeBookmark:key.mouse.left:CONTROL
key_key.jei.showBookmarkTooltipFeatures:key.keyboard.left.control
key_key.jei.quickMove:key.mouse.left:SHIFT
key_key.jei.clearSearchBar:key.mouse.right
key_key.jei.previousSearch:key.keyboard.up
key_key.jei.nextSearch:key.keyboard.down
key_key.jei.cheatOneItem:key.mouse.left
key_key.jei.cheatOneItem2:key.mouse.right
key_key.jei.cheatItemStack:key.mouse.left:SHIFT
key_key.jei.toggleCheatModeConfigButton:key.mouse.left:CONTROL
key_key.jei.toggleHideIngredient:key.mouse.left:CONTROL
key_key.jei.toggleWildcardHideIngredient:key.mouse.right:CONTROL
key_key.jei.recipeBack:key.keyboard.backspace
key_key.jei.previousRecipePage:key.keyboard.page.up
key_key.jei.nextRecipePage:key.keyboard.page.down
key_key.jei.pauseRecipeCycling:key.keyboard.left.shift
key_key.jei.previousCategory:key.keyboard.page.up:SHIFT
key_key.jei.nextCategory:key.keyboard.page.down:SHIFT
key_key.jei.closeRecipeGui:key.keyboard.escape
key_key.fast_tpa.send:key.keyboard.right.alt
key_key.autobattle.toggle:key.keyboard.unknown
key_key.autobattle.hunt_target:key.keyboard.v
key_key.ponder.ponder:key.keyboard.w
key_gui.xaero_instant_waypoint:key.keyboard.keypad.add
key_key.fsweep.store_all:key.keyboard.f
key_key.ares_hud.open_config:key.keyboard.apostrophe
key_key.modernui.openCenter:key.keyboard.unknown
key_key.modernui.zoom:key.keyboard.unknown
key_key.cobblemonraiddens.mouse:key.keyboard.left.alt
key_key.cobblemonraiddens.accept:key.keyboard.y
key_key.cobblemonraiddens.deny:key.keyboard.n
key_key.sophisticatedcore.sort:key.keyboard.unknown
key_key.sophisticatedcore.transfer_to_storage:key.keyboard.unknown
key_key.sophisticatedcore.transfer_to_inventory:key.keyboard.unknown
key_gui.xaero_open_map:key.keyboard.m
key_placebo.toggleTrails:key.keyboard.unknown
key_placebo.toggleWings:key.keyboard.unknown
key_key.cobblemoncharms.open_multi_charm:key.keyboard.unknown
key_key.ftbultimine:key.keyboard.grave.accent
key_ftbultimine.change_shape.next:key.keyboard.up
key_ftbultimine.change_shape.prev:key.keyboard.down
key_key.berrypouch.open_pouch:key.keyboard.unknown
key_key.berrypouch.cycle_bait_left:key.keyboard.left.bracket
key_key.berrypouch.cycle_bait_right:key.keyboard.right.bracket
key_key.berrypouch.toggle_auto_berry:key.keyboard.unknown
key_key.fightorflight.startbattle:key.keyboard.unknown
key_key.fightorflight.moveslot1:key.keyboard.unknown
key_key.fightorflight.moveslot2:key.keyboard.unknown
key_key.fightorflight.moveslot3:key.keyboard.unknown
key_key.fightorflight.moveslot4:key.keyboard.unknown
key_key.fightorflight.cmdmode.move_attack:key.keyboard.unknown
key_key.fightorflight.cmdmode.move:key.keyboard.unknown
key_key.fightorflight.cmdmode.stay:key.keyboard.unknown
key_key.fightorflight.cmdmode.attack:key.keyboard.unknown
key_key.fightorflight.cmdmode.attack_position:key.keyboard.unknown
key_key.fightorflight.cmdmode.nocmd:key.keyboard.unknown
key_key.fightorflight.cmdmode.clear:key.keyboard.unknown
key_key.more_cobblemon_tweaks.open_config:key.keyboard.unknown
key_key.cobblemon_riders_call.call_selected_ride:key.keyboard.x
key_key.cobblemon_riders_call.open_camera_settings:key.keyboard.comma
key_key.cobblemon_picnic.recall_party:key.keyboard.unknown
key_key.arc_quest.open_journal:key.keyboard.j
key_key.arc_quest.open_guide_list:key.keyboard.i
key_key.arc_quest.open_tracking_menu:key.keyboard.tab
key_key.followme.toggle:key.keyboard.unknown
key_Toggle belt:key.keyboard.z
key_create.keyinfo.toolmenu:key.keyboard.left.alt
key_create.keyinfo.toolbelt:key.keyboard.left.alt
key_create.keyinfo.rotate_menu:key.keyboard.unknown
key_create.keyinfo.shift_modifier:key.keyboard.left.shift
key_create.keyinfo.ctrl_modifier:key.keyboard.left.control
key_create.keyinfo.alt_modifier:key.keyboard.left.alt
key_key.beyonddimensions.toggle_magnet_key:key.keyboard.equal
key_chloride.zoom:key.keyboard.unknown
key_key.configured.open_mod_list:key.keyboard.unknown
key_key.cobblemon-battle-extras.battle_logs:key.keyboard.l
key_key.ezactions.open:key.keyboard.g
key_key.ezactions.open_editor:key.keyboard.period
key_key.raws_visual_keybinder.open_screen:key.keyboard.f10
key_key.raws_visual_keybinder.switch_to_preset_1:key.keyboard.unknown
key_key.raws_visual_keybinder.switch_to_preset_2:key.keyboard.unknown
key_key.raws_visual_keybinder.switch_to_preset_3:key.keyboard.unknown
key_key.raws_visual_keybinder.switch_to_preset_4:key.keyboard.unknown
key_key.raws_visual_keybinder.switch_to_preset_5:key.keyboard.unknown
key_key.jade.config:key.keyboard.keypad.0
key_key.jade.show_overlay:key.keyboard.keypad.1
key_key.jade.toggle_liquid:key.keyboard.keypad.2
key_key.jade.show_recipes:key.keyboard.keypad.3
key_key.jade.show_uses:key.keyboard.keypad.4
key_key.jade.narrate:key.keyboard.keypad.5
key_key.jade.show_details:key.keyboard.left.shift
key_key.beltborne_lanterns.open_config:key.keyboard.keypad.9
key_key.beltborne_lanterns.toggle_debug:key.keyboard.keypad.7
key_key.beltborne_lanterns.open_debug:key.keyboard.keypad.8
key_key.beltborne_lanterns.toggle_lantern:key.keyboard.keypad.6
key_key.parcool.Enable:key.keyboard.unknown
key_key.parcool.Crawl:key.keyboard.unknown
key_key.parcool.ClingToCliff:key.mouse.right
key_key.parcool.Breakfall:key.keyboard.left.alt
key_key.parcool.FastRun:key.keyboard.left.control
key_key.parcool.Dodge:key.keyboard.left.alt
key_key.parcool.RideZipline:key.mouse.right
key_key.parcool.WallSlide:key.mouse.right
key_key.parcool.WallJump:key.keyboard.space
key_key.parcool.Vault:key.mouse.right
key_key.parcool.HorizontalWallRun:key.keyboard.left.alt
key_key.parcool.HideInBlock:key.keyboard.unknown
key_key.parcool.openSetting:key.keyboard.unknown
key_key.parcool.QuickTurn:key.keyboard.unknown
key_key.parcool.Flipping:key.keyboard.unknown
key_key.parcool.HangDown:key.mouse.right
key_key.freecam.toggle:key.keyboard.unknown
key_key.freecam.playerControl:key.keyboard.unknown
key_key.freecam.tripodReset:key.keyboard.unknown
key_key.freecam.configGui:key.keyboard.unknown
key_key.armourers_workshop.open-wardrobe:key.keyboard.p:CONTROL
key_key.armourers_workshop.undo:key.keyboard.unknown
key_key.entityculling.toggle:key.keyboard.unknown
```
