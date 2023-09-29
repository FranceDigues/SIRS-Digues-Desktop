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

import fr.sirs.Injector;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.model.AbstractAmenagementHydraulique;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.OrganeProtectionCollective;
import fr.sirs.core.model.OuvrageAssocieAmenagementHydraulique;
import fr.sirs.core.model.PrestationAmenagementHydraulique;
import fr.sirs.core.model.StructureAmenagementHydraulique;
import java.util.List;
import org.ektorp.CouchDbConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author maximegavens
 */
@Component
public class AmenagementHydrauliqueRepository extends AbstractSIRSRepository<AmenagementHydraulique> {

    @Autowired
    public AmenagementHydrauliqueRepository(CouchDbConnector db) {
        super(AmenagementHydraulique.class, db);
        initStandardDesignDocument();
    }

    @Override
    public AmenagementHydraulique create() {
        return InjectorCore.getBean(SessionCore.class).getElementCreator().createElement(AmenagementHydraulique.class);
    }
    
    /*
    * Complete the removal operation by removing the existing links between other elements,
    * This way guarantees the orphelin status for those elements
    */
    @Override
    public void remove(AmenagementHydraulique amenagement) {
        // Original removing of the element
        super.remove(amenagement);
        final String ID = amenagement.getId();

        // Get all AbstractAmenagementHydraulique subclasses
        final Class[] abstractAh = {
            OrganeProtectionCollective.class,
            DesordreDependance.class,
            PrestationAmenagementHydraulique.class,
            OuvrageAssocieAmenagementHydraulique.class,
            StructureAmenagementHydraulique.class
        };

        // For all, we retrieve all elements that have the current AH id
        // then we remove the link between the two, so that these elements can appear in orphelins list of their pojotable
        for (Class c: abstractAh) {
            final AbstractAmenagementHydrauliqueRepository repo = (AbstractAmenagementHydrauliqueRepository) Injector.getSession().getRepositoryForClass(c);
            List<AbstractAmenagementHydraulique> aahList = repo.getByAmenagementHydrauliqueId(ID);

            aahList.forEach(aah -> aah.setAmenagementHydrauliqueId(null));
            repo.executeBulk(aahList);
        }
    }
    
}
