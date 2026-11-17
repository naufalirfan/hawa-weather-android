# Optimization and obfuscation rules for Hawa Weather

# Preserve line numbers for readable stack traces in Play Console
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider

# Data classes and model parsing if any
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
