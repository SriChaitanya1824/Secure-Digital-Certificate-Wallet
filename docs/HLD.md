# High-Level Design (HLD)

## Executive Summary

**Secure Digital Certificate Wallet** is a portfolio project demonstrating secure credential management for Android, featuring asymmetric cryptography, offline-first architecture, and privacy-preserving credential sharing.

This document provides the system-level view, key components, data flows, and trust model.

---

## 1. Business Problem

### Current State

Traditional physical and digital certificates suffer from:
- **Difficult Verification**: No reliable authenticity checks
- **Forgery Risk**: Easy to create fake documents
- **Poor Discovery**: Scattered across emails, clouds, paper
- **Privacy Issues**: Share entire certificate or nothing
- **Revocation Gaps**: No mechanism to invalidate outdated credentials

### Proposed Solution

A distributed wallet system where:
1. **Issuers** (universities, employers) digitally sign and publish credentials
2. **Wallet Holders** store, manage, and selectively share credentials
3. **Verifiers** scan QR codes and cryptographically verify authenticity

---

## 2. Goals

### Functional

✅ Store multiple digitally signed certificates  
✅ Verify certificate authenticity via signature validation  
✅ Support selective disclosure (share only needed claims)  
✅ Share credentials via scannable QR codes  
✅ Work offline with cached certificates  
✅ Revoke compromised credentials  
✅ Track verification history  

### Non-Functional

✅ Secure local storage (Android Keystore)  
✅ Biometric-protected sensitive operations  
✅ Responsive UI (Jetpack Compose)  
✅ Reliable offline operation  
✅ Scalable API design  
✅ Comprehensive error handling  

### Out of Scope

❌ Real integration with government certificate authorities  
❌ Production-grade HSM/KMS integration  
❌ Formal W3C Verifiable Credentials standard  
❌ Multi-region disaster recovery  
❌ AI-based fraud detection  

---

## 3. System Context Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     External Systems                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐         ┌──────────────┐               │
│  │  ISSUER      │         │  VERIFIER    │               │
│  │  Portal      │         │  App         │               │
│  │              │         │              │               │
│  │ Create &     │         │ Scan & Verify              │
│  │ Sign Certs   │         │              │               │
│  └──────┬───────┘         └────┬─────────┘               │
│         │                       │                         │
│         │ API Calls            │ API Calls               │
│         │                       │                         │
│         ▼                       ▼                         │
│  ┌─────────────────────────────────────────────────────┐ │
│  │         BACKEND API (Spring Boot + PostgreSQL)      │ │
│  │                                                      │ │
│  │  - Cert Management                                  │ │
│  │  - Verification                                     │ │
│  │  - Issuer Registry                                  │ │
│  │  - Revocation                                       │ │
│  │  - Audit Logs                                       │ │
│  └──────────────────┬──────────────────────────────────┘ │
│                     │                                     │
│                     │                                     │
│                     ▼                                     │
│  ┌─────────────────────────────────────────────────────┐ │
│  │         ANDROID WALLET                              │ │
│  │  (Kotlin + Jetpack Compose)                         │ │
│  │                                                      │ │
│  │  - Home / Certificates                              │ │
│  │  - Import / Share                                   │ │
│  │  - QR Scanner                                       │ │
│  │  - Verification History                             │ │
│  │  - Settings                                         │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│              ANDROID APPLICATION (Kotlin)                    │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  UI Layer (Jetpack Compose)                            │ │
│  │  ─────────────────────────────────────────────────     │ │
│  │  HomeScreen │ CertificateList │ DetailScreen           │ │
│  │  ScannerScreen │ VerificationResult │ ProfileScreen     │ │
│  └────────────────┬───────────────────────────────────────┘ │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────────┐ │
│  │  ViewModel Layer (State Management)                    │ │
│  │  ─────────────────────────────────────────────────     │ │
│  │  WalletViewModel │ AuthViewModel │ ScannerViewModel    │ │
│  │  VerificationViewModel │ ProfileViewModel              │ │
│  └────────────────┬───────────────────────────────────────┘ │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────────┐ │
│  │  Repository Layer (Data Access)                        │ │
│  │  ─────────────────────────────────────────────────     │ │
│  │  CertificateRepository │ VerificationRepository        │ │
│  │  AuthRepository │ SyncRepository                       │ │
│  └────────────┬─────────────────┬───────────────────────┘ │
│               │                 │                          │
│  ┌────────────▼──────────────┐  │                         │
│  │  Local Data Layer         │  │                         │
│  │  ─────────────────────    │  │                         │
│  │  Room Database (SQLCipher) │  │                         │
│  │  - Certificates            │  │                         │
│  │  - Events                   │  │                         │
│  │  - Verification History     │  │                         │
│  └────────────────────────────┘  │                         │
│                                  │                         │
│                  ┌───────────────▼──────────────┐          │
│                  │  Remote Data Layer           │          │
│                  │  ──────────────────────      │          │
│                  │  Retrofit HTTP Client        │          │
│                  │  - API Services              │          │
│                  │  - JWT Authentication        │          │
│                  │  - Error Handling            │          │
│                  └──────────────────────────────┘          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Security Layer                                      │ │
│  │  ──────────────────────────────────────────────     │ │
│  │  Android Keystore │ Biometric │ Encryption          │ │
│  │  TokenStorage    │ SigningService                   │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Dependency Injection (Hilt)                         │ │
│  │  ──────────────────────────────────────────────     │ │
│  │  Module Configuration │ Component Bindings           │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│         BACKEND API (Spring Boot + Kotlin)                   │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  REST Controllers                                      │ │
│  │  ──────────────────────────────────────────────────   │ │
│  │  AuthController │ CertificateController              │ │
│  │  VerificationController │ SyncController              │ │
│  └────────────────┬───────────────────────────────────────┘ │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────────┐ │
│  │  Service Layer                                         │ │
│  │  ──────────────────────────────────────────────────   │ │
│  │  AuthService │ CertificateService                     │ │
│  │  VerificationService │ SigningService                 │ │
│  │  SyncService                                          │ │
│  └────────────────┬───────────────────────────────────────┘ │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────────┐ │
│  │  Repository Layer                                      │ │
│  │  ──────────────────────────────────────────────────   │ │
│  │  Spring Data JPA                                       │ │
│  │  UserRepository │ CertificateRepository               │ │
│  │  IssuerRepository │ RevocationRepository               │ │
│  └────────────────┬───────────────────────────────────────┘ │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────────┐ │
│  │  Database Layer (PostgreSQL)                           │ │
│  │  ──────────────────────────────────────────────────   │ │
│  │  users │ issuers │ certificates                       │ │
│  │  certificate_claims │ revocations │ events            │ │
│  │  verification_events │ audit_logs                     │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Security                                              │ │
│  │  ──────────────────────────────────────────────────   │ │
│  │  JWT Token Provider │ Spring Security Config          │ │
│  │  Cryptographic Signing Service                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. Certificate Lifecycle

