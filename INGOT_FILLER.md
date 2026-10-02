# 锭填充器

需求来源：[issue #31 的锭填充器及后续评论](https://github.com/gengyoubo/changed-extra/issues/31#issuecomment-5949137637)。

机器 ID 为 `changede:ingot_filler`，储存 100,000 LP、10,000 mB 胶液；两槽分别输入锭和输出成品。胶液可用桶或现有基础流体管道输入，LP 由普通电能管道供给。一个槽罐只储存一种胶液。

机器配方为 `XXX / AYB / XXX`：X=铱锭，Y=硅硼钙铝锭，A=`changed:infuser`（Changed 汉化：胶液灌注机），B=`changed_addon:unifuser`（Addon 汉化：注液仪）。A/B 可以左右交换。

胶锭加 1,000 mB 白胶液或黑胶液，加工 10 秒得到一枚对应的白胶锭或黑胶锭。默认 200 LP/s；胶液为 100 mB/s。内部逐 tick 累计整数余数，完成时恰好消耗 2,000 LP 和 1,000 mB，无小数丢失。缺液、缺 LP、输出槽不匹配或已满时暂停，不额外耗资源；补充后继续。机器 NBT 保存进度、LP、胶液、输入／输出及两种余数。

GUI 显示储量、进度、LP/s 和 mB/s。**清空**按钮丢弃储存胶液并取消当前这次加工，输入锭与储存 LP 保留，已消耗资源不退还；按钮由服务端验证菜单及玩家距离。自动化只允许向输入槽插入、从输出槽提取、向胶液槽灌注。

## 数据包配方

```json
{
  "type": "changede:ingot_filling",
  "ingredient": {"item": "changede:latex_ingot"},
  "fluid": {"fluid": "changed:white_latex", "amount": 1000},
  "time": 10,
  "lp_per_second": 200,
  "result": {"item": "changede:white_latex_ingot", "count": 1}
}
```

`time` 为秒，可以包含精确到游戏 tick 的小数（0.05 秒的倍数）。加载时用十进制精确计算 `amount / time`，必须为正整数 mB/s；例如 1,000 mB / 3 s 无法整除，该配方报错并跳过，不影响其他有效配方。原因包含配方 ID、胶液量和时间，写入服务端日志，打开机器时也通过服务端同步到 GUI 的 `!` 提示。修正数据包并 `/reload` 后重新打开 GUI 更新提示；已删除的错误配方不留旧提示。

JEI 展示输入锭、胶液、输出、加工时间及 LP/s、mB/s，并把锭填充器作为工作站。

基础 WLP/DLP 管道配方为 `XXX / YYY / XXX`（X=白／黑胶锭，Y=基础电能管道），产出三段。对应转换器配方为 `ZZZ / XYX / ZZZ`（Z=铱锭，X=对应管道，Y=对应胶锭）。

## 验证

`tests/skill/ingot-filler-smoke.init.gradle` 在隔离世界运行 `IngotFillerIntegrationProbe`。测试涵盖真实配方加载跳过错误、GUI 原因、黑白加工资源守恒、逐秒速率、暂停恢复、NBT 保存和整数余数、服务器清空、菜单容量同步、网络配方及实际合成（包括 A/B 交换）。测试资源和探针仅在该脚本运行时编入，正式 JAR 不包含。
