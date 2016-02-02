/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotoolkit.gui.javafx.contexttree;

import fr.sirs.CorePlugin;
import fr.sirs.Plugin;
import fr.sirs.Plugins;
import fr.sirs.core.authentication.SIRSAuthenticator;
import fr.sirs.core.component.DatabaseRegistry;
import java.net.Authenticator;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.gui.javafx.render2d.FXMapFrame;
import org.geotoolkit.gui.javafx.util.FXUtilities;
import org.geotoolkit.lang.Setup;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;

/**
 * COPIED FROM Geotoolkit.org. Remove on future release.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class FXMapContextTree extends BorderPane{

    private static final DataFormat MAPITEM_FORMAT = new DataFormat("contextItem");
    
    private final ObservableList<Object> menuItems = FXCollections.observableArrayList();
    private final TreeTableView<MapItem> treetable = new TreeTableView();
    private final ObjectProperty<MapContext> itemProperty = new SimpleObjectProperty<>();
    
    
    public FXMapContextTree() {
        this(null);
    }
    
    public FXMapContextTree(MapContext item){    
        setCenter(treetable);
        
        //configure treetable
        treetable.getColumns().add(new MapItemNameColumn());
        treetable.getColumns().add(new MapItemGlyphColumn());
        treetable.getColumns().add(new MapItemVisibleColumn());
        treetable.setTableMenuButtonVisible(false);
        treetable.setEditable(true);
        treetable.setContextMenu(new ContextMenu());
        
        treetable.setRowFactory(new Callback<TreeTableView<MapItem>, TreeTableRow<MapItem>>() {
            @Override
            public TreeTableRow<MapItem> call(TreeTableView<MapItem> param) {
                final TreeTableRow row = new TreeTableRow();
                initDragAndDrop(row);
                return row;
            }
        });
        treetable.getStylesheets().add("org/geotoolkit/gui/javafx/parameter/parameters.css");

        //this will cause the column width to fit the view area
        treetable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        final ContextMenu menu = new ContextMenu();
        treetable.setContextMenu(menu);
        
        //update context menu based on selected items
        treetable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treetable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener() {
            @Override
            public void onChanged(ListChangeListener.Change change) {
                final ObservableList items = menu.getItems();
                items.clear();
                final List<? extends TreeItem> selection = FXUtilities.getSelectionItems(treetable);
                for(int i=0,n=menuItems.size(); i<n; i++){
                    final Object candidate = menuItems.get(i);
                    if(candidate instanceof TreeMenuItem){
                        final MenuItem mc = ((TreeMenuItem)candidate).init(selection);
                        if(mc!=null) items.add(mc);
                    } else if(candidate instanceof SeparatorMenuItem){
                        //special case, we don't want any separator at the start or end
                        //or 2 succesive separators
                        if(i==0 || i==n-1 || items.isEmpty()) continue;
                        
                        if(items.get(items.size()-1) instanceof SeparatorMenuItem){
                            continue;
                        }
                        items.add((SeparatorMenuItem)candidate);
                        
                    }else if(candidate instanceof MenuItem){
                        items.add((MenuItem)candidate);
                    }
                }
                
                //special case, we don't want any separator at the start or end
                if(!items.isEmpty()){
                    if(items.get(0) instanceof SeparatorMenuItem){
                        items.remove(0);
                    }
                    if(!items.isEmpty()){
                        final int idx = items.size()-1;
                        if(items.get(idx) instanceof SeparatorMenuItem){
                            items.remove(idx);
                        }
                    }
                }
                
            }
        });
                
        
        treetable.setShowRoot(true);
        itemProperty.addListener(new ChangeListener<MapItem>() {
            @Override
            public void changed(ObservableValue<? extends MapItem> observable, MapItem oldValue, MapItem newValue) {
                 if(newValue==null){
                    treetable.setRoot(null);
                }else{
                    treetable.setRoot(new TreeMapItem(newValue));
                }
            }
        });
        
        setMapItem(item);
    }
    
    private void initDragAndDrop(final TreeTableRow<MapItem> row){
        row.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                final int selection = treetable.getSelectionModel().getSelectedIndex();
                final Dragboard db = treetable.startDragAndDrop(TransferMode.MOVE);
                db.setContent(Collections.singletonMap(MAPITEM_FORMAT, selection));
                event.consume();
            }
        });

        row.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                if (event.getDragboard().hasContent(MAPITEM_FORMAT)) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            }
        });

        row.setOnDragEntered(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {                
                event.consume();
                System.out.println(event.getGestureTarget());
            }
        });

        row.setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                event.consume();
            }
        });
        
        row.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {

                final Dragboard db = event.getDragboard();
                boolean success = false;

                conditions:
                if (db.hasContent(MAPITEM_FORMAT)) {
                    final int index = (Integer) db.getContent(MAPITEM_FORMAT);
                    if (index >= 0) {
                        final MapItem root = treetable.getRoot() == null ? null : treetable.getRoot().getValue();
                        if (root == null)
                            break conditions;

                        ObservableList<TreeItem<MapItem>> movedRows = treetable.getSelectionModel().getSelectedItems();
                        final TreeItem<MapItem> targetRow = row.getTreeItem();

                        // Prevent moving a row in itself
                        if (targetRow != null) {
                            movedRows = movedRows.filtered(toMove -> !FXUtilities.isParent(toMove, targetRow));
                        }
                        if (movedRows.isEmpty())
                            break conditions;

                        final MapItem targetItem = row.getItem();
                        final MapItem targetParent = (targetRow == null || targetRow.getParent() == null ? null : targetRow.getParent().getValue());

                        for (final TreeItem<MapItem> movedRow : movedRows) {
                            final MapItem movedItem = movedRow.getValue();
                            final MapItem movedParent = (movedRow.getParent() == null ? null : movedRow.getParent().getValue());
                            // Root or null item dragged. Cannot move them.
                            if (movedItem == null || movedParent == null) {
                                continue;
                            }

                            movedParent.items().remove(movedItem);
                            if (targetItem == null) {
                                // Insert in empty row, should be at end of the tree.
                                root.items().add(0, movedItem);
                            } else if (targetParent == null) {
                                // Add directly on root.
                                root.items().add(movedItem);
                            } else if (targetItem instanceof MapLayer) {
                                //insert as sibling
                                final int insertIndex = targetParent.items().indexOf(targetItem);
                                targetParent.items().add(insertIndex, movedItem);
                            } else {
                                //insert as children
                                targetItem.items().add(movedItem);
                            }
                        }
                    }
                    success = true;
                }

                // Clear selection to avoid random index selection on tree update.
                treetable.getSelectionModel().clearSelection();

                event.setDropCompleted(success);
                event.consume();
            }
        });

        row.setOnDragDone(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                event.consume();
            }
        });
    }
        
    public TreeTableView getTreetable() {
        return treetable;
    }

    /**
     * This list can contain MenuItem of TreeMenuItem.
     * 
     * @return ObservableList of contextual menu items.
     */
    public ObservableList<Object> getMenuItems() {
        return menuItems;
    }
    
    public ObjectProperty<MapContext> mapItemProperty(){
        return itemProperty;
    }
    
    public MapContext getMapItem() {
        return itemProperty.get();
    }

    public void setMapItem(MapContext mapItem) {
        itemProperty.set(mapItem);
    }

    public static void main(String... args) throws Exception {
        new JFXPanel();
        
        //Geotoolkit startup
        Setup.initialize(null);
        //work in lazy mode, do your best for lenient datum shift
        Hints.putSystemDefault(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

        // Allow authentication to CouchDb service.
        Authenticator.setDefault(new SIRSAuthenticator());
        
        final DatabaseRegistry registry = new DatabaseRegistry();
        registry.connectToSirsDatabase("isere3", false, false, false);
        final MapContext ctx = MapBuilder.createContext();
        ctx.setAreaOfInterest(new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), 5, 44, 2, 2));
        final Plugin plugin = Plugins.getPlugin(CorePlugin.NAME);
        plugin.load();
        ctx.items().addAll(plugin.getMapItems());
        FXMapFrame.show(ctx);
    }
}
