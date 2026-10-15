# 逆转器：Mek 源码核对与实施方案

状态：首版已实现，原预留 JSON 已转为活动配方。使用方式、默认值和文件路径见 [气体科技说明](GAS_TECHNOLOGY.md)。下文保留源码依据与原方案，首版数值以气体科技说明为准。
目标：CE 的 WLP＋DLP 机器，通过 Mek 气体管道接收用尽的核废料，以 10:1 转换为裂变燃料。
本次核对本地 Mekanism 源码版本 `v1.20.1-10.4.16.80`，与当前项目的 Mek 版本一致。

## 源码结论

| 位置 | 核对结果及实现含义 |
| --- | --- |
| `MekanismGases` | 输入是 `mekanism:spent_nuclear_waste`，输出是 `mekanism:fissile_fuel`；都是 `GasStack`，不走 Forge 流体接口。前者带 `Radiation(0.01)` 属性，后者没有该属性 |
| `TileEntityIsotopicCentrifuge` | 已有气体输入、输出双罐、分面、自动输出和气体配方匹配，可参考其接入方式；它的能源是单一 Mek 电能，需要改为 CE 双能源结算 |
| `ChemicalAttributeValidator` / `GasAttributes.Radiation` | 默认属性校验在开启辐射时拒收放射性气体；只匹配气体 ID 不够。输入罐必须显式允许辐射属性，并另外按配方过滤气体 |
| `ChemicalTankBuilder.output` | 原生输出罐只允许 INTERNAL 插入，允许输出；能避免外部设备往产物罐塞气体改变加工结果 |
| `GasToGasRecipeSerializer` | 原生支持气体输入 ingredient、气体输出和网络同步；没有 CE 的时间、WLP、DLP 字段，需要自有序列化器 |
| `CachedRecipe` / `OneInputCachedRecipe` | 检查输入、输出空间和能源后推进，能源逐 tick 消耗，普通单输入配方在完成时扣物料、添加产物。能源接口只接一个 J 储能容器，不能直接表达 WLP＋DLP |
| `TileEntityMekanism.blockRemoved` / `BlockMekanism.getDrops` | 辐射开启时，拆除含放射性气体的机器会释放辐射，并清掉掉落物里的相应气体；需同时沿用方块与方块实体这两部分，不能只把废料写入掉落 NBT |
| `TileEntityRadioactiveWasteBarrel` | 废料桶提供上下气体接口，启用输出时向下排出；逆转器可用加压管道接入现有废料处理链。机器自身无需复制废料桶的衰变逻辑 |

## 推荐结构

独立注册 CE 的 `reverser` 方块，使用自己的 Mek `BlockTypeTile` 和继承 `TileEntityMekanism` 的方块实体。
复用 Mek 的气体储罐、能力、保存、辐射移除和气体输出设施；加工循环由 CE 管理，以便明确处理两种能源。
不添加 Mek 的电能容器，不把 WLP、DLP 当作 J，不接收 FE 或普通 LP。

首版两个气罐，容量可以先参考离心机的各 10,000 mB；这是建议值，不是已经确定的逆转器参数。
输入罐允许放射性属性，同时只接受现有逆转配方匹配的气体；输出罐仅由机器内部写入。
普通自动化只抽取产物。参考 Mek 的放射性输入罐抽取规则，在允许清空废料时提供可控的管道排空途径，避免玩家必须带废料拆机。

建议默认接口按机器朝向定义：

| 相对面 | 默认用途 |
| --- | --- |
| 左 | WLP 输入 |
| 右 | DLP 输入 |
| 后 | 气体输入 |
| 前 | 气体输出，支持主动排出 |
| 上、下 | 首版保留 |

气体侧使用 `Capabilities.GAS_HANDLER`；WLP/DLP 侧使用 CE 的能源端口接口。气体分面与双能源分面分别处理，不用 Mek 的单一 ENERGY 侧配置冒充两种能源。

## CE 需要先补的双能源端口

