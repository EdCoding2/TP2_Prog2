package ca.cegep.biblio.unit;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.util.IdGenerator;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StatistiquesTest {

    private BibliothequeService service;
    private Etudiant etudiant1;
    private Etudiant etudiant2;
    private Professeur professeur;
    private Visiteur visiteur;

    @BeforeEach
    void setUp() {
        service    = new BibliothequeService();
        etudiant1  = new Etudiant("Alice", IdGenerator.genererIdUsager());
        etudiant2  = new Etudiant("Bob", IdGenerator.genererIdUsager());
        professeur = new Professeur("Charlie", IdGenerator.genererIdUsager());
        visiteur   = new Visiteur("David", IdGenerator.genererIdUsager());

        service.ajouterUsager(etudiant1);
        service.ajouterUsager(etudiant2);
        service.ajouterUsager(professeur);
        service.ajouterUsager(visiteur);

        // Exemplaires avec ISBN distincts
        String[] isbns = {"111", "222", "333", "444", "555", "666"};
        for (String isbn : isbns) {
            Exemplaire ex = new Exemplaire(
                    IdGenerator.genererIdExemplaire(),
                    "Titre " + isbn,
                    "Auteur",
                    isbn,
                    Statut.DISPONIBLE,
                    EtatPhysique.BON
            );
            service.ajouterExemplaire(ex);
        }
    }

    // -------------------------------------------------------------------------
    // Tests statistiques par type d'usager
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Statistiques — aucun emprunt, map vide")
    void testStatistiquesAucunEmprunt() {
        Map<String, Long> stats = service.getStatistiquesParTypeUsager();
        assertTrue(stats.isEmpty());
    }

    @Test
    @DisplayName("Statistiques — emprunts par étudiant comptés correctement")
    void testStatistiquesEtudiants() {
        service.emprunter(etudiant1, "111");
        service.emprunter(etudiant2, "222");

        Map<String, Long> stats = service.getStatistiquesParTypeUsager();

        assertTrue(stats.containsKey("Etudiant"));
        assertEquals(2L, stats.get("Etudiant"));
    }

    @Test
    @DisplayName("Statistiques — emprunts par professeur comptés correctement")
    void testStatistiquesProfesseur() {
        service.emprunter(professeur, "111");
        service.emprunter(professeur, "222");
        service.emprunter(professeur, "333");

        Map<String, Long> stats = service.getStatistiquesParTypeUsager();

        assertTrue(stats.containsKey("Professeur"));
        assertEquals(3L, stats.get("Professeur"));
    }

    @Test
    @DisplayName("Statistiques — emprunts par visiteur comptés correctement")
    void testStatistiquesVisiteur() {
        service.emprunter(visiteur, "111");

        Map<String, Long> stats = service.getStatistiquesParTypeUsager();

        assertTrue(stats.containsKey("Visiteur"));
        assertEquals(1L, stats.get("Visiteur"));
    }

    @Test
    @DisplayName("Statistiques — tous les types présents simultanément")
    void testStatistiquesTousTypes() {
        // Etudiant : 2 emprunts
        service.emprunter(etudiant1, "111");
        service.emprunter(etudiant2, "222");

        // Professeur : 3 emprunts
        service.emprunter(professeur, "333");
        service.emprunter(professeur, "444");
        service.emprunter(professeur, "555");

        // Visiteur : 1 emprunt
        service.emprunter(visiteur, "666");

        Map<String, Long> stats = service.getStatistiquesParTypeUsager();

        assertEquals(3, stats.size());
        assertEquals(2L, stats.get("Etudiant"));
        assertEquals(3L, stats.get("Professeur"));
        assertEquals(1L, stats.get("Visiteur"));
    }

    @Test
    @DisplayName("Statistiques — total global correct")
    void testStatistiquesTotalGlobal() {
        service.emprunter(etudiant1, "111");
        service.emprunter(etudiant2, "222");
        service.emprunter(professeur, "333");
        service.emprunter(visiteur, "444");

        Map<String, Long> stats = service.getStatistiquesParTypeUsager();

        long total = stats.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(4L, total);
    }

    @Test
    @DisplayName("Statistiques — après retour, compteur mis à jour")
    void testStatistiquesApresRetour() {
        service.emprunter(etudiant1, "111");
        service.emprunter(etudiant2, "222");

        // Retourner un exemplaire
        Exemplaire exemplaireRetourne = service.getExemplaires().stream()
                .filter(e -> e.getIsbn().equals("111"))
                .findFirst()
                .orElseThrow();
        service.retourner(etudiant1, exemplaireRetourne, EtatPhysique.BON);

        Map<String, Long> stats = service.getStatistiquesParTypeUsager();

        // Après retour, seulement 1 emprunt étudiant actif
        assertEquals(1L, stats.get("Etudiant"));
    }

    @Test
    @DisplayName("Compteur AtomicInteger — incrémenté à chaque emprunt réussi")
    void testCompteurAtomicInteger() {
        assertEquals(0, service.getCompteurEmprunts());

        service.emprunter(etudiant1, "111");
        assertEquals(1, service.getCompteurEmprunts());

        service.emprunter(etudiant2, "222");
        assertEquals(2, service.getCompteurEmprunts());

        service.emprunter(professeur, "333");
        assertEquals(3, service.getCompteurEmprunts());
    }

    @Test
    @DisplayName("Compteur AtomicInteger — non décrémenté après retour")
    void testCompteurNonDecrementeApresRetour() {
        service.emprunter(etudiant1, "111");
        assertEquals(1, service.getCompteurEmprunts());

        Exemplaire ex = service.getExemplaires().stream()
                .filter(e -> e.getIsbn().equals("111"))
                .findFirst()
                .orElseThrow();

        service.retourner(etudiant1, ex, EtatPhysique.BON);

        // Le compteur reste à 1 — il est cumulatif, pas courant
        assertEquals(1, service.getCompteurEmprunts());
    }
}