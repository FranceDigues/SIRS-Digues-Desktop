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

import static fr.sirs.core.component.ReferenceUsageRepository.USAGES;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;

import fr.sirs.core.model.ReferenceUsage;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.ViewQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@View(name = USAGES, map="classpath:ReferenceUsages-map.js")
public class ReferenceUsageRepository extends
        CouchDbRepositorySupport<ReferenceUsage> {

    public static final String USAGES = "usages";

    @Autowired
    public ReferenceUsageRepository(CouchDbConnector couchDbConnector) {
        super(ReferenceUsage.class, couchDbConnector);
        initStandardDesignDocument();
    }

    public List<ReferenceUsage> getReferenceUsages(final String referenceId){
        ArgumentChecks.ensureNonNull("Reference id", referenceId);
        final ViewQuery viewQuery = createQuery(USAGES).includeDocs(false).key(referenceId);
        final List<ReferenceUsage> usages = db.queryView(viewQuery, ReferenceUsage.class);
        return usages;
    }

    public List<ReferenceUsage> getReferenceUsages(){
        final ViewQuery viewQuery = createQuery(USAGES).includeDocs(false);
        final List<ReferenceUsage> usages = db.queryView(viewQuery, ReferenceUsage.class);
        return usages;
    }
}
