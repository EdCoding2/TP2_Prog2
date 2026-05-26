package ca.cegep.biblio.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class DateUtils {

    // Empêche l'instanciation — classe utilitaire statique uniquement
    private DateUtils() {}

    // Vérifie si une date est un jour ouvrable (lundi à vendredi)
    public static boolean estJourOuvrable(LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        return jour != DayOfWeek.SATURDAY && jour != DayOfWeek.SUNDAY;
    }

    // Retourne le prochain jour ouvrable à partir d'une date (inclut la date si ouvrable)
    public static LocalDate prochainJourOuvrable(LocalDate date) {
        LocalDate result = date;
        while (!estJourOuvrable(result)) {
            result = result.plusDays(1);
        }
        return result;
    }

    // Ajoute un nombre de jours ouvrables à une date (saute les weekends)
    public static LocalDate ajouterJoursOuvrables(LocalDate date, int jours) {
        LocalDate result = date;
        int joursAjoutes = 0;
        while (joursAjoutes < jours) {
            result = result.plusDays(1);
            if (estJourOuvrable(result)) {
                joursAjoutes++;
            }
        }
        return result;
    }
}