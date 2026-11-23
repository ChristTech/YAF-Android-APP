# Architecture and Engineering Decision Log
## Church Youth Platform

This document records key architectural, technical, and process decisions made throughout the lifecycle of the Church Youth Platform project.

---

### Decision: ADR-001 — Client Architecture: Native Android with Kotlin & Jetpack Compose
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** The SRS mandates a mobile-first platform supporting youth members with fast responsiveness, high accessibility, and high performance on Android devices, tablets, and varied form factors (NFR-001, NFR-009, NFR-010, NFR-011).
- **Alternatives Considered:** 
  - Cross-platform hybrid web wrapper (Capacitor/Cordova) — Rejected due to slower animation performance, high memory footprint, and subpar offline touch responsiveness.
  - Traditional Android XML Views — Rejected in favor of modern declarative Jetpack Compose, which accelerates UI development, reduces boilerplate, and ensures robust state-driven UI consistency.
- **Chosen Approach:** Kotlin + Jetpack Compose with Material Design 3 (M3).
- **Consequences:** Provides native performance (60fps), fluid gestures, deep Android system integration (biometrics, notifications, file export), and clean declarative UI management.

---

### Decision: ADR-002 — Persistence & Offline Resilience: Android Room Database
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** The SRS (NFR-002, NFR-014) specifies that the platform must operate effectively over slow or unstable internet connections common in church halls and regional locations, with zero silent data loss.
- **Alternatives Considered:**
  - Cloud-only remote API with transient memory caching — Rejected because intermittent network disconnects would break member access to programmes, devotional content, and financial forms.
  - Raw SQLite / Key-Value Stores — Rejected due to lack of type safety, complex schema migration overhead, and lack of reactive Flow integration.
- **Chosen Approach:** Android Room ORM with SQLite backend, utilizing Kotlin Coroutines and reactive `Flow` observation for all critical data stores.
- **Consequences:** Ensures full offline capability for reading events, reading announcements, recording offline drafts, and immediate UI updates, with structured schema migrations and transactional integrity.

---

### Decision: ADR-003 — Application Pattern: MVVM with Clean Architecture
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** The platform is planned to grow over 4 major phases and 18+ stages. A clear separation of concerns is necessary to avoid monolithic, fragile code (NFR-013).
- **Alternatives Considered:**
  - Simple monolithic Activity-based code — Rejected as unmaintainable beyond the MVP.
  - Redux/MVI — Considered, but standard MVVM with Android `ViewModel`, `StateFlow`, and repository abstractions provides the optimal balance of simplicity, team familiarity, and testability.
- **Chosen Approach:** MVVM layered into Presentation (Compose UI + ViewModels), Domain (Data models, Use Cases, Validation), and Data (Room DAOs, Repositories, Network DataSource stubs/APIs).
- **Consequences:** Highly testable with JUnit and Robolectric; isolates UI from business logic and database interactions.

---

### Decision: ADR-004 — Role-Based Access Control (RBAC) Architecture
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** The SRS defines 9 stakeholder user types (Youth Members, Youth Executives, Pastors/Ministers, Youth Coordinators, Group/Cell Leaders, Department Leaders, Administrators, Parents/Guardians, Guests) with distinct functional permissions and data boundaries.
- **Alternatives Considered:**
  - Simple boolean flags (`isAdmin`) — Inadequate for granular permissions such as department leaders managing only their specific choir or media rosters without global admin rights.
  - External complex permission engine — Excessive overhead.
- **Chosen Approach:** Strongly typed enum `UserRole` and permission set evaluator (`Permission` flags: `PUBLISH_ANNOUNCEMENTS`, `MANAGE_EVENTS`, `VIEW_FINANCES`, `MANAGE_USERS`, `SUBMIT_REPORTS`, `MANAGE_DEPARTMENT`, etc.).
- **Consequences:** Allows dynamic feature gating, secure view rendering, and strict authorization enforcement at both the repository and UI layer.

---

