package fr.sirs.importer.link;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.MaitreOeuvreMarche;
import fr.sirs.core.model.Marche;
import fr.sirs.core.model.Organisme;
import fr.sirs.core.model.RefFonctionMaitreOeuvre;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.OrganismeImporter;
import fr.sirs.importer.documentTroncon.document.marche.MarcheImporter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class MarcheMaitreOeuvreImporter extends GenericEntityLinker {

    private final MarcheImporter marcheImporter;
    private final OrganismeImporter organismeImporter;
    private final TypeFonctionMoImporter typeFonctionMoImporter;
    
    public MarcheMaitreOeuvreImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector,
            final MarcheImporter marcheImporter,
            final OrganismeImporter organismeImporter) {
        super(accessDatabase, couchDbConnector);
        this.marcheImporter = marcheImporter;
        this.organismeImporter = organismeImporter;
        this.typeFonctionMoImporter = new TypeFonctionMoImporter(accessDatabase, 
                couchDbConnector);
    }

    private enum Columns {
        ID_MARCHE,
        ID_ORGANISME,
        ID_FONCTION_MO,
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
        return DbImporter.TableName.MARCHE_MAITRE_OEUVRE.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {
        
        final Map<Integer, Marche> marches = marcheImporter.getRelated();
        final Map<Integer, Organisme> organismes = organismeImporter.getOrganismes();
        final Map<Integer, RefFonctionMaitreOeuvre> fonctionsMo = typeFonctionMoImporter.getTypeReferences();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            
            final Marche marche = marches.get(row.getInt(Columns.ID_MARCHE.toString()));
            final Organisme maitreOeuvre = organismes.get(row.getInt(Columns.ID_ORGANISME.toString()));
            
            if(marche!=null && maitreOeuvre!=null){
                final MaitreOeuvreMarche maitreOeuvreMarche = new MaitreOeuvreMarche();
                
                maitreOeuvreMarche.setFonctionMaitreOeuvre(fonctionsMo.get(row.getInt(Columns.ID_FONCTION_MO.toString())).getId());
            
                if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                    maitreOeuvreMarche.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
                }
            
                maitreOeuvreMarche.setOrganismeId(maitreOeuvre.getId());
                
                // Jointure, donc pas d'ID propre : on affecte l'id de l'organisme comme pseudo-id.
                maitreOeuvreMarche.setDesignation(String.valueOf(row.getInt(Columns.ID_ORGANISME.toString())));
                maitreOeuvreMarche.setValid(true);
                marche.getMaitreOeuvre().add(maitreOeuvreMarche);
            }
        }
    }
}
