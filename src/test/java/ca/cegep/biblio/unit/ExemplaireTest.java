package ca.cegep.biblio.unit;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.util.IdGenerator;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class ExemplaireTest {

    private BibliothequeService service;
    private Exemplaire exemplaire1;
    private Exemplaire exemplaire2;

    @BeforeEach
    void setUp() {
        service     = new BibliothequeService();
        exemplaire1 = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Le Petit Prince",
                "Antoine de Saint-Exupéry",
                "978-2-07-040850-4",
                Statut.DISPONIBLE,
                EtatPhysique.BON
        );
        exemplaire2 = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "1984",
                "George Orwell",
                "978-0-452-28423-4",
                Statut.DISPONIBLE,
                EtatPhysique.NEUF
        );
    }

    // -------------------------------------------------------------------------
    // Tests création
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Création d'un exemplaire — attributs corrects")
    void testCreationExemplaire() {
        assertNotNull(exemplaire1.getIdExemplaire());
        assertEquals("Le Petit Prince", exemplaire1.getTitre());
        assertEquals("Antoine de Saint-Exupéry", exemplaire1.getAuteur());
        assertEquals("978-2-07-040850-4", exemplaire1.getIsbn());
        assertEquals(Statut.DISPONIBLE, exemplaire1.getStatut());
        assertEquals(EtatPhysique.BON, exemplaire1.getEtatPhysique());
        assertNull(exemplaire1.getDateDisponibilite());
    }

    // -------------------------------------------------------------------------
    // Tests equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Equals — deux exemplaires avec même idExemplaire sont égaux")
    void testEqualsMemeId() {
        Exemplaire copie = new Exemplaire(
                exemplaire1.getIdExemplaire(),
                "Autre titre",
                "Autre auteur",
                "000",
                Statut.EMPRUNTE,
                EtatPhysique.USE
        );
        assertEquals(exemplaire1, copie);
        assertEquals(exemplaire1.hashCode(), copie.hashCode());
    }

    @Test
    @DisplayName("Equals — deux exemplaires avec IDs différents ne sont pas égaux")
    void testEqualsIdsDifferents() {
        assertNotEquals(exemplaire1, exemplaire2);
    }

    // -------------------------------------------------------------------------
    // Tests CRUD via BibliothequeService
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ajout d'un exemplaire — présent dans la collection")
    void testAjouterExemplaire() {
        service.ajouterExemplaire(exemplaire1);
        assertTrue(service.getExemplaires().contains(exemplaire1));
        assertEquals(1, service.getExemplaires().size());
    }

    @Test
    @DisplayName("Ajout de plusieurs exemplaires — taille correcte")
    void testAjouterPlusieursExemplaires() {
        service.ajouterExemplaire(exemplaire1);
        service.ajouterExemplaire(exemplaire2);
        assertEquals(2, service.getExemplaires().size());
    }

    @Test
    @DisplayName("Modification d'un exemplaire — titre mis à jour")
    void testModifierExemplaire() {
        service.ajouterExemplaire(exemplaire1);
        boolean resultat = service.modifierExemplaire(
                exemplaire1.getIdExemplaire(),
                "Nouveau Titre",
                "Nouvel Auteur",
                "000-000"
        );
        assertTrue(resultat);
        assertEquals("Nouveau Titre", exemplaire1.getTitre());
        assertEquals("Nouvel Auteur", exemplaire1.getAuteur());
        assertEquals("000-000", exemplaire1.getIsbn());
    }

    @Test
    @DisplayName("Modification d'un exemplaire inexistant — retourne false")
    void testModifierExemplaireInexistant() {
        boolean resultat = service.modifierExemplaire("ID-INEXISTANT", "Titre", "Auteur", "000");
        assertFalse(resultat);
    }

    @Test
    @DisplayName("Suppression d'un exemplaire disponible — retiré de la collection")
    void testSupprimerExemplaire() {
        service.ajouterExemplaire(exemplaire1);
        boolean resultat = service.supprimerExemplaire(exemplaire1.getIdExemplaire());
        assertTrue(resultat);
        assertFalse(service.getExemplaires().contains(exemplaire1));
    }

    @Test
    @DisplayName("Suppression d'un exemplaire emprunté — refusée")
    void testSupprimerExemplaireEmprunte() {
        service.ajouterExemplaire(exemplaire1);
        Usager usager = new Etudiant("Alice", IdGenerator.genererIdUsager());
        service.ajouterUsager(usager);
        service.emprunter(usager, exemplaire1.getIsbn());

        boolean resultat = service.supprimerExemplaire(exemplaire1.getIdExemplaire());
        assertFalse(resultat);
        assertTrue(service.getExemplaires().contains(exemplaire1));
    }

    @Test
    @DisplayName("Suppression d'un exemplaire inexistant — retourne false")
    void testSupprimerExemplaireInexistant() {
        boolean resultat = service.supprimerExemplaire("ID-INEXISTANT");
        assertFalse(resultat);
    }
}