现有 `ILatexTypedEnergyHandler.getEnergyType()` 只能报告一种类型；`TypedEnergyPipeBlockEntity` 在连接、抽取和推送阶段都要求它与管道类型相等。
因此，仅给逆转器增加两个储能变量，仍然无法让两种管道同时工作。

建议新增可表达多种能源的端口接口，提供：

- 按 `能源类型＋机器侧面` 判断能否连接、输入、输出。
- 按类型查询储能与容量，并按类型和侧面接收、提取。
- 逆转器持有独立的 `TypedLatexEnergyStorage(WLP)` 与 `TypedLatexEnergyStorage(DLP)`，两者只入不出。

旧的单能源接口通过默认方法适配到新接口，现有管道、转换器继续按原类型工作。
管道对机器的判断改为查询端口支持能力，管道之间仍要求同类型连接；不得因此把 WLP 与 DLP 网络接通。
Jade 同步增加双能源机器分支，分别显示两条储能，不把两个余额相加成同一种能源。

## JSON 与 JEI

新增 `ReversingRecipe extends GasToGasRecipe`，使用自己的 `changede:reversing` 类型、序列化器和 CE 注册入口。
继承气体输入 ingredient 的匹配语义，可以继续支持指定气体、标签和候选输入；输出保留 `GasStack`。
首版只实现气体逆转。以后扩展物品或流体时，可新增对应配方类型，保留现有气体配方兼容性。

当前预留文件中的输入、输出已符合 Mek 气体配方的基本结构：

```json
{
  "input": { "gas": "mekanism:spent_nuclear_waste", "amount": 10 },
  "output": { "gas": "mekanism:fissile_fuel", "amount": 1 }
}
```

正式启用还需确定并添加以下必填字段，不提供免费的缺省耗电：

| 字段 | 单位与校验 |
| --- | --- |
| `time` | 秒，必须为正、有限，且能精确表示为整数个游戏 tick |
| `wlp_per_second` | WLP/秒，首条双能源配方要求正整数 |
| `dlp_per_second` | DLP/秒，首条双能源配方要求正整数 |

输入输出数量使用 Mek 的 long 体积，校验为正且单批量不超过机器储罐能力；未知气体、空标签、无效耗时或数值溢出应有明确诊断。
正式字段在 JSON、服务端对象和网络包中保持一致。原生 `GasToGasRecipe.write()` 只写输入输出，自定义配方必须追加时长及两种耗电，并在读取端对应恢复。
配方文件添加 `mekanism` 的 Forge 模组条件，启用后移入 `data/changede/recipes/reversing/`。

逆转器有自己的 JEI 分类，使用 `MekanismJEI.TYPE_GAS` 展示真实气体；页面显示 10:1、加工时间、WLP/秒、DLP/秒以及每批总耗能，机器作为该分类的催化剂。
界面采用两个气体槽、两条独立能源条和加工进度，明确显示缺 WLP、缺 DLP、缺输入、输出满、运行与红石暂停。

## 加工与中断

采用 Mek 普通单输入配方的完成时扣料模式，首版单批处理，不增加隐藏的放射性物料缓存。

1. 匹配输入种类和数量，确认当前配方与运行记录一致。
2. 模拟完整产物能否进入输出罐；输出满时暂停，不消耗任一种能源。
3. 分别计算本 tick 的 WLP、DLP 费用，先检查两个本机储能都足够，再同时扣费并推进一个 tick；缺任一种时都不扣费。
4. 费用各自携带除以 20 的余数，最后一个加工 tick 补齐应付尾数；单批总费用等于 `ceil(每秒费用×加工tick数/20)`。
5. 完成 tick 再确认完整输入和输出空间，内部扣除 10 mB、加入 1 mB，一次提交；不存在先产出后补扣材料的路径。

数量不足、缺电、输出满或红石禁用时暂停；输入种类或运行配方发生变化时重置进度。已有消耗不退款，不发放半成品。
保存配方 ID、影响加工结果与费用的配方签名、进度、两种能源余数、储能和两个气罐。区块重载后校验签名，再恢复同一工单；数据包重载改变配方内容时取消旧进度。
同一世界 tick 只推进一次，避免额外 tick 调用重复推进；换配方或重新加载不能省掉 WLP/DLP 尾数。
普通区块卸载不应释放辐射；真正拆除交给 Mek 的移除与掉落流程。拆除后不继承加工进度，放射性气体遵循 Mek 辐射规则，不能同时泄漏并保留在掉落物里。

