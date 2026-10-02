# 已实装技能清单

每个 JSON 文件为一个分支，含入口最多 30 个节点；重复成长用独立 I／II 节点，单节点只能购买一次。使用 XP points；WLP 物品成本尚未接入。混合 Form 同时显示全部命中分支；完成前 10 个通用成长节点后，从原黑白胶分叉点 common_strength_2 开始解锁分类技能。

节肢动物先于昆虫／蛛形解锁。机制数值为最终路线值，不累加前后级；普通属性节点按各级加算／基础倍率加算。九条命真正死亡后恢复，换形态、重登、重载均不补次数。

| 分支 | 节点数 |
| --- | --- |
| 空 | 12 |
| 蛛形 | 7 |
| 节肢动物 | 3 |
| 双足 | 7 |
| 鸟系 | 7 |
| 犬系 | 7 |
| 头足类 | 7 |
| 黑胶 | 16 |
| 龙系 | 9 |
| 猫系 | 9 |
| 四足兽形 | 7 |
| 鱼类 | 7 |
| 人形 | 7 |
| 昆虫 | 7 |
| 陆 | 7 |
| 水生哺乳类 | 7 |
| 鱼尾 | 7 |
| 其他哺乳类 | 7 |
| 植物 | 7 |
| 鳐系 | 7 |
| 爬行类 | 7 |
| 海 | 9 |
| 鲨系 | 7 |
| 蛇身 | 7 |
| 半人马形 | 7 |
| 通用 | 25 |
| 白胶 | 15 |

## 空

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 空关键节点（关键） | 25 | `changede:common_strength_2` | 接入原 Form 滑翔；飞行消耗额外 +50%，每个护甲槽带来 5% 速度／转向响应惩罚。 |
| 飞行节能 1 | 25 | `changede:air_core` | 飞行额外消耗从 +50% 降到 +25%。 |
| 飞行节能 2 | 50 | `changede:air_endurance_1` | 飞行额外消耗完全取消；保留原能力基础消耗。 |
| 负重适应 1 | 25 | `changede:air_core` | 护甲带来的本分支速度／响应惩罚减半。 |
| 负重适应 2 | 50 | `changede:air_load_adaptation_1` | 护甲带来的本分支速度／响应惩罚取消。 |
| 飞行控制 | 25 | `changede:air_core` | 提升原有飞行操控与起飞滑翔能力 15% |
| 空中体魄 I | 25 | `changede:air_core` | 共享空分支的体魄训练，最大生命 +2。 |
| 空中机动 | 40 | `changede:flight_technique` | 原有飞行速度和起飞滑翔推进额外 +10% |
| 空中体魄 II | 50 | `changede:yufeng_body_growth` | 继续训练空中体魄，最大生命再 +2。 |
| 飞行精通 | 50 | `changede:aerobatics` | 原有飞行速度和起飞滑翔推进额外 +10% |
| 安全着陆 | 25 | `changede:aerobatics` | 摔落伤害降低 25% |
| 熟练着陆 | 40 | `changede:safe_landing` | 额外降低 25% 摔落伤害 |

## 蛛形

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 蛛形关键节点（关键） | 25 | `changede:arthropod_core` | 进入蛛形成长；保留原 Form 已有攀爬／结网，不继承昆虫生命减半或环境恢复。 |
| 蛛形稳定 I | 25 | `changede:arachnid_core` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 蛛形稳定 II | 50 | `changede:arachnid_balance_1` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 蛛形力量 I | 25 | `changede:arachnid_core` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 蛛形力量 II | 50 | `changede:arachnid_strength_1` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 蛛形体魄 I | 25 | `changede:arachnid_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 蛛形体魄 II | 50 | `changede:arachnid_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 节肢动物

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 节肢动物关键节点（关键） | 25 | `changede:common_strength_2` | 先开启節肢大类，再分昆虫／蛛形；此节点生效后才参与原版节肢杀手判定，不重复计算。 |
| 外骨骼 I | 25 | `changede:arthropod_core` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |
| 外骨骼 II | 50 | `changede:arthropod_shell_1` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |

