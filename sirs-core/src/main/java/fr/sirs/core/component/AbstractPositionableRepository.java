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
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.StreamingIterable;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;

/**
 * A repository to access the view giving positionable objects by {@link TronconDigue}.
 * @author Samuel Andrés (Geomatys)
 * @param <T> Type of object managed by this repository.
 */
public abstract class AbstractPositionableRepository<T extends Positionable> extends AbstractSIRSRepository<T> {
    
    public AbstractPositionableRepository(Class<T> type, CouchDbConnector db) {
        super(type, db);
    }

    public List<T> getByLinear(final TronconDigue linear) {
        ArgumentChecks.ensureNonNull("Linear", linear);
        return this.getByLinearId(linear.getId());
    }

    public List<T> getByLinearId(final String linearId) {
        ArgumentChecks.ensureNonNull("Linear", linearId);
        return cacheList(globalRepo.getByLinearId(type, linearId));
    }

    public StreamingIterable<T> getByLinearStreaming(final TronconDigue linear) {
        ArgumentChecks.ensureNonNull("Linear", linear);
        return this.getByLinearIdStreaming(linear.getId());
    }

    public StreamingIterable<T> getByLinearIdStreaming(final String linearId) {
        ArgumentChecks.ensureNonNull("Linear", linearId);
        return new StreamingViewIterable(globalRepo.createByLinearIdQuery(type, linearId));
    }
}
