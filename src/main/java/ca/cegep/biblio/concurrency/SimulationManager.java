package ca.cegep.biblio.concurrency;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SimulationManager {

    private ExecutorService executorService;
    private final BibliothequeService bibliothequeService;

    public SimulationManager(BibliothequeService bibliothequeService, int nbThreads) {
        this.bibliothequeService = bibliothequeService;
        this.executorService     = Executors.newFixedThreadPool(nbThreads);
    }

    // -------------------------------------------------------------------------
    // Simulation d'emprunts parallèles
    // -------------------------------------------------------------------------

    // Soumet un Callable par usager — retourne la liste des Future<Boolean>
    public List<Future<Boolean>> simulerEmpruntsParalleles(List<Usager> usagers, List<String> isbns) {
        List<Future<Boolean>> resultats = new ArrayList<>();

        for (int i = 0; i < usagers.size(); i++) {
            final Usager usager = usagers.get(i);
            final String isbn   = isbns.get(i % isbns.size());

            Callable<Boolean> tache = () -> bibliothequeService.emprunter(usager, isbn);
            resultats.add(executorService.submit(tache));
        }

        return resultats;
    }

    // -------------------------------------------------------------------------
    // Simulation de retours parallèles
    // -------------------------------------------------------------------------

    // Soumet un Callable par emprunt — retourne la liste des Future<Boolean>
    public List<Future<Boolean>> simulerRetoursParalleles(List<Emprunt> emprunts,
                                                          double tauxAbime) {
        List<Future<Boolean>> resultats = new ArrayList<>();

        for (Emprunt emprunt : emprunts) {
            Callable<Boolean> tache = () -> {
                // 10% de chance que l'exemplaire revienne abîmé
                EtatPhysique etat = Math.random() < tauxAbime
                        ? EtatPhysique.A_REPARER
                        : EtatPhysique.BON;

                return bibliothequeService.retourner(
                        emprunt.getUsager(),
                        emprunt.getExemplaire(),
                        etat
                );
            };
            resultats.add(executorService.submit(tache));
        }

        return resultats;
    }

    // -------------------------------------------------------------------------
    // Simulation ajout/retrait de livres en parallèle
    // -------------------------------------------------------------------------

    // Soumet des Runnable pour ajout et suppression simultanés
    public void simulerModificationCatalogue(List<Exemplaire> aAjouter,
                                             List<String> idsASupprimer) {
        // Ajouts
        for (Exemplaire exemplaire : aAjouter) {
            executorService.submit((Runnable) () ->
                    bibliothequeService.ajouterExemplaire(exemplaire));
        }

        // Suppressions
        for (String id : idsASupprimer) {
            executorService.submit((Runnable) () ->
                    bibliothequeService.supprimerExemplaire(id));
        }
    }

    // -------------------------------------------------------------------------
    // Simulation suppression d'un usager avec emprunts actifs
    // -------------------------------------------------------------------------

    public Future<Boolean> simulerSuppressionUsager(String idUsager) {
        Callable<Boolean> tache = () -> bibliothequeService.supprimerUsager(idUsager);
        return executorService.submit(tache);
    }

    // -------------------------------------------------------------------------
    // Attendre la fin de toutes les tâches soumises
    // -------------------------------------------------------------------------

    public void attendreFin(List<Future<Boolean>> futures) throws InterruptedException {
        for (Future<Boolean> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                System.out.println("Erreur dans une tâche : " + e.getCause().getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Avancement jour par jour avec RepairScheduler
    // -------------------------------------------------------------------------

    // Simule l'avancement d'un nombre de jours ouvrables
    public void simulerJoursOuvrables(int nbJours, RepairScheduler repairScheduler) {
        for (int i = 0; i < nbJours; i++) {
            executorService.submit((Runnable) repairScheduler::verifierMaintenant);
        }
    }

    // -------------------------------------------------------------------------
    // Arrêt propre
    // -------------------------------------------------------------------------

    public void arreter() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("SimulationManager arrêté.");
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public ExecutorService getExecutorService() { return executorService; }
}