## 双足

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 双足关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 灵巧步伐 I | 25 | `changede:biped_core` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 灵巧步伐 II | 50 | `changede:biped_dexterity_1` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 持械体魄 I | 25 | `changede:biped_core` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 持械体魄 II | 50 | `changede:biped_strength_1` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 双足强壮 I | 25 | `changede:biped_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 双足强壮 II | 50 | `changede:biped_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 鸟系

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 鸟系关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 鸟系缓冲 I | 25 | `changede:bird_core` | 摔落伤害减免每级增加 10%。各级按本级贡献叠加。 |
| 鸟系缓冲 II | 50 | `changede:bird_landing_1` | 摔落伤害减免每级增加 10%。各级按本级贡献叠加。 |
| 鸟系步伐 I | 25 | `changede:bird_core` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 鸟系步伐 II | 50 | `changede:bird_stride_1` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 鸟系耐力 I | 25 | `changede:bird_core` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |
| 鸟系耐力 II | 50 | `changede:bird_energy_1` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |

## 犬系

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 犬系关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 犬系力量 I | 25 | `changede:canine_core` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 犬系力量 II | 50 | `changede:canine_strength_1` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 追猎步伐 I | 25 | `changede:canine_core` | 基础地面移速每级增加 2.5%。各级按本级贡献叠加。 |
| 追猎步伐 II | 50 | `changede:canine_stride_1` | 基础地面移速每级增加 2.5%。各级按本级贡献叠加。 |
| 犬系强壮 I | 25 | `changede:canine_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 犬系强壮 II | 50 | `changede:canine_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 头足类

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 头足类关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 腕足力量 I | 25 | `changede:cephalopod_core` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 腕足力量 II | 50 | `changede:cephalopod_strength_1` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 腕足推进 I | 25 | `changede:cephalopod_core` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 腕足推进 II | 50 | `changede:cephalopod_swimming_1` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 腕足稳定 I | 25 | `changede:cephalopod_core` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 腕足稳定 II | 50 | `changede:cephalopod_balance_1` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |

## 黑胶

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 黑胶关键节点（关键） | 25 | `changede:common_strength_2` | 开启黑胶同族的克制、抗性和装备适配成长。 |
| 白胶克星 I | 25 | `changede:dark_shell` | 对白胶系列目标的伤害 +1，多个节点加算。 |
| 白胶抵抗 I | 25 | `changede:dark_white_nemesis_1` | 受到白胶系列攻击的伤害减少 10%，多个节点减免比例加算。 |
| 白雾抵抗 I | 25 | `changede:dark_white_resistance_1` | 受到白雾的伤害减少 10%；不影响黑雨伤害。 |
| 护甲适配 I | 25 | `changede:dark_white_fog_resistance_1` | 穿戴至少一件护甲时，额外护甲 +1；每个节点只计算一次，脱下护甲即移除。 |
| 武器适配 I | 25 | `changede:dark_armor_adaptation_1` | 主手或副手持有武器时，额外护甲 +1；收起武器即移除。 |
| 胶锭武器适配性 I | 25 | `changede:dark_weapon_adaptation_1` | 使用胶锭武器攻击时，伤害 +1；当前适用于胶锭矛，多个节点加算。 |
| 武器适配 II | 25 | `changede:dark_latex_weapon_adaptation_1` | 主手或副手持有武器时，额外护甲 +1；收起武器即移除。 |
| 白胶抵抗 II | 25 | `changede:dark_weapon_adaptation_2` | 受到白胶系列攻击的伤害减少 10%，多个节点减免比例加算。 |
| 白胶克星 II | 25 | `changede:dark_white_resistance_2` | 对白胶系列目标的伤害 +1，多个节点加算。 |
| 武器适配 III | 25 | `changede:dark_white_nemesis_2` | 主手或副手持有武器时，额外护甲 +1；收起武器即移除。 |
| 胶锭武器适配性 II | 25 | `changede:dark_weapon_adaptation_3` | 使用胶锭武器攻击时，伤害 +1；当前适用于胶锭矛，多个节点加算。 |
| 护甲适配 II | 25 | `changede:dark_latex_weapon_adaptation_2` | 穿戴至少一件护甲时，额外护甲 +1；每个节点只计算一次，脱下护甲即移除。 |
| 胶锭武器适配性 III | 25 | `changede:dark_armor_adaptation_2` | 使用胶锭武器攻击时，伤害 +1；当前适用于胶锭矛，多个节点加算。 |
| 胶锭武器适配性 IV | 25 | `changede:dark_latex_weapon_adaptation_3` | 使用胶锭武器攻击时，伤害 +1；当前适用于胶锭矛，多个节点加算。 |
| 白胶克星 III | 25 | `changede:dark_latex_weapon_adaptation_4` | 对白胶系列目标的伤害 +1，多个节点加算。 |

