package fr.sirs.importer.link;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.DocumentTroncon;
import fr.sirs.core.model.DocumentGrandeEchelle;
import fr.sirs.core.model.Prestation;
import fr.sirs.core.model.RapportEtude;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.objet.prestation.PrestationImporter;
import fr.sirs.importer.theme.document.DocumentImporter;
import fr.sirs.importer.theme.document.related.documentAGrandeEchelle.DocumentAGrandeEchelleImporter;
import fr.sirs.importer.theme.document.related.rapportEtude.RapportEtudeImporter;
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
public class PrestationDocumentImporter extends GenericEntityLinker {

    private final PrestationImporter prestationImporter;
    private final DocumentImporter documentImporter;
    
    final DocumentAGrandeEchelleImporter documentAGrandeEchelleImporter;
    final RapportEtudeImporter rapportEtudeImporter;
        
        
    public PrestationDocumentImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector,
            final PrestationImporter prestationImporter,
            final DocumentImporter documentImporter) {
        super(accessDatabase, couchDbConnector);
        this.prestationImporter = prestationImporter;
        this.documentImporter = documentImporter;
        
        documentAGrandeEchelleImporter = documentImporter.getDocumentAGrandeEchelleImporter();
        rapportEtudeImporter = documentImporter.getRapportEtudeImporter();
    }

    private enum Columns {
        ID_PRESTATION,
        ID_DOC,
//        DATE_DERNIERE_MAJ
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
        return DbImporter.TableName.PRESTATION_DOCUMENT.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {
        
        final Map<Integer, Prestation> prestations = prestationImporter.getById();
        final Map<Integer, DocumentTroncon> documents = documentImporter.getDocuments();
        final Map<String, RapportEtude> rapportsEtude = rapportEtudeByCouchDbId();
//        final Map<String, DocumentGrandeEchelle> documentsGrandeEchelle = documentGrandeEchelleByCouchDbId();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            
            final Prestation prestation = prestations.get(row.getInt(Columns.ID_PRESTATION.toString()));
            final DocumentTroncon document = documents.get(row.getInt(Columns.ID_DOC.toString()));
            
            if(prestation!=null && document!=null){
                if(document.getSirsdocument()!=null){
                    /*if(documentsGrandeEchelle.get(document.getSirsdocument())!=null){
                        final DocumentGrandeEchelle documentGrandeEchelle = documentsGrandeEchelle.get(document.getSirsdocument());
                        documentGrandeEchelle.getPrestation().add(prestation.getId());
                        prestation.getDocumentGrandeEchelle().add(documentGrandeEchelle.getId());
                    } 
                    else*/ if (rapportsEtude.get(document.getSirsdocument())!=null){
                        final RapportEtude rapport = rapportsEtude.get(document.getSirsdocument());
                        rapport.getPrestation().add(prestation.getId());
                        prestation.getRapportEtude().add(rapport.getId());
                    }
                }
            }
        }
    }
    
    private Map<String, RapportEtude> rapportEtudeByCouchDbId() 
            throws IOException, AccessDbImporterException{
        final Map<String, RapportEtude> rapports = new HashMap<>();
        for(final RapportEtude rapport : rapportEtudeImporter.getRelated().values()){
            rapports.put(rapport.getId(), rapport);
        }
        return rapports;
    }
    
//    private Map<String, DocumentGrandeEchelle> documentGrandeEchelleByCouchDbId() 
//            throws IOException, AccessDbImporterException{
//        final Map<String, DocumentGrandeEchelle> documents = new HashMap<>();
//        for(final DocumentGrandeEchelle document : documentAGrandeEchelleImporter.getRelated().values()){
//            documents.put(document.getId(), document);
//        }
//        return documents;
//    }
}
