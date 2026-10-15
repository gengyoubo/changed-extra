# 逆转器 JSON 配方

依据 [Issue #31 的逆转器提案](https://github.com/gengyoubo/changed-extra/issues/31#issuecomment-6182781314)，首条配方已经实现：10 mB 用尽的核废料 → 1 mB 裂变燃料，耗时 10 秒，每秒同时消耗 200 WLP＋200 DLP。

Mek 源码核对、双能源端口改造、加工结算及辐射处理方案见 [逆转器实施方案](REVERSER_DESIGN.md)。

活动文件：`src/main/resources/resourcepacks/gas_technology/data/changede/recipes/reversing/spent_nuclear_waste_to_fissile_fuel.json`。

| 字段 | 含义 |
| --- | --- |
| `type` | 配方序列化器 ID：`changede:reversing` |
| `input.gas` / `output.gas` | Mek 气体注册名；这里使用用尽的核废料和裂变燃料，均不是物品或 Forge 流体 |
| `input.amount` / `output.amount` | Mek 气体体积单位，必须为正整数；当前为 10:1 |
| `time` | 加工秒数，必须是 0.05 秒的整数倍 |
| `wlp_per_second` / `dlp_per_second` | 每秒分别消耗的 WLP 和 DLP，两个字段均为必填正整数 |

配方在安装 Mek 时自动加载，包含 JSON 校验与网络同步。原未启用样本已由此活动配方替代。

逐 tick 同时检查并消耗两种能源，完成时扣除 10 份输入并提交 1 份输出；缺电或满仓暂停。每批各消耗 2,000 WLP/DLP，可通过数据包覆盖数值。五种气体、合成、管道、机器使用和验证方式见 [气体科技说明](GAS_TECHNOLOGY.md)。
