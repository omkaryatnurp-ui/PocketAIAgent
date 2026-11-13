# Pocket AI Agent 📱⚡🤖

**Your Personal Android AI Coding Workspace**

Turn any Android smartphone into a mobile AI software engineering workstation. Connect Google Gemini, Anthropic Claude, DeepSeek, or run completely offline with the zero-dependency built-in Free Mode. Manage multi-agent coding teams, edit code with real-time syntax styling, execute sandboxed terminal commands, and track background jobs with battery-aware Android runtime services.

---

## 🌟 Key Capabilities

- **Android-First & Mobile-Optimized**: Designed specifically for small touchscreens with a dark developer aesthetic, high-contrast typography, and low memory consumption.
- **100% Free AI Mode**: Built-in offline rule engine generates implementation plans, scaffolds full projects, and provides AST-based linting without requiring any API keys or network connection.
- **Extensible Multi-Provider AI**: Connect your own API keys for **Google Gemini** (with generous 15 RPM free tier), **Anthropic Claude 3.5 Sonnet**, **DeepSeek-V3 / R1**, and **Antigravity Bridge**.
- **Multi-Agent Coding Team**: Orchestrates a 6-stage development pipeline:
  1. **Planner Agent**: Analyzes the request and decomposes it into numbered milestones.
  2. **Developer Agent**: Writes and edits source code safely within the project workspace.
  3. **Debugger Agent**: Inspects syntax, nullability, and runtime stack traces.
  4. **Tester Agent**: Generates and validates unit test assertions.
  5. **Reviewer Agent**: Audits code quality, diffs, and guarantees zero credential leakage.
  6. **Coordinator Agent**: Oversees pipeline approvals, checkpoints, and executive reports.
- **Background Task Manager & Recovery**: Uses Android Foreground Services with ongoing interactive notifications (Pause, Resume, Cancel) and persistent Room database checkpoints that survive app restarts.
- **Mobile IDE & Sandboxed Shell**:
  - Sandboxed file explorer supporting Android, Kotlin, Java, Python, Web (HTML/JS), Rust, and Node.js.
  - In-app code editor with line numbers, search & replace, and unified diff comparison.
  - Restricted terminal emulator with command history and destructive command protection.
  - Project ZIP export and import.
- **Device & Battery Awareness**: Dedicated Resource Manager monitors real-time battery percentage, charging state, device temperature, RAM, and internal storage, with configurable profiles (Balanced, Performance, Battery Saver, Charging Only, Wi-Fi Only, Thermal Protection).
- **Hardened Security**: All credentials are stored in hardware-backed `EncryptedSharedPreferences`. Logs are scrubbed by `LogRedactor` before being saved or rendered. Zero telemetry collected.

---

## 📲 How to Build & Download APK from Your Phone (No PC Required)

You do **not** need a PC or Android Studio to build and install Pocket AI Agent on your smartphone. Follow these simple steps using GitHub Actions:

1. **Fork or Push** this repository to your GitHub account.
2. In your phone's browser or the GitHub mobile app, open your repository and tap the **Actions** tab.
3. Select the **Build Debug APK** workflow.
4. Tap **Run workflow** -> **Branch: main** -> **Run workflow**.
5. Wait ~2–3 minutes for the build to finish with a green checkmark.
6. Tap the completed workflow run, scroll down to **Artifacts**, and tap **`pocket-ai-agent-debug-apk`**.
7. Extract the downloaded ZIP file on your phone and tap `app-debug.apk` to install!

---

## 📂 Project Structure

```
pocket-ai-agent/
├── app/
│   ├── build.gradle.kts           # Android compile SDK 34, Compose, Room, WorkManager
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions, activities & foreground service
│       │   ├── java/com/pocketai/agent/
│       │   │   ├── PocketAiApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/      # Room database, 9 entities, 4 DAOs, migrations
│       │   │   │   ├── security/   # EncryptedSharedPreferences & LogRedactor
│       │   │   │   ├── provider/   # Gemini, Claude, DeepSeek, Antigravity, FreeMode
│       │   │   │   ├── agent/      # Multi-agent coordinator & pipeline stages
│       │   │   │   ├── project/    # Workspace file manager & ZIP exporter
│       │   │   │   ├── terminal/   # Sandboxed shell with command protection
│       │   │   │   ├── git/        # GitHub API client, local commits, diff generator
│       │   │   │   ├── device/     # Battery metrics & hardware thermal protection
│       │   │   │   └── service/    # TaskExecutionService & WorkManager
│       │   │   └── ui/             # Jetpack Compose UI (20 fully wired screens)
│       └── test/                   # Automated unit test suite
├── .github/workflows/build-apk.yml # Automated CI/CD pipeline
├── SETUP.md                        # Setup and local build instructions
├── API_CONFIG.md                   # Free tier and provider configuration guide
└── README.md
```

---

## 🛡️ Acceptance & Testing Validation

| Acceptance Criterion | Verification Method | Status |
| :--- | :--- | :--- |
| **App Launches & Navigates** | Jetpack Compose NavHost with 20 distinct routes | ✅ Verified |
| **Project Creation & Persistence** | Room `projects` & `project_files` surviving restarts | ✅ Verified |
| **Multi-Provider AI & Free Tier** | Gemini, Claude, DeepSeek, Antigravity, Free Mode | ✅ Verified |
| **Offline Free Mode** | Local rule engine with AST planning & synthesis | ✅ Verified |
| **Background Task Runtime** | Foreground service with notification & pause/resume | ✅ Verified |
| **Task Checkpoints & Recovery** | `task_checkpoints` snapshot restore mechanism | ✅ Verified |
| **Credential Security & Redaction** | Hardware Keystore + `LogRedactor` unit test pass | ✅ Verified |
| **ZIP Project Export** | In-memory and disk ZIP stream verification | ✅ Verified |
| **Sandboxed Terminal** | Directory traversal blocked; destructive cmd alert | ✅ Verified |
