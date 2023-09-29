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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.AUTHOR_FIELD;
import static fr.sirs.SIRS.FOREIGN_PARENT_ID_FIELD;
import static fr.sirs.SIRS.VALID_FIELD;
import fr.sirs.StructBeanSupplier;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.Preview;
import fr.sirs.index.ElementHit;
import fr.sirs.map.ExportTask;
import static fr.sirs.theme.ui.PojoTable.editElement;
import fr.sirs.util.property.Reference;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.bean.BeanStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.gui.javafx.util.FXTableView;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.storage.DataStores;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ObjectTable extends BorderPane {

    private static final String BUTTON_STYLE = "buttonbar-button";
    private static final String[] COLUMNS_TO_IGNORE = new String[] {
        "documentId", "elementClassName", //ElementHit
        "docClass","elementId","elementClass","docId", //Preview
        AUTHOR_FIELD, VALID_FIELD, FOREIGN_PARENT_ID_FIELD};

    // Barre de droite : manipulation du tableau et passage en mode parcours de fiche
    protected final ToggleButton uiFicheMode = new ToggleButton(null, new ImageView(SIRS.ICON_FILE_WHITE));
    protected final Button uiExport = new Button(null, new ImageView(SIRS.ICON_EXPORT_WHITE));
    protected final HBox searchEditionToolbar = new HBox(uiFicheMode,uiExport);
    private final LabelMapper labelMapper;

    // Barre de gauche : navigation dans le parcours de fiches
    protected FXElementPane elementPane = null;
    private final Button uiPrevious = new Button("",new ImageView(SIRS.ICON_CARET_LEFT));
    private final Button uiNext = new Button("",new ImageView(SIRS.ICON_CARET_RIGHT));
    private final Button uiCurrent = new Button();
    protected final HBox navigationToolbar = new HBox(uiPrevious, uiCurrent, uiNext);

    protected final Class pojoClass;
    protected final BorderPane topPane;
    protected final TableView<Object> uiTable = new FXTableView<>();


    public ObjectTable(final Class pojoClass, final String title) {
        this.pojoClass = pojoClass;
        this.labelMapper = LabelMapper.get(this.pojoClass);

        final Label uiTitle = new Label(title);
        uiTitle.getStyleClass().add("pojotable-header");
        uiTitle.setAlignment(Pos.CENTER);

        searchEditionToolbar.getStyleClass().add("buttonbar");


        topPane = new BorderPane(uiTitle,null,searchEditionToolbar,null,null);
        setTop(topPane);
        uiTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        uiTable.setMaxWidth(Double.MAX_VALUE);
        uiTable.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        uiTable.setPlaceholder(new Label(""));
        uiTable.setTableMenuButtonVisible(true);
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        final EditColumn editCol = new EditColumn(this::editPojo);
        uiTable.getColumns().add(editCol);
        try {
            final HashMap<String, PropertyDescriptor> properties = SIRS.listSimpleProperties(this.pojoClass);
            // On enlève les propriétés inutiles pour l'utilisateur
            for (final String key : COLUMNS_TO_IGNORE) {
                properties.remove(key);
            }
            //contruction des colonnes editable
            for (final PropertyDescriptor desc : properties.values()) {
                getPropertyColumn(desc).ifPresent(column -> uiTable.getColumns().add(column));
            }
        } catch (IntrospectionException ex) {
            SIRS.LOGGER.log(Level.WARNING, "property columns cannot be created.", ex);
        }

        //
        // NAVIGATION FICHE PAR FICHE
        //
        navigationToolbar.getStyleClass().add("buttonbarleft");

        uiCurrent.setFont(Font.font(16));
        uiCurrent.getStyleClass().add(BUTTON_STYLE);
        uiCurrent.setAlignment(Pos.CENTER);
        uiCurrent.setTextFill(Color.WHITE);

        uiPrevious.getStyleClass().add(BUTTON_STYLE);
        uiPrevious.setTooltip(new Tooltip("Fiche précédente."));
        uiPrevious.setOnAction((ActionEvent event) -> {
            uiTable.getSelectionModel().selectPrevious();
        });

        uiNext.getStyleClass().add(BUTTON_STYLE);
        uiNext.setTooltip(new Tooltip("Fiche suivante."));
        uiNext.setOnAction((ActionEvent event) -> {
            uiTable.getSelectionModel().selectNext();
        });
        navigationToolbar.visibleProperty().bind(uiFicheMode.selectedProperty());

        uiFicheMode.getStyleClass().add(BUTTON_STYLE);
        uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches."));

        // Update counter when we change selected element.
        final ChangeListener<Number> selectedIndexListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            uiCurrent.setText(""+(newValue.intValue()+1) + " / " + uiTable.getItems().size());
            updateFiche();
        };
        uiFicheMode.setSelected(true);
        uiFicheMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    // If there's no selection, initialize on first element.
                    if (uiTable.getSelectionModel().getSelectedIndex() < 0) {
                        uiTable.getSelectionModel().select(0);
                    }
                    uiTable.getSelectionModel().selectedIndexProperty().addListener(selectedIndexListener);
                    updateFiche();

                } else {
                    // Update display
                    uiTable.getSelectionModel().selectedIndexProperty().removeListener(selectedIndexListener);
                    setCenter(uiTable);

                    uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches."));
                }
            }
        });

        topPane.setLeft(navigationToolbar);

        uiExport.getStyleClass().add(BUTTON_STYLE);
        uiExport.setTooltip(new Tooltip("Sauvegarder en CSV"));
        uiExport.disableProperty().bind(Bindings.isNull(uiTable.getSelectionModel().selectedItemProperty()));
        uiExport.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                final DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle(GeotkFX.getString(org.geotoolkit.gui.javafx.contexttree.menu.ExportItem.class, "folder"));
                final File folder = chooser.showDialog(null);

                if(folder!=null){
                    try{
                        //il y a plusieurs types différent dans cette liste
                        final ObservableList lst = FXCollections.observableArrayList(uiTable.getSelectionModel().getSelectedItems());
                        final Map<Class,BeanFeatureSupplier> suppliers = new HashMap<>();
                        for(Object o : lst){
                            final Class clazz;
                            if(o instanceof ElementHit){
                                clazz = ((ElementHit)o).getElementClass();
                            }else if(o instanceof Preview){
                                clazz = Class.forName(((Preview)o).getDocClass());
                            }else{
                                clazz = null;
                            }

                            if(clazz!=null && !suppliers.containsKey(clazz)){
                                final Predicate filter = new Predicate() {
                                    @Override
                                    public boolean test(Object o) {
                                        try{
                                            Class candidate = null;
                                            if(o instanceof ElementHit){
                                                candidate = ((ElementHit)o).getElementClass();
                                            }else if(o instanceof Preview){
                                                candidate = Class.forName(((Preview)o).getDocClass());
                                            }
                                            return clazz.equals(candidate);
                                        }catch(Exception ex){
                                            //will not happen
                                            return false;
                                        }
                                    }
                                };
                                final Collection col = new AbstractCollection() {
                                    @Override
                                    public Iterator iterator() {
                                        return lst.stream().filter(filter).map(ObjectTable::fullElement).iterator();
                                    }

                                    @Override
                                    public int size() {
                                        return lst.filtered(filter).size();
                                    }
                                };
                                final BeanFeatureSupplier sup = new StructBeanSupplier(clazz, () -> col);
                                suppliers.put(clazz, sup);
                            }
                        }

                        final BeanStore store = new BeanStore(suppliers.values().toArray(new BeanFeatureSupplier[0]));
                        for(GenericName n : store.getNames()){
                            final FeatureMapLayer layer = MapBuilder.createFeatureLayer(store.createSession(false)
                                    .getFeatureCollection(QueryBuilder.all(n)));
                            layer.setName(n.tip().toString());

                            FileFeatureStoreFactory factory = (FileFeatureStoreFactory) DataStores.getFactoryById("csv");
                            TaskManager.INSTANCE.submit(new ExportTask(layer, folder, factory, null));
                        }
                    } catch (Exception ex) {
                        Dialog d = new Alert(Alert.AlertType.ERROR, "Impossible de créer le fichier CSV", ButtonType.OK);
                        d.setResizable(true);
                        d.showAndWait();
                        throw new UnsupportedOperationException("Failed to create csv store : " + ex.getMessage(), ex);
                    }
                }
            }
        });

        uiFicheMode.setSelected(false);
    }

    private static Object fullElement(Object cdt){
        Optional<? extends Element> ele = Injector.getSession().getElement(cdt);
        if(ele.isPresent()){
            return ele.get();
        }else{
            return null;
        }
    }

    private void updateFiche(){
        final Object cdt = uiTable.getSelectionModel().getSelectedItem();
        final Optional<? extends Element> ele = Injector.getSession().getElement(cdt);
        if(!ele.isPresent()){
            setCenter(new Label("Pas d'éditeur disponible pour cet objet"));
        }else{
            elementPane = SIRS.generateEditionPane(ele.get(), SIRS.EDITION_PREDICATE);
            elementPane.elementProperty().set(ele.get());

            uiFicheMode.setTooltip(new Tooltip("Passer en mode de tableau synoptique."));

            uiCurrent.setText("" + (uiTable.getSelectionModel().getSelectedIndex()+1) + " / " + uiTable.getItems().size());
            setCenter((Node) elementPane);
        }
    }

    public void setTableItems(ObservableList elements){
        uiTable.setItems(elements);
    }

    /**
     * Try to find and display a form to edit input object.
     * @param pojo The object we want to edit.
     */
    protected void editPojo(Object pojo){
        editElement(pojo, SIRS.CONSULTATION_PREDICATE);
    }

    protected Optional<TableColumn> getPropertyColumn(final PropertyDescriptor desc) {
        if (desc != null) {
            final TableColumn col = new PropertyColumn(desc);
            col.sortableProperty().setValue(Boolean.TRUE);
            return Optional.of(col);
        }
        return Optional.empty();
    }

    public class PropertyColumn extends TableColumn<Object, Object>{

        public PropertyColumn(final PropertyDescriptor desc) {
            super(labelMapper.mapPropertyName(desc.getDisplayName()));

            final Reference ref = desc.getReadMethod().getAnnotation(Reference.class);

            //choix de l'editeur en fonction du type de données
            if (ref != null) {
                //reference vers un autre objet
                setEditable(false);
                setCellFactory(SIRS.getOrCreateTableCellFactory(ref));
                try {
                    final Method propertyAccessor = pojoClass.getMethod(desc.getName()+"Property");
                    setCellValueFactory((TableColumn.CellDataFeatures<Object, Object> param) -> {
                        try {
                            return (ObservableValue) propertyAccessor.invoke(param.getValue());
                        } catch (Exception ex) {
                            SirsCore.LOGGER.log(Level.WARNING, null, ex);
                            return null;
                        }
                    });
                } catch (Exception ex) {
                    setCellValueFactory(SIRS.getOrCreateCellValueFactory(desc.getName()));
                }

            } else {
                setCellValueFactory(SIRS.getOrCreateCellValueFactory(desc.getName()));
                setEditable(false);
            }
        }
    }


    public static class EditColumn extends TableColumn {

        public EditColumn(Consumer editFct) {
            super("Edition");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(SIRS.ICON_EDIT_BLACK));

            setCellValueFactory(new Callback<TableColumn.CellDataFeatures, ObservableValue>() {

                @Override
                public ObservableValue call(TableColumn.CellDataFeatures param) {
                    return new SimpleObjectProperty<>(param.getValue());
                }
            });

            setCellFactory(new Callback<TableColumn, TableCell>() {

                public TableCell call(TableColumn param) {
                    return new ButtonTableCell(
                            false, new ImageView(SIRS.ICON_EDIT_BLACK),
                            (Object t) -> true, (Object t) -> {
                                editFct.accept(t);
                                return t;
                            });
                }
            });
        }
    }
}
