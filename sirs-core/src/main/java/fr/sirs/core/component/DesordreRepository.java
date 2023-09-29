/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.core.component;

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.model.Desordre;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outil gérant les échanges avec la bdd CouchDB pour tous les objets Desordre.
 * @author Estelle Idee (Geomatys)
 */
@Views({
        @View(name = AbstractDesordreRepository.ALL_OPEN_BY_LINEAR_ID, map = "classpath:Desordre-by-linearId.js")
})
@Component("fr.sirs.core.component.DesordreRepository")
public class DesordreRepository extends AbstractDesordreRepository<Desordre> {
    @Autowired
    private DesordreRepository (CouchDbConnector db) {
        super(Desordre.class, db);
        initStandardDesignDocument();
    }

    @Override
    public Desordre create() {
        return InjectorCore.getBean(SessionCore.class).getElementCreator().createElement(Desordre.class);
    }

    /**
     * get all the desordres for a Troncon that are open (end date null)
     * @param linearId id of the troncon
     * @return the list of the open desordres present on the troncon
     */
    @Override
    public List<Desordre> getDesordreOpenByLinearId(final String linearId) {
        ArgumentChecks.ensureNonNull("Desordre Troncon", linearId);
        return this.queryView(ALL_OPEN_BY_LINEAR_ID, linearId);
    }
}