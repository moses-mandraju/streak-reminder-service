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