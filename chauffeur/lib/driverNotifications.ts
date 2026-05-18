export type DriverNotificationType =
  | "MISSION_REASSIGNED"
  | "MISSION_CANCELLED"
  | "TRUCK_BREAKDOWN_HANDLED"
  | "SENSOR_BREAKDOWN_HANDLED"
  | "DELAY_DETECTED"
  | "INCIDENT_CONTACT";

export type DriverNotification = {
  id: number;
  type: DriverNotificationType;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;
  status?: "SENT" | "READ" | "RESPONDED";
  response?: "POSITION_CONFIRMED" | "PROBLEM_RESOLVED" | "NEED_ASSISTANCE" | null;
  incidentId?: number | null;
  truckId?: number | null;
  truckCode?: string | null;
  missionId?: number | null;
  respondedAt?: string | null;
};

function formatTruckCode(value?: string | null) {
  if (!value) return "Camion";
  return value.replace(/^TRUCK/i, "Camion");
}

function formatIncidentCode(value: string) {
  return value
    .replace(/GPS_LOST|GPS-LOST/g, "perte GPS")
    .replace(/BREAKDOWN/g, "panne")
    .replace(/FUEL_LOW|FUEL-LOW/g, "carburant faible")
    .replace(/TRAFFIC_BLOCK|TRAFFIC-BLOCK/g, "route bloquée")
    .replace(/DELAY/g, "retard")
    .replace(/_/g, " ")
    .replace(/-/g, " ");
}

export function getDriverNotificationText(notification: DriverNotification) {
  const truck = formatTruckCode(notification.truckCode);

  switch (notification.type) {
    case "INCIDENT_CONTACT":
      return {
        title: `${truck} - Besoin de confirmation`,
        message:
          notification.message?.trim() ||
          "La municipalité demande une confirmation sur un incident camion.",
      };
    case "TRUCK_BREAKDOWN_HANDLED":
      return {
        title: `${truck} - Panne camion traitée`,
        message:
          notification.message?.trim() ||
          "Le problème camion a été pris en charge par la municipalité.",
      };
    case "SENSOR_BREAKDOWN_HANDLED":
      return {
        title: "Capteur traité",
        message:
          notification.message?.trim() ||
          "Le problème capteur signalé a été pris en charge.",
      };
    case "DELAY_DETECTED":
      return {
        title: "Retard détecté",
        message:
          notification.message?.trim() ||
          "Un retard a été détecté sur votre tournée.",
      };
    case "MISSION_REASSIGNED":
      return {
        title: "Tournée mise à jour",
        message:
          notification.message?.trim() ||
          "Votre tournée a été modifiée. Consultez le tableau de bord.",
      };
    case "MISSION_CANCELLED":
      return {
        title: "Mission annulee",
        message:
          notification.message?.trim() ||
          "Votre mission a ete annulee. Le tableau de bord a ete mis a jour.",
      };
    default:
      return {
        title: formatIncidentCode(notification.title || "Notification"),
        message: formatIncidentCode(notification.message || ""),
      };
  }
}

export function isPhoneNotificationImportant(notification: DriverNotification) {
  const raw = `${notification.type} ${notification.title} ${notification.message}`.toUpperCase();

  return (
    notification.type === "INCIDENT_CONTACT" ||
    notification.type === "TRUCK_BREAKDOWN_HANDLED" ||
    raw.includes("GPS_LOST") ||
    raw.includes("GPS-LOST")
  );
}
