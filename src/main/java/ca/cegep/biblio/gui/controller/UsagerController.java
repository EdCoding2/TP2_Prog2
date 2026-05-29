package ca.cegep.biblio.gui.controller;

import ca.cegep.biblio.gui.App;
import ca.cegep.biblio.model.*;
import ca.cegep.biblio.service.BibliothequeService;
import ca.cegep.biblio.util.IdGenerator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class UsagerController implements Initializable {

    @FXML private TableView<Usager>             usagerTableView;
    @FXML private TableColumn<Usager, String>   colNom;
    @FXML private TableColumn<Usager, String>   colId;
    @FXML private TableColumn<Usager, String>   colType;
    @FXML private TableColumn<Usager, String>   colEmprunts;
    @FXML private TableColumn<Usager, String>   colMax;
    @FXML private TableColumn<Usager, String>   colDuree;

    @FXML private TextField  rechercheUsagerField;
    @FXML private TitledPane formulairePane;
    @FXML private TextField  nomField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label      idLabel;
    @FXML private TextField  idField;
    @FXML private ListView<String> empruntsUsagerListView;
    @FXML private Label      messageLabel;

    private BibliothequeService service;
    private ObservableList<Usager> observableUsagers;
    private FilteredList<Usager>   filteredUsagers;
    private boolean modeAjout = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = App.getBibliothequeService();

        // Configurer les colonnes
        colNom.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNom()));
        colId.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getId()));
        colType.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getClass().getSimpleName()));
        colEmprunts.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(c.getValue().getEmpruntsEnCours().size())));
        colMax.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(c.getValue().getMaxLivres())));
        colDuree.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getDureeEmpruntJours() + " jours"));

        // Types disponibles dans le ComboBox
        typeCombo.setItems(FXCollections.observableArrayList(
                "Etudiant", "Professeur", "Visiteur"
        ));

        // Afficher les emprunts quand un usager est sélectionné
        usagerTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, nouveau) -> {
                    if (nouveau != null) afficherEmpruntsUsager(nouveau);
                });

        rafraichirTableau();
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @FXML
    private void ajouterUsager() {
        modeAjout = true;
        viderFormulaire();
        idLabel.setVisible(false);
        idField.setVisible(false);
        formulairePane.setExpanded(true);
        messageLabel.setText("");
    }

    @FXML
    private void modifierUsager() {
        Usager selectionne = usagerTableView.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            messageLabel.setText("⚠️ Veuillez sélectionner un usager.");
            return;
        }
        modeAjout = false;
        remplirFormulaire(selectionne);
        idLabel.setVisible(true);
        idField.setVisible(true);
        formulairePane.setExpanded(true);
        messageLabel.setText("");
    }

    @FXML
    private void supprimerUsager() {
        Usager selectionne = usagerTableView.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            messageLabel.setText("⚠️ Veuillez sélectionner un usager.");
            return;
        }

        // Vérifier emprunts actifs AVANT la confirmation
        if (!selectionne.getEmpruntsEnCours().isEmpty()) {
            messageLabel.setText("❌ Impossible — \"" + selectionne.getNom()
                    + "\" a " + selectionne.getEmpruntsEnCours().size()
                    + " emprunt(s) actif(s).");
            messageLabel.setStyle("-fx-text-fill: #f38ba8;");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'usager \"" + selectionne.getNom() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Confirmation");
        confirmation.showAndWait().ifPresent(reponse -> {
            if (reponse == ButtonType.YES) {
                boolean ok = service.supprimerUsager(selectionne.getId());
                if (ok) {
                    rafraichirTableau();
                    messageLabel.setText("✅ Usager supprimé.");
                    messageLabel.setStyle("-fx-text-fill: #a6e3a1;");
                } else {
                    messageLabel.setText("❌ Suppression refusée.");
                    messageLabel.setStyle("-fx-text-fill: #f38ba8;");
                }
            }
        });
    }

    @FXML
    private void confirmerFormulaire() {
        String nom  = nomField.getText().trim();
        String type = typeCombo.getValue();

        if (nom.isEmpty() || type == null) {
            messageLabel.setText("⚠️ Le nom et le type sont obligatoires.");
            return;
        }

        if (modeAjout) {
            Usager nouvel = creerUsager(type, nom);
            service.ajouterUsager(nouvel);
            messageLabel.setText("✅ Usager ajouté.");
        } else {
            Usager selectionne = usagerTableView.getSelectionModel().getSelectedItem();
            if (selectionne != null) {
                service.modifierUsager(selectionne.getId(), nom);
                messageLabel.setText("✅ Usager modifié.");
            }
        }

        rafraichirTableau();
        formulairePane.setExpanded(false);
        viderFormulaire();
    }

    @FXML
    private void annulerFormulaire() {
        formulairePane.setExpanded(false);
        viderFormulaire();
        messageLabel.setText("");
    }

    // -------------------------------------------------------------------------
    // Filtre en temps réel
    // -------------------------------------------------------------------------

    @FXML
    private void filtrerTableau() {
        String filtre = rechercheUsagerField.getText().toLowerCase();
        filteredUsagers.setPredicate(u ->
                filtre.isEmpty() ||
                u.getNom().toLowerCase().contains(filtre) ||
                u.getId().toLowerCase().contains(filtre) ||
                u.getClass().getSimpleName().toLowerCase().contains(filtre)
        );
    }

    // -------------------------------------------------------------------------
    // Affichage emprunts de l'usager sélectionné
    // -------------------------------------------------------------------------

    private void afficherEmpruntsUsager(Usager usager) {
        empruntsUsagerListView.getItems().setAll(
            usager.getEmpruntsEnCours().stream()
                    .map(e -> String.format("%-40s | Retour : %s",
                            e.getExemplaire().getTitre(),
                            e.getDateRetourPrevue()))
                    .toList()
        );
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    private Usager creerUsager(String type, String nom) {
        String id = IdGenerator.genererIdUsager();
        return switch (type) {
            case "Professeur" -> new Professeur(nom, id);
            case "Visiteur"   -> new Visiteur(nom, id);
            default           -> new Etudiant(nom, id);
        };
    }

    private void rafraichirTableau() {
        observableUsagers = FXCollections.observableArrayList(service.getUsagers());
        filteredUsagers   = new FilteredList<>(observableUsagers, u -> true);
        usagerTableView.setItems(filteredUsagers);
    }

    private void viderFormulaire() {
        nomField.clear();
        typeCombo.setValue(null);
        idField.clear();
    }

    private void remplirFormulaire(Usager usager) {
        nomField.setText(usager.getNom());
        typeCombo.setValue(usager.getClass().getSimpleName());
        idField.setText(usager.getId());
    }
}