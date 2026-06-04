import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  Alert,
  ActivityIndicator,
  TouchableOpacity,
} from "react-native";
import { CameraView, useCameraPermissions } from "expo-camera";
import { useLocalSearchParams, router } from "expo-router";
import { BASE_URL } from "../../lib/api";
import { alertMessageFr } from "../../lib/alertMessages";
import { getToken } from "../../lib/storage";

export default function ScanScreen() {
  const [permission, requestPermission] = useCameraPermissions();
  const [scanned, setScanned] = useState(false);
  const [loading, setLoading] = useState(false);
  const [lastCode, setLastCode] = useState<string | null>(null);

  const { expectedBinCode, missionBinId, resumeIndex, isLastBin } = useLocalSearchParams<{
    expectedBinCode?: string;
    missionBinId?: string;
    resumeIndex?: string;
    isLastBin?: string;
  }>();

  const isLastMissionBin = isLastBin === "1";

  useEffect(() => {
    if (!permission) {
      requestPermission();
    }
  }, [permission, requestPermission]);

  function rescan() {
    setScanned(false);
    setLastCode(null);
  }

  function extractBinCode(raw: string) {
    const trimmed = raw.trim();

    try {
      const parsed = JSON.parse(trimmed);
      const value =
        parsed?.binCode ??
        parsed?.bin_code ??
        parsed?.code ??
        parsed?.id ??
        parsed?.binId;

      if (value != null) return String(value).trim();
    } catch {}

    try {
      const url = new URL(trimmed);
      const value =
        url.searchParams.get("binCode") ??
        url.searchParams.get("bin_code") ??
        url.searchParams.get("code") ??
        url.searchParams.get("id") ??
        url.pathname.split("/").filter(Boolean).pop();

      if (value) return value.trim();
    } catch {}

    return trimmed;
  }

  const handleScan = async ({ data }: { data: string }) => {
    if (scanned || loading) return;

    setScanned(true);
    const scannedCode = extractBinCode(data);
    setLastCode(scannedCode);

    try {
      if (expectedBinCode && scannedCode !== expectedBinCode.trim()) {
        Alert.alert(
          "QR code invalide",
          `Ce QR code ne correspond pas à cette poubelle.\n\nPoubelle attendue: ${expectedBinCode}`,
          [
            {
              text: "Quitter",
              style: "cancel",
              onPress: () => {
                if (missionBinId) {
                  router.replace({
                    pathname: "/route-map",
                    params: {
                      invalidScan: "1",
                      missionBinId,
                      resumeIndex: resumeIndex ?? "0",
                    },
                  });
                  return;
                }

                router.replace("/(tabs)/dashboard");
              },
            },
            { text: "Rescanner", onPress: rescan },
          ]
        );
        return;
      }

      setLoading(true);

      const token = await getToken();

      if (!token) {
        throw new Error("Utilisateur non connecté");
      }

      const res = await fetch(`${BASE_URL}/api/drivers/bin-scan`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          missionBinId: missionBinId ? Number(missionBinId) : null,
          binCode: scannedCode,
          driverNote: "collecte effectuée",
          issueType: null,
        }),
      });

      const rawText = await res.text();
      let result: any = null;

      try {
        result = rawText ? JSON.parse(rawText) : null;
      } catch {
        throw new Error(rawText || "Réponse invalide du serveur");
      }

      if (!res.ok) {
        throw new Error(result?.message || "Erreur lors du scan");
      }

      Alert.alert(
        "Succès",
        alertMessageFr(
          result?.message,
          `Poubelle ${result?.binCode || scannedCode} collectée avec succès`
        ),
        [
          {
            text: isLastMissionBin ? "OK" : "Continuer route",
            onPress: () => {
              if (missionBinId) {
                router.replace({
                  pathname: "/route-map",
                  params: {
                    actionDone: "collect",
                    collectedBinId: missionBinId,
                    missionComplete: isLastMissionBin ? "1" : "0",
                  },
                });
                return;
              }

              router.replace("/(tabs)/dashboard");
            },
          },
        ]
      );
    } catch (err: any) {
      Alert.alert("Erreur", alertMessageFr(err.message, "Une erreur est survenue"), [
        {
          text: "Quitter",
          style: "cancel",
          onPress: () => {
            if (missionBinId) {
              router.replace({
                pathname: "/route-map",
                params: {
                  invalidScan: "1",
                  missionBinId,
                  resumeIndex: resumeIndex ?? "0",
                },
              });
              return;
            }

            router.replace("/(tabs)/dashboard");
          },
        },
        { text: "Rescanner", onPress: rescan },
      ]);
    } finally {
      setLoading(false);
    }
  };

  if (!permission) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (!permission.granted) {
    return (
      <View style={styles.center}>
        <Text style={styles.infoText}>Permission caméra requise</Text>
        <TouchableOpacity style={styles.button} onPress={requestPermission}>
          <Text style={styles.buttonText}>Autoriser la caméra</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <CameraView
        style={StyleSheet.absoluteFillObject}
        barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
        onBarcodeScanned={handleScan}
      />

      <View style={styles.overlay}>
        <Text style={styles.title}>Scanner QR Code</Text>

        {expectedBinCode ? (
          <Text style={styles.codeText}>Poubelle attendue: {expectedBinCode}</Text>
        ) : null}

        {loading && <ActivityIndicator color="#fff" size="large" />}

        {lastCode && (
          <Text style={styles.codeText}>Code détecté: {lastCode}</Text>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  overlay: {
    position: "absolute",
    bottom: 40,
    left: 20,
    right: 20,
    backgroundColor: "rgba(15,23,42,0.82)",
    borderRadius: 24,
    padding: 22,
    alignItems: "center",
  },
  title: {
    color: "#fff",
    fontSize: 22,
    fontWeight: "900",
    marginBottom: 12,
    textAlign: "center",
  },
  codeText: {
    color: "#fff",
    fontSize: 16,
    marginTop: 10,
    marginBottom: 12,
    textAlign: "center",
    fontWeight: "700",
  },
  button: {
    backgroundColor: "#19C37D",
    paddingVertical: 12,
    paddingHorizontal: 18,
    borderRadius: 12,
    marginTop: 10,
  },
  buttonText: {
    color: "#fff",
    fontWeight: "800",
  },
  center: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  infoText: {
    fontSize: 16,
    marginBottom: 16,
  },
});
