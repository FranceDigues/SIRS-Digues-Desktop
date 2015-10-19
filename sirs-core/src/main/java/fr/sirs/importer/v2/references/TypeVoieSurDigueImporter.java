package fr.sirs.importer.v2.references;

import fr.sirs.core.model.RefVoieDigue;
import static fr.sirs.importer.DbImporter.TableName.*;
import fr.sirs.importer.v2.references.GenericTypeReferenceImporter;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class TypeVoieSurDigueImporter extends GenericTypeReferenceImporter<RefVoieDigue> {

    @Override
    public String getTableName() {
        return TYPE_VOIE_SUR_DIGUE.toString();
    }

    @Override
    protected Class<RefVoieDigue> getElementClass() {
	return RefVoieDigue.class;
    }
}
