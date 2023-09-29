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
package fr.sirs.util;

import fr.sirs.core.model.AmenagementHydrauliqueView;
import fr.sirs.core.model.TronconDigue;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @author Maxime Gavens (Geomatys)
 */
public class SirsStringConverter_Test {

    /**
     * Test de la méthode toString(Object item, final boolean prefixed, final boolean suffixed)
     * 
     * 
     */
    @Test
    public void toString_Test() {
        SirsStringConverter converter = new SirsStringConverter();
        
        TronconDigue testedElement = new TronconDigue();
        testedElement.setDesignation("27");
        testedElement.setLibelle("Isère RD du pont RN90 (P 549) à amont pont de Pique Pierre (P 610)");
        
        //prefixed = true et suffixed = true
        //Résultat attendu de la forme : "Abstract : nom Complet" 
        Assert.assertEquals("TrD - 27 : Isère RD du pont RN90 (P 549) à amont pont de Pique Pierre (P 610)", converter.toString(testedElement, true, true));
        
        
        //prefixed = true et suffixed = false
        //Résultat attendu de la forme : "Abstract" 
        Assert.assertEquals("TrD - 27", converter.toString(testedElement, true, false));
        
        //prefixed = false et suffixed = true
        //Résultat attendu de la forme : "nom Complet" 
        Assert.assertEquals("Isère RD du pont RN90 (P 549) à amont pont de Pique Pierre (P 610)", converter.toString(testedElement, false, true));
    }

    @Test
    public void toStringAhTest() {
        final SirsStringConverter converter = new SirsStringConverter();

        final AmenagementHydrauliqueView ahView = new AmenagementHydrauliqueView();
        ahView.setDesignation("ahViewDesignation1");
        final String toString = converter.toString(ahView, true, true);
        final String toString2 = converter.toString(ahView, true, false);
        final String toString3 = converter.toString(ahView, false, true);

        Assert.assertEquals("AH - ahViewDesignation1", toString);
        Assert.assertEquals("AH - ahViewDesignation1", toString2);
        Assert.assertEquals("AH - ahViewDesignation1", toString3);


        final AmenagementHydrauliqueView ahView2 = new AmenagementHydrauliqueView();
        final String toString4 = converter.toString(ahView2, true, true);
        final String toString5 = converter.toString(ahView2, true, false);
        final String toString6 = converter.toString(ahView2, false, true);

        Assert.assertEquals("AH", toString4);
        Assert.assertEquals("AH", toString5);
        Assert.assertEquals("AH", toString6);
    }
}
