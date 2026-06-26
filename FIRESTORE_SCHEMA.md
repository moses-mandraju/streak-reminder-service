# Firestore Schema for Streak Reminder Service

This document records the exact Firestore collections and document schema used by the existing React app in `streak-checker`.

## Root collection and user parent path

The React application uses the following top-level structure:

- `users/{uid}/habits/{habitId}`
- `users/{uid}/deviceTokens/{token}`

The user document at `users/{uid}` is only a parent path in the observed app code. There is no explicit user document field schema written by `streak-checker` in the inspected source files.

## Habit document schema

Habit documents are stored under `users/{uid}/habits/{habitId}` and include the following fields:

- `title` (string)
- `emoji` (string)
- `createdDate` (string) - date key in format `yyyy-MM-dd`
- `currentStreak` (number)
- `longestStreak` (number)
- `lastCompletedDate` (string) - date key in format `yyyy-MM-dd`, empty string when never completed
- `completionHistory` (array of strings) - list of date keys in format `yyyy-MM-dd`

Reminder-related fields are stored directly on the habit document:

- `reminderEnabled` (boolean)
- `reminderTime` (string) - e.g. `09:00`
- `reminderFrequency` (string) - default `Daily`; other values may include `Weekly`
- `selectedDays` (array of strings) - used when `reminderFrequency` is not daily, e.g. `['monday', 'wednesday']`
- `notificationTitle` (string)
- `notificationMessage` (string)
- `reminderTimezone` (string) - IANA timezone name, default from the browser environment

Default reminder field values from the React app:

- `reminderEnabled: false`
- `reminderTime: '09:00'`
- `reminderFrequency: 'Daily'`
- `selectedDays: []`
- `notificationTitle: ''`
- `notificationMessage: ''`
- `reminderTimezone: <browser timezone>`

## Device token schema

Device tokens are stored under `users/{uid}/deviceTokens/{token}`. The token value is used as the Firestore document ID.

Each device token document contains:

- `token` (string)
- `createdAt` (Firestore server timestamp)
- `lastSeenAt` (Firestore server timestamp)

This design supports multiple devices per user.

## Notification-related code

The React app writes device tokens with `saveDeviceToken(uid, token)` in `src/firebase/messaging.js`.

It also uses a service worker in `src/sw.js` to display notifications from FCM:

- `payload.notification.title`
- `payload.notification.body`

The service worker code does not change the Firestore schema; it only receives push payloads.

## Firebase configuration

Firebase is configured in `src/firebase/config.js` using environment variables:

- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_STORAGE_BUCKET`
- `VITE_FIREBASE_MESSAGING_SENDER_ID`
- `VITE_FIREBASE_APP_ID`

Firebase Messaging uses `VITE_FIREBASE_VAPID_KEY` in `src/firebase/messaging.js`.

## How the Spring Boot service should interact with Firestore

The new `streak-reminder-service` should use the exact same collection paths and field names.

### User and device token access

- Query `users` collection by UID
- Read `users/{uid}/deviceTokens` subcollection
- Handle multiple token documents per user
- Use the `token` field and document ID interchangeably for FCM send targets

### Habit reminders

- Read habit documents from `users/{uid}/habits`
- Only process documents where `reminderEnabled == true`
- Use `reminderTime`, `reminderFrequency`, `selectedDays`, `reminderTimezone`, `notificationTitle`, and `notificationMessage` as stored on the habit document

### Reminder scheduling and timezone handling

- Convert the stored `reminderTime` and `reminderTimezone` into a ZonedDateTime or equivalent
- Evaluate due reminders on each scheduler tick
- For `Daily`, the reminder is due each day at the configured time in the stored timezone
- For `Weekly`, the reminder is due on any stored day in `selectedDays` at the configured local time

### Avoiding duplicate notifications

- Track which reminders have already been sent for the current expected send window
- Use a combination of habit ID, user UID, reminder time, and date to prevent duplicate sends

### Recommendation for missing data

- If `selectedDays` is empty and `reminderFrequency` is `Weekly`, do not send a notification
- If `notificationTitle` or `notificationMessage` is empty, the service should still function but can substitute a safe default text

## Observed limits of the existing schema

- No explicit `users/{uid}` profile fields were found in the inspected source files
- All notification and scheduling state is stored on habit documents
- Device tokens are stored separately per user in a Firestore subcollection

---

This document should guide the Spring Boot microservice implementation and ensure it reuses the React app’s Firestore schema exactly.