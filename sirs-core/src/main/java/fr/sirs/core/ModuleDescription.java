package fr.sirs.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapItem;

/**
 * A brief description of map layers which can be provided by the current database.
 * @author Alexis Manin (Geomatys)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ModuleDescription {
    
    public final SimpleStringProperty name = new SimpleStringProperty();
    
    public final SimpleStringProperty version = new SimpleStringProperty();
    
    public final SimpleStringProperty title = new SimpleStringProperty();
    
    public final ObservableList<Layer> layers = FXCollections.observableArrayList();
                        
        public String getName() {
            return name.get();
        }
        
        public void setName(final String newName) {
            name.set(newName);
        }
                            
        public String getVersion() {
            return version.get();
        }
        
        public void setVersion(final String newVersion) {
            version.set(newVersion);
        }
        
        public String getTitle() {
            return title.get();
        }
        
        public void setTitle(final String newTitle) {
            title.set(newTitle);
        }
        
        public ObservableList<Layer> getLayers() {
            return layers;
        }
        
        public void setLayers(final List<Layer> layers) {
            this.layers.setAll(layers);
        }
        
    /**
     * A simple container to describe a map item/layer.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Layer {
        
        public final SimpleStringProperty title = new SimpleStringProperty();
        
        public final SimpleStringProperty fieldToFilterOn = new SimpleStringProperty("@class");
        
        public final SimpleStringProperty filterValue = new SimpleStringProperty();
        
        public final ObservableList<Layer> children = FXCollections.observableArrayList();
                
        public String getTitle() {
            return title.get();
        }

        public String getFieldToFilterOn() {
            return fieldToFilterOn.get();
        }

        public String getFilterValue() {
            return filterValue.get();
        }

        public ObservableList<Layer> getChildren() {
            return children;
        }
        
        public void setChildren(final List<Layer> layers) {
            this.children.setAll(layers);
        }
        
        public void setTitle(final String newTitle) {
            title.set(newTitle);
        }

        public void setFieldToFilterOn(final String filterField) {
            fieldToFilterOn.set(filterField);
        }

        public void setFilterValue(final String value) {
            filterValue.set(value);
        }
    }
    
    public static Optional<Layer> getLayerDescription(final MapItem item) {
        final Layer currentLayer = new Layer();
        if (item.items() != null && !item.items().isEmpty()) {
            for (final MapItem child : item.items()) {
                getLayerDescription(child).ifPresent(computed -> currentLayer.children.add(computed));
            }
            if (!currentLayer.children.isEmpty()) {
                currentLayer.fieldToFilterOn.set(null);
            }
        } else if (item instanceof FeatureMapLayer) {
            final FeatureMapLayer fLayer = (FeatureMapLayer) item;
            final FeatureCollection c = fLayer.getCollection();
            FeatureIterator iterator = c.iterator();
            if (iterator.hasNext()) {
                Feature next = iterator.next();
                if (next instanceof BeanFeature) {
                    Object bean = next.getUserData().get(BeanFeature.KEY_BEAN);
                    currentLayer.filterValue.set(bean.getClass().getCanonicalName());
                }
            }
        } 
        
        if (currentLayer.children.isEmpty() && currentLayer.filterValue.get() == null) {
            return Optional.empty();
        } else {
            currentLayer.title.set(item.getName());
            return Optional.of(currentLayer);
        }
    }
}