### Decision: ADR-005 — Security, Hashing & Auditability Discipline
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** Sensitive personal, financial, and attendance records require non-repudiation, protection against tampering, and verifiable audit trails (NFR-005, NFR-006, NFR-007, NFR-015).
- **Alternatives Considered:**
  - Plaintext credential verification or external third-party credential caching — Rejected for security violations.
- **Chosen Approach:** Secure salted SHA-256 password hashing, role-based session tokens, and an immutable `AuditLog` database entity that logs critical actions (financial record additions, role promotions, status updates) with timestamp and user ID.
- **Consequences:** Transparent auditability for executive review; robust tamper-proof data handling.

---

### Decision: ADR-006 — Incremental MVP Delivery Strategy
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** The stakeholder explicitly mandated: *"A smaller version launched quickly, with additional features added later."* (SRS Section 4.2 & Section 13).
- **Alternatives Considered:**
  - Big-bang release attempting all 28 requirements at once — Rejected due to high risk of missed launch deadlines, quality degradation, and scope creep.
- **Chosen Approach:** Phase 1 delivers the 5 core MVP pillars in discrete, testable stages (1.1 through 1.7), creating a complete working product before expanding to Phase 2 (Community/Departments), Phase 3 (Content Hub), and Phase 4 (Integrations).
- **Consequences:** Immediate stakeholder usability, fast feedback loop, and rock-solid foundation for future expansions.

---

### Decision: ADR-007 — Visual Identity & Palette Derivation from Stakeholder Emblem
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** The stakeholder provided the official youth crest featuring the Holy Ghost Fire, world map globe, and the Shield of Faith. The UI must be visually authentic to church branding and dignified for worship and administration.
- **Alternatives Considered:**
  - Generic blue or green Material 3 theme — Rejected as non-authentic to the church identity.
- **Chosen Approach:** Extracted and harmonized palette:
  - Primary: Fiery Red Flame (`#D32F2F`)
  - Secondary: Globe Obsidian / Ink Black (`#111827`)
  - Tertiary: Shield Silver / Platinum (`#78909C`)
  - Accent: Flame Gold (`#F59E0B`)
  - Surfaces: Clean crisp white (`#FFFFFF`) and slate neutral background (`#F8FAFC`).
  - Integrated custom vector asset `ic_church_logo.xml` and adaptive launcher icon.
- **Consequences:** Unified, bold, high-contrast, accessible visual design that immediately reflects the church youth platform's core identity.

---

### Decision: ADR-008 — Multi-Method Attendance & Verification Architecture
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** FR-016 and FR-017 mandate diverse attendance mechanisms: manual headcount, QR code check-in, and self check-in with dynamic verification codes to cater to various service environments (sanctuary, cell fellowships, outdoor retreats).
- **Chosen Approach:** Room-backed `AttendanceRecordEntity` storing meeting title, meeting type, gender breakdown, first-timers, verification codes, and check-in methods. Real-time aggregation of attendance across all church meetings with quick-code generation and validation.
- **Consequences:** Eliminates paper attendance sheets, supports real-time headcount dashboards, and ensures accurate records for leadership decision-making.

---

### Decision: ADR-009 — UI Theming: Default Light Mode with Switchable Dark Mode
- **Date:** 2026-10-05
- **Status:** Accepted
- **Reason:** User requirement specifies preference for the UI in Light Mode while remaining easily switchable to Dark Mode.
- **Chosen Approach:** Global `ThemeManager` state object with `isDarkMode = mutableStateOf(false)`. Light mode uses crisp `#FFFFFF` surfaces, `#F8FAFC` background, with Fiery Red (`#D32F2F`) and Globe Obsidian (`#111827`) brand accents. A top-bar toggle button and profile setting allow immediate switching to Dark Mode (`#0F172A`).
- **Consequences:** Maximizes visual contrast, daylight readability in church meetings, and fulfills user preference while preserving modern accessibility.

---

