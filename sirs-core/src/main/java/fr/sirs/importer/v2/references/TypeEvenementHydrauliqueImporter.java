package fr.sirs.importer.v2.references;

import fr.sirs.core.model.RefEvenementHydraulique;
import static fr.sirs.importer.DbImporter.TableName.*;
import org.springframework.stereotype.Component;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
@Component
class TypeEvenementHydrauliqueImporter extends GenericTypeReferenceImporter<RefEvenementHydraulique> {

    @Override
    public String getTableName() {
        return TYPE_EVENEMENT_HYDRAU.toString();
    }

    @Override
    public Class<RefEvenementHydraulique> getElementClass() {
        return RefEvenementHydraulique.class;
    }
}
