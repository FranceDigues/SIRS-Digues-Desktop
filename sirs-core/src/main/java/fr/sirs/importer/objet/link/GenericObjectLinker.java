package fr.sirs.importer.objet.link;

import com.healthmarketscience.jackcess.Database;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.GenericLinker;
import java.io.IOException;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
abstract class GenericObjectLinker extends GenericLinker {

    public GenericObjectLinker(Database accessDatabase, CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }
    
    @Override
    public void link() throws IOException, AccessDbImporterException{
        compute();
    }
}
