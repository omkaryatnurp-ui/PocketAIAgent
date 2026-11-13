# Pocket AI Agent — Setup & Developer Guide

## Prerequisites & Architecture

- **Operating System**: Android 8.0 (API 26) through Android 14+ (API 34).
- **Target Java**: Java 17 (OpenJDK 17).
- **Build System**: Gradle 8.7 + Android Gradle Plugin 8.4.1.
- **Language**: Kotlin 1.9.24.
- **UI Framework**: Jetpack Compose with Material 3.

---

## 🏗️ Local Development & Build Instructions

If compiling directly on an Android device running Termux or a local Linux environment:

1. **Verify Java 17**:
   ```bash
   java -version
   # Expected: openjdk version "17.0.x"
   ```

2. **Run Automated Unit Tests**:
   ```bash
   ./gradlew test --stacktrace
   ```

3. **Assemble the Debug APK**:
   ```bash
   ./gradlew assembleDebug --stacktrace
   ```
   The resulting APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Security & Privacy Model

Pocket AI Agent strictly conforms to mobile zero-trust design:

1. **Hardware-Backed Encryption**:
   - `SecureKeyStorage` utilizes `androidx.security.crypto.EncryptedSharedPreferences` backed by the Android Keystore.
   - Keys are never committed to version control, hardcoded in strings, or transmitted to third-party tracking services.

2. **Automated Log Redaction**:
   - Every log record stored in `AppDatabase` passes through `LogRedactor.redact()`.
   - Regular expressions mask Gemini (`AIza...`), Claude (`sk-ant-api...`), DeepSeek (`sk-...`), and GitHub tokens (`ghp_...`), as well as HTTP `Authorization: Bearer` headers.

3. **Workspace Path Containment**:
   - All file operations in `WorkspaceManager` and `SandboxedTerminal` verify that canonical paths strictly start with `workspaceDirectory.canonicalPath`. Any attempted path traversal (`../../etc/passwd`) is immediately blocked with a `SecurityException`.
