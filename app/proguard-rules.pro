# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
 
# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
 
# Gson
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保留被 Gson 注解的类
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class cn.zhangjh.zhiyue.api.BookService {
    public static <methods>;
}
-keepattributes EnclosingMethod
 
# Retrofit with Gson or Moshi converter
-keep class cn.zhangjh.zhiyue.model.** { *; }
-keep class cn.zhangjh.zhiyue.request.** { *; }
-keep class cn.zhangjh.zhiyue.viewmodel.** { *; }