## 龙系

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 龙系关键节点（关键） | 25 | `changede:common_strength_2` | 开启采集与狩猎路线；后续虚拟附魔不永久改写工具。 |
| 龙的时运 | 25 | `changede:dragon_core` | 有效采集虚拟时运 +1，普通上限 III；精准采集优先。 |
| 龙的抢夺 | 25 | `changede:dragon_core` | 有效击杀虚拟抢夺 +1，普通上限 III。 |
| 矿脉馈赠 | 50 | `changede:dragon_fortune_1` | 有效自然矿石有 5% 概率追加 1 个基础产物；玩家放置或旧矿石来源未知时不触发。 |
| 狩猎战利品 | 50 | `changede:dragon_looting_1` | 有效敌对生物击杀有 3% 概率追加铁粒／金粒／绿宝石之一。 |
| 龙鳞 I | 25 | `changede:dragon_core` | 护甲韧性每级增加 0.5 点。各级按本级贡献叠加。 |
| 龙鳞 II | 50 | `changede:dragon_scales_1` | 护甲韧性每级增加 0.5 点。各级按本级贡献叠加。 |
| 龙的体魄 I | 25 | `changede:dragon_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 龙的体魄 II | 50 | `changede:dragon_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 猫系

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 猫系关键节点（关键） | 25 | `changede:common_strength_2` | 8 格内威慑苦力怕／幻翼，直接攻击及可归因投射物伤害 +10%。 |
| 猫的威慑 | 25 | `changede:feline_core` | 威慑半径提升到 12 格。 |
| 利爪 1 | 25 | `changede:feline_core` | 直接／可归因投射物伤害最终提高 15%。 |
| 利爪 2 | 50 | `changede:feline_claws_1` | 直接／可归因投射物伤害最终提高 20%。 |
| 灵活落地 I | 25 | `changede:feline_core` | 摔落伤害减免每级增加 10%。各级按本级贡献叠加。 |
| 灵活落地 II | 50 | `changede:feline_balance_1` | 摔落伤害减免每级增加 10%。各级按本级贡献叠加。 |
| 猫的体魄 I | 25 | `changede:feline_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 猫的体魄 II | 50 | `changede:feline_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 猫有九条命（关键） | 150 | `changede:feline_presence_1`、`changede:feline_claws_2` | 最多抵抗九次致命事件；每次消耗一条命，真正死亡重生后恢复九次。图腾优先；换形态、重登、重载不补次数。 |

## 四足兽形

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 四足兽形关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 四足步幅 I | 25 | `changede:feral_core` | 基础地面移速每级增加 2.5%。各级按本级贡献叠加。 |
| 四足步幅 II | 50 | `changede:feral_stride_1` | 基础地面移速每级增加 2.5%。各级按本级贡献叠加。 |
| 兽形力量 I | 25 | `changede:feral_core` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 兽形力量 II | 50 | `changede:feral_strength_1` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 四足缓冲 I | 25 | `changede:feral_core` | 摔落伤害减免每级增加 10%。各级按本级贡献叠加。 |
| 四足缓冲 II | 50 | `changede:feral_landing_1` | 摔落伤害减免每级增加 10%。各级按本级贡献叠加。 |

## 鱼类

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 鱼类关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 鱼类游速 I | 25 | `changede:fish_core` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 鱼类游速 II | 50 | `changede:fish_swimming_1` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 鱼类体魄 I | 25 | `changede:fish_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 鱼类体魄 II | 50 | `changede:fish_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 鱼类觅食 I | 25 | `changede:fish_core` | 食物原始饱和度加成每级增加 10%。各级按本级贡献叠加。 |
| 鱼类觅食 II | 50 | `changede:fish_energy_1` | 食物原始饱和度加成每级增加 10%。各级按本级贡献叠加。 |

## 人形

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 人形关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 人形力量 I | 25 | `changede:humanoid_core` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 人形力量 II | 50 | `changede:humanoid_strength_1` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 人形体魄 I | 25 | `changede:humanoid_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 人形体魄 II | 50 | `changede:humanoid_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 人形耐力 I | 25 | `changede:humanoid_core` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |
| 人形耐力 II | 50 | `changede:humanoid_energy_1` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |

