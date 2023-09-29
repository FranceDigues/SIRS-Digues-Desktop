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
import java.util.List;
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

    public ExportAction(BeanFeatureSupplier sup, final ObservableList<TableColumn<Element, ?>> currentColumns) {
        this.featureSupplier = sup;
        this.currentColumns = currentColumns;
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
                TaskManager.INSTANCE.submit(new ExportTask(layer, folder, factory, extractVisibleColumnNames()));
            } catch (Exception ex) {
                Dialog d = new Alert(Alert.AlertType.ERROR, "Impossible de créer le fichier CSV", ButtonType.OK);
                d.setResizable(true);
                d.showAndWait();
                throw new UnsupportedOperationException("Failed to create csv store : " + ex.getMessage(), ex);
            }
        }
    }

    protected String[] extractVisibleColumnNames() {
        final List<String> propertyNames = new ArrayList<>();

        for (final TableColumn column : currentColumns) {
            if (column.isVisible()) {
                if (column instanceof PojoTable.PropertyColumn) {
                    propertyNames.add(((PojoTable.PropertyColumn) column).getName());
                } else if (column instanceof PojoTable.EnumColumn) {
                    propertyNames.add(((PojoTable.EnumColumn) column).getName());
                }
            }
        }
        return propertyNames.toArray(new String[propertyNames.size()]);
    }
}
