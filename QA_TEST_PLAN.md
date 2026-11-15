# CoinsLive Live (Android) — QA Test Plan

**App:** CoinsLive Live (`in.coinslive.live`)  
**Audience:** Testing intern  
**Build:** Use latest internal / debug APK from the team  
**Backend:** Production API `https://www.coinslive.in` (confirm with eng if using staging)

Mark each case **Pass / Fail / Blocked** and note device + Android version + build version (`versionName` / `versionCode`).

---

## 0. Setup (before testing)

| # | Step | Expected |
|---|------|----------|
| 0.1 | Install APK; open app | App launches; no crash on cold start |
| 0.2 | Note device model, Android OS version, app version | Record in report |
| 0.3 | Have **2 accounts** ready (Host A, Attendee B) if possible | Two people or two devices ideal |
| 0.4 | Stable Wi‑Fi + one run on mobile data | Note network used per case |

**Devices to cover (if available):** 1 phone Android 12+, 1 phone Android 14/15, optional tablet.

---

## 1. Sign-in / Account

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| A1 | First launch sign-in | Open app signed out → sign in with Clerk | Lands on Live tab; no blank/error loop | |
| A2 | Sign-out | Account tab → sign out | Returns to sign-in; cannot open rooms | |
| A3 | Sign-in again | Sign in with same account | Sessions list loads | |
| A4 | Kill & reopen | Force-stop app → reopen while signed in | Still signed in (or clear re-auth) | |
| A5 | Airplane mode sign-in | Toggle airplane → try sign-in | Clear error, no crash | |

---

## 2. Home (Live tab) — list & create visibility

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| H1 | Empty / first load | Signed-in user with no sessions | Empty states for Workshops / Adhyayan; create tiles visible | |
| H2 | Pull to refresh | Pull down on Live | Spinner; list refreshes; no crash | |
| H3 | Refresh button | Tap refresh in navy hero | List reloads | |
| H4 | **Create Adhyayan appears** | Live → Adhyayan tile → create with title → after create go back to Live | New room under **Your Adhyayan** (usually status Scheduled) | |
| H5 | Create Workshop appears | Live → Workshop tile → create → back to Live | New workshop under **Your Workshops** | |
| H6 | Sections | Host has live + scheduled + ended | **Live now**, **Your Workshops**, **Your Adhyayan**, **Recently ended** look correct | |
| H7 | Open from row | Tap a Workshop / Adhyayan row | Opens correct lobby | |
| H8 | Resume refresh | Create Adhyayan → open lobby → Back to Live | New Adhyayan still visible without manual refresh | |

**Regression focus:** Newly created Adhyayan must show under **Your Adhyayan**, not only after force-kill.

---

## 3. Create Workshop

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| W1 | Validation | Create with empty title | Create disabled / blocked | |
| W2 | Minimal create | Title only → Create | Success → Workshop lobby | |
| W3 | Description + capacity | Fill description, change attendee cap | Creates OK | |
| W4 | Schedule later | Toggle schedule → pick time → create | Creates; shows scheduled time on home | |
| W5 | Back without create | Fill form → system Back | Returns to Live; no orphan session | |

---

## 4. Create Adhyayan

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| D1 | Validation | Empty title | Create disabled | |
| D2 | Public room | Access Public, capacity 12 → Create | Lobby opens; room under Your Adhyayan | |
| D3 | Private room | Access Private → Create | Lobby shows **room code** for host | |
| D4 | Approval room | Access Approval → Create | Creates; join path requires request (test with Attendee B) | |
| D5 | Optional fields | Topic, description, learning goal | Saved / shown where applicable | |
| D6 | Capacity stepper | Set min/max (2–40) | Values stick; create OK | |

---

## 5. Workshop lobby & live room (Host)

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| WH1 | Lobby load | Open hosted workshop | Status, title load; no crash | |
| WH2 | Enter prep / room | Enter room as host | Camera/mic permission prompt if needed | |
| WH3 | Permissions deny | Deny camera & mic | Clear message; no native crash | |
| WH4 | Permissions allow | Allow both → enter | Local video/preview works | |
| WH5 | Mic / Cam toggles | Toggle Mic on/off, Cam on/off | UI + actual media match | |
| WH6 | Go Live | Host Go Live | Status becomes live; attendees can see | |
| WH7 | Share workshop | Share icon → share sheet | Link opens `/workshop/{slug}/live` style URL | |
| WH8 | Tools: Q&A | Tools → Q&A — answer / publish / dismiss (with attendee question) | Actions succeed | |
| WH9 | Tools: Messages | Reply / resolve attendee message | Works | |
| WH10 | Tools: Announce | Send broadcast | Appears for host; attendee sees if wired | |
| WH11 | People | People tab | Count / names update | |
| WH12 | End workshop | End | Room ends; attendees see ended state | |
| WH13 | Leave | Leave without End | Leaves room; session may still be live/prep per product rules | |

---

## 6. Workshop — Attendee

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| WA1 | Join Hub | Join tab → Workshop → slug (+ name) → register | Opens lobby | |
| WA2 | Enter while prep | Enter before host live | Waiting / prep messaging | |
| WA3 | Enter while live | Enter after host live | Sees host video/audio | |
| WA4 | Ask question | Chat/Tools → ask Q | Host sees question | |
| WA5 | Message host | Send private message | Host sees in Messages | |
| WA6 | Leave | Leave | Returns safely; can rejoin if still live | |

---

## 7. Adhyayan lobby & room (Host)

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| AH1 | Lobby details | Open created Adhyayan | Title, status, access, room code (if private) | |
| AH2 | Share invite | Share invite link | Share sheet; URL like `/adhyayan/{slug}/live` | |
| AH3 | Open prep | Scheduled → Open prep room | Status → backstage | |
| AH4 | Go live and enter | Go live and enter | Status live → enters room with A/V | |
| AH5 | Enter room | Already live → Enter room | Connects LiveKit | |
| AH6 | Peer grid | 2+ participants with cameras | Grid / tiles show others | |
| AH7 | Mic / Cam | Toggle | Works for host | |
| AH8 | End session | End | Session ended; list updates on return | |
| AH9 | Leave | Leave without End | Exits; session may stay live | |

---

## 8. Adhyayan — Joiner (second account)

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| AJ1 | Public join | Join tab → Adhyayan → public slug → Join | Admitted → can enter when live | |
| AJ2 | Private join wrong code | Private slug + wrong room code | Error; not admitted | |
| AJ3 | Private join correct code | Correct room code | Admitted | |
| AJ4 | Approval join | Approval session → Request to join | Pending message; host must admit (if host tools exist on web/iOS, note Android host admit gap if any) | |
| AJ5 | Enter live | When live + admitted → Enter | Sees peers; mic/cam work | |
| AJ6 | Waiting for host | Admitted but not live | Waiting message; no crash | |

---

## 9. Picture-in-Picture (critical new feature)

Test on **Workshop and Adhyayan** while connected (camera on preferred).

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| P1 | Minimise button | In room → Minimise | Small floating PiP window; call continues | |
| P2 | Home button / gesture | In room → press Home | Enters PiP (Android 8+); audio/video continue | |
| P3 | Back → PiP | In room → Back | Prefers PiP (does not immediately kill call) | |
| P4 | PiP UI | While in PiP | Controls/chrome hidden; video still visible | |
| P5 | Expand PiP | Tap PiP window | Returns to full room UI | |
| P6 | Leave from full UI | Expand → Leave | PiP ends; back to previous screen | |
| P7 | End while eligible | End session | Cannot stay in “live” PiP after end | |
| P8 | Switch apps | PiP → open Chrome briefly | PiP stays; meeting audio continues | |
| P9 | Not in room | On Home/Live → Home key | **No** PiP | |
| P10 | Deny overlay / OEM | On Xiaomi/Oppo/Vivo if available | Note if PiP blocked by OEM settings | |

---

## 10. Deep links

HTTPS App Links are **www only** (`www.coinslive.in`). Apex `coinslive.in` is not in the `autoVerify` filter (apex→www redirects fail Play Digital Asset Links). Prefer www URLs when testing L3/L4.

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| L1 | Custom scheme workshop | Open `coinslive://workshop/{slug}/live` (valid slug) | App opens workshop lobby (signed in) | |
| L2 | Custom scheme Adhyayan | `coinslive://adhyayan/{slug}/live` | Adhyayan lobby | |
| L3 | HTTPS workshop (App Link) | `https://www.coinslive.in/workshop/{slug}/live` | Opens app (verified App Link); lands correctly | |
| L4 | HTTPS Adhyayan (App Link) | `https://www.coinslive.in/adhyayan/{slug}/live` | Same | |
| L5 | Signed out deep link | Sign out → open deep link | Sign-in first, then can continue (note actual behavior) | |
| L6 | Invalid slug | Bad slug deep link | Error message; no crash | |

---

## 11. Join Hub

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| J1 | Workshop join empty slug | Submit empty | Blocked / error | |
| J2 | Workshop valid slug | Known workshop slug | Registers / opens lobby | |
| J3 | Adhyayan valid slug | Known slug | Join flow works | |
| J4 | Kind switch | Toggle Workshop ↔ Adhyayan | Correct fields (room code for Adhyayan) | |

---

## 12. Stability & edge cases

| ID | Case | Steps | Expected | Result |
|----|------|-------|----------|--------|
| E1 | Rotate / fold (if device supports) | Rotate in room | No crash; video recovers | |
| E2 | Incoming call | During live room receive phone call | After call, media recovers or clear state | |
| E3 | Poor network | Throttle / weak Wi‑Fi in room | Reconnect or readable error; no silent hang | |
| E4 | Background 2+ min | Leave app without PiP (or close PiP) then return | Document behavior (may disconnect) | |
| E5 | Double tap create | Spam Create button | Single create; no duplicates spam | |
| E6 | Rapid navigate | Lobby ↔ Room ↔ Back quickly | No crash / leaked audio | |

---

## 13. Out of scope / known gaps (do not file as P0 unless regressed)

- **Screen share publish** on Android — not implemented yet  
- Adhyayan chat / polls / materials (web has more; mobile is primarily A/V)  
- Mentor feed / coins payments / non-live product surfaces  

---

## Bug report template (copy per bug)

```
Title:
Severity: Blocker / High / Medium / Low
Build:
Device / Android:
Account used:
Steps:
Expected:
Actual:
Screenshot / screen recording:
Network (Wi‑Fi / data):
Happens every time? Y/N
```

---

## Suggested test order (half-day smoke)

1. A1 → H4 (**Adhyayan create → appears on home**) → AH4 → P1/P2 → AH8  
2. H5 → WH6 → WA1/WA3 → P1 → WH12  
3. L1/L2 deep links  
4. AJ2/AJ3 private Adhyayan  
5. A2/A3 account  

## Full pass

Run all tables above; attach filled Result column + bug list to eng.
