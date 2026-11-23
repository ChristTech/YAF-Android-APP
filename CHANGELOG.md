# Changelog
All notable changes to the Youth Alive Fellowship | Kingdom Giants project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.5.0] - 2026-10-25 — Local Bible Database, 3-Mode Multiplayer Game Engine & Navigation Architecture Cleanup

### Added
- **Local Bible Database & Device File Uploader (`BibleVerseEntity`, `BibleDao`, `BibleRepository`):**
  - Pre-populated local SQLite `bible_verses` table (Room DB v6) with 25+ Living Faith / Youth Alive scriptures across KJV, NKJV, AMP, and NLT translations with thematic filters.
  - Added `BibleScripturePickerDialog` and `UploadBibleDatabaseDialog` with Android native file picker (`ActivityResultContracts.GetContent`) supporting `.json`, `.txt`, and `.csv` Bible dataset files from device storage, bulk JSON/pipe-delimited paste, and single-verse entry.
  - Integrated **Select from Bible Database** directly into `CreateMultiplayerGameDialog`, `CreateQuizDialog`, and the Games Hub (pre-populating a Scripture Scramble question when a verse is selected from the Games Hub).
  - Created `BibleReaderScreen.kt` (**In-App Bible Explorer**) mounted as Sub-tab 1 in the **Spiritual** section (`SpiritualHubScreen`).
- **Complete 3-Mode Multiplayer Game Engine (`MultiplayerGameScreen`, `BibleGamesTabContent`, `EngagementViewModel`):**
  - Added **3-Game-Mode Instant Arena Launcher** (`game_modes_showcase_section`) on the Games Hub for 1-tap entry into **Trivia Challenge**, **Who Am I?** (progressive 3-clue character mystery), and **Scripture Scramble** (word-chip verse reconstruction).
  - Added Admin/Executive **"Cancel"** and **"End Game"** lifecycle buttons on scheduled and active multiplayer session cards.
  - Added **"Recent Multiplayer Showdowns"** (`completedSessions`) history section with a 1-tap **"Podium"** viewer button.
  - Synchronized completed multiplayer session scores into `gamesRepository.submitScore(...)` (`game_scores`) so Member Progress (*Quizzes Taken*, *Personal Best*, *Kingdom Rank*) and the *Youth Global Leaderboard* reflect multiplayer performance.
- **Navigation Section Cleanups (`MainAppScaffold.kt`, `WeeklyReportsTabContent.kt`):**
  - Kept the **Games** section 100% focused on games and leaderboards.
  - Re-homed **Weekly Unit Reports** (`WeeklyReportsScreen`, FR-024) into the **Community** section (`CommunityHubScreen` Sub-tab 2: `"Unit Reports"`).
  - Replaced the duplicate `"Bible Trivia & Reports"` sub-tab in `SpiritualHubScreen` with the new `"Bible Explorer"` (`BibleReaderScreen`).

---

## [1.4.0] - 2026-10-14 — Offline Leadership Notifications & Live Alerts System

### Added
- **Dual-Channel Offline Notification Engine (ADR-014):**
  - **Android System Notifications:** Configured `NotificationChannel("youth_alive_fellowship_channel")` with `IMPORTANCE_HIGH` and declared `POST_NOTIFICATIONS` + `VIBRATE` permissions for heads-up device status bar notifications.
  - **In-App Persistent Notification Center:** Backed by Room SQLite (`NotificationEntity`, `NotificationDao`, `FellowshipNotificationService`) tracking read/unread status, categories, author roles, relative timestamps, and urgency tags.
- **Automated Leadership Triggers:**
  - **Announcements & Broadcasts:** Instant notification whenever an Admin or Youth Executive publishes a fellowship broadcast.
  - **Programmes & Events:** Notification sent whenever a new programme/event is scheduled.
  - **Live Game Time Showdown:** Instant notification with game details when an Admin or Executive triggers "Alert All" for the live showdown.
  - **Weekly Report Reviews:** Feedback notifications when leadership reviews unit reports.
- **TopAppBar Badged Notification Bell & Dialog:**
  - Badged notification bell with real-time unread count indicator (`BadgedBox`).
  - Full-screen `NotificationCenterDialog` with category filtering chips (All, Announcements, Events, Game Time, Report Reviews), "Mark all as read" action, and dismissal.
