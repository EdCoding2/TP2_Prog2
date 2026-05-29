package ca.cegep.biblio.service;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.persistence.JsonPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BibliothequeService {

    private List<Exemplaire> exemplaires;
    private List<Usager> usagers;
    private List<Emprunt> emprunts;

    private final EmpruntService empruntService;
    private final RechercheService rechercheService;
    private final JsonPersistence jsonPersistence;

    
    private static final String CHEMIN_JSON = construireChemin();

    private static String construireChemin() {
        // Cherche le dossier data/ depuis la racine du projet
        java.io.File dossierData = new java.io.File("data");
        if (!dossierData.exists()) {
            dossierData.mkdirs();
        }
        return dossierData.getAbsolutePath()
                + java.io.File.separator + "sauvegarde.json";
    }

    public BibliothequeService() {
        this.exemplaires    = new ArrayList<>();
        this.usagers        = new ArrayList<>();
        this.emprunts       = new ArrayList<>();
        this.jsonPersistence   = new JsonPersistence();
        this.empruntService    = new EmpruntService(exemplaires, usagers, emprunts);
        this.rechercheService  = new RechercheService(exemplaires);
    }

    // -------------------------------------------------------------------------
    // CRUD Exemplaires
    // -------------------------------------------------------------------------

    public synchronized void ajouterExemplaire(Exemplaire exemplaire) {
        exemplaires.add(exemplaire);
    }

    public synchronized boolean modifierExemplaire(String idExemplaire, String nouveauTitre,
                                                    String nouvelAuteur, String nouvelIsbn) {
        return exemplaires.stream()
                .filter(e -> e.getIdExemplaire().equals(idExemplaire))
                .findFirst()
                .map(e -> {
                    e.setTitre(nouveauTitre);
                    e.setAuteur(nouvelAuteur);
                    e.setIsbn(nouvelIsbn);
                    return true;
                })
                .orElse(false);
    }

    public synchronized boolean supprimerExemplaire(String idExemplaire) {
        // Vérifier que l'exemplaire n'est pas emprunté
        boolean estEmprunte = emprunts.stream()
                .anyMatch(e -> e.getExemplaire().getIdExemplaire().equals(idExemplaire));

        if (estEmprunte) {
            System.out.println("Impossible de supprimer un exemplaire actuellement emprunté.");
            return false;
        }

        return exemplaires.removeIf(e -> e.getIdExemplaire().equals(idExemplaire));
    }

    // -------------------------------------------------------------------------
    // CRUD Usagers
    // -------------------------------------------------------------------------

    public synchronized void ajouterUsager(Usager usager) {
        usagers.add(usager);
    }

    public synchronized boolean modifierUsager(String idUsager, String nouveauNom) {
        return usagers.stream()
                .filter(u -> u.getId().equals(idUsager))
                .findFirst()
                .map(u -> {
                    u.setNom(nouveauNom);
                    return true;
                })
                .orElse(false);
    }

    public synchronized boolean supprimerUsager(String idUsager) {
        // Refuser la suppression si l'usager a des emprunts actifs
        return usagers.stream()
                .filter(u -> u.getId().equals(idUsager))
                .findFirst()
                .map(u -> {
                    if (!u.getEmpruntsEnCours().isEmpty()) {
                        System.out.println("Impossible de supprimer un usager avec des emprunts actifs.");
                        return false;
                    }
                    usagers.remove(u);
                    return true;
                })
                .orElse(false);
    }

    // -------------------------------------------------------------------------
    // Délégation vers EmpruntService
    // -------------------------------------------------------------------------

    public boolean emprunter(Usager usager, String isbn) {
        return empruntService.emprunter(usager, isbn);
    }

    public boolean retourner(Usager usager, Exemplaire exemplaire, EtatPhysique etat) {
        return empruntService.retourner(usager, exemplaire, etat);
    }

    public List<Emprunt> getEmpruntsEnRetard() {
        return empruntService.getEmpruntsEnRetard();
    }

    public int getCompteurEmprunts() {
        return empruntService.getCompteurEmprunts();
    }

    public java.util.concurrent.atomic.AtomicInteger getAtomicCompteur() {
        return empruntService.getAtomicCompteur();
    }

    // -------------------------------------------------------------------------
    // Délégation vers RechercheService
    // -------------------------------------------------------------------------

    public List<Exemplaire> rechercherParTitre(String titre) {
        return rechercheService.rechercherParTitre(titre);
    }

    public List<Exemplaire> rechercherParAuteur(String auteur) {
        return rechercheService.rechercherParAuteur(auteur);
    }

    public List<Exemplaire> rechercherParISBN(String isbn) {
        return rechercheService.rechercherParISBN(isbn);
    }

    public List<Exemplaire> getDisponibles() {
        return rechercheService.getDisponibles();
    }

    public List<Exemplaire> getEnReparation() {
        return rechercheService.getEnReparation();
    }

    public java.util.Optional<java.time.LocalDate> getDateRetourPlusProche(String isbn) {
        return rechercheService.getDateRetourPlusProche(isbn, emprunts);
    }

    // -------------------------------------------------------------------------
    // Persistance
    // -------------------------------------------------------------------------

    public void chargerDonnees() {
        try {
            JsonPersistence.DonneesJson donnees = jsonPersistence.charger(CHEMIN_JSON);
            if (donnees != null) {
                exemplaires.addAll(donnees.getExemplaires());
                usagers.addAll(donnees.getUsagers());
                emprunts.addAll(donnees.getEmprunts());
                System.out.println("Données chargées avec succès.");
            }
        } catch (IOException e) {
            System.out.println("Aucune sauvegarde trouvée — démarrage avec une collection vide.");
        }
    }

    public void sauvegarderDonnees() {
        try {
            jsonPersistence.sauvegarder(exemplaires, usagers, emprunts, CHEMIN_JSON);
            System.out.println("Données sauvegardées avec succès.");
        } catch (IOException e) {
            System.out.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Getters des collections (utiles pour les tests et l'interface)
    // -------------------------------------------------------------------------

    public List<Exemplaire> getExemplaires()  { return exemplaires; }
    public List<Usager> getUsagers()          { return usagers; }
    public List<Emprunt> getEmprunts()        { return emprunts; }
    public Map<String, Long> getStatistiquesParTypeUsager() {
        return emprunts.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getUsager().getClass().getSimpleName(),
                        Collectors.counting()
                ));
    }
}