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
package fr.sirs.plugin.document;

import fr.sirs.plugin.document.ui.DocumentsPane;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;

import java.io.File;

/**
 * Exemple de bouton de plugins
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class DocumentManagementTheme extends AbstractPluginsButtonTheme {

    private static final Image BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/gestion_documents.png"));
    private final FileTreeItem root;
    private final DynamicDocumentTheme dynDcTheme;

    public DocumentManagementTheme(final FileTreeItem root, final DynamicDocumentTheme dynDcTheme) {
        super("Gestion des documents", "Gestion des documents", BUTTON_IMAGE);
        if (root == null) {
            throw new IllegalArgumentException("root cannot be null");
        }
        this.root = root;
        this.dynDcTheme = dynDcTheme;
    }

    @Override
    public Parent createPane() {
        final BorderPane borderPane = new BorderPane(new DocumentsPane(root, dynDcTheme));

        return borderPane;
    }

    @Override
    public ChangeListener<Boolean> getSelectedPropertyListener() {
        return (observable, oldValue, newValue) -> {
            final File rootValue = root.getValue();
            if (newValue && rootValue != null) {
                DocumentPropertiesFileUtilities.updateFileSystem(rootValue);
                root.update(root.rootShowHiddenFile);
            }
        };
    }

}
