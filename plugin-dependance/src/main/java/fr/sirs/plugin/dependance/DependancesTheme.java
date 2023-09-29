/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.dependance;

import fr.sirs.Injector;
import fr.sirs.core.model.AireStockageDependance;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.AutreDependance;
import fr.sirs.core.model.CheminAccesDependance;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.OuvrageVoirieDependance;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SimpleFXEditMode;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Parent;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Panneau regroupant les dépendances.
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class DependancesTheme extends AbstractPluginsButtonTheme {
    private static final Image BUTTON_IMAGE = new Image(
            DependancesTheme.class.getResourceAsStream("images/dependance.png"));

    public DependancesTheme() {
        super("Gestion des dépendances", "Gestion des dépendances", BUTTON_IMAGE);
    }

    /**
     * Création du panneau contenant les 4 types possibles de dépendances.
     *
     * @return le conteneur de ce panneau.
     */
    @Override
    public Parent createPane() {
        final TabPane tabPane = new TabPane();
        final Tab ouvragesTab = new Tab("Ouvrages de voirie");
        ouvragesTab.setContent(createTablePane(OuvrageVoirieDependance.class, "Liste d'ouvrages de voirie"));
        ouvragesTab.setClosable(false);

        final Tab areaTab = new Tab("Aires de stockage");
        areaTab.setContent(createTablePane(AireStockageDependance.class, "Liste des aires de stockage"));
        areaTab.setClosable(false);

        final Tab accessPathTab = new Tab("Chemins d'accès");
        accessPathTab.setContent(createTablePane(CheminAccesDependance.class, "Liste des chemins d'accès"));
        accessPathTab.setClosable(false);

        final Tab arrangementsTab = new Tab("Aménagements hydrauliques");
        arrangementsTab.setContent(createTablePane(AmenagementHydraulique.class, "Liste des aménagements hydrauliques"));
        arrangementsTab.setClosable(false);

        final Tab othersTab = new Tab("Autres");
        othersTab.setContent(createTablePane(AutreDependance.class, "Liste des dépendances d'autres types"));
        othersTab.setClosable(false);

        tabPane.getTabs().add(ouvragesTab);
        tabPane.getTabs().add(areaTab);
        tabPane.getTabs().add(accessPathTab);
        tabPane.getTabs().add(arrangementsTab);
        tabPane.getTabs().add(othersTab);
        return new BorderPane(tabPane);
    }

    /**
     * Créé une {@linkplain PojoTable table} pour la classe fournie avec le titre donné.
     *
     * @param clazz Classe d'objets montrés par cette table.
     * @param title Titre à placer en tête de table.
     * @return le conteneur de cette table.
     */
    private BorderPane createTablePane(final Class clazz, final String title) {
        // Gestion du bouton consultation / édition pour la pojo table
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        final PojoTable dependancesTable = new PojoTable(Injector.getSession().getRepositoryForClass(clazz), title, (ObjectProperty<? extends Element>) null);
        dependancesTable.editableProperty().bind(editMode.editionState());
        return new BorderPane(dependancesTable, topPane, null, null, null);
    }
}
