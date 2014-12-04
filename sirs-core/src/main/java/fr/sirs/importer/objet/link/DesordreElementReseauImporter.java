package fr.sirs.importer.objet.link;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.Desordre;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.DbImporter;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.OuvrageParticulier;
import fr.sirs.core.model.OuvrageTelecomEnergie;
import fr.sirs.core.model.OuvrageVoirie;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.ReseauHydroCielOuvert;
import fr.sirs.core.model.ReseauTelecomEnergie;
import fr.sirs.core.model.VoieDigue;
import fr.sirs.importer.objet.desordre.DesordreImporter;
import fr.sirs.importer.objet.reseau.ElementReseauImporter;
import fr.sirs.importer.objet.reseau.TypeElementReseauImporter;
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
public class DesordreElementReseauImporter extends GenericObjetLinker {
    
    private final ElementReseauImporter elementReseauImporter;
    private final DesordreImporter desordreImporter;
    private final TypeElementReseauImporter typeElementReseauImporter;

    public DesordreElementReseauImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector, 
            final ElementReseauImporter elementReseauImporter,
            final DesordreImporter desordreImporter) {
        super(accessDatabase, couchDbConnector);
        this.elementReseauImporter = elementReseauImporter;
        this.desordreImporter = desordreImporter;
        typeElementReseauImporter = elementReseauImporter.getTypeElementReseauImporter();
    }

    private enum Columns {
        ID_DESORDRE,
        ID_ELEMENT_RESEAU,
        ID_TYPE_ELEMENT_RESEAU,
//        DATE_DERNIERE_MAJ
    };

    @Override
    public String getTableName() {
        return DbImporter.TableName.DESORDRE_ELEMENT_RESEAU.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {
        
        final Map<Integer, Objet> elementsReseaux = elementReseauImporter.getById();
        final Map<Integer, Desordre> desordres = desordreImporter.getById();
        final Map<Integer, Class> classesElementReseaux = typeElementReseauImporter.getTypes();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
//            final ObjetReferenceObjet elementReseauDesordre = new ObjetReferenceObjet();
            
            final Objet elementReseau = elementsReseaux.get(row.getInt(Columns.ID_ELEMENT_RESEAU.toString()));
            final Class classeElementReseau = classesElementReseaux.get(row.getInt(Columns.ID_TYPE_ELEMENT_RESEAU.toString()));
            final Desordre desordre = desordres.get(row.getInt(Columns.ID_DESORDRE.toString()));
            
            if(elementReseau!=null && desordre!=null){
                
                if (elementReseau.getClass().equals(classeElementReseau)) {
                    if (elementReseau instanceof VoieDigue) {
                        desordre.getVoie_digue().add(elementReseau.getId());
                    } 
                    else if (elementReseau instanceof OuvrageParticulier){
                        desordre.getOuvrage_particulier().add(elementReseau.getId());
                    }
                    else if (elementReseau instanceof OuvrageHydrauliqueAssocie){
                        desordre.getOuvrag_hydraulique_associe().add(elementReseau.getId());
                    }
                    else if (elementReseau instanceof OuvrageTelecomEnergie){
                        desordre.getOuvrage_telecom_energie().add(elementReseau.getId());
                    }
                    else if (elementReseau instanceof OuvrageVoirie){
                        desordre.getOuvrage_voirie().add(elementReseau.getId());
                    }
                    else if (elementReseau instanceof ReseauTelecomEnergie){
                        desordre.getReseau_telecom_energie().add(elementReseau.getId());
                    }
                    else if (elementReseau instanceof ReseauHydroCielOuvert){
                        desordre.getReseau_hydro_ciel_ouvert().add(elementReseau.getId());
                    }
                    else if (elementReseau instanceof ReseauHydrauliqueFerme){
                        desordre.getReseau_hydraulique_ferme().add(elementReseau.getId());
                    }
                    else {
                        System.out.println(elementReseau.getClass().getSimpleName());
                        throw new AccessDbImporterException("Bad type.");
                    }
                }
                else {
                    throw new AccessDbImporterException("Bad referenced type. Incoherent data.");
                }
            }
        }
    }

    @Override
    protected List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (Columns c : Columns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }
}
