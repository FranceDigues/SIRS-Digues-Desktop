/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */

package fr.sirs.util;

import org.geotoolkit.filter.function.AbstractFunctionFactory;

import java.util.HashMap;
import java.util.Map;

public class SirsFunctionFactory
        extends AbstractFunctionFactory {

    private static final Map<String, Class> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.put(DesordreUrgenceLayerFunction.NAME, DesordreUrgenceLayerFunction.class);
        FUNCTIONS.put(LinearIdFilterFunction.NAME, LinearIdFilterFunction.class);
    }

    public SirsFunctionFactory() {
        super("sirsFunction", FUNCTIONS);
    }
}
