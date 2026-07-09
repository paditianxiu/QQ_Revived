# QQ Revived 项目结构与架构说明

本文档面向后续接手的 AI 或维护者。修改项目前请先阅读本文件，再读相关源码。

## 项目定位

QQ Revived 是一个基于 libxposed 的 Android Xposed 模块，目标宿主包名是 `com.tencent.qqlite`。模块包名是 `me.padi.qqlite.revived`，应用显示名称是 `QQ Revived`。

核心原则：
- Hook 层只做宿主适配、反射查找、事件桥接。
- UI 层只做渲染和用户事件转发。
- 所有 UI 状态由 ViewModel 管理。
- View 体系和 Compose 体系共享 `shared` 层状态与 ViewModel。
- Koin 作为 DI 容器，启动必须兼容 Xposed 注入场景，不能只依赖模块 Application。

## 入口与运行流程

Xposed 入口：
- `app/src/main/resources/META-INF/xposed/java_init.list`
- 当前值：`me.padi.qqlite.revived.ModuleMainKt`

模块入口：
- `app/src/main/java/me/padi/qqlite/revived/ModuleMainKt.kt`

入口流程：
1. `ModuleMainKt` 只处理目标宿主 `com.tencent.qqlite`。
2. 在 `onPackageLoaded` 和 `onPackageReady` 中调用 `RevivedKoin.ensureStarted()`，保证 Xposed 进程中 DI 已初始化。
3. Framework Hook 安装：`PhoneResourcesHook`、`StatusBarHook`、`OrientationHook`。
4. App Hook 安装：`VideoReportHiddenApiHook`、`HomeComposeHook`、`AioComposeHook`、`AutoSizeHook`、`WatchUiHook`。
5. Hot reload 时会 reset hook 状态和运行时缓存。

普通模块 App 入口：
- `App.kt`：模块自身 Application，调用 `RevivedKoin.ensureStarted(this)` 并注册 `XposedServiceHelper`。
- `legacy/view/activity/MainActivity.kt`：传统 View 管理页入口，Manifest 指向这里。

## 依赖注入

DI 文件：
- `di/RevivedKoin.kt`

设计要点：
- `ensureStarted(context: Context? = null)` 是幂等启动入口。
- 在模块 App 进程可以传入 `Application Context`。
- 在 Xposed 宿主注入进程可以无 Context 启动 Koin，避免依赖宿主 Application 生命周期。
- `HomeViewModel` 需要初始状态参数，通过 `parametersOf(initialState)` 注入。
- `homeViewModelFactory()` 用于自建 `ViewModelStoreOwner` 场景，例如注入宿主 ViewTree 的 ComposeView。

不要在 Hook 代码里直接 `HomeViewModel(...)`。需要临时实例时也应通过 `RevivedKoin.createHomeViewModel(...)`。

## 顶层目录职责

根包：
`app/src/main/java/me/padi/qqlite/revived`

主要目录：
- `di/`：Koin 启动器和模块注册。
- `hooks/`：Xposed Hook 与宿主适配。
- `compose/`：新 Compose UI 架构。
- `legacy/view/`：模块管理页传统 Android View 入口。
- `shared/`：跨 UI 体系共享的 model、ViewModel、状态工具。
- `utils/`：宿主 ComposeView 注入、反射、Context/Inflater 辅助工具。

## Hook 分层

`hooks/common/`
- 通用 Hook 基础设施和全局行为。
- 包含 `BaseHook`、`ModuleHookChain`、`ClassLookup`、`AutoSizeHook`、`StatusBarHook`、`PhoneResourcesHook` 等。

`hooks/aio/`
- 聊天页相关 Hook。
- `AioTopBarHook`：聊天页顶部栏适配。
- `AioAvatarHook`：聊天消息头像适配。

`hooks/home/`
- Home 页面宿主适配。
- `HomeComposeHook`：Hook 主入口，注入 Compose Home。
- `HomeHookState`：宿主类、字段、方法缓存，以及宿主数据到 shared model 的转换。
- `HomeBinding`：实现 `HomeUiController`，桥接 Compose UI 与宿主点击、头像、QZone load more。
- `HomeRuntimeStore`：Hook 运行时弱引用缓存和主线程 Handler。
- `HomePageResolver`、`HomeProfileCache`、`HomeHostInterop`：Home 专属宿主适配工具。

Hook 层可以依赖：
- `shared`
- `compose/screens/home/HomeUiController`
- `compose/screens/home/HomeScreen`

Hook 层不应持有 UI 状态真相；状态应进入 ViewModel。

## Compose 架构

`compose/`
- `screens/`：页面级 Composable。

Home Compose 目录：
- `compose/screens/home/HomeScreen.kt`
- `compose/screens/home/HomeUiController.kt`
- `compose/screens/home/pages/*`

