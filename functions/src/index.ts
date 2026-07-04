import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

// ── Channel routing ─────────────────────────────────────────────────
// Maps notification type → Android notification channel ID.
// Must match NotificationChannelHelper constants on the client.
function channelForType(type: string): string {
  switch (type) {
    case "REQUEST":
    case "BLOOD_REQUEST":
    case "REQUEST_ACCEPTED":
    case "DONATION":
    case "DONATION_CONFIRMED":
      return "blood_requests";
    case "JOIN":
    case "COMMUNITY_JOIN_REQUEST":
    case "JOIN_REQUEST_ACCEPTED":
    case "JOIN_REQUEST_REJECTED":
    case "ADMIN":
    case "COMMUNITY_BROADCAST":
      return "community";
    case "SUPERADMIN_ANNOUNCEMENT":
      return "announcements";
    default:
      return "spondon_notifications";
  }
}

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

    // Check if this is a topic broadcast
    const isTopic = userId.startsWith("topic:");
    let fcmToken: string | undefined;

    if (!isTopic) {
      // Look up the target user's FCM token
      const userDoc = await db.collection("users").doc(userId).get();
      if (!userDoc.exists) {
        console.warn(`User ${userId} not found, skipping FCM push.`);
        return;
      }

      fcmToken = userDoc.data()?.fcmToken as string | undefined;
      if (!fcmToken) {
        console.warn(`User ${userId} has no FCM token, skipping push.`);
        return;
      }
    }

    const channelId = channelForType(type);

    // Send a data-only FCM message (no "notification" key).
    const messagePayload = {
      data: {
        title,
        body,
        type,
        deepLink,
        channelId,
        notificationId: event.params.notificationId,
        // Pass through extra fields for notification actions
        ...(data.communityId ? { communityId: data.communityId as string } : {}),
        ...(data.requesterId ? { requesterId: data.requesterId as string } : {}),
        ...(data.requestId ? { requestId: data.requestId as string } : {}),
      },
      android: {
        priority: "high" as const,
      },
    };

    const message: admin.messaging.Message = isTopic 
      ? { ...messagePayload, topic: userId.replace("topic:", "") }
      : { ...messagePayload, token: fcmToken! };

    try {
      await admin.messaging().send(message);
      if (isTopic) {
        console.log(`FCM push sent to topic ${userId}`);
      } else {
        console.log(`FCM push sent to user ${userId} (token: ${fcmToken!.substring(0, 10)}...)`);
      }

      // Delete non-admin notifications immediately after sending FCM
      if (type !== "ADMIN" && type !== "SUPERADMIN_ANNOUNCEMENT") {
        await snapshot.ref.delete();
        console.log(`Deleted non-admin notification document: ${event.params.notificationId}`);
      }
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
