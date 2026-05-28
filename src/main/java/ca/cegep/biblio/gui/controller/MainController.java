package ca.cegep.biblio.gui.controller;

import ca.cegep.biblio.gui.App;
import ca.cegep.biblio.model.Exemplaire;
import ca.cegep.biblio.service.BibliothequeService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController implements Initializable {

    @FXML private Label     compteurLabel;
    @FXML private Label     nbReparationLabel;
    @FXML private ListView<String> reparationListView;
    @FXML private StackPane contenuPrincipal;

    private BibliothequeService service;
    private ScheduledExecutorService scheduler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = App.getBibliothequeService();

        // Rafraîchir le compteur et les réparations toutes les 2 secondes
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() ->
            Platform.runLater(this::rafraichirInfos),
            0, 2, TimeUnit.SECONDS
        );

        // Charger la vue livres par défaut
        ouvrirLivres();
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    @FXML
    private void ouvrirLivres() {
        chargerVue("/ca/cegep/biblio/gui/view/livre.fxml");
    }

    @FXML
    private void ouvrirUsagers() {
        chargerVue("/ca/cegep/biblio/gui/view/usager.fxml");
    }

    @FXML
    private void ouvrirEmprunts() {
        chargerVue("/ca/cegep/biblio/gui/view/emprunt.fxml");
    }

    @FXML
    private void ouvrirRecherche() {
        chargerVue("/ca/cegep/biblio/gui/view/recherche.fxml");
    }

    // -------------------------------------------------------------------------
    // Sauvegarde manuelle
    // -------------------------------------------------------------------------

    @FXML
    private void sauvegarder() {
        service.sauvegarderDonnees();
        rafraichirInfos();
    }

    // -------------------------------------------------------------------------
    // Rafraîchissement du compteur et des réparations
    // -------------------------------------------------------------------------

    private void rafraichirInfos() {
        // Compteur global
        compteurLabel.setText(String.valueOf(service.getCompteurEmprunts()));

        // Exemplaires en réparation
        List<Exemplaire> enReparation = service.getEnReparation();
        nbReparationLabel.setText(String.valueOf(enReparation.size()));

        reparationListView.getItems().setAll(
            enReparation.stream()
                .map(e -> String.format("%-40s | Dispo le : %s",
                        e.getTitre(),
                        e.getDateDisponibilite()))
                .toList()
        );
    }
    @FXML
    private void fermerApplication() {
        // Sauvegarde avant fermeture
        service.sauvegarderDonnees();
        if (App.getRepairScheduler() != null) {
            App.getRepairScheduler().arreter();
        }
        arreterScheduler();
        javafx.application.Platform.exit();
    }
    // -------------------------------------------------------------------------
    // Chargement dynamique des vues
    // -------------------------------------------------------------------------

    private void chargerVue(String cheminFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource(cheminFxml)
            );
            Node vue = loader.load();
            contenuPrincipal.getChildren().setAll(vue);
        } catch (IOException e) {
            System.out.println("Erreur chargement vue : " + e.getMessage());
        }
    }

    // Appelé par App.stop() pour arrêter proprement le scheduler UI
    public void arreterScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}