- **Pre-populated Initial Alerts:** Pre-seeded realistic notifications from both Administrator and Executive leaders so users experience the notification center immediately.

---

## [1.3.0] - 2026-10-14 — Role-Tailored Game Section & Interactive Game Types

### Added
- **Interactive Game Mechanics (FR-023, ADR-012):**
  - **Trivia Challenge (`TRIVIA_CHALLENGE`):** Timed multiple-choice Bible knowledge & Kingdom questions with speed bonus and scripture references.
  - **Who Am I? (`WHO_AM_I`):** Progressive clue-based mystery character identification challenge (David, Esther, Elijah, Paul).
  - **Scripture Scramble (`SCRIPTURE_SCRAMBLE`):** Foundational verse reconstruction puzzle game (Philippians 4:13, Daniel 11:32b, Jeremiah 29:11, Ephesians 6:10).
- **Curriculum Quiz Categories (`QuizCategory`):**
  - Bible Trivia, Scripture Memory, Church History, Leadership & Faith, Youth Life & Wisdom.
- **Role-Tailored Game Section Architecture:**
  - **Admin:** Game Command header; `CreateQuizDialog` for custom quizzes; quiz deletion & score audit controls; weekly report review dashboard with aggregated KPIs.
  - **Youth Executive:** Executive Leadership Challenge banner; unit report submission FAB (`+`) with detailed attendance, activities, and goals.
  - **Member:** Personal Progress card (quizzes completed, personal best score, Kingdom Rank badge, Colossians 3:16 encouragement); personal leaderboard highlighting (`★ YOU`).
  - **Guest:** Welcoming guest card; instant access to play all quizzes; post-quiz call to action to register for permanent leaderboard score tracking.
- **Live Multiplayer Showdown Arena (ADR-013):**
  - **Per-Question Countdown Timer:** 15s timer per question with animated urgency colors (green -> amber -> red) and speed bonuses up to +450 points.
  - **Intermission Round Standings:** Round intermission screen after each question showing user accuracy feedback, point breakdown, and real-time fellowship standings ("who is leading currently") with dynamic rank changes (`▲ +2 positions`, `Holding 1st Place 👑`) and `★ YOU` highlighting.
  - **No Host Code Required:** Barrier-free 1-tap entry into the Live Game Arena with phone game time notification alerts and dynamic participant tracking.
  - **Grand 3-Tier Podium Ceremony:** Final podium celebration displaying Gold 🥇, Silver 🥈, and Bronze 🥉 places with avatar pillars and tailored role actions.
- **Modularized UI Structure:**
  - Decoupled `GamesAndReportsScreen.kt` into `BibleGamesTabContent.kt`, `WeeklyReportsTabContent.kt`, `ActiveQuizRunner.kt`, and `CreateQuizDialog.kt`.

---

## [1.2.0] - 2026-10-07 — 4 Canonical User Personas & Tailored Screen Architecture

### Changed
- **Canonical 4 User Roles:**
  - Standardized user governance and RBAC matrix strictly around 4 canonical user types: **Admin**, **Youth Executive**, **Member**, and **Guest**.
  - Streamlined `UserRole` enum (`ADMINISTRATOR`, `YOUTH_EXECUTIVE`, `YOUTH_MEMBER`, `GUEST`) and associated permissions in `RoleEvaluator.kt`.
- **Role-Tailored Screen Experiences:**
  - **Admin (`ADMINISTRATOR`):** Flame Red badge ("System Administrator • Full Platform Oversight"); full Command Centre (Audit Logs, CSV Data Export, Emergency SMS, ChMS Sync); event creation FAB; User Approval Console in Profile; full unit roster & attendance oversight; forum post moderation.
  - **Youth Executive (`YOUTH_EXECUTIVE`):** Flame Gold badge ("Executive Leader • Ministry Coordination"); tailored Executive Command Centre (Operational KPIs, Universal Search, Ministry Reports); event publishing FAB; Weekly Unit/Cell Reports submission & review; department service attendance recording.
  - **Member (`YOUTH_MEMBER`):** Forest Green badge ("Kingdom Giant • Active Youth Member"); clean 2-tab Profile (My Profile + Giving & Pledges; Command Centre omitted); 1-tap event RSVP/Registration; assigned unit details & QR Check-In Pass; active forum discussion & confidential pastoral care; interactive Bible Trivia game with global leaderboard.
  - **Guest (`GUEST`):** Slate Blue badge ("Welcome to Youth Alive Fellowship!"); Guest Welcome Banners on Programmes, Units, and Games; 1-tap "Upgrade to Full Member" card on Profile; visitor event RSVP; guest hospitality check-in guidance; interactive Bible trivia quizzes without admin clutter.
