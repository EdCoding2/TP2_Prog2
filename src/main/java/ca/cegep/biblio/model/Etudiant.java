package ca.cegep.biblio.model;

public class Etudiant extends Usager {

    // Constructeur
    public Etudiant(String nom, String id) {
        super(nom, id);
    }

    @Override
    public int getMaxLivres() {
        return 3;
    }

    @Override
    public int getDureeEmpruntJours() {
        return 14;
    }

    @Override
    public String toString() {
        return "Etudiant{" +
                "nom='" + getNom() + '\'' +
                ", id='" + getId() + '\'' +
                ", empruntsEnCours=" + getEmpruntsEnCours().size() +
                '}';
    }
}