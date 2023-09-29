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

import java.util.Comparator;

/**
 * This custom comparator is used to compare String that contains Double value.
 * With these following rules:
 *  - A string that represents a Double is always superior to a string which does not represent one
 *  - Use classic comparator in the case of two String or two Double
 *
 * @author maximegavens
 */
public class AlphaNumComparator implements Comparator<String> {
    @Override
    public int compare(String s1, String s2) {
        if (s1 == s2) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        try {
            final Double d1 = Double.parseDouble(s1);
            try {
                final Double d2 = Double.parseDouble(s2);
                return d1.compareTo(d2);
            } catch (NumberFormatException ex) {
                return 1;
            }
        } catch (NumberFormatException ex) {
            try {
                final double d2 = Double.parseDouble(s2);
                return -1;
            } catch (NumberFormatException ex2) {
                return s1.compareTo(s2);
            }
        }
    }
}