### Decision: ADR-010 — User Roles Streamlined to 4 Canonical Types: Admin, Youth Executive, Member, and Guest
- **Date:** 2026-10-07
- **Status:** Accepted
- **Reason:** User directive to simplify role hierarchy into exactly 4 canonical user types: Admin, Youth Executive, Member, and Guest.
- **Alternatives Considered:** Retaining 9 specialized roles (Pastors, Coordinators, Cell Leaders, Department Leaders, Parents) was found to introduce unnecessary complexity for users and administrators.
- **Chosen Approach:**
  - **Admin (`ADMINISTRATOR`):** Full platform command centre, audit trail inspection, user registration approvals, financial exports, and universal search.
  - **Youth Executive (`YOUTH_EXECUTIVE`):** Programme/event publishing, fellowship broadcast alerts, department/attendance oversight, weekly reports review.
  - **Member (`YOUTH_MEMBER`):** Programme RSVP, department workspace & QR check-in, community forums, interactive Bible Trivia games & leaderboard, sermon audio library, personal tithes & offerings.
  - **Guest (`GUEST`):** Public programme viewing, welcome greeting, and invitation prompt to register as a full Member.
- **Consequences:** Cleaner role boundaries, simpler permission matrix, streamlined onboarding, and faster role-switching for testing.

---

### Decision: ADR-011 — Screen-by-Screen Experience Matrix for 4 User Types
- **Date:** 2026-10-07
- **Status:** Accepted
- **Reason:** To ensure an intuitive, role-appropriate, and secure user experience, every screen and navigation hub must specifically tailor its presentation, actions, and affordances according to the active stakeholder role.
- **Screen Matrix:**
  1. **TopAppBar & Global Shell:**
     - **Admin:** Flame Red badge, "System Administrator • Full Platform Oversight".
     - **Youth Executive:** Flame Gold badge, "Executive Leader • Ministry Coordination".
     - **Member:** Forest Green badge, "Kingdom Giant • Active Youth Member".
     - **Guest:** Slate Blue badge, "Welcome to Youth Alive Fellowship!".
  2. **Programmes Dashboard (Tab 0):**
     - **Admin & Youth Executive:** See FAB (+) to create and publish programmes, access attendee rosters, manage capacity caps.
     - **Member:** Clean list of upcoming programmes, 1-tap RSVP/Register button, capacity indicator, no admin FAB clutter.
     - **Guest:** Prominent Guest Welcome Banner explaining fellowship gatherings, ability to RSVP as a visitor, no creation FAB.
  3. **Community Hub (Tab 1):**
     - **Departments & Attendance:**
       - *Admin & Executive:* Manage units, assign leaders, record attendance for meetings/services with headcount breakdowns (Male, Female, First-timers).
       - *Member:* "My Assigned Unit & Cell" details, meeting schedules, rehearsal venues, leader contacts, personal QR Check-In Pass.
       - *Guest:* "Discover Our Ministry Units" exploration guide (Choir, Media, Ushering, Drama, Prayer, Sports) with interest inquiry, QR pass.
     - **Youth Forum & Pastoral Contact:**
       - *Admin & Executive:* Forum participation + moderation tools (Hide/Flag inappropriate posts, Pin topics), pastoral inbox.
       - *Member:* Create discussion topics, share testimonies, pray for peers, like & comment, confidential Pastoral Contact.
       - *Guest:* Inspiring discussion reader, Guest orientation & confidential prayer request submission.
  4. **Games & Reports (Tab 2):**
     - **All 4 Roles:** Can play interactive Bible Trivia Quiz (timer, questions, scoring).
     - **Weekly Reports:**
       - *Admin & Executive:* Review weekly reports across departments & cells; Executive has FAB (+) to submit reports.
       - *Member & Guest:* Informational overview of departmental reporting; no report submission clutter.
       - *Guest Callout:* Banner inviting them to join as Member to save rank permanently on Global Leaderboard.
  5. **Spiritual Growth (Tab 3):**
     - Accessible to all roles with daily devotionals, multilingual memory verses, and sermon archives.
  6. **Profile Hub (Tab 4):**
     - **Admin:** Subtabs "My Profile" (with Admin Approval Console), "Giving & Pledges", and "Admin Command Centre" (Full System Oversight: KPIs, Search, Audit Logs, CSV Exports, SMS Broadcast, ChMS Sync).
     - **Youth Executive:** Subtabs "My Profile", "Giving & Pledges", and "Executive Command Centre" (Operational KPIs, Search, Reports).
     - **Member:** Subtabs "My Profile", "Giving & Pledges" (Personal Tithes, Offerings, Pledges, Bank Details). Command Centre hidden.
     - **Guest:** Subtabs "My Profile", "Giving & Pledges". Highlighted "Become a Kingdom Giant — Upgrade to Full Member" 1-tap card. Command Centre hidden.
