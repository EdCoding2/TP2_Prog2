package ca.cegep.biblio.model;

public class Professeur extends Usager {

    // Constructeur
    public Professeur(String nom, String id) {
        super(nom, id);
    }

    @Override
    public int getMaxLivres() {
        return 6;
    }

    @Override
    public int getDureeEmpruntJours() {
        return 30;
    }

    @Override
    public String toString() {
        return "Professeur{" +
                "nom='" + getNom() + '\'' +
                ", id='" + getId() + '\'' +
                ", empruntsEnCours=" + getEmpruntsEnCours().size() +
                '}';
    }
}