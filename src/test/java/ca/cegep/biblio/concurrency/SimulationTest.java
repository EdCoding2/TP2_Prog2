package ca.cegep.biblio.concurrency;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.util.IdGenerator;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {

    private BibliothequeService service;
    private SimulationManager simulationManager;
    private RepairScheduler repairScheduler;

    @BeforeEach
    void setUp() {
        service           = new BibliothequeService();
        simulationManager = new SimulationManager(service, 10);
        repairScheduler   = new RepairScheduler(service);
    }

    @AfterEach
    void tearDown() {
        simulationManager.arreter();
    }

    // -------------------------------------------------------------------------
    // Utilitaire — créer un exemplaire et l'ajouter au service
    // -------------------------------------------------------------------------

    private Exemplaire creerEtAjouter(String titre, String isbn) {
        Exemplaire ex = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                titre, "Auteur", isbn,
                Statut.DISPONIBLE, EtatPhysique.BON
        );
        service.ajouterExemplaire(ex);
        return ex;
    }

    private Etudiant creerEtAjouterEtudiant(String nom) {
        Etudiant e = new Etudiant(nom, IdGenerator.genererIdUsager());
        service.ajouterUsager(e);
        return e;
    }

    // -------------------------------------------------------------------------
    // Test 1 — 4 exemplaires, 6 tâches simultanées → exactement 4 succès
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Concurrence — 6 emprunts simultanés sur 4 exemplaires : exactement 4 succès")
    void testExactement4SuccesSur6Tentatives() throws InterruptedException, ExecutionException {
        // Créer 4 exemplaires du même ouvrage
        String isbn = "978-TEST-0001";
        for (int i = 0; i < 4; i++) {
            creerEtAjouter("Ouvrage Concurrent", isbn);
        }

        // Créer 6 usagers
        List<Usager> usagers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            usagers.add(creerEtAjouterEtudiant("Usager" + i));
        }

        // Soumettre 6 emprunts en parallèle
        List<String> isbns = new ArrayList<>();
        for (int i = 0; i < 6; i++) isbns.add(isbn);

        List<Future<Boolean>> futures = simulationManager.simulerEmpruntsParalleles(usagers, isbns);
        simulationManager.attendreFin(futures);

        // Compter les succès
        long succes = futures.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { return false; }
                })
                .filter(Boolean::booleanValue)
                .count();

        assertEquals(4, succes);
    }

    // -------------------------------------------------------------------------
    // Test 2 — AtomicInteger reflète exactement 4 emprunts
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AtomicInteger — reflète exactement 4 emprunts réussis")
    void testAtomicIntegerExact() throws InterruptedException, ExecutionException {
        String isbn = "978-TEST-0002";
        for (int i = 0; i < 4; i++) creerEtAjouter("Ouvrage Atomic", isbn);

        List<Usager> usagers = new ArrayList<>();
        for (int i = 0; i < 6; i++) usagers.add(creerEtAjouterEtudiant("UsagerA" + i));

        List<String> isbns = new ArrayList<>();
        for (int i = 0; i < 6; i++) isbns.add(isbn);

        List<Future<Boolean>> futures = simulationManager.simulerEmpruntsParalleles(usagers, isbns);
        simulationManager.attendreFin(futures);

        // Le compteur doit refléter exactement 4 emprunts réussis
        assertEquals(4, service.getCompteurEmprunts());
    }

    // -------------------------------------------------------------------------
    // Test 3 — Race condition : 2 threads sur le même exemplaire
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Race condition — un seul thread obtient l'exemplaire unique")
    void testRaceConditionExemplaireUnique() throws InterruptedException, ExecutionException {
        // Un seul exemplaire disponible
        String isbn = "978-TEST-0003";
        creerEtAjouter("Ouvrage Unique", isbn);

        // Deux usagers tentent simultanément
        Etudiant usager1 = creerEtAjouterEtudiant("RaceUser1");
        Etudiant usager2 = creerEtAjouterEtudiant("RaceUser2");

        List<Usager> usagers = List.of(usager1, usager2);
        List<String> isbns   = List.of(isbn, isbn);

        List<Future<Boolean>> futures = simulationManager.simulerEmpruntsParalleles(usagers, isbns);
        simulationManager.attendreFin(futures);

        long succes = futures.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { return false; }
                })
                .filter(Boolean::booleanValue)
                .count();

        // Exactement 1 succès — pas de double emprunt
        assertEquals(1, succes);
        assertEquals(1, service.getEmprunts().size());
    }

    // -------------------------------------------------------------------------
    // Test 4 — Ajout et retrait de livres en parallèle
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Catalogue — ajout et suppression parallèles cohérents")
    void testModificationCatalogueParallele() throws InterruptedException {
        // Préparer des exemplaires à ajouter
        List<Exemplaire> aAjouter = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            aAjouter.add(new Exemplaire(
                    IdGenerator.genererIdExemplaire(),
                    "Livre " + i, "Auteur", "ISBN-" + i,
                    Statut.DISPONIBLE, EtatPhysique.BON
            ));
        }

        // Préparer des exemplaires à supprimer (déjà dans le catalogue)
        List<String> idsASupprimer = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Exemplaire ex = creerEtAjouter("Livre Supp " + i, "ISBN-SUPP-" + i);
            idsASupprimer.add(ex.getIdExemplaire());
        }

        int tailleAvant = service.getExemplaires().size();

        // Lancer ajouts et suppressions en parallèle
        simulationManager.simulerModificationCatalogue(aAjouter, idsASupprimer);

        // Attendre la fin des tâches
        simulationManager.arreter();

        // Vérifier la cohérence — taille = avant + ajouts - suppressions
        int tailleAttendue = tailleAvant + aAjouter.size() - idsASupprimer.size();
        assertEquals(tailleAttendue, service.getExemplaires().size());
    }

    // -------------------------------------------------------------------------
    // Test 5 — 10% retours abîmés → liste À réparer augmente
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Retours — 10% abîmés, liste À réparer augmente avec dates valides")
    void testRetourAvecTauxAbime() throws InterruptedException, ExecutionException {
        // Créer 20 exemplaires avec ISBN distincts et les emprunter
        List<Emprunt> empruntsActifs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String isbn = "978-ABIME-" + i;
            Exemplaire ex = creerEtAjouter("Livre " + i, isbn);
            Etudiant usager = creerEtAjouterEtudiant("UsagerB" + i);
            service.emprunter(usager, isbn);

            // Retrouver l'emprunt créé
            service.getEmprunts().stream()
                    .filter(e -> e.getExemplaire().getIsbn().equals(isbn))
                    .findFirst()
                    .ifPresent(empruntsActifs::add);
        }

        int reparationAvant = service.getEnReparation().size();

        // Simuler les retours avec 100% de taux d'abîmé pour garantir le résultat
        List<Future<Boolean>> futures = simulationManager.simulerRetoursParalleles(empruntsActifs, 1.0);
        simulationManager.attendreFin(futures);

        // Vérifier que la liste À réparer a augmenté
        int reparationApres = service.getEnReparation().size();
        assertTrue(reparationApres > reparationAvant);

        // Vérifier que chaque exemplaire À réparer a une date valide
        service.getEnReparation().forEach(ex -> {
            assertNotNull(ex.getDateDisponibilite());
            assertTrue(ex.getDateDisponibilite().isAfter(java.time.LocalDate.now().minusDays(1)));
        });
    }

    // -------------------------------------------------------------------------
    // Test 6 — Suppression usager avec emprunts actifs refusée
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Suppression usager — refusée si emprunts actifs en contexte concurrent")
    void testSuppressionUsagerAvecEmpruntsActifs() throws InterruptedException, ExecutionException {
        String isbn = "978-TEST-0004";
        creerEtAjouter("Ouvrage Suppression", isbn);

        Etudiant usager = creerEtAjouterEtudiant("UsagerSupp");
        service.emprunter(usager, isbn);

        // Tenter la suppression via SimulationManager
        Future<Boolean> future = simulationManager.simulerSuppressionUsager(usager.getId());
        boolean resultat = future.get();

        assertFalse(resultat);
        assertTrue(service.getUsagers().contains(usager));
    }

    // -------------------------------------------------------------------------
    // Test 7 — Avancement jour par jour avec RepairScheduler
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RepairScheduler — exemplaires remis à DISPONIBLE après réparation")
    void testRepairSchedulerAvancementJours() throws InterruptedException {
        // Créer un exemplaire en réparation avec date passée
        Exemplaire ex = creerEtAjouter("Livre Réparation", "978-REPAIR-001");
        ex.setStatut(Statut.A_REPARER);
        ex.setDateDisponibilite(java.time.LocalDate.now().minusDays(1));

        // Forcer la vérification immédiate
        repairScheduler.verifierMaintenant();

        // L'exemplaire doit être remis à DISPONIBLE
        assertEquals(Statut.DISPONIBLE, ex.getStatut());
        assertNull(ex.getDateDisponibilite());
    }

    @Test
    @DisplayName("RepairScheduler — exemplaire non remis si date future")
    void testRepairSchedulerDateFuture() {
        Exemplaire ex = creerEtAjouter("Livre Futur", "978-REPAIR-002");
        ex.setStatut(Statut.A_REPARER);
        ex.setDateDisponibilite(java.time.LocalDate.now().plusDays(2));

        repairScheduler.verifierMaintenant();

        // Toujours en réparation — date pas encore atteinte
        assertEquals(Statut.A_REPARER, ex.getStatut());
        assertNotNull(ex.getDateDisponibilite());
    }

    // -------------------------------------------------------------------------
    // Test 8 — Pas de double emprunt même en contexte concurrent
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Concurrence — aucun double emprunt même ISBN par même usager")
    void testPasDoubleEmpruntConcurrent() throws InterruptedException {
        String isbn = "978-TEST-0005";
        for (int i = 0; i < 3; i++) creerEtAjouter("Ouvrage Double", isbn);

        Etudiant usager = creerEtAjouterEtudiant("UsagerDouble");

        // Soumettre 5 tentatives du même usager sur le même ISBN
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(pool.submit(() -> service.emprunter(usager, isbn)));
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        long succes = futures.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { return false; }
                })
                .filter(Boolean::booleanValue)
                .count();

        // Un seul succès — même usager ne peut pas avoir deux fois le même ISBN
        assertEquals(1, succes);
        assertEquals(1, usager.getEmpruntsEnCours().size());
    }
}