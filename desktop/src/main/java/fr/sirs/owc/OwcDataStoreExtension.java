/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package fr.sirs.owc;

import fr.sirs.core.authentication.PasswordDeserializer;
import fr.sirs.core.authentication.PasswordSerializer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ObjectConverters;
import org.geotoolkit.storage.coverage.CoverageReference;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.query.Selector;
import org.geotoolkit.data.query.Source;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.owc.gtkext.ParameterType;
import org.geotoolkit.owc.xml.OwcExtension;
import org.geotoolkit.owc.xml.v10.OfferingType;
import org.geotoolkit.security.ClientSecurity;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.utility.parameter.ParametersExt;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;
import org.geotoolkit.security.ApiSecurity;
import org.geotoolkit.security.BasicAuthenticationSecurity;

/**
 * Same object as geotk one but with a different way to store the clientSecurity parameters.
 *
 * @author Johann Sorel (Geomatys)
 * @author Maxime Gavens (Geomatys)
 */
public class OwcDataStoreExtension extends OwcExtension {

    public static final String CODE = "http://www.geotoolkit.org/owc2";
    //this parameter is part of factory parameter descriptors
    private static final String KEY_STOREFACTORY = "identifier";
    private static final String KEY_DATANAME = "dataName";
    public static final String KEY_USER = "user_security";
    public static final String KEY_PASSWORD = "password_security";
    public static final String KEY_API_KEY = "apiKey_security";

    public OwcDataStoreExtension() {
        super(CODE, 0);
    }

    @Override
    public boolean canHandle(MapLayer layer) {
        return getParams(layer) != null && getStoreFactoryName(layer) != null;
    }

    @Override
    public MapLayer createLayer(OfferingType offering) throws DataStoreException {
        final List<Object> fields = offering.getOperationOrContentOrStyleSet();

        //rebuild parameters map
        String factoryName = null;
        String typeName = null;
        String user = null;
        String password = null;
        String apiKey = null;
        final Map params = new HashMap();
        for(Object o : fields){
            if(o instanceof JAXBElement){
                o = ((JAXBElement)o).getValue();
            }

            if(o instanceof ParameterType){
                final ParameterType param = (ParameterType) o;
                final String key = param.getKey();
                final Class valClass;

                try {
                    valClass = Class.forName(param.getType());
                } catch (ClassNotFoundException ex) {
                    throw new DataStoreException(ex.getMessage(),ex);
                }
                Object value = param.getValue();
                value = ObjectConverters.convert(value, valClass);

                if(KEY_STOREFACTORY.equalsIgnoreCase(key)){
                    factoryName = (String)value;
                    params.put(key, factoryName);
                } else if(KEY_DATANAME.equalsIgnoreCase(key)){
                    typeName = (String)value;
                } else if(KEY_USER.equalsIgnoreCase(key)) {
                    user = (String)value;
                } else if(KEY_PASSWORD.equalsIgnoreCase(key)) {
                    password = (String)value;
                } else if(KEY_API_KEY.equalsIgnoreCase(key)) {
                    apiKey = (String)value;
                } else{
                    params.put(key, value);
                }
            }
        }

        if (user != null && password != null) {
            final String decryptUser = PasswordDeserializer.decode(user);
            final String decryptPassword = PasswordDeserializer.decode(password);
            params.put("security", new BasicAuthenticationSecurity(decryptUser, decryptPassword));
        } else if(apiKey != null) {
            final String decryptApiKey = PasswordDeserializer.decode(apiKey);
            params.put("security", new ApiSecurity(decryptApiKey));
        }

        final DataStoreFactory ff = DataStores.getFactoryById(factoryName);
        if(ff!=null){
            final DataStore store = ff.open(params);
            if(store instanceof FeatureStore){
                final Session session = ((FeatureStore)store).createSession(true);
                final FeatureCollection col = session.getFeatureCollection(QueryBuilder.all(NamesExt.valueOf(typeName)));
                final FeatureMapLayer layer = MapBuilder.createFeatureLayer(col);
                return layer;
            }else if(store instanceof CoverageStore){
                final CoverageReference covref = ((CoverageStore)store).getCoverageReference(NamesExt.valueOf(typeName));
                final CoverageMapLayer layer = MapBuilder.createCoverageLayer(covref);
                return layer;
            }
        }

        //unknown factory, may no be in the classpath
        return MapBuilder.createEmptyMapLayer();
    }

