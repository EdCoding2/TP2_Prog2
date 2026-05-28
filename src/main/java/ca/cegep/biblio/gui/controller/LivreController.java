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

public class LivreController implements Initializable {

    @FXML private TableView<Exemplaire>             livreTableView;
    @FXML private TableColumn<Exemplaire, String>   colTitre;
    @FXML private TableColumn<Exemplaire, String>   colAuteur;
    @FXML private TableColumn<Exemplaire, String>   colIsbn;
    @FXML private TableColumn<Exemplaire, String>   colStatut;
    @FXML private TableColumn<Exemplaire, String>   colEtat;
    @FXML private TableColumn<Exemplaire, String>   colDispo;

    @FXML private TextField  rechercheLivreField;
    @FXML private TitledPane formulairePane;
    @FXML private TextField  titreField;
    @FXML private TextField  auteurField;
    @FXML private TextField  isbnField;
    @FXML private ComboBox<EtatPhysique> etatCombo;
    @FXML private ComboBox<Statut>       statutCombo;
    @FXML private Label      messageLabel;

    private BibliothequeService service;
    private ObservableList<Exemplaire> observableExemplaires;
    private FilteredList<Exemplaire>   filteredExemplaires;
    private boolean modeAjout = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = App.getBibliothequeService();

        // Configurer les colonnes
        colTitre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTitre()));
        colAuteur.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAuteur()));
        colIsbn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getIsbn()));
        colStatut.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatut().name()));
        colEtat.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEtatPhysique().name()));
        colDispo.setCellValueFactory(c -> {
            java.time.LocalDate d = c.getValue().getDateDisponibilite();
            return new SimpleStringProperty(d != null ? d.toString() : "—");
        });

        // Remplir les ComboBox
        etatCombo.setItems(FXCollections.observableArrayList(EtatPhysique.values()));
        statutCombo.setItems(FXCollections.observableArrayList(Statut.values()));

        // Charger les données
        rafraichirTableau();
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @FXML
    private void ajouterExemplaire() {
        modeAjout = true;
        viderFormulaire();
        formulairePane.setExpanded(true);
        messageLabel.setText("");
    }

    @FXML
    private void modifierExemplaire() {
        Exemplaire selectionne = livreTableView.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            messageLabel.setText("⚠️ Veuillez sélectionner un exemplaire.");
            return;
        }
        modeAjout = false;
        remplirFormulaire(selectionne);
        formulairePane.setExpanded(true);
        messageLabel.setText("");
    }

    @FXML
    private void supprimerExemplaire() {
        Exemplaire selectionne = livreTableView.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            messageLabel.setText("⚠️ Veuillez sélectionner un exemplaire.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + selectionne.getTitre() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Confirmation");
        confirmation.showAndWait().ifPresent(reponse -> {
            if (reponse == ButtonType.YES) {
                boolean ok = service.supprimerExemplaire(selectionne.getIdExemplaire());
                if (ok) {
                    rafraichirTableau();
                    messageLabel.setText("✅ Exemplaire supprimé.");
                } else {
                    messageLabel.setText("❌ Impossible — exemplaire actuellement emprunté.");
                }
            }
        });
    }

    @FXML
    private void confirmerFormulaire() {
        String titre  = titreField.getText().trim();
        String auteur = auteurField.getText().trim();
        String isbn   = isbnField.getText().trim();
        EtatPhysique etat   = etatCombo.getValue();
        Statut statut       = statutCombo.getValue();

        if (titre.isEmpty() || auteur.isEmpty() || isbn.isEmpty()
                || etat == null || statut == null) {
            messageLabel.setText("⚠️ Tous les champs sont obligatoires.");
            return;
        }

        if (modeAjout) {
            Exemplaire nouvel = new Exemplaire(
                    IdGenerator.genererIdExemplaire(),
                    titre, auteur, isbn, statut, etat
            );
            service.ajouterExemplaire(nouvel);
            messageLabel.setText("✅ Exemplaire ajouté.");
        } else {
            Exemplaire selectionne = livreTableView.getSelectionModel().getSelectedItem();
            if (selectionne != null) {
                service.modifierExemplaire(selectionne.getIdExemplaire(), titre, auteur, isbn);
                selectionne.setEtatPhysique(etat);
                selectionne.setStatut(statut);
                messageLabel.setText("✅ Exemplaire modifié.");
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
        String filtre = rechercheLivreField.getText().toLowerCase();
        filteredExemplaires.setPredicate(e ->
                filtre.isEmpty() ||
                e.getTitre().toLowerCase().contains(filtre) ||
                e.getAuteur().toLowerCase().contains(filtre) ||
                e.getIsbn().toLowerCase().contains(filtre)
        );
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    private void rafraichirTableau() {
        observableExemplaires = FXCollections.observableArrayList(service.getExemplaires());
        filteredExemplaires   = new FilteredList<>(observableExemplaires, e -> true);
        livreTableView.setItems(filteredExemplaires);
    }

    private void viderFormulaire() {
        titreField.clear();
        auteurField.clear();
        isbnField.clear();
        etatCombo.setValue(null);
        statutCombo.setValue(null);
    }

    private void remplirFormulaire(Exemplaire ex) {
        titreField.setText(ex.getTitre());
        auteurField.setText(ex.getAuteur());
        isbnField.setText(ex.getIsbn());
        etatCombo.setValue(ex.getEtatPhysique());
        statutCombo.setValue(ex.getStatut());
    }
}