## 昆虫

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 昆虫关键节点（关键） | 25 | `changede:arthropod_core` | 最大生命减半；森林或实际淋雨每 4 秒恢复 1 点生命。节肢杀手由父节点控制。 |
| 环境恢复 1 | 25 | `changede:insect_core` | 满足森林或淋雨条件时每 3 秒恢复 1 点生命，两条件不重复叠加。 |
| 环境恢复 2 | 50 | `changede:insect_recovery_1` | 满足森林或淋雨条件时每 2 秒恢复 1 点生命，两条件不重复叠加。 |
| 昆虫体魄 I | 25 | `changede:insect_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 昆虫体魄 II | 50 | `changede:insect_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 觅食效率 I | 25 | `changede:insect_core` | 食物原始饱和度加成每级增加 10%。各级按本级贡献叠加。 |
| 觅食效率 II | 50 | `changede:insect_energy_1` | 食物原始饱和度加成每级增加 10%。各级按本级贡献叠加。 |

## 陆

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 陆关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 陆行步幅 I | 25 | `changede:land_core` | 基础地面移速每级增加 2.5%。各级按本级贡献叠加。 |
| 陆行步幅 II | 50 | `changede:land_stride_1` | 基础地面移速每级增加 2.5%。各级按本级贡献叠加。 |
| 陆行耐力 I | 25 | `changede:land_core` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |
| 陆行耐力 II | 50 | `changede:land_endurance_1` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |
| 站稳脚步 I | 25 | `changede:land_core` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 站稳脚步 II | 50 | `changede:land_balance_1` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |

## 水生哺乳类

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 水生哺乳类关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 水生游速 I | 25 | `changede:marine_mammal_core` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 水生游速 II | 50 | `changede:marine_mammal_swimming_1` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 水生体魄 I | 25 | `changede:marine_mammal_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 水生体魄 II | 50 | `changede:marine_mammal_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 水生耐力 I | 25 | `changede:marine_mammal_core` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |
| 水生耐力 II | 50 | `changede:marine_mammal_energy_1` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |

## 鱼尾

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 鱼尾关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 尾鳍推进 I | 25 | `changede:mer_core` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 尾鳍推进 II | 50 | `changede:mer_swimming_1` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 鱼尾体魄 I | 25 | `changede:mer_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 鱼尾体魄 II | 50 | `changede:mer_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 水下耐力 I | 25 | `changede:mer_core` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |
| 水下耐力 II | 50 | `changede:mer_energy_1` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |

## 其他哺乳类

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 其他哺乳类关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 哺乳类体魄 I | 25 | `changede:other_mammal_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 哺乳类体魄 II | 50 | `changede:other_mammal_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 哺乳类步伐 I | 25 | `changede:other_mammal_core` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 哺乳类步伐 II | 50 | `changede:other_mammal_stride_1` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 哺乳类觅食 I | 25 | `changede:other_mammal_core` | 食物原始饱和度加成每级增加 10%。各级按本级贡献叠加。 |
| 哺乳类觅食 II | 50 | `changede:other_mammal_energy_1` | 食物原始饱和度加成每级增加 10%。各级按本级贡献叠加。 |

## 植物

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 植物关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 植物韧皮 I | 25 | `changede:plant_core` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |
| 植物韧皮 II | 50 | `changede:plant_armor_1` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |
| 植物体魄 I | 25 | `changede:plant_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 植物体魄 II | 50 | `changede:plant_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 植物恢复 I | 25 | `changede:plant_core` | 自然回血速度加成每级增加 10%。各级按本级贡献叠加。 |
| 植物恢复 II | 50 | `changede:plant_recovery_1` | 自然回血速度加成每级增加 10%。各级按本级贡献叠加。 |

## 鳐系

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 鳐系关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 鳐系推进 I | 25 | `changede:ray_core` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 鳐系推进 II | 50 | `changede:ray_swimming_1` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 鳐系稳定 I | 25 | `changede:ray_core` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 鳐系稳定 II | 50 | `changede:ray_balance_1` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 鳐系耐力 I | 25 | `changede:ray_core` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |
| 鳐系耐力 II | 50 | `changede:ray_energy_1` | 饥饿消耗减免每级增加 2.5%。各级按本级贡献叠加。 |