    @Override
    public OfferingType createOffering(MapLayer mapLayer) {
        final OfferingType offering = new OfferingType();
        offering.setCode(getCode());

        //write the type name
        final List<Object> fieldList = offering.getOperationOrContentOrStyleSet();
        final GenericName typeName = getTypeName(mapLayer);
        if(typeName!=null){
            fieldList.add(new ParameterType(KEY_DATANAME,String.class.getName(),NamesExt.toExpandedString(typeName)));
        }

        //write store creation parameters
        final ParameterValueGroup params = getParams(mapLayer);

        final ParameterDescriptorGroup desc = params.getDescriptor();
        for(GeneralParameterDescriptor pdesc : desc.descriptors()){
            final GeneralParameterValue param = ParametersExt.getParameter(params, pdesc.getName().getCode());
            if(param instanceof ParameterValue){
                final ParameterDescriptor pvdesc = (ParameterDescriptor) pdesc;
                final Object value = ((ParameterValue)param).getValue();
                if(value!=null){
                    if(value instanceof ClientSecurity) {
                        if(value instanceof BasicAuthenticationSecurity) {
                            final String user = ((BasicAuthenticationSecurity) value).getUser();
                            final String password = ((BasicAuthenticationSecurity) value).getPassword();
                            final String encryptUser = PasswordSerializer.encode(user);
                            final String encryptPassword = PasswordSerializer.encode(password);

                            fieldList.add(new ParameterType(
                                    KEY_USER,
                                    user.getClass().getName(),
                                    encryptUser
                            ));
                            fieldList.add(new ParameterType(
                                    KEY_PASSWORD,
                                    password.getClass().getName(),
                                    encryptPassword
                            ));
                        } else if(value instanceof ApiSecurity) {
                            final String apiKey = ((ApiSecurity) value).getApikey();
                            final String apiKeyPassword = PasswordSerializer.encode(apiKey);
                            fieldList.add(new ParameterType(
                                KEY_API_KEY,
                                apiKey.getClass().getName(),
                                apiKeyPassword
                            ));
                        }
                    } else {
                        fieldList.add(new ParameterType(
                            pdesc.getName().getCode(),
                            pvdesc.getValueClass().getName(),
                            String.valueOf(value)));
                    }
                }
            }
        }

        return offering;
    }

    private static String getStoreFactoryName(MapLayer layer){
        if(layer instanceof FeatureMapLayer){
            final FeatureMapLayer fml = (FeatureMapLayer) layer;
            final Source source = fml.getCollection().getSource();
            if(source instanceof Selector){
                final Selector selector = (Selector)source;
                final Session session = selector.getSession();
                if(session!=null){
                    final FeatureStore store = session.getFeatureStore();
                    if(store!=null){
                        final DataStoreFactory factory = store.getFactory();
                        final InternationalString id = factory.getIdentification().getCitation().getTitle();
                        return id.toString();
                    }
                }
            }
        }else if(layer instanceof CoverageMapLayer){
            final CoverageMapLayer cml = (CoverageMapLayer) layer;
            final CoverageReference covref = cml.getCoverageReference();
            final CoverageStore store = covref.getStore();
            if(store!=null){
                final DataStoreFactory factory = store.getFactory();
                final InternationalString id = factory.getIdentification().getCitation().getTitle();
                return id.toString();
            }
        }
        return null;
    }

    private static ParameterValueGroup getParams(MapLayer layer){
        if(layer instanceof FeatureMapLayer){
            final FeatureMapLayer fml = (FeatureMapLayer) layer;
            final Source source = fml.getCollection().getSource();
            if(source instanceof Selector){
                final Selector selector = (Selector)source;
                final Session session = selector.getSession();
                if(session!=null){
                    final FeatureStore store = session.getFeatureStore();
                    if(store!=null){
                        return store.getConfiguration();
                    }
                }
            }
        }else if(layer instanceof CoverageMapLayer){
            final CoverageMapLayer cml = (CoverageMapLayer) layer;
            final CoverageReference covref = cml.getCoverageReference();
            final CoverageStore store = covref.getStore();
            if(store!=null){
                return store.getConfiguration();
            }
        }
        return null;
    }

    private static GenericName getTypeName(MapLayer layer){
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