- **Memory Bank Synchronization:**
  - Updated `PROJECT_STATUS.md`, `ARCHITECTURE.md`, `DECISIONS.md` (ADR-010, ADR-011), and `CHANGELOG.md`.

---

## [1.1.0] - 2026-10-06 — Youth Alive Fellowship | Kingdom Giants Brand & Demo Fix

### Changed
- **Platform Identity Alignment:**
  - Renamed platform from "Church Youth Platform" to **"Youth Alive Fellowship | Kingdom Giants"** across `metadata.json`, `strings.xml`, `settings.gradle.kts`, `CommonComponents.kt`, `MainAppScaffold.kt`, and all UI screens.
  - Updated platform tagline to **"Kingdom Giants"** (localized across English, Yorùbá, Hausa, and Igbo).
- **Navigation Architecture & Games Session:**
  - Elevated the **Games & Bible Trivia Session** to be a primary bottom navigation destination with a dedicated gamepad controller icon (`Icons.Default.SportsEsports`).
  - Added direct access to live multiple-choice trivia quizzes with countdown timer, scoring, and global fellowship leaderboard.
  - Added an Announcements / Alerts notification bell icon in the TopAppBar for instant access to broadcasts without crowding the bottom navigation.
- **Authentication & Demo Quick Access:**
  - Fixed demo quick-login issue where unseeded database returned "Invalid email/phone or password."
  - Added eager pre-population trigger upon database instance retrieval.
  - Made demo login resilient with automated fallback credential resolution for Administrator, Youth Executive, and Youth Member accounts.
  - Added explicit `@youthalive.org` demo accounts alongside legacy `@churchyouth.org` accounts.

### Added
- **Stage 4.1: Multilingual Localisation (NFR-012):**
  - Integrated full localization bundles for Nigeria's 4 core languages: English (`values`), Yorùbá (`values-yo`), Hausa (`values-ha`), and Igbo (`values-ig`).
  - Implemented dynamic runtime `LanguageManager` with in-app language switcher in Profile settings.
  - Translated scripture memory verses and greeting banners across all 4 languages.
- **Stage 4.2: Data Backup, Export & External Integration (FR-028, NFR-008):**
  - CSV export tools for Financial Records, Attendance Records, and Member Roster (`DataExporter`).
  - Bi-directional sync adapter for external Church Management Systems (`ChmsSyncAdapter`).
  - SMS gateway broadcast adapter for instant emergency announcements to member mobile numbers (`SmsGatewayAdapter`).
- **Stage 4.3: Security Audit, Session Management & Audit Trail Console (NFR-005, NFR-015):**
  - Dedicated Administrator Audit Trail Console in Executive Command Centre displaying immutable logs.
  - Real-time logging of financial additions, role changes, member approvals, and moderation actions.
- **Stage 4.4: QA & Release Validation:**
  - Added `Phase4Test.kt` validating Language Localization, CSV Exporters, ChMS sync, and SMS gateways.
  - Complete automated test suite passing at 100%.

---

## [0.4.0] - 2026-10-05 — Phase 2 & Phase 3 Complete (Community, Departments, Administration, Spiritual Library)

### Added
- **Light / Dark Mode Dynamic Theming:**
  - Configured app to launch in clean Light Mode by default, adhering to user requirement.
  - Added dynamic theme toggle button in TopAppBar and `ThemeManager` allowing immediate one-tap switching to Dark Mode and back.
- **Stage 2.1: Groups & Department Workspaces (FR-013, FR-014, FR-015):**
  - Dedicated unit workspaces for Media, Choir, Drama, Ushering, Prayer, Sports, and Cell Fellowships.
  - Member assignment & unit roster management for department heads and administrators.
  - Meeting schedules, venues, and leader oversight cards.