关键边界：
- Compose Home 只依赖 `HomeUiController` 和 `shared.model.home`。
- Compose Home 不直接依赖 `hooks.home.HomeBinding` 或其他 Hook 包。
- `HomeUiController` 把 UI 事件抽象为接口：页面切换、点击、滚动快照、头像宿主 View 创建、QZone load more。

当前依赖方向：
`hooks/home -> compose/screens/home -> shared`

不要反过来让 `compose` 依赖 `hooks`。

## 传统 View 架构

`legacy/view/`
- `activity/`：模块管理页 Activity。

当前主 Activity：
- `legacy/view/activity/MainActivity.kt`

MainActivity 规则：
- 继承 `ComponentActivity`。
- 通过 Koin 获取 `MainViewModel`。
- UI 文本来自 `MainUiState`。
- XposedService 回调只更新 ViewModel，不在 Activity 中拼接长期状态。

## Shared 状态层

`shared/model/home/`
- `HomeModels.kt`：Home UI 状态和行模型，例如 `HomeUiState`、`RecentRow`、`ContactRow`、`QZoneFeedRow`。
- `HomeUiConstants.kt`：Home 纯 UI/状态常量和 `coerceInHome`。
- `HomeImageValue.kt`：图片地址归一化等纯函数。

`shared/viewmodel/home/`
- `HomeViewModel.kt`：Home 状态唯一更新入口。
- `HomeStateStore.kt`：跨 ViewModel 生命周期保存 Home 最新快照和 profile。

`shared/model/module/`
- `MainUiState.kt`：模块管理页 UI 状态。

`shared/viewmodel/module/`
- `MainViewModel.kt`：模块管理页状态管理。

原则：
- shared 层不能依赖 hooks 层。
- shared model 尽量保持纯数据和纯函数。
- ViewModel 可以依赖 shared model，不应依赖 Android View 或宿主反射对象，除非当前模型中为了桥接宿主 raw item 已明确存在。

## Home 当前数据流

1. 宿主 Home Fragment 创建 View。
2. `HomeComposeHook` hook 宿主创建 View 的方法。
3. `HomeHookState` 读取宿主 ViewPager、反射方法、数据源。
4. `HomeBinding` 绑定宿主 View、事件 View、host fragment。
5. `addHostComposeView` 创建带 Lifecycle/SavedState/ViewModelStore 的 ComposeView。
6. `HomeBinding.attachViewModel(owner)` 使用 Koin + ViewModelProvider 获取 `HomeViewModel`。
7. `HomeScreen(controller)` 收集 `controller.uiState` 渲染。
8. UI 事件通过 `HomeUiController` 回到 `HomeBinding`，再调用宿主方法。
9. Hook 到的新数据通过 `HomeBinding` 更新 `HomeViewModel`。

## 关键工具

`utils/HostComposeView.kt`
- 在宿主 ViewGroup 中安全注入 ComposeView。
- 自建 `LifecycleOwner`、`SavedStateRegistryOwner`、`ViewModelStoreOwner`。
- 对 Xposed 注入宿主场景非常关键，改动需谨慎。

`utils/MaterialInflater.kt`
- 宿主 Material 组件/主题相关 inflater context。

`hooks/common/ClassLookup.kt`
- 宿主类查找工具。

## Gradle 与主要依赖

版本集中在：
- `gradle/libs.versions.toml`

关键依赖：
- libxposed API / service
- Jetpack Compose UI / Foundation / Material 3
- Lifecycle Runtime / ViewModel / Compose integration
- Navigation Compose
- Koin Android / Koin Compose / Koin ViewModel
- Coil 3
- Miuix KMP UI

Koin 当前版本：`4.2.2`。

## 修改规则

优先级：
1. 先理解当前依赖方向。
2. Hook 逻辑放到对应页面目录；非页面专属放 `hooks/common`。
3. UI 状态放 ViewModel，不在 Activity/Composable/Hook 中长期手搓状态。
4. Compose 页面放 `compose/screens`。
5. 传统 View 放 `legacy/view`。
6. shared 层不能引用 hooks。

不要做：
- 不要让 Compose 直接 import `hooks.*`。
- 不要在 Hook 层直接构造 ViewModel。
- 不要把宿主反射细节扩散到 UI 层。
- 不要为了未来可能用到的能力提前加复杂抽象。
- 不要主动 commit、push、reset。

## 验证命令

常用编译验证：

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

注意：当前环境中 Gradle wrapper 访问 `D:\.gradle` 可能需要提升权限。

结构扫描建议：

```powershell
rg -n "me\.padi\.qqlite\.revived\.hooks" "app/src/main/java/me/padi/qqlite/revived/compose"
rg -n "me\.padi\.qqlite\.revived\.hooks" "app/src/main/java/me/padi/qqlite/revived/shared"
```

期望：
- Compose 层不引用 hooks。
- Shared 层不引用 hooks。

## 当前已知状态

- 宿主运行正常。
- 模块自身运行正常。
- `HomeComposeHook` 是当前 Home 主路径。
- 最新验证命令 `.\gradlew.bat :app:compileDebugKotlin` 已通过。
