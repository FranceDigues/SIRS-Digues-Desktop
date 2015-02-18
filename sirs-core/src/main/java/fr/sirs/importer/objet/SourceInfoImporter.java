package fr.sirs.importer.objet;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.RefSource;
import fr.sirs.importer.DbImporter;
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
public class SourceInfoImporter extends GenericTypeReferenceImporter<RefSource> {
    
    public SourceInfoImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    private enum Columns {
        ID_SOURCE,
        LIBELLE_SOURCE,
        ABREGE_TYPE_SOURCE_INFO,
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
        return DbImporter.TableName.SOURCE_INFO.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefSource typeSource = new RefSource();
            
            typeSource.setId(typeSource.getClass().getSimpleName()+":"+row.getInt(String.valueOf(Columns.ID_SOURCE.toString())));
            typeSource.setLibelle(row.getString(Columns.LIBELLE_SOURCE.toString()));
            typeSource.setAbrege(row.getString(Columns.ABREGE_TYPE_SOURCE_INFO.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeSource.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            typeSource.setPseudoId(row.getInt(String.valueOf(Columns.ID_SOURCE.toString())));
            types.put(row.getInt(String.valueOf(Columns.ID_SOURCE.toString())), typeSource);
        }
        couchDbConnector.executeBulk(types.values());
    }   
}
