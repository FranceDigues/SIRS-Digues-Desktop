package fr.sirs.importer.v2;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.vividsolutions.jts.geom.Coordinate;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Identifiable;
import fr.sirs.core.model.Positionable;
import fr.sirs.importer.GenericImporter;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentOperationResult;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.esrigeodb.GeoDBStore;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.geotoolkit.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * Contains main properties and data needed for an import from an access
 * database to a couchDB database.
 *
 * @author Alexis Manin (Geomatys)
 */
public class ImportContext {

    public final String startXName = "X_DEBUT";
    public final String startYName = "Y_DEBUT";
    public final String endXName = "X_FIN";
    public final String endYName = "Y_FIN";

    /**
     * Input database containing object properties.
     */
    public final Database inputDb;
    /**
     * Input database containing projection and geometric information.
     */
    public final Database inputCartoDb;
    /**
     * Source database projection.
     */
    public final CoordinateReferenceSystem inputCRS;

    /**
     * Target database.
     */
    public final CouchDbConnector outputDb;
    /**
     * Target database projection.
     */
    public final CoordinateReferenceSystem outputCRS;

    /**
     * Transformation to convert a geometry from input database projection to
     * target database CRS.
     */
    public final MathTransform geoTransform;

    public final ConcurrentHashMap<Class, GenericImporter> importers = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Class, HashSet<DocumentModifier>> modifiers = new ConcurrentHashMap<>();

    /**
     * recommended limit size for bulk updates.
     * /!\ This flag is NOT used by {@link #executeBulk(java.util.Collection) }.
     * It's only an informative attribute which should be used by importers when computing.
     */
    public int bulkLimit = 10000;

    /**
     * List errors which occured while importing database. Errors can be
     * registered using {@link #reportError(fr.sirs.importer.v2.ErrorReport)
     * }.
     */
    public final ObservableList<ErrorReport> errors = FXCollections.observableArrayList();

    public ImportContext(final Database inputDb, final Database inputCartoDb, final CouchDbConnector outputDb, final CoordinateReferenceSystem outputCRS) throws FactoryException, MalformedURLException, DataStoreException {
        ArgumentChecks.ensureNonNull("Input database which contains properties", inputDb);
        ArgumentChecks.ensureNonNull("Input database which contains geometries", inputCartoDb);
        ArgumentChecks.ensureNonNull("Output database connection", outputDb);
        ArgumentChecks.ensureNonNull("Output projection", outputCRS);

        this.inputDb = inputDb;
        this.inputCartoDb = inputCartoDb;
        this.outputDb = outputDb;
        this.outputCRS = outputCRS;

        CoordinateReferenceSystem crs = null;
        try (final FeatureStore store = new GeoDBStore("test", inputCartoDb.getFile().toURI().toURL())) {
            for (final GenericName name : store.getNames()) {
                GeometryDescriptor geomDesc = store.getFeatureType(name).getGeometryDescriptor();
                if (geomDesc != null) {
                    crs = geomDesc.getType().getCoordinateReferenceSystem();
                    if (crs != null)
                        break;
                }
            }
        }

        if (crs == null) {
            inputCRS = CRS.decode("EPSG:27593", true);
        } else {
            inputCRS = crs;
        }

        geoTransform = CRS.findMathTransform(inputCRS, outputCRS, true);
    }

    public <T> T convertData(final Object input, final Class<T> outputClass) {
        if (outputClass.isAssignableFrom(input.getClass())) {
            return (T) input;
        } else if (input instanceof Date) {
            if (outputClass.isAssignableFrom(LocalDate.class)) {
                return (T) toLocalDate((Date) input);
            } else if (outputClass.isAssignableFrom(LocalDateTime.class)) {
                return (T) toLocalDateTime((Date) input);
            }
        }
        return ObjectConverters.convert(input, outputClass);
    }

