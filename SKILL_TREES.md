# 胶兽技能树：自然成长阶段

当前实现对应 issue #8 的第一阶段：连续 DAG、独立视觉区域、`rewards[]`、学习记录与生效状态分离，以及服务端条件说明。WLP、Power 奖励、物品条件、多级节点和洗点继续留在 [后续设计](SKILL_TREE_DESIGN.md) 中。

## 连续画布与区域

打开背包左侧“技能”。拖动平移，滚轮围绕鼠标缩放，方向键移动，“回到主干”恢复起点。普通节点显示效果图标，名称和条件放在 Tooltip；关键节点保留标题。

所有已加载分支均保留在同一画布中，与当前 Form 是否适用无关。黑胶、白胶坐标已经分开，Yufeng 在黑胶分支左侧继续深入；右侧始终延续上一范围的成长，且不要求学习左侧的新关键节点。切换 Form 不重新居中、不删除学习记录。主方向向下，任意深度和跨文件前置保持 DAG 语义。

视觉区域来自客户端资源包 `assets/<namespace>/latex_skill_visuals/*.json`，默认文件为 `assets/changede/latex_skill_visuals/default.json`。与游戏逻辑的 `data/.../latex_skill_trees` 分离；资源包按文件资源位置覆盖，F3+T 重载视觉，服务器 `/reload` 重载技能定义。新文件中的主题和区域 ID 必须唯一，区域必须引用已定义主题；非法配置保留上一份有效视觉数据。

默认包含四个主题：实验室白墙/蓝条纹墙、黑胶、白胶、Yufeng。Yufeng 使用紫色调胶块、稀疏翼纹、菱形节点和双股连线，不再继承黑胶区域的全部样式。胶块和墙面使用 Changed 方块图集，保留动画及资源包替换。

```json
{
  "regions": [
    {
      "id": "example:dark_area",
      "x": -4, "y": 3.5,
      "width": 4.5, "height": 7.5,
      "theme": "changede:dark_latex",
      "feather": 0.8, "priority": 10
    }
  ]
}
```

区域坐标与节点共用网格，允许小数；横向一个单位为 150，纵向一个单位为 64。`x/y` 表示左上角。`feather` 是围绕矩形边界的完整平滑过渡宽度，边界处覆盖率为 50%。没有区域覆盖时回到实验室。按 `priority` 从低到高叠加，同优先级按 ID 排序；高优先级的 Yufeng 区域可以覆盖黑胶区域，并在四边及角落平滑过渡。

每个背景 tile 使用所在画布位置的覆盖权重绘制多层 alpha 混合。节点和连线使用同一权重混合主题色，线沿路径分段采样。背景固定在画布坐标中，拖动逐步进入对应区域，缩放和再次打开不改变区域归属；不由节点位置推导，也不由玩家当前形态切换背景。

`themes` 配置图集 sprite `tile/stripe`、`tint`、四状态颜色 `active/available/dormant/locked`、节点底色 `surface` 和 `motif`（`grid/latex/wings`），颜色格式为 `#RRGGBB`。最多 64 个主题、256 个区域；区域 ID 和主题 ID 都使用命名空间。

## 当前成长清单

通用根节点免费且不提供额外属性；后面有 24 个独立节点，每个消耗 25 XP。重复条目按 I/II/III/IV 标记，并保留独立 ID、成本及学习记录。

关键节点前的 10 个通用节点：强壮X → 强壮Y → 力量 → 生命力 → 耐饿X → 耐饿Y → 耐饿Z → 强壮X → 强壮Y → 力量。

第 10 个节点左侧分出已存在的黑胶、白胶关键节点；没有为未知种族编造关键节点。右侧通用延续有 14 个节点：强壮X → 强壮Y → 力量 → 生命力 → 耐饿X → 耐饿Y → 耐饿Z → 强壮X → 强壮Y → 力量 → 生命力 → 耐饿X → 耐饿Y → 耐饿Z。白胶暂时保留同样的 14 个同族成长节点；黑胶改为下面的专属清单，仅匹配的胶体类型生效。

全图共 64 个节点：25 个通用（包含免费根节点），16 个黑胶、15 个白胶（均包含种族关键节点），以及已有的 8 个 Yufeng 节点。背景区域随新增深度扩大，节点坐标没有重叠；Yufeng 区域避开白胶节点。

