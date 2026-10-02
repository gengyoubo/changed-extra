# Form 归属表

本表作为已实装技能树 forms 选择器的来源；游戏读取各分支 JSON，不直接加载这份 Markdown。入口复用原黑白胶分叉点 `changede:common_strength_2`（前 10 个通用成长之后），顶层分类入口直接依赖它；节肢动物先设关键节点，再分昆虫／蛛形等详细节点，不新建通用入口。机器可读清单见 [FORM_AFFILIATIONS.json](FORM_AFFILIATIONS.json)，机制设计见 [SKILL_TREE_DESIGN.md](SKILL_TREE_DESIGN.md)。

## 核对范围与证据

- Changed **0.15.7**：当前依赖 `curse.maven:changed-minecraft-mod-661527:8217446`，87 个固定注册 Form ID。核对 [v0.15.7 注册源码](https://github.com/LtxProgrammer/Changed-Minecraft-Mod/blob/v0.15.7/src/main/java/net/ltxprogrammer/changed/init/ChangedTransfurVariants.java) 与当前实际依赖 JAR。
- Addon **2.9.2**：当前依赖 `curse.maven:changed-addon-plus-711303:8515645`，62 个固定注册 Form ID。通过 Vineflower 1.10.1 阅读实际依赖的源码反编译结果；[上游仓库](https://github.com/Foxyas/Changed-Addon-Rework) 当前 Main 已是 2.9.9，没有用新版本替换本次审计。
- CE 本地特殊 Form：`local/forms/index.json` 中的 1 个明确 profile 单列。合计 **150 个明确 Form ID**。用户自定义和运行时远程特殊 profile 不存在固定完整清单，另列默认规则。
- JSON 保留依赖版本、JAR SHA-256、注册字段、实体类、身体/胶体来源方法、运动能力及族群证据，便于升级依赖时重新核对。反编译中间文件位于 `build/form-audit/*-sources`，不随仓库提交。

## 分类判定

- **每个维度独立留空**。某维度查不到或有歧义就记“无归属”，不把整行其他已有证据的归属一并删除，也不创建“未知族群/非胶”分支。
- **胶体**按实体 `getLatexType()` 的实现与继承链确认 `DARK_LATEX` / `WHITE_LATEX`，其他值均无归属。名称带黑/白、颜色、面具标记不替代实际胶体类型。
- **身体**按实体 `getEntityShape()`：ANTHRO=双足、FERAL=四足兽形、TAUR=半人马形、NAGA=蛇身、MER=鱼尾。动态自定义身体不固定归类；本表体现代码身体标记。
- **海**要求明确水下呼吸配置、Aquatic/SemiAquatic 实体代码，或水獭明确的游泳强化与 60 秒空气容量。普通 Form 的轻微游泳速度加成不单独证明海归属；海归属也不自动授予水下呼吸。
- **陆**要求源码支持空气呼吸与正地面移动速度，且身体不是鱼尾。仅水下呼吸的形态不加入陆；蛇身可有陆归属。
- **空**要求注册配置明确 `glide()`；多段跳单独记在 capabilities 中，不等同于飞行，也不靠有翅膀的名字推断。因而此版蜜蜂、飞蛾只有多段跳证据，暂不归空；未来明确授予滑翔后再更新分类。
- **族群**使用具体实体的明确物种名称和资源标签；猫系 Exp2 另有实体代码的猫叫声证据。复用祖先行为、模型、害怕苦力怕或角色名不足以判定族群，Avali、Protogen、Yufeng 等无法按现有普通族群可靠判定时留空。
- 混合身份允许多个族群；虎鲨的“虎”本身不构成猫系证据。Stiger 的源码资源名“蜘蛛虎”支持猫/蛛形，鱿鱼犬支持犬/头足，龙雪豹鲨支持猫/龙/鲨。

- **节肢动物层级**：有明确昆虫或蛛形证据时，同时加入节肢动物父归属；无法辨别的族群不据此补猜。昆虫与蛛形是节肢动物下的细分分支，节肢杀手影响统一由节肢动物关键节点控制，昆虫生命减半和环境回血仍属于昆虫分支。JSON 的 family_hierarchy 与 branch_parents 记录该层级。

## 容易误判的实例

- Yufeng 使用狼系祖先代码，粉鹿复用粉色翼龙代码，Gnoll Taur 复用狼系行为；这些复用不作为额外族群归属。
- `form_white_latex_wolf/*`、`form_dark_latex_wolf_pup`、`form_white_latex_centaur` 等名称看似黑/白胶，但当前依赖源码继承 `ChangedEntity.getLatexType() = NONE`，因此胶体栏留空；这属于代码与名称差异，后续若修复运行时类型再更新清单。
- 雌性蝠鲼为 MER，雄性为 ANTHRO；同一命名族群可以拥有不同身体分支。
- `form_latex_snep` 和 `form_latex_snep_feral` 当前都使用 `LatexSnepEntity` 的 FERAL 标记，按代码同归四足兽形。

## 完整归属

| Form ID | 名称 | 胶体 | 身体 | 运动 | 族群 | 实体源码身份 |
| --- | --- | --- | --- | --- | --- | --- |
| `changed:form_beifeng` | 北风 | 无归属 | 双足 | 陆 | 无归属 | `Beifeng` |
| `changed:form_crystal_wolf` | 魔晶狼 | 无归属 | 双足 | 陆 | 犬系 | `CrystalWolf` |
| `changed:form_crystal_wolf_horned` | 有角魔晶狼 | 无归属 | 双足 | 陆 | 犬系 | `CrystalWolfHorned` |
| `changed:form_custom_latex` | 自定义胶兽 | 无归属 | 无归属 | 无归属 | 无归属 | `CustomLatexEntity` |
| `changed:form_dark_dragon` | 黑胶龙 | 黑胶 | 双足 | 陆、空 | 龙系 | `DarkDragon` |
| `changed:form_dark_latex_double_yufeng` | 黑胶双头驭风 | 黑胶 | 双足 | 陆、空 | 无归属 | `DarkLatexDoubleYufeng` |
| `changed:form_dark_latex_wolf/female` | 雌性黑胶狼 | 黑胶 | 双足 | 陆 | 犬系 | `DarkLatexWolfFemale` |
| `changed:form_dark_latex_wolf/male` | 雄性黑胶狼 | 黑胶 | 双足 | 陆 | 犬系 | `DarkLatexWolfMale` |
| `changed:form_dark_latex_wolf_partial` | 部分兽化黑胶狼 | 黑胶 | 双足 | 陆 | 犬系 | `DarkLatexWolfPartial` |
| `changed:form_dark_latex_wolf_pup` | 黑胶狼幼崽 | 无归属 | 四足兽形 | 陆 | 犬系 | `DarkLatexWolfPup` |
| `changed:form_dark_latex_yufeng` | 黑胶驭风 | 黑胶 | 双足 | 陆、空 | 无归属 | `DarkLatexYufeng` |
| `changed:form_gas_skunk` | 毒气臭鼬 | 无归属 | 双足 | 陆 | 其他哺乳类 | `GasSkunk` |
| `changed:form_gas_tiger` | 毒气虎 | 无归属 | 双足 | 陆 | 猫系 | `GasTiger` |
| `changed:form_gas_wolf/female` | 雌性毒气狼 | 无归属 | 双足 | 陆 | 犬系 | `GasWolfFemale` |
| `changed:form_gas_wolf/male` | 雄性毒气狼 | 无归属 | 双足 | 陆 | 犬系 | `GasWolfMale` |
| `changed:form_gas_wolf_pup` | 毒气狼幼崽 | 无归属 | 四足兽形 | 陆 | 犬系 | `GasWolfPup` |
| `changed:form_green_lizard` | 胶液蜥蜴 | 无归属 | 双足 | 陆 | 爬行类 | `GreenLizard` |
| `changed:form_latex_alien` | 胶液外星人 | 无归属 | 双足 | 陆 | 无归属 | `LatexAlien` |
| `changed:form_latex_bee` | 胶液蜜蜂 | 无归属 | 双足 | 陆 | 节肢动物、昆虫 | `LatexBee` |
| `changed:form_latex_benign_orca` | 束缚胶液虎鲸 | 无归属 | 双足 | 海、陆 | 水生哺乳类 | `LatexBenignOrca` |
| `changed:form_latex_benign_wolf` | 束缚胶狼 | 无归属 | 双足 | 陆 | 犬系 | `LatexBenignWolf` |
| `changed:form_latex_blue_dragon` | 胶液蓝龙 | 无归属 | 双足 | 陆 | 龙系 | `LatexBlueDragon` |
| `changed:form_latex_blue_wolf` | 胶液蓝狼 | 无归属 | 双足 | 陆 | 犬系 | `LatexBlueWolf` |
| `changed:form_latex_crocodile` | 胶液鳄鱼 | 无归属 | 双足 | 陆 | 爬行类 | `LatexCrocodile` |
| `changed:form_latex_crow` | 胶液乌鸦 | 无归属 | 双足 | 陆、空 | 鸟系 | `LatexCrow` |
| `changed:form_latex_deer` | 胶液鹿 | 无归属 | 双足 | 陆 | 其他哺乳类 | `LatexDeer` |
| `changed:form_latex_eel` | 胶液鳗鱼 | 无归属 | 双足 | 海、陆 | 鱼类 | `LatexEel` |
| `changed:form_latex_fennec_fox` | 胶液狐狸 | 无归属 | 双足 | 陆 | 犬系 | `LatexFennecFox` |
| `changed:form_latex_gnoll_taur` | 胶液四足鬣狗人 | 无归属 | 半人马形 | 陆 | 其他哺乳类 | `LatexGnollTaur` |
| `changed:form_latex_golden_dragon` | 胶液金龙 | 无归属 | 双足 | 陆、空 | 龙系 | `LatexGoldenDragon` |
| `changed:form_latex_human` | 胶液人 | 无归属 | 双足 | 陆 | 人形 | `LatexHuman` |
| `changed:form_latex_hypno_cat` | 胶液催眠猫 | 无归属 | 双足 | 陆 | 猫系 | `LatexHypnoCat` |
| `changed:form_latex_keon_wolf` | 胶液银狐 | 无归属 | 双足 | 陆 | 犬系 | `LatexKeonWolf` |
| `changed:form_latex_kobold` | 胶液 Kobold | 无归属 | 双足 | 陆 | 无归属 | `LatexKobold` |
| `changed:form_latex_leaf` | 胶液植物龙 | 无归属 | 双足 | 陆 | 植物 | `LatexLeaf` |
| `changed:form_latex_manta_ray/female` | 雌性胶液蝠鲼 | 无归属 | 鱼尾 | 海 | 鳐系 | `LatexMantaRayFemale` |
| `changed:form_latex_manta_ray/male` | 雄性胶液蝠鲼 | 无归属 | 双足 | 海、陆 | 鳐系 | `LatexMantaRayMale` |
| `changed:form_latex_medusa_cat` | 胶液美杜莎猫 | 无归属 | 双足 | 陆 | 猫系 | `LatexMedusaCat` |
| `changed:form_latex_mermaid_shark/female` | 胶液塞壬 | 无归属 | 鱼尾 | 海 | 无归属 | `LatexSiren` |
| `changed:form_latex_mermaid_shark/male` | 胶液人鱼鲨 | 无归属 | 鱼尾 | 海 | 鲨系 | `LatexMermaidShark` |
| `changed:form_latex_mimic_plant` | 胶液拟态植物 | 无归属 | 双足 | 陆 | 植物 | `LatexMimicPlant` |
| `changed:form_latex_ming_cat` | 胶液冥猫 | 无归属 | 双足 | 陆 | 猫系 | `LatexMingCat` |
| `changed:form_latex_moth` | 胶液飞蛾 | 无归属 | 双足 | 陆 | 节肢动物、昆虫 | `LatexMoth` |
| `changed:form_latex_mutant_bloodcell_wolf` | 变异细胞胶狼 | 白胶 | 双足 | 陆 | 犬系 | `LatexMutantBloodcellWolf` |
| `changed:form_latex_orca` | 胶液虎鲸 | 无归属 | 双足 | 海、陆 | 水生哺乳类 | `LatexOrca` |
| `changed:form_latex_otter` | 胶液水獭 | 无归属 | 双足 | 海、陆 | 其他哺乳类 | `LatexOtter` |
| `changed:form_latex_pink_deer` | 胶液粉鹿 | 无归属 | 双足 | 陆 | 其他哺乳类 | `LatexPinkDeer` |
| `changed:form_latex_pink_wyvern` | 胶液枫岚 | 无归属 | 双足 | 陆 | 龙系 | `LatexPinkWyvern` |
| `changed:form_latex_pink_yuin_dragon` | 胶液 Yuin 复合体 | 无归属 | 双足 | 陆、空 | 龙系 | `LatexPinkYuinDragon` |
| `changed:form_latex_purple_fox` | 胶液紫狐 | 无归属 | 双足 | 陆 | 犬系 | `LatexPurpleFox` |
| `changed:form_latex_rabbit/female` | 雌性胶液兔子 | 无归属 | 双足 | 陆 | 其他哺乳类 | `LatexRabbitFemale` |
| `changed:form_latex_rabbit/male` | 雄性胶液兔子 | 无归属 | 双足 | 陆 | 其他哺乳类 | `LatexRabbitMale` |
| `changed:form_latex_raccoon` | 胶液浣熊 | 无归属 | 双足 | 陆 | 其他哺乳类 | `LatexRaccoon` |
| `changed:form_latex_red_dragon` | 胶液红龙 | 无归属 | 双足 | 陆、空 | 龙系 | `LatexRedDragon` |
| `changed:form_latex_red_panda` | 胶液小熊猫 | 无归属 | 双足 | 陆 | 其他哺乳类 | `LatexRedPanda` |
| `changed:form_latex_shark` | 胶液鲨鱼 | 无归属 | 双足 | 海、陆 | 鲨系 | `LatexShark` |
| `changed:form_latex_shark/female` | 雌性融合胶液鲨鱼 | 无归属 | 双足 | 海、陆 | 鲨系 | `BuffLatexSharkFemale` |
| `changed:form_latex_shark/male` | 雄性融合胶液鲨鱼 | 无归属 | 双足 | 海、陆 | 鲨系 | `BuffLatexSharkMale` |
| `changed:form_latex_shark_feral` | 野生胶液鲨鱼 | 无归属 | 四足兽形 | 海 | 鲨系 | `FeralShark` |
| `changed:form_latex_siamese_cat` | 胶液暹罗猫 | 无归属 | 双足 | 陆 | 猫系 | `LatexSiameseCat` |
| `changed:form_latex_snake` | 胶液蛇 | 无归属 | 蛇身 | 陆 | 爬行类 | `LatexSnake` |
| `changed:form_latex_snow_leopard/female` | 雌性胶液雪豹 | 无归属 | 双足 | 陆 | 猫系 | `LatexSnowLeopardFemale` |
| `changed:form_latex_snow_leopard/male` | 雄性胶液雪豹 | 无归属 | 双足 | 陆 | 猫系 | `LatexSnowLeopardMale` |
| `changed:form_latex_squid_dog/female` | 雌性胶液乌贼犬 | 无归属 | 双足 | 海、陆 | 犬系、头足类 | `LatexSquidDogFemale` |
| `changed:form_latex_squid_dog/male` | 雄性胶液乌贼犬 | 无归属 | 双足 | 海、陆 | 犬系、头足类 | `LatexSquidDogMale` |
| `changed:form_latex_squirrel` | 胶液松鼠 | 无归属 | 双足 | 陆 | 其他哺乳类 | `LatexSquirrel` |
| `changed:form_latex_stiger` | 胶液蜘蛛虎 | 无归属 | 双足 | 陆 | 猫系、节肢动物、蛛形 | `LatexStiger` |
| `changed:form_latex_tiger_shark` | 胶液虎鲨 | 无归属 | 双足 | 海、陆 | 鲨系 | `LatexTigerShark` |
| `changed:form_latex_traffic_cone_dragon` | 胶液交通锥标龙 | 无归属 | 双足 | 陆 | 龙系 | `LatexTrafficConeDragon` |
| `changed:form_latex_translucent_lizard` | 胶液半透明蜥蜴 | 无归属 | 双足 | 陆 | 爬行类 | `LatexTranslucentLizard` |
| `changed:form_latex_watermelon_cat` | 胶液西瓜猫 | 无归属 | 双足 | 陆 | 猫系 | `LatexWatermelonCat` |
| `changed:form_latex_white_tiger` | 胶液白虎 | 无归属 | 双足 | 陆 | 猫系 | `LatexWhiteTiger` |
| `changed:form_latex_yuin` | 胶液 Yuin | 无归属 | 双足 | 陆 | 无归属 | `LatexYuin` |
| `changed:form_phage_latex_wolf/female` | 雌性噬菌体胶狼 | 黑胶 | 双足 | 陆 | 犬系 | `PhageLatexWolfFemale` |
| `changed:form_phage_latex_wolf/male` | 雄性噬菌体胶狼 | 黑胶 | 双足 | 陆 | 犬系 | `PhageLatexWolfMale` |
| `changed:form_pooltoy_wolf` | 游泳池玩具狼 | 无归属 | 双足 | 陆 | 犬系 | `PooltoyWolf` |
| `changed:form_pure_white_latex_cerberus` | 纯白胶液三头犬 | 白胶 | 双足 | 陆 | 犬系 | `PureWhiteLatexCerberus` |
| `changed:form_pure_white_latex_wolf` | 纯白胶狼 | 白胶 | 双足 | 陆 | 犬系 | `PureWhiteLatexWolf` |
| `changed:form_pure_white_latex_wolf_pup` | 纯白胶狼幼崽 | 白胶 | 四足兽形 | 陆 | 犬系 | `PureWhiteLatexWolfPup` |
| `changed:form_sniper_dog` | 胶液犬 | 无归属 | 双足 | 陆 | 犬系 | `SniperDog` |
| `changed:form_white_latex_centaur` | 白胶半人兽 | 无归属 | 半人马形 | 陆 | 无归属 | `WhiteLatexCentaur` |
| `changed:form_white_latex_knight` | 白胶骑士 | 无归属 | 双足 | 陆 | 无归属 | `WhiteLatexKnight` |
| `changed:form_white_latex_knight_fusion` | 合体白胶骑士 | 无归属 | 双足 | 陆 | 无归属 | `WhiteLatexKnightFusion` |
| `changed:form_white_latex_wolf/female` | 雌性白胶狼 | 无归属 | 双足 | 陆 | 犬系 | `WhiteLatexWolfFemale` |
| `changed:form_white_latex_wolf/male` | 雄性白胶狼 | 无归属 | 双足 | 陆 | 犬系 | `WhiteLatexWolfMale` |
| `changed:form_white_wolf/female` | 雌性白狼 | 无归属 | 双足 | 陆 | 犬系 | `WhiteWolfFemale` |
| `changed:form_white_wolf/male` | 雄性白狼 | 无归属 | 双足 | 陆 | 犬系 | `WhiteWolfMale` |
| `changed:special/form_8922cbbc-0c40-4262-b0b8-a153ec94ecdc` | 本地特殊 Form 8922cbbc-0c40-4262-b0b8-a153ec94ecdc | 无归属 | 双足 | 海、陆、空 | 无归属 | `SpecialLatex` |
| `changed_addon:form_avali` | Avali | 无归属 | 双足 | 陆、空 | 无归属 | `AvaliEntity` |
| `changed_addon:form_avali_zergodmaster` | Avali Zergodmaster | 无归属 | 双足 | 陆、空 | 无归属 | `AvaliZerGodMasterEntity` |
| `changed_addon:form_bagel` | Bagel | 无归属 | 双足 | 陆 | 无归属 | `BagelEntity` |
| `changed_addon:form_biosynth_snow_leopard/female` | 雌性生物合成雪豹 | 无归属 | 双足 | 陆 | 猫系 | `SnowLeopardFemaleOrganicEntity` |
| `changed_addon:form_biosynth_snow_leopard/male` | 雄性生物合成雪豹 | 无归属 | 双足 | 陆 | 猫系 | `SnowLeopardMaleOrganicEntity` |
| `changed_addon:form_blue_lizard` | 蓝色蜥蜴 | 无归属 | 双足 | 陆 | 爬行类 | `BlueLizard` |
| `changed_addon:form_borealis/female` | Borealis Female | 无归属 | 双足 | 陆 | 无归属 | `BorealisFemaleEntity` |
| `changed_addon:form_borealis/male` | Borealis Male | 无归属 | 双足 | 陆 | 无归属 | `BorealisMaleEntity` |
| `changed_addon:form_buff_dazed_latex` | Buff Latex Dazed | 无归属 | 双足 | 陆 | 无归属 | `BuffDazedLatexEntity` |
| `changed_addon:form_buny` | 兔子 | 无归属 | 双足 | 陆 | 其他哺乳类 | `BunyEntity` |
| `changed_addon:form_dark_latex_yufeng_queen` | Dark Latex Yufeng Queen | 黑胶 | 双足 | 陆、空 | 无归属 | `DarkLatexYufengQueenEntity` |
| `changed_addon:form_dazed_latex` | Dazed胶兽 | 无归属 | 双足 | 陆 | 无归属 | `DazedLatexEntity` |
| `changed_addon:form_exp1/female` | 雌性实验体1号 | 无归属 | 双足 | 陆 | 无归属 | `Exp1FemaleEntity` |
| `changed_addon:form_exp1/male` | 雄性实验体1号 | 无归属 | 双足 | 陆 | 无归属 | `Exp1MaleEntity` |
| `changed_addon:form_exp2/female` | 雌性实验体2号 | 无归属 | 双足 | 陆 | 猫系 | `Exp2FemaleEntity` |
| `changed_addon:form_exp2/male` | 雄性实验体2号 | 无归属 | 双足 | 陆 | 猫系 | `Exp2MaleEntity` |
| `changed_addon:form_exp6` | 实验体6号 | 无归属 | 双足 | 陆 | 无归属 | `Exp6Entity` |
| `changed_addon:form_experiment009` | 实验体 009 | 无归属 | 双足 | 陆 | 无归属 | `Experiment009Entity` |
| `changed_addon:form_experiment009_boss` | 实验体 009 | 无归属 | 双足 | 陆 | 无归属 | `Experiment009BossEntity` |
| `changed_addon:form_experiment_10` | 实验体10号 | 无归属 | 双足 | 陆 | 无归属 | `Experiment10Entity` |
| `changed_addon:form_experiment_10_boss` | 实验体10号 | 无归属 | 双足 | 陆 | 无归属 | `Experiment10BossEntity` |
| `changed_addon:form_fengqi_wolf` | Feng QI狼 | 无归属 | 双足 | 陆 | 犬系 | `FengQIWolfEntity` |
| `changed_addon:form_foxta_foxy` | Foxta狐狸 | 无归属 | 双足 | 陆 | 犬系 | `FoxtaFoxyEntity` |
| `changed_addon:form_foxyas` | Foxyas the Latex Snow Fox | 无归属 | 双足 | 陆 | 犬系 | `LatexSnowFoxFoxyasEntity` |
| `changed_addon:form_hayden_fennec_fox` | Hayden Fennec狐狸 | 无归属 | 双足 | 陆 | 犬系 | `HaydenFennecFoxEntity` |
| `changed_addon:form_himalayan_crystal_gas_cat/female` | 雌性水晶气体猫 | 无归属 | 双足 | 陆 | 猫系 | `CrystalGasCatFemaleEntity` |
| `changed_addon:form_himalayan_crystal_gas_cat/male` | 雄性水晶气体猫 | 无归属 | 双足 | 陆 | 猫系 | `CrystalGasCatMaleEntity` |
| `changed_addon:form_latex_border_collie` | Latex Border Collie | 无归属 | 双足 | 陆 | 犬系 | `LatexBorderCollieEntity` |
| `changed_addon:form_latex_calico_cat` | 花斑猫胶兽 | 无归属 | 双足 | 陆 | 猫系 | `LatexCalicoCatEntity` |
| `changed_addon:form_latex_cheetah/female` | Female Latex Cheetah | 无归属 | 双足 | 陆 | 猫系 | `LatexCheetahFemale` |
| `changed_addon:form_latex_cheetah/male` | Male Latex Cheetah | 无归属 | 双足 | 陆 | 猫系 | `LatexCheetahMale` |
| `changed_addon:form_latex_dragon_snow_leopard_shark` | 龙雪豹鲨鱼胶兽 | 无归属 | 双足 | 海、陆、空 | 猫系、龙系、鲨系 | `LatexDragonSnowLeopardSharkEntity` |
| `changed_addon:form_latex_kayla_shark` | Latex Kayla Shark | 无归属 | 双足 | 海、陆 | 鲨系 | `LatexKaylaSharkEntity` |
| `changed_addon:form_latex_kitsune/female` | 雌性狐狸胶兽 | 无归属 | 双足 | 陆 | 犬系 | `LatexKitsuneFemaleEntity` |
| `changed_addon:form_latex_kitsune/male` | 雄性狐狸胶兽 | 无归属 | 双足 | 陆 | 犬系 | `LatexKitsuneMaleEntity` |
| `changed_addon:form_latex_mongoose` | Mongoose | 无归属 | 双足 | 陆 | 其他哺乳类 | `MongooseEntity` |
| `changed_addon:form_latex_snep` | Snep胶兽 | 无归属 | 四足兽形 | 陆 | 无归属 | `LatexSnepEntity` |
| `changed_addon:form_latex_snep_feral` | Snep胶兽 | 无归属 | 四足兽形 | 陆 | 无归属 | `LatexSnepEntity` |
| `changed_addon:form_latex_snow_fox/female` | 雌性雪狐胶兽 | 无归属 | 双足 | 陆 | 犬系 | `LatexSnowFoxFemaleEntity` |
| `changed_addon:form_latex_snow_fox/male` | 雄性雪狐胶兽 | 无归属 | 双足 | 陆 | 犬系 | `LatexSnowFoxMaleEntity` |
| `changed_addon:form_latex_snow_leopard_partial` | 不完全体雪豹 | 无归属 | 双足 | 陆 | 猫系 | `SnowLeopardPartialEntity` |
| `changed_addon:form_latex_squid_tiger_shark` | 虎鲨胶兽 | 无归属 | 双足 | 海、陆 | 鲨系、头足类 | `LatexSquidTigerSharkEntity` |
| `changed_addon:form_latex_white_snow_leopard/female` | Female Latex White Snow Leopard | 无归属 | 双足 | 陆 | 猫系 | `LatexWhiteSnowLeopardFemale` |
| `changed_addon:form_latex_white_snow_leopard/male` | Male Latex White Snow Leopard | 无归属 | 双足 | 陆 | 猫系 | `LatexWhiteSnowLeopardMale` |
| `changed_addon:form_latex_wind_cat/female` | Female Latex Wind Cat | 无归属 | 双足 | 陆 | 猫系 | `LatexWindCatFemaleEntity` |
| `changed_addon:form_latex_wind_cat/male` | Male Latex Wind Cat | 无归属 | 双足 | 陆 | 猫系 | `LatexWindCatMaleEntity` |
| `changed_addon:form_luminara_flower_beast` | Luminara Flower Beast | 无归属 | 双足 | 陆、空 | 植物 | `LuminaraFlowerBeastEntity` |
| `changed_addon:form_luminarctic_leopard/female` | 雌性发光豹 | 无归属 | 双足 | 陆 | 猫系 | `LuminarcticLeopardFemaleEntity` |
| `changed_addon:form_luminarctic_leopard/male` | 发光豹 | 无归属 | 双足 | 陆 | 猫系 | `LuminarcticLeopardMaleEntity` |
| `changed_addon:form_lynx` | Lynx | 无归属 | 双足 | 陆 | 猫系 | `LynxEntity` |
| `changed_addon:form_mirror_white_tiger_female` | 镜面白虎 | 无归属 | 双足 | 陆 | 猫系 | `MirrorWhiteTigerEntity` |
| `changed_addon:form_pink_cyan_skunk` | Pink Cyan Skunk | 无归属 | 双足 | 陆 | 其他哺乳类 | `PinkCyanSkunkEntity` |
| `changed_addon:form_protogen` | Protogen | 无归属 | 双足 | 陆 | 无归属 | `ProtogenEntity` |
| `changed_addon:form_protogen_0senia0` | Protogen 0senia0 | 无归属 | 双足 | 陆 | 无归属 | `Protogen0senia0Entity` |
| `changed_addon:form_prototype` | 原型机 | 无归属 | 双足 | 陆 | 无归属 | `PrototypeEntity` |
| `changed_addon:form_puro_kind/female` | 雌性普罗种类胶兽 | 黑胶 | 双足 | 陆 | 无归属 | `PuroKindFemaleEntity` |
| `changed_addon:form_puro_kind/male` | 雄性普罗种类胶兽 | 黑胶 | 双足 | 陆 | 无归属 | `PuroKindMaleEntity` |
| `changed_addon:form_reyn` | Reyn | 无归属 | 双足 | 陆 | 无归属 | `ReynEntity` |
| `changed_addon:form_snepsi_leopard` | Snepsi 豹 | 无归属 | 双足 | 陆 | 猫系 | `SnepsiLeopardEntity` |
| `changed_addon:form_void_fox` | 虚空狐 | 无归属 | 双足 | 陆 | 犬系 | `VoidFoxEntity` |
| `changed_addon:form_white_fox` | White Fox | 无归属 | 双足 | 陆 | 犬系 | `WhiteFoxEntity` |
| `changed_addon:form_wolfy` | Wolfy | 黑胶 | 双足 | 陆 | 无归属 | `WolfyEntity` |

## 动态 Form 与别名

`changed:form_custom_latex` 的皮肤、腿型和能力可变，不用一个固定实体 ID 给所有自定义实例套同一归属。所有未知动态 profile 默认各维度无归属；以后只有显式可核对的 profile 数据才补归属。

`changed:form_special` 是特殊 Form 入口别名，先解析具体 profile 再分类。未知 `changed:special/form_<UUID>` 不从 UUID、材质或显示名称猜族群。本地已存在的 UUID 在上表按实际配置记录，其 canGlide=true、breatheMode=ANY，缺少 latexType，因此胶体和族群为空。

## 分支接入约束

顶层分类入口的 `parents` 为 `["changede:common_strength_2"]`；`changede:arthropod_core` 属于顶层，`changede:insect_core` 和 `changede:arachnid_core` 的父节点均为 `changede:arthropod_core`，不得绕过它直连通用入口。现有通用节点 ID、坐标、培养记录保持稳定；分类后代再各自安排技能与补偿节点。某个 Form 没有任何可靠分类时仍保留通用成长。

命中昆虫或蛛形时显示节肢动物入口及对应后代；学习节肢动物关键节点后才能学习细分节点。蜘蛛虎同时显示猫系，以及节肢动物→蛛形路线，不获得昆虫路线。多个节肢子类命中时共享同一个父关键节点，节肢杀手不重复计算。

分类清单与分支是否可学习是两件事：只有分类命中才显示入口，已学习且当前适用才生效。不匹配的分支隐藏但保留培养记录；多运动或混合族群同时显示多个入口。黑白胶入口与 Yufeng 已迁移到新分类结构，当前技能见 SKILL_CONTENT.md，游戏行为以 SKILL_TREES.md 为准。
