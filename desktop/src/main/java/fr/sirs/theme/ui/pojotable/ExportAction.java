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
package fr.sirs.theme.ui.pojotable;

import fr.sirs.core.model.Element;
import fr.sirs.map.ExportTask;
import fr.sirs.theme.ui.PojoTable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.stage.DirectoryChooser;
import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.bean.BeanStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.storage.DataStores;

/**
 * Comportement par défaut des pojotables à l'action sur le bouton d'export.
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class ExportAction implements EventHandler<ActionEvent> {

    private final BeanFeatureSupplier featureSupplier;
    private final ObservableList<TableColumn<Element, ?>> currentColumns;

    // The extraFunction is used to update the created file in case some columns must be added to the file but can't via this process.
    // Example : the list of a désordre's observations is not part of its FeatureType.
    // Thus, the need of an extraFunction that will allow to add those attributes to the file.
    private final BiConsumer<File, List<Object>> extraFunction;

    public ExportAction(BeanFeatureSupplier sup, final ObservableList<TableColumn<Element, ?>> currentColumns) {
        this(sup, currentColumns, null);
    }

    public ExportAction(BeanFeatureSupplier sup, final ObservableList<TableColumn<Element, ?>> currentColumns, final BiConsumer<File, List<Object>> extraFunction) {
        this.featureSupplier = sup;
        this.currentColumns = currentColumns;
        this.extraFunction = extraFunction;
    }

    @Override
    public void handle(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(GeotkFX.getString(org.geotoolkit.gui.javafx.contexttree.menu.ExportItem.class, "folder"));
        final File folder = chooser.showDialog(null);

        if(folder!=null){
            try{
                final BeanStore store = new BeanStore(featureSupplier);
                final FeatureMapLayer layer = MapBuilder.createFeatureLayer(store.createSession(false)
                        .getFeatureCollection(QueryBuilder.all(store.getNames().iterator().next())));
                layer.setName(store.getNames().iterator().next().tip().toString());

                FileFeatureStoreFactory factory = (FileFeatureStoreFactory) DataStores.getFactoryById("csv");
                if (extraFunction != null) {
                    // Copy all the elements otherwise the selectedElement can be modified by the user during the call of the extraFunction
                    // with the consequence of losing the list of the elements.
                    final List<Object> elements = layer.getCollection().stream()
                            .map(b -> b.getUserData())
                            .map(map -> map.get("bean"))
                            .collect(Collectors.toList());
                    TaskManager.INSTANCE.submit(new ExportTask(layer, folder, factory, extractVisibleColumnsNameText(), extraFunction, elements));
                } else
                    TaskManager.INSTANCE.submit(new ExportTask(layer, folder, factory, extractVisibleColumnsNameText()));

            } catch (Exception ex) {
                Dialog d = new Alert(Alert.AlertType.ERROR, "Impossible de créer le fichier CSV", ButtonType.OK);
                d.setResizable(true);
                d.showAndWait();
                throw new UnsupportedOperationException("Failed to create csv store : " + ex.getMessage(), ex);
            }
        }
    }

    protected Map<String, String> extractVisibleColumnsNameText() {
        final Map<String, String> propertyNamesTexts = new HashMap<>();

        for (final TableColumn column : extractVisibleColumns(currentColumns)) {
            if (column.isVisible()) {
                if (column instanceof PojoTable.PropertyColumn) {
                    propertyNamesTexts.put(((PojoTable.PropertyColumn) column).getName(), column.getText());
                } else if (column instanceof PojoTable.EnumColumn) {
                    propertyNamesTexts.put(((PojoTable.EnumColumn) column).getName(), column.getText());
                } else if (column instanceof PojoTable.ObservationPropertyColumn) {
                    propertyNamesTexts.put(((PojoTable.ObservationPropertyColumn) column).getName(), column.getText());
                }
            }
        }
        return propertyNamesTexts;
    }

    public static List<TableColumn<Element, ?>> extractVisibleColumns(final ObservableList<TableColumn<Element, ?>> currentColumns) {
        final List<TableColumn<Element, ?>> propertyNames = new ArrayList<>();

        for (final TableColumn column : currentColumns) {
            if (column.isVisible()) {
                if (column instanceof PojoTable.PropertyColumn
                || column instanceof PojoTable.EnumColumn
                || column instanceof PojoTable.ObservationPropertyColumn) {
                    propertyNames.add(column);
                }
            }
        }
        return propertyNames;
    }

}