| 节点 | 每次学习效果 | 叠加 |
| --- | --- | --- |
| 强壮X | 最大生命值 +1（半颗心） | 加算 |
| 强壮Y | 护甲 +1 | 加算 |
| 力量 | 攻击伤害 +1 | 加算 |
| 生命力 | 自然回血速度 +20% | 加算，仍需饥饿值 ≥18 且开启自然回血规则 |
| 耐饿X | 正的饥饿消耗减少 10% | 减免比例加算，最多 100%；包含自然回血消耗 |
| 耐饿Y | 食物提供的饱和度 +100% 原始值 | 依次 2/3/4 倍，不增加食物恢复的饥饿点数 |
| 耐饿Z | 饱和度上限 +1 | 加算；满饥饿时从 20 提升为 21/22/23，不直接补充饱和度 |

本次数值与关键节点后同族归属采用先前说明的默认选择，仍可依据用户后续回复调整。只学习全部通用节点时，总计生命 +4、护甲 +4、攻击 +4、自然回血速度 +60%、饥饿消耗 -30%、食物饱和度 4 倍、饱和度上限 +3；匹配的同族成长再按其独立节点叠加。

回血采用原版 FoodData 的自然回血计时器加速，保持每次治疗量、治疗饥饿消耗和饥饿伤害分支；不放大药水或其他来源的治疗。耐饿X作用于 FoodData.addExhaustion；耐饿Y和Z作用于普通食物的饱和度计算及上限。未学习时四个属性取原版等价默认值。失去额外容量时，已有饱和度收回到当前允许上限；学习记录仍保留。

### 黑胶通用成长

黑胶关键节点之后、特殊 Form 分叉之前：白胶克星 I → 白胶抵抗 I → 白雾抵抗 I → 护甲适配 I → 武器适配 I。

武器适配 I 左侧分出 Yufeng 关键节点，右侧继续：胶锭武器适配性 I → 武器适配 II → 白胶抵抗 II → 白胶克星 II → 武器适配 III → 胶锭武器适配性 II → 护甲适配 II → 胶锭武器适配性 III → 胶锭武器适配性 IV → 白胶克星 III。右侧不需要解锁 Yufeng，也不需要当前形态为 Yufeng。只为已定义的特殊 Form 添加关键节点。

15 个普通节点各消耗 25 XP，全部使用 Attribute 奖励；不授予主动能力。

| 节点 | 每次学习效果 | 全部黑胶节点累计 |
| --- | --- | --- |
| 白胶克星 | 对白胶系列目标伤害 +1 | +3 点 |
| 白胶抵抗 | 白胶系列攻击伤害 -10% | -20% |
| 白雾抵抗 | 白雾伤害 -10% | -10% |
| 护甲适配 | 穿戴至少一件护甲时，额外护甲 +1 | +2 护甲 |
| 武器适配 | 主手或副手持有武器时，额外护甲 +1 | +3 护甲 |
| 胶锭武器适配性 | 使用胶锭武器攻击时，伤害 +1 | +4 点 |

白胶系列包含 Changed 白胶实体和白胶形态玩家；抵抗按伤害来源的攻击者分类，包含其投射物。克星与胶锭武器加伤为固定点数，在原版护甲结算前加算，沿用旧比例奖励时先计算旧倍率。白雾使用独立的 `changede:white_fog` 伤害类型，保持原来的绕过护甲行为；黑雨保留 `changede:latex_weather`，不会被白雾抵抗误减免。

胶锭武器由 `changede:latex_weapons` 物品标签定义，目前只有 `changede:latex_spear`（胶锭矛）。加伤只用于实际武器的玩家攻击，包含主副手胶锭矛戳刺和冲锋；不因为副手携带胶锭矛而给其他主手武器加伤。普通武器按武器类、攻击属性或 `changede:skill_weapons` 标签识别。护甲适配按是否穿戴判断，每节点只算一次，与护甲件数无关；两类适配条件同时满足时合计 +5 护甲，卸下装备即移除对应加成。

旧黑胶身体成长占位节点已从默认定义移出，保留其历史学习数据但不继续生效或自动退款；黑胶关键节点与 Yufeng 节点 ID 保留，Yufeng 的前置改为武器适配 I。

## 四种状态与条件

