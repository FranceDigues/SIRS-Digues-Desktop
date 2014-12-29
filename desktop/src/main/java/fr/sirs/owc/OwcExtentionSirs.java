package fr.sirs.owc;

import fr.sirs.CorePlugin;
import org.geotoolkit.owc.xml.OwcExtension;
import fr.sirs.Plugin;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.spi.ServiceRegistry;
import javax.xml.bind.JAXBElement;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.coverage.CoverageReference;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.bean.BeanStore;
import org.geotoolkit.data.query.Selector;
import org.geotoolkit.data.query.Source;
import org.geotoolkit.feature.type.Name;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.owc.gtkext.ParameterType;
import static org.geotoolkit.owc.xml.OwcMarshallerPool.*;
import org.geotoolkit.owc.xml.v10.OfferingType;

/**
 * Extension OWC pour SIRS.
 * 
 * @author Samuel Andrés (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class OwcExtentionSirs extends OwcExtension {
    
    public static final String CODE = "http://www.france-digues.fr/owc";
    public static final String KEY_BEANCLASS = "beanClass";

    private static final List<MapItem> mapItems = new ArrayList();
    
    static {
        final Iterator<Plugin> ite = ServiceRegistry.lookupProviders(Plugin.class);
        while(ite.hasNext()){
            mapItems.addAll(ite.next().getMapItems());
        }
        System.out.println(mapItems);
    }

    public OwcExtentionSirs() {
        super(CODE,10);
    }
    
    @Override
    public boolean canHandle(MapLayer layer) {
        if(layer instanceof FeatureMapLayer){
            final FeatureMapLayer fml = (FeatureMapLayer) layer;
            final FeatureCollection collection = fml.getCollection();
            final FeatureStore store = collection.getSession().getFeatureStore();
            return store instanceof BeanStore && getTypeName(layer) != null;
        }
        return false;
    }
    
    @Override
    public MapLayer createLayer(OfferingType offering) throws DataStoreException {
        final List<Object> fields = offering.getOperationOrContentOrStyleSet();
        
        //rebuild parameters map
        String beanClassName = null;
        for(Object o : fields){
            if(o instanceof JAXBElement){
                o = ((JAXBElement)o).getValue();
            }
            if(o instanceof ParameterType){
                final ParameterType param = (ParameterType) o;
                if(KEY_BEANCLASS.equals(param.getKey())){
                    beanClassName = param.getValue();
                }
            }
        }
        
        final Class clazz;
        try {
            clazz = Class.forName(beanClassName);
        } catch (ClassNotFoundException ex) {
            throw new DataStoreException(ex.getMessage(),ex);
        }
        return CorePlugin.createLayer(clazz);
    }
    
    @Override
    public OfferingType createOffering(MapLayer mapLayer) {
        final OfferingType offering = OWC_FACTORY.createOfferingType();
        offering.setCode(CODE);
        
        final FeatureMapLayer fml = (FeatureMapLayer) mapLayer;
        final FeatureCollection collection = fml.getCollection();
        final BeanStore store = (BeanStore) collection.getSession().getFeatureStore();
        final Name typeName = getTypeName(fml);
        final BeanFeatureSupplier supplier;
        try {
            supplier = store.getBeanSupplier(typeName);
        } catch (DataStoreException ex) {
            throw new IllegalStateException(ex.getMessage(),ex);
        }
        final Class beanClass = supplier.getBeanClass();
        //write the blean class name
        final List<Object> fieldList = offering.getOperationOrContentOrStyleSet();
        fieldList.add(new ParameterType(KEY_BEANCLASS,String.class.getName(),beanClass.getName()));
        return offering;
    }
        
    private static Name getTypeName(MapLayer layer){
        if(layer instanceof FeatureMapLayer){
            final FeatureMapLayer fml = (FeatureMapLayer) layer;
            final Source source = fml.getCollection().getSource();
            if(source instanceof Selector){
                final Selector selector = (Selector)source;
                return selector.getFeatureTypeName();
            }
        }else if(layer instanceof CoverageMapLayer){
            final CoverageMapLayer cml = (CoverageMapLayer) layer;
            final CoverageReference covref = cml.getCoverageReference();
            return covref.getName();
        }
        return null;
    }
    
}
