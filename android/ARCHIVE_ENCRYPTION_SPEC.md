# MF MPanel Evidence — Archive Encryption Specification

## 1. Overview & Encryption Contract

MF MPanel Evidence exports evidence archives as genuine **AES-256 encrypted ZIP archives**. This guarantees that captured visible screenshots, original URLs, Link IDs, and manifest records remain fully confidential in transit (including over WhatsApp and external storage) and can only be opened by authorized administrators with the correct archive password.

### Cryptographic Parameters

| Parameter | Specification |
| :--- | :--- |
| **Archive Format** | ZIP (PKZip Standard format with AES extension) |
| **Encryption Standard** | **AES-256** (WinZip AES specification / RFC 5114 compatible) |
| **Key Derivation** | PBKDF2 with HMAC-SHA1 (1,000 iterations per WinZip AE-2 specification) |
| **Key Length** | 256 bits |
| **Compression Method** | DEFLATE |
| **Authentication & Integrity** | 10-byte HMAC-SHA1 authentication code at end of encrypted stream |
| **File Checksums** | SHA-256 (64-character hex lowercase) per individual screenshot and manifest.json |
| **Filename Obfuscation / Encrypted Entries** | Entry contents (`manifest.json` and all `links/<linkId>/screenshot_xxxx.jpg`) are encrypted. |

---

## 2. Archive File Structure

When decrypted using the authorized password, the ZIP archive unpacks into the following deterministic directory tree:

```text
MF_Evidence_<batchId>_<exportId>.zip
├── manifest.json
└── links/
    ├── <linkId_1>/
    │   ├── screenshot_0001.jpg
    │   ├── screenshot_0002.jpg
    │   └── ...
    ├── <linkId_2>/
    │   ├── screenshot_0001.jpg
    │   └── ...
```

### Path Traversal Protection
All entry paths in the archive are strictly normalized and sanitized:
- No relative parent navigators (`..`) are permitted.
- No leading slashes (`/` or `\`) or drive specifiers are permitted.
- Any archive containing path traversal sequences MUST be rejected by the Website Admin ZIP Generator.

---

## 3. Manifest Specification (`manifest.json`)

The root `manifest.json` file describes the evidence payload and provides cryptographic checksums for automated verification:

```json
{
  "manifestVersion": "1.0.0",
  "exportId": "EXP-123456",
  "batchId": "BATCH-20261015-01",
  "exportingUserUid": "USR-998877",
  "exportTimestamp": 1792055500000,
  "totalLinks": 2,
  "totalScreenshots": 3,
  "encryptionAlgorithm": "AES-256",
  "links": [
    {
      "linkId": "A01",
      "url": "https://www.instagram.com/instagram/",
      "platform": "Instagram",
      "profileId": "@instagram",
      "requiredCount": 1,
      "actualCount": 2,
      "status": "COMPLETED",
      "screenshots": [
        {
          "screenshotId": "A01-SCR-0001",
          "fileName": "screenshot_0001.jpg",
          "capturedAt": 1792055400000,
          "fileSizeBytes": 145820,
          "sha256Checksum": "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890",
          "mimeType": "image/jpeg"
        },
        {
          "screenshotId": "A01-SCR-0002",
          "fileName": "screenshot_0002.jpg",
          "capturedAt": 1792055410000,
          "fileSizeBytes": 152900,
          "sha256Checksum": "c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890a1b2",
          "mimeType": "image/jpeg"
        }
      ]
    }
  ]
}
```

---

## 4. Website Admin ZIP Generator Decryption & Validation Protocol

The MF MPanel Admin ZIP Generator executes the following 7-step validation workflow:

1. **Password Authentication**: The administrator enters the archive password. The generator tests decryption on `manifest.json`. If incorrect, extraction fails fast and an audit event is logged.
2. **Path Traversal Audit**: All file headers are examined before extraction to ensure no paths contain `..` or illegal characters.
3. **Manifest Version Check**: Verifies `manifestVersion == "1.0.0"`.
4. **Link Matching**: Matches each `linkId` against the existing database records for the specified `batchId`.
5. **SHA-256 Verification**: Computes the SHA-256 checksum of every extracted screenshot file and asserts exact match against `sha256Checksum`.
6. **Completeness Audit**: Checks that `actualCount >= requiredCount` for all links.
7. **Report Generation**: Emits an import validation audit report and updates the workflow status.