| 标记 | 状态 | 含义 |
| --- | --- | --- |
| ● | 已解锁且生效 | 当前 Form 适用，所有前置也生效 |
| ◉ | 已解锁但未生效 | 培养记录保留，当前形态或前置不适用 |
| ✦ | 可购买 | 当前适用，前置及 XP 条件满足 |
| ○ | 未解锁 | Tooltip 显示未满足条件 |

服务端返回 `SkillAvailability(unlocked, active, purchasable, reasons)`，同时发送所有 `requirements` 的通过/失败标记。前端直接使用 `purchasable`，服务端在点击时重新校验当前 Form、学习记录、前置、玩家状态和 XP。经验只阻止购买，已经学习的节点不会因余额降低而失效。

Tooltip 包含条件清单及由实际 `rewards[]` 生成的效果。条件区显示 ✓/✗，区分未学习的父节点、已学习但未生效的父节点、非胶兽形态、黑胶/白胶类型、具体 Form 或实体标签、XP 不足以及死亡/旁观状态。原因 ID 使用命名空间；未来物品、科技和互斥可增加新原因，当前不实现这些要求。

进度继续保存为 `changede_latex_skills` → 树 ID → 节点 ID → 布尔值，仍沿用各树 ID；内容替换移出的节点保留历史数据。死亡、重新登录和跨 Form 切换保留进度；人类形态暂停所有胶兽成长效果。适用属性与飞行数据每秒刷新，解锁立即刷新；装备变化立即重算条件护甲。

## Reward 与技能数据

文件仍位于 `data/<namespace>/latex_skill_trees/<tree>.json`。`scope` 为 `global/group/form`；`latex_type`、`entity_tag`、`forms` 为可组合选择条件，组合时全部满足。通用树不允许附加选择条件，群体/个体树至少指定一种选择条件。

```json
{
  "scope": "global",
  "nodes": [
    {
      "id": "example:vitality",
      "title": "skill.example.vitality",
      "description": "skill.example.vitality.description",
      "cost": 25,
      "parents": ["changede:latex_mastery"],
      "x": 2, "y": 1, "key": false,
      "rewards": [
        { "type": "changede:attribute", "attribute": "minecraft:generic.max_health", "amount": 2 }
      ]
    }
  ]
}
```

当前只注册 `changede:attribute` 和 `changede:none`；空奖励列表也合法。`SkillReward` 提供 `apply/remove` 生命周期和快照描述，后续 RewardType 通过工厂注册扩展。没有注册 `changede:power`，未知类型会拒绝本次重载。

Attribute 的 `attribute` 为注册属性 ID，`amount` 为原版属性单位；默认 `operation: addition`，还支持 `multiply_base`、`multiply_total`。非加法数值上限为 1，加法为 100，均要求非负有限数。通用范围允许 Minecraft 属性以及四个 CE 通用成长属性：自然回血速度、饥饿消耗减免、食物饱和度倍率、额外饱和度上限。其他身体特色属性仍放群体/个体范围。

| 属性 | 用途 |
| --- | --- |
| `minecraft:generic.max_health` | 最大生命，+2 为一颗心 |
| `minecraft:generic.attack_damage` | 攻击伤害 |
| `minecraft:generic.armor` / `armor_toughness` | 护甲与韧性 |
| `minecraft:generic.movement_speed` | 地面移动速度（可配置，当前清单未使用） |
| `minecraft:generic.knockback_resistance` | 抗击退，0.05 为 +5% |
| `changede:regeneration_speed` | 自然回血计时倍率，基础 1；每节点 +0.2 |
| `changede:exhaustion_reduction` | 饥饿消耗减免比例，基础 0；每节点 +0.1 |
| `changede:food_saturation` | 食物提供的饱和度倍率，基础 1；每节点 +1 |
| `changede:saturation_capacity` | 相对于当前饥饿值的额外饱和度上限，基础 0；每节点 +1 |
| `changede:flight_control` | 原有飞行速度及起飞/滑翔推进增幅，0–1；不授予飞行权限 |
| `changede:landing_resistance` | 摔落伤害减免比例，0–1 |
| `changede:damage_vs_white` / `damage_vs_dark` | 原有群体克制加伤比例，0–1 |
| `changede:flat_damage_vs_white` | 对白胶目标固定加伤，基础 0 |
| `changede:white_latex_resistance` | 白胶攻击伤害减免，0–1 |
| `changede:white_fog_resistance` | 白雾伤害减免，0–1 |
| `changede:armor_adaptation` | 穿戴护甲时的额外护甲，基础 0 |
| `changede:weapon_adaptation` | 持有武器时的额外护甲，基础 0 |
| `changede:latex_weapon_damage` | 胶锭武器实际攻击固定加伤，基础 0 |

