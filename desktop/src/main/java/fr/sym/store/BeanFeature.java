
package fr.sym.store;

import com.vividsolutions.jts.geom.Geometry;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.feature.AbstractFeature;
import org.geotoolkit.feature.AbstractProperty;
import org.geotoolkit.feature.Attribute;
import org.geotoolkit.feature.DefaultGeometryAttribute;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.GeometryAttribute;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.type.AttributeDescriptor;
import org.geotoolkit.feature.type.AttributeType;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.geotoolkit.feature.type.GeometryType;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.geometry.DefaultBoundingBox;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class BeanFeature extends AbstractFeature<Collection<Property>>{

    private final Object bean;
    private final Mapping mapping;
    
    public BeanFeature(Object bean, Mapping mapping){
        super(mapping.featureType, mapping.buildId(bean));
        this.bean = bean;
        this.mapping = mapping;
        
        value = new ArrayList<>();
        for(PropertyDescriptor desc : mapping.featureType.getDescriptors()){
            final Property property;
            if(desc instanceof GeometryDescriptor){
                property = new BeanGeometryProperty((GeometryDescriptor) desc);
            }else{
                property = new BeanAttributeProperty((AttributeDescriptor) desc);
            }
            value.add(property);
        }
    }
        
    public static class Mapping {
        public final FeatureType featureType;
        public final String idField;
        public final Map<String,java.beans.PropertyDescriptor> accessors = new HashMap<>();
        public java.beans.PropertyDescriptor idAccessor;

        public Mapping(Class clazz, String namespace, CoordinateReferenceSystem crs, String idField) {
            this.idField = idField;
            final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
            ftb.setName(namespace,clazz.getSimpleName());
            try {
                for (java.beans.PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
                    final String propName = pd.getName();
                    if(propName.equals(idField)){
                        //ignore the id field, it will be used as featureId
                        idAccessor = pd;
                        continue;
                    }
                    
                    final Class propClazz = pd.getReadMethod().getReturnType();
                    if(Geometry.class.isAssignableFrom(propClazz)){
                        ftb.add(propName, propClazz, crs);
                        ftb.setDefaultGeometry(propName);
                    }else{
                        ftb.add(propName, propClazz);
                    }
                    accessors.put(propName, pd);
                }
            } catch (IntrospectionException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            featureType = ftb.buildFeatureType();
        }
        
        public FeatureId buildId(Object bean){
            try {
                return new DefaultFeatureId(String.valueOf(idAccessor.getReadMethod().invoke(bean)));
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
        }
        
    }
    
    private class BeanAttributeProperty extends AbstractProperty implements Attribute{

        private final AttributeDescriptor desc;
        
        public BeanAttributeProperty(AttributeDescriptor desc){
            this.desc = desc;
        }
        
        @Override
        public Object getValue() {
            try {
                return mapping.accessors.get(getName().getLocalPart()).getReadMethod().invoke(bean);
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
        }

        @Override
        public void setValue(Object newValue) {
            try {
                mapping.accessors.get(getName().getLocalPart()).getWriteMethod().invoke(bean,newValue);
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
        }

        @Override
        public AttributeType getType() {
            return getDescriptor().getType();
        }

        @Override
        public AttributeDescriptor getDescriptor() {
            return desc;
        }

        @Override
        public Map<Object, Object> getUserData() {
            return Collections.EMPTY_MAP;
        }

        @Override
        public Identifier getIdentifier() {
            return null;
        }

        @Override
        public void validate() {
        }

        @Override
        public Collection<Object> getValues() {
            return Collections.singleton(getValue());
        }

        @Override
        public void setValues(Collection<? extends Object> clctn) throws IllegalArgumentException {
            setValue(clctn.iterator().next());
        }
        
    }
    
    private final class BeanGeometryProperty extends BeanAttributeProperty implements GeometryAttribute{
        
        protected BoundingBox bounds;
    
        public BeanGeometryProperty(GeometryDescriptor desc){
            super(desc);
        }

        @Override
        public GeometryType getType() {
            return (GeometryType)super.getType();
        }

        @Override
        public GeometryDescriptor getDescriptor() {
            return (GeometryDescriptor)super.getDescriptor();
        }

        /**
        * Set the bounds for the contained geometry.
        */
       @Override
       public synchronized void setBounds(final Envelope bbox) {
           bounds = DefaultBoundingBox.castOrCopy(bbox);
       }

       /**
        * Returns the non null envelope of this attribute. If the attribute's
        * geometry is <code>null</code> the returned Envelope
        * <code>isNull()</code> is true.
        *
        * @return Bounds of the geometry
        */
       @Override
       public synchronized BoundingBox getBounds() {
           final Object val = getValue();
           if(bounds == null){
               //we explicitly use the getValue method, since subclass can override it

               //get the type crs if defined
               CoordinateReferenceSystem crs = getType().getCoordinateReferenceSystem();

               if(crs == null){
                   //the type does not define the crs, then the object value might define it
                   if(val instanceof com.vividsolutions.jts.geom.Geometry){
                       try {
                           crs = JTS.findCoordinateReferenceSystem((com.vividsolutions.jts.geom.Geometry) val);
                       } catch (NoSuchAuthorityCodeException ex) {
                           Logger.getLogger(DefaultGeometryAttribute.class.getName()).log(Level.WARNING, null, ex);
                       } catch (FactoryException ex) {
                           Logger.getLogger(DefaultGeometryAttribute.class.getName()).log(Level.WARNING, null, ex);
                       }
                   }else if(val instanceof org.opengis.geometry.Geometry){
                       crs = ((org.opengis.geometry.Geometry)val).getCoordinateReferenceSystem();
                   }
               }

               bounds = new JTSEnvelope2D(crs);
           }

           if (val instanceof com.vividsolutions.jts.geom.Geometry) {
               ((JTSEnvelope2D)bounds).init(((com.vividsolutions.jts.geom.Geometry)val).getEnvelopeInternal());
           } else {
               ((JTSEnvelope2D)bounds).setToNull();
           }

           return bounds;
       }
        
        
    }
    
}
