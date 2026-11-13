# AI Provider Setup & Quota Configuration

Pocket AI Agent provides a flexible, multi-provider AI architecture that allows you to switch between free offline engines, free-tier cloud models, and frontier coding APIs.

---

## 1. Free Mode (Offline Rule Engine) 🆓

- **Cost**: $0.00 (100% Free).
- **Network Requirement**: None (Works completely offline in airplane mode).
- **API Key**: Not required.
- **Capabilities**:
  - Generates implementation plans and architectures for common project types (Android, Python, Web, Rust, Node.js).
  - Code generation based on templates and clean architecture best practices.
  - Static AST/regex-based linting, syntax error diagnostics, and null-safety fixes.
  - Automated unit test boilerplate generation.

---

## 2. Google Gemini (Generous Free Tier) 🌐

Google AI Studio provides a free tier for developers:
- **Free Quotas**:
  - **Gemini 1.5 Flash**: Up to 15 Requests Per Minute (RPM) and 1,000,000 Tokens Per Minute (TPM) completely free.
  - **Gemini 1.5 Pro**: Free tier up to 2 RPM with 2M token context window.
- **How to Get a Free Key**:
  1. Visit [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) from your phone's browser.
  2. Sign in with your Google account.
  3. Tap **Create API Key**.
  4. In Pocket AI Agent, go to **Settings** -> **AI Providers** -> **Google Gemini** -> **Set Key**, paste your key, and tap **Save Key**.
  5. Tap **Test API** to verify connectivity!

---

## 3. Anthropic Claude (Frontier Coding) 🧠

- **Supported Models**: `claude-3-5-sonnet-20241022`, `claude-3-haiku-20240307`, `claude-3-opus-20240229`.
- **How to Configure**:
  1. Go to [https://console.anthropic.com/settings/keys](https://console.anthropic.com/settings/keys).
  2. Create a new key (`sk-ant-api...`).
  3. In Pocket AI Agent, select **Anthropic Claude** in **AI Providers** and paste your token.

---

## 4. DeepSeek (Cost-Effective Reasoning) 💡

- **Supported Models**: `deepseek-chat` (DeepSeek-V3), `deepseek-reasoner` (DeepSeek-R1).
- **How to Configure**:
  1. Go to [https://platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys).
  2. Create an API key (`sk-...`).
  3. In Pocket AI Agent, select **DeepSeek** in **AI Providers** and save your key.

---

## 5. Antigravity Bridge 🚀

- **Integration Mode**: Workspace synchronization and task manifest handoff protocol (`antigravity-handoff-v1`).
- Compliant integration using structured project export bundles, task manifest handoff, and external agent workflow synchronization without accessing unauthorized private APIs.
