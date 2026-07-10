-dontwarn io.github.libxposed.annotation.**
-dontwarn androidx.window.extensions.WindowExtensions
-dontwarn androidx.window.extensions.WindowExtensionsProvider
-dontwarn androidx.window.extensions.area.ExtensionWindowAreaPresentation
-dontwarn androidx.window.extensions.core.util.function.Consumer
-dontwarn androidx.window.extensions.core.util.function.Function
-dontwarn androidx.window.extensions.core.util.function.Predicate
-dontwarn androidx.window.extensions.layout.DisplayFeature
-dontwarn androidx.window.extensions.layout.FoldingFeature
-dontwarn androidx.window.extensions.layout.WindowLayoutComponent
-dontwarn androidx.window.extensions.layout.WindowLayoutInfo
-dontwarn androidx.window.sidecar.SidecarDeviceState
-dontwarn androidx.window.sidecar.SidecarDisplayFeature
-dontwarn androidx.window.sidecar.SidecarInterface$SidecarCallback
-dontwarn androidx.window.sidecar.SidecarInterface
-dontwarn androidx.window.sidecar.SidecarProvider
-dontwarn androidx.window.sidecar.SidecarWindowLayoutInfo
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Keep ViewTree owner bridge classes and their static get/set methods.
# These are accessed by exact class/method names via reflection in HostComposeView.kt.
-keep class androidx.lifecycle.ViewTreeLifecycleOwner {
    public static ** get(android.view.View);
    public static void set(android.view.View, androidx.lifecycle.LifecycleOwner);
}

-keep class androidx.lifecycle.ViewTreeViewModelStoreOwner {
    public static ** get(android.view.View);
    public static void set(android.view.View, androidx.lifecycle.ViewModelStoreOwner);
}

-keep class androidx.savedstate.ViewTreeSavedStateRegistryOwner {
    public static ** get(android.view.View);
    public static void set(android.view.View, androidx.savedstate.SavedStateRegistryOwner);
}

-keep class androidx.navigationevent.ViewTreeNavigationEventDispatcherOwner {
    public static ** get(android.view.View);
    public static void set(android.view.View, androidx.navigationevent.NavigationEventDispatcherOwner);
}
