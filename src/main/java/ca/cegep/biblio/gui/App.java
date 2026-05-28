package ca.cegep.biblio.gui;

import ca.cegep.biblio.concurrency.RepairScheduler;
import ca.cegep.biblio.service.BibliothequeService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    // Instance unique partagée entre tous les contrôleurs
    private static BibliothequeService bibliothequeService;
    private static RepairScheduler repairScheduler;

    @Override
    public void start(Stage stage) throws IOException {
        // Initialiser le service principal
        bibliothequeService = new BibliothequeService();

        // Charger les données sauvegardées
        bibliothequeService.chargerDonnees();

        // Démarrer le scheduler de réparation
        repairScheduler = new RepairScheduler(bibliothequeService);
        repairScheduler.demarrer();

        // Charger la vue principale
        FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/ca/cegep/biblio/gui/view/main.fxml")
        );
        Scene scene = new Scene(loader.load(), 1200, 800);

        // Appliquer le CSS
        scene.getStylesheets().add(
                App.class.getResource("/styles.css").toExternalForm()
        );

        stage.setTitle("Bibliothèque — Cégep");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    @Override
    public void stop() {
        // Sauvegarde automatique à la fermeture
        if (bibliothequeService != null) {
            bibliothequeService.sauvegarderDonnees();
        }

        // Arrêter proprement le scheduler
        if (repairScheduler != null) {
            repairScheduler.arreter();
        }

        System.out.println("Application fermée proprement.");
    }

    // Getter statique — accessible depuis tous les contrôleurs
    public static BibliothequeService getBibliothequeService() {
        return bibliothequeService;
    }

    public static RepairScheduler getRepairScheduler() {
        return repairScheduler;
    }

    public static void main(String[] args) {
        launch(args);
    }
}