- **Stage 2.2: Multi-Method Attendance Tracking (FR-016, FR-017):**
  - Services attendance ledger (Sunday Service, Youth Meeting, Bible Study, Rehearsals, Prayer Vigils).
  - Multi-method check-in support: Manual headcount entry, QR code meeting pass generation, and member self-verification check-in.
  - Real-time attendance statistics aggregation across services.
- **Stage 2.3: Community Discussion Forum & Direct Pastoral Access (FR-018, FR-019):**
  - Categorized discussion forum (Faith & Career, Spiritual Growth, Testimonies, Prayer Requests, Q&A).
  - Interactive likes, replies, threaded comment discussions, and content moderation tools (flag/hide).
  - Direct Pastoral & Executive confidential contact channel with encrypted delivery.
- **Stage 2.4: Executive Command Centre & Universal Search (FR-025, FR-026):**
  - High-level executive KPI metrics: Members, Attendance, Finances, Programmes.
  - Universal full-text search across Events, Announcements, Discussions, Spiritual resources, and Members.
- **Phase 3: Spiritual & Educational Content Hub (FR-020):**
  - Sermon library, Daily Devotionals, Bible Study Outlines, and Prophetic Prayer Points.
  - Simulated audio sermon recording player with progress tracker.
  - Offline bookmarking support with Room local persistence.
  - Scripture Memory Verse of the Week challenge with interactive reveal card.
- **Automated Tests:**
  - `Phase2And3Test.kt`: Unit and Robolectric tests covering Department roster assignment, Attendance recording & QR verification, Community forum & moderation, Spiritual bookmarks, and Theme toggling.

### Status
COMPLETE (Phase 2 & Phase 3 Verified & Functional)

---

## [0.3.0] - 2026-10-05 — Phase 1 MVP Complete (Stages 1.2 to 1.7) & Logo Visual Identity

### Added
- **Visual Identity & Logo Integration:**
  - Integrated custom church emblem vector asset (`ic_church_logo.xml`) featuring the Holy Ghost Fire flame, global world map, and the Shield of Faith.
  - Adapted color system to the logo: Fiery Red Flame (`#D32F2F`), Globe Obsidian (`#111827`), Shield Silver (`#78909C`), and Flame Gold (`#F59E0B`).
  - Configured adaptive launcher icon with white perimeter and centered church emblem.
- **Stage 1.2: Authentication & RBAC (FR-001, FR-003, FR-004):**
  - Registration UI with validation, department and fellowship selection, emergency contact info, and admin approval notice.
  - Sign-in UI with email/phone credentials, password visibility toggle, error handling, and demo quick-login buttons for Admin, Executive, and Member roles.
- **Stage 1.3: User Profiles & Member Management (FR-002, FR-027):**
  - Profile screen displaying personal info, department details, emergency contact card.
  - Real profile editing modal dialog with instant persistence.
  - Administrative User Approval Console for Administrators and Youth Executives to review pending accounts.
- **Stage 1.4: Youth Programme Dashboard (FR-005 to FR-009):**
  - Interactive programme dashboard with upcoming events, date pills, venue, minister, and attendance capacity progress bars.
  - RSVP registration and cancellation with real-time capacity management.
  - Event detail modal and "Publish Youth Programme" dialog for authorized leaders.
- **Stage 1.5: Announcement Portal & Broadcasts (FR-010 to FR-012):**
  - Announcement feed with priority banners (URGENT in fiery red, IMPORTANT in amber, NORMAL in silver).
  - Target audience filters (All Church, Youth Fellowship, My Department, Executives Only).
  - Broadcast publishing dialog for executives and ministers.
- **Stage 1.6: Financial Commitments & Records (FR-021, FR-022):**
  - Financial dashboard with total personal giving and global youth summary.
  - Official church bank transfer details (Zenith Bank account and USSD dial syntax).
  - "Give / Record Commitment" form supporting tithes, offerings, pledges, and dues across Bank Transfer, Card, Cash, and USSD.
  - Transaction ledger with reference codes and status badges.
