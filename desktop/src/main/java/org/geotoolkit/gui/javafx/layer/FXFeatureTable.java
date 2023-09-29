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
package org.geotoolkit.gui.javafx.layer;

import com.vividsolutions.jts.geom.Geometry;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.geotoolkit.internal.Loggers;
import org.geotoolkit.map.FeatureMapLayer;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;

/**
 *
 * @author Johann Sorel (Geomatys)
 * 
 * TODO : REPLACE BY THE SAME CLASS DEFINED IN GEOTOOLKIT
 */
public class FXFeatureTable extends FXPropertyPane{
    
    protected final TableView<Feature> table = new TableView<>();
    private boolean loadAll = false;
    
    protected FeatureMapLayer layer;
    
    // Bundle management
    /**
     * bundles contains ResourceBundles indexed by table names.
     */
    private final Map<String, ResourceBundle> bundles = new HashMap<>();
    private String bundlePrefix;
    
    private final Locale locale;
    private final ClassLoader classLoader;
    
    /**
     * Default constructor, without mapping policy.
     */
    public FXFeatureTable() {
        setCenter(initTable());
        locale=Locale.getDefault();
        classLoader=null;
    }
    
    /**
     * A constructor that allows to map the column table names using bundles.
     * 
     * For instance, if the following map is given :
     * "TableA" => "fr.geotk.Bundle1"
     * "TableB" => "fr.geotk.nmspace.Bundle2"
     * The bundle "Bundle1" will be used to map TableA column names and the bundle
     * "Bundle2" will be used to map TableB column names.
     * 
     * @param bundleMapping A mapping between table names (keys) and the 
     * corresponding bundle base name (value). Must not be null.
     * @param locale A locale. If null, the locale will be the default locale.
     * @param classLoader The class loader necessary to find the ressource bundles.
     */
    public FXFeatureTable(final Map<String, String> bundleMapping, final Locale locale, final ClassLoader classLoader){
        setCenter(initTable());
        this.locale = (locale==null) ? Locale.getDefault() : locale;
        this.classLoader = classLoader;
        for(final String keyTableName : bundleMapping.keySet()){
            final String bundleBaseName = bundleMapping.get(keyTableName);
            try{
                final ResourceBundle bundle;
                if(classLoader!=null && locale!=null)
                    bundle = ResourceBundle.getBundle(bundleBaseName, locale, classLoader);
                else
                    bundle = ResourceBundle.getBundle(bundleBaseName);
                bundles.put(keyTableName, bundle);
            }
            catch(Exception ex){
                Loggers.JAVAFX.log(Level.INFO, ex.getMessage(),ex);
            }
        }
    }
    
    /**
     * A constructor that allows to map the column table names using bundles.
     * 
     * For instance, if the following map is given :
     * "TableA" => "fr.geotk.Bundle1"
     * "TableB" => "fr.geotk.nmspace.Bundle2"
     * The bundle "Bundle1" will be used to map TableA column names and the bundle
     * "Bundle2" will be used to map TableB column names.
     * 
     * @param bundleMapping A mapping between table names (keys) and the correspondint bundle base name (value). Must not be null.
     */
    public FXFeatureTable(final Map<String, String> bundleMapping){
        this(bundleMapping, Locale.getDefault(), null);
    }
    
    /**
     * A constructor that allows to map the column table names using bundles.
     * 
     * The bundleBaseNamePrefix is used to build a map bundle names from table names.
     * 
     * For instance, if the bundleBaseNamePrefix is "fr.geotk.nmspace.", the
     * following map is built :
     * "TableA" => "fr.geotk.nmspace.TableA"
     * "TableB" => "fr.geotk.nmspace.TableB"
     * 
     * Thus, the bundle "TableA" will be used to map TableA column names and the 
     * bundle "TableB" will be used to map TableB column names.
     * 
     * @param bundleBaseNamePrefix A base name prefix used to build a bundle mapping with table names.
     * @param locale A locale. If null, the locale will be the default locale.
     * @param classLoader The class loader necessary to find the ressource bundles.
     */
    public FXFeatureTable(final String bundleBaseNamePrefix, final Locale locale, final ClassLoader classLoader){
        setCenter(initTable());
        bundlePrefix = bundleBaseNamePrefix;
        this.locale= (locale==null) ? Locale.getDefault() : locale;
        this.classLoader=classLoader;
    }
    
