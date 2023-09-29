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

package fr.sirs.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author maximegavens
 */
public class AlphaNumComparatorTest {
    
    @Test
    public void testComparator() {
        AlphaNumComparator comparator = new AlphaNumComparator();
        
        // equal
        assertEquals(comparator.compare(null, null), 0);
        assertEquals(comparator.compare("3", "3"), 0);
        assertEquals(comparator.compare("0.2", "0.2"), 0);
        assertEquals(comparator.compare("test", "test"), 0);
        
        // Superior
        assertEquals(comparator.compare("0.1", null), 1);
        assertEquals(comparator.compare("test", null), 1);
        assertEquals(comparator.compare("2", "1"), 1);
        assertEquals(comparator.compare("0.1", "0.001"), 1);
        assertEquals(comparator.compare("12", "test"), 1);
        assertEquals(comparator.compare("34.43", "test"), 1);
        assertTrue(comparator.compare("b", "az") >= 1);
        
        // Inferior
        assertEquals(comparator.compare(null, "0.1"), -1);
        assertEquals(comparator.compare(null, "test"), -1);
        assertEquals(comparator.compare("1", "2"), -1);
        assertEquals(comparator.compare("0.3", "12.34567"), -1);
        assertEquals(comparator.compare("test", "12"), -1);
        assertEquals(comparator.compare("test", "12.34567"), -1);
        assertTrue(comparator.compare("a", "helloworldhelloworldhelloworldhelloworldhelloworld") <= -1);
    }
}
