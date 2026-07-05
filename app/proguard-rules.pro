# ============================================================
# Gson 序列化/反序列化保护规则
# 防止 R8 移除或混淆 Gson 使用的数据类、TypeToken 等
# ============================================================

# 保留所有使用 @SerializedName 注解的字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留 Gson TypeToken（泛型类型信息依赖匿名子类）
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# 保留所有数据模型类（ApiResponse 及所有 plan model）
-keep class com.nku.helloworld.auth.model.** { *; }
-keep class com.nku.helloworld.ui.plan.model.** { *; }

# 保留 Gson 核心类
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}