## 核燃料链的实际比例

原版配方按以下路线运行，暂不计输送损耗和 CE 升级：

```text
10,000 mB 裂变燃料
→ 10,000 mB 核废料
→ 1,000 mB 钋或钚
→ 1 个对应颗粒 + 1,000 mB 用尽的核废料
→ 逆转器回收 100 mB 裂变燃料
```

所以“用尽的核废料 10:1”相当于回收初始燃料约 1%，同时保留钋/钚颗粒收益。不得误用 `mekanism:nuclear_waste` 作为首条配方输入，否则会省略中间核材料生产链，并变成直接回收约 10%。

CE 的 X 处理器减少耗料并增加废料，Y 处理器返还燃料，因此实际组合回收率可能高于原版基线。
理想无损且 Y 返还概率 `p < 1` 时，长期平均回收量与净耗料之比约为：

```text
r = 0.01 × (1 + X数量×废料增益) × (1 + X数量×燃料效率增益) / (1 - p)
```

达到或超过 1 时可能形成自给燃料循环；Y 达到 100% 返还时，本来就不再有净耗料。
这需要作为联动玩法测试与调参项，不在实现逆转器时偷偷修改现有 X/Y 升级。加工时长、双能源费用、机器合成配方目前尚未确定。

## 实施顺序与验收

先加向后兼容的双能源端口和管道适配，再注册气体配方与序列化器，接入储罐及单批加工，最后添加机器界面、JEI、Jade、合成、掉落和中英文提示。
仅安装 Mek 主模组时即可注册气体逆转器，不额外要求 Generators 或 AE2；相关类和配方数据与无 Mek 环境隔离。

验收至少覆盖：实际加压管道输送废料及产物；两种 CE 管道同时供能且不串网；10:1 与非法原始核废料输入；单边缺电与满输出均不扣费；整数尾数、断电续作、换料、配方热重载、区块重载；拆除辐射及掉落无重复；缺少可选模组；与 X/Y 组合下的物料守恒。

## 源码定位

- [气体定义与辐射属性](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/main/java/mekanism/common/registries/MekanismGases.java)
- [同位素离心机的双罐、分面与加工](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/main/java/mekanism/common/tile/machine/TileEntityIsotopicCentrifuge.java)
- [核废料处理及颗粒配方](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/datagen/main/java/mekanism/common/recipe/impl/MekanismRecipeProvider.java)
- [气体配方 JSON 与网络序列化](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/main/java/mekanism/common/recipe/serializer/GasToGasRecipeSerializer.java)
- [逐 tick 加工、能源与进度处理](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/api/java/mekanism/api/recipes/cache/CachedRecipe.java)
- [完成时扣料与输出](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/api/java/mekanism/api/recipes/cache/OneInputCachedRecipe.java)
- [气罐属性校验与输出规则](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/api/java/mekanism/api/chemical/ChemicalTankBuilder.java)
- [移除时释放辐射](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/main/java/mekanism/common/tile/base/TileEntityMekanism.java)
- [掉落物中的放射性气体处理](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/main/java/mekanism/common/block/BlockMekanism.java)
- [废料桶接口与衰变](https://github.com/mekanism/Mekanism/blob/v1.20.1-10.4.16.80/src/main/java/mekanism/common/tile/TileEntityRadioactiveWasteBarrel.java)

CE 对应文件：`LP/ILatexTypedEnergyHandler.java`、`LP/TypedLatexEnergyStorage.java`、`LP/BlockEntity/WireBlockEntity/E/TypedEnergyPipeBlockEntity.java`、`LP/BlockEntity/MachineBlockEntity/LatexEnergyConverterBlockEntity.java`、`LP/recipe/CELPRecipes.java`、`JEI/CEJeiPlugin.java`。
