package ca.cegep.biblio.unit;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.util.IdGenerator;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class RechercheTest {

    private BibliothequeService service;
    private Exemplaire exemplaireA;
    private Exemplaire exemplaireB;
    private Exemplaire exemplaireC;
    private Exemplaire exemplaireD;

    @BeforeEach
    void setUp() {
        service = new BibliothequeService();

        exemplaireA = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Les Misérables",
                "Victor Hugo",
                "978-2-07-040850-1",
                Statut.DISPONIBLE,
                EtatPhysique.BON
        );
        exemplaireB = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Notre-Dame de Paris",
                "Victor Hugo",
                "978-2-07-040850-2",
                Statut.DISPONIBLE,
                EtatPhysique.NEUF
        );
        exemplaireC = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Germinal",
                "Émile Zola",
                "978-2-07-040850-3",
                Statut.EMPRUNTE,
                EtatPhysique.USE
        );
        exemplaireD = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Les Misérables — Tome 2",
                "Victor Hugo",
                "978-2-07-040850-4",
                Statut.A_REPARER,
                EtatPhysique.A_REPARER
        );

        service.ajouterExemplaire(exemplaireA);
        service.ajouterExemplaire(exemplaireB);
        service.ajouterExemplaire(exemplaireC);
        service.ajouterExemplaire(exemplaireD);
    }

    // -------------------------------------------------------------------------
    // Tests recherche par titre
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Recherche par titre — résultats corrects et triés")
    void testRechercheParTitre() {
        List<Exemplaire> resultats = service.rechercherParTitre("misérables");
        assertEquals(2, resultats.size());
        // Vérifier l'ordre alphabétique
        assertTrue(resultats.get(0).getTitre()
                .compareTo(resultats.get(1).getTitre()) <= 0);
    }

    @Test
    @DisplayName("Recherche par titre — insensible à la casse")
    void testRechercheParTitreInsensible() {
        List<Exemplaire> resultats = service.rechercherParTitre("MISÉRABLES");
        assertEquals(2, resultats.size());
    }

    @Test
    @DisplayName("Recherche par titre — aucun résultat")
    void testRechercheParTitreAucunResultat() {
        List<Exemplaire> resultats = service.rechercherParTitre("Titre Inexistant");
        assertTrue(resultats.isEmpty());
    }

    @Test
    @DisplayName("Recherche par titre — résultats triés alphabétiquement")
    void testRechercheParTitreTri() {
        List<Exemplaire> resultats = service.rechercherParTitre("Victor");
        // Tous les livres de Victor Hugo — vérifier l'ordre
        for (int i = 0; i < resultats.size() - 1; i++) {
            assertTrue(
                resultats.get(i).getTitre()
                    .compareTo(resultats.get(i + 1).getTitre()) <= 0
            );
        }
    }

    // -------------------------------------------------------------------------
    // Tests recherche par auteur
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Recherche par auteur — résultats corrects et triés")
    void testRechercheParAuteur() {
        List<Exemplaire> resultats = service.rechercherParAuteur("Victor Hugo");
        assertEquals(3, resultats.size());
    }

    @Test
    @DisplayName("Recherche par auteur — insensible à la casse")
    void testRechercheParAuteurInsensible() {
        List<Exemplaire> resultats = service.rechercherParAuteur("victor hugo");
        assertEquals(3, resultats.size());
    }

    @Test
    @DisplayName("Recherche par auteur — résultats triés alphabétiquement")
    void testRechercheParAuteurTri() {
        List<Exemplaire> resultats = service.rechercherParAuteur("Hugo");
        for (int i = 0; i < resultats.size() - 1; i++) {
            assertTrue(
                resultats.get(i).getAuteur()
                    .compareTo(resultats.get(i + 1).getAuteur()) <= 0
            );
        }
    }

    @Test
    @DisplayName("Recherche par auteur — aucun résultat")
    void testRechercheParAuteurAucunResultat() {
        List<Exemplaire> resultats = service.rechercherParAuteur("Auteur Inexistant");
        assertTrue(resultats.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Tests recherche par ISBN
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Recherche par ISBN — résultat correct")
    void testRechercheParISBN() {
        List<Exemplaire> resultats = service.rechercherParISBN("978-2-07-040850-1");
        assertEquals(1, resultats.size());
        assertEquals(exemplaireA, resultats.get(0));
    }

    @Test
    @DisplayName("Recherche par ISBN — aucun résultat")
    void testRechercheParISBNAucunResultat() {
        List<Exemplaire> resultats = service.rechercherParISBN("000-000-000");
        assertTrue(resultats.isEmpty());
    }

    @Test
    @DisplayName("Recherche par ISBN partiel — résultats corrects")
    void testRechercheParISBNPartiel() {
        List<Exemplaire> resultats = service.rechercherParISBN("978-2-07-040850");
        assertEquals(4, resultats.size());
    }

    // -------------------------------------------------------------------------
    // Tests disponibilité
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDisponibles — seulement les exemplaires DISPONIBLE")
    void testGetDisponibles() {
        List<Exemplaire> disponibles = service.getDisponibles();
        assertEquals(2, disponibles.size());
        assertTrue(disponibles.stream()
                .allMatch(e -> e.getStatut() == Statut.DISPONIBLE));
    }

    @Test
    @DisplayName("getEnReparation — seulement les exemplaires A_REPARER")
    void testGetEnReparation() {
        exemplaireD.setDateDisponibilite(LocalDate.now().plusDays(3));
        List<Exemplaire> enReparation = service.getEnReparation();
        assertEquals(1, enReparation.size());
        assertEquals(exemplaireD, enReparation.get(0));
    }

    // -------------------------------------------------------------------------
    // Tests date de retour la plus proche
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDateRetourPlusProche — retourne la date la plus proche")
    void testGetDateRetourPlusProche() {
        Etudiant usager1 = new Etudiant("Alice", IdGenerator.genererIdUsager());
        Etudiant usager2 = new Etudiant("Bob", IdGenerator.genererIdUsager());
        service.ajouterUsager(usager1);
        service.ajouterUsager(usager2);

        service.emprunter(usager1, "978-2-07-040850-1");
        service.emprunter(usager2, "978-2-07-040850-2");

        Optional<LocalDate> dateProche = service.getDateRetourPlusProche("978-2-07-040850-1");
        assertTrue(dateProche.isPresent());
    }

    @Test
    @DisplayName("getDateRetourPlusProche — vide si aucun emprunt pour cet ISBN")
    void testGetDateRetourPlusProcheVide() {
        Optional<LocalDate> dateProche = service.getDateRetourPlusProche("978-2-07-040850-1");
        assertFalse(dateProche.isPresent());
    }
}