import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

/**
 * Cloud Function: Send FCM push when a notification document is created.
 *
 * Trigger: Firestore `notifications/{notificationId}` — onCreate
 *
 * Flow:
 *  1. Read the new notification document (userId, title, body, type, deepLink)
 *  2. Look up the target user's FCM token from `users/{userId}`
 *  3. Send a **data-only** FCM message so `onMessageReceived` is always
 *     called on the client — both in foreground and background
 */
export const sendNotificationOnCreate = onDocumentCreated(
  "notifications/{notificationId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data();
    const userId = data.userId as string | undefined;
    const title = (data.title as string) || "Spondon";
    const body = (data.body as string) || "";
    const type = (data.type as string) || "REQUEST";
    const deepLink = (data.deepLink as string) || "";

    if (!userId) {
      console.warn("Notification document missing userId, skipping FCM push.");
      return;
    }

    // Look up the target user's FCM token
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
      console.warn(`User ${userId} not found, skipping FCM push.`);
      return;
    }

    const fcmToken = userDoc.data()?.fcmToken as string | undefined;
    if (!fcmToken) {
      console.warn(`User ${userId} has no FCM token, skipping push.`);
      return;
    }

    // Send a data-only FCM message (no "notification" key).
    // This ensures onMessageReceived() is called on the Android client
    // regardless of whether the app is in foreground or background.
    const message: admin.messaging.Message = {
      token: fcmToken,
      data: {
        title,
        body,
        type,
        deepLink,
        notificationId: event.params.notificationId,
      },
      // Android-specific config for high priority delivery
      android: {
        priority: "high",
      },
    };

    try {
      await admin.messaging().send(message);
      console.log(`FCM push sent to user ${userId} (token: ${fcmToken.substring(0, 10)}...)`);
    } catch (error: unknown) {
      // If the token is invalid/expired, clean it up
      if (
        error instanceof Error &&
        "code" in error &&
        ((error as { code: string }).code === "messaging/invalid-registration-token" ||
          (error as { code: string }).code === "messaging/registration-token-not-registered")
      ) {
        console.warn(`Removing stale FCM token for user ${userId}`);
        await db.collection("users").doc(userId).update({ fcmToken: "" });
      } else {
        console.error(`Failed to send FCM to user ${userId}:`, error);
      }
    }
  }
);
