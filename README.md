# Secure Digital Certificate Wallet

A production-style Android portfolio project demonstrating secure digital certificate management, cryptographic verification, and privacy-preserving credential sharing.

**Status**: Portfolio/Sandbox Implementation
**Purpose**: Demonstrates advanced Android development, cryptography, and digital identity concepts for a Certificate Authority role.

> **Important**: This is a local sandbox implementation. It does NOT integrate with real government systems, VIDA services, or production certificate authorities. All trust relationships are local and for demonstration purposes only.

---

## Table of Contents

- [Overview](#overview)
- [Business Problem](#business-problem)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Repository Structure](#repository-structure)
- [End-to-End Flow](#end-to-end-flow)
- [Local Setup](#local-setup)
- [Docker Setup](#docker-setup)
- [Project Components](#project-components)
- [Certificate Lifecycle](#certificate-lifecycle)
- [Security Architecture](#security-architecture)
- [Database Design](#database-design)
- [Testing](#testing)
- [Known Limitations](#known-limitations)
- [Production Evolution](#production-evolution)
- [Interview Guide](#interview-guide)

---

## Overview

**Secure Digital Certificate Wallet** is an Android application that enables users to:

1. **Store** digitally signed credentials issued by trusted organizations
2. **View** detailed certificate information with cryptographic verification
3. **Share** certificates selectively—users choose which claims to reveal
4. **Verify** incoming credentials through QR code scanning and signature validation
5. **Revoke** credentials when they expire or become invalid
6. **Work Offline** with previously synchronized credential data

The system uses **asymmetric cryptography** (Ed25519 signatures) to ensure credential integrity, **Android Keystore** for secure local key storage, and **BiometricPrompt** for access control.

This is a serious portfolio project demonstrating:

- Advanced Android architecture (MVVM + Clean Architecture)
- Kotlin + Jetpack Compose UI framework
- Cryptographic signing and verification
- Secure credential management
- Offline-first architecture
- RESTful API design
- Database design and migrations
- CI/CD pipelines
- Comprehensive documentation

---

## Business Problem

### Current State

People receive important certificates from universities, employers, training organizations, government agencies, and professional bodies. Traditional certificates are:

- **Difficult to verify** – No reliable way to check authenticity
- **Easy to forge** – Simple document copies can be faked
- **Hard to manage** – Scattered across email and cloud storage
- **Impossible to selectively share** – Must share entire document or nothing
- **Difficult to revoke** – No mechanism for issuers to invalidate outdated credentials

### Solution

**Secure Digital Certificate Wallet** enables a distributed trust model where:

1. **Issuer** (University, Employer, etc.) creates a digitally signed certificate
2. **Wallet Holder** (Student, Employee) receives and stores the credential securely
3. **Verifier** (Employer, Regulator) scans the QR code and verifies authenticity

---

## Key Features

### For Wallet Holders

✅ **Secure Storage**
- Certificates stored in encrypted local database (Room)
- Sensitive data protected by Android Keystore
- Biometric authentication for sensitive operations

✅ **Certificate Management**
- View all stored certificates
- Filter by status (Active, Expired, Revoked, Suspended)
- Search by issuer or credential type
- Monitor expiry with notifications

✅ **Selective Disclosure**
- Choose which claims to share (e.g., Name only vs. Full Certificate)
- Minimal disclosure by default (privacy-first)
- Generate time-bound presentation tokens

✅ **QR Credential Sharing**
- Generate signed QR codes containing cryptographic proof
- Credentials can be verified without network access
- QR payload includes nonce, timestamp, and digital signature

✅ **Offline Capability**
- View previously synchronized certificates offline
- Local verification history
- Clear indication of offline vs. online verification status
- Automatic sync when network returns

### For Verifiers

✅ **QR Scanning**
- Camera-based QR code scanning (CameraX)
- Real-time scan results

✅ **Comprehensive Verification**
- Validate signature authenticity
- Check issuer trust status
- Verify expiration
- Check revocation status
- Display clear verification result

✅ **Verification History**
- Track all scanned credentials
- Timestamp and offline/online indicator
- Result summary (Valid, Expired, Revoked, Unknown Issuer, etc.)

### For Issuers

✅ **Certificate Creation**
- Define credential type (Degree, Employment, Training, License, etc.)
- Set subject, claims, validity period
- Review before signing
- Cryptographically sign with issuer key

✅ **Revocation**
- Revoke certificates at any time
- Mark certificate with revocation reason
- Affects wallet holder's view and verifier's results

✅ **Issuer Registry**
- Each issuer has a public key for verification
- Issued/suspended/revoked status
- Wallet trusts configured issuers

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    System Context                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐         ┌──────────────┐               │
│  │   ISSUER     │         │  WALLET      │               │
│  │              │◄───────►│              │               │
│  │  - Create    │ Issue   │  - Store     │               │
│  │  - Sign      │ Certs   │  - View      │               │
│  │  - Revoke    │         │  - Share     │               │
│  └──────┬───────┘         └────┬─────────┘               │
│         │                       │                         │
│         │                       │                         │
│         ▼                       ▼                         │
│   ┌─────────────────────────────────────┐               │
│   │     BACKEND API                     │               │
│   │  (Spring Boot + PostgreSQL)         │               │
│   │                                     │               │
│   │  - Cert Management                  │               │
│   │  - Verification                     │               │
│   │  - Revocation                       │               │
│   │  - Audit Logging                    │               │
│   └──────────────────┬──────────────────┘               │
│                      │                                   │
│                      │  Scan QR                         │
│                      │                                   │
│                  ┌───▼───────┐                          │
│                  │ VERIFIER   │                          │
│                  │            │                          │
│                  │ - Scan QR  │                          │
│                  │ - Verify   │                          │
│                  │ - Display  │                          │
│                  └────────────┘                          │
│                                                          │
└─────────────────────────────────────────────────────────────┘
```

### High-Level Component Diagram

```
ANDROID WALLET
│
├─ Authentication
│  ├─ LoginViewModel
│  ├─ AuthRepository
│  └─ AuthService (Retrofit)
│
├─ Wallet Management
│  ├─ WalletViewModel
│  ├─ CertificateRepository (Room + Remote)
│  ├─ CertificateUseCase
│  └─ CameraX QR Scanner
│
├─ Certificate Operations
│  ├─ CertificateDetailViewModel
│  ├─ SignatureVerificationService
│  ├─ CertificateSigningService
│  └─ RevocationService
│
├─ Security
│  ├─ Android Keystore Manager
│  ├─ BiometricPrompt Manager
│  ├─ EncryptionManager
│  └─ TokenStorage (EncryptedSharedPreferences)
│
├─ Local Storage
│  ├─ Room Database
│  │  ├─ CertificateEntity
│  │  ├─ VerificationEventEntity
│  │  └─ AuditLogEntity
│  └─ DataStore (Preferences)
│
└─ UI Layer (Jetpack Compose)
   ├─ HomeScreen
   ├─ WalletScreen
   ├─ CertificateDetailScreen
   ├─ ScannerScreen
   ├─ VerificationResultScreen
   └─ ProfileScreen
```

---

## Technology Stack

### Android

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Hilt |
| Async | Kotlin Coroutines + Flow/StateFlow |
| Networking | Retrofit 2 + OkHttp |
| Local Storage | Room + DataStore |
| Security | Android Keystore + BiometricPrompt |
| Camera | CameraX |
| QR Codes | ZXing |
| Navigation | Jetpack Compose Navigation |
| Background Tasks | WorkManager |
| Testing | JUnit + MockWebServer + Compose UI Tests |
| Build | Gradle Kotlin DSL |

### Backend

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| Framework | Spring Boot 3 |
| API | Spring Web + Spring Security |
| Authentication | JWT (Access + Refresh Tokens) |
| Database | PostgreSQL |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| API Documentation | Springdoc OpenAPI (Swagger) |
| Cryptography | Bouncy Castle + Spring Security Crypto |
| Testing | JUnit + Spring Test |
| Build | Gradle Kotlin DSL |

### Infrastructure

| Category | Technology |
|----------|------------|
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| VCS | Git + GitHub |

---

## Repository Structure

```
/
├── README.md                              # This file
├── .gitignore                             # Git ignore rules
├── docker-compose.yml                     # Local development stack
├── .env.example                           # Environment variables template
│
├── android/
│   ├── app/                               # Main application module
│   ├── core/                              # Shared core utilities
│   ├── core-security/                     # Keystore, Biometric, Encryption
│   ├── core-network/                      # Retrofit, OkHttp, API clients
│   ├── core-database/                     # Room, DAOs, entities
│   ├── core-ui/                           # Composable components, themes
│   ├── feature-auth/                      # Login/Register screens
│   ├── feature-wallet/                    # Wallet home and list
│   ├── feature-certificate/               # Certificate detail
│   ├── feature-scanner/                   # QR scanner
│   ├── feature-verification/              # Verification results and history
│   ├── feature-profile/                   # User profile and settings
│   ├── build.gradle.kts                   # Root build configuration
│   └── settings.gradle.kts                # Module definitions
│
├── issuer-backend/
│   ├── src/main/kotlin/
│   │   └── com/vita/issuer/
│   │       ├── Application.kt
│   │       ├── config/                    # Spring configuration
│   │       ├── controller/                # REST endpoints
│   │       ├── service/                   # Business logic
│   │       ├── repository/                # Data access
│   │       ├── entity/                    # JPA entities
│   │       ├── dto/                       # Request/Response DTOs
│   │       ├── security/                  # Signing, crypto
│   │       ├── exception/                 # Custom exceptions
│   │       └── audit/                     # Audit logging
│   ├── src/test/kotlin/                   # Unit and integration tests
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/                  # Flyway migrations
│   ├── build.gradle.kts
│   └── Dockerfile
│
├── verifier-backend/
│   ├── src/main/kotlin/
│   │   └── com/vita/verifier/
│   │       ├── Application.kt
│   │       ├── config/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── entity/
│   │       ├── dto/
│   │       ├── security/
│   │       ├── exception/
│   │       └── audit/
│   ├── src/test/kotlin/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   ├── build.gradle.kts
│   └── Dockerfile
│
├── docs/
│   ├── HLD.md                             # High-Level Design
│   ├── LLD.md                             # Low-Level Design
│   ├── SYSTEM_DESIGN.md                   # Scalability and production evolution
│   ├── THREAT_MODEL.md                    # Security analysis (STRIDE)
│   ├── BUSINESS_RULES.md                  # Domain constraints
│   ├── DATABASE.md                        # Schema and ER diagram
│   ├── API.md                             # API reference
│   ├── SECURITY.md                        # Security architecture
│   ├── ADR.md                             # Architecture Decision Records
│   ├── TESTING.md                         # Test strategy and coverage
│   ├── PERFORMANCE.md                     # Performance metrics
│   └── SCREENSHOTS/                       # UI screenshots (if available)
│
├── .github/workflows/
│   ├── android.yml                        # Android CI/CD
│   └── backend.yml                        # Backend CI/CD
│
└── .env.example                           # Environment template
```

---

## End-to-End Flow

### 1. Certificate Issuance

```
┌─────────────────────────────────────────────────────────────┐
│  ISSUER CREATES CERTIFICATE                                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Login to Issuer Portal                                  │
│  2. Click "Issue Certificate"                               │
│  3. Fill details:                                            │
│     - Recipient Name                                        │
│     - Credential Type (Degree, Training, etc.)              │
│     - Claims (Institute, Date of Birth, etc.)               │
│     - Validity (Valid From, Expires At)                     │
│  4. Review Certificate                                      │
│  5. Sign with Issuer Private Key (Ed25519)                  │
│  6. Store in Database                                       │
│                                                              │
│  Result: Signed Certificate                                 │
│          certificateId: "CERT-ABC123"                       │
│          signature: "0x1a2b3c..." (Ed25519)                 │
│          status: ACTIVE                                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 2. Certificate Import into Wallet

```
┌─────────────────────────────────────────────────────────────┐
│  WALLET HOLDER IMPORTS CERTIFICATE                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Open Wallet App                                         │
│  2. Login                                                   │
│  3. Tap "Add Certificate"                                   │
│  4. Enter Credential ID (CERT-ABC123)                       │
│  5. Wallet fetches from Backend API                         │
│  6. Verify Signature:                                       │
│     - Fetch Issuer Public Key                               │
│     - Verify certificate signature                          │
│     - Verify issuer is trusted                              │
│  7. Check expiry and status                                 │
│  8. Store in Room (encrypted)                               │
│  9. Backup key metadata in Keystore                         │
│                                                              │
│  Result: Certificate cached locally                         │
│          Available for sharing even offline                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 3. Selective Disclosure & QR Sharing

```
┌─────────────────────────────────────────────────────────────┐
│  WALLET HOLDER SHARES CERTIFICATE                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. View Certificate Detail                                 │
│  2. Tap "Share"                                             │
│  3. Select Claims to Share:                                 │
│     Option A: Full Disclosure                               │
│       - Name, DOB, Institution, Degree, Dates               │
│     Option B: Minimal Disclosure                            │
│       - Name, Degree only                                   │
│     Option C: Proof Only                                    │
│       - "Age Over 18: Yes"                                  │
│  4. Generate Presentation:                                  │
│     {                                                       │
│       "credentialId": "CERT-ABC123",                        │
│       "issuer": "VIT",                                      │
│       "subject": "John Doe",                                │
│       "claims": {"degree": "B.Tech", ...},                  │
│       "issuedAt": 1696000000,                               │
│       "expiresAt": 1727536000,                              │
│       "nonce": "rand123",                                   │
│       "timestamp": 1696100000                               │
│     }                                                       │
│  5. Sign with Wallet Private Key (Ed25519)                  │
│  6. Encode to QR Code (with signature)                      │
│  7. Display QR                                              │
│                                                              │
│  Result: QR contains signed presentation                    │
│          Can be verified without network                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4. QR Verification

```
┌─────────────────────────────────────────────────────────────┐
│  VERIFIER SCANS AND VALIDATES QR                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Open Verifier App (Scanner Screen)                      │
│  2. Point camera at QR code                                 │
│  3. Decode QR payload                                       │
│  4. Verify Wallet Holder Signature:                         │
│     - Extract public key                                    │
│     - Verify presentation signature                         │
│  5. Fetch Certificate from Backend:                         │
│     - credentialId: "CERT-ABC123"                           │
│  6. Fetch Issuer from Registry:                             │
│     - Get issuer public key                                 │
│     - Check issuer is trusted                               │
│  7. Verify Certificate Signature:                           │
│     - Verify issuer signature on certificate                │
│  8. Check Certificate Status:                               │
│     - NOT revoked                                           │
│     - NOT suspended                                         │
│     - NOT expired                                           │
│  9. Check Replay:                                           │
│     - Nonce is fresh                                        │
│     - Timestamp is recent                                   │
│                                                              │
│  Result: VERIFIED or REJECTED                               │
│          Display:                                           │
│          - Status (Valid/Expired/Revoked/Unknown)           │
│          - Issuer details                                   │
│          - Shared claims                                    │
│          - Verification time                                │
│          - Offline status (if applicable)                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Certificate Lifecycle

```
                    ┌──────────┐
                    │ CREATED  │
                    └─────┬────┘
                          │
                    Issue & Sign
                          │
                          ▼
                    ┌──────────┐
                    │ ACTIVE   │◄──────┐
                    └─────┬────┘       │
                          │           │
          ┌───────────────┼───────────┴──────┐
          │               │                  │
       Revoke          Expire          Suspend
          │               │                  │
          ▼               ▼                  ▼
      ┌────────┐     ┌────────┐        ┌──────────┐
      │REVOKED │     │EXPIRED │        │SUSPENDED │
      └────────┘     └────────┘        └──────────┘
          │               │                  │
          └───────────────┴──────────────────┘
                          │
                   Cannot Present as Valid
                          
Status Rules:
- ACTIVE: Valid for presentation, not expired, not revoked
- EXPIRED: Current date >= expiresAt
- REVOKED: Issuer explicitly revoked
- SUSPENDED: Issuer suspended (temporary hold)
```

---

## Security Architecture

### Local Security (Android)

#### 1. Android Keystore

- **Private Keys**: Stored in Keystore, never exposed to app code
- **Keys Used For**: 
  - Wallet presentation signing (Ed25519)
  - Token encryption/decryption
- **Protected By**: Device lock (PIN, pattern, biometric)

#### 2. Biometric Authentication

- **Trigger**: Sensitive operations (share certificate, delete credential, view claims)
- **Protection**: BiometricPrompt + Device Credential fallback
- **Never Stored**: Raw biometric data never stored in app

#### 3. Encrypted Local Storage

- **Room Database**: All entities encrypted using SQLCipher
- **Tokens**: Stored in EncryptedSharedPreferences
- **Sensitive Fields**: Certificate claims, issuer keys

### API Security

#### 1. JWT Authentication

- **Access Token**: Short-lived (15 min)
- **Refresh Token**: Long-lived (7 days), stored securely
- **Token Refresh**: Automatic on 401

#### 2. HTTPS + Certificate Pinning

- **TLS 1.2+**: All API communication
- **Certificate Pinning**: Pin issuer API certificate in OkHttp

### Cryptographic Signing

#### 1. Certificate Signing

- **Algorithm**: Ed25519 (asymmetric)
- **Payload**: Canonical JSON representation
- **Signature**: Deterministic and verifiable

#### 2. QR Presentation Signing

- **Payload**: Encoded in QR (base64)
- **Signature**: Included in QR
- **Nonce**: Fresh random value prevents replay

---

## Database Design

### Entity Relationship Diagram

```
┌────────────────────────────────────────────────────────────┐
│                     USERS                                  │
├────────────────────────────────────────────────────────────┤
│ userId (PK)       │ email │ name │ passwordHash           │
│ createdAt         │ updatedAt                              │
└────────────────────────────────────────────────────────────┘
           │
           ▼
┌────────────────────────────────────────────────────────────┐
│                  CERTIFICATES                              │
├────────────────────────────────────────────────────────────┤
│ certificateId (PK)│ issuerId (FK) │ subjectId (FK)        │
│ credentialType    │ issuedAt      │ expiresAt             │
│ signature         │ status        │ createdAt             │
│ updatedAt                                                  │
└────────────────────────────────────────────────────────────┘
           │
           ├───────────────────────┐
           │                       │
           ▼                       ▼
┌──────────────────────┐  ┌──────────────────────────┐
│  CERTIFICATE_CLAIMS  │  │   CERTIFICATE_EVENTS     │
├──────────────────────┤  ├──────────────────────────┤
│ claimId (PK)         │  │ eventId (PK)             │
│ certificateId (FK)   │  │ certificateId (FK)       │
│ key                  │  │ eventType                │
│ value                │  │ timestamp                │
└──────────────────────┘  └──────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                     ISSUERS                                │
├────────────────────────────────────────────────────────────┤
│ issuerId (PK)     │ name │ publicKey │ status             │
│ createdAt         │ updatedAt                              │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                   REVOCATIONS                              │
├────────────────────────────────────────────────────────────┤
│ revocationId (PK) │ certificateId (FK)                     │
│ reason            │ revokedAt │ revokedBy                  │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│              VERIFICATION_EVENTS                           │
├────────────────────────────────────────────────────────────┤
│ eventId (PK)      │ userId (FK)      │ certificateId       │
│ result            │ reason           │ isOffline           │
│ timestamp                                                  │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                  AUDIT_LOGS                                │
├────────────────────────────────────────────────────────────┤
│ logId (PK)        │ userId (FK)      │ action              │
│ resourceType      │ resourceId       │ timestamp           │
│ details                                                    │
└────────────────────────────────────────────────────────────┘
```

---

## Testing

### Android

- **Unit Tests**: Certificate validity logic, expiry calculation, signature verification
- **Integration Tests**: Repository behavior, Room operations, API mocking
- **UI Tests**: Compose component rendering, navigation flows, state management
- **MockWebServer**: HTTP request/response testing

### Backend

- **Unit Tests**: Service logic, JWT validation, revocation status
- **Integration Tests**: Database operations, API endpoints, transaction handling
- **Test Data**: Seed certificates in different states (active, expired, revoked)

### CI/CD

- **GitHub Actions**: Automated build, test, lint on every push
- **Android**: Gradle build + unit tests + lint checks
- **Backend**: Gradle build + unit tests + integration tests

---

## Known Limitations

### Current Portfolio Implementation

1. **Local Trust Only**: All trust relationships are configured locally. No real issuer validation.
2. **Development Keys**: Signing keys are managed via application configuration, not HSM.
3. **Simplified Revocation**: Revocation status is checked at verification time, not using a credential status list.
4. **No Credential Status List (CSL)**: Production would use OCSP or CRL for real-time revocation.
5. **Simplified Replay Protection**: Uses nonce and timestamp; production would use server-side challenge-response.
6. **No Selective Disclosure Format**: This is a UI simulation, not implementing W3C Verifiable Credentials or SD-JWT.
7. **Single Database**: One PostgreSQL instance; production would have replicas.
8. **No Key Rotation**: HSM-backed keys with rotation not implemented.
9. **No Fraud Detection**: No ML-based anomaly detection for verification patterns.
10. **Offline Limitations**: Revocation status stale when offline; clearly communicated in UI.

---

## Production Evolution

To scale this to production:

### Infrastructure

- **API Gateway**: Rate limiting, request validation, routing
- **Load Balancers**: Distribute traffic across servers
- **Database Replicas**: Read replicas for verification queries
- **Caching**: Redis for issuer registry, revocation status, user sessions
- **Message Queues**: RabbitMQ for async certificate events

### Security

- **HSM/KMS**: CloudHSM or AWS KMS for private key management
- **Key Rotation**: Automated key rollover with versioning
- **Certificate Pinning**: Use SPKI pinning in production
- **WAF**: Web Application Firewall for API protection
- **2FA**: Multi-factor authentication for issuer accounts

### Credential Format

- **W3C Verifiable Credentials**: Standard VC data model
- **SD-JWT**: Structured JSON with selective disclosure
- **OIDC Federation**: OpenID Connect for trust between issuers

### Operations

- **Distributed Tracing**: OpenTelemetry for debugging
- **Centralized Logging**: ELK stack or CloudWatch
- **Metrics**: Prometheus for performance monitoring
- **Alerting**: PagerDuty for incident response
- **Disaster Recovery**: Multi-region backup and failover

---

## Interview Guide

### Technical Questions

**Cryptography**
1. How do you ensure certificate authenticity?
2. Why Ed25519 instead of RSA?
3. How do you prevent QR code tampering?
4. What is a nonce and why is it important?
5. How would you implement key rotation?

**Android Architecture**
1. Why MVVM + Clean Architecture?
2. How do you manage state in Compose?
3. What is the repository pattern and why use it?
4. How do you handle API errors consistently?
5. How do you test a ViewModel?

**Security**
1. How does Android Keystore protect keys?
2. Why use BiometricPrompt instead of passwords?
3. How do you store JWT tokens securely?
4. How would you prevent token theft?
5. What is certificate pinning and why do it?

**Offline Design**
1. How do you sync certificates offline-first?
2. How do you handle conflicts?
3. When revocation is offline, how do you handle it?
4. What is sync state and why track it?
5. How do you indicate offline verification?

**Database**
1. How do you design the certificate table?
2. Why use foreign keys?
3. How do you ensure data consistency?
4. What indexes are critical for performance?
5. How do you handle certificate lifecycle in DB?

**API Design**
1. How do you version your API?
2. How do you handle backward compatibility?
3. Why use DTOs instead of entities?
4. How do you document APIs?
5. How do you handle rate limiting?

**Testing**
1. How do you test cryptographic operations?
2. How do you mock network calls?
3. How do you test Compose UI?
4. What makes a good unit test?
5. How do you test async operations?

---

## Local Setup

### Prerequisites

- macOS 12+ or Linux
- Android Studio Flamingo+
- Android SDK 31+
- JDK 17+
- Docker + Docker Compose
- Git

### 1. Clone Repository

```bash
git clone https://github.com/SriChaitanya1824/Secure-Digital-Certificate-Wallet.git
cd Secure-Digital-Certificate-Wallet
```

### 2. Environment Setup

```bash
cp .env.example .env
# Edit .env with your configuration
```

### 3. Start Backend Services

```bash
docker-compose up -d postgresql
# Wait for PostgreSQL to be ready
sleep 10
```

### 4. Build and Run Backend

```bash
cd issuer-backend
./gradlew build
./gradlew bootRun

# In another terminal:
cd verifier-backend
./gradlew build
./gradlew bootRun
```

### 5. Build Android App

```bash
cd android
./gradlew build
```

### 6. Run Tests

```bash
# Backend
cd issuer-backend
./gradlew test

# Android
cd ../android
./gradlew test
```

### 7. Open in Android Studio

```bash
# Open the android/ directory in Android Studio
open -a "Android Studio" android/
```

---

## Docker Setup

### Start Full Stack

```bash
docker-compose up --build
```

This starts:
- PostgreSQL (localhost:5432)
- Issuer Backend (localhost:8081)
- Verifier Backend (localhost:8082)

### Stop Services

```bash
docker-compose down
```

### View Logs

```bash
docker-compose logs -f issuer-backend
docker-compose logs -f verifier-backend
docker-compose logs -f postgresql
```

---

## Environment Variables

See `.env.example` for all configuration options.

Key variables:

```
# Database
POSTGRES_DB=secure_wallet
POSTGRES_USER=wallet_user
POSTGRES_PASSWORD=secure_password

# Backend
ISSUER_API_PORT=8081
VERIFIER_API_PORT=8082
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION_MS=900000

# Android
API_BASE_URL=http://localhost:8081
VERIFY_API_BASE_URL=http://localhost:8082
```

---

## Components Overview

### Authentication Module (`feature-auth`)
- Login/Register screens
- Session management
- JWT token handling
- Logout flow

### Wallet Module (`feature-wallet`)
- Home screen with statistics
- Certificate list with filtering
- Synchronization status
- Offline indicator

### Certificate Module (`feature-certificate`)
- Certificate detail view
- Claim display
- Issuer information
- Status indicators
- Expiry timeline

### Scanner Module (`feature-scanner`)
- CameraX-based QR scanning
- Real-time barcode detection
- QR payload decoding
- Error handling

### Verification Module (`feature-verification`)
- Verification result display
- Signature validation feedback
- Revocation status
- Verification history
- Offline/online indicator

### Profile Module (`feature-profile`)
- User settings
- Security preferences
- Biometric configuration
- Account management

---

## Known Issues & Workarounds

1. **Camera Permission on Older Android**
   - Works fine on Android 5+
   - Fallback to text input for QR payload (not implemented in this version)

2. **Biometric on Emulator**
   - Requires hardware or special emulator configuration
   - Falls back to device credential on unsupported devices

3. **Offline Certificate Viewing**
   - Only previously synced certificates visible
   - Network status clearly indicated

---

## Contributing

This is a portfolio project. For suggestions or improvements:

1. Document the change
2. Reference business requirements
3. Follow Kotlin conventions
4. Add tests for new features
5. Update relevant documentation

---

## License

This project is for educational and portfolio purposes.

---

## Author

**Chaitanya Sri**
- GitHub: [@SriChaitanya1824](https://github.com/SriChaitanya1824)
- Purpose: Android Developer Portfolio Project
- Target Company: VIDA (Certificate Authority & Digital Identity)

---

## Support & Documentation

- **[HLD](docs/HLD.md)**: High-level system design and architecture
- **[LLD](docs/LLD.md)**: Low-level implementation details
- **[System Design](docs/SYSTEM_DESIGN.md)**: Production-scale considerations
- **[Threat Model](docs/THREAT_MODEL.md)**: Security analysis
- **[API Documentation](docs/API.md)**: Complete API reference
- **[Database Design](docs/DATABASE.md)**: Schema and relationships
- **[Security Architecture](docs/SECURITY.md)**: Encryption and protection mechanisms
- **[Testing Strategy](docs/TESTING.md)**: Test coverage and approach
- **[Architecture Decision Records](docs/ADR.md)**: Design decisions and rationale

---

**Last Updated**: October 2026
**Status**: Actively maintained portfolio project
# Secure-Digital-Certificate-Wallet
