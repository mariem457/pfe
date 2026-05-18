const ENGLISH_MESSAGE_LABELS: Array<[RegExp, string]> = [
  [/cannot start a cancelled mission|cancelled mission/i, "Impossible de démarrer une mission annulée."],
  [/bad credentials|invalid credentials|incorrect password/i, "Email ou mot de passe incorrect."],
  [/user not found|account not found/i, "Compte introuvable."],
  [/email.*already|already.*email/i, "Cette adresse e-mail est déjà utilisée."],
  [/username.*already|already.*username/i, "Ce nom d'utilisateur est déjà utilisé."],
  [/phone.*already|already.*phone/i, "Ce numéro de téléphone est déjà utilisé."],
  [/invalid.*code|code.*invalid|expired.*code|code.*expired/i, "Code invalide ou expiré."],
  [/unauthorized|forbidden|access denied/i, "Accès refusé. Veuillez vous reconnecter."],
  [/not found/i, "Élément introuvable."],
  [/network request failed|failed to fetch/i, "Impossible de joindre le serveur."],
  [/internal server error|server error/i, "Erreur serveur. Veuillez réessayer."],
  [/invalid request|bad request/i, "Demande invalide."],
];

const ENGLISH_WORDS =
  /\b(error|failed|failure|invalid|unable|cannot|can't|please|missing|required|not found|already|unauthorized|forbidden|network|successfully|created|updated|deleted|expired|credentials)\b/i;

function extractMessageText(message: string) {
  const trimmed = message.trim();

  if (!trimmed.startsWith("{")) {
    return trimmed;
  }

  try {
    const parsed = JSON.parse(trimmed) as {
      message?: unknown;
      error?: unknown;
      detail?: unknown;
      title?: unknown;
    };

    for (const value of [parsed.message, parsed.detail, parsed.title, parsed.error]) {
      if (typeof value === "string" && value.trim()) {
        return value.trim();
      }
    }
  } catch {
    return trimmed;
  }

  return trimmed;
}

export function alertMessageFr(
  message: unknown,
  fallback = "Une erreur est survenue."
) {
  if (typeof message !== "string") {
    return fallback;
  }

  const trimmed = extractMessageText(message);

  if (!trimmed) {
    return fallback;
  }

  for (const [pattern, label] of ENGLISH_MESSAGE_LABELS) {
    if (pattern.test(trimmed)) {
      return label;
    }
  }

  return ENGLISH_WORDS.test(trimmed) ? fallback : trimmed;
}
