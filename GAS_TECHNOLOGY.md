# 气体科技与逆转器（Issue #28 / #31）

适用 Minecraft 1.20.1、Mekanism 10.4.16.80。只要求 Mek 主模组，不额外要求 Generators 或 AE2。

## 气体生产与合成

新增五种 Mek 气体：`changede:white_latex_gas`、`changede:dark_latex_gas`、`changede:skunk_gas`、`changede:tiger_gas`、`changede:wolf_gas`。
白/黑胶气体分别对应 Changed 白/黑胶液，其余对应 Changed 已有的三种兽化气体流体；没有虚构对应药剂。
旋转冷凝机提供五条 1 mB 流体 ⇄ 1 mB 气体的双向配方，沿用机器能源消耗。
Changed 气罐本身没有通用流体接口，本次未改变其放气行为或添加吸取接口。

化学灌注器另有六条配方：10 mB 白胶气体或黑胶气体＋10 mB 任一种兽化气体→20 mB 对应兽化气体。总体积守恒，仍消耗灌注器能源。
所有配比、产量均在 JSON 中定义。

## 管道与逆转器

`changede:basic_gas_pipe` 使用 Mek 基础加压管网、容量、抽取速率、配置器控制、保存和辐射规则，可与原生管道、废料桶、化学储罐互通。不同气体分开布网。
合成：上下两排钢锭、中间一排玻璃，得到 8 根。

`changede:reverser` 默认每 10 秒将 10 mB 用尽的核废料逆转成 1 mB 裂变燃料，每秒同时消耗 200 WLP＋200 DLP，每批各消耗 2,000。
两个气罐各 10,000 mB，WLP、DLP 独立储能各 100,000。
面向正面：左侧白口输入 WLP，右侧黑口输入 DLP；后方蓝口进气，前方绿口出气并自动排出产物。
不接收 FE 或普通 LP。合成使用铱锭、形质晶核心、白/黑胶电转换器、钢制机壳和精英控制电路。

逐 tick 同时检查两种能源、输入和产物空间，再扣费推进；完成时一次扣料并产出。
缺任一种能源、输入不足、输出满或收到红石信号时暂停。耗电余数分别保存，完成时结清。
更换输入种类或配方内容时取消旧进度，已消耗能源不退款；断电、区块重载可继续相同工单，同一世界 tick 只推进一次。

右击打开界面，Jade 查看状态和双储能，JEI 查看气体配方、时长与总耗能。
“排空”模式暂停加工，将输入送回后方接收管网；没有接收空间时保留，不直接删除气体。
含废料拆除时遵循 Mek 的辐射释放和掉落气体清除规则；非放射性产物和存电可随掉落保存，进度不随掉落保存。

## 活动 JSON 数据

数据包 `src/main/resources/resourcepacks/gas_technology/` 在安装 Mek 时自动加载：

- `data/changede/recipes/gas/rotary/`：五条双向转换。
- `data/changede/recipes/gas/infusing/`：六条气体合成。
- `data/changede/recipes/reversing/spent_nuclear_waste_to_fissile_fuel.json`：逆转配方。
- 同包提供合成、掉落和采掘标签。

逆转配方类型为 `changede:reversing`，`input` 使用 Mek 气体 ingredient，`output` 为气体栈；支持输入标签和候选项。
`time` 是正秒数，必须能精确表示为整数 tick；`wlp_per_second`、`dlp_per_second` 必须为正整数，单 tick 费用不能超出储能容量。输入输出单批量须能被气罐容纳。
默认只接受 `spent_nuclear_waste`，不接受原始 `nuclear_waste`。高优先级数据包可覆盖相同配方 ID。无 Mek 时不注册此部分方块、气体、菜单或数据包。

## 验证

```powershell
.\gradlew.bat dualEnergyProcessChecks --offline
# 独立 build/gas-technology-server 世界，需先接受该目录的 Minecraft EULA。
.\gradlew.bat runServer -PverifyGasTechnology=true -PomitGeneratorsForChecks=true -PomitCreateForChecks=true -PomitTinkersForChecks=true -PomitTwilightForChecks=true --offline
# 无 Mek：独立 build/gas-optional-server 世界。
.\gradlew.bat runServer -PverifyGasTechnology=true -PomitMekanismForChecks=true -PomitCreateForChecks=true -PomitTinkersForChecks=true -PomitTwilightForChecks=true --offline
```

结果写入工作目录的 `gas-checks.txt`，检查失败会使验证用 `runServer` 任务失败。
双能源纯逻辑检查通过 243,480 个断言。客户端外观与 JEI 尚未进行人工游戏内验收。
源码依据和核燃料链的 1% 基线回收率推导见 [逆转器方案](REVERSER_DESIGN.md)。