特色身体加成已经变为可同步的玩家 Attribute；现有物理与伤害事件读取属性，不再读取节点 `power`。飞行控制保持现有速度/推进强化的实际语义，不宣称新增转向机制或饥饿系统。默认内容已按新的强壮/力量/生命力/耐饿清单重排；飞行耐力当前体现最大生命加成。

每个 Attribute Modifier 的 UUID 由节点 ID 和 Reward 索引稳定生成，不会因重复刷新累加。同一节点的多个奖励以及不同节点的相同属性分别保留来源。重载改值、改操作、删奖励、删节点、改属性或切换 Form 时，协调器移除过期 CE 技能 Modifier，再补充当前目标集合；其他系统 Modifier 不受影响。

旧 `power/amount` JSON 必须迁移，加载器遇到旧字段会明确拒绝并保留先前有效定义，避免静默丢效果。旧版 Modifier 会在刷新时清理，已有学习记录不变。起飞/冲刺两个节点从默认内容中移出，主动能力不再加入环形候选；旧记录和能力注册 ID 保留，WLP 阶段再设计实际 Power 授予机制。本阶段不因移出节点自动退款。

## XP 与校验

`cost` 现在是原版 XP points，不再是等级，范围 0–1000000。默认普通成长为 25 XP，部分高级节点为 40/50 XP，主干起点免费；创造模式免费但仍要求适用形态和生效前置。

钱包由当前等级和经验条推算，不使用可能与附魔后余额不一致的 `totalExperience`。扣款调用原版 `giveExperiencePoints(-cost)`，验证实际扣除精确 points 后才保存学习记录；若 Forge XP 事件取消或改变扣款，则恢复经验字段和分数并拒绝解锁。重复点击已学节点不会再次扣款。

节点 ID 全局唯一，坐标为整数，绝对值不超过 10000；同一文件不允许重叠。默认跨文件坐标也已避开重叠。每节点最多 16 个奖励，所有定义最多 2048 个节点。未知前置、重复前置、重复 ID、循环和非法奖励会拒绝整次技能重载，保留上一次有效图。网络协议为 6，联机双方需安装相同版本。

## 验证

`tests/skill/SkillGraphTest.java` 覆盖乱序输入、分叉、任意深度关键节点、2048 节点长链、失效祖先、未知/重复前置和循环。`SkillGrowthTest.java` 覆盖 XP 等级分段与部分经验条、四种状态、区域边界/角落/负坐标、权重归一化及颜色混合。`validate_skill_resources.py` 检查默认 DAG、分支无重叠、仅两种奖励、区域覆盖、旁支前置和中英文键。

编译使用仓库 Gradle 8.14.4 与 Java 17 toolchain：`build --offline`。纯 Java 测试用 JDK 17+ 的 `javac/java` 编译运行；资源检查运行 `python tests/skill/validate_skill_resources.py`。`SkillNutritionTest.java` 另外验证默认营养行为、减耗、双倍食物饱和度、额外容量、原版回血条件及小比例回血计时。`SkillCombatRulesTest.java` 验证固定加伤与比例加伤的区别、重复节点叠加、抵抗比例及装备护甲的条件与移除；资源检查同时校验黑胶 15 节点顺序、累计数值及新伤害类型和武器标签。

仍需游戏内验收：自然回血与自然回血规则关闭、饥饿伤害计时不受生命力影响、跑跳及回血消耗减免、普通食物双倍饱和度和饱和度 20 以上的保存/重登、切换 Form 后额外容量收回；以及连续拖动通用→黑胶→Yufeng 的背景过渡及最小缩放性能；四种节点状态和中英文 Tooltip；Yufeng→白胶→Yufeng 不丢记录且效果恢复；XP 跨级精确扣款和双击；资源重载改/删多个奖励不重复累加；死亡和重新登录；无飞行形态不会得到飞行权限。

黑胶额外验收：白胶实体与白胶玩家的加伤/抵抗、普通目标不受克星影响、白雾抵抗不影响黑雨、主副手胶锭矛戳刺/冲锋加伤、副手持矛不增强主手其他武器、护甲及武器适配在装备切换和 Form 切换后正确移除。
