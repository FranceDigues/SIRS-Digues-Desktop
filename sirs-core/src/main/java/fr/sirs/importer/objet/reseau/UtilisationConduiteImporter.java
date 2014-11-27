package fr.sirs.importer.objet.reseau;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.RefUtilisationConduite;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.GenericTypeImporter;
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
class UtilisationConduiteImporter extends GenericTypeImporter<RefUtilisationConduite> {
    
    
    UtilisationConduiteImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    private enum TypeUtilisationConduiteColumns {
        ID_UTILISATION_CONDUITE,
        LIBELLE_UTILISATION_CONDUITE,
        ABREGE_UTILISATION_CONDUITE,
        DATE_DERNIERE_MAJ
    };
    
    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (TypeUtilisationConduiteColumns c : TypeUtilisationConduiteColumns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.UTILISATION_CONDUITE.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefUtilisationConduite typeUtilisation = new RefUtilisationConduite();
            
            typeUtilisation.setLibelle(row.getString(TypeUtilisationConduiteColumns.LIBELLE_UTILISATION_CONDUITE.toString()));
            typeUtilisation.setAbrege(row.getString(TypeUtilisationConduiteColumns.ABREGE_UTILISATION_CONDUITE.toString()));
            if (row.getDate(TypeUtilisationConduiteColumns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeUtilisation.setDateMaj(LocalDateTime.parse(row.getDate(TypeUtilisationConduiteColumns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            types.put(row.getInt(String.valueOf(TypeUtilisationConduiteColumns.ID_UTILISATION_CONDUITE.toString())), typeUtilisation);
        }
        couchDbConnector.executeBulk(types.values());
    }
}
