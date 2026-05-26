package ca.cegep.biblio.model;

import java.time.LocalDate;
import java.util.Objects;

public class Exemplaire {

    private String idExemplaire;
    private String titre;
    private String auteur;
    private String isbn;
    private Statut statut;
    private EtatPhysique etatPhysique;
    private LocalDate dateDisponibilite;

    // Constructeur complet
    public Exemplaire(String idExemplaire, String titre, String auteur, String isbn,
                      Statut statut, EtatPhysique etatPhysique) {
        this.idExemplaire = idExemplaire;
        this.titre = titre;
        this.auteur = auteur;
        this.isbn = isbn;
        this.statut = statut;
        this.etatPhysique = etatPhysique;
        this.dateDisponibilite = null;
    }

    // Getters
    public String getIdExemplaire()           { return idExemplaire; }
    public String getTitre()                  { return titre; }
    public String getAuteur()                 { return auteur; }
    public String getIsbn()                   { return isbn; }
    public Statut getStatut()                 { return statut; }
    public EtatPhysique getEtatPhysique()     { return etatPhysique; }
    public LocalDate getDateDisponibilite()   { return dateDisponibilite; }

    // Setters
    public void setIdExemplaire(String idExemplaire)         { this.idExemplaire = idExemplaire; }
    public void setTitre(String titre)                       { this.titre = titre; }
    public void setAuteur(String auteur)                     { this.auteur = auteur; }
    public void setIsbn(String isbn)                         { this.isbn = isbn; }
    public void setStatut(Statut statut)                     { this.statut = statut; }
    public void setEtatPhysique(EtatPhysique etatPhysique)   { this.etatPhysique = etatPhysique; }
    public void setDateDisponibilite(LocalDate date)         { this.dateDisponibilite = date; }

    // Equals et hashCode basés sur idExemplaire uniquement
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exemplaire)) return false;
        Exemplaire that = (Exemplaire) o;
        return Objects.equals(idExemplaire, that.idExemplaire);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idExemplaire);
    }

    @Override
    public String toString() {
        return "Exemplaire{" +
                "id='" + idExemplaire + '\'' +
                ", titre='" + titre + '\'' +
                ", auteur='" + auteur + '\'' +
                ", isbn='" + isbn + '\'' +
                ", statut=" + statut +
                ", etatPhysique=" + etatPhysique +
                ", dateDisponibilite=" + dateDisponibilite +
                '}';
    }
}