## 爬行类

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 爬行类关键节点（关键） | 25 | `changede:common_strength_2` | 爆炸伤害减免 30%；对猫系目标造成的伤害降低 20%。 |
| 爆炸防护 1 | 25 | `changede:reptile_core` | 爆炸减免最终提高到 40%，不会自动获得自爆。 |
| 爆炸防护 2 | 50 | `changede:reptile_blast_guard_1` | 爆炸减免最终提高到 50%，不会自动获得自爆。 |
| 鳞甲强化 I | 25 | `changede:reptile_core` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |
| 鳞甲强化 II | 50 | `changede:reptile_scales_1` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |
| 扎根防御 I | 25 | `changede:reptile_core` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 扎根防御 II | 50 | `changede:reptile_balance_1` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |

## 海

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 海关键节点（关键） | 25 | `changede:common_strength_2` | 水中减伤 20%；火焰伤害 +25%、下界普通伤害 +15%，同时满足取较高倍率。 |
| 水下作业 I | 25 | `changede:sea_core` | 消除无水下速掘时的水下挖掘惩罚；保留悬浮惩罚。 |
| 水下作业 II | 50 | `changede:sea_work_1` | 水下挖掘速度额外提高 25%。 |
| 水中防御 | 25 | `changede:sea_core` | 水中伤害减免最终提升到 30%，替代初级 20%。 |
| 温差适应 | 50 | `changede:sea_core` | 火焰额外惩罚降到 12.5%、下界额外惩罚降到 7.5%。 |
| 游泳强化 I | 25 | `changede:sea_core` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 游泳强化 II | 50 | `changede:sea_swimming_1` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 水生强壮 I | 25 | `changede:sea_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 水生强壮 II | 50 | `changede:sea_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 鲨系

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 鲨系关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 鲨系力量 I | 25 | `changede:shark_core` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 鲨系力量 II | 50 | `changede:shark_strength_1` | 攻击伤害每级增加 0.5 点。各级按本级贡献叠加。 |
| 鲨系推进 I | 25 | `changede:shark_core` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 鲨系推进 II | 50 | `changede:shark_swimming_1` | 基础游泳速度每级增加 5%。各级按本级贡献叠加。 |
| 鲨系体魄 I | 25 | `changede:shark_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 鲨系体魄 II | 50 | `changede:shark_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 蛇身

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 蛇身关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 蛇身移动 I | 25 | `changede:snake_core` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 蛇身移动 II | 50 | `changede:snake_stride_1` | 基础地面移速每级增加 2%。各级按本级贡献叠加。 |
| 盘踞稳定 I | 25 | `changede:snake_core` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 盘踞稳定 II | 50 | `changede:snake_balance_1` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 蛇身体魄 I | 25 | `changede:snake_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 蛇身体魄 II | 50 | `changede:snake_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 半人马形

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 半人马形关键节点（关键） | 25 | `changede:common_strength_2` | 开启本分类的成长路线；不额外改变原 Form 的基础能力。 |
| 重心稳定 I | 25 | `changede:taur_core` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 重心稳定 II | 50 | `changede:taur_balance_1` | 击退抵抗每级增加 2.5%。各级按本级贡献叠加。 |
| 厚实体魄 I | 25 | `changede:taur_core` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |
| 厚实体魄 II | 50 | `changede:taur_armor_1` | 护甲每级增加 0.5 点。各级按本级贡献叠加。 |
| 半人马强壮 I | 25 | `changede:taur_core` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |
| 半人马强壮 II | 50 | `changede:taur_health_1` | 最大生命每级增加 1 点。各级按本级贡献叠加。 |

