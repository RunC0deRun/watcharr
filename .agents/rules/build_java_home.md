# JAVA_HOME Configuration for Builds

For all build and execution commands (e.g., Gradle builds, running unit tests, assembling the application) in this project, you must use the following JDK path:

```
/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

## Guardrails
- Before running `./gradlew` or any other build command, set the `JAVA_HOME` environment variable to `/Applications/Android Studio.app/Contents/jbr/Contents/Home`.
- Example terminal execution:
  ```bash
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew build
  ```
