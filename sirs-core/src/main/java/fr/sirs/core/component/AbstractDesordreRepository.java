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

import fr.sirs.core.model.Positionable;
import org.ektorp.CouchDbConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outil gérant les échanges avec la bdd CouchDB pour tous les objets Desordre.
 * @author Estelle Idee (Geomatys)
 */

public abstract class AbstractDesordreRepository<T extends Positionable> extends
        AbstractPositionableRepository<T> {
    public static final String ALL_OPEN_BY_LINEAR_ID = "allOpenByLinearId";
    @Autowired
    protected AbstractDesordreRepository(Class pojoClass, CouchDbConnector db) {
        super(pojoClass, db);
        initStandardDesignDocument();
    }

    /**
     * get all the desordres for a Troncon that are open (end date null)
     * @param linearId id of the troncon
     * @return the list of the open desordres present on the troncon
     */
    public abstract List<T> getDesordreOpenByLinearId(final String linearId);

}