# Android Compose 通用空架构

可直接在 Android Studio 打开的原生 Android 多模块起点。保留工程名 BgdDemo 和包名 com.bgd.myapplication，没有账号、设备、录音等业务。首页和主题设置用于演示导航与数据流，可以独立替换。

## 环境

| 项目 | 配置 |
| --- | --- |
| Gradle / AGP | 9.5.0 / 9.3.2（沿用初始工程） |
| Gradle JDK | 21（Android Studio 内置 JBR 可用） |
| Java / Kotlin 字节码 | 17 |
| Kotlin Compose / Serialization 插件 | 2.3.21 |
| SDK | compile 37 / target 36 / min 24 |
| UI | Compose BOM 2026.08.00 / Material 3 |
| 导航 | Navigation 3，1.1.7 |
| DI | Hilt 2.60.1 / AndroidX Hilt 1.4.0 / KSP 2.3.9 |
| 数据 | Retrofit 3 / OkHttp 5 / Serialization / Room 2.8.4 / DataStore 1.2.1 |

所有依赖版本在 gradle/libs.versions.toml。AGP 9 使用内置 Kotlin，不应用 kotlin-android 插件。升级时一起验证 AGP、Kotlin 编译器插件和 KSP，不能只看单个库的最新版本。

## 模块职责

```text
app                     Application、Activity、导航和应用级主题
core/model              共享模型（不依赖 UI、数据库或网络）
core/data               Repository 接口、实现与 Hilt 绑定
core/datastore          设置持久化
core/network            Json、OkHttp、RetrofitFactory
core/database           Room、DAO、schema 导出
core/designsystem       Material 3 主题、Coil 3 依赖入口
feature/home            无业务的首页
feature/settings        Route / Screen / UiState / Action / ViewModel
```

依赖方向：app → feature → core/data → core/datastore → core/model。
设计系统依赖 model，feature 彼此不依赖。network 和 database 目前是可注入基础设施；未来由 core/data 的具体 Repository 使用。app 聚合模块以使 Hilt 能发现绑定。

当前无共享 UI 组件或调度器工具，不建立空的 core/ui、core/common。复杂业务出现后再增加 domain/use case；不引入 BaseActivity、BaseViewModel、BaseRepository 或额外 MVI 框架。

## 已打通的数据流

设置页点击主题 → SettingsAction → SettingsViewModel → SettingsRepository → DataStore。
DataStore Flow → UiState → collectAsStateWithLifecycle → Screen。
应用级 ViewModel 订阅同一个 Repository 更新全局主题，支持跟随系统、浅色、深色和 Android 12+ 动态配色。

Screen 只接收状态和回调，可独立 Preview。ViewModel 只访问 Repository。IO 错误进入页面状态；协程取消和非 IO 编程错误不被吞掉。保存期间禁用重复操作，持久化成功后才通过数据流更新选中项。

Navigation 3 使用可序列化 NavKey 和 rememberNavBackStack；安装 SaveableStateHolder 与 ViewModelStore 装饰器，返回时不弹出根页面。首页无需业务状态，因此没有人为添加 ViewModel。

## 构建与验证

安装 SDK Platform 37；在 Android Studio 的 Gradle JDK 设置中选择 JDK 21。
本机系统 JAVA_HOME 仍为 Java 8，命令行构建需要在当前终端切换，不必修改系统环境：

```powershell
$env:JAVA_HOME = 'D:\Android\Android Studio\jbr'
# 本机 PATH 有多余引号时，仅在当前终端清理，避免 Java 测试进程参数解析失败。
$env:Path = $env:Path.Replace('"', '')
.\gradlew.bat :app:assembleDebug testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
# 连接设备或启动模拟器后
.\gradlew.bat :app:connectedDebugAndroidTest
# 发布前验证 R8
.\gradlew.bat :app:assembleRelease
```

Debug APK：app/build/outputs/apk/debug/app-debug.apk。
Release 默认开启 R8 和资源压缩，没有附带发布签名，不能直接作为商店发行包。
测试覆盖主题键兼容、设置读取与写入失败恢复；设备测试覆盖导航和 Activity 重建。

## 复制为新项目

1. 复制源码、Gradle wrapper、配置；排除 .gradle、.idea、各模块 build 和 local.properties。
2. 修改 settings.gradle.kts 的 rootProject.name，以及 app 的 applicationId、namespace、应用名称。
3. 在 Android Studio 用 Refactor 重命名 com.bgd.myapplication 包；同步修改每个模块 namespace、测试包和导入。
4. 重新生成 local.properties，设置 SDK 与 Gradle JDK，执行上述验证。
5. 替换首页，按下述步骤增加 feature；按需要保留设置页。

## 新增功能

复制 feature/settings 的结构与 build.gradle.kts，修改 namespace、包名、状态与资源，并在 settings.gradle.kts 中 include。
app 添加 project 依赖，然后在 BgdApp 的 entryProvider 注册新的 @Serializable NavKey。
跨 feature 跳转通过回调交给 app，不直接依赖另一个 feature。

业务数据由 core/data 的 Repository 暴露 Flow 和 suspend 方法。只有在组合多个 Repository 或复用复杂规则时才增加 UseCase。
DTO 留在 network，Entity 留在 database，映射由 Repository 协调；简单数据不强制复制三份模型。

## 接入后端与数据库

- 在 core/data 添加 core/network 依赖。在应用或数据层 Hilt Module 中用 RetrofitFactory.create(真实 HttpUrl) 创建 Retrofit 和业务 API。endpoint 必须 HTTPS 并以 / 结尾；模板不访问占位地址。
- 网络默认不记录请求正文或认证信息；如需 debug 日志，按业务配置脱敏。异常映射在具体 Repository 中实现，始终保留 CancellationException。
- core/database 提供一个可选 CacheEntry / CacheDao，使 Room 和 KSP 能真正生成数据库实现；启动不插入数据。首发前可以替换成业务表。
- Room schema 输出到 core/database/schemas，应提交版本控制。发布后改表必须递增数据库版本、提供 Migration 并测试迁移；没有破坏性降级兜底。
- DataStore 仅保存非敏感主题设置。备份规则只允许该设置文件，数据库缓存不参与备份。认证信息需要另行设计存储与备份策略。

Coil 3 依赖放在 designsystem，添加图片组件时使用 AsyncImage。Paging、WorkManager、前台服务、Adaptive 布局、Macrobenchmark / Baseline Profile 待真实列表、后台任务、屏幕布局和性能路径确定后按需添加；当前模板未创建无意义任务或虚假性能基线。

## 本次验证（2026-09-09）

- Debug / Release（R8）构建通过；Release 为未签名 APK。
- 5 个 JVM 单元测试通过，0 失败。
- Compose v2 导航测试 APK 编译通过；未连接设备，未执行设备测试。
- Lint：0 errors、5 warnings，均为 targetSdk 与依赖升级提示；没有隐藏或建立 baseline 忽略这些提示。
- 本机无可用 AVD，尚未完成实际启动、进程死亡恢复和真机视觉验证。

## 官方参考

- [Android 架构建议](https://developer.android.com/topic/architecture/recommendations)
- [Navigation 3 状态与 ViewModel 生命周期](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [AGP 内置 Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
