/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.reglementaire;

import fr.sirs.Injector;
import fr.sirs.PropertiesFileUtilities;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.HorodatageReference;
import fr.sirs.core.model.RefHorodatageStatus;
import fr.sirs.plugin.reglementaire.ui.*;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.prefs.Preferences;

/**
 * Panel gathering the Registre functionalities.
 * <p>
 * Redmine ticket #7782
 *
 * @author Estelle Idee (Geomatys)
 */
public final class RegistreTheme extends AbstractPluginsButtonTheme {
    private static final Image BUTTON_IMAGE = new Image(
            RegistreTheme.class.getResourceAsStream("images/gen_etats.png"));

    public RegistreTheme() {
        super(SirsCore.REGISTRE_THEME_NAME, SirsCore.REGISTRE_THEME_NAME, BUTTON_IMAGE);
    }

    public static final String EXTRACTION_TAB = "Extraction";
    private static final String PATH_KEY = "path";
    private HorodatageReportPane horodatageReportPane;
    private ExtractionDocumentsPane extractionPane;

    @Override
    public Parent createPane() {
        // Check whether all @RefHorodatageStatus are available : Non horodaté, En attente, Horodaté.

        if (HorodatageReference.getRefNonTimeStampedStatus() == null || HorodatageReference.getRefWaitingStatus() == null || HorodatageReference.getRefTimeStampedStatus() == null) {
            PropertiesFileUtilities.showInformationDialog("Les statuts d'horodatage suivants sont nécessaires au fonctionnement de la génération du tableau de synthèse des prestations : " +
                    "\n" +
                    "\n- Non horodaté" +
                    "\n- En attente" +
                    "\n- Horodaté" +
                    "\n\nVeuillez mettre à jour la table de référence avant de revenir au menu Registre." +
                    "\n\nImportant : le redémarrage de l'application est nécessaire afin de prendre en compte les nouvelles valeurs de référence.",
                    "Statuts d'horodatage manquant",
                    650, 400);

            final Session session = Injector.getSession();
            session.getFrame().addTab(session.getOrCreateReferenceTypeTab(RefHorodatageStatus.class));
            throw new IllegalStateException("Missing timestamp status in " + RefHorodatageStatus.class.getSimpleName());
        }

        final BorderPane borderPane = new BorderPane();
        final TabPane tabPane = new TabPane();

        // Tab to generate "Tableaux de Synthese" for Prestation.
        final Tab syntheseTab = new Tab("Tableaux de synthèse");
        syntheseTab.setClosable(false);
        horodatageReportPane = new HorodatageReportPane();
        syntheseTab.setContent(horodatageReportPane);

        // Tab to import timestamped "Tableaux de Synthèse" and organise files.
        final Tab gestionTab = new Tab("Gestion");
        gestionTab.setClosable(false);
        FileTreeItem root = new FileTreeItem(false);
        gestionTab.setContent(new RegistreDocumentsPane(root));

        // Tab to create the final report with cover page and summary table.
        final Tab extractionTab = new Tab(EXTRACTION_TAB);
        extractionTab.setClosable(false);
        extractionPane = new ExtractionDocumentsPane(root);
        extractionTab.setContent(extractionPane);

        // Add created tabs
        tabPane.getTabs().add(syntheseTab);
        tabPane.getTabs().add(gestionTab);
        tabPane.getTabs().add(extractionTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.equals(syntheseTab)) {
                // forces to refresh the Prestation list when prestations have been modified from the PojoTable, the FXPrestationPane or elsewhere.
                if (horodatageReportPane != null) horodatageReportPane.refresh();
            }
        });

        borderPane.setCenter(tabPane);
        return borderPane;
    }

     public static javafx.util.Callback<DatePicker, javafx.scene.control.DateCell> getUiPeriodDebutDayCellFactory(final DatePicker uiPeriodFin) {
        return param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                final LocalDate dateFin = uiPeriodFin.getValue();
                setDisable(empty || (dateFin != null && date.isAfter(dateFin)));
            }
        };
    }

    public static javafx.util.Callback<DatePicker, javafx.scene.control.DateCell> getUiPeriodFinDayCellFactory(final DatePicker uiPeriodDebut) {
        return param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                final LocalDate dateDebut = uiPeriodDebut.getValue();
                setDisable(empty || (dateDebut != null && date.isBefore(dateDebut)));
            }
        };
    }

    /**
     * @return Last chosen path for generation report, or null if we cannot find any.
     * @param clazz the class to get the preferences for.
     * @return the path for the class.
     */
    public static Path getPreviousPath(final Class clazz) {
        final Preferences prefs = Preferences.userNodeForPackage(clazz);
        final String str        = prefs.get(PATH_KEY, null);
        if (str != null) {
            final Path file = Paths.get(str);
            if (Files.isDirectory(file)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Set value to be retrieved by {@link #getPreviousPath(Class)} () }.
     *
     * @param path To put as previously chosen path. Should be a directory.
     * @param clazz the class to set the preferences for.
     */
    public static void setPreviousPath(final Path path, final Class clazz) {
        final Preferences prefs = Preferences.userNodeForPackage(clazz);
        prefs.put(PATH_KEY, path.toAbsolutePath().toString());
    }

    @Override
    public void resetContent() {
        if (horodatageReportPane != null) horodatageReportPane.resetPane();
        if (extractionPane != null) extractionPane.resetPane();
    }

    /**
     * Method to be implemented if required to refresh the theme content.
     * Modify method Session.getOrCreateThemeTab(Theme) to call this method when selecting/unselecting the theme tab for a specific Theme.
     */
    public void refresh() {
        // forces to refresh the Prestation list when prestations have been modified from the PojoTable, the FXPrestationPane or elsewhere.
        if (horodatageReportPane != null) horodatageReportPane.refresh();
    }
}
