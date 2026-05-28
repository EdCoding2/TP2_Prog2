package ca.cegep.biblio.unit;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.util.IdGenerator;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class UsagerTest {

    private BibliothequeService service;
    private Etudiant etudiant;
    private Professeur professeur;
    private Visiteur visiteur;

    @BeforeEach
    void setUp() {
        service    = new BibliothequeService();
        etudiant   = new Etudiant("Alice", IdGenerator.genererIdUsager());
        professeur = new Professeur("Bob", IdGenerator.genererIdUsager());
        visiteur   = new Visiteur("Charlie", IdGenerator.genererIdUsager());
    }

    // -------------------------------------------------------------------------
    // Tests création et règles par type
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Etudiant — max 3 livres, 14 jours")
    void testReglesEtudiant() {
        assertEquals(3, etudiant.getMaxLivres());
        assertEquals(14, etudiant.getDureeEmpruntJours());
    }

    @Test
    @DisplayName("Professeur — max 6 livres, 30 jours")
    void testReglesProfesseur() {
        assertEquals(6, professeur.getMaxLivres());
        assertEquals(30, professeur.getDureeEmpruntJours());
    }

    @Test
    @DisplayName("Visiteur — max 1 livre, 7 jours")
    void testReglesVisiteur() {
        assertEquals(1, visiteur.getMaxLivres());
        assertEquals(7, visiteur.getDureeEmpruntJours());
    }

    @Test
    @DisplayName("Usager créé — peut emprunter par défaut")
    void testPeutEmprunterParDefaut() {
        assertTrue(etudiant.peutEmprunter());
        assertTrue(professeur.peutEmprunter());
        assertTrue(visiteur.peutEmprunter());
    }

    @Test
    @DisplayName("Usager — empruntsEnCours vide à la création")
    void testEmpruntsEnCoursVide() {
        assertEquals(0, etudiant.getEmpruntsEnCours().size());
        assertEquals(0, professeur.getEmpruntsEnCours().size());
        assertEquals(0, visiteur.getEmpruntsEnCours().size());
    }

    // -------------------------------------------------------------------------
    // Tests CRUD usagers via BibliothequeService
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ajout d'un usager — présent dans la collection")
    void testAjouterUsager() {
        service.ajouterUsager(etudiant);
        assertTrue(service.getUsagers().contains(etudiant));
        assertEquals(1, service.getUsagers().size());
    }

    @Test
    @DisplayName("Ajout de plusieurs usagers — taille correcte")
    void testAjouterPlusieursUsagers() {
        service.ajouterUsager(etudiant);
        service.ajouterUsager(professeur);
        service.ajouterUsager(visiteur);
        assertEquals(3, service.getUsagers().size());
    }

    @Test
    @DisplayName("Modification d'un usager — nom mis à jour")
    void testModifierUsager() {
        service.ajouterUsager(etudiant);
        boolean resultat = service.modifierUsager(etudiant.getId(), "Alice Modifiée");
        assertTrue(resultat);
        assertEquals("Alice Modifiée", etudiant.getNom());
    }

    @Test
    @DisplayName("Modification d'un usager inexistant — retourne false")
    void testModifierUsagerInexistant() {
        boolean resultat = service.modifierUsager("ID-INEXISTANT", "Nouveau Nom");
        assertFalse(resultat);
    }

    @Test
    @DisplayName("Suppression d'un usager sans emprunts — réussie")
    void testSupprimerUsagerSansEmprunts() {
        service.ajouterUsager(etudiant);
        boolean resultat = service.supprimerUsager(etudiant.getId());
        assertTrue(resultat);
        assertFalse(service.getUsagers().contains(etudiant));
    }

    @Test
    @DisplayName("Suppression d'un usager avec emprunts actifs — refusée")
    void testSupprimerUsagerAvecEmprunts() {
        // Préparer un exemplaire et un emprunt actif
        Exemplaire exemplaire = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Le Petit Prince",
                "Antoine de Saint-Exupéry",
                "978-2-07-040850-4",
                Statut.DISPONIBLE,
                EtatPhysique.BON
        );
        service.ajouterExemplaire(exemplaire);
        service.ajouterUsager(etudiant);
        service.emprunter(etudiant, exemplaire.getIsbn());

        // Tenter la suppression
        boolean resultat = service.supprimerUsager(etudiant.getId());
        assertFalse(resultat);
        assertTrue(service.getUsagers().contains(etudiant));
    }

    @Test
    @DisplayName("Suppression d'un usager inexistant — retourne false")
    void testSupprimerUsagerInexistant() {
        boolean resultat = service.supprimerUsager("ID-INEXISTANT");
        assertFalse(resultat);
    }

    // -------------------------------------------------------------------------
    // Tests equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Equals — deux usagers avec même ID sont égaux")
    void testEqualsMemeId() {
        Etudiant copie = new Etudiant("Autre Nom", etudiant.getId());
        assertEquals(etudiant, copie);
        assertEquals(etudiant.hashCode(), copie.hashCode());
    }

    @Test
    @DisplayName("Equals — deux usagers avec IDs différents ne sont pas égaux")
    void testEqualsIdsDifferents() {
        assertNotEquals(etudiant, professeur);
    }
}