package fr.sirs.importer.v2.references;

import fr.sirs.core.model.RefTypeProfilTravers;
import static fr.sirs.importer.DbImporter.TableName.*;
import org.springframework.stereotype.Component;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
@Component
class TypeProfilTraversImporter extends GenericTypeReferenceImporter<RefTypeProfilTravers> {

    @Override
    public String getTableName() {
        return TYPE_PROFIL_EN_TRAVERS.toString();
    }

    @Override
    public Class<RefTypeProfilTravers> getElementClass() {
        return RefTypeProfilTravers.class;
    }
}
