package ca.cegep.biblio.service;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.util.DateUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EmpruntService {

    private final List<Exemplaire> exemplaires;
    private final List<Usager> usagers;
    private final List<Emprunt> emprunts;
    private final AtomicInteger compteurEmprunts;

    public EmpruntService(List<Exemplaire> exemplaires, List<Usager> usagers, List<Emprunt> emprunts) {
        this.exemplaires = exemplaires;
        this.usagers = usagers;
        this.emprunts = emprunts;
        this.compteurEmprunts = new AtomicInteger(0);
    }

    // Emprunt d'un livre — synchronisé pour éviter les race conditions
    public synchronized boolean emprunter(Usager usager, String isbn) {

        // Vérifier si l'usager peut encore emprunter
        if (!usager.peutEmprunter()) {
            System.out.println("Limite d'emprunts atteinte pour " + usager.getNom());
            return false;
        }

        // Vérifier que l'usager n'a pas déjà cet ISBN
        if (usager.aDejaIsbn(isbn)) {
            System.out.println("L'usager a déjà emprunté un exemplaire de cet ouvrage.");
            return false;
        }

        // Trouver un exemplaire disponible avec cet ISBN
        Exemplaire exemplaireDispo = exemplaires.stream()
                .filter(e -> e.getIsbn().equals(isbn))
                .filter(e -> e.getStatut() == Statut.DISPONIBLE)
                .findFirst()
                .orElse(null);

        if (exemplaireDispo == null) {
            System.out.println("Aucun exemplaire disponible pour l'ISBN : " + isbn);
            return false;
        }

        // Calculer les dates
        LocalDate dateEmprunt      = LocalDate.now();
        LocalDate dateRetourPrevue = DateUtils.ajouterJoursOuvrables(dateEmprunt, usager.getDureeEmpruntJours());

        // Créer l'emprunt
        Emprunt emprunt = new Emprunt(exemplaireDispo, usager, dateEmprunt, dateRetourPrevue);

        // Mettre à jour le statut de l'exemplaire
        exemplaireDispo.setStatut(Statut.EMPRUNTE);

        // Ajouter l'emprunt aux listes
        emprunts.add(emprunt);
        usager.getEmpruntsEnCours().add(emprunt);

        // Incrémenter le compteur global
        compteurEmprunts.incrementAndGet();

        System.out.println("Emprunt réussi : " + exemplaireDispo.getTitre() + " par " + usager.getNom());
        return true;
    }

    // Retour d'un livre — synchronisé pour éviter les race conditions
    public synchronized boolean retourner(Usager usager, Exemplaire exemplaire, EtatPhysique etat) {

        // Vérifier que le retour se fait un jour ouvrable
        if (!DateUtils.estJourOuvrable(LocalDate.now())) {
            System.out.println("Les retours ne sont pas acceptés les weekends.");
            return false;
        }

        // Trouver l'emprunt correspondant
        Emprunt empruntTrouve = emprunts.stream()
                .filter(e -> e.getExemplaire().equals(exemplaire))
                .filter(e -> e.getUsager().equals(usager))
                .findFirst()
                .orElse(null);

        if (empruntTrouve == null) {
            System.out.println("Aucun emprunt trouvé pour cet exemplaire et cet usager.");
            return false;
        }

        // Mettre à jour l'état physique
        exemplaire.setEtatPhysique(etat);

        // Si l'état est À réparer — bloquer l'exemplaire et calculer la date de disponibilité
        if (etat == EtatPhysique.A_REPARER) {
            exemplaire.setStatut(Statut.A_REPARER);
            LocalDate dateDisponibilite = DateUtils.ajouterJoursOuvrables(LocalDate.now(), 3);
            exemplaire.setDateDisponibilite(dateDisponibilite);
            System.out.println("Exemplaire en réparation jusqu'au : " + dateDisponibilite);
        } else {
            exemplaire.setStatut(Statut.DISPONIBLE);
            exemplaire.setDateDisponibilite(null);
        }

        // Retirer l'emprunt des listes
        emprunts.remove(empruntTrouve);
        usager.getEmpruntsEnCours().remove(empruntTrouve);

        System.out.println("Retour effectué : " + exemplaire.getTitre() + " par " + usager.getNom());
        return true;
    }

    // Retourne tous les emprunts en retard
    public List<Emprunt> getEmpruntsEnRetard() {
        return emprunts.stream()
                .filter(Emprunt::estEnRetard)
                .sorted((e1, e2) -> e1.getDateRetourPrevue().compareTo(e2.getDateRetourPrevue()))
                .collect(Collectors.toList());
    }

    // Statistiques par type d'usager
    public Map<String, Long> getStatistiquesParTypeUsager() {
        return emprunts.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getUsager().getClass().getSimpleName(),
                        Collectors.counting()
                ));
    }

    // Getter du compteur global
    public int getCompteurEmprunts() {
        return compteurEmprunts.get();
    }

    // Getter de l'AtomicInteger (utile pour l'affichage JavaFX)
    public AtomicInteger getAtomicCompteur() {
        return compteurEmprunts;
    }
}