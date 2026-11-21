## End-to-end flow

1. **User opens app**
   - React app loads and initializes Firebase using config.js.

2. **Login**
   - Firebase Authentication signs in the user.
   - The app uses `user.uid` as the Firestore parent path: `users/{uid}`.

3. **Create habit**
   - Habit created under `users/{uid}/habits/{habitId}` via `createHabitDocument`.
   - Habit fields include:
     - `title`, `emoji`, `createdDate`
       - `frequencyType`, `weeklyTarget`, `scheduledDays` (schedule scoring; missing `frequencyType` means daily)
     - `currentStreak`, `longestStreak`, `lastCompletedDate`
     - `completionHistory`

4. **Reminder stored**
   - Reminder settings are stored directly on the habit document:
     - `reminderEnabled`
     - `reminderTime`
     - `reminderFrequency`
     - `selectedDays`
     - `notificationTitle`
     - `notificationMessage`
     - `reminderTimezone`

5. **Device token stored**
   - Browser device token saved under `users/{uid}/deviceTokens/{token}`.
   - Token document contains:
     - `token`
     - `createdAt`
     - `lastSeenAt`

6. **Spring Scheduler**
   - `streak-reminder-service` runs a scheduled task every minute.
   - It scans Firestore for habit reminders due now.

7. **Firestore query**
   - Service reads:
     - `users/{uid}/habits`
     - `users/{uid}/deviceTokens`
   - It must use the exact same field names and collection structure from the React app.

8. **ReminderService**
   - Business logic lives in service layer.
   - It decides which reminders are due based on:
     - `reminderEnabled`
     - `reminderTime`
     - `reminderFrequency`
     - `selectedDays`
     - `reminderTimezone`
   - For daily reminders, compare local time in `reminderTimezone`.
   - For weekly reminders, compare the stored day names plus time.

9. **NotificationService**
   - Builds notification payloads.
   - Prevents duplicate sends for the same habit, same user, same date/time window.
   - Supports multiple tokens per user.

10. **Firebase Admin SDK**
    - Uses Firebase Admin to send FCM messages to device tokens.
    - Reads token documents from `users/{uid}/deviceTokens`.

11. **FCM**
    - Sends push payload with `notification.title` and `notification.body` from the habit fields.

12. **Phone receives notification**
    - Device receives FCM notification via browser/service worker or native FCM client.

13. **User clicks notification**
    - The service worker handles payload clicks.
    - The app opens and shows the user the app.

## Notes
- This flow is based on the exact Firestore schema documented in `streak-reminder-service/FIRESTORE_SCHEMA.md`.
- The Spring service must not rename or change field names.
- No REST controllers are required yet; the scheduler and services are the current focus.

## One-time task reminder flow

1. The To-Do planner writes task details and reminder preferences to the existing `users/{uid}/tasks/{taskId}` document, including the task date, local time, IANA time zone, reminder offset, and `reminderStatus: pending`.
2. The Spring scheduler reads task documents on its existing minute cadence. It computes the reminder instant from task date/time and time zone; all-day reminders use 09:00 local time.
3. Before sending, a Firestore transaction changes the current pending task to `sending`. This prevents two scheduler instances from claiming the same reminder.
4. The service rechecks the task and completion state, loads the user's existing device tokens, and sends one FCM message per active token. A stable task/date/time tag lets the service worker replace duplicates.
5. On success the task status becomes `sent`. A completed task is marked `cancelled`; edits/reschedules reset the status to `pending`; a deleted task no longer exists to be sent. Invalid reminders become `invalid`, and tasks outside the configured two-minute grace window become `missed`.
6. FCM `UNREGISTERED` responses remove stale device tokens using the existing token cleanup path. If no token is currently available or all sends fail, the task remains retryable until its reminder time expires.

No extra task collection, REST endpoint, API key, or Firestore migration is needed. Deploy the updated Spring service along with the React app for task reminders to be delivered.