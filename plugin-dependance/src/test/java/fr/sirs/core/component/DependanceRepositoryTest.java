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

import fr.sirs.core.CouchDBTestCase;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.core.model.PrestationAmenagementHydraulique;
import java.util.List;
import org.ektorp.DocumentNotFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author maximegavens
 */
public class DependanceRepositoryTest extends CouchDBTestCase {

    @Autowired
    PrestationAmenagementHydrauliqueRepository prestationRepository;

    @Test
    public void crudAndViewTest() {

        // Create
        final PrestationAmenagementHydraulique prestation = ElementCreator.createAnonymValidElement(PrestationAmenagementHydraulique.class);
        prestation.setLibelle("libellePrestation1");
        prestation.setDesignation("designationPrestation1");
        prestation.setAuthor("maxime");
        prestation.setCoutGlobal(10000);
        prestation.setAmenagementHydrauliqueId("1234567890");
        prestationRepository.add(prestation);

        // Read
        final PrestationAmenagementHydraulique get = prestationRepository.get(prestation.getId());
        Assert.assertEquals(prestation, get);
        Assert.assertEquals(10000, get.getCoutGlobal(), 1E-6);

        // Update
        get.setCoutGlobal(50000);
        prestationRepository.update(get);
        final PrestationAmenagementHydraulique get2 = prestationRepository.get(prestation.getId());
        Assert.assertEquals("libellePrestation1", get2.getLibelle());
        Assert.assertEquals(50000, get2.getCoutGlobal(), 1E-6);

        // View
        final List<PrestationAmenagementHydraulique> oneResult = prestationRepository.getByAmenagementHydrauliqueId("1234567890");
        Assert.assertEquals(1, oneResult.size());
        Assert.assertEquals(prestation, oneResult.get(0));

        // Delete
        prestationRepository.remove(get2);
        try {
            prestationRepository.get(get2.getId());
            Assert.assertTrue(false);
        } catch (DocumentNotFoundException ex) {
            Assert.assertTrue(true);
        }
    }
}
