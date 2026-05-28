package ca.cegep.biblio.persistence;

import ca.cegep.biblio.model.*;
import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JsonPersistence {

    private final Gson gson;

    public JsonPersistence() {
        this.gson = new GsonBuilder()
                // Adaptateur pour LocalDate
                .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                    @Override
                    public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                        return new JsonPrimitive(src.toString());
                    }
                })
                .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
                    @Override
                    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                            throws JsonParseException {
                        return LocalDate.parse(json.getAsString());
                    }
                })
                // Adaptateur pour le polymorphisme Usager
                .registerTypeAdapter(Usager.class, new UsagerAdapter())
                .setPrettyPrinting()
                .create();
    }

    // -------------------------------------------------------------------------
    // Sauvegarde
    // -------------------------------------------------------------------------

    public void sauvegarder(List<Exemplaire> exemplaires, List<Usager> usagers,
                             List<Emprunt> emprunts, String cheminFichier) throws IOException {
        DonneesJson donnees = new DonneesJson(exemplaires, usagers, emprunts);
        String json = gson.toJson(donnees);

        // Créer le dossier si inexistant
        Path chemin = Paths.get(cheminFichier);
        if (chemin.getParent() != null) {
            Files.createDirectories(chemin.getParent());
        }

        Files.writeString(chemin, json);
    }

    // -------------------------------------------------------------------------
    // Chargement
    // -------------------------------------------------------------------------

    public DonneesJson charger(String cheminFichier) throws IOException {
        Path chemin = Paths.get(cheminFichier);

        if (!Files.exists(chemin)) {
            return null;
        }

        String json = Files.readString(chemin);
        DonneesJson donnees = gson.fromJson(json, DonneesJson.class);

        // Reconstruire les références croisées Emprunt → Exemplaire et Emprunt → Usager
        if (donnees != null && donnees.getEmprunts() != null) {
            for (Emprunt emprunt : donnees.getEmprunts()) {
                // Retrouver l'exemplaire par idExemplaire
                Exemplaire ex = donnees.getExemplaires().stream()
                        .filter(e -> e.getIdExemplaire().equals(
                                emprunt.getExemplaire().getIdExemplaire()))
                        .findFirst()
                        .orElse(null);

                // Retrouver l'usager par id
                Usager us = donnees.getUsagers().stream()
                        .filter(u -> u.getId().equals(
                                emprunt.getUsager().getId()))
                        .findFirst()
                        .orElse(null);

                if (ex != null) emprunt.setExemplaire(ex);
                if (us != null) {
                    emprunt.setUsager(us);
                    us.getEmpruntsEnCours().add(emprunt);
                }
            }
        }

        return donnees;
    }

    // -------------------------------------------------------------------------
    // Classe conteneur JSON
    // -------------------------------------------------------------------------

    public static class DonneesJson {
        private List<Exemplaire> exemplaires;
        private List<Usager> usagers;
        private List<Emprunt> emprunts;

        public DonneesJson() {
            this.exemplaires = new ArrayList<>();
            this.usagers     = new ArrayList<>();
            this.emprunts    = new ArrayList<>();
        }

        public DonneesJson(List<Exemplaire> exemplaires, List<Usager> usagers, List<Emprunt> emprunts) {
            this.exemplaires = exemplaires;
            this.usagers     = usagers;
            this.emprunts    = emprunts;
        }

        public List<Exemplaire> getExemplaires() { return exemplaires; }
        public List<Usager> getUsagers()         { return usagers; }
        public List<Emprunt> getEmprunts()       { return emprunts; }
    }

    // -------------------------------------------------------------------------
    // Adaptateur polymorphisme Usager (Etudiant / Professeur / Visiteur)
    // -------------------------------------------------------------------------

    private static class UsagerAdapter implements JsonDeserializer<Usager>, JsonSerializer<Usager> {

        private static final String TYPE_FIELD = "typeUsager";

        @Override
        public JsonElement serialize(Usager src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = context.serialize(src, src.getClass()).getAsJsonObject();
            obj.addProperty(TYPE_FIELD, src.getClass().getSimpleName());
            return obj;
        }

        @Override
        public Usager deserialize(JsonElement json, Type typeOfT,
                                  JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get(TYPE_FIELD).getAsString();

            if ("Etudiant".equals(type)) {
                return context.deserialize(obj, Etudiant.class);
            } else if ("Professeur".equals(type)) {
                return context.deserialize(obj, Professeur.class);
            } else if ("Visiteur".equals(type)) {
                return context.deserialize(obj, Visiteur.class);
            } else {
                throw new JsonParseException("Type usager inconnu : " + type);
            }
        }
    }
}