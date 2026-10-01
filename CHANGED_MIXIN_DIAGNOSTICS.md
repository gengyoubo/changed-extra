# Changed Mixin 崩溃诊断补充

Forge 的 `MOD changed` 仍表示正在加载或受影响的模组。CE 不更改异常对象、原因链、堆栈、原始 Failure message、错误处理决定或报告路径，只追加 `Changed Mixin Diagnostics` 分类及加载错误界面的补充段落。原有打开日志、崩溃报告和模组目录按钮保留。

诊断器通过 CE 的 Mixin 插件尽早注册 `IMixinErrorHandler`，不依赖 CE 的 `@Mod` 构造或事件注册完成。回调记录 Mixin 提供的实际转换目标，并原样返回 ERROR/WARN/NONE。异常分析使用 `InvalidMixinException.getMixin()`、`InvalidInjectionException.getContext()` 与 `IMixinInfo.getConfig()`，不解析异常消息来猜 Mixin 名称或所属模组；根因消息直接引用原始异常。

只增强 Forge 标记为 `changed` 且有 Changed 类转换证据的 Mixin 错误。明确区分：

- Affected Mod：Changed (changed)。
- Failure occurred while transforming：实际目标类；无法确认时列出可能目标并注明未知。
- Failing Mixin / Mixin Config：结构化 Mixin 信息。
- Likely Source Mod：从早期 `LoadingModList` 元数据核对同一模组文件中的配置与 Mixin class。
- Root Cause：最深原因的类型与完整原始消息。

如果配置和 class 无法定位到唯一模组（例如共享多模组容器），明确显示来源未知，仍保留其余诊断。Changed 自己的 Mixin 失败会明确标为 Changed，不会把所有错误推给第三方。该逻辑也能识别针对 Changed 的 Addon、Synergy 等补丁，前提是归属证据完整。

报告和界面补丁放在独立、可选的 `changede-diagnostics.mixins.json` 中，注入最低要求为 0；接口变动时允许跳过增强。诊断分析、元数据读取及界面补充失败均回退到原始行为。缓存最多 128 项，异常遍历最多 256 项，使用身份集合防止原因或 suppressed 循环。错误界面中英文提示不依赖 CE 资源包成功加载，详情行高度与实际追加文本一同计算；完整报告保留原始根因消息，界面显示有限长度摘要。

验证命令：`gradle verifyChangedMixinDiagnostics --offline`。测试包括混合嵌套异常、多个目标中的实际目标、第三方和 Changed 自身归属、未知/读取失败来源、无关异常、suppressed、循环原因链，以及异常和错误处理决定保持不变。发布前还需核对正式端 Forge 方法名并用隔离加载失败验证报告和界面。