- **Stage 1.7: Youth Engagement Games & Weekly Reports (FR-023, FR-024):**
  - Interactive Bible Trivia & Faith Challenge quiz game runner with options, answer verification, score tally, and scripture finish screen.
  - Global Youth Leaderboard.
  - Weekly Unit Report submission form and executive review list.
- **MainAppScaffold Navigation Shell:**
  - 5-tab Material 3 bottom navigation bar connecting all 5 MVP pillars.
  - Dynamic TopAppBar with church logo emblem, user name, and role badge.
  - 9-role persona switcher for rapid testing and stakeholder demonstration.
- **Automated Tests:**
  - `ViewModelFlowsTest` verifying Auth, Dashboard, Finance, and Engagement flows.
  - 17 unit and Robolectric tests passing at 100%.

### Status
COMPLETE (Phase 1 MVP Verified & Functional)

---

## [0.2.0] - 2026-10-05 — Stage 1.1: Core Foundation, Database Schema & Security Infrastructure

### Added
- Complete Room Persistence schema with 10 entities: `UserEntity`, `SessionEntity`, `AuditLogEntity`, `EventEntity`, `EventRegistrationEntity`, `AnnouncementEntity`, `FinancialRecordEntity`, `GameQuizEntity`, `GameScoreEntity`, and `WeeklyReportEntity`.
- Comprehensive TypeConverters supporting all domain enums (roles, statuses, departments, priorities, payment methods, quiz categories).
- Type-safe Room DAOs with reactive Kotlin `Flow` queries and transaction support: `UserDao`, `AuditLogDao`, `EventDao`, `AnnouncementDao`, `FinancialRecordDao`, `GameDao`, `WeeklyReportDao`.
- `ChurchYouthDatabase` singleton with SQLite engine and `DatabasePrepopulator` initial seed accounts (Admin, Youth Executive, Youth Member, starter events, urgent announcements, sample quiz, and weekly report).
- Cryptographic security engine `PasswordHasher` utilizing multi-iteration SHA-256 with cryptographically generated 16-byte random salts and constant-time equality checks.
- Granular Role-Based Access Control evaluator `RoleEvaluator` supporting all 9 stakeholder roles with specific permission sets.
- Offline-first Repository implementations: `UserRepositoryImpl`, `EventRepositoryImpl`, `AnnouncementRepositoryImpl`, `FinanceRepositoryImpl`, `GamesRepositoryImpl`, and `WeeklyReportRepositoryImpl`.
- Central Dependency Injection container: `AppContainer`, `DefaultAppContainer`, and registered `ChurchYouthApp` in AndroidManifest.
- Unit and Robolectric test suite: `SecurityTest` and `DatabaseAndRepositoryTest` covering encryption, RBAC, CRUD, and audit trail verification.

### Tests
- `SecurityTest`: Passed (Salt uniqueness, password verification, invalid rejection, role permissions).
- `DatabaseAndRepositoryTest`: Passed (User registration, login, event registration with count aggregation, financial ledger calculation, audience announcement filtering, audit logging).
- Automated test run: **BUILD SUCCESSFUL** via `gradle :app:testDebugUnitTest`.

### Status
COMPLETE

---

## [0.1.0] - 2026-10-05 — Stage 0: Architecture, Project Baseline & Traceability

### Added
- Created `PROJECT_STATUS.md` as the unified project progress tracking document.
- Created `REQUIREMENTS_TRACEABILITY.md` mapping all 28 Functional Requirements and 15 Non-Functional Requirements from SRS v1.0.
- Created `ARCHITECTURE.md` specifying system architecture, data models, RBAC rules, and technical design.
- Created `DECISIONS.md` logging baseline architectural and engineering decisions.
- Created `CHANGELOG.md` to track developmental evolution across all stages.

### Changed
- Configured platform identity in `metadata.json` with project title "Church Youth Platform" and description.
- Set launcher app name string to "Church Youth" in `app/src/main/res/values/strings.xml`.
- Set Gradle root project name to "Church Youth Platform" in `settings.gradle.kts`.
- Configured unique Android application ID `com.aistudio.churchyouth.ptqmvy` in `app/build.gradle.kts`.

### Tests
- Baseline compilation verified via Gradle build toolchain (`compile_applet` passed).

### Status
- COMPLETE (Stage 0 Baseline established).
