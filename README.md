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

## 🔑 Authentication & Demo Access

The platform features a secure local authentication engine with local SQLite storage. Passwords are encrypted using a unique cryptographic salt and the SHA-256 algorithm before storage.

For rapid evaluation and testing, the application automatically seeds an enterprise developer profile upon first launch:
- **Email:** `demo@enterprise.com`
- **Password:** `password123`

*(Alternatively, you can create a new profile instantly using the **"ลงทะเบียนสร้างบัญชี"** button on the Login Screen.)*

---

## 🛠️ Local Prototyping & "Run Locally" Guide

### 1. Requirements & IDE Setup
- **Android Studio** (Koala or newer recommended)
- **JDK 17** or newer
- **Android SDK** (API Level 24 to 36)

### 2. Configure Your Environment Variables
To use direct sandbox analysis (bypassing the secure proxy gateway), set up your API keys:
1. Copy the template `.env.example` to a new file named `.env` in the project root:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and configure your API key:
   ```env
   GEMINI_API_KEY=your_actual_gemini_api_key
   ```
The Secrets Gradle Plugin will automatically read this file and inject the key into `BuildConfig` at compile-time.

---

## 🔐 Keystore & Build Configuration

### 1. Debug Keystore Setup (CRITICAL)
For local development and debug builds, the build chain (`app/build.gradle.kts`) references a `debug.keystore` at the root directory. Because this file is ignored by Version Control (via `.gitignore`), you **must** generate it locally before building the app to avoid compile errors.

Run the following command in the project root to generate your local debug keystore:
```bash
keytool -genkey -v -keystore debug.keystore -storealias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
```

### 2. Release Signing Configuration
To generate production-ready signed APKs or AABs, the build system checks for the presence of a release keystore. You must supply your release signing keys using the following system environment variables:

- `KEYSTORE_PATH`: Absolute filesystem path to your release `.jks` keystore (defaults to `${rootDir}/my-upload-key.jks` if not set).
- `STORE_PASSWORD`: The master password for the keystore (defaults to `android`).
- `KEY_ALIAS`: The alias of your private signing key (defaults to `upload`).
- `KEY_PASSWORD`: The password for the specific private key (defaults to `android`).

*Note: The build pipeline employs a fail-fast verification check. If you attempt to run any `Release` task and the keystore at `KEYSTORE_PATH` is missing, the build process will exit immediately with an error message instructing you to configure your environment variables.*

---

## 🌐 Enterprise Secure Proxy (Gateway) Configuration

On the **Dashboard (แผงควบคุม)** screen, enterprise operators can bypass local API key injection by routing requirements and security scans through an internal secure proxy gateway:
1. **Enable Proxy:** Toggle the **"เส้นทางการเรียก AI (Enterprise Routing)"** switch.
2. **Define Endpoint:** Provide your internal endpoint in the **"ที่อยู่ API Gateway (Backend URL)"** field (defaults to `https://gateway.enterprise-analyst.ai/v1/analyze`).
3. **Set Credentials:** Input your Bearer token in the **"โทเค็นรับสิทธิ์ฝั่งเกตเวย์หลังบ้าน (Authorization Bearer Token)"** field.
When active, client-side requests are dispatched exclusively to the secure proxy server, appending the Bearer token in the `Authorization: Bearer <token>` header, enforcing Zero-Trust credential security.