- **Consequences:** Tailored user journey for each persona, zero administrative clutter for members and guests, while granting complete oversight to leadership.

---

### Decision: ADR-012 — Role-Tailored Screen Experiences in Youth Alive Fellowship Game Section
- **Date:** 2026-10-08
- **Status:** Accepted
- **Reason:** User directive to focus strictly on the Game section, maintaining the platform identity (**Youth Alive Fellowship**) across exactly 4 user roles: Admin, Youth Executive, Member, and Guest, without altering any other part of the application.
- **Chosen Architecture & Screen Breakdown:**
  1. **Admin (`ADMINISTRATOR`):**
     - Header: "Youth Alive Fellowship • Game Command" (Fiery Red Flame accent).
     - Trivia Management: "+ Create New Scripture Quiz" dialog (`CreateQuizDialog`) to publish custom quizzes with categories, difficulty, time limits, and multiple questions; quiz deletion; score moderation/deletion for audit integrity.
     - Weekly Reports Oversight: KPI summary cards (Total Reports, Total Attendance Aggregated, Total 1st Timers), department filter chips, and "Review & Leave Official Notes" dialog (`ReviewReportDialog`) with approval status.
  2. **Youth Executive (`YOUTH_EXECUTIVE`):**
     - Header: "Youth Alive Fellowship • Executive Portal" (Flame Gold accent).
     - Trivia: Executive leadership challenge banner, full quiz play, leaderboard tracking.
     - Reports: Floating Action Button (`+`) to submit weekly unit accountability reports (`SubmitWeeklyReportDialog`) across Media, Choir, Ushering, Drama, Prayer, and Cells; status tracking (`SUBMITTED`, `REVIEWED`) with executive leadership notes.
  3. **Member (`YOUTH_MEMBER`):**
     - Header: "Youth Alive Fellowship • Kingdom Giants" (Vibrant Green accent).
     - Personal Trivia Stats Card: Live tracking of personal best score, total quizzes taken, and Kingdom Rank title (Bible Explorer, Faith Seeker, Bible Scholar, Kingdom Giant 👑) with Colossians 3:16.
     - Quizzes & Leaderboard: Full quiz participation, with member's own score highlighted in green with a "★ YOU" badge on the global leaderboard.
     - Reports Tab: Re-framed as "Praise Reports & Highlights" — an inspirational, uplifting view of unit achievements and answered prayers without administrative submission forms.
  4. **Guest (`GUEST`):**
     - Header: "Welcome to Youth Alive Fellowship" (Royal Blue accent).
     - Trivia: Welcoming Guest Banner explaining fellowship trivia, barrier-free access to play all quizzes immediately.
     - Quiz Finish Experience: Dedicated congratulatory card with a call-to-action to register as a full Member to save their score permanently to the Global Leaderboard.
     - Reports Tab: Re-framed as "Ministry Unit Insights" — an exploration window showing how youth units serve with excellence and inviting guests to join.
- **Consequences:** Clean modular architecture (`GamesAndReportsScreen.kt`, `BibleGamesTabContent.kt`, `WeeklyReportsTabContent.kt`, `ActiveQuizRunner.kt`, `CreateQuizDialog.kt`), files kept under 350 lines, high role distinction, zero regression on other screens.

---

