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

import fr.sirs.core.model.OrganeProtectionCollective;
import fr.sirs.plugin.dependance.ui.AbstractDescriptionPane;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 * Thème des organes de protection collective
 *
 * @author Gavens Maxime (Geomatys)
 */
public class OrganeProtectionTheme extends AbstractDescriptionTheme {

    public OrganeProtectionTheme() {
        super("Organes de protection collective", "Organes de protection collective (AH)");
    }

    @Override
    public Parent createPane() {
        final BorderPane borderPane = new AbstractDescriptionPane("Tableau des organes de protection collective",  OrganeProtectionCollective.class);
        return borderPane;
    }
}
