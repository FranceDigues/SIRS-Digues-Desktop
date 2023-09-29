/*
 * This file is part of SIRS-Digues 2.
 *
 *  Copyright (C) 2021, FRANCE-DIGUES,
 *
 *  SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any
 *  later version.
 *
 *  SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.dependance;

import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;

/**
 * Panneau regroupant les thèmes de description des dépendance et des aménagements hydrauliques.
 *
 * @author Maxime Gavens (Geomatys)
 */
public final class DescriptionDependanceAHTheme extends AbstractPluginsButtonTheme {
    // UPDATE ICON ?
    private static final Image BUTTON_IMAGE = new Image(
            DescriptionDependanceAHTheme.class.getResourceAsStream("images/desordre.png"));

    public DescriptionDependanceAHTheme() {
        super("Description dépendance et AH", "Description dépendance et AH", BUTTON_IMAGE);
        getSubThemes().add(new StructureTheme());
        getSubThemes().add(new OuvrageAssocieTheme());
        getSubThemes().add(new DesordreTheme());
        getSubThemes().add(new PrestationTheme());
        getSubThemes().add(new OrganeProtectionTheme());
    }

    /**
     * Création du panneau principal de ce thème qui regroupera tous les éléments.
     *
     * @return Le panneau généré pour ce thème.
     */
    @Override
    public Parent createPane() {
        final BorderPane borderPane = new BorderPane();

        return borderPane;
    }
}
