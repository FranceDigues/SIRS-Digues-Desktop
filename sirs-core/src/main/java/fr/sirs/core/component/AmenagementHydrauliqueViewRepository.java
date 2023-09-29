/**
 *
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

import static fr.sirs.core.component.AmenagementHydrauliqueViewRepository.AH_VIEW_QUERY;
import fr.sirs.core.model.AmenagementHydrauliqueView;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author maximegavens
 */
@Component
@View(name=AH_VIEW_QUERY, map="classpath:Amenagement-hydraulique-view-map.js")
public class AmenagementHydrauliqueViewRepository extends CouchDbRepositorySupport<AmenagementHydrauliqueView> {

    public static final String AH_VIEW_QUERY = "amenagement_hydraulique_specific_view";

    @Autowired
    public AmenagementHydrauliqueViewRepository(CouchDbConnector couchDbConnector) {
        super(AmenagementHydrauliqueView.class, couchDbConnector);
        initStandardDesignDocument();
    }

    public List<AmenagementHydrauliqueView> getAmenagementHydrauliqueView(final String ahId) {
        ArgumentChecks.ensureNonNull("Amenagement hydraulique id", ahId);
        final ViewQuery viewQuery = createQuery(AH_VIEW_QUERY).includeDocs(false).key(ahId);
        return db.queryView(viewQuery, AmenagementHydrauliqueView.class);
    }

    public List<AmenagementHydrauliqueView> getAmenagementHydrauliqueView() {
        final ViewQuery viewQuery = createQuery(AH_VIEW_QUERY).includeDocs(false);
        return db.queryView(viewQuery, AmenagementHydrauliqueView.class);
    }
    
}
