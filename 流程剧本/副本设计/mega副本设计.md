# Mega 模拟试炼设计

共配置 **87 场**单种形态的挑战：Lv.38（2 场）、Lv.42（3 场）、Lv.48（49 场）、Lv.55（27 场）、Lv.60（1 场）、Lv.62（2 场）、Lv.72（2 场）、Lv.75（1 场）。不同 Mega 形态分别占一行。

此表从 [实际运行配置](../../modpack_override/config/cobblemon_rumble_core/mega_challenges.tsv) 导出，按配置顺序排列。等级、性格、努力值及招式以 TSV 为准；装载时只开放当前 Cobblemon 数据中存在的形态。

招式以 Smogon 对应赛制的社区配招为参考，再按本机学招表调整。强化、恢复等变化招式仍保留在配置中；当前 RTC 敌方 AI 会从其中可执行的伤害招式选择，不会按回合制顺序打出整套连招。

| 试炼 ID | 宝可梦 | 等级 | 性格 | 招式 | 宝可币 | 来源 |
| --- | --- | ---: | --- | --- | ---: | --- |
| `venusaurite` | 妙蛙花 · Mega | 55 | 大胆 | 寄生种子／剧毒／光合作用／打草结 | 7,000 | smogon:ubers |
| `charizardite_x` | 喷火龙 · Mega X | 55 | 爽朗 | 龙之舞／闪焰冲锋／地震／龙爪 | 7,000 | smogon:ou |
| `charizardite_y` | 喷火龙 · Mega Y | 55 | 胆小 | 喷射火焰／真气弹／日光束／羽栖 | 7,000 | smogon:ou |
| `blastoisinite` | 水箭龟 · Mega | 55 | 胆小 | 高速旋转／恶之波动／波导弹／冰冻光束 | 7,000 | smogon:ru |
| `beedrillite` | 大针蜂 · Mega | 42 | 爽朗 | 急速折返／毒击／直冲钻／拍落 | 5,000 | smogon:nationaldexuu |
| `pidgeotite` | 大比鸟 · Mega | 48 | 胆小 | 暴风／热风／急速折返／羽栖 | 7,000 | smogon:uu |
| `raichunite_x` | 雷丘 · Mega X | 48 | 爽朗 | 伏特攻击／终极冲击／疯狂伏特／舍身冲撞 | 7,000 | rumble |
| `raichunite_y` | 雷丘 · Mega Y | 48 | 胆小 | 破坏光线／十万伏特／伏特攻击／终极冲击 | 7,000 | rumble |
| `clefablite` | 皮可西 · Mega | 48 | 胆小 | 破坏光线／月亮之力／预知未来／魅诱之声 | 7,000 | rumble |
| `alakazite` | 胡地 · Mega | 48 | 胆小 | 精神强念／真气弹／暗影球／自我再生 | 7,000 | smogon:ou |
| `victreebelite` | 大食花 · Mega | 48 | 胆小 | 飞叶风暴／破坏光线／污泥波／能量球 | 7,000 | rumble |
| `slowbronite` | 呆壳兽 · Mega | 48 | 大胆 | 热水／偷懒／冰冻光束／精神强念 | 7,000 | smogon:ou |
| `gengarite` | 耿鬼 · Mega | 48 | 胆小 | 破坏光线／污泥波／污泥炸弹／暗影球 | 7,000 | rumble |
| `kangaskhanite` | 袋兽 · Mega | 48 | 固执 | 增强拳／舍身冲撞／地震／突袭 | 7,000 | smogon:battlespotsingles |
| `starminite` | 宝石海星 · Mega | 48 | 胆小 | 破坏光线／精神强念／冲浪／水炮 | 7,000 | rumble |
| `pinsirite` | 凯罗斯 · Mega | 48 | 爽朗 | 终极冲击／替身／地震／剑舞 | 7,000 | smogon:1v1 |
| `gyaradosite` | 暴鲤龙 · Mega | 55 | 爽朗 | 龙之舞／攀瀑／咬碎／替身 | 7,000 | smogon:nationaldex |
| `aerodactylite` | 化石翼龙 · Mega | 48 | 爽朗 | 尖石攻击／地震／追打／羽栖 | 7,000 | smogon:uu |
| `dragoninite` | 快龙 · Mega | 55 | 胆小 | 流星群／破坏光线／逆鳞／暴风 | 7,000 | rumble |
| `mewtwonite_x` | 超梦 · Mega X | 72 | 爽朗 | 终极冲击／预知未来／舍身冲撞／劈瓦 | 11,000 | rumble |
| `mewtwonite_y` | 超梦 · Mega Y | 72 | 胆小 | 精神击破／冰冻光束／大字爆炎／冥想 | 11,000 | smogon:ubers |
| `meganiumite` | 大竺葵 · Mega | 55 | 胆小 | 疯狂植物／花瓣舞／破坏光线／终极冲击 | 7,000 | rumble |
| `feraligite` | 大力鳄 · Mega | 55 | 爽朗 | 逆鳞／加农水炮／终极冲击／舍身冲撞 | 7,000 | rumble |
| `ampharosite` | 电龙 · Mega | 48 | 内敛 | 伏特替换／龙之波动／真气弹／十万伏特 | 7,000 | smogon:ru |
| `steelixite` | 大钢蛇 · Mega | 48 | 勇敢 | 隐形岩／陀螺球／地震／诅咒 | 7,000 | smogon:uu |
| `scizorite` | 巨钳螳螂 · Mega | 48 | 淘气 | 剑舞／羽栖／子弹拳／急速折返 | 7,000 | smogon:ou |
| `heracronite` | 赫拉克罗斯 · Mega | 48 | 固执 | 剑舞／近身战／岩石爆击／飞弹针 | 7,000 | smogon:ou |
| `skarmorite` | 盔甲鸟 · Mega | 48 | 爽朗 | 勇鸟猛攻／铁蹄光线／终极冲击／舍身冲撞 | 7,000 | rumble |
| `houndoominite` | 黑鲁加 · Mega | 48 | 胆小 | 诡计／大字爆炎／恶之波动／污泥炸弹 | 7,000 | smogon:uu |
| `tyranitarite` | 班基拉斯 · Mega | 55 | 固执 | 拍落／岩崩／隐形岩／终极冲击 | 7,000 | smogon:nationaldexdoubles |
| `sceptilite` | 蜥蜴王 · Mega | 55 | 胆小 | 飞叶风暴／龙之波动／寄生种子／破坏光线 | 7,000 | smogon:ru |
| `blazikenite` | 火焰鸡 · Mega | 55 | 固执 | 剑舞／闪焰冲锋／踢倒／尖石攻击 | 7,000 | smogon:ubers |
| `swampertite` | 巨沼怪 · Mega | 55 | 爽朗 | 攀瀑／地震／冰冻拳／隐形岩 | 7,000 | smogon:ou |
| `gardevoirite` | 沙奈朵 · Mega | 48 | 胆小 | 鬼火／精神冲击／巨声／真气弹 | 7,000 | smogon:ou |
| `sablenite` | 勾魂眼 · Mega | 38 | 大胆 | 鬼火／自我再生／欺诈／挑衅 | 5,000 | smogon:1v1 |
| `mawilite` | 大嘴娃 · Mega | 38 | 固执 | 剑舞／嬉闹／突袭／拍落 | 5,000 | smogon:ou |
| `aggronite` | 波士可多拉 · Mega | 55 | 慎重 | 隐形岩／重磅冲撞／地震／冰冻拳 | 7,000 | smogon:ou |
| `medichamite` | 恰雷姆 · Mega | 42 | 爽朗 | 飞膝踢／冰冻拳／意念头锤／近身战 | 5,000 | smogon:ou |
| `manectite` | 雷电兽 · Mega | 48 | 胆小 | 伏特替换／过热／十万伏特／破坏光线 | 7,000 | smogon:uu |
| `sharpedonite` | 巨牙鲨 · Mega | 48 | 固执 | 咬碎／冰冻牙／地震／终极冲击 | 7,000 | smogon:uu |
| `cameruptite` | 喷火驼 · Mega | 48 | 内敛 | 隐形岩／大字爆炎／大地之力／剧毒 | 7,000 | smogon:ou |
| `altarianite` | 七夕青鸟 · Mega | 48 | 淘气 | 泰山压顶／羽栖／吼叫／治愈铃声 | 7,000 | smogon:ou |
| `banettite` | 诅咒娃娃 · Mega | 48 | 固执 | 同命／暗影爪／垃圾射击／挑衅 | 7,000 | smogon:ru |
| `chimechite` | 风铃铃 · Mega | 48 | 胆小 | 预知未来／精神强念／能量球／巨声 | 7,000 | rumble |
| `absolite` | 阿勃梭鲁 · Mega | 48 | 天真 | 拍落／追打／大字爆炎／突袭 | 7,000 | smogon:uu |
| `absolite_z` | 阿勃梭鲁 · mega_z | 48 | 爽朗 | 终极冲击／欺诈／近身战／舍身冲撞 | 7,000 | rumble |
| `glalitite` | 冰鬼护 · Mega | 48 | 天真 | 撒菱／舍身冲撞／冷冻干燥／地震 | 7,000 | smogon:uu |
| `salamencite` | 暴飞龙 · Mega | 55 | 固执 | 龙之舞／舍身冲撞／羽栖／硬撑 | 7,000 | smogon:ubers |
| `metagrossite` | 巨金怪 · Mega | 55 | 爽朗 | 彗星拳／冰冻拳／岩崩／子弹拳 | 7,000 | smogon:ubers |
| `latiasite` | 拉帝亚斯 · Mega | 62 | 胆小 | 冰冻光束／精神强念／电磁波／羽栖 | 11,000 | smogon:ou |
| `latiosite` | 拉帝欧斯 · Mega | 62 | 天真 | 精神强念／地震／冰冻光束／羽栖 | 11,000 | smogon:ou |
| `rayquaza` | 烈空坐 · Mega | 75 | 爽朗 | 画龙点睛／逆鳞／终极冲击／流星群 | 11,000 | rumble |
| `staraptite` | 姆克鹰 · Mega | 48 | 爽朗 | 勇鸟猛攻／近身战／终极冲击／舍身冲撞 | 7,000 | rumble |
| `lopunnite` | 长耳兔 · Mega | 48 | 爽朗 | 飞膝踢／增强拳／再来一次／终极冲击 | 7,000 | smogon:ou |
| `garchompite` | 烈咬陆鲨 · Mega | 55 | 急躁 | 流星群／地震／大字爆炎／隐形岩 | 7,000 | smogon:ou |
| `garchompite_z` | 烈咬陆鲨 · mega_z | 55 | 胆小 | 流星群／破坏光线／大地之力／逆鳞 | 7,000 | rumble |
| `lucarionite` | 路卡利欧 · Mega | 55 | 爽朗 | 剑舞／彗星拳／近身战／子弹拳 | 7,000 | smogon:ubers |
| `lucarionite_z` | 路卡利欧 · mega_z | 55 | 胆小 | 铁蹄光线／破坏光线／近身战／飞膝踢 | 7,000 | rumble |
| `abomasite` | 暴雪王 · Mega | 48 | 固执 | 剑舞／木槌／冰砾／地震 | 7,000 | smogon:ru |
| `galladite` | 艾路雷朵 · Mega | 48 | 爽朗 | 剑舞／近身战／意念头锤／拍落 | 7,000 | smogon:ou |
| `froslassite` | 雪妖女 · Mega | 48 | 胆小 | 破坏光线／冰冻光束／暗影球／暴风雪 | 7,000 | rumble |
| `emboarite` | 炎武王 · Mega | 55 | 爽朗 | 近身战／闪焰冲锋／蛮力／爆炸烈焰 | 7,000 | rumble |
| `excadrite` | 龙头地鼠 · Mega | 48 | 爽朗 | 地震／铁蹄光线／终极冲击／十万马力 | 7,000 | rumble |
| `audinite` | 差不多娃娃 · Mega | 42 | 胆小 | 破坏光线／终极冲击／魔法闪耀／大字爆炎 | 5,000 | rumble |
| `scolipite` | 蜈蚣王 · Mega | 48 | 爽朗 | 超级角击／终极冲击／铁滚轮／舍身冲撞 | 7,000 | rumble |
| `scraftinite` | 头巾混混 · Mega | 48 | 爽朗 | 近身战／飞膝踢／终极冲击／欺诈 | 7,000 | rumble |
| `eelektrossite` | 麻麻鳗鱼王 · Mega | 48 | 爽朗 | 终极冲击／闪电强袭／疯狂伏特／近身战 | 7,000 | rumble |
| `chandelurite` | 水晶灯火灵 · Mega | 48 | 胆小 | 过热／破坏光线／大字爆炎／暗影球 | 7,000 | rumble |
| `golurkite` | 泥偶巨人 · Mega | 48 | 爽朗 | 地震／灵骚／终极冲击／十万马力 | 7,000 | rumble |
| `chesnaughtite` | 布里卡隆 · Mega | 55 | 爽朗 | 近身战／蛮力／木槌／疯狂植物 | 7,000 | rumble |
| `delphoxite` | 妖火红狐 · Mega | 55 | 胆小 | 爆炸烈焰／预知未来／过热／破坏光线 | 7,000 | rumble |
| `greninjite` | 甲贺忍蛙 · Mega | 55 | 胆小 | 加农水炮／破坏光线／冲浪／恶之波动 | 7,000 | rumble |
| `pyroarite` | 火炎狮 · Mega | 48 | 胆小 | 破坏光线／过热／终极冲击／大字爆炎 | 7,000 | rumble |
| `floettite` | 花叶蒂（永恒之花） · Mega | 55 | 胆小 | 月亮之力／花瓣舞／魅诱之声／能量球 | 7,000 | rumble |
| `meowsticite` | 超能妙喵 · Mega | 48 | 胆小 | 破坏光线／精神强念／广域战力／终极冲击 | 7,000 | rumble |
| `malamarite` | 乌贼王 · Mega | 48 | 爽朗 | 终极冲击／欺诈／预知未来／蛮力 | 7,000 | rumble |
| `barbaracite` | 龟足巨铠 · Mega | 48 | 爽朗 | 蛮力／终极冲击／十字劈／尖石攻击 | 7,000 | rumble |
| `dragalgite` | 毒藻龙 · Mega | 48 | 胆小 | 流星群／破坏光线／污泥波／污泥炸弹 | 7,000 | rumble |
| `hawluchanite` | 摔角鹰人 · Mega | 48 | 爽朗 | 勇鸟猛攻／近身战／蛮力／终极冲击 | 7,000 | rumble |
| `diancite` | 蒂安希 · Mega | 60 | 爽朗 | 大爆炸／终极冲击／钻石风暴／嬉闹 | 11,000 | rumble |
| `crabominite` | 好胜毛蟹 · Mega | 48 | 爽朗 | 近身战／蛮力／终极冲击／冰锤 | 7,000 | rumble |
| `golisopite` | 具甲武者 · Mega | 55 | 爽朗 | 终极冲击／迎头一击／近身战／铁头 | 7,000 | rumble |
| `drampanite` | 老翁龙 · Mega | 48 | 胆小 | 破坏光线／流星群／终极冲击／逆鳞 | 7,000 | rumble |
| `falinksite` | 列阵兵 · Mega | 48 | 爽朗 | 近身战／蛮力／终极冲击／超级角击 | 7,000 | rumble |
| `scovillainite` | 狠辣椒 · Mega | 48 | 爽朗 | 终极冲击／飞叶风暴／过热／种子炸弹 | 7,000 | rumble |
| `glimmoranite` | 晶光花 · Mega | 55 | 胆小 | 破坏光线／污泥波／污泥炸弹／力量宝石 | 7,000 | rumble |
| `baxcalibrite` | 戟脊龙 · Mega | 55 | 爽朗 | 巨剑突击／逆鳞／终极冲击／舍身冲撞 | 7,000 | rumble |

更新表格：`python3 scripts/check-encounter-design.py --write-mega`。
