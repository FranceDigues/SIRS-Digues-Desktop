package fr.sirs.importer.theme.document.related.profilLong;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.RefTypeProfilTravers;
import fr.sirs.importer.DbImporter;
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
class TypePositionProfilLongImporter extends GenericImporter {

    private Map<Integer, RefTypeProfilTravers> typesPositionProfilLong = null;

    TypePositionProfilLongImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }
    
    private enum TypeProfilTraversColumns {
        ID_TYPE_POSITION_PROFIL_EN_LONG,
        LIBELLE_TYPE_POSITION_PROFIL_EN_LONG,
        ABREGE_TYPE_POSITION_PROFIL_EN_LONG,
        DATE_DERNIERE_MAJ
    };

    /**
     * 
     * @return A map containing all the database RefTypeProfilTravers referenced by their
     * internal ID.
     * @throws IOException 
     */
    public Map<Integer, RefTypeProfilTravers> getTypePositionProfilLong() throws IOException {
        if(typesPositionProfilLong == null) compute();
        return typesPositionProfilLong;
    }

    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (TypeProfilTraversColumns c : TypeProfilTraversColumns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.TYPE_POSITION_PROFIL_EN_LONG_SUR_DIGUE.toString();
    }

    @Override
    protected void compute() throws IOException {
        typesPositionProfilLong = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
//            final RefTypeProfilTravers typeProfilTravers = new RefTypeProfilTravers();
//            
//            typeProfilTravers.setLibelle(row.getString(TypeProfilTraversColumns.LIBELLE_TYPE_PROFIL_EN_TRAVERS.toString()));
//            
//            if (row.getDate(TypeProfilTraversColumns.DATE_DERNIERE_MAJ.toString()) != null) {
//                typeProfilTravers.setDateMaj(LocalDateTime.parse(row.getDate(TypeProfilTraversColumns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
//            }
//            typesPositionProfilLong.put(row.getInt(String.valueOf(TypeProfilTraversColumns.ID_TYPE_PROFIL_EN_TRAVERS.toString())), typeProfilTravers);
        }
        couchDbConnector.executeBulk(typesPositionProfilLong.values());
    }
}
