# System Architecture Document
## Church Youth Platform

**Document Version:** 1.0  
**Status:** Approved Baseline  
**Target Release:** 27 September 2026  

---

## 1. System Overview & Vision

The **Church Youth Platform** is designed to *“support the rising of giants”* by providing a central digital platform through which church youth activities, programmes, announcements, records, educational resources, and spiritual engagement are managed and accessed. The platform bridges the local assembly with a broader digital and global youth audience while ensuring high resilience, ease of use for first-time members, and strict administrative accountability.

---

## 2. Technology Stack & Component Selection

| Layer | Recommended Technology | Justification |
|:---|:---|:---|
| **Frontend UI** | Jetpack Compose + Material Design 3 (M3) | Declarative UI, state-driven rendering, fluid 60fps animations, adaptive for mobile & tablets, accessibility built-in. |
| **Language & Concurrency** | Kotlin 2.x + Coroutines & Flow | Null safety, reactive data streams, non-blocking asynchronous processing for smooth UI. |
| **Architecture Pattern** | MVVM + Clean Architecture | Clear separation between Presentation (UI/ViewModels), Domain (Use Cases/Entities), and Data (Repositories/DAOs). |
| **Local Database & Cache** | Android Room ORM (SQLite) | ACID compliance, compile-time query checks, reactive Flow observation, reliable offline operation (NFR-002, NFR-014). |
| **Authentication & RBAC** | Multi-Factor Credential Engine + Google Identity | Supports Email/Phone, salt-hashed credentials, role-based capability gating across 9 stakeholder tiers. |
| **Networking & API** | Retrofit 2 + Moshi + OkHttp | Industry standard for REST communication, typed JSON parsing, automatic token interception, and offline caching interceptors. |
| **Security & Privacy** | Android Keystore / PBKDF2/SHA-256 Hashing | Credential salting, secure token storage, zero local card data storage (NFR-005, NFR-006, NFR-007). |
| **Audit & Logging** | Immutable Local Audit Trail (`AuditLogEntity`) | Traceable records for administrative, financial, and membership state changes (NFR-015). |
| **Testing Strategy** | JUnit 4 + Robolectric + Compose UI Test | Fast JVM-based testing of Critical User Journeys (CUJs), database operations, and ViewModel state transitions without requiring physical devices. |

---

## 3. Layered Architectural Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                              │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │   Jetpack Compose UI (Material 3 Dynamic Theming & Components)   │  │
│  │   - Programme Dashboard      - Announcements & Alerts            │  │
│  │   - Profiles & Onboarding    - Financial Records & Giving        │  │
│  │   - Youth Games & Quizzes    - Weekly Reports Hub                │  │
│  │   - Departments & Groups     - Attendance Tracker                │  │
│  │   - Spiritual Content Hub    - Executive & Admin Console         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                 ▲                                      │
│                                 │ (StateFlow / UI Events)              │
│                                 ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                         VIEWMODELS                               │  │
│  │   - AuthViewModel            - DashboardViewModel                │  │
│  │   - EventViewModel           - AnnouncementViewModel             │  │
│  │   - FinanceViewModel         - GamesViewModel                    │  │
│  │   - ReportsViewModel         - AdminViewModel                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │   - Domain Models (User, Event, Announcement, Finance, Game)     │  │
│  │   - RBAC & Permission Evaluator                                  │  │
│  │   - Input Validation & Business Rules                            │  │
│  │   - Use Cases (RegisterEvent, RecordGiving, SubmitReport)        │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            DATA LAYER                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │   Repository Implementations (Offline-First Single Source of Truth)│
│  │   - UserRepository           - EventRepository                   │  │
│  │   - AnnouncementRepository   - FinanceRepository                 │  │
│  │   - GamesRepository          - ReportsRepository                 │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                  │                                    │                │
│                  ▼                                    ▼                │
│  ┌──────────────────────────────┐    ┌──────────────────────────────┐  │
│  │    LOCAL PERSISTENCE (ROOM)  │    │     REMOTE DATA SOURCE       │  │
│  │   - SQLite via Room DB       │    │   - Retrofit / REST API      │  │
│  │   - Reactive DAOs (Flow)     │    │   - Network Status Monitor   │  │
│  │   - Security & Audit Tables  │    │   - Sync Worker & Fallback   │  │
│  └──────────────────────────────┘    └──────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Role-Based Access Control (RBAC) Architecture (4 Canonical User Types)

The system streamlines user governance strictly into 4 canonical user types with tailored screen views:

| Role | Display Name | Core Purpose | Tailored Screen & Permission View |
|:---|:---|:---|:---|
| **ADMINISTRATOR** | **Admin** | System Governance & Full Oversight | **Full System Command Centre:** Real-time KPI summaries, universal search, immutable audit log inspector, CSV data exports (Finances, Attendance, Roster), emergency SMS broadcast, ChMS sync. Floating Action Button (+) to create/manage programmes, User Approval Console to approve pending registrations, full unit management, forum moderation. |
| **YOUTH_EXECUTIVE** | **Youth Executive** | Ministry Coordination & Leadership | **Executive Command Centre:** Operational KPIs, universal search, ministry data exports. Event publishing FAB (+), Weekly Unit/Cell Reports submission & review, service/rehearsal attendance recording (headcounts by gender & first-timers), unit roster coordination, community moderation. |
| **YOUTH_MEMBER** | **Member** | Active Youth Member (Kingdom Giant) | **Clean Member Interface:** 2-tab Profile (My Profile + Giving & Pledges; Command Centre hidden). Upcoming programme schedule with 1-tap RSVP/Registration; assigned Department & Cell information with meeting schedules and leader contacts; personal QR Check-In Pass; active Youth Forum discussions & confidential Pastoral Contact; interactive Bible Trivia game with live Global Leaderboards; personal tithes, offerings, pledges, bank details & receipts. |
| **GUEST** | **Guest** | First-Timer / Visitor / Prospective Member | **Welcoming Hospitality Experience:** Prominent Guest Welcome Banners on Programmes, Units, and Games. 1-tap "Upgrade to Full Member" card on Profile to join the family immediately. Public programme viewing & visitor RSVP; Units exploration guide (Choir, Media, Ushering, Drama, Prayer, Sports) with ministry interest inquiry; confidential prayer request channel; interactive Bible Trivia quizzes. Administrative and leadership tools completely hidden. |