    /**
     * Try to extract start and end points from given a row
     *
     * @param input
     * @param toSet
     * @throws TransformException
     */
    public void setGeoPositions(final Row input, final Positionable toSet) throws TransformException {
        final Double startX = input.getDouble(startXName);
        final Double startY = input.getDouble(startYName);
        final Double endX = input.getDouble(endXName);
        final Double endY = input.getDouble(endYName);

        final boolean hasGeoStart = startX != null && startY != null;
        final boolean hasGeoEnd = endX != null && endY != null;

        if (hasGeoStart && hasGeoEnd) {
            final double[] points = new double[]{startX, startY, endX, endY};
            geoTransform.transform(points, 0, points, 0, 2);
            toSet.setPositionDebut(GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(points[0], points[1])));
            toSet.setPositionFin(GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(points[2], points[3])));

        } else if (hasGeoStart) {
            final double[] points = new double[]{endX, endY};
            geoTransform.transform(points, 0, points, 0, 1);
            toSet.setPositionFin(GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(points[0], points[1])));

        } else if (hasGeoEnd) {
            final double[] points = new double[]{endX, endY};
            geoTransform.transform(points, 0, points, 0, 1);
            toSet.setPositionDebut(GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(points[0], points[1])));
        }
    }

    public void reportError(final String tableName, final Row sourceRow, final Exception occurredError) {
        ImportContext.this.reportError(new ErrorReport(occurredError, sourceRow, tableName));
    }

    public void reportError(final String tableName, final Row sourceRow, final Exception occurredError, final String message) {
        ErrorReport errorReport = new ErrorReport(occurredError, sourceRow, tableName);
        errorReport.customErrorMsg = message;
        ImportContext.this.reportError(errorReport);
    }

    /**
     * Inform system that an error occured while importing a document.
     *
     * @param report Error report to submit.
     */
    public void reportError(final ErrorReport report) {
        ArgumentChecks.ensureNonNull("Error report", report);
        errors.add(report);
        if (report.error == null) {
            SirsCore.LOGGER.log(Level.FINE, "New error submitted !");
        } else {
            SirsCore.LOGGER.log(Level.FINE, "New error submitted !", report.error);
        }
    }

    /**
     * Insert or update elements given, then return them along with their ids.
     * Documents which failed to be updated are not listed. Instead, an error
     * report is submitted for each one.
     *
     * @param toUpdate collection of elements to insert or update.
     * @return successfully inserted / updated documents and their ids.
     */
    public Map<String, Object> executeBulk(Collection toUpdate) {
        ArgumentChecks.ensureNonNull("Objects to update.", toUpdate);
        if (toUpdate.isEmpty())
            return new HashMap<>();

        // Ensure we update a document only once.
        if (!(toUpdate instanceof HashSet)) {
            toUpdate = new HashSet(toUpdate);
        }

        // Try to perform bulk, then analyze result to find errors.
        List<DocumentOperationResult> bulkResult = outputDb.executeBulk(new HashSet(toUpdate));
        HashMap<String, Object> ids = buildIdMap(toUpdate);
        if (bulkResult != null && !bulkResult.isEmpty()) {
            for (final DocumentOperationResult opResult : bulkResult) {
                final ErrorReport report = new ErrorReport();
                report.corruptionLevel = CorruptionLevel.ROW;
                report.customErrorMsg = buildErrorMsg(opResult);
                final String id = opResult.getId();
                if (id != null) {
                    report.target = ids.get(id);
                    ids.remove(id); // Remove, because we must not send ids of failed updates.
                }
                ImportContext.this.reportError(report);
            }
        }

        return ids;
    }

    /*
     * Utilities
     */
    /**
     * Convert a {@link Date} to a {@link LocalDate}.
     *
     * @param date Date to convert. If null, a null value is returned.
     * @return The converted date, or null if no input was given.
     */
    public static LocalDate toLocalDate(final Date date) {
        return date == null ? null : LocalDate.from(date.toInstant());
    }

    /**
     * Convert a {@link Date} to a {@link LocalDateTime}.
     *
     * @param date Date to convert. If null, a null value is returned.
     * @return The converted date and time, or null if no input was given.
     */
    public static LocalDateTime toLocalDateTime(final Date date) {
        return date == null ? null : LocalDateTime.from(date.toInstant());
    }

    /**
     * Get error information from input report to build a detailed error
     * message.
     *
     * @param error The CouchDB error report.
     * @return A formatted error message. Never null.
     * @throws IllegalArgumentException if input error report is null.
     */
    public static String buildErrorMsg(final DocumentOperationResult error) {
        ArgumentChecks.ensureNonNull("Error report", error);
        final StringBuilder builder = new StringBuilder();
        builder.append("An error occurred while ");
        final String id = error.getId();
        if (id != null && !id.isEmpty()) {
            builder.append("updating document with ID : ").append(id);
            final String revision = error.getRevision();
            if (revision != null && !revision.isEmpty()) {
                builder.append(" - revision : ").append(revision);
            }
        } else {
            builder.append("inserting a document.");
        }

        if (error.isErroneous()) {
            builder.append('\n')
                    .append("--- Error ---\n")
                    .append(builder);
        }

        final String reason = error.getReason();
        if (reason != null && !reason.isEmpty()) {
            builder.append('\n')
                    .append("--- Reason ---\n")
                    .append(builder);
        }

        return builder.toString();
    }

    /**
     * For each object in given collection, if it is an {@link Identifiable}
     * object, put it in a map whose keys are values identifiers.
     *
     * @param toAnalyze Collection of objects to sort.
     * @return A map containing all identifiable objects of input collection.
     * Never null, but can be empty.
     */
    public static HashMap<String, Object> buildIdMap(final Collection toAnalyze) {
        final HashMap<String, Object> result = new HashMap<>();
        for (final Object o : toAnalyze) {
            if (o instanceof Identifiable) {
                final Identifiable i = (Identifiable) o;
                if (i.getId() != null) {
                    result.put(i.getId(), i);
                }
            }
        }
        return result;
    }

    /**
     * Retrieve object imported from given row, then return it.
     * @param currentRow The row which has been used for import of searched object.
     * @return Object created after input row import. Never null.
     * @throws IllegalStateException if we cannot find any imported document for given row.
     */
    Object getBoundTarget(Row currentRow) throws IllegalStateException {

        throw new UnsupportedOperationException("Not supported yet.");
    }
}