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

package fr.sirs.core.model;

import java.time.LocalDate;
import java.time.Month;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maximegavens
 */
public class AvecObservationsTest {

    @Test
    public void lastObservationPredicateTest() {
        Observation o1 = new Observation();
        o1.setDate(LocalDate.of(1912, 6, 23));
        Observation o2 = new Observation();
        o2.setDate(LocalDate.of(1955, 2, 24));

        Desordre d1 = new Desordre();
        d1.observations.add(o1);
        d1.observations.add(o2);

        AvecObservations.LastObservationPredicate lop = new AvecObservations.LastObservationPredicate(LocalDate.of(1900, Month.MARCH, 1), LocalDate.of(2000, Month.MARCH, 1));
        AvecObservations.LastObservationPredicate lop2 = new AvecObservations.LastObservationPredicate(LocalDate.of(1900, Month.MARCH, 1), LocalDate.of(1950, Month.MARCH, 1));

        Assert.assertTrue(lop.test(d1));
        Assert.assertFalse(lop2.test(d1));
    }
}