    /**
     * A constructor that allows to map the column table names using bundles.
     * 
     * The bundleBaseNamePrefix is used to build a map bundle names from table names.
     * 
     * For instance, if the bundleBaseNamePrefix is "fr.geotk.nmspace.", the
     * following map is built :
     * "TableA" => "fr.geotk.nmspace.TableA"
     * "TableB" => "fr.geotk.nmspace.TableB"
     * 
     * Thus, the bundle "TableA" will be used to map TableA column names and the 
     * bundle "TableB" will be used to map TableB column names.
     * 
     * @param bundleBaseNamePrefix A base name prefix used to build a bundle mapping with table names.
     */
    public FXFeatureTable(final String bundleBaseNamePrefix){
        this(bundleBaseNamePrefix, Locale.getDefault(), null);
    }
    
    private Node initTable(){
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        //listen to table selection
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Feature>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends Feature> c) {
                final Iterator<Feature> ite = table.getSelectionModel().getSelectedItems().iterator();
                final Set<Identifier> set = new HashSet<>();
                while(ite.hasNext()){
                    set.add(ite.next().getIdentifier());
                }
                final Id selection = GO2Utilities.FILTER_FACTORY.id(set);
                if(layer!=null){
                    layer.setSelectionFilter(selection);
                }
                
            }
        });
        
        table.setPlaceholder(new Label("Aucune donnée"));
        return table;
    }
    
    @Override
    public String getTitle() {
        return "Feature table";
    }
    
    public String getCategory(){
        return "";
    }

    public TableView<Feature> getTable() { return table; }

    public boolean isLoadAll() {
        return loadAll;
    }

    public void setLoadAll(boolean loadAll) {
        this.loadAll = loadAll;
    }
    
    public void setEditable(boolean editable){
        table.setEditable(editable);
    }

    public boolean isEditable(){
        return table.isEditable();
    }
    
    public boolean init(Object candidate){
        if(!(candidate instanceof FeatureMapLayer)) return false;
        
        layer = (FeatureMapLayer) candidate;
        final FeatureCollection features = layer.getCollection();
        
        table.getColumns().clear();
        final FeatureType featureType = features.getFeatureType();
        for(PropertyDescriptor prop : featureType.getDescriptors()){
            final TableColumn<Feature,String> column = new TableColumn<Feature,String>(generateFinalColumnName(prop));
            column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Feature, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Feature, String> param) {
                    final Object val = param.getValue().getPropertyValue(prop.getName().toString());
                    if(val instanceof Geometry){
                        return new SimpleObjectProperty<>("{geometry}");
                    }else{
                        return new SimpleObjectProperty<>(String.valueOf(val));
                    }
                }
            });
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            table.getColumns().add(column);
        }
        
        if(loadAll){
            table.setItems(FXCollections.observableArrayList((Feature[])features.toArray(new Feature[0])));
        }
        
        //TODO make a caching version, this is too slow for today use.
        //final ObservableFeatureCollection obsCol = new ObservableFeatureCollection((FeatureCollection<Feature>) col);
        //table.setItems(obsCol);
        
        return true;
    }
    
    private String generateFinalColumnName(final PropertyDescriptor prop) {
        Map<String, Entry<String, String>> labelInfo;
        try {
            labelInfo = (Map) prop.getUserData().get("labelInfo");
        } catch (Exception ex) {
            Loggers.JAVAFX.log(Level.INFO, ex.getMessage(), ex);
            labelInfo = null;
        }

        final String labelName = prop.getName().toString();
        String columnName = labelName;
        String tableName = null;

        // If exists, explore labelInfo to retrive table and column respect to this label.
        if (labelInfo != null) {
            final Entry<String, String> entry = labelInfo.get(labelName);
            if (entry != null) {
                if (entry.getKey() != null) {
                    tableName = entry.getKey();
                } else {
                    tableName = null;
                }
                if (entry.getValue() != null) {
                    columnName = entry.getValue();
                } else {
                    columnName = labelName;
                }
            }
        }

        //If table name is not null, try to found resourcebundle for this table.
        if (tableName != null) {

            // If there isn't resource bundles (or not for the curruen table), try to generate.
            if (bundles.get(tableName) == null) {
                if (bundlePrefix != null) {
                    try {
                        if(classLoader!=null && locale!=null)
                            bundles.put(tableName, ResourceBundle.getBundle(bundlePrefix + tableName, Locale.getDefault(), classLoader));
                        else
                            bundles.put(tableName, ResourceBundle.getBundle(bundlePrefix + tableName));
                    } catch (MissingResourceException e){
                        /*If resourcebundle is not found, do nothing : let the bundle null for this table name.*/
                    }
                }
            }
        }

        final ResourceBundle bundle = bundles.get(tableName);

        String finalColumnName;
        if (labelName == null) {
            finalColumnName = "";
        } else if (bundle == null) {
            if (!labelName.equals(columnName)) {
                finalColumnName = columnName + " as " + labelName;
            } else {
                finalColumnName = columnName;
            }
        } else {
            try {
                if (!labelName.equals(columnName)) {
                    finalColumnName = bundle.getString(columnName) + " as " + labelName;
                } else {
                    finalColumnName = bundle.getString(columnName);
                }
            } catch (MissingResourceException ex) {
                if (!labelName.equals(columnName)) {
                    finalColumnName = columnName + " as " + labelName;
                } else {
                    finalColumnName = columnName;
                }
            }
        }
        return finalColumnName;
    }
    
    private void loadData(ActionEvent event){
        final FeatureCollection col = layer.getCollection();
        table.setItems(FXCollections.observableArrayList((Feature[])col.toArray(new Feature[0])));
    }
        
    private static class ObservableFeatureCollection extends AbstractSequentialList<Feature> implements ObservableList<Feature>{

        private final FeatureCollection features;
        
        private ObservableFeatureCollection(FeatureCollection features){
            this.features = features;
        }
        
        @Override
        public ListIterator<Feature> listIterator(int index) {
            
            final QueryBuilder qb = new QueryBuilder(features.getFeatureType().getName());
            qb.setStartIndex(index);
            
            final FeatureCollection subcol;            
            try {
                subcol = features.subCollection(qb.buildQuery());
            } catch (DataStoreException ex) {
                Loggers.JAVAFX.log(Level.WARNING, ex.getMessage(),ex);
                return Collections.EMPTY_LIST.listIterator();
            }
            final Iterator ite = subcol.iterator();
            
            final ListIterator lite = new ListIterator() {

                @Override
                public boolean hasNext() {
                    return ite.hasNext();
                }

                @Override
                public Object next() {
                    return ite.next();
                }

                @Override
                public boolean hasPrevious() {
                    return index>0;
                }

                @Override
                public Object previous() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public int nextIndex() {
                    return index+1;
                }

                @Override
                public int previousIndex() {
                    return index-1;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void set(Object e) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void add(Object e) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
            
            return lite;
        }

        @Override
        public int size() {
            return features.size();
        }

        @Override
        public void addListener(ListChangeListener<? super Feature> listener) {
        }

        @Override
        public void removeListener(ListChangeListener<? super Feature> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }
        
        @Override
        public boolean addAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setAll(Collection<? extends Feature> col) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean retainAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void remove(int from, int to) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
    
    
}
