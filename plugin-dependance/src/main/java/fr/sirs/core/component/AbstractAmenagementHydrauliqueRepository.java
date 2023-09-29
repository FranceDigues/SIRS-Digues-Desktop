/*
 * This file is part of SIRS-Digues 2.
 *
 *  Copyright (C) 2021, FRANCE-DIGUES,
 *
 *  SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any
 *  later version.
 *
 *  SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.core.component;

import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.AbstractAmenagementHydraulique;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author maximegavens
 */
public abstract class AbstractAmenagementHydrauliqueRepository<T extends AbstractAmenagementHydraulique> extends AbstractSIRSRepository<T> {

    public static final String BY_AMENAGEMENT_HYDRAULIQUE_ID = "byAmenagementHydrauliqueId";

    public AbstractAmenagementHydrauliqueRepository(Class<T> type, CouchDbConnector db) {
        super(type, db);
    }

    public List<T> getByAmenagementHydrauliqueId(final String ahId) {
        List<T> result = this.queryView(BY_AMENAGEMENT_HYDRAULIQUE_ID, ahId);
        // if the key is null, couchdb returns all the elements for this class,
        // we don't want it so we operate ourselves the filtering
        if (ahId == null) {
            result.removeIf(aah -> aah.getAmenagementHydrauliqueId() != null);
        }
        return result;
    }

    public List<T> getByAmenagementHydraulique(final AmenagementHydraulique ah) {
        ArgumentChecks.ensureNonNull("Amenagement hydraulique", ah);
        return this.queryView(BY_AMENAGEMENT_HYDRAULIQUE_ID, ah.getId());
    }
}
