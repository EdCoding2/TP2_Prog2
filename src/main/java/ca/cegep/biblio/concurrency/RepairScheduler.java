package ca.cegep.biblio.concurrency;

import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.model.Exemplaire;
import ca.cegep.biblio.model.Statut;
import ca.cegep.biblio.util.DateUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RepairScheduler {

    private ExecutorService executorService;
    private volatile boolean actif;
    private final BibliothequeService bibliothequeService;

    public RepairScheduler(BibliothequeService bibliothequeService) {
        this.bibliothequeService = bibliothequeService;
        this.actif = false;
    }

    // Démarre le scheduler dans un thread dédié
    public void demarrer() {
        actif = true;
        executorService = Executors.newSingleThreadExecutor();

        executorService.submit((Runnable) () -> {
            while (actif) {
                try {
                    // Attendre jusqu'au prochain jour ouvrable à 8h00
                    long secondesAttente = calculerSecondesJusquaProchain8h();
                    TimeUnit.SECONDS.sleep(secondesAttente);

                    // Vérifier seulement les jours ouvrables
                    if (DateUtils.estJourOuvrable(LocalDate.now())) {
                        verifierReparations();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        System.out.println("RepairScheduler démarré.");
    }

    // Vérifie les exemplaires en réparation et remet à DISPONIBLE si la date est atteinte
    private void verifierReparations() {
        List<Exemplaire> exemplaires = bibliothequeService.getExemplaires();

        synchronized (exemplaires) {
            LocalDate aujourdhui = LocalDate.now();

            exemplaires.stream()
                    .filter(e -> e.getStatut() == Statut.A_REPARER)
                    .filter(e -> e.getDateDisponibilite() != null)
                    .filter(e -> !e.getDateDisponibilite().isAfter(aujourdhui))
                    .forEach(e -> {
                        e.setStatut(Statut.DISPONIBLE);
                        e.setDateDisponibilite(null);
                        System.out.println("Exemplaire remis à DISPONIBLE : " + e.getTitre());
                    });
        }
    }

    // Calcule le nombre de secondes jusqu'au prochain 8h00
    private long calculerSecondesJusquaProchain8h() {
        LocalTime maintenant    = LocalTime.now();
        LocalTime cible         = LocalTime.of(8, 0);

        if (maintenant.isBefore(cible)) {
            return maintenant.until(cible, ChronoUnit.SECONDS);
        } else {
            // Prochain 8h00 = demain
            return maintenant.until(cible.plusHours(24), ChronoUnit.SECONDS);
        }
    }

    // Arrête proprement le scheduler
    public void arreter() {
        actif = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("RepairScheduler arrêté.");
    }

    // Méthode utilitaire pour forcer une vérification immédiate (utile pour les tests)
    public void verifierMaintenant() {
        verifierReparations();
    }
}