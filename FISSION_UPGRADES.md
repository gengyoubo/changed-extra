# 裂变反应堆升级（Issue #39）

适用 Minecraft 1.20.1、Mekanism / Mekanism Generators 10.4.16.80、AE2 Forge 15.4.10。
升级方块替换反应堆外壳，参与原生多方块验证，保留原生界面、拆解、损坏与辐射行为。
只统计当前成型反应堆内的方块；外部堆放的方块不生效。

| 方块 | 默认效果与叠放 |
| --- | --- |
| 高级冷却方块 | N 个提供 N×1% 节水效率，耗水为原来的 `1 / (1 + N×0.01)`，产汽与散热保持不变 |
| 高级铀处理器 X | N 个使燃料消耗除以 `1 + N×0.01`，热量及核废料产量乘以 `1 + N×0.01` |
| 高级铀处理器 Y | N 个提供 `min(N×1%, 100%)` 返还概率；每次燃烧结算成功时返还当次实际消耗的裂变燃料，保留热量与核废料 |
| 紧急跳闸系统 | 在燃烧前检查水量，剩余冷却水不足预测消耗的 10 秒时停堆；补水后需重新启动 |
| 裂变反应堆 ME 交互接口 | 每 tick 从相连 ME 网络直接补充整个水箱的空余容量，无额外管道或输出总线速率上限 |
| 裂变反应堆温差发电模块 | 辅助稳温并回收电能；450 K 启动，每块最高抽热 100,000 J/t；满电保留 25% 独立散热，达到耐热极限后永久熔断 |

例如 10 个冷却方块耗水约为原来的 90.91%；10 个 X 处理器耗料约 90.91%、产热/废料为 110%；10 个 Y 处理器为 10% 返还概率。
冷却水是整数 mB，每次耗水向上取整，避免小流量凭空产汽。燃料与废料的小数余额沿用 Mekanism 的原生缓存字段。
新增热量仍需足够冷却，原生过热、废料溢出辐射、蒸汽满仓排放行为保留。

温差模块可安装多个，各自储能、输出，共用真实热量预算；先抽热，再让蒸汽系统处理剩余热量。
空手潜行右击或 Jade 查看运行状态。熔断后需保持反应堆成型、停堆且低于 450 K，手持裂变换热核心右击维修。
详细数值、热量结算与验证方式见 [温差模块说明](FISSION_THERMOELECTRIC_DESIGN.md)。

跳闸阈值使用 `当前水量 / 每 tick 预测耗水量 < 200 tick`。
预测取最近产汽量、当前温度可产生的蒸汽、设定燃烧速率产热需求的最大值，计入冷却和 X 处理器叠放。
因此首次冷启动也能识别缺水，既有高温也会触发保护。水冷保护不干预装有钠冷却剂的反应堆。
停堆后继续执行原生冷却；本系统不会消除余热。

ME 补水发生在跳闸检查与燃烧之前，需要有效频道、供电及 AE2 允许的机器访问。
抽取通过 AE2 `StorageHelper.poweredExtraction` 完成，按 AE2 标准消耗能量；只抽取普通水，不混入钠。
多个接口各自申请剩余容量，满水后不会继续抽取。节点及意外拒收的少量待入水量保存到方块 NBT。
接口断开、移除或区块卸载时释放节点与能力。

四个叠放参数默认均为 0.01，可在世界的 `serverconfig/changede-fission-server.toml` 调整：
`coolingEfficiencyPerBlock`、`fuelEfficiencyPerBlock`、`outputBonusPerBlock`、`fuelReturnChancePerBlock`。

新增合成、掉落表、镐采掘标签、模型与中英文提示。
合成使用反应堆外壳、CE 铱锭/胶锭及对应控制电路/流体储罐/比较器/ME 接口。
未安装 Generators 时不注册升级方块与数据包；未安装 AE2 时不注册 ME 接口，其余升级可用。

## 源码依据

JEI 使用 Mekanism 原有的裂变反应堆分类 `mekanismgenerators:fission`。
升级方块通过与原生 `GeneratorsJEI` 相同的 `CatalystRegistryHelper.register` 注册到左侧列表，
按 U 可查询用途；JEI 物品说明列出当前服务器的叠放参数、跳闸条件与 ME 供水要求。
原生图中的状态、1.0 消耗速度、300 K 温度等是固定的演示文字，不是实时机器数据。

- [Mekanism 10.4.16.80：裂变燃烧、冷却、损坏与辐射](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/src/generators/java/mekanism/generators/common/content/fission/FissionReactorMultiblockData.java)
- [原生 JEI 分类与催化剂注册](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/src/generators/java/mekanism/generators/client/jei/GeneratorsJEI.java)
- [原生 JEI 裂变反应堆界面](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/src/generators/java/mekanism/generators/client/jei/FissionReactorRecipeCategory.java)
- [原生结构验证器](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/src/generators/java/mekanism/generators/common/content/fission/FissionReactorValidator.java)
- [燃料与废料小数余额的存档缓存](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/src/generators/java/mekanism/generators/common/content/fission/FissionReactorCache.java)
- [AE2 15.4.10：带能耗的存储抽取](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/api/storage/StorageHelper.java)
- [AE2 流体单位与水键](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/api/stacks/AEFluidKey.java)
- [AE2 节点生命周期](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/blockentity/grid/AENetworkBlockEntity.java)

## 验证

实现阶段使用独立测试配置与项目已接受的 EULA，已有结果：

- 完整反应堆与 AE2 网络：78 项断言通过，见 `build/fission-server/fission-checks.txt`。
- 不安装 AE2：64 项断言通过，见 `build/fission-no-ae-server/fission-checks.txt`。
- 加入温差模块后，完整反应堆与 AE2 网络：162 项断言通过，见 `build/thermoelectric-server/fission-checks.txt`；热量模型独立检查 507 项通过。

这些检查覆盖原生反应堆成型、叠放数量、燃料/废料小数守恒、冷却产汽与耗水、10 秒跳闸边界、首次干启动、钠冷却、原生缓存往返、合成及掉落、实际 ME 网络与流体存储元件供水、节点清理与缺少 AE2 的隔离。

当前项目构建命令：

```powershell
.\gradlew.bat build --offline
```

JEI 接入沿用 Mekanism 的分类 UID 与注册助手，不替换原生配方或创建另一套裂变分类。
