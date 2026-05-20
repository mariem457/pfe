import * as Location from "expo-location";

export type LocationRequirementResult = {
  ok: boolean;
  message?: string;
};

export async function requireDriverLocation(): Promise<LocationRequirementResult> {
  const servicesEnabled = await Location.hasServicesEnabledAsync();

  if (!servicesEnabled) {
    return {
      ok: false,
      message: "Activez la localisation du téléphone pour accéder à l'application chauffeur.",
    };
  }

  const permission = await Location.requestForegroundPermissionsAsync();

  if (permission.status !== "granted") {
    return {
      ok: false,
      message: "Autorisez l'accès à la localisation pour accéder à l'application chauffeur.",
    };
  }

  return { ok: true };
}
