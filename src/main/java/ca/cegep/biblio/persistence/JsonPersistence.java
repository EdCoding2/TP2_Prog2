package ca.cegep.biblio.persistence;

import ca.cegep.biblio.model.*;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JsonPersistence {

    private final Gson gson;

    public JsonPersistence() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, t, ctx) ->
                                new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, t, ctx) ->
                                LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    // -------------------------------------------------------------------------
    // DTOs — objets plats sans références circulaires
    // -------------------------------------------------------------------------

    // DTO Exemplaire
    private static class ExemplaireDto {
        String idExemplaire, titre, auteur, isbn;
        String statut, etatPhysique;
        LocalDate dateDisponibilite;

        static ExemplaireDto from(Exemplaire e) {
            ExemplaireDto d  = new ExemplaireDto();
            d.idExemplaire   = e.getIdExemplaire();
            d.titre          = e.getTitre();
            d.auteur         = e.getAuteur();
            d.isbn           = e.getIsbn();
            d.statut         = e.getStatut().name();
            d.etatPhysique   = e.getEtatPhysique().name();
            d.dateDisponibilite = e.getDateDisponibilite();
            return d;
        }

        Exemplaire toModel() {
            Exemplaire e = new Exemplaire(
                    idExemplaire, titre, auteur, isbn,
                    Statut.valueOf(statut),
                    EtatPhysique.valueOf(etatPhysique)
            );
            e.setDateDisponibilite(dateDisponibilite);
            return e;
        }
    }

    // DTO Usager — pas de liste d'emprunts (évite la récursion)
    private static class UsagerDto {
        String id, nom, type;

        static UsagerDto from(Usager u) {
            UsagerDto d = new UsagerDto();
            d.id   = u.getId();
            d.nom  = u.getNom();
            d.type = u.getClass().getSimpleName();
            return d;
        }

        Usager toModel() {
            return switch (type) {
                case "Professeur" -> new Professeur(nom, id);
                case "Visiteur"   -> new Visiteur(nom, id);
                default           -> new Etudiant(nom, id);
            };
        }
    }

    // DTO Emprunt — stocke seulement les IDs (pas les objets complets)
    private static class EmpruntDto {
        String idExemplaire, idUsager;
        LocalDate dateEmprunt, dateRetourPrevue;

        static EmpruntDto from(Emprunt e) {
            EmpruntDto d     = new EmpruntDto();
            d.idExemplaire   = e.getExemplaire().getIdExemplaire();
            d.idUsager       = e.getUsager().getId();
            d.dateEmprunt    = e.getDateEmprunt();
            d.dateRetourPrevue = e.getDateRetourPrevue();
            return d;
        }
    }

    // Conteneur JSON
    private static class SaveFile {
        List<ExemplaireDto> exemplaires = new ArrayList<>();
        List<UsagerDto>     usagers     = new ArrayList<>();
        List<EmpruntDto>    emprunts    = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Sauvegarde
    // -------------------------------------------------------------------------

    public void sauvegarder(List<Exemplaire> exemplaires, List<Usager> usagers,
                             List<Emprunt> emprunts, String cheminFichier)
            throws IOException {

        SaveFile save = new SaveFile();

        for (Exemplaire e : exemplaires)
            save.exemplaires.add(ExemplaireDto.from(e));

        for (Usager u : usagers)
            save.usagers.add(UsagerDto.from(u));

        for (Emprunt e : emprunts)
            save.emprunts.add(EmpruntDto.from(e));

        Path chemin = Paths.get(cheminFichier);
        if (chemin.getParent() != null)
            Files.createDirectories(chemin.getParent());

        Files.writeString(chemin, gson.toJson(save));
    }

    // -------------------------------------------------------------------------
    // Chargement
    // -------------------------------------------------------------------------

    public DonneesJson charger(String cheminFichier) throws IOException {
        Path chemin = Paths.get(cheminFichier);
        if (!Files.exists(chemin)) return null;

        String json = Files.readString(chemin);
        SaveFile save = gson.fromJson(json, SaveFile.class);
        if (save == null) return null;

        // Reconstruire les modèles
        List<Exemplaire> exemplaires = new ArrayList<>();
        for (ExemplaireDto d : save.exemplaires)
            exemplaires.add(d.toModel());

        List<Usager> usagers = new ArrayList<>();
        for (UsagerDto d : save.usagers)
            usagers.add(d.toModel());

        // Reconstruire les emprunts en résolvant les références par ID
        List<Emprunt> emprunts = new ArrayList<>();
        for (EmpruntDto d : save.emprunts) {
            Exemplaire ex = exemplaires.stream()
                    .filter(e -> e.getIdExemplaire().equals(d.idExemplaire))
                    .findFirst().orElse(null);
            Usager us = usagers.stream()
                    .filter(u -> u.getId().equals(d.idUsager))
                    .findFirst().orElse(null);

            if (ex != null && us != null) {
                Emprunt emprunt = new Emprunt(ex, us, d.dateEmprunt, d.dateRetourPrevue);
                emprunts.add(emprunt);
                // Reconstruire empruntsEnCours de l'usager
                us.getEmpruntsEnCours().add(emprunt);
            }
        }

        return new DonneesJson(exemplaires, usagers, emprunts);
    }

    // -------------------------------------------------------------------------
    // Conteneur de retour
    // -------------------------------------------------------------------------

    public static class DonneesJson {
        private final List<Exemplaire> exemplaires;
        private final List<Usager>     usagers;
        private final List<Emprunt>    emprunts;

        public DonneesJson(List<Exemplaire> exemplaires,
                           List<Usager> usagers,
                           List<Emprunt> emprunts) {
            this.exemplaires = exemplaires;
            this.usagers     = usagers;
            this.emprunts    = emprunts;
        }

        public DonneesJson() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public List<Exemplaire> getExemplaires() { return exemplaires; }
        public List<Usager>     getUsagers()     { return usagers; }
        public List<Emprunt>    getEmprunts()    { return emprunts; }
    }
}