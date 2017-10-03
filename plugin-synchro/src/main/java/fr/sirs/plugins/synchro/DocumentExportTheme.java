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
package fr.sirs.plugins.synchro;

import fr.sirs.plugins.synchro.ui.mount.DocumentExportPane;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import javafx.scene.Parent;
import javafx.scene.image.Image;

/**
 * Exemple de bouton de plugins
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class DocumentExportTheme extends AbstractPluginsButtonTheme {

    private static final Image ICON = new Image(DocumentExportTheme.class.getResourceAsStream("documentExport.png"));

    public DocumentExportTheme() {
        super("Export de documents", "Interface permettant de transférer des documents depuis l'application de bureau vers l'application mobile.", ICON);
    }

    @Override
    public Parent createPane() {
        return new DocumentExportPane();
    }
}
