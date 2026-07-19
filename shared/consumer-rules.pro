# Keep the native external methods inside TsnetManager
-keepclasseswithmembers class io.github.runc0derun.watcharr.shared.playback.TsnetManager {
    native <methods>;
}

# Keep the TsnetCallback interface and its methods since JNI invokes them via reflection
-keep interface io.github.runc0derun.watcharr.shared.playback.TsnetManager$TsnetCallback {
    *;
}
