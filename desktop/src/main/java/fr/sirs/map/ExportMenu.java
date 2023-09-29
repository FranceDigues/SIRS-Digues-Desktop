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
package fr.sirs.map;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.contexttree.TreeMenuItem;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.FactoryMetadata;

/**
 * Export selected layer in the context tree.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ExportMenu extends TreeMenuItem {

    private static final Image ICON = SwingFXUtils.toFXImage(
            IconBuilder.createImage(FontAwesomeIcons.ICON_DOWNLOAD, 16, FontAwesomeIcons.DEFAULT_COLOR), null);

    private final Map<FileChooser.ExtensionFilter, FileFeatureStoreFactory> index = new HashMap<>();
    private WeakReference<TreeItem> itemRef;

    public ExportMenu() {

        menuItem = new Menu(GeotkFX.getString(org.geotoolkit.gui.javafx.contexttree.menu.ExportItem.class,"export"));
        menuItem.setGraphic(new ImageView(ICON));

        //select file factories which support writing
        final Set<FileFeatureStoreFactory> factories = DataStores.getAvailableFactories(FileFeatureStoreFactory.class);
        for(final FileFeatureStoreFactory factory : factories){
            final FactoryMetadata metadata = factory.getMetadata();
            if(metadata.supportStoreCreation()
                    && metadata.supportStoreWriting()
                    && metadata.supportedGeometryTypes().length>0){
                final String[] exts = factory.getFileExtensions();
                final String name = factory.getDisplayName().toString();
                final FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(name, exts);
                index.put(filter, factory);

                ((Menu)menuItem).getItems().add(new ExportMenuItem(factory));
            }
        }

    }

    @Override
    public MenuItem init(List<? extends TreeItem> selection) {
        boolean valid = uniqueAndType(selection,FeatureMapLayer.class) && !index.isEmpty();
        if(valid && selection.get(0).getParent()!=null){
            itemRef = new WeakReference<>(selection.get(0));
            return menuItem;
        }
        return null;
    }

    private class ExportMenuItem extends MenuItem {

        public ExportMenuItem(FileFeatureStoreFactory factory) {
            super(factory.getDisplayName().toString());

            setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    if(itemRef == null) return;
                    final TreeItem treeItem = itemRef.get();
                    if(treeItem == null) return;

                    final FeatureMapLayer layer = (FeatureMapLayer) treeItem.getValue();

                    final DirectoryChooser chooser = new DirectoryChooser();
                    chooser.setTitle(GeotkFX.getString(org.geotoolkit.gui.javafx.contexttree.menu.ExportItem.class, "folder"));

                    final File folder = chooser.showDialog(null);

                    if(folder!=null){
                        TaskManager.INSTANCE.submit(new ExportTask(layer, folder, factory, null));
                    }
                }
            });
        }

    }

}