## 通用

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 胶兽通用（关键） | 0 |  | 通用成长的起点。 |
| 强壮X I | 25 | `changede:latex_mastery` | 最大生命值 +1 |
| 强壮Y I | 25 | `changede:vitality` | 护甲 +1 |
| 力量 I | 25 | `changede:resilience` | 攻击伤害 +1 |
| 生命力 I | 25 | `changede:common_strength` | 自然回血速度 +20%；仍需满足原版回血条件。 |
| 耐饿X I | 25 | `changede:common_regeneration_1` | 饥饿消耗减少 10%，多个节点加算。 |
| 耐饿Y I | 25 | `changede:common_hunger_x_1` | 食物提供的饱和度增加原始值的 100%；多个节点加算，上限另由耐饿Z提高。 |
| 耐饿Z I | 25 | `changede:common_hunger_y_1` | 饱和度上限 +1；不直接恢复饥饿或饱和度。 |
| 强壮X II | 25 | `changede:common_hunger_z_1` | 最大生命值 +1 |
| 强壮Y II | 25 | `changede:common_endurance` | 护甲 +1 |
| 力量 II | 25 | `changede:common_recovery` | 攻击伤害 +1 |
| 强壮X III | 25 | `changede:common_strength_2` | 最大生命值 +1 |
| 强壮Y III | 25 | `changede:common_vitality_3` | 护甲 +1 |
| 力量 III | 25 | `changede:common_armor_3` | 攻击伤害 +1 |
| 生命力 II | 25 | `changede:common_strength_3` | 自然回血速度 +20%；仍需满足原版回血条件。 |
| 耐饿X II | 25 | `changede:common_regeneration_2` | 饥饿消耗减少 10%，多个节点加算。 |
| 耐饿Y II | 25 | `changede:common_hunger_x_2` | 食物提供的饱和度增加原始值的 100%；多个节点加算，上限另由耐饿Z提高。 |
| 耐饿Z II | 25 | `changede:common_hunger_y_2` | 饱和度上限 +1；不直接恢复饥饿或饱和度。 |
| 强壮X IV | 25 | `changede:common_hunger_z_2` | 最大生命值 +1 |
| 强壮Y IV | 25 | `changede:common_vitality_4` | 护甲 +1 |
| 力量 IV | 25 | `changede:common_armor_4` | 攻击伤害 +1 |
| 生命力 III | 25 | `changede:common_strength_4` | 自然回血速度 +20%；仍需满足原版回血条件。 |
| 耐饿X III | 25 | `changede:common_regeneration_3` | 饥饿消耗减少 10%，多个节点加算。 |
| 耐饿Y III | 25 | `changede:common_hunger_x_3` | 食物提供的饱和度增加原始值的 100%；多个节点加算，上限另由耐饿Z提高。 |
| 耐饿Z III | 25 | `changede:common_hunger_y_3` | 饱和度上限 +1；不直接恢复饥饿或饱和度。 |

## 白胶

| 技能 | XP | 前置 | 效果 |
| --- | --- | --- | --- |
| 白胶关键节点（关键） | 25 | `changede:common_strength_2` | 开启白胶同族身体成长。 |
| 强壮X I | 25 | `changede:white_vitality` | 最大生命值 +1 |
| 强壮Y I | 25 | `changede:white_endurance` | 护甲 +1 |
| 力量 I | 25 | `changede:white_continuation` | 攻击伤害 +1 |
| 生命力 I | 25 | `changede:white_strength` | 自然回血速度 +20%；仍需满足原版回血条件。 |
| 耐饿X I | 25 | `changede:white_regeneration_1` | 饥饿消耗减少 10%，多个节点加算。 |
| 耐饿Y I | 25 | `changede:white_hunger_x_1` | 食物提供的饱和度增加原始值的 100%；多个节点加算，上限另由耐饿Z提高。 |
| 耐饿Z I | 25 | `changede:white_hunger_y_1` | 饱和度上限 +1；不直接恢复饥饿或饱和度。 |
| 强壮X II | 25 | `changede:white_hunger_z_1` | 最大生命值 +1 |
| 强壮Y II | 25 | `changede:white_persistence` | 护甲 +1 |
| 力量 II | 25 | `changede:white_constitution` | 攻击伤害 +1 |
| 生命力 II | 25 | `changede:white_strength_2` | 自然回血速度 +20%；仍需满足原版回血条件。 |
| 耐饿X II | 25 | `changede:white_regeneration_2` | 饥饿消耗减少 10%，多个节点加算。 |
| 耐饿Y II | 25 | `changede:white_hunger_x_2` | 食物提供的饱和度增加原始值的 100%；多个节点加算，上限另由耐饿Z提高。 |
| 耐饿Z II | 25 | `changede:white_hunger_y_2` | 饱和度上限 +1；不直接恢复饥饿或饱和度。 |
