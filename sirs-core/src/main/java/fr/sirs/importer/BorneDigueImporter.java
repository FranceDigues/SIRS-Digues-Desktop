package fr.sirs.importer;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.model.BorneDigue;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class BorneDigueImporter extends GenericImporter {

    private Map<Integer, BorneDigue> bornesDigue = null;
    private Map<Integer, List<BorneDigue>> bornesDigueByTronconId = null;

    BorneDigueImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }
    
    private enum Columns {
        ID_BORNE, 
        ID_TRONCON_GESTION, 
        NOM_BORNE,
        X_POINT, 
        Y_POINT, 
        Z_POINT, 
        DATE_DEBUT_VAL, 
        DATE_FIN_VAL,
        FICTIVE, 
//        X_POINT_ORIGINE, 
//        Y_POINT_ORIGINE,
        COMMENTAIRE_BORNE, 
        DATE_DERNIERE_MAJ
    };

    /**
     * 
     * @return A map containing all the BorneDigue instances referenced by their
     * internal database identifier.
     * @throws IOException 
     */
    public Map<Integer, BorneDigue> getBorneDigue() throws IOException {
        if (bornesDigue == null) compute();
        return bornesDigue;
    }
    
    /**
     * 
     * @return A map containing the BorneDigue lists referenced by the internal 
     * database TronconDigue idenfifier.
     * @throws IOException 
     */
    public Map<Integer, List<BorneDigue>> getBorneDigueByTronconId() throws IOException{
        if(bornesDigueByTronconId==null) compute();
        return bornesDigueByTronconId;
    }

    @Override
    protected List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for(Columns c : Columns.values())
            columns.add(c.toString());
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.BORNE_DIGUE.toString();
    }
    
    @Override
    protected void compute() throws IOException{
        bornesDigue = new HashMap<>();
        bornesDigueByTronconId = new HashMap<>();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final BorneDigue borne = new BorneDigue();

            borne.setLibelle(row.getString(Columns.NOM_BORNE.toString()));
            borne.setCommentaire(row.getString(Columns.COMMENTAIRE_BORNE.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                borne.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            if (row.getDate(Columns.DATE_DEBUT_VAL.toString()) != null) {
                borne.setDate_debut(LocalDateTime.parse(row.getDate(Columns.DATE_DEBUT_VAL.toString()).toString(), dateTimeFormatter));
            }
            if (row.getDate(Columns.DATE_FIN_VAL.toString()) != null) {
                borne.setDate_fin(LocalDateTime.parse(row.getDate(Columns.DATE_FIN_VAL.toString()).toString(), dateTimeFormatter));
            }
            borne.setFictive(row.getBoolean(Columns.FICTIVE.toString()));
            GeometryFactory geometryFactory = new GeometryFactory();

            try {
                final MathTransform lambertToRGF = CRS.findMathTransform(CRS.decode("EPSG:27563"), getOutputCrs(), true);

                final Point point;
                if (row.getDouble(Columns.Z_POINT.toString()) != null) {
                    point = (Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                            row.getDouble(Columns.X_POINT.toString()),
                            row.getDouble(Columns.Y_POINT.toString()),
                            row.getDouble(Columns.Z_POINT.toString()))), lambertToRGF);
                } else {
                    point = (Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                            row.getDouble(Columns.X_POINT.toString()),
                            row.getDouble(Columns.Y_POINT.toString()))), lambertToRGF);
                }
                borne.setGeometry(point);
            } catch (FactoryException | MismatchedDimensionException | TransformException ex) {
                Logger.getLogger(BorneDigueImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            borne.setPseudoId(String.valueOf(row.getInt(Columns.ID_BORNE.toString())));
            borne.setValid(true);
            
            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            bornesDigue.put(row.getInt(Columns.ID_BORNE.toString()), borne);

            // Set the list ByTronconId
            List<BorneDigue> listByTronconId = bornesDigueByTronconId.get(row.getInt(Columns.ID_TRONCON_GESTION.toString()));
            if (listByTronconId == null) {
                listByTronconId = new ArrayList<>();
                bornesDigueByTronconId.put(row.getInt(Columns.ID_TRONCON_GESTION.toString()), listByTronconId);
            }
            listByTronconId.add(borne);
            bornesDigueByTronconId.put(row.getInt(Columns.ID_TRONCON_GESTION.toString()), listByTronconId);
        }
        couchDbConnector.executeBulk(bornesDigue.values());
    }
}