```
┌─────────────┐
│   ISSUED    │   Issuer creates certificate, signs cryptographically
└──────┬──────┘
       │
       │ Import into Wallet
       │
       ▼
┌─────────────┐
│   ACTIVE    │   Valid for sharing, not expired, not revoked
└──────┬──────┘
       │
       ├─────────────────────────────────────┐
       │                                     │
       │ Expires                        Issuer Revokes
       │                                     │
       ▼                                     ▼
┌─────────────┐                        ┌──────────┐
│   EXPIRED   │                        │ REVOKED  │
└─────────────┘                        └──────────┘

Status Rules:
- ACTIVE: current_time < expires_at AND NOT revoked
- EXPIRED: current_time >= expires_at
- REVOKED: Issuer explicitly revoked (permanent)
- SUSPENDED: Issuer temporarily disabled (temporary hold)

Presentation Rules:
- ACTIVE: May be presented
- EXPIRED: Cannot be presented
- REVOKED: Cannot be presented (clearly marked)
- SUSPENDED: Cannot be presented
```

---

## 6. Trust Model

```
┌──────────────────────┐
│  ISSUER              │
│  (Organization)      │
│                      │
│ Has:                 │
│ - issuerId           │
│ - Public Key         │
│ - Trust Status       │
└──────┬───────────────┘
       │ Publishes
       │
       ▼
┌────────────────────────────┐
│ ISSUER REGISTRY            │
│ (Backend Database)         │
│                            │
│ Contains:                  │
│ - Trusted issuers          │
│ - Suspended issuers        │
│ - Revoked issuers          │
│                            │
│ Statuses:                  │
│ - TRUSTED   (verify certs) │
│ - SUSPENDED (reject certs) │
│ - REVOKED   (reject certs) │
└────────┬───────────────────┘
         │ Used by
         │
         ▼
┌─────────────────────────┐
│ VERIFICATION PROCESS    │
│                         │
│ For each QR code:       │
│ 1. Extract issuer       │
│ 2. Check issuer status  │
│ 3. Fetch issuer pubkey  │
│ 4. Verify signature     │
│ 5. Check expiry         │
│ 6. Check revocation     │
│ 7. Check replay         │
│                         │
│ Result: VERIFIED or NOT │
└─────────────────────────┘

Threat: Malicious Issuer
Mitigation: Only trust pre-configured issuers
           Unknown issuers rejected
           Issuer registry can be updated OTA

Limitation: Trust setup is manual/local
          In production: Use OIDC federation or trust anchors
```

