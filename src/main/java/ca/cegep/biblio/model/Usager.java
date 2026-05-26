package ca.cegep.biblio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Usager {

    private String nom;
    private String id;
    private List<Emprunt> empruntsEnCours;

    // Constructeur
    public Usager(String nom, String id) {
        this.nom = nom;
        this.id = id;
        this.empruntsEnCours = new ArrayList<>();
    }

    // Méthodes abstraites — chaque sous-classe définit ses propres règles
    public abstract int getMaxLivres();
    public abstract int getDureeEmpruntJours();

    // Méthode concrète — vérifie si l'usager peut encore emprunter
    public boolean peutEmprunter() {
        return empruntsEnCours.size() < getMaxLivres();
    }

    // Vérifie si l'usager a déjà emprunté un exemplaire avec cet ISBN
    public boolean aDejaIsbn(String isbn) {
        return empruntsEnCours.stream()
                .anyMatch(e -> e.getExemplaire().getIsbn().equals(isbn));
    }

    // Getters
    public String getNom()                        { return nom; }
    public String getId()                         { return id; }
    public List<Emprunt> getEmpruntsEnCours()     { return empruntsEnCours; }

    // Setters
    public void setNom(String nom)   { this.nom = nom; }
    public void setId(String id)     { this.id = id; }

    // Equals et hashCode basés sur id uniquement
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usager)) return false;
        Usager that = (Usager) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Usager{" +
                "nom='" + nom + '\'' +
                ", id='" + id + '\'' +
                ", empruntsEnCours=" + empruntsEnCours.size() +
                '}';
    }
}