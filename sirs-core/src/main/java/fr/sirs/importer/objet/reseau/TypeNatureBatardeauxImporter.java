package fr.sirs.importer.objet.reseau;

import fr.sirs.core.model.RefNatureBatardeaux;
import static fr.sirs.importer.DbImporter.TableName.*;
import fr.sirs.importer.v2.references.GenericTypeReferenceImporter;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class TypeNatureBatardeauxImporter extends GenericTypeReferenceImporter<RefNatureBatardeaux> {

    @Override
    public String getTableName() {
        return TYPE_NATURE_BATARDEAUX.toString();
    }

    @Override
    protected Class<RefNatureBatardeaux> getDocumentClass() {
	return RefNatureBatardeaux.class;
    }
}