---

## 7. Offline Architecture

```
┌─────────────────────────────────────────────────────────┐
│           ONLINE STATE                                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Wallet ──────► Backend API                             │
│  ├─ Sync certificates                                   │
│  ├─ Fetch issuer keys                                   │
│  ├─ Verify revocation status                            │
│  └─ Upload events                                       │
│                                                          │
│  All data in sync, verification accurate                │
│  SyncState = SYNCED                                     │
│                                                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│           OFFLINE STATE                                 │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Wallet ──X──► Backend API  [Connection Lost]           │
│                                                          │
│  Can do:                                                 │
│  ✓ View cached certificates                            │
│  ✓ Share credentials via QR                            │
│  ✓ Verify QR (offline mode)                            │
│  ✓ View history                                         │
│                                                          │
│  Cannot do:                                              │
│  ✗ Check current revocation status                      │
│  ✗ Verify against updated issuer keys                   │
│  ✗ Sync new certificates                                │
│                                                          │
│  UI clearly marks:                                       │
│  - "OFFLINE" mode                                       │
│  - "Last synced: X minutes ago"                          │
│  - "Revocation status may be stale"                      │
│                                                          │
│  SyncState = STALE or SYNC_FAILED                       │
│                                                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│           RECONNECTION STATE                            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Network returns ──► Automatic sync triggered           │
│                                                          │
│  Sync process:                                          │
│  1. Fetch all certificates (since last sync time)       │
│  2. Fetch revocations for local certs                   │
│  3. Merge local and remote state                        │
│  4. Update sync timestamp                               │
│  5. Mark any conflicts                                  │
│                                                          │
│  SyncState transitions:                                 │
│  STALE → PENDING_SYNC → SYNCED                         │
│  or                                                      │
│  STALE → PENDING_SYNC → SYNC_FAILED                    │
│                                                          │
└─────────────────────────────────────────────────────────┘

Conflict Resolution:
- Issuer revoked certificate  → Update local status
- Certificate deleted         → Mark as deleted locally
- New certificate available   → Fetch if requested
- Signature mismatch          → Flag for manual review
```

---

## 8. Security Architecture

### Authentication

```
Login/Register
        ↓
Backend validates credentials
        ↓
Issue JWT tokens:
  - Access Token (short-lived, 15 min)
  - Refresh Token (long-lived, 7 days)
        ↓
Tokens stored securely:
  - EncryptedSharedPreferences
  - Master Key from Android Keystore
        ↓
API requests include:
  Authorization: Bearer <accessToken>
        ↓
On 401: Auto-refresh using RefreshToken
```

### Sensitive Operations (Biometric)

```
Sensitive Operation Trigger:
  - View certificate claims
  - Share credential
  - Delete certificate
  - Export data
        ↓
Biometric Prompt shown:
  - Fingerprint OR
  - Face recognition OR
  - Device credential (PIN/Pattern)
        ↓
On Success: Operation proceeds
On Failure: Operation cancelled
        ↓
Audit logged
```

### Cryptographic Signing

```
Issuer:
  Private Key (stored securely, not in app code)
        ↓
  Sign(Certificate JSON) → Signature
        ↓
Certificate + Signature stored in backend
        ↓
Wallet:
  Download Certificate + Signature + Issuer Public Key
        ↓
Verify(Certificate, Signature, PublicKey) → OK/FAIL
        ↓
Wallet signs presentation:
  Wallet Private Key (in Keystore)
        ↓
  Sign(Presentation) → Presentation Signature
        ↓
QR encode: Presentation + Signature
        ↓
Verifier:
  Decode QR → Presentation + Signature
        ↓
Verify(Presentation, Signature, Wallet PublicKey)
        ↓
Verify(Certificate, Signature, Issuer PublicKey)
        ↓
Check Revocation / Expiry / Issuer Status
        ↓
Result: VERIFIED or REJECTED
```

### Replay Protection

```
Each presentation includes:
  - nonce: Fresh random value
  - timestamp: Current time
  - expiresAt: Presentation validity window (5 min)

Verifier checks:
  - nonce hasn't been seen before (in this session)
  - timestamp is recent (within 5 min)
  - current_time < expiresAt

Prevents:
  - Reusing old QR codes
  - Delayed replay attacks

Limitation: 
  - Session-based tracking (not backend-side)
  - In production: server-side nonce registry
```

