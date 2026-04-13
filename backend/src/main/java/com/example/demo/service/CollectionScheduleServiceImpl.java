package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class CollectionScheduleServiceImpl implements CollectionScheduleService {

    @Override
    public boolean isCollectionAllowed(RoutingBinDto bin) {
        if (bin == null || bin.getWasteType() == null || bin.getWasteType().isBlank()) {
            return false;
        }

        String wasteType = normalize(bin.getWasteType());
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();

        if ("GRAY".equals(wasteType) || "GREEN".equals(wasteType)) {
            return !now.isBefore(LocalTime.of(16, 0))
                    && !now.isAfter(LocalTime.of(23, 0));
        }

        if ("YELLOW".equals(wasteType)) {
            boolean allowedDay = today == DayOfWeek.MONDAY
                    || today == DayOfWeek.WEDNESDAY
                    || today == DayOfWeek.FRIDAY;

            boolean allowedTime = !now.isBefore(LocalTime.of(15, 30))
                    && !now.isAfter(LocalTime.of(22, 30));

            return allowedDay && allowedTime;
        }

        if ("WHITE".equals(wasteType)) {
            boolean allowedDay = today == DayOfWeek.MONDAY
                    || today == DayOfWeek.WEDNESDAY;

            boolean allowedTime = !now.isBefore(LocalTime.of(14, 0))
                    && !now.isAfter(LocalTime.of(20, 0));

            return allowedDay && allowedTime;
        }

        return true;
    }

    @Override
    public String explainCollectionWindow(RoutingBinDto bin) {
        if (bin == null || bin.getWasteType() == null || bin.getWasteType().isBlank()) {
            return "Type de bac non renseigné. La fenêtre de collecte ne peut pas être déterminée.";
        }

        String wasteType = normalize(bin.getWasteType());

        if ("GRAY".equals(wasteType) || "GREEN".equals(wasteType)) {
            return "Collecte autorisée tous les jours entre 16h00 et 23h00.";
        }

        if ("YELLOW".equals(wasteType)) {
            return "Collecte autorisée uniquement le lundi, mercredi et vendredi entre 15h30 et 22h30.";
        }

        if ("WHITE".equals(wasteType)) {
            return "Collecte autorisée uniquement le lundi et mercredi entre 14h00 et 20h00.";
        }

        return "Aucune règle de collecte n'est définie pour ce type de bac.";
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}