---

## 5. Core Data Entities

### 5.1 User & Identity
- `UserEntity`: `id`, `fullName`, `email`, `phone`, `passwordHash`, `salt`, `role`, `status` (PENDING, APPROVED, SUSPENDED), `dateOfBirth`, `gender`, `address`, `department`, `fellowshipGroup`, `profilePhotoUri`, `emergencyContactName`, `emergencyContactPhone`, `createdAt`, `updatedAt`.
- `SessionEntity`: `sessionId`, `userId`, `token`, `expiresAt`, `lastActiveTimestamp`.
- `AuditLogEntity`: `id`, `timestamp`, `userId`, `actionType`, `targetEntity`, `detailsJson`, `ipOrDeviceId`.

### 5.2 Programmes & Events (MVP Pillar 1)
- `EventEntity`: `id`, `title`, `description`, `dateTimeMillis`, `venue`, `speaker`, `posterUri`, `contactPerson`, `contactPhone`, `isRegistrationEnabled`, `participantLimit`, `registeredCount`, `category` (YOUTH_SERVICE, WORKSHOP, RETREAT, SOCIAL, OUTREACH), `status` (UPCOMING, ONGOING, COMPLETED, CANCELLED), `createdById`.
- `EventRegistrationEntity`: `id`, `eventId`, `userId`, `registeredAt`, `attendanceStatus` (REGISTERED, ATTENDED, CANCELLED).

### 5.3 Announcements & Broadcasts (MVP Pillar 2)
- `AnnouncementEntity`: `id`, `title`, `content`, `publishedAt`, `expiresAt`, `authorId`, `authorName`, `priority` (NORMAL, IMPORTANT, URGENT), `targetAudience` (ALL, EXECUTIVES, DEPARTMENT_SPECIFIC, FELLOWSHIP_SPECIFIC), `targetGroupId`, `readCount`.

### 5.4 Financial Commitments & Records (MVP Pillar 3)
- `FinancialRecordEntity`: `id`, `userId`, `type` (TITHE, OFFERING, PLEDGE, SPECIAL_PROJECT, DUES, DONATION), `amount`, `currency`, `dateMillis`, `status` (PLEDGED, COMPLETED, PENDING_VERIFICATION), `paymentMethod` (BANK_TRANSFER, CARD, CASH, USSD), `referenceCode`, `notes`, `recordedById`.

### 5.5 Games & Weekly Reports (MVP Pillar 5)
- `GameQuizEntity`: `id`, `title`, `category` (BIBLE_TRIVIA, SCRIPTURE_MEMORY, DOCTRINE, CHURCH_HISTORY), `questionsCount`, `difficultyLevel` (EASY, MEDIUM, HARD), `timeLimitSeconds`.
- `GameScoreEntity`: `id`, `quizId`, `userId`, `userName`, `score`, `totalPossible`, `timeSpentSeconds`, `timestamp`.
- `WeeklyReportEntity`: `id`, `reportingUnit` (MEDIA, CHOIR, DRAMA, USHERING, PRAYER, SPORTS, CELL_GROUP, GENERAL_EXECUTIVE), `submittedById`, `weekStartDateMillis`, `weekEndDateMillis`, `attendanceCount`, `activitiesSummary`, `achievements`, `challenges`, `nextWeekPlan`, `submittedAt`, `status` (SUBMITTED, REVIEWED, ARCHIVED).

---

## 6. Offline-First Synchronization Architecture

1. **Local Reads**: UI queries Room Database via Kotlin `Flow`, providing immediate, zero-latency rendering.
2. **Local Writes**: User actions write to Room immediately. Background coroutines queue sync operations.
3. **Connectivity Detection**: `ConnectivityManager` monitors network state. When online, pending remote sync queues dispatch seamlessly.
4. **Data Integrity**: Database transactions wrap multi-table modifications (e.g., event registration incrementing `registeredCount` and adding `EventRegistrationEntity`).

---

## 7. Security & Non-Functional Specifications

- **Credentials:** Salted SHA-256 with user-specific cryptographic salt; plain passwords never stored.
- **Data Protection:** Personal contact data and financial entries visible only to data owners, authorized leaders, or system administrators.
- **Financial Compliance:** Payment reference verification; no credit/debit card numbers or CVVs stored in the database.
- **Touch Targets & Layouts:** M3 compliant 48dp minimum targets; edge-to-edge system insets handling; scalable fonts.
- **Multilingual Readiness:** String externalization for English, Yoruba, Hausa, and Igbo.
