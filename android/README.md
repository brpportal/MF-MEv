# MF MPanel Evidence — Advanced Android Evidence Collection App

MF MPanel Evidence is a native Android application built for the MF MPanel ecosystem. It enables authorized Evidence Officers to import website-generated link batches, capture high-fidelity visible screenshots inside an integrated, session-preserving in-app browser, store evidence securely offline, and export genuine AES-256 encrypted ZIP archives with automated WhatsApp and Storage Access Framework sharing.

---

## 1. Core Workflow

```text
MF MPanel Website (Batch JSON)
       │
       ▼
Android Document Picker / Direct Paste
       │
       ▼
In-App Browser (WebView with Cookie / Session Reuse)
       │
       ▼
Minimal Capture Interface (Only "Capture" & "Back" Controls)
       │
       ▼
Automatic Collision-Resistant ID Generation (<linkId>-SCR-0001)
       │
       ▼
Local App-Private Storage (`evidence/<batchId>/<linkId>/...`)
       │
       ▼
Room Database Transaction & Metadata Sync Outbox (WorkManager)
       │
       ▼
AES-256 Encrypted ZIP Export (`MF_Evidence_<batchId>_<exportId>.zip`)
       │
       ▼
WhatsApp Sharing / Save to Device (FileProvider & SAF)
       │
       ▼
Website Admin ZIP Generator (Password Decryption & SHA-256 Integrity Verification)
```

---

## 2. Architecture & Technology Stack

- **Language:** 100% Kotlin
- **UI Toolkit:** Jetpack Compose with Material Design 3 (Edge-to-Edge window insets)
- **Local Persistence:** Room Database with KSP (`AppDatabase`, `BatchDao`, `LinkDao`, `ScreenshotDao`, `SyncOutboxDao`, `ExportDao`)
- **Browser & Session Engine:** Android WebView with persistent `CookieManager`, `WebStorage`, and `PlatformSessionManager` (Instagram, Facebook, TikTok, YouTube, X, and HTTPS websites)
- **Cryptography & Archives:** Pure AES-256 ZIP encryption using `Zip4j` (`net.lingala.zip4j`), SHA-256 cryptographic checksums, and cryptographically secure password generation
- **Offline Background Synchronization:** Android WorkManager (`SyncWorker`) with Room Outbox pattern
- **Sharing:** Android `FileProvider` with `FLAG_GRANT_READ_URI_PERMISSION`, dedicated WhatsApp document dispatch, and Storage Access Framework `CreateDocument`
- **Cloud Backend:** Firebase Authentication (Jetpack `CredentialManager` Google Sign-In) and Cloud Firestore metadata synchronization

---

## 3. Minimal Full-Screen Capture Interface

Per the strict specification, when collecting evidence, the webpage occupies the full screen viewport. The interface contains **only two compact, accessible controls**:
1. **Back Button**: Navigates back in WebView history if available, or returns safely to the Link Queue without discarding any captured evidence.
2. **Capture Button**: Captures the visible viewport, generates the sequential ID (`<linkId>-SCR-xxxx`), saves atomically to app-private storage, computes SHA-256, updates Room stats, enqueues metadata sync, and displays a subtle transient confirmation.

No decorative overlays, toolbars, or visual distractions cover the webpage.

---

## 4. Platform Login & Session Management

Located in the **Settings** and **Platforms** screen:
- Supported platforms: Instagram, Facebook, TikTok, YouTube, X (Twitter), and general HTTPS sites.
- Status detection: Uses `CookieManager` to verify valid session tokens.
- **Privacy Guarantee:** Platform passwords and authentication tokens are never stored, extracted, logged, or uploaded to Firebase. Sessions remain strictly inside the Android app sandbox.
- **Logout:** Allows clearing cookies for individual platforms or wiping all browser sessions with confirmation.

---

## 5. AES-256 Archive Encryption Specification

All exports use PKZip AES-256 format (`ZipParameters.encryptionMethod = EncryptionMethod.AES`, `aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256`).
- **Archive contents:**
  - `manifest.json`: Root metadata file detailing versions, Link IDs, profile handles, URLs, and individual screenshot SHA-256 hashes.
  - `links/<linkId>/screenshot_0001.jpg`: Sequentially indexed evidence images.
- **Password Security:**
  - Generated using `SecureRandom` (16 characters: uppercase, lowercase, digits, symbols).
  - Never stored in plaintext in the database, never included in export ZIP filenames, and never appended to WhatsApp text messages.
  - The officer securely provides the password to the administrator via their authorized secondary channel.

---

## 6. Website Admin ZIP Generator Integration

A reference validation tool is provided at `website_admin_zip_generator/admin_zip_validator.py`.
Administrators can decrypt and validate exported ZIPs using:
```bash
python3 website_admin_zip_generator/admin_zip_validator.py <path_to_zip> <password>
```
The validator audits:
- Correct password decryption
- Path traversal rejection (`..` / illegal paths)
- Manifest version (`1.0.0`)
- Link ID matching
- File SHA-256 checksums

---

## 7. Firebase Configuration Instructions

To connect to your existing MF MPanel Firebase project:
1. Place the official `google-services.json` from the Firebase Console into `/app/google-services.json`.
2. Ensure your Firebase project ID matches the existing project.
3. In `app/src/main/res/values/firebase_applet_config.xml`, specify the custom Firestore database ID if applicable.
4. When testing without live Firebase credentials, the application automatically operates in local Evidence Team mode, enabling complete testing of batch imports, in-app browser captures, local database transactions, and encrypted exports.

---

## 8. Integration Verification Status

| Component | Status | Details |
| :--- | :--- | :--- |
| **Jetpack Compose UI** | Verified | Clean M3 theme, full edge-to-edge support |
| **In-App WebView Browser** | Verified | Persistent cookies, JavaScript, overview viewport |
| **Minimal Capture Interface** | Verified | Only 2 controls (Capture & Back), no clutter |
| **Automatic Screenshot IDs** | Verified | Collision-resistant UUID + `<linkId>-SCR-xxxx` |
| **Local Room Database** | Verified | Batches, links, screenshots, outbox, exports |
| **AES-256 ZIP Encryption** | Verified | Zip4j AES-256, SHA-256 checksums, manifest |
| **Decryption Round-Trip** | Verified | Automated tests confirm encryption & extraction |
| **FileProvider & Sharing** | Verified | Native share sheet & direct WhatsApp document intent |
| **Storage Access Framework** | Verified | SAF `CreateDocument` for local file export |
| **WorkManager Sync Outbox** | Verified | Offline-first retry queue for metadata events |
| **Website Admin Validator** | Verified | Python / Node.js reference validator script included |
