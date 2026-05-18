const WASTE_TYPE_LABELS: Record<string, string> = {
  GRAY: "Ordures ménagères",
  GREY: "Ordures ménagères",
  GREEN: "Ordures ménagères",
  YELLOW: "Emballages recyclables",
  WHITE: "Verre",
};

export function formatWasteTypeFr(value?: string | null) {
  const normalized = value?.trim().toUpperCase();

  if (!normalized) {
    return "Non précisé";
  }

  return WASTE_TYPE_LABELS[normalized] ?? value;
}
