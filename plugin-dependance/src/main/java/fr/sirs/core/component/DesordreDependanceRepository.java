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

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.model.DesordreDependance;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static fr.sirs.core.component.DesordreDependanceRepository.*;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
@Views({
    @View(name=BY_AMENAGEMENT_HYDRAULIQUE_ID, map="function(doc) {if(doc['@class']=='fr.sirs.core.model.DesordreDependance') {emit(doc.amenagementHydrauliqueId, doc._id)}}"),
    @View(name=BY_DEPENDANCE_ID, map="function(doc) {if(doc['@class']=='fr.sirs.core.model.DesordreDependance') {emit(doc.dependanceId, doc._id)}}"),
        @View(name = ALL_OPEN_BY_AH_ID, map = "classpath:DesordreDependance-by-ah-id.js")
})
@Component("fr.sirs.core.component.DesordreDependanceRepository")
public class DesordreDependanceRepository extends AbstractAmenagementHydrauliqueRepository<DesordreDependance> {

    public static final String BY_DEPENDANCE_ID = "byDependanceId";
    public static final String ALL_OPEN_BY_AH_ID = "allOpenByAHId";

    @Autowired
    private DesordreDependanceRepository ( CouchDbConnector db) {
       super(DesordreDependance.class, db);
       initStandardDesignDocument();
   }

    @Override
    public DesordreDependance create() {
        return InjectorCore.getBean(SessionCore.class).getElementCreator().createElement(DesordreDependance.class);
    }

    public List<DesordreDependance> getByDependanceId(final String depId) {
        List<DesordreDependance> result = this.queryView(BY_DEPENDANCE_ID, depId);
        // if the key is null, couchdb returns all the elements for this class,
        // we don't want it so we operate ourselves the filtering
        if (depId == null) {
            result.removeIf(aah -> aah.getDependanceId() != null);
        }
        return result;
    }

    public List<DesordreDependance> getByDependanceOrAHId(final String id) {
        if (id == null) {
            final List<DesordreDependance> all = getAll();
            all.removeIf(dd -> dd.getDependanceId() != null || dd.getAmenagementHydrauliqueId() != null);
            return all;
        }
        final List<DesordreDependance> byAmenagementHydrauliqueId = getByAmenagementHydrauliqueId(id);
        final List<DesordreDependance> byDependanceId = getByDependanceId(id);

        // to optimize
        if (byAmenagementHydrauliqueId.isEmpty()) return byDependanceId;
        if (byDependanceId.isEmpty()) return byAmenagementHydrauliqueId;
        byAmenagementHydrauliqueId.addAll(byDependanceId);
        return byAmenagementHydrauliqueId;
    }

    public List<DesordreDependance> getDesordreOpenByLinearId(final String ahId) {
        ArgumentChecks.ensureNonNull("DesordreDependance AH", ahId);
        return this.queryView(ALL_OPEN_BY_AH_ID, ahId);
    }
}