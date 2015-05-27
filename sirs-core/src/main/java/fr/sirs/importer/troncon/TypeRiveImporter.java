package fr.sirs.importer.troncon;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import static fr.sirs.core.model.ElementCreator.createAnonymValidElement;
import fr.sirs.core.model.RefRive;
import static fr.sirs.importer.DbImporter.TableName.*;
import fr.sirs.importer.GenericTypeReferenceImporter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class TypeRiveImporter extends GenericTypeReferenceImporter<RefRive> {

    TypeRiveImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }
    
    private enum Columns {
        ID_TYPE_RIVE, 
        LIBELLE_TYPE_RIVE, 
        DATE_DERNIERE_MAJ
    };

    @Override
    protected List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (Columns c : Columns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return TYPE_RIVE.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefRive typeRive = createAnonymValidElement(RefRive.class);
            
            typeRive.setId(typeRive.getClass().getSimpleName()+":"+row.getInt(String.valueOf(Columns.ID_TYPE_RIVE.toString())));
            typeRive.setLibelle(row.getString(Columns.LIBELLE_TYPE_RIVE.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeRive.setDateMaj(DbImporter.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()), dateTimeFormatter));
            }
            
            typeRive.setDesignation(String.valueOf(row.getInt(String.valueOf(Columns.ID_TYPE_RIVE.toString()))));
            
            types.put(row.getInt(String.valueOf(Columns.ID_TYPE_RIVE.toString())), typeRive);
        }
        couchDbConnector.executeBulk(types.values());
    }
}
