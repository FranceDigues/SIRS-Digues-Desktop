package fr.sirs.importer.objet.ligneEau;

import fr.sirs.core.model.LigneEau;
import fr.sirs.importer.v2.event.EvenementHydrauliqueImporter;
import fr.sirs.importer.objet.*;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
abstract class GenericLigneEauImporter extends GenericObjetImporter<LigneEau> {

    protected final EvenementHydrauliqueImporter evenementHydrauliqueImporter;
    protected final TypeRefHeauImporter typeRefHeauImporter;

}
