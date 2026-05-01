package com.example.demo.dto.routing;

public enum BinDecisionCategory {

    MANDATORY("MANDATORY", "Bac prioritaire à collecter immédiatement"),
    OPPORTUNISTIC("OPPORTUNISTIC", "Bac intéressant à collecter si la tournée le permet"),
    REPORTABLE("REPORTABLE", "Bac non prioritaire, à surveiller ou à reporter");

    private final String code;
    private final String descriptionFr;

    BinDecisionCategory(String code, String descriptionFr) {
        this.code = code;
        this.descriptionFr = descriptionFr;
    }

    public String getCode() {
        return code;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    public static BinDecisionCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        for (BinDecisionCategory category : values()) {
            if (category.code.equalsIgnoreCase(code.trim())) {
                return category;
            }
        }

        return null;
    }
}