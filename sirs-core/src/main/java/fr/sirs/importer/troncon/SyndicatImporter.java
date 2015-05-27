package fr.sirs.importer.troncon;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import static fr.sirs.core.model.ElementCreator.createAnonymValidElement;
import fr.sirs.core.model.Syndicat;
import fr.sirs.importer.DbImporter;
import static fr.sirs.importer.DbImporter.TableName.*;
import static fr.sirs.importer.DbImporter.cleanNullString;
import fr.sirs.importer.GenericImporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class SyndicatImporter extends GenericImporter {

    private Map<Integer, Syndicat> syndicats = null;

    SyndicatImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    private enum Columns {
        ID_SYNDICAT,
        LIBELLE_SYNDICAT,
        DATE_DERNIERE_MAJ
    };

    public Map<Integer, Syndicat> getSyndicats() throws IOException {
        if (syndicats == null) compute();
        return syndicats;
    }

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
        return SYNDICAT.toString();
    }

    @Override
    protected void compute() throws IOException {
        syndicats = new HashMap<>();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final Syndicat syndicat = createAnonymValidElement(Syndicat.class);
            
            syndicat.setLibelle(cleanNullString(row.getString(Columns.LIBELLE_SYNDICAT.toString())));
            
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                syndicat.setDateMaj(DbImporter.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()), dateTimeFormatter));
            }
            
            syndicat.setDesignation(String.valueOf(row.getInt(Columns.ID_SYNDICAT.toString())));
            
            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            syndicats.put(row.getInt(Columns.ID_SYNDICAT.toString()), syndicat);
        }
        couchDbConnector.executeBulk(syndicats.values());
    }
}
