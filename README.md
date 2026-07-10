# Bento System Analyst (AI-Powered Enterprise System Analyst & Telemetry Platform)

Bento System Analyst is a professional, enterprise-grade Android application developed with modern Jetpack Compose and Kotlin. It serves as an elite AI-powered System Analyst, Security Auditor, and Architect. It automatically scans technical specifications, identifies security loopholes, designs ER diagrams, visualizes cloud topologies, and audits developer and operator actions.

## 🌟 Key Features

### 1. Multi-Dimensional AI Analysis (Zero-Trust Security)
- **Comprehensive Scans:** Automatically reviews requirements, business logic, system architecture, database design (ERD), API endpoints, OWASP security threats, and cloud infrastructure topologies.
- **Enterprise Secure Proxy (Gateway):** Run analysis securely without exposing sensitive client-side API keys. When configured, traffic is dispatched exclusively through the backend API gateway.
- **Direct Sandbox Mode:** Client-to-Gemini SDK prototyping for sandbox environments, featuring robust alert handling if the API key is not configured.

### 2. Live Telemetry & Real-Time Performance Audits
- **System Telemetry:** Real-time JVM memory tracking, active thread monitoring, and secure gateway ping verification.
- **Lifecycle-Bound Loops:** Periodic telemetry monitoring automatically suspends when the application enters the background to conserve device battery and cellular data.

### 3. Bulletproof Error & Crash Monitoring
- **JSON Serialization:** Crash logs are serialized safely using native Android JSON libraries to prevent injection or corruption.
- **Robust Exception Interceptor:** Captures unhandled runtime crashes and records detailed device metadata, stack traces, and active exception states.

## 🛠️ Tech Stack & Architecture

- **UI Framework:** Jetpack Compose (100%)
- **Architecture:** Clean MVVM with Single Source of Truth
- **Database:** SQLite Local Room Database with Coroutines Flow
- **Dependency Management:** Version Catalog (`libs.versions.toml`) & Gradle Kotlin DSL
- **Compilation Platform:** Android 13+ (Target SDK 36, Compile SDK 36)

## 🔑 Configuration & Setup

### AI Studio Secrets Panel
To enable direct sandbox analysis, configure your credentials in the **Secrets panel** of your AI Studio editor:
```env
GEMINI_API_KEY=your_actual_gemini_api_key
```

### Local Prototyping
You can copy `.env.example` to `.env` in the root directory and define:
```env
GEMINI_API_KEY=your_actual_gemini_api_key
```
The Secrets Gradle Plugin automatically reads and injects this key into `BuildConfig` at compile-time.
