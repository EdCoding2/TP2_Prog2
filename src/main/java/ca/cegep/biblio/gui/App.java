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
        bibliothequeService = new BibliothequeService();
        bibliothequeService.chargerDonnees();

        repairScheduler = new RepairScheduler(bibliothequeService);
        repairScheduler.demarrer();

        FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/ca/cegep/biblio/gui/view/main.fxml")
        );
        Scene scene = new Scene(loader.load());

        scene.getStylesheets().add(
                App.class.getResource("/styles.css").toExternalForm()
        );

        // Plein écran maximisé au démarrage
        stage.setMaximized(true);
        stage.setTitle("Bibliothèque — Cégep");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        try {
            if (bibliothequeService != null) {
                bibliothequeService.sauvegarderDonnees();
                System.out.println("Sauvegarde effectuée à la fermeture.");
            }
        } catch (Exception e) {
            System.out.println("Erreur sauvegarde fermeture : " + e.getMessage());
        }

        if (repairScheduler != null) {
            repairScheduler.arreter();
        }
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