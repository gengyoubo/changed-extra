## Changed Extra
Changed Extra（以下称为CE）分为三大部分，汉化、通用和扩展。  
汉化：使用了 Mixin 对 Changed 与 Changed Addon Plus 的部分 硬编码 内容做出 汉化 的同时，对某些物品汉化进行优化；  
通用：对 Changed 与 Changed Addon Plus 进行扩展，也是本模组的核心；  
扩展：扩展：在通用的基础上添加科技元素；  
## CE扩展项
CE:基本
CE:科技
CE:维度
CE:扩展
## 版本号
changed-extra-X.X.X-（分支）
第一个X为大版本更新次数
第二个X为小版本更新次数
第三个X为测试版本更新次数
偶尔会有分支
主分支版本：一般是添加新的游戏功能，不会注重修复bug
副分支版本：一般是上一个版本出现严重的bug时会开启，偶尔会更新游戏功能（一般情况不会和主分支发布同样的游戏功能）
（分支版本省略时，默认为主分支版本）
## Q&A
Q：为什么会要分主分支版本和副分支版本？
A：因为作者在制作模组的时候要突然修复上个版本的问题，而且还是在制作新版内容的时候。而且这个时候无法立即发布修复版本，只能在旧版本去修复问题。当然，会有版本同样但是内容不一样的时候。一般会在正式版的时候合并。
## 汉化召集处
如果对本模组的汉化不满意的可以投稿至 gengyoubo@gmail.com 或者在issue评论。
## 开发运行

`runClient`、`runClientFast`、`runClientCoverage`、`runClientJfr` 和 `runServer` 启动前会将编译后的类、资源和 Mixin refmap 合并复制到独立的 `.gradle/changede-runtime/<随机 ID>/main`。运行中的游戏使用这份快照，后续编译不会改写它，避免物品等延迟加载时出现 `ClassNotFoundException`。源码更新后重新启动开发客户端才能使用新代码；`verifyDevRunSnapshot` 可在不启动游戏的情况下校验快照。

IDEA 的 `runClient`／`runServer` 配置通过对应 Gradle 任务启动，而不是直接读取变化中的类目录。`configureStableIdeRuns` 可更新这两个配置；`genIntellijRuns` 重新生成配置后也会自动应用。IDE 直接运行 BootstrapLauncher 的旧配置不会使用快照保护。

## License
This project is licensed under the MIT License.
