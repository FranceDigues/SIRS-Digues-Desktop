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

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.Preview;
import fr.sirs.ui.Growl;
import fr.sirs.util.DesordreUrgenceLayerFunction;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.ektorp.DocumentNotFoundException;
import org.geotoolkit.data.*;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
import org.geotoolkit.data.memory.WrapFeatureCollection;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.feature.Attribute;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.geotoolkit.filter.binarycomparison.DefaultPropertyIsEqualTo;
import org.geotoolkit.internal.Loggers;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.storage.FactoryMetadata;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.Geometry;
import org.opengis.util.GenericName;

import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.geotoolkit.feature.FeatureTypeUtilities.createSubType;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ExportTask extends Task<Boolean> {
    private final FeatureMapLayer layer;
    private final FileFeatureStoreFactory factory;
    private final File folder;
    private final Map<String,String> columnsToFilter;

    // The extraFunction is used to update the created file in case some columns must be added to the file but can't via this process.
    // Example : the list of a désordre's observations is not part of its FeatureType.
    // Thus, the need of an extraFunction that will allow to add those attributes to the file.
    private final BiConsumer<File, List<Object>> extraFunction;

    // The exportTask being performed outside of the main thread, the user can modify the selected element in the PojoTable after launching the export process.C
    // The consequence is the loss of the inital selected elements.
    // Thus, the copy all the elements as they were at the beginning of the process.
    private final List<Object> elements;

    public static String removeAccents(String input) {
        if (input == null) return null;
        return Normalizer.normalize(input, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
    }

    /**
     *
     * @param layer data to export
     * @param folder output location
     * @param factory handle the data store
     * @param columnsToFilter allows to filter the features. Leave to null to keep all columns
     */
    public ExportTask(FeatureMapLayer layer, File folder, FileFeatureStoreFactory factory, Map<String,String> columnsToFilter) {
        this(layer, folder, factory, columnsToFilter, null, null);
    }

    /**
     *
     * @param layer data to export
     * @param folder output location
     * @param factory handle the data store
     * @param columnsToFilter allows to filter the features. Leave to null to keep all columns
     * @param extraFunction a function to add columns to the created file.
     * @param elements the list of the elements present in the file. Used for the extraFunction.
     */
    public ExportTask(FeatureMapLayer layer, File folder, FileFeatureStoreFactory factory, Map<String,String> columnsToFilter, BiConsumer<File, List<Object>> extraFunction, final List<Object> elements) {
        ArgumentChecks.ensureNonNull("layer", layer);
        updateTitle("Export de " + layer.getName());
        this.folder = folder;
        this.factory = factory;
        this.layer = layer;
        this.columnsToFilter = columnsToFilter;
        this.extraFunction = extraFunction;
        this.elements = elements;
    }

    @Override
    protected Boolean call() throws Exception {
        try {
            final Query query = layer.getQuery();
            final FeatureCollection baseCol;
            String extraLayerName = "";
            if (query != null) {
                baseCol = new FillCoordCollection(layer.getCollection().subCollection(query));
                final Filter filter = query.getFilter();
                if (filter instanceof DefaultPropertyIsEqualTo) {
                    DefaultPropertyIsEqualTo fil = (DefaultPropertyIsEqualTo) filter;
                    final Expression expression1 = fil.getExpression1();
                    if (expression1 instanceof DesordreUrgenceLayerFunction) {
                        final String name = layer.getName();
                        if (name != null && !name.isEmpty()) {
                            // Force to remove special characters.
                            extraLayerName = "_" + removeAccents(name).replace(" ", "-");
                        }
                    }
                }
            } else {
                baseCol = new FillCoordCollection(layer.getCollection());
            }
            final FeatureType baseType = baseCol.getFeatureType();
            final FactoryMetadata metadata = factory.getMetadata();
            final Class<Geometry>[] supportedGeometryTypes = metadata.supportedGeometryTypes();
            //detect if we need one or multiple types.
            final FeatureCollection[] cols;
            if (baseType.getGeometryDescriptor()==null || ArraysExt.contains(supportedGeometryTypes, baseType.getGeometryDescriptor().getType().getBinding())) {
                cols = new FeatureCollection[]{baseCol};
            } else {
                //split the feature collection in sub geometry types
                cols = FeatureStoreUtilities.decomposeByGeometryType(baseCol, supportedGeometryTypes);
            }
            //transforme each collection
            //replace ids by libellé when possible
            for (int i = 0; i < cols.length; i++) {
                final Previews previews = Injector.getSession().getPreviews();
                cols[i] = new WrapFeatureCollection(cols[i]) {
                    @Override
                    protected Feature modify(Feature ftr) throws FeatureStoreRuntimeException {
                        if(ftr instanceof BeanFeature){
                            //do not modify the real bean
                            ftr = FeatureUtilities.copy(ftr);
                        }

                        for (Property p : ftr.getProperties()) {
                            if (p instanceof Attribute) {
                                final Class<?> binding = p.getType().getBinding();
                                if (String.class.isAssignableFrom(binding)) {
                                    final String value = String.valueOf(p.getValue());
                                    try {
                                        final Preview lbl = previews.get(value);
                                        if (lbl != null) {
                                            p.setValue(lbl.getLibelle());
                                        }
                                    } catch (DocumentNotFoundException e) {
                                        SirsCore.LOGGER.log(Level.FINEST, "No document found for id : {0}", value);
                                    }
                                }
                            }
                        }
                        return ftr;
                    }
                };
            }
            boolean allColsEmpty = true;
            for (FeatureCollection col : cols) {
                if (col.isEmpty()) {
                    continue;
                }
                allColsEmpty = false;
                FeatureType inType = col.getFeatureType();
                inType = filterFTByColumns(inType);
                final String inTypeName = inType.getName().tip().toString();
                final String filePrefix = inTypeName + extraLayerName;
                final boolean isCsvFile = factory instanceof CSVFeatureStoreFactory;
                //output fileTmp path - If CSV file then temporary file as the header will be modified at the end
                final String[] fileExtensions = factory.getFileExtensions();
                final int length = fileExtensions.length;
                if (length != 1) {
                    if (length == 0) {
                        throw new IllegalStateException("Factory " + factory.getClass() + " has no fileExtensions.");
                    } else {
                        final StringBuilder message = new StringBuilder(fileExtensions[0]);
                        for (int i = 1; i < fileExtensions.length; i++) {
                            message.append(", " + fileExtensions[i]);
                        }
                        SIRS.LOGGER.log(Level.WARNING, "Factory " + factory.getClass() + " has more than one fileExtension : " + message);
                    }
                }
                final String fileExtension = fileExtensions[0];
                File fileTmp = createFile(filePrefix, fileExtension, "_tmp");
                //create output store
                final FeatureStore store = factory.createDataStore(fileTmp.toURI());
                //delete feature types
                //create output type
                store.createFeatureType(inType.getName(), inType);
                final FeatureType outType = store.getFeatureType(inTypeName);
                final GenericName outName = outType.getName();
                //write datas
                final Session session = store.createSession(false);
                session.addFeatures(outName, col);
                //close store
                store.close();
                if (extraFunction != null && elements != null) {
                    extraFunction.accept(fileTmp, elements);
                }
                if (isCsvFile) {
                    // we want to override the columns header with the columns' text instead of name field
//                    fileTmp.
                    try (final BufferedReader br = new BufferedReader(new FileReader(fileTmp))) {
                        //output file path
                        File file = createFile(filePrefix, fileExtension, null);

                        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                            String line = br.readLine();
                            // Modify header
                            final List<String> newHeader = Arrays.stream(line.split(";"))
                                    .map(header -> columnsToFilter.get(header.substring(0, header.indexOf("("))).replace("\n", " "))
                                    .collect(Collectors.toList());
                            bw.write(String.join(";", newHeader));

                            // copy all remaining lines
                            while ((line = br.readLine()) != null) {
                                bw.newLine();
                                bw.write(line);
                            }
                            fileTmp.delete();
                        }
                    }
                }
            }
            if (allColsEmpty) {
                Platform.runLater(() ->
                    new Growl(Growl.Type.WARNING, "La couche sélectionnée est vide.").showAndFade());
            }
            done();
        } catch (DataStoreException ex) {
            Loggers.DATA.log(Level.WARNING, ex.getMessage(), ex);
            setException(ex);
            return false;
        }
        return true;
    }

    private File createFile(final String filePrefix, final String fileExtension, String extra) {
        extra = extra == null ? "" : extra;
        File file = new File(folder, filePrefix + extra + fileExtension);
        //if fileTmp exist, add date aside it
        if (file.exists()) {
            //generate name + time
            String name = filePrefix + " " + TemporalUtilities.toISO8601(new Date()) + extra + fileExtension;
            name = name.replace(':', '_');
            file = new File(folder, name);
            //it should not exist, but delete it if there is one in case
            file.delete();
        }
        return file;
    }

    private FeatureType filterFTByColumns(final FeatureType ft) {
        if (columnsToFilter == null || columnsToFilter.isEmpty()) return ft;
        final List<GenericName> pkeys = new ArrayList<GenericName>();

        final Set<String> columnsNames = columnsToFilter.keySet();
        for (String toKeep: columnsNames) {
            PropertyDescriptor descriptor = ft.getDescriptor(toKeep);
            if (descriptor != null) pkeys.add(descriptor.getName());
        }

        if (pkeys.isEmpty()){
            return ft;
        } else {
            return createSubType(ft, pkeys.toArray(new GenericName[pkeys.size()]));
        }
    }

    private static class FillCoordCollection extends WrapFeatureCollection{

        public FillCoordCollection(FeatureCollection originalFC) {
            super(originalFC);
        }

        @Override
        protected Feature modify(Feature feature) throws FeatureStoreRuntimeException {
            final Object baseBean = feature.getUserData().get(BeanFeature.KEY_BEAN);

            if(baseBean instanceof Positionable){
                final Positionable pos = (Positionable) baseBean;
                final TronconUtils.PosInfo info = new TronconUtils.PosInfo(pos);

                try{
                    //on calcul les informations au besoin
                    feature.setPropertyValue("positionDebut", info.getGeoPointStart());
                    feature.setPropertyValue("positionFin", info.getGeoPointEnd());
                    final TronconUtils.PosSR possr = info.getForSR();
                    feature.setPropertyValue("systemeRepId", possr.srid);
                    feature.setPropertyValue("borneDebutId", possr.borneStartId);
                    feature.setPropertyValue("borne_debut_distance", (float)possr.distanceStartBorne);
//                    feature.setPropertyValue("positionFin", possr.startAval);
                    feature.setPropertyValue("borneFinId", possr.borneEndId);
                    feature.setPropertyValue("borne_fin_distance", (float)possr.distanceEndBorne);
//                    feature.setPropertyValue("positionFin", possr.endAval);
                }catch(Exception ex){
                    SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
                }
            }

            return feature;
        }
    }


}
