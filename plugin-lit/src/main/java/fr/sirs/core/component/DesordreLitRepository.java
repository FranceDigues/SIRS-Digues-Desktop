

package fr.sirs.core.component;


import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;

import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import fr.sirs.core.model.DesordreLit;

import java.util.List;

/**
 * Outil gérant les échanges avec la bdd CouchDB pour tous les objets DesordreLit.
 *
 * @author Estelle Idée (Geomatys)
 */

@Views({
        @View(name = AbstractDesordreRepository.ALL_OPEN_BY_LINEAR_ID, map = "classpath:DesordreLit-by-linearId.js")
})
@Component("fr.sirs.core.component.DesordreLitRepository")
public class DesordreLitRepository extends AbstractDesordreRepository<DesordreLit> {
    @Autowired
    private DesordreLitRepository(CouchDbConnector db) {
        super(DesordreLit.class, db);
        initStandardDesignDocument();
    }

    @Override
    public DesordreLit create() {
        return InjectorCore.getBean(SessionCore.class).getElementCreator().createElement(DesordreLit.class);
    }

    /**
     * get all the desordres for a Troncon that are open (end date null)
     * @param linearId id of the troncon
     * @return the list of the open desordres present on the troncon
     */
    @Override
    public List<DesordreLit> getDesordreOpenByLinearId(final String linearId) {
        ArgumentChecks.ensureNonNull("Desordre Troncon", linearId);
        return this.queryView(ALL_OPEN_BY_LINEAR_ID, linearId);
    }
}