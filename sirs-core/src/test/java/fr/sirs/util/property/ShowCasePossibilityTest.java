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

package fr.sirs.util.property;

import java.util.ArrayList;
import javafx.util.StringConverter;
import org.apache.sis.util.NullArgumentException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class ShowCasePossibilityTest {

    final private String abstractString = "Abrégé";
    final private String fullNameString = "Nom Complet";
    final private String bothString = "Abrégé : Nom Complet";

    final private ArrayList<String> testedList = new ArrayList();

    @Before
    public void before() {
        testedList.add("Mauvais nom");
        testedList.add("");
        testedList.add(null);
    }

    @Test
    public void getFromName_Test() {
        //Tests
        Assert.assertEquals(ShowCasePossibility.ABSTRACT, ShowCasePossibility.getFromName(abstractString));
        Assert.assertEquals(ShowCasePossibility.FULL_NAME, ShowCasePossibility.getFromName(fullNameString));
        Assert.assertEquals(ShowCasePossibility.BOTH, ShowCasePossibility.getFromName(bothString));

        int val0 = 0;

        //Tests des exceptions
        for (String testedString : testedList) {
            try {
                ShowCasePossibility.getFromName(testedString);
                val0++;
            } catch (IllegalArgumentException iae) {
                Assert.assertNotNull(testedString);
                Assert.assertEquals(testedString + " n'est pas un nom valide pour l'énum ShowCase_Possibility", iae.getMessage());
            } catch (NullArgumentException ne) {
                Assert.assertNull(testedString);
                final String message = ne.getMessage();
                Assert.assertTrue(((message!=null) && (message.contains("searchedString"))));
            }
        }

        // On vérifie que val0 n'a pas été incrémenté :
        Assert.assertEquals(0, val0);
    }

    @Test
    public void getConverter_Test() {
        StringConverter<ShowCasePossibility> converter = ShowCasePossibility.getConverter();

        //====================
        //Tests fromString() :
        //====================
        Assert.assertEquals(ShowCasePossibility.ABSTRACT, converter.fromString(abstractString));
        Assert.assertEquals(ShowCasePossibility.FULL_NAME, converter.fromString(fullNameString));
        Assert.assertEquals(ShowCasePossibility.BOTH, converter.fromString(bothString));


        // Test de la valeur par défaut :
        testedList.forEach(wrongString ->  Assert.assertEquals(ShowCasePossibility.BOTH, converter.fromString(wrongString)));


        //====================
        //Tests toString() :
        //====================

        Assert.assertEquals(abstractString, converter.toString(ShowCasePossibility.ABSTRACT));
        Assert.assertEquals(fullNameString, converter.toString(ShowCasePossibility.FULL_NAME));
        Assert.assertEquals(bothString, converter.toString(ShowCasePossibility.BOTH));

    }

}
