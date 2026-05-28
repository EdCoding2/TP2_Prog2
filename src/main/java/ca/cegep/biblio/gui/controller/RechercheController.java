package ca.cegep.biblio.gui.controller;

import ca.cegep.biblio.gui.App;
import ca.cegep.biblio.model.Exemplaire;
import ca.cegep.biblio.model.Statut;
import ca.cegep.biblio.service.BibliothequeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class RechercheController implements Initializable {

    @FXML private RadioButton radioTitre;
    @FXML private RadioButton radioAuteur;
    @FXML private RadioButton radioIsbn;
    @FXML private TextField   rechercheField;
    @FXML private Label       nbResultatsLabel;

    @FXML private TableView<Exemplaire>             resultatsTableView;
    @FXML private TableColumn<Exemplaire, String>   colTitre;
    @FXML private TableColumn<Exemplaire, String>   colAuteur;
    @FXML private TableColumn<Exemplaire, String>   colIsbn;
    @FXML private TableColumn<Exemplaire, String>   colStatut;
    @FXML private TableColumn<Exemplaire, String>   colEtat;
    @FXML private TableColumn<Exemplaire, String>   colDisponible;
    @FXML private TableColumn<Exemplaire, String>   colRetourProche;

    @FXML private TitledPane detailPane;
    @FXML private Label      detailTitre;
    @FXML private Label      detailAuteur;
    @FXML private Label      detailIsbn;
    @FXML private Label      detailStatut;
    @FXML private Label      detailDispo;
    @FXML private Label      detailRetour;

    private BibliothequeService service;
    private ToggleGroup toggleGroup;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = App.getBibliothequeService();

        // Grouper les radio buttons
        toggleGroup = new ToggleGroup();
        radioTitre.setToggleGroup(toggleGroup);
        radioAuteur.setToggleGroup(toggleGroup);
        radioIsbn.setToggleGroup(toggleGroup);
        radioTitre.setSelected(true); // Par défaut : titre

        // Configurer les colonnes
        configurerColonnes();

        // Afficher le détail quand un exemplaire est sélectionné
        resultatsTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, nouveau) -> {
                    if (nouveau != null) afficherDetail(nouveau);
                });
    }

    // -------------------------------------------------------------------------
    // Configuration colonnes
    // -------------------------------------------------------------------------

    private void configurerColonnes() {
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

        // Disponible ?
        colDisponible.setCellValueFactory(c -> {
            boolean dispo = c.getValue().getStatut() == Statut.DISPONIBLE;
            return new SimpleStringProperty(dispo ? "✅ Oui" : "❌ Non");
        });

        // Prochain retour — affiché seulement si tous les exemplaires du même
        // ISBN sont empruntés
        colRetourProche.setCellValueFactory(c -> {
            String isbn = c.getValue().getIsbn();
            boolean tousEmpruntes = service.rechercherParISBN(isbn).stream()
                    .allMatch(e -> e.getStatut() != Statut.DISPONIBLE);

            if (tousEmpruntes) {
                Optional<LocalDate> date = service.getDateRetourPlusProche(isbn);
                return new SimpleStringProperty(
                        date.map(LocalDate::toString).orElse("—"));
            }
            return new SimpleStringProperty("—");
        });
    }

    // -------------------------------------------------------------------------
    // Recherche
    // -------------------------------------------------------------------------

    @FXML
    private void rechercher() {
        String terme = rechercheField.getText().trim();
        if (terme.isEmpty()) {
            nbResultatsLabel.setText("—");
            resultatsTableView.getItems().clear();
            return;
        }

        List<Exemplaire> resultats = effectuerRecherche(terme);
        afficherResultats(resultats);
    }

    @FXML
    private void rechercherEnTempsReel() {
        String terme = rechercheField.getText().trim();
        if (terme.length() >= 2) {
            List<Exemplaire> resultats = effectuerRecherche(terme);
            afficherResultats(resultats);
        }
    }

    @FXML
    private void changerTypeRecherche() {
        // Relancer la recherche si un terme est déjà entré
        String terme = rechercheField.getText().trim();
        if (!terme.isEmpty()) {
            List<Exemplaire> resultats = effectuerRecherche(terme);
            afficherResultats(resultats);
        }
    }

    private List<Exemplaire> effectuerRecherche(String terme) {
        if (radioAuteur.isSelected()) {
            return service.rechercherParAuteur(terme);
        } else if (radioIsbn.isSelected()) {
            return service.rechercherParISBN(terme);
        } else {
            return service.rechercherParTitre(terme);
        }
    }

    private void afficherResultats(List<Exemplaire> resultats) {
        resultatsTableView.setItems(FXCollections.observableArrayList(resultats));
        nbResultatsLabel.setText(resultats.size() + " résultat(s)");
        detailPane.setExpanded(false);
    }

    // -------------------------------------------------------------------------
    // Effacer
    // -------------------------------------------------------------------------

    @FXML
    private void effacer() {
        rechercheField.clear();
        resultatsTableView.getItems().clear();
        nbResultatsLabel.setText("—");
        detailPane.setExpanded(false);
        radioTitre.setSelected(true);
    }

    // -------------------------------------------------------------------------
    // Panneau de détail
    // -------------------------------------------------------------------------

    private void afficherDetail(Exemplaire exemplaire) {
        String isbn = exemplaire.getIsbn();

        // Compter les exemplaires disponibles pour cet ISBN
        long nbDispo = service.rechercherParISBN(isbn).stream()
                .filter(e -> e.getStatut() == Statut.DISPONIBLE)
                .count();

        // Date de retour la plus proche si aucun dispo
        Optional<LocalDate> dateProche = service.getDateRetourPlusProche(isbn);

        detailTitre.setText(exemplaire.getTitre());
        detailAuteur.setText(exemplaire.getAuteur());
        detailIsbn.setText(isbn);
        detailStatut.setText(exemplaire.getStatut().name());
        detailDispo.setText(nbDispo + " exemplaire(s) disponible(s)");
        detailRetour.setText(nbDispo == 0
                ? dateProche.map(d -> "Le " + d).orElse("Aucun emprunt actif")
                : "—");

        detailPane.setExpanded(true);
    }
}