### Decision: ADR-013 — Live Multiplayer Showdown Arena Mechanics
- **Date:** 2026-10-14
- **Status:** Accepted
- **Reason:** User specification for live interactive showdown gameplay:
  1. Per-question countdown timer (15 seconds) with animated urgency color transitions.
  2. Between questions: An intermission round results screen displaying user accuracy, speed bonus breakdown, total game points, and **who is currently leading** (real-time fellowship standings with dynamic rank shifts like `▲ +2 positions` and `★ YOU` highlighting).
  3. No host code or PIN required: Seamless barrier-free access where all fellowship members are notified when game time is active and can jump straight into the arena with one tap.
  4. Final 3-tier podium ceremony (Gold 🥇 1st place, Silver 🥈 2nd place, Bronze 🥉 3rd place).
- **Implementation:**
  - `EngagementViewModel.kt`: `ActiveQuizState` enhanced with `timeRemainingSeconds`, `isTimeUp`, `isIntermissionLeaderboard`, `pointsAwardedLastQuestion`, `speedBonusLastQuestion`, `totalGamePoints`, `competitors`, `currentUserRank`, `currentUserRankMovement`.
  - Realistic multiplayer simulation of peer competitors (Sister Grace, Brother Samuel, Brother David O., Sister Faith, Brother Emmanuel) computing synchronized points, speed bonuses, and dynamic leaderboard positions.
  - `ActiveQuizRunner.kt`: Modularized into 3 arena stages:
    - `LiveQuestionActiveView`: Per-question 15s timer, linear progress ring, prompt card, 4 color-coded fast-action option cards (Red ▲, Blue ◆, Amber ●, Green ■) with speed incentives.
    - `LiveIntermissionLeaderboardView`: Round feedback banner ("CORRECT! 🎉 +850 pts", "TIME'S UP! ⏰", "INCORRECT ❌"), current fellowship standings showing top leaders, rank movements, and Scripture insights.
    - `LiveGamePodiumView`: 3-tier visual podium (1st, 2nd, 3rd) with trophy crown, final placement stats, role callout, and return button.
  - `BibleGamesTabContent.kt`: Prominent "🔴 LIVE GAME ARENA — NO CODE NEEDED" hero banner notifying users of live games, active participant count, instant 1-tap join button, and admin/executive alert broadcasting.
- **Consequences:** Highly engaging, fast-paced game energy without requiring manual host codes; seamless mobile experience for Youth Alive Fellowship.

---

### Decision: ADR-014 — Offline Notification Architecture for Leadership Broadcasts & Game Alerts
- **Date:** 2026-10-14
- **Status:** Accepted
- **Reason:** User request to complete all multi-user features offline prior to cloud server connection, specifically enabling users to receive notifications whenever something is posted by an Administrator or Youth Executive.
- **Implementation:**
  - **Dual-Channel Notification Engine (`FellowshipNotificationService`):**
    1. **Android System Notifications:** Declares `POST_NOTIFICATIONS` and `VIBRATE` permissions; builds `NotificationChannel("youth_alive_fellowship_channel")` with `IMPORTANCE_HIGH` and posts heads-up system status bar alerts.
    2. **In-App Persistent Notification Center:** Backed by Room SQLite database (`NotificationEntity`, `NotificationDao`); tracks read/unread states, categories, author roles, relative timestamps, and urgency badges.
  - **Automated Leadership Triggers:**
    - **Announcements & Broadcasts:** When Admin/Executive publishes in `AnnouncementsViewModel.publishAnnouncement()`, dispatches instant notification.
    - **Programmes & Events:** When Admin/Executive publishes a new programme in `DashboardViewModel.createEvent()`, dispatches event notification.
    - **Game Time Alerts:** When Admin/Executive triggers `broadcastGameTimeAlert()`, dispatches live Bible Showdown notification.
    - **Report Reviews:** When Admin reviews a unit report in `EngagementViewModel.reviewReport()`, dispatches review feedback notification.
  - **UI Integration (`MainAppScaffold.kt`):**
    - Dynamic TopAppBar Notification Bell with animated unread badge counter (`BadgedBox`).
    - Full-screen `NotificationCenterDialog` with category filtering chips (All, Announcements, Events, Game Time, Report Reviews), unread indicators, "Mark all as read" action, and item dismissal.
- **Consequences:** Full local notification fidelity without requiring an active remote server or push notification backend; prepares the app seamlessly for cloud sync.



