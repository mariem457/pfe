import * as Location from "expo-location";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { BASE_URL } from "./api";
import { getToken, getTruckId, getUserId } from "./storage";

type TruckLocationPayload = {
  driverId: number;
  lat: number;
  lng: number;
  speedKmh: number | null;
  headingDeg: number | null;
  timestamp: string;
};

const OFFLINE_TRUCK_LOCATIONS_KEY = "offline_truck_locations";
const MAX_OFFLINE_TRUCK_LOCATIONS = 500;

async function fetchWithTimeout(
  url: string,
  options: RequestInit = {},
  timeoutMs = 10000
) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } catch (error: any) {
    if (error?.name === "AbortError") {
      throw new Error("Le serveur ne répond pas. Vérifiez la connexion.");
    }

    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

export async function sendTruckLocation() {
  const token = await getToken();
  const userId = await getUserId();

  if (!token || !userId) return;

  const { status } = await Location.requestForegroundPermissionsAsync();

  if (status !== "granted") return;

  const location = await Location.getCurrentPositionAsync({
    accuracy: Location.Accuracy.High,
  });

  await sendTruckLocationPayload({
    driverId: Number(userId),
    lat: location.coords.latitude,
    lng: location.coords.longitude,
    speedKmh:
      location.coords.speed != null ? location.coords.speed * 3.6 : null,
    headingDeg: location.coords.heading ?? null,
    timestamp: new Date(location.timestamp).toISOString(),
  });
}

export async function sendTruckLocationPoint(params: {
  lat: number;
  lng: number;
  speedKmh?: number | null;
  headingDeg?: number | null;
  timestamp?: string;
}) {
  const userId = await getUserId();

  if (!userId) return;

  await sendTruckLocationPayload({
    driverId: Number(userId),
    lat: params.lat,
    lng: params.lng,
    speedKmh: params.speedKmh ?? null,
    headingDeg: params.headingDeg ?? null,
    timestamp: params.timestamp ?? new Date().toISOString(),
  });
}

async function sendTruckLocationPayload(payload: TruckLocationPayload) {
  const token = await getToken();

  if (!token) return;

  const queuedLocations = await getQueuedTruckLocations();

  if (queuedLocations.length > 0) {
    const flushed = await flushQueuedTruckLocations(token, queuedLocations);
    if (!flushed) {
      await enqueueTruckLocation(payload);
      return;
    }
  }

  try {
    await postTruckLocation(token, payload);
  } catch (error) {
    console.log("TRUCK LOCATION OFFLINE QUEUED:", error);
    await enqueueTruckLocation(payload);
  }
}

async function postTruckLocation(token: string, payload: TruckLocationPayload) {
  const response = await fetchWithTimeout(`${BASE_URL}/api/truck-locations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }
}

async function flushQueuedTruckLocations(
  token: string,
  queuedLocations: TruckLocationPayload[]
) {
  const remaining = [...queuedLocations];

  while (remaining.length > 0) {
    try {
      await postTruckLocation(token, remaining[0]);
      remaining.shift();
      await saveQueuedTruckLocations(remaining);
    } catch (error) {
      console.log("TRUCK LOCATION QUEUE SYNC ERROR:", error);
      await saveQueuedTruckLocations(remaining);
      return false;
    }
  }

  return true;
}

async function enqueueTruckLocation(payload: TruckLocationPayload) {
  const queuedLocations = await getQueuedTruckLocations();
  await saveQueuedTruckLocations(
    [...queuedLocations, payload].slice(-MAX_OFFLINE_TRUCK_LOCATIONS)
  );
}

async function getQueuedTruckLocations(): Promise<TruckLocationPayload[]> {
  try {
    const raw = await AsyncStorage.getItem(OFFLINE_TRUCK_LOCATIONS_KEY);
    const parsed = raw ? JSON.parse(raw) : [];

    return Array.isArray(parsed)
      ? parsed.filter(isTruckLocationPayload)
      : [];
  } catch (error) {
    console.log("TRUCK LOCATION QUEUE READ ERROR:", error);
    return [];
  }
}

async function saveQueuedTruckLocations(payloads: TruckLocationPayload[]) {
  await AsyncStorage.setItem(
    OFFLINE_TRUCK_LOCATIONS_KEY,
    JSON.stringify(payloads)
  );
}

function isTruckLocationPayload(value: any): value is TruckLocationPayload {
  return (
    Number.isFinite(value?.driverId) &&
    Number.isFinite(value?.lat) &&
    Number.isFinite(value?.lng) &&
    typeof value?.timestamp === "string"
  );
}

export async function getCurrentMissionId(): Promise<number | null> {
  const token = await getToken();
  const userId = await getUserId();

  if (!token || !userId) return null;

  const response = await fetchWithTimeout(`${BASE_URL}/api/drivers/${userId}/my-bins`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    console.log("CURRENT MISSION ERROR:", await response.text());
    return null;
  }

  const data = await response.json();

  if (!Array.isArray(data) || data.length === 0) {
    return null;
  }

  return data[0]?.missionId ?? null;
}

export async function declareTruckIncident(params: {
  missionId?: number | null;
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

  const response = await fetchWithTimeout(
    `${BASE_URL}/api/truck-incidents`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        truckId,
        missionId: params.missionId ?? null,
        incidentType: params.incidentType,
        severity: params.incidentType === "BREAKDOWN" ? "CRITICAL" : "HIGH",
        description: params.description,
        status: "OPEN",
        reportedByUserId: userId,
        autoDetected: false,
        lat: params.lat,
        lng: params.lng,
      }),
    },
    30000
  );

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

