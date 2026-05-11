import AsyncStorage from "@react-native-async-storage/async-storage";

const AUTH_KEY = "auth_data";
const REMEMBER_EMAIL_KEY = "remember_email";

export type AuthData = {
  token: string;
  userId: number;
  role: string;
  mustChangePassword: boolean;
  username?: string;
  email?: string;
  truckId?: number;
};

export async function saveAuth(auth: AuthData): Promise<void> {
  await AsyncStorage.setItem(AUTH_KEY, JSON.stringify(auth));
}

export async function getAuth(): Promise<AuthData | null> {
  try {
    const value = await AsyncStorage.getItem(AUTH_KEY);
    return value ? JSON.parse(value) : null;
  } catch (error) {
    console.log("Erreur lecture auth_data:", error);
    return null;
  }
}

export async function removeAuth(): Promise<void> {
  await AsyncStorage.removeItem(AUTH_KEY);
}

export async function getToken(): Promise<string | null> {
  const auth = await getAuth();
  return auth?.token ?? null;
}

export async function getUserId(): Promise<number | null> {
  const auth = await getAuth();
  return auth?.userId ?? null;
}

export async function getRole(): Promise<string | null> {
  const auth = await getAuth();
  return auth?.role ?? null;
}

export async function getUsername(): Promise<string | null> {
  const auth = await getAuth();
  return auth?.username ?? null;
}

export async function getEmail(): Promise<string | null> {
  const auth = await getAuth();
  return auth?.email ?? null;
}

export async function getMustChangePassword(): Promise<boolean> {
  const auth = await getAuth();
  return auth?.mustChangePassword ?? false;
}

export async function isLoggedIn(): Promise<boolean> {
  const token = await getToken();
  return !!token;
}

export async function saveTruckId(truckId: number | string): Promise<void> {
  const auth = await getAuth();

  if (!auth) return;

  await saveAuth({
    ...auth,
    truckId: Number(truckId),
  });
}

export async function getTruckId(): Promise<number | null> {
  const auth = await getAuth();
  return auth?.truckId ?? null;
}

export async function saveRememberedEmail(email: string): Promise<void> {
  await AsyncStorage.setItem(REMEMBER_EMAIL_KEY, email);
}

export async function getRememberedEmail(): Promise<string | null> {
  return AsyncStorage.getItem(REMEMBER_EMAIL_KEY);
}

export async function removeRememberedEmail(): Promise<void> {
  await AsyncStorage.removeItem(REMEMBER_EMAIL_KEY);
}
