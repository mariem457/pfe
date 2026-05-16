import { BASE_URL } from "./api";
import { getToken } from "./storage";

export type DriverNotificationResponseType =
  | "POSITION_CONFIRMED"
  | "PROBLEM_RESOLVED"
  | "NEED_ASSISTANCE";

export async function getDriverNotifications() {
  const token = await getToken();

  const res = await fetch(`${BASE_URL}/api/driver/notifications`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!res.ok) throw new Error("Impossible de charger les notifications");

  return res.json();
}

export async function markDriverNotificationAsRead(notificationId: number) {
  const token = await getToken();

  const res = await fetch(
    `${BASE_URL}/api/driver/notifications/${notificationId}/read`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!res.ok) throw new Error("Impossible de marquer la notification comme lue");

  return res.json();
}

export async function respondToDriverNotification(
  notificationId: number,
  response: DriverNotificationResponseType
) {
  const token = await getToken();

  const res = await fetch(
    `${BASE_URL}/api/driver/notifications/${notificationId}/respond`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ response }),
    }
  );

  if (!res.ok) throw new Error(await res.text());

  return res.json();
}