package fr.sirs.importer.objet;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import static fr.sirs.core.model.ElementCreator.createAnonymValidElement;
import fr.sirs.core.model.RefFonction;
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
public class TypeFonctionImporter extends GenericTypeReferenceImporter<RefFonction> {
    
    TypeFonctionImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    private enum Columns {
        ID_TYPE_FONCTION,
        LIBELLE_TYPE_FONCTION,
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
        return TYPE_FONCTION.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefFonction typeFonction = createAnonymValidElement(RefFonction.class);
            
            typeFonction.setId(typeFonction.getClass().getSimpleName()+":"+row.getInt(String.valueOf(Columns.ID_TYPE_FONCTION.toString())));
            typeFonction.setLibelle(row.getString(Columns.LIBELLE_TYPE_FONCTION.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeFonction.setDateMaj(DbImporter.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()), dateTimeFormatter));
            }
            typeFonction.setDesignation(String.valueOf(row.getInt(String.valueOf(Columns.ID_TYPE_FONCTION.toString()))));
            
            types.put(row.getInt(String.valueOf(Columns.ID_TYPE_FONCTION.toString())), typeFonction);
        }
        couchDbConnector.executeBulk(types.values());
    }
    
}
