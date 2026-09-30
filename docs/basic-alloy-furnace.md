# 基础合金炉配方

机器 ID：`changede:basic_alloy_furnace`。储存 50000 LP，有两个输入槽和一个输出槽。

配方放在 `data/<命名空间>/recipes/` 下，支持数据包与 `/reload`，JEI 会显示合金炉配方。

```json
{
  "type": "changede:alloy_furnace",
  "X": { "item": "changed_addon:iridium" },
  "Y": { "item": "changede:latex_ingot" },
  "time": 10,
  "lp_per_second": 200,
  "result": { "item": "changede:iridium_ingot", "count": 1 }
}
```

- `X`、`Y`、`time`、`result` 必填。每次各消耗一个输入物品，X/Y 可以交换槽位。
- `X`、`Y` 使用 Minecraft Ingredient 格式，可以用 `{"tag": "forge:ingots/iron"}`。
- `time` 单位为秒，必须大于零。小数时间向上取整到游戏刻（每秒 20 刻）。
- `lp_per_second` 是每秒 LP 消耗，可省略，默认 200；允许非负整数，0 表示免费加工。
- `result.count` 可省略，默认为 1，必须能装入单个输出槽。
- 每刻按配置速率扣除 LP，余数累积；最后不足一个 LP 的部分向上取整。例如 10 秒、200 LP/秒共消耗 2000 LP。
- 缺电或输出槽无法容纳产物时暂停，恢复后继续加工。更换输入种类或配方会重置进度。

内置配方：铱 + 胶锭 → 铱锭；硅硼钙铝石 + 铁锭 → 硅硼钙铝锭。均为 10 秒、200 LP/秒。
