package ca.cegep.biblio.model;

import java.time.LocalDate;
import java.util.Objects;

public class Emprunt {

    private Exemplaire exemplaire;
    private Usager usager;
    private LocalDate dateEmprunt;
    private LocalDate dateRetourPrevue;

    // Constructeur complet
    public Emprunt(Exemplaire exemplaire, Usager usager, LocalDate dateEmprunt, LocalDate dateRetourPrevue) {
        this.exemplaire = exemplaire;
        this.usager = usager;
        this.dateEmprunt = dateEmprunt;
        this.dateRetourPrevue = dateRetourPrevue;
    }

    // Vérifie si l'emprunt est en retard
    public boolean estEnRetard() {
        return LocalDate.now().isAfter(dateRetourPrevue);
    }

    // Getters
    public Exemplaire getExemplaire()         { return exemplaire; }
    public Usager getUsager()                 { return usager; }
    public LocalDate getDateEmprunt()         { return dateEmprunt; }
    public LocalDate getDateRetourPrevue()    { return dateRetourPrevue; }

    // Setters
    public void setExemplaire(Exemplaire exemplaire)         { this.exemplaire = exemplaire; }
    public void setUsager(Usager usager)                     { this.usager = usager; }
    public void setDateEmprunt(LocalDate dateEmprunt)        { this.dateEmprunt = dateEmprunt; }
    public void setDateRetourPrevue(LocalDate dateRetourPrevue) { this.dateRetourPrevue = dateRetourPrevue; }

    // Equals et hashCode basés sur exemplaire et usager
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Emprunt)) return false;
        Emprunt that = (Emprunt) o;
        return Objects.equals(exemplaire, that.exemplaire) &&
               Objects.equals(usager, that.usager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exemplaire, usager);
    }

    @Override
    public String toString() {
        return "Emprunt{" +
                "exemplaire=" + exemplaire.getIdExemplaire() +
                ", usager=" + usager.getNom() +
                ", dateEmprunt=" + dateEmprunt +
                ", dateRetourPrevue=" + dateRetourPrevue +
                ", enRetard=" + estEnRetard() +
                '}';
    }
}