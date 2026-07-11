# Repository Guidelines

## 项目结构与模块组织
本仓库是单模块 Android 工程，核心模块为 `:app`。主要源码位于 `app/src/main/java/me/padi/qqlite/revived/`，按职责划分为：

- `compose/`：注入宿主的 Compose 首页与聊天页 UI
- `hooks/common`、`hooks/home`、`hooks/aio`：libxposed Hook 与宿主桥接逻辑
- `shared/`：跨页面共享的 Model 与 ViewModel
- `utils/`：反射、Context、宿主 Compose 注入等基础工具
- `legacy/view/`：模块侧传统 Activity 页面

资源与 Xposed 元数据位于 `app/src/main/res` 和 `app/src/main/resources/META-INF/xposed`。逆向分析记录和宿主 APK 相关文件位于 `apk/`。更完整的设计说明见 `PROJECT_ARCHITECTURE.md`。

## 宿主 APK 与依赖定位
当前需要分析的宿主 APK 固定路径为 `D:/Application/Data/Project/QQ_Revived/apk/QQ手表版_9.0.5.apk`。涉及宿主适配、反射字段、页面结构或行为比对时，默认以这个 APK 为基准。

Miuix 依赖当前缓存于 `D:/.gradle/caches/modules-2/files-2.1/top.yukonga.miuix.kmp/`。本项目实际使用的 Android 侧构件可在该目录下找到，例如：

- `miuix-ui-android/0.9.3/`
- `miuix-preference-android/0.9.3/`
- `miuix-icons-android/0.9.3/`
- `miuix-squircle-android/0.9.3/`
- `miuix-navigation3-ui-android/0.9.3/`

需要查看源码或产物时，优先查找对应目录中的 `*-sources.jar`、`.aar` 和 `.module` 文件。

## 构建、测试与开发命令
在仓库根目录使用 Gradle Wrapper：

- `.\gradlew.bat :app:assembleDebug`：构建调试 APK
- `.\gradlew.bat :app:compileDebugKotlin`：快速校验 Kotlin 与 Compose 编译
- `.\gradlew.bat :app:lintDebug`：运行 Android Lint，当前配置为 `abortOnError=true`
- `.\gradlew.bat :app:installDebug`：将最新调试构建安装到已连接设备

构建产物输出到 `app/build/outputs/apk/`。

默认交付流程要求：当需求已经实现完毕且代码修改完成后，除非用户明确说明跳过安装，否则应执行 `.\gradlew.bat :app:installDebug`，把最新 `debug` 构建安装到设备上进行最终验证。

## 代码风格与命名约定
遵循现有 Kotlin 风格：4 空格缩进，不使用 Tab，方法保持简洁，按功能进行包划分。类名、Composable 使用 `UpperCamelCase`，方法和属性使用 `lowerCamelCase`，常量使用 `SCREAMING_SNAKE_CASE`，例如 Hook 标记或目标包名常量。

Hook 层只负责宿主适配、反射定位和事件桥接，不承担 UI 状态真相。UI 状态应归属 `shared/` 中的 ViewModel，`compose/` 和 `shared/` 不应反向依赖具体 Hook 实现。代码注释语言需与仓库现有风格保持一致，并尽量保持精简。

## 测试指南
当前仓库没有独立的 `test/` 或 `androidTest/` 目录。在补齐自动化测试前，`compileDebugKotlin` 与 `lintDebug` 是每次改动的最低校验要求。涉及 Hook、宿主注入或反射目标的改动时，还应在 `com.tencent.qqlite` 上进行真机验证，重点检查生命周期、返回键、输入法行为和反射命中情况。

## 提交与合并请求规范
现有提交历史以简短、聚焦任务的中文主题为主，例如 `修复"共享头衔"的问题`、`重构 AIO 长按菜单接管逻辑...`。提交应保持单一职责，标题使用明确、可执行的描述，避免把多个无关改动混在同一次提交中。

提交 PR 时应说明影响的宿主页面或能力、涉及的 Hook/UI 路径、已执行的验证步骤；如果 Compose 界面有可见变化，应附带截图。如果调整了反射名称、类路径或宿主版本假设，需要在描述中显式标明。
