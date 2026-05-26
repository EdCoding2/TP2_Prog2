package ca.cegep.biblio.service;

import ca.cegep.biblio.model.Exemplaire;
import ca.cegep.biblio.model.Statut;
import ca.cegep.biblio.model.Emprunt;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RechercheService {

    private List<Exemplaire> exemplaires;

    public RechercheService(List<Exemplaire> exemplaires) {
        this.exemplaires = exemplaires;
    }

    // Recherche par titre — insensible à la casse, triée alphabétiquement
    public List<Exemplaire> rechercherParTitre(String titre) {
        String titreLower = titre.toLowerCase();
        return exemplaires.stream()
                .filter(e -> e.getTitre().toLowerCase().contains(titreLower))
                .sorted(Comparator.comparing(Exemplaire::getTitre))
                .collect(Collectors.toList());
    }

    // Recherche par auteur — insensible à la casse, triée alphabétiquement
    public List<Exemplaire> rechercherParAuteur(String auteur) {
        String auteurLower = auteur.toLowerCase();
        return exemplaires.stream()
                .filter(e -> e.getAuteur().toLowerCase().contains(auteurLower))
                .sorted(Comparator.comparing(Exemplaire::getAuteur))
                .collect(Collectors.toList());
    }

    // Recherche par ISBN — triée alphabétiquement
    public List<Exemplaire> rechercherParISBN(String isbn) {
        return exemplaires.stream()
                .filter(e -> e.getIsbn().contains(isbn))
                .sorted(Comparator.comparing(Exemplaire::getIsbn))
                .collect(Collectors.toList());
    }

    // Retourne tous les exemplaires disponibles à l'emprunt
    public List<Exemplaire> getDisponibles() {
        return exemplaires.stream()
                .filter(e -> e.getStatut() == Statut.DISPONIBLE)
                .sorted(Comparator.comparing(Exemplaire::getTitre))
                .collect(Collectors.toList());
    }

    // Retourne tous les exemplaires en réparation
    public List<Exemplaire> getEnReparation() {
        return exemplaires.stream()
                .filter(e -> e.getStatut() == Statut.A_REPARER)
                .sorted(Comparator.comparing(Exemplaire::getDateDisponibilite))
                .collect(Collectors.toList());
    }

    // Convertit une liste d'exemplaires en liste de titres
    public List<String> getTitresDisponibles() {
        return getDisponibles().stream()
                .map(Exemplaire::getTitre)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // Convertit une liste d'exemplaires en liste d'ISBNs
    public List<String> getISBNDisponibles() {
        return getDisponibles().stream()
                .map(Exemplaire::getIsbn)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // Retourne la date de retour la plus proche pour un ISBN donné
    public Optional<LocalDate> getDateRetourPlusProche(String isbn, List<Emprunt> emprunts) {
        return emprunts.stream()
                .filter(e -> e.getExemplaire().getIsbn().equals(isbn))
                .map(Emprunt::getDateRetourPrevue)
                .min(Comparator.naturalOrder());
    }
}