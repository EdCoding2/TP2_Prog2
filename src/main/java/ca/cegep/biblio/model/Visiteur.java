package ca.cegep.biblio.model;

public class Visiteur extends Usager {

    // Constructeur
    public Visiteur(String nom, String id) {
        super(nom, id);
    }

    @Override
    public int getMaxLivres() {
        return 1;
    }

    @Override
    public int getDureeEmpruntJours() {
        return 7;
    }

    @Override
    public String toString() {
        return "Visiteur{" +
                "nom='" + getNom() + '\'' +
                ", id='" + getId() + '\'' +
                ", empruntsEnCours=" + getEmpruntsEnCours().size() +
                '}';
    }
}