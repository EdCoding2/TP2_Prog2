package ca.cegep.biblio.unit;

import ca.cegep.biblio.model.*;
import ca.cegep.biblio.persistence.JsonPersistence;
import ca.cegep.biblio.util.IdGenerator;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceTest {

    private JsonPersistence persistence;
    private Path fichierTemp;

    private Exemplaire exemplaire1;
    private Exemplaire exemplaire2;
    private Etudiant etudiant;
    private Professeur professeur;
    private Visiteur visiteur;
    private Emprunt emprunt;

    @BeforeEach
    void setUp() throws IOException {
        persistence  = new JsonPersistence();
        fichierTemp  = Files.createTempFile("test-sauvegarde", ".json");

        exemplaire1 = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "Le Petit Prince",
                "Antoine de Saint-Exupéry",
                "978-2-07-040850-4",
                Statut.DISPONIBLE,
                EtatPhysique.BON
        );
        exemplaire2 = new Exemplaire(
                IdGenerator.genererIdExemplaire(),
                "1984",
                "George Orwell",
                "978-0-452-28423-4",
                Statut.DISPONIBLE,
                EtatPhysique.NEUF
        );

        etudiant   = new Etudiant("Alice", IdGenerator.genererIdUsager());
        professeur = new Professeur("Bob", IdGenerator.genererIdUsager());
        visiteur   = new Visiteur("Charlie", IdGenerator.genererIdUsager());

        // Créer un emprunt entre etudiant et exemplaire1
        emprunt = new Emprunt(
                exemplaire1,
                etudiant,
                java.time.LocalDate.now(),
                java.time.LocalDate.now().plusDays(14)
        );
        exemplaire1.setStatut(Statut.EMPRUNTE);
        etudiant.getEmpruntsEnCours().add(emprunt);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Supprimer le fichier temporaire après chaque test
        Files.deleteIfExists(fichierTemp);
    }

    // -------------------------------------------------------------------------
    // Tests sauvegarde de base
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sauvegarde — fichier JSON créé")
    void testSauvegardeCreeFichier() throws IOException {
        List<Exemplaire> exemplaires = List.of(exemplaire1, exemplaire2);
        List<Usager> usagers         = List.of(etudiant, professeur, visiteur);
        List<Emprunt> emprunts       = List.of(emprunt);

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        assertTrue(Files.exists(fichierTemp));
        assertTrue(Files.size(fichierTemp) > 0);
    }

    @Test
    @DisplayName("Sauvegarde — fichier JSON non vide")
    void testSauvegardeContenuNonVide() throws IOException {
        List<Exemplaire> exemplaires = List.of(exemplaire1);
        List<Usager> usagers         = List.of(etudiant);
        List<Emprunt> emprunts       = List.of(emprunt);

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        String contenu = Files.readString(fichierTemp);
        assertFalse(contenu.isBlank());
        assertTrue(contenu.contains("Le Petit Prince"));
        assertTrue(contenu.contains("Alice"));
    }

    // -------------------------------------------------------------------------
    // Tests chargement de base
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Chargement — fichier inexistant retourne null")
    void testChargementFichierInexistant() throws IOException {
        JsonPersistence.DonneesJson donnees = persistence.charger("fichier-inexistant.json");
        assertNull(donnees);
    }

    @Test
    @DisplayName("Chargement — nombre d'exemplaires correct")
    void testChargementNombreExemplaires() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>(List.of(exemplaire1, exemplaire2));
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant));
        List<Emprunt> emprunts       = new ArrayList<>();

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        assertNotNull(donnees);
        assertEquals(2, donnees.getExemplaires().size());
    }

    @Test
    @DisplayName("Chargement — nombre d'usagers correct")
    void testChargementNombreUsagers() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>();
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant, professeur, visiteur));
        List<Emprunt> emprunts       = new ArrayList<>();

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        assertNotNull(donnees);
        assertEquals(3, donnees.getUsagers().size());
    }

    // -------------------------------------------------------------------------
    // Tests fidélité des données
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Chargement — attributs exemplaire préservés")
    void testChargementAttributsExemplaire() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>(List.of(exemplaire1));
        List<Usager> usagers         = new ArrayList<>();
        List<Emprunt> emprunts       = new ArrayList<>();

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        Exemplaire charge = donnees.getExemplaires().get(0);

        assertEquals(exemplaire1.getIdExemplaire(), charge.getIdExemplaire());
        assertEquals(exemplaire1.getTitre(), charge.getTitre());
        assertEquals(exemplaire1.getAuteur(), charge.getAuteur());
        assertEquals(exemplaire1.getIsbn(), charge.getIsbn());
        assertEquals(exemplaire1.getStatut(), charge.getStatut());
        assertEquals(exemplaire1.getEtatPhysique(), charge.getEtatPhysique());
    }

    @Test
    @DisplayName("Chargement — polymorphisme Usager préservé")
    void testChargementPolymorphismeUsager() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>();
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant, professeur, visiteur));
        List<Emprunt> emprunts       = new ArrayList<>();

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        List<Usager> chargesUsagers = donnees.getUsagers();

        // Vérifier que les types sont préservés
        assertTrue(chargesUsagers.stream().anyMatch(u -> u instanceof Etudiant));
        assertTrue(chargesUsagers.stream().anyMatch(u -> u instanceof Professeur));
        assertTrue(chargesUsagers.stream().anyMatch(u -> u instanceof Visiteur));
    }

    @Test
    @DisplayName("Chargement — attributs usager préservés")
    void testChargementAttributsUsager() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>();
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant));
        List<Emprunt> emprunts       = new ArrayList<>();

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        Usager charge = donnees.getUsagers().get(0);

        assertEquals(etudiant.getNom(), charge.getNom());
        assertEquals(etudiant.getId(), charge.getId());
        assertInstanceOf(Etudiant.class, charge);
    }

    @Test
    @DisplayName("Chargement — dates emprunt préservées")
    void testChargementDatesEmprunt() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>(List.of(exemplaire1));
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant));
        List<Emprunt> emprunts       = new ArrayList<>(List.of(emprunt));

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        Emprunt chargeEmprunt = donnees.getEmprunts().get(0);

        assertEquals(emprunt.getDateEmprunt(), chargeEmprunt.getDateEmprunt());
        assertEquals(emprunt.getDateRetourPrevue(), chargeEmprunt.getDateRetourPrevue());
    }

    // -------------------------------------------------------------------------
    // Tests références croisées
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Chargement — références croisées Emprunt→Exemplaire reconstruites")
    void testChargementReferencesExemplaire() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>(List.of(exemplaire1));
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant));
        List<Emprunt> emprunts       = new ArrayList<>(List.of(emprunt));

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        Emprunt chargeEmprunt = donnees.getEmprunts().get(0);

        assertNotNull(chargeEmprunt.getExemplaire());
        assertEquals(exemplaire1.getIdExemplaire(),
                chargeEmprunt.getExemplaire().getIdExemplaire());
    }

    @Test
    @DisplayName("Chargement — références croisées Emprunt→Usager reconstruites")
    void testChargementReferencesUsager() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>(List.of(exemplaire1));
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant));
        List<Emprunt> emprunts       = new ArrayList<>(List.of(emprunt));

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        Emprunt chargeEmprunt = donnees.getEmprunts().get(0);

        assertNotNull(chargeEmprunt.getUsager());
        assertEquals(etudiant.getId(), chargeEmprunt.getUsager().getId());
    }

    @Test
    @DisplayName("Chargement — empruntsEnCours usager reconstruits")
    void testChargementEmpruntsEnCoursUsager() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>(List.of(exemplaire1));
        List<Usager> usagers         = new ArrayList<>(List.of(etudiant));
        List<Emprunt> emprunts       = new ArrayList<>(List.of(emprunt));

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        Usager chargeUsager = donnees.getUsagers().stream()
                .filter(u -> u.getId().equals(etudiant.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, chargeUsager.getEmpruntsEnCours().size());
    }

    // -------------------------------------------------------------------------
    // Tests collections vides
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sauvegarde et chargement — collections vides")
    void testSauvegardeChargementVide() throws IOException {
        List<Exemplaire> exemplaires = new ArrayList<>();
        List<Usager> usagers         = new ArrayList<>();
        List<Emprunt> emprunts       = new ArrayList<>();

        persistence.sauvegarder(exemplaires, usagers, emprunts, fichierTemp.toString());

        JsonPersistence.DonneesJson donnees = persistence.charger(fichierTemp.toString());
        assertNotNull(donnees);
        assertTrue(donnees.getExemplaires().isEmpty());
        assertTrue(donnees.getUsagers().isEmpty());
        assertTrue(donnees.getEmprunts().isEmpty());
    }
}