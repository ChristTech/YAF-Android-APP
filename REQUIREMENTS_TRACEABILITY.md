# Requirements Traceability Matrix (RTM)
## Church Youth Platform

**Document Version:** 1.0  
**Source Document:** Software Requirements Specification (SRS v1.0)  
**Status Reference:** NOT STARTED | PLANNED | IN PROGRESS | IMPLEMENTED | TESTING | COMPLETE | BLOCKED  

---

### Functional Requirements (FR-001 to FR-028)

| Requirement ID | Requirement Name | Description | Phase | Stage | Status | Verification Method | Notes |
|:---|:---|:---|:---:|:---:|:---:|:---|:---|
| **FR-001** | User Registration | System allows users to register for an account with credentials and basic profile details. | 1 | 1.2 | COMPLETE | Unit / Robolectric / UI | Core Auth & Onboarding flow (`AuthScreen`) |
| **FR-002** | User Profiles | System maintains user profiles: personal, contact, fellowship, department, emergency contacts. | 1 | 1.3 | COMPLETE | Database / ViewModel / UI | MVP Pillar 3 (`ProfileScreen`) |
| **FR-003** | Administrator Approval | Supports admin approval of new registrations where required by church policy. | 1 | 1.2 | COMPLETE | Unit / State Transition test | Configurable approval pipeline (`ProfileScreen`) |
| **FR-004** | Authentication | Secure authentication via email/phone credentials and Google sign-in support. | 1 | 1.2 | COMPLETE | Auth Flow / Password Hash test | Multi-method auth (`AuthViewModel`) |
| **FR-005** | Youth Programme Dashboard | Central interactive dashboard presenting upcoming youth programmes and activities. | 1 | 1.4 | COMPLETE | Compose UI / ViewModel test | MVP Pillar 1 (`ProgrammeDashboardScreen`) |
| **FR-006** | Event Management | Authorized users create, update, publish, reschedule, and manage youth programmes. | 1 | 1.4 | COMPLETE | CRUD / RBAC authorization test | Executive privilege (`CreateEventDialog`) |
| **FR-007** | Event Information | Rich event data: name, date/time, venue, speaker, poster, contact, limits, agenda. | 1 | 1.4 | COMPLETE | Database / UI Detail test | Full event detail modal (`EventDetailDialog`) |
| **FR-008** | Event Registration | Members register/RSVP for events with registration enabled. | 1 | 1.4 | COMPLETE | Concurrency / Capacity test | RSVP capacity enforcement (`EventRepository`) |
| **FR-009** | Participant Management | Authorized executives view and export registered participants for events. | 1 | 1.4 | COMPLETE | Admin List / Export test | Attendance tracking & roster |
| **FR-010** | Announcements | Authorized executives publish official broadcasts for church & youth activities. | 1 | 1.5 | COMPLETE | UI / Broadcast test | MVP Pillar 2 (`AnnouncementsScreen`) |
| **FR-011** | Targeted Announcements | Filter announcements by youth group, department (e.g. Choir, Media), or leadership tier. | 1 | 1.5 | COMPLETE | Filtering / Role query test | Scoped visibility (`AnnouncementRepository`) |
| **FR-012** | Notifications | System provides reminders and notification feeds for events and announcements. | 1 | 1.5 | COMPLETE | In-app notification feed test | Urgent priority badge & Pinned alerts |
| **FR-013** | Groups and Departments | Support units: Media, Choir, Drama, Ushering, Prayer, Sports, and Cell Fellowships. | 2 | 2.1 | COMPLETE | Unit CRUD / Hierarchy test | Community Pillar (`DepartmentScreen`) |
| **FR-014** | Department Assignment | Authorized leaders assign members to departments and fellowships. | 2 | 2.1 | COMPLETE | Role Assignment test | Member roster management (`AssignMemberDialog`) |
| **FR-015** | Department Management | Unit leaders manage rosters, unit attendance, announcements, and unit reports. | 2 | 2.1 | COMPLETE | Sub-unit RBAC test | Scoped leader dashboard (`DepartmentWorkspaceDialog`) |
| **FR-016** | Attendance Recording | Record attendance for services, meetings, Bible studies, prayer sessions, and events. | 2 | 2.2 | COMPLETE | Entry / Summary aggregation test | Supports regular and special meets (`AttendanceTrackerTab`) |
| **FR-017** | Attendance Methods | Support manual entry, QR code check-in, self check-in, and location-based check-in. | 2 | 2.2 | COMPLETE | QR / Check-in simulation test | Multi-mode check-in (`QrCheckInTab`) |
| **FR-018** | Communication | Group chat, discussion forums, member comments, and direct executive contact. | 2 | 2.3 | COMPLETE | Messaging / Forum feed test | Moderated engagement (`CommunityScreen`) |
| **FR-019** | Moderation | Authorized moderators flag, edit, hide, or approve user-generated communication. | 2 | 2.3 | COMPLETE | Content Moderation test | Profanity & etiquette protection (`CommunityViewModel`) |
| **FR-020** | Spiritual & Educational Content | Sermons, Bible studies, devotionals, articles, audio, video, prayer points, bookmarks. | 3 | 3.1-3.3 | COMPLETE | Media player / Bookmark test | Content Library & Downloads (`SpiritualScreen`) |
| **FR-021** | Financial Commitments & Records | Management and recording of tithes, offerings, pledges, project commitments, dues. | 1 | 1.6 | COMPLETE | Transaction / Ledger tests | MVP Pillar 4 (`FinanceScreen`) |
| **FR-022** | Payment Channels | Bank transfer details, payment verification, and multi-channel payment records. | 1 | 1.6 | COMPLETE | Verification / Receipt test | Audit-logged transactions |
| **FR-023** | Youth Games | Interactive engagement games (Bible trivia, scripture memory, quiz leaderboards). | 1 | 1.7 | COMPLETE | Game Engine / Score test | MVP Pillar 5 (`GamesAndReportsScreen`) |
| **FR-024** | Weekly Reports | Creation, submission, storage, and authorized access to weekly activity reports. | 1 | 1.7 | COMPLETE | Report Form / Approval test | MVP Pillar 5 (`WeeklyReportRepository`) |
| **FR-025** | Administrative Dashboard | Central administrative hub for user management, events, finance, and system health. | 2 | 2.4 | COMPLETE | Admin Navigation / KPI tests | High-level metrics & oversight (`AdminDashboardScreen`) |
| **FR-026** | Search and Access | Universal search and discovery across programmes, announcements, content, records. | 2 | 2.4 | COMPLETE | Query & Filter test | Full-text indexed search (`UniversalSearchTab`) |
| **FR-027** | User Data Management | Admin tools for managing user status, role elevation, suspension, and data export. | 1 | 1.3 | COMPLETE | Admin Action / Audit test | Privacy and data governance (`ProfileScreen`) |
| **FR-028** | Existing-System Integration | Extensible architecture for church management system, SMS, and data sync. | 4 | 4.2 | COMPLETE | Integration Adapter test | External sync adapters (`DataExportAndSync.kt`) |

