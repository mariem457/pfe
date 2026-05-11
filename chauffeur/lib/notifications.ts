import { BASE_URL } from "./api";
import { getToken } from "./storage";

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
