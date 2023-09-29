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
import static fr.sirs.core.component.AbstractAmenagementHydrauliqueRepository.BY_AMENAGEMENT_HYDRAULIQUE_ID;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.TraitAmenagementHydraulique;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Outil gérant les échanges avec la bdd CouchDB pour tous les objets TraitAmenagementHydraulique.
 *
 * @author Maxime Gavens (Geomatys)
 */
@View(name=BY_AMENAGEMENT_HYDRAULIQUE_ID, map="function(doc) {if(doc['@class']=='fr.sirs.core.model.TraitAmenagementHydraulique') {emit(doc.amenagementHydrauliqueId, doc._id)}}")
@Component("fr.sirs.core.component.TraitAmenagementHydrauliqueRepository")
public class TraitAmenagementHydrauliqueRepository extends
AbstractSIRSRepository
<TraitAmenagementHydraulique> {

    @Autowired
    private TraitAmenagementHydrauliqueRepository ( CouchDbConnector db) {
       super(TraitAmenagementHydraulique.class, db);
       initStandardDesignDocument();
   }
    
    @Override
    public TraitAmenagementHydraulique create() {
        return InjectorCore.getBean(SessionCore.class).getElementCreator().createElement(TraitAmenagementHydraulique.class);
    }

    public List<TraitAmenagementHydraulique> getByAmenagementHydrauliqueId(final String planId) {
        ArgumentChecks.ensureNonNull("Amenagement hydraulique id", planId);
        return this.queryView(BY_AMENAGEMENT_HYDRAULIQUE_ID, planId);
    }

    public List<TraitAmenagementHydraulique> getByAmenagementHydraulique(final AmenagementHydraulique amenagement) {
        ArgumentChecks.ensureNonNull("Amenagement hydraulique", amenagement);
        return getByAmenagementHydrauliqueId(amenagement.getId());
    }
}

