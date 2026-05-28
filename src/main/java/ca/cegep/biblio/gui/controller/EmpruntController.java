package ca.cegep.biblio.gui.controller;

import ca.cegep.biblio.gui.App;
import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EmpruntController implements Initializable {

    // --- Onglet emprunts en cours ---
    @FXML private TableView<Emprunt>             empruntTableView;
    @FXML private TableColumn<Emprunt, String>   colUsagerNom;
    @FXML private TableColumn<Emprunt, String>   colTitre;
    @FXML private TableColumn<Emprunt, String>   colIsbn;
    @FXML private TableColumn<Emprunt, String>   colDateEmprunt;
    @FXML private TableColumn<Emprunt, String>   colDateRetour;
    @FXML private TableColumn<Emprunt, String>   colRetard;

    // --- Onglet nouvel emprunt ---
    @FXML private ComboBox<Usager>  usagerEmpruntCombo;
    @FXML private TextField         isbnEmpruntField;
    @FXML private Label             disponibiliteLabel;
    @FXML private Label             messageEmpruntLabel;

    // --- Onglet retour ---
    @FXML private ComboBox<Usager>      usagerRetourCombo;
    @FXML private ComboBox<Exemplaire>  exemplaireRetourCombo;
    @FXML private ComboBox<EtatPhysique> etatRetourCombo;
    @FXML private Label                 messageRetourLabel;

    // --- Onglet retards ---
    @FXML private TableView<Emprunt>             retardTableView;
    @FXML private TableColumn<Emprunt, String>   colRetardUsager;
    @FXML private TableColumn<Emprunt, String>   colRetardTitre;
    @FXML private TableColumn<Emprunt, String>   colRetardDate;
    @FXML private TableColumn<Emprunt, String>   colRetardJours;

    private BibliothequeService service;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = App.getBibliothequeService();

        configurerColonnesEmprunts();
        configurerColonnesRetards();
        remplirComboBoxes();
        rafraichirEmpruntsEnCours();
        rafraichirRetards();

        // Vérifier disponibilité en temps réel quand ISBN change
        isbnEmpruntField.textProperty().addListener((obs, ancien, nouveau) ->
                verifierDisponibilite(nouveau.trim()));
    }

    // -------------------------------------------------------------------------
    // Configuration colonnes
    // -------------------------------------------------------------------------

    private void configurerColonnesEmprunts() {
        colUsagerNom.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsager().getNom()));
        colTitre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getExemplaire().getTitre()));
        colIsbn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getExemplaire().getIsbn()));
        colDateEmprunt.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDateEmprunt().toString()));
        colDateRetour.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDateRetourPrevue().toString()));
        colRetard.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().estEnRetard() ? "⚠️ Oui" : "✅ Non"));
    }

    private void configurerColonnesRetards() {
        colRetardUsager.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsager().getNom()));
        colRetardTitre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getExemplaire().getTitre()));
        colRetardDate.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDateRetourPrevue().toString()));
        colRetardJours.setCellValueFactory(c -> {
            long jours = ChronoUnit.DAYS.between(
                    c.getValue().getDateRetourPrevue(), LocalDate.now());
            return new SimpleStringProperty(jours + " jour(s)");
        });
    }

    // -------------------------------------------------------------------------
    // Remplir les ComboBox
    // -------------------------------------------------------------------------

    private void remplirComboBoxes() {
        // Usagers
        usagerEmpruntCombo.setItems(
                FXCollections.observableArrayList(service.getUsagers()));
        usagerRetourCombo.setItems(
                FXCollections.observableArrayList(service.getUsagers()));

        // État physique pour retour
        etatRetourCombo.setItems(
                FXCollections.observableArrayList(EtatPhysique.values()));

        // Affichage personnalisé des usagers dans les ComboBox
        usagerEmpruntCombo.setCellFactory(lv -> cellUsager());
        usagerEmpruntCombo.setButtonCell(cellUsager());
        usagerRetourCombo.setCellFactory(lv -> cellUsager());
        usagerRetourCombo.setButtonCell(cellUsager());

        // Affichage personnalisé des exemplaires
        exemplaireRetourCombo.setCellFactory(lv -> cellExemplaire());
        exemplaireRetourCombo.setButtonCell(cellExemplaire());
    }

    // -------------------------------------------------------------------------
    // Onglet nouvel emprunt
    // -------------------------------------------------------------------------

    private void verifierDisponibilite(String isbn) {
        if (isbn.isEmpty()) {
            disponibiliteLabel.setText("—");
            return;
        }

        List<Exemplaire> disponibles = service.rechercherParISBN(isbn).stream()
                .filter(e -> e.getStatut() == Statut.DISPONIBLE)
                .toList();

        if (!disponibles.isEmpty()) {
            disponibiliteLabel.setText("✅ " + disponibles.size() + " exemplaire(s) disponible(s)");
            disponibiliteLabel.setStyle("-fx-text-fill: green;");
        } else {
            Optional<LocalDate> dateProche = service.getDateRetourPlusProche(isbn);
            if (dateProche.isPresent()) {
                disponibiliteLabel.setText("❌ Tous empruntés — retour prévu le : "
                        + dateProche.get());
            } else {
                disponibiliteLabel.setText("❌ Aucun exemplaire trouvé pour cet ISBN");
            }
            disponibiliteLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void confirmerEmprunt() {
        Usager usager = usagerEmpruntCombo.getValue();
        String isbn   = isbnEmpruntField.getText().trim();

        if (usager == null || isbn.isEmpty()) {
            messageEmpruntLabel.setText("⚠️ Veuillez sélectionner un usager et entrer un ISBN.");
            return;
        }

        boolean ok = service.emprunter(usager, isbn);
        if (ok) {
            messageEmpruntLabel.setText("✅ Emprunt enregistré avec succès.");
            messageEmpruntLabel.setStyle("-fx-text-fill: green;");
            isbnEmpruntField.clear();
            disponibiliteLabel.setText("—");
            rafraichirEmpruntsEnCours();
            remplirComboBoxes();
        } else {
            messageEmpruntLabel.setText("❌ Emprunt refusé — vérifiez les règles.");
            messageEmpruntLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // -------------------------------------------------------------------------
    // Onglet retour
    // -------------------------------------------------------------------------

    @FXML
    private void chargerEmpruntsUsager() {
        Usager usager = usagerRetourCombo.getValue();
        if (usager == null) return;

        exemplaireRetourCombo.setItems(
                FXCollections.observableArrayList(
                        usager.getEmpruntsEnCours().stream()
                                .map(Emprunt::getExemplaire)
                                .toList()
                )
        );
    }

    @FXML
    private void confirmerRetour() {
        Usager usager           = usagerRetourCombo.getValue();
        Exemplaire exemplaire   = exemplaireRetourCombo.getValue();
        EtatPhysique etat       = etatRetourCombo.getValue();

        if (usager == null || exemplaire == null || etat == null) {
            messageRetourLabel.setText("⚠️ Veuillez remplir tous les champs.");
            return;
        }

        boolean ok = service.retourner(usager, exemplaire, etat);
        if (ok) {
            messageRetourLabel.setText("✅ Retour enregistré avec succès.");
            messageRetourLabel.setStyle("-fx-text-fill: green;");
            usagerRetourCombo.setValue(null);
            exemplaireRetourCombo.getItems().clear();
            etatRetourCombo.setValue(null);
            rafraichirEmpruntsEnCours();
            rafraichirRetards();
            remplirComboBoxes();
        } else {
            messageRetourLabel.setText("❌ Retour refusé — weekend ou emprunt introuvable.");
            messageRetourLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // -------------------------------------------------------------------------
    // Onglet retards
    // -------------------------------------------------------------------------

    @FXML
    private void rafraichirRetards() {
        retardTableView.setItems(
                FXCollections.observableArrayList(service.getEmpruntsEnRetard()));
    }

    // -------------------------------------------------------------------------
    // Rafraîchissement tableau principal
    // -------------------------------------------------------------------------

    private void rafraichirEmpruntsEnCours() {
        empruntTableView.setItems(
                FXCollections.observableArrayList(service.getEmprunts()));
    }

    // -------------------------------------------------------------------------
    // Cellules personnalisées
    // -------------------------------------------------------------------------

    private ListCell<Usager> cellUsager() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Usager u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null :
                        u.getNom() + " (" + u.getClass().getSimpleName() + ")");
            }
        };
    }

    private ListCell<Exemplaire> cellExemplaire() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Exemplaire e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null :
                        e.getTitre() + " — " + e.getIsbn());
            }
        };
    }
}