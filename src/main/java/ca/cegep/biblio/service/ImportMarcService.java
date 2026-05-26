package ca.cegep.biblio.service;

import ca.cegep.biblio.model.EtatPhysique;
import ca.cegep.biblio.model.Exemplaire;
import ca.cegep.biblio.model.Statut;
import ca.cegep.biblio.util.IdGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ImportMarcService {

    private static final Random random = new Random();

    // Point d'entrée principal
    public List<Exemplaire> importerFichier(String cheminFichier, boolean ajouterACollection) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(cheminFichier));
        List<Exemplaire> exemplaires = new ArrayList<>();
        int position = 0;

        while (position < data.length) {
            // Le leader fait 24 caractères — les 5 premiers indiquent la longueur de la notice
            if (position + 24 > data.length) break;

            String longueurStr = new String(data, position, 5);
            int longueur;
            try {
                longueur = Integer.parseInt(longueurStr.trim());
            } catch (NumberFormatException e) {
                break;
            }

            if (longueur <= 0 || position + longueur > data.length) break;

            // Extraire la notice complète
            String notice = new String(data, position, longueur, java.nio.charset.StandardCharsets.UTF_8);

            // Parser les champs
            String titre  = extraireChamp(notice, "245");
            String auteur = extraireChamp(notice, "100");
            if (auteur.isEmpty()) auteur = extraireChamp(notice, "700");
            String isbn   = extraireChamp(notice, "020");

            // Nettoyer les valeurs
            titre  = nettoyer(titre);
            auteur = nettoyer(auteur);
            isbn   = nettoyerIsbn(isbn);

            if (!titre.isEmpty()) {
                int nbExemplaires = determinerNbExemplaires();
                for (int i = 0; i < nbExemplaires; i++) {
                    Exemplaire ex = new Exemplaire(
                            IdGenerator.genererIdExemplaire(),
                            titre,
                            auteur,
                            isbn,
                            Statut.DISPONIBLE,
                            EtatPhysique.BON
                    );
                    exemplaires.add(ex);
                }
            }

            position += longueur;
        }

        return exemplaires;
    }

    // Détermine le nombre d'exemplaires selon la distribution du TP
    private int determinerNbExemplaires() {
        int r = random.nextInt(100);
        if (r < 50) return 1;       // 50%
        if (r < 80) return 2;       // 30%
        if (r < 95) return 3;       // 15%
        return 4;                   //  5%
    }

    // Extrait la valeur brute d'un champ MARC par son code à 3 chiffres
    private String extraireChamp(String notice, String codeChamp) {
        // Le répertoire commence à la position 24 du leader
        // Chaque entrée du répertoire = 12 caractères (tag 3 + longueur 4 + position 5)
        try {
            String baseAdresseStr = notice.substring(12, 17);
            int baseAdresse = Integer.parseInt(baseAdresseStr.trim());

            int i = 24;
            while (i + 12 <= baseAdresse) {
                String tag      = notice.substring(i, i + 3);
                String longStr  = notice.substring(i + 3, i + 7);
                String posStr   = notice.substring(i + 7, i + 12);

                int longueurChamp = Integer.parseInt(longStr.trim());
                int posChamp      = Integer.parseInt(posStr.trim());

                if (tag.equals(codeChamp)) {
                    int debut = baseAdresse + posChamp;
                    if (debut + longueurChamp <= notice.length()) {
                        return notice.substring(debut, debut + longueurChamp);
                    }
                }
                i += 12;
            }
        } catch (Exception e) {
            // Notice mal formée — on ignore silencieusement
        }
        return "";
    }

    // Supprime les indicateurs MARC et les sous-champs ($a, $b, etc.)
    private String nettoyer(String valeur) {
        if (valeur.length() > 2) {
            valeur = valeur.substring(2); // Sauter les 2 indicateurs
        }
        // Garder seulement le contenu du sous-champ $a
        valeur = valeur.replaceAll("\u001F[a-z]", " "); // \u001F = séparateur de sous-champ
        valeur = valeur.replaceAll("[/,:=]$", "").trim();
        return valeur;
    }

    // Nettoie l'ISBN (garde seulement les chiffres et tirets)
    private String nettoyerIsbn(String valeur) {
        if (valeur.length() > 2) {
            valeur = valeur.substring(2);
        }
        valeur = valeur.replaceAll("\u001F[a-z]", " ");
        // Garder seulement la partie numérique
        valeur = valeur.replaceAll("[^0-9\\-X]", "").trim();
        return valeur;
    }
}