# Security Specification: The Gideonites - The Youth Alive Fellowship

## 1. Data Invariants
1. **Strict Multi-User PII Isolation (`/users/{userId}`)**: A user profile contains PII (`email`, `phone`) and can only be read (`get`/`list`) or modified by the authenticated owner (`request.auth.uid == userId`) or a verified Administrator (`isAdmin()`).
2. **Self-Assigned Privilege Prevention**: Non-admin users creating a profile in `/users/{userId}` can only assign `GUEST` or `YOUTH_MEMBER` roles and cannot escalate their `role` during profile updates.
3. **Authoritative Ownership & Immutable Creation Metadata**: Every document in `/users`, `/daily_scriptures`, `/programmes`, and `/quiz_sessions` binds its creator (`userId`, `authorId`, `createdById`, `hostId`) to `request.auth.uid` on creation and locks both the owner UID and `createdAt` timestamp as immutable on update.
4. **Query Enforcer on Shared Fellowship Collections**: All `list` operations on `/daily_scriptures`, `/programmes`, and `/quiz_sessions` require `resource.data.fellowshipId == 'GIDEONITES'` or creator ownership, rejecting unfiltered collection scrapes.
5. **Terminal State Locking**: Once a `YouthProgrammeDoc` or `CloudQuizSessionDoc` reaches `COMPLETED` or `CANCELLED`, non-admin updates are rejected.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Scrape**: `unauthDb.collection("users").get()` -> Must fail.
2. **Cross-User PII Leak**: Alice attempting `get` on `/users/bob_456` -> Must fail.
3. **Self-Privilege Escalation on Signup**: Alice creating `/users/alice_123` with `role: "ADMINISTRATOR"` -> Must fail.
4. **Role Escalation on Update**: Alice updating `/users/alice_123` with `role: "YOUTH_EXECUTIVE"` -> Must fail.
5. **Shadow / Ghost Field Injection**: Alice updating `/users/alice_123` with `{ isVerified: true }` -> Must fail via `hasOnly()`.
6. **Email Spoofing Attack**: Attacker authenticating with `email: "adebisivictor39@gmail.com"` and `email_verified: false` attempting admin read on `/users/bob_456` -> Must fail.
7. **Identity Spoofing on Scripture Broadcast**: Alice creating `/daily_scriptures/doc1` with `authorId: "bob_456"` -> Must fail.
8. **Unfiltered Query Scrape**: Alice calling `db.collection("daily_scriptures").get()` without `.where("fellowshipId", "==", "GIDEONITES")` -> Must fail.
9. **Unauthorized Scripture Tampering**: Bob attempting to update Alice's `/daily_scriptures/doc1` -> Must fail.
10. **Terminal State Bypass**: Host attempting to increment `currentQuestionIndex` on a `/quiz_sessions/s1` document whose `status` is already `"COMPLETED"` -> Must fail.
11. **Value Poisoning on Programme RSVP**: Member attempting to increment `registeredCount` by `+50` instead of `+1` on `/programmes/p1` -> Must fail.
12. **Future Timestamp Injection**: Client submitting a `createdAt` timestamp in the future (`> request.time`) -> Must fail.
