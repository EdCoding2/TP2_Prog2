package ca.cegep.biblio.util;

import java.util.UUID;

public class IdGenerator {

    // Empêche l'instanciation — classe utilitaire statique uniquement
    private IdGenerator() {}

    // Génère un ID unique pour un exemplaire
    public static String genererIdExemplaire() {
        return "EX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Génère un ID unique pour un usager
    public static String genererIdUsager() {
        return "US-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}