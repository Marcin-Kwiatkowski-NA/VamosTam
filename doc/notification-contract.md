# Notification Contract v1

Single source of truth for the payload shape shared between backend (FCM + STOMP
+ REST notification center) and frontend (push handlers, bell row tap, in-app
snackbars, cold-start pending-link consumer).

PRs that diverge from this doc get rejected.

## Ownership

- Backend owns **payload keys** and their semantics.
- Frontend owns **final route strings** (GoRouter paths).
- `deepLink` is a convenience string derived server-side from typed fields — it
  is **not** the contract. Never build new features on it.

## FCM `data` payload schema (v1)

Every notification push emits the following keys in `Message.data`:

| Key              | Type   | When present | Description |
|------------------|--------|--------------|-------------|
| `routeVersion`   | string | always       | `"1"` |
| `targetType`     | string | always       | `ENTITY` \| `LIST` \| `NONE` |
| `entityType`     | string | always       | `CONVERSATION` \| `RIDE` \| `SEAT` \| `REVIEW` \| `SAVED_SEARCH` \| `USER` |
| `entityId`       | string | `targetType=ENTITY` | ID of the target entity |
| `resultKind`     | string | `targetType=LIST`   | `RIDE` \| `SEAT` |
| `listFilters`    | string | `targetType=LIST`   | JSON-encoded map (see below) |
| `type`           | string | always       | `NotificationType.name()` (e.g. `SEARCH_ALERT_MATCH`) |
| `notificationId` | string | persisted rows | UUID of the Notification row |
| `channel`        | string | persisted rows | `NotificationChannel.name()` |
| `collapseKey`    | string | always       | empty string if unused |
| `deepLink`       | string | compat       | Derived. Client uses only if `targetType` missing (pre-v1 rows). |
| `bigBody`        | string | R2+          | Multi-line expanded body for Android BigText |

### `listFilters` map keys

Serialized as compact JSON (no whitespace). Only filters that narrow the list
view, no labels/comments.

| Key                 | Type   | Notes |
|---------------------|--------|-------|
| `originOsmId`       | number | |
| `destinationOsmId`  | number | |
| `originLat`         | number | |
| `originLon`         | number | |
| `destinationLat`    | number | |
| `destinationLon`    | number | |
| `originName`        | string | Already UI-ready (truncated upstream if needed) |
| `destinationName`   | string | |
| `earliestDeparture` | string | ISO `YYYY-MM-DD` |

## FCM 4 KB budget discipline

FCM rejects the whole message if the serialized `data` map exceeds 4096 bytes.
Rules:

1. **No duplication.** Fields that belong inside `listFilters` (names, coords,
   osmIds, date) live **only** inside `listFilters`. Do not also emit them as
   top-level keys.
2. **`deepLink` is derived** from the typed fields. Do not pack extra labels
   into it.
3. **Short keys only** where the map is already defined — no comment strings.
4. **R2 rich field caps:**
   - `bigBody` ≤ 256 chars
   - `previewRows` ≤ 3 rows × ~60 chars (≈180 chars total)
   - Names truncated to 20 chars, times `HH:mm`, prices integer + ` zł`
5. **Observability.** `NotificationService.buildFcmData` logs the serialized
   payload size at DEBUG and emits a WARN when it exceeds 3500 bytes so
   regressions surface before FCM rejects.

## Route rules (v1)

Client resolver (`NotificationNavigationTarget.toRoute()`) applies:

| Target | Route |
|--------|-------|
| `ENTITY + CONVERSATION + id`   | `/messages/chat/{id}` |
| `ENTITY + RIDE + id`           | `/rides/offer/r-{id}` |
| `ENTITY + SEAT + id`           | `/seats/offer/s-{id}` |
| `ENTITY + REVIEW + id`         | `/user/{id}/reviews` |
| `ENTITY + SAVED_SEARCH + id`   | `/profile/search-alerts` |
| `ENTITY + USER + id`           | `/user/{id}` |
| `LIST   + RIDE + filters`      | `/rides/list?{encodedFilters}` |
| `LIST   + SEAT + filters`      | `/seats/list?{encodedFilters}` |
| `NONE`                         | `/notifications` |
| `FALLBACK(deepLink)`           | `deepLink` if non-empty, else `/notifications` |

## Fallback rules

- `targetType=ENTITY` with missing `entityId` → route to the corresponding
  index (`/rides`, `/seats`, etc.) rather than crashing.
- `targetType=LIST` with missing `listFilters` → route to unfiltered list.
- `targetType` missing (pre-v1 row) → `FallbackTarget(deepLink)`.
- `deepLink` missing too → `/notifications` (the notification center).

## SearchAlert push split

SearchAlert matches are split per kind to avoid a "mixed" payload:

| Matches                       | Pushes emitted                                        |
|-------------------------------|-------------------------------------------------------|
| 1 ride                        | `ENTITY / RIDE / {rideId}`                            |
| 2+ rides                      | `LIST   / RIDE / {listFilters}`                       |
| 1 seat                        | `ENTITY / SEAT / {seatId}`                            |
| 2+ seats                      | `LIST   / SEAT / {listFilters}`                       |
| Mixed (rides + seats)         | **Two separate `notify()` calls** — never one payload |

## Context-aware mark-read

Opening the underlying entity (conversation, ride, seat, review) clears its
bell rows, regardless of entry path. Endpoint:

```
POST /me/notifications/read-by-entity
Body: { "entityType": "...", "entityId": "..." }
```

Idempotent — always returns 204 even when 0 rows are affected.

Tap-row mark-read (`POST /{id}/read`) remains as a secondary trigger for the
"user only opened the bell, didn't navigate" case.
