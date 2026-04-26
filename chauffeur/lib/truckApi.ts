import * as Location from "expo-location";
import { getToken, getTruckId, getUserId } from "./storage";

const BASE_URL = "http://10.221.127.114:8081";

export async function sendTruckLocation() {
  const token = await getToken();
  const userId = await getUserId();

  if (!token || !userId) return;

  const { status } = await Location.requestForegroundPermissionsAsync();

  if (status !== "granted") return;

  const location = await Location.getCurrentPositionAsync({
    accuracy: Location.Accuracy.High,
  });

  const response = await fetch(`${BASE_URL}/api/truck-locations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      driverId: userId,
      lat: location.coords.latitude,
      lng: location.coords.longitude,
      speedKmh:
        location.coords.speed != null ? location.coords.speed * 3.6 : null,
      headingDeg: location.coords.heading ?? null,
    }),
  });

  if (!response.ok) {
    console.log("TRUCK LOCATION ERROR:", await response.text());
  }
}

export async function declareTruckIncident(params: {
  incidentType: string;
  description: string;
  lat: number | null;
  lng: number | null;
}) {
  const token = await getToken();
  const userId = await getUserId();
  const truckId = await getTruckId();

  if (!token || !userId || !truckId) {
    throw new Error("Session invalide ou camion non trouvé");
  }

  const response = await fetch(`${BASE_URL}/api/truck-incidents`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      truckId,
      missionId: null,
      incidentType: params.incidentType,
      severity: params.incidentType === "BREAKDOWN" ? "CRITICAL" : "HIGH",
      description: params.description,
      status: "OPEN",
      reportedByUserId: userId,
      autoDetected: false,
      lat: params.lat,
      lng: params.lng,
    }),
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  return response.json();
  
  
}
export async function getMyTruckIncidents() {
  const token = await getToken();
  const truckId = await getTruckId();

  if (!token || !truckId) {
    throw new Error("Session invalide ou camion non trouvé");
  }

  const response = await fetch(`${BASE_URL}/api/truck-incidents/truck/${truckId}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  return response.json();
}