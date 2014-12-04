package fr.sirs.importer;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.component.ContactRepository;
import fr.sirs.core.model.Contact;
import static fr.sirs.importer.DbImporter.cleanNullString;
import java.io.IOException;
import java.time.LocalDateTime;
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
public class IntervenantImporter extends GenericImporter {

    private Map<Integer, Contact> intervenants = null;
    private ContactRepository contactRepository;

    private IntervenantImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    IntervenantImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector, 
            final ContactRepository contactRepository) {
        this(accessDatabase, couchDbConnector);
        this.contactRepository = contactRepository;
    }

    private enum Columns {
        ID_INTERVENANT,
        NOM_INTERVENANT,
        PRENOM_INTERVENANT,
        ADRESSE_PERSO_INTERV,
        ADRESSE_L1_PERSO_INTERV,
        ADRESSE_L2_PERSO_INTERV,
        ADRESSE_L3_PERSO_INTERV,
        ADRESSE_CODE_POSTAL_PERSO_INTERV,
        ADRESSE_NOM_COMMUNE_PERSO_INTERV,
        TEL_PERSO_INTERV,
        FAX_PERSO_INTERV,
        MAIL_INTERV,
        SERVICE_INTERV,
        FONCTION_INTERV,
        DATE_DEBUT,
        DATE_FIN,
        DATE_DERNIERE_MAJ
    };

    public Map<Integer, Contact> getIntervenants() throws IOException {
        if (intervenants == null) compute();
        return intervenants;
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
        return DbImporter.TableName.INTERVENANT.toString();
    }

    @Override
    protected void compute() throws IOException {
        intervenants = new HashMap<>();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final Contact intervenant = new Contact();

            intervenant.setNom(row.getString(Columns.NOM_INTERVENANT.toString()));
            
            intervenant.setPrenom(row.getString(Columns.PRENOM_INTERVENANT.toString()));
            
            intervenant.setAdresse(cleanNullString(row.getString(Columns.ADRESSE_PERSO_INTERV.toString()))
                    + cleanNullString(row.getString(Columns.ADRESSE_L1_PERSO_INTERV.toString()))
                    + cleanNullString(row.getString(Columns.ADRESSE_L2_PERSO_INTERV.toString()))
                    + cleanNullString(row.getString(Columns.ADRESSE_L3_PERSO_INTERV.toString())));
            
            intervenant.setCode_postal(cleanNullString(String.valueOf(row.getInt(Columns.ADRESSE_CODE_POSTAL_PERSO_INTERV.toString()))));
            
            intervenant.setCommune(row.getString(Columns.ADRESSE_NOM_COMMUNE_PERSO_INTERV.toString()));
            
            intervenant.setTelephone(row.getString(Columns.TEL_PERSO_INTERV.toString()));
            
            intervenant.setEmail(row.getString(Columns.MAIL_INTERV.toString()));
            
            intervenant.setFax(row.getString(Columns.FAX_PERSO_INTERV.toString()));
            
            intervenant.setService(row.getString(Columns.SERVICE_INTERV.toString()));
            
            intervenant.setFonction(row.getString(Columns.FONCTION_INTERV.toString()));
            
            if (row.getDate(Columns.DATE_DEBUT.toString()) != null) {
                intervenant.setDate_debut(LocalDateTime.parse(row.getDate(Columns.DATE_DEBUT.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDate(Columns.DATE_FIN.toString()) != null) {
                intervenant.setDate_fin(LocalDateTime.parse(row.getDate(Columns.DATE_FIN.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                intervenant.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            
            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            intervenants.put(row.getInt(Columns.ID_INTERVENANT.toString()), intervenant);

        }
        couchDbConnector.executeBulk(intervenants.values());
    }
}