---

### Non-Functional Requirements (NFR-001 to NFR-015)

| Requirement ID | Category | Description | Phase | Stage | Status | Verification Method |
|:---|:---|:---|:---:|:---:|:---:|:---|
| **NFR-001** | Performance | Fast UI response times (<200ms transitions) and smooth 60fps scrolling under load. | All | All | COMPLETE | Performance profiling & LazyLists |
| **NFR-002** | Connectivity | Offline-first architecture; complete offline readability and local persistence during poor network. | All | 1.1 | COMPLETE | Room Offline & Cache integration |
| **NFR-003** | Availability | High availability, resilient error boundaries, and non-blocking background workers. | All | All | COMPLETE | Crash-free session verification |
| **NFR-004** | Scalability | Modular repository architecture capable of handling thousands of members and records. | 1-4 | 1.1 | COMPLETE | Room indexing & benchmark tests |
| **NFR-005** | Security | Strong credential encryption, salted hashing, role enforcement, and session timeouts. | 1 | 1.1 | COMPLETE | Security & crypto unit tests (`SecurityTest`) |
| **NFR-006** | Payment Security | Sensitive card data never stored locally; verifiable reference tokens and bank proof attachments. | 1 | 1.6 | COMPLETE | Financial audit validation |
| **NFR-007** | Data Privacy | Personal, financial, and contact data accessible only to authorized roles and data owners. | 1-2 | 1.3, 2.4 | COMPLETE | Permission boundary checks |
| **NFR-008** | Backup & Recovery | Automated local and exportable database backups ensuring zero data loss. | 4 | 4.2 | COMPLETE | Export/Import integrity test (`DataExporter`) |
| **NFR-009** | Usability | Intuitive, clean Material 3 design welcoming to first-time youth and elderly alike. | All | All | COMPLETE | Heuristic UI/UX evaluation (M3 standard) |
| **NFR-010** | Accessibility | Touch targets >= 48dp, high contrast typography, scalable fonts (sp), full TalkBack descriptions. | All | All | COMPLETE | Accessibility audit & Compose checks |
| **NFR-011** | Device Support | Responsive layouts for compact phones, foldable devices, and tablets (Window Size Classes). | All | All | COMPLETE | Multi-resolution layout tests |
| **NFR-012** | Languages | Support for 4 core languages: English, Yoruba, Hausa, Igbo. | 4 | 4.1 | COMPLETE | String resource & LanguageManager verification |
| **NFR-013** | Maintainability | Clean architecture, well-separated concerns, clear naming, and low coupling. | All | All | COMPLETE | Code review & architecture checks |
| **NFR-014** | Reliability | Robust transactional database updates; zero silent data loss on crashes or power failure. | 1 | 1.1 | COMPLETE | Room Transaction tests (`DatabaseAndRepositoryTest`) |
| **NFR-015** | Auditability | Immutable audit logs for all financial entries, role changes, and administrative actions. | 1 | 1.1 | COMPLETE | Audit log verification (`DatabaseAndRepositoryTest`, `AdminDashboardScreen`) |
