package fr.sirs.importer.objet.reseau;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.RefConduiteFermee;
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
class TypeConduiteFermeeImporter extends GenericTypeReferenceImporter<RefConduiteFermee> {
    
    TypeConduiteFermeeImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    private enum Columns {
        ID_TYPE_CONDUITE_FERMEE,
        LIBELLE_TYPE_CONDUITE_FERMEE,
        ABREGE_TYPE_CONDUITE_FERMEE,
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
        return DbImporter.TableName.TYPE_CONDUITE_FERMEE.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefConduiteFermee typeConduite = new RefConduiteFermee();
            
            typeConduite.setId(typeConduite.getClass().getSimpleName()+":"+row.getInt(String.valueOf(Columns.ID_TYPE_CONDUITE_FERMEE.toString())));
            typeConduite.setLibelle(row.getString(Columns.LIBELLE_TYPE_CONDUITE_FERMEE.toString()));
            typeConduite.setAbrege(row.getString(Columns.ABREGE_TYPE_CONDUITE_FERMEE.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeConduite.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            
            typeConduite.setPseudoId(String.valueOf(row.getInt(String.valueOf(Columns.ID_TYPE_CONDUITE_FERMEE.toString()))));
            typeConduite.setValid(true);
            
            types.put(row.getInt(String.valueOf(Columns.ID_TYPE_CONDUITE_FERMEE.toString())), typeConduite);
        }
        couchDbConnector.executeBulk(types.values());
    }
}
