package fr.sirs.importer.link;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.Contact;
import static fr.sirs.core.model.ElementCreator.createAnonymValidElement;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.ObjetStructure;
import fr.sirs.core.model.Organisme;
import fr.sirs.core.model.ProprieteObjet;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.DbImporter;
import static fr.sirs.importer.DbImporter.TableName.*;
import fr.sirs.importer.IntervenantImporter;
import fr.sirs.importer.OrganismeImporter;
import fr.sirs.importer.objet.structure.ElementStructureImporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class ElementStructureProprietaireImporter extends GenericEntityLinker {

    private final ElementStructureImporter elementStructureImporter;
    private final IntervenantImporter intervenantImporter;
    private final OrganismeImporter organismeImporter;
    
    public ElementStructureProprietaireImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector,
            final ElementStructureImporter elementStructureImporter,
            final IntervenantImporter intervenantImporter,
            final OrganismeImporter organismeImporter) {
        super(accessDatabase, couchDbConnector);
        this.elementStructureImporter = elementStructureImporter;
        this.intervenantImporter = intervenantImporter;
        this.organismeImporter = organismeImporter;
    }

    private enum Columns {
        ID_ELEMENT_STRUCTURE,
        DATE_DEBUT_PROPRIO,
        DATE_FIN_PROPRIO,
        ID_ORG_PROPRIO,
        ID_INTERV_PROPRIO,
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
        return ELEMENT_STRUCTURE_PROPRIETAIRE.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {
        
        final Map<Integer, ObjetStructure> structures = elementStructureImporter.getById();
        final Map<Integer, Contact> intervenants = intervenantImporter.getIntervenants();
        final Map<Integer, Organisme> organismes = organismeImporter.getOrganismes();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            
            final ObjetStructure structure = structures.get(row.getInt(Columns.ID_ELEMENT_STRUCTURE.toString()));
            final Contact intervenant = intervenants.get(row.getInt(Columns.ID_INTERV_PROPRIO.toString()));
            final Organisme organisme = organismes.get(row.getInt(Columns.ID_ORG_PROPRIO.toString()));
            
            if(structure!=null && (intervenant!=null || organisme!=null)){
                final ProprieteObjet propriete;
                if(intervenant!=null){
                    propriete = readContactStructure(row);
                    propriete.setContactId(intervenant.getId());
                    structure.getProprietes().add(propriete);
                }
                else{
                    propriete = readOrganismeStructure(row);
                    propriete.setOrganismeId(organisme.getId());
                    structure.getProprietes().add(propriete);
                }
            }
        }
    }
    
    
    private ProprieteObjet readContactStructure(final Row row){
        
        final ProprieteObjet propriete = createAnonymValidElement(ProprieteObjet.class);

        if (row.getDate(Columns.DATE_DEBUT_PROPRIO.toString()) != null) {
            propriete.setDate_debut(DbImporter.parse(row.getDate(Columns.DATE_DEBUT_PROPRIO.toString()), dateTimeFormatter));
        }

        if (row.getDate(Columns.DATE_FIN_PROPRIO.toString()) != null) {
            propriete.setDate_fin(DbImporter.parse(row.getDate(Columns.DATE_FIN_PROPRIO.toString()), dateTimeFormatter));
        }

        if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
            propriete.setDateMaj(DbImporter.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()), dateTimeFormatter));
        }
        // Jointure, donc pas d'id propre : on choisit arbitrairement l'id du proprio.
        propriete.setDesignation(String.valueOf(row.getInt(Columns.ID_INTERV_PROPRIO.toString())));
        
        return propriete;
    }
    
    
    private ProprieteObjet readOrganismeStructure(final Row row){
        
        final ProprieteObjet propriete = createAnonymValidElement(ProprieteObjet.class);

        if (row.getDate(Columns.DATE_DEBUT_PROPRIO.toString()) != null) {
            propriete.setDate_debut(DbImporter.parse(row.getDate(Columns.DATE_DEBUT_PROPRIO.toString()), dateTimeFormatter));
        }

        if (row.getDate(Columns.DATE_FIN_PROPRIO.toString()) != null) {
            propriete.setDate_fin(DbImporter.parse(row.getDate(Columns.DATE_FIN_PROPRIO.toString()), dateTimeFormatter));
        }

        if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
            propriete.setDateMaj(DbImporter.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()), dateTimeFormatter));
        }
        // Jointure, donc pas d'id propre : on choisit arbitrairement l'id du proprio.
        propriete.setDesignation(String.valueOf(row.getInt(Columns.ID_ORG_PROPRIO.toString())));
        
        return propriete;
    }
}
