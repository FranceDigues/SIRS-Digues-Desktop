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
package fr.sirs.plugin.berge.ui;

import fr.sirs.core.model.Element;
import fr.sirs.theme.ui.PojoTable;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.geotoolkit.filter.DefaultPropertyIsLike;
import org.geotoolkit.filter.DefaultPropertyName;
import org.geotoolkit.filter.binarylogic.DefaultAnd;
import org.opengis.filter.Filter;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class BergesPojoTable extends PojoTable {

    private final TextField uiKeywordSearch = new TextField();
    private final List<String> COLUMNS_TO_REMOVE = Arrays.asList("typeTronconId", "amenagementHydrauliqueId");

     /**
     * Création de la table présentant les obligations réglementaires.
     *
     * @param clazz   Classe d'objets affichés par cette table
     */
    public BergesPojoTable(final Class clazz, final ObjectProperty<? extends Element> container) {
        super(clazz, "Liste des berges", container);

        // Remove the non-necessary colomns
        this.getTable().getColumns().removeIf(c -> COLUMNS_TO_REMOVE.contains(c.getId()));


        if (getFilterUI() instanceof VBox) {
            final VBox vbox = (VBox) getFilterUI();
            final HBox hbox = new HBox();
            hbox.setSpacing(20);
            final Label label = new Label("Recherche par mots clés");
            label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            hbox.getChildren().add(label);
            hbox.getChildren().add(uiKeywordSearch);
            vbox.getChildren().add(vbox.getChildren().size() - 1, hbox);
        }

        createNewProperty().set(false);
        uiAdd.visibleProperty().set(false);
        setDeletor(b -> repo.remove(b));
    }

    @Override
    public Filter getFilter() {
        if (uiKeywordSearch == null || uiKeywordSearch.getText().isEmpty()) {
            return super.getFilter();
        }

        final Filter filterKWSearch = new DefaultPropertyIsLike(new DefaultPropertyName("libellé"), uiKeywordSearch.getText() + "*", "*", "?", "\\", false);
        final Filter filter = super.getFilter();
        if (filter == null) {
            return filterKWSearch;
        }

        return new DefaultAnd(filterKWSearch, filter);
    }

    @Override
    public void resetFilter(final VBox filterContent) {
        super.resetFilter(filterContent);

        uiKeywordSearch.setText("");
    }
}
