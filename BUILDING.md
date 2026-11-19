# Building Time-Cost APKs

## Android Studio

1. Open the project root (the directory containing `settings.gradle.kts`) in Android Studio.
2. Install Android SDK Platform 35 and Android SDK Build-Tools 35.0.0 from **Tools → SDK Manager**. Use JDK 17 or newer (the project targets Java 17).
3. Wait for Gradle Sync to finish successfully. If Android Studio reports that the SDK location is missing, set **File → Project Structure → SDK Location** to the installed Android SDK directory. Do not commit `local.properties`; its `sdk.dir` value is specific to each computer.
4. To create APKs, choose **Build → Generate App Bundles or APKs → Build APK(s)**. The project has two flavors: `demo` and `prod`. Select `demoDebug` or `prodDebug` in **Build Variants** if you only want one APK.
5. APK output paths are `app/build/outputs/apk/demo/debug/app-demo-debug.apk` and `app/build/outputs/apk/prod/debug/app-prod-debug.apk`.

## Command line

On Linux/macOS, from the project root run:

```sh
./gradlew assembleDemoDebug assembleProdDebug
```

On Windows, run `gradlew.bat assembleDemoDebug assembleProdDebug`.

If Gradle cannot find the Android SDK, configure `ANDROID_HOME`/`ANDROID_SDK_ROOT` or create a local, untracked `local.properties` containing `sdk.dir=/absolute/path/to/your/Android/Sdk` (Windows paths should use escaped backslashes or forward slashes). The Gradle Wrapper downloads the required Gradle distribution automatically.

## Validation performed

Both `assembleDemoDebug` and `assembleProdDebug` built successfully. Unit tests for both variants and lint for both variants completed successfully. Lint reported warnings only; no build errors were reported.
