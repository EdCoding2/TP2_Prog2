package ca.cegep.biblio.unit;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.util.DateUtils;
import ca.cegep.biblio.util.IdGenerator;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class EmpruntRulesTest {

    private BibliothequeService service;
    private Etudiant etudiant;
    private Professeur professeur;
    private Visiteur visiteur;
    private Exemplaire exemplaire1;
    private Exemplaire exemplaire2;
    private Exemplaire exemplaire3;

    @BeforeEach
    void setUp() {
        service    = new BibliothequeService();
        etudiant   = new Etudiant("Alice", IdGenerator.genererIdUsager());
        professeur = new Professeur("Bob", IdGenerator.genererIdUsager());
        visiteur   = new Visiteur("Charlie", IdGenerator.genererIdUsager());

        // Trois exemplaires du même ouvrage (même ISBN)
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
                "Le Petit Prince",
                "Antoine de Saint-Exupéry",
                "978-2-07-040850-4",
                Statut.DISPONIBLE,
                EtatPhysique.BON
        );
        exemplaire3 = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Le Petit Prince",
                "Antoine de Saint-Exupéry",
                "978-2-07-040850-4",
                Statut.DISPONIBLE,
                EtatPhysique.BON
        );

        service.ajouterExemplaire(exemplaire1);
        service.ajouterExemplaire(exemplaire2);
        service.ajouterExemplaire(exemplaire3);
        service.ajouterUsager(etudiant);
        service.ajouterUsager(professeur);
        service.ajouterUsager(visiteur);
    }

    // -------------------------------------------------------------------------
    // Tests emprunt normal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Emprunt normal — succès")
    void testEmpruntNormal() {
        boolean resultat = service.emprunter(etudiant, "978-2-07-040850-4");
        assertTrue(resultat);
        assertEquals(1, etudiant.getEmpruntsEnCours().size());
        assertEquals(Statut.EMPRUNTE, exemplaire1.getStatut());
    }

    // -------------------------------------------------------------------------
    // Tests limite par type d'usager
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Etudiant — ne peut pas emprunter plus de 3 livres")
    void testLimiteEtudiant() {
        // Préparer 4 livres avec ISBN différents
        String[] isbns = {"111", "222", "333", "444"};
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

        assertTrue(service.emprunter(etudiant, "111"));
        assertTrue(service.emprunter(etudiant, "222"));
        assertTrue(service.emprunter(etudiant, "333"));
        assertFalse(service.emprunter(etudiant, "444")); // 4e emprunt — refusé
        assertEquals(3, etudiant.getEmpruntsEnCours().size());
    }

    @Test
    @DisplayName("Visiteur — ne peut pas emprunter plus de 1 livre")
    void testLimiteVisiteur() {
        Exemplaire ex2 = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Autre Livre",
                "Auteur",
                "999",
                Statut.DISPONIBLE,
                EtatPhysique.BON
        );
        service.ajouterExemplaire(ex2);

        assertTrue(service.emprunter(visiteur, "978-2-07-040850-4"));
        assertFalse(service.emprunter(visiteur, "999")); // 2e emprunt — refusé
        assertEquals(1, visiteur.getEmpruntsEnCours().size());
    }

    // -------------------------------------------------------------------------
    // Tests livre À réparer
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Emprunt d'un exemplaire À réparer — refusé")
    void testEmpruntLivreAReparer() {
        exemplaire1.setStatut(Statut.A_REPARER);
        exemplaire2.setStatut(Statut.A_REPARER);
        exemplaire3.setStatut(Statut.A_REPARER);

        boolean resultat = service.emprunter(etudiant, "978-2-07-040850-4");
        assertFalse(resultat);
        assertEquals(0, etudiant.getEmpruntsEnCours().size());
    }

    // -------------------------------------------------------------------------
    // Tests double emprunt même ISBN
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Double emprunt même ISBN — refusé")
    void testDoubleEmpruntMemeIsbn() {
        assertTrue(service.emprunter(etudiant, "978-2-07-040850-4"));
        assertFalse(service.emprunter(etudiant, "978-2-07-040850-4"));
        assertEquals(1, etudiant.getEmpruntsEnCours().size());
    }

    // -------------------------------------------------------------------------
    // Tests saturation des exemplaires
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Saturation — 3 exemplaires empruntés, 4e usager échoue")
    void testSaturationExemplaires() {
        Etudiant usager2 = new Etudiant("David", IdGenerator.genererIdUsager());
        Etudiant usager3 = new Etudiant("Eve", IdGenerator.genererIdUsager());
        Etudiant usager4 = new Etudiant("Frank", IdGenerator.genererIdUsager());
        service.ajouterUsager(usager2);
        service.ajouterUsager(usager3);
        service.ajouterUsager(usager4);

        assertTrue(service.emprunter(etudiant, "978-2-07-040850-4"));
        assertTrue(service.emprunter(usager2, "978-2-07-040850-4"));
        assertTrue(service.emprunter(usager3, "978-2-07-040850-4"));
        assertFalse(service.emprunter(usager4, "978-2-07-040850-4")); // Aucun exemplaire dispo
    }

    // -------------------------------------------------------------------------
    // Tests libération des exemplaires
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Libération — après retour, 4e usager peut emprunter")
    void testLiberationExemplaire() {
        Etudiant usager2 = new Etudiant("David", IdGenerator.genererIdUsager());
        Etudiant usager3 = new Etudiant("Eve", IdGenerator.genererIdUsager());
        Etudiant usager4 = new Etudiant("Frank", IdGenerator.genererIdUsager());
        service.ajouterUsager(usager2);
        service.ajouterUsager(usager3);
        service.ajouterUsager(usager4);

        service.emprunter(etudiant, "978-2-07-040850-4");
        service.emprunter(usager2, "978-2-07-040850-4");
        service.emprunter(usager3, "978-2-07-040850-4");

        // Retourner un exemplaire
        service.retourner(etudiant, exemplaire1, EtatPhysique.BON);

        // 4e usager peut maintenant emprunter
        assertTrue(service.emprunter(usager4, "978-2-07-040850-4"));
    }

    // -------------------------------------------------------------------------
    // Tests calcul date de disponibilité après réparation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Réparation — retour le lundi, disponible le jeudi (3 jours ouvrables)")
    void testReparationRetourLundi() {
        // Trouver le prochain lundi
        LocalDate lundi = LocalDate.now();
        while (lundi.getDayOfWeek() != DayOfWeek.MONDAY) {
            lundi = lundi.plusDays(1);
        }

        LocalDate dateAttendue = lundi.plusDays(3); // Mardi, Mercredi, Jeudi
        LocalDate dateCalculee = DateUtils.ajouterJoursOuvrables(lundi, 3);
        assertEquals(dateAttendue, dateCalculee);
    }

    @Test
    @DisplayName("Réparation — retour le jeudi, disponible le mardi (saute le weekend)")
    void testReparationRetourJeudi() {
        // Trouver le prochain jeudi
        LocalDate jeudi = LocalDate.now();
        while (jeudi.getDayOfWeek() != DayOfWeek.THURSDAY) {
            jeudi = jeudi.plusDays(1);
        }

        // Jeudi + 3 jours ouvrables = vendredi, lundi, mardi
        LocalDate dateAttendue = jeudi.plusDays(5); // saute samedi et dimanche
        LocalDate dateCalculee = DateUtils.ajouterJoursOuvrables(jeudi, 3);
        assertEquals(dateAttendue, dateCalculee);
    }

    // -------------------------------------------------------------------------
    // Tests emprunts en retard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Emprunts en retard — dates passées détectées")
    void testEmpruntsEnRetardDetectes() {
        // Créer manuellement un emprunt avec date de retour dans le passé
        LocalDate datePassee = LocalDate.now().minusDays(5);
        Emprunt empruntEnRetard = new Emprunt(
                exemplaire1,
                etudiant,
                datePassee.minusDays(14),
                datePassee
        );
        service.getEmprunts().add(empruntEnRetard);
        etudiant.getEmpruntsEnCours().add(empruntEnRetard);
        exemplaire1.setStatut(Statut.EMPRUNTE);

        assertTrue(service.getEmpruntsEnRetard().contains(empruntEnRetard));
    }

    @Test
    @DisplayName("Emprunts en retard — dates futures ignorées")
    void testEmpruntsFutursIgnores() {
        // Emprunt normal avec date future
        service.emprunter(etudiant, "978-2-07-040850-4");

        // Aucun emprunt en retard
        assertTrue(service.getEmpruntsEnRetard().isEmpty());
    }
}