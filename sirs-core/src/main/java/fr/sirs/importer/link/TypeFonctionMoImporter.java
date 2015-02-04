package fr.sirs.importer.link;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.RefFoncitonMaitreOeuvre;
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
class TypeFonctionMoImporter extends GenericTypeReferenceImporter<RefFoncitonMaitreOeuvre> {

    TypeFonctionMoImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }
    
    private enum Columns {
        ID_FONCTION_MO,
        LIBELLE_FONCTION_MO,
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
        return DbImporter.TableName.TYPE_FONCTION_MO.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefFoncitonMaitreOeuvre typeFonctionMo = new RefFoncitonMaitreOeuvre();
            
            typeFonctionMo.setId(typeFonctionMo.getClass().getSimpleName()+":"+row.getInt(String.valueOf(Columns.ID_FONCTION_MO.toString())));
            typeFonctionMo.setLibelle(row.getString(Columns.LIBELLE_FONCTION_MO.toString()));
            
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeFonctionMo.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            types.put(row.getInt(String.valueOf(Columns.ID_FONCTION_MO.toString())), typeFonctionMo);
        }
        couchDbConnector.executeBulk(types.values());
    }
}