---

## 9. Data Flow: QR Verification

```
VERIFIER APP
┌────────────────────┐
│  Scanner Screen    │
│  ┌──────────────┐  │
│  │ CameraX      │  │
│  │ QR Detector  │  │
│  └──────┬───────┘  │
│         │          │
│         │ Scan     │
│         │ Success  │
│         ▼          │
│  ┌──────────────┐  │
│  │ Decode QR    │  │
│  └──────┬───────┘  │
│         │          │
│         ▼          │
│  Presentation      │
│  + Signature       │
└────────┬───────────┘
         │
         │ Submit to API
         │
         ▼
BACKEND API
┌────────────────────────────────┐
│  Verification Controller       │
│  POST /api/verification/verify │
│  ┌──────────────────────────┐  │
│  │ VerificationService      │  │
│  │                          │  │
│  │ 1. Verify Wallet Sig     │  │
│  │ 2. Fetch Certificate     │  │
│  │ 3. Get Issuer Key        │  │
│  │ 4. Verify Cert Sig       │  │
│  │ 5. Check Issuer Status   │  │
│  │ 6. Check Expiry          │  │
│  │ 7. Check Revocation      │  │
│  │ 8. Check Replay          │  │
│  │ 9. Build Response        │  │
│  └──────────┬───────────────┘  │
│             │                  │
│             ▼                  │
│  VerificationResponse:         │
│  - status: VERIFIED            │
│  - issuer: ...                 │
│  - subject: ...                │
│  - claims: ...                 │
│  - message: ...                │
└─────────────┬──────────────────┘
              │
              │ Return response
              │
              ▼
VERIFIER APP
┌────────────────────┐
│  Result Screen     │
│  ┌──────────────┐  │
│  │ Status       │  │
│  │ Issuer       │  │
│  │ Subject      │  │
│  │ Claims       │  │
│  │ Verified At  │  │
│  │ Offline?     │  │
│  └──────────────┘  │
│                    │
│ Record event       │
│ Show to user       │
└────────────────────┘
```

---

## 10. Deployment Model

```
LOCAL DEVELOPMENT (Docker Compose)

┌──────────────────────────────────────────────────┐
│  docker-compose.yml                              │
│  ─────────────────────────────────────────────   │
│  ├─ PostgreSQL:5432                              │
│  │  └─ secure_wallet database                    │
│  │                                               │
│  ├─ Issuer Backend:8081                          │
│  │  └─ Flyway migrations (V1__Initial_Schema)   │
│  │  └─ Seed data (sample issuer, certs)         │
│  │                                               │
│  ├─ Verifier Backend:8082                        │
│  │  └─ Same schema, different instance          │
│  │                                               │
│  └─ (Android app connects to localhost)          │
└──────────────────────────────────────────────────┘

Commands:

# Start everything
docker-compose up --build

# Stop
docker-compose down

# View logs
docker-compose logs -f issuer-backend
docker-compose logs -f postgresql

# Access PostgreSQL
docker exec -it postgres psql -U wallet_user -d secure_wallet
```

---

## 11. Key Constraints and Assumptions

### Constraints

1. **Local Trust**: All trust relationships configured locally (no real CAs)
2. **Single Database**: One PostgreSQL instance (not replicated)
3. **Development Keys**: Signing keys in config (not HSM)
4. **Synchronous API**: No event queues or async processing
5. **In-Memory Tokens**: Refresh tokens stored in database (not Redis)
6. **Manual Issuer Setup**: Issuer registry created manually

### Assumptions

1. Devices have network at setup time (to download initial certs)
2. User can set biometric authentication on device
3. Certificate validity is determined by expiresAt timestamp
4. All times in UTC Unix seconds
5. Email is unique identifier for users
6. Issuer names are user-friendly (not cryptographic identifiers)

---

## 12. API Design Principles

1. **Stateless**: Each request independent, state in JWT
2. **Idempotent**: Revoke same cert twice = same result
3. **Versioned**: No breaking changes without version bump
4. **Documented**: Swagger/OpenAPI for all endpoints
5. **Validated**: Request body validation + error messages
6. **Consistent**: Error response format across all endpoints
7. **Audited**: All state-changing operations logged

---

## Conclusion

This HLD provides a blueprint for a production-grade digital credential wallet. The architecture separates concerns (UI, API, Data), uses proven patterns (MVVM, Repository), and prioritizes security (Keystore, Biometric, Signing).

The offline-first design ensures usability even without network, while the comprehensive verification flow protects against various attack vectors.

See [LLD.md](LLD.md) for implementation details and [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) for production scaling considerations.
