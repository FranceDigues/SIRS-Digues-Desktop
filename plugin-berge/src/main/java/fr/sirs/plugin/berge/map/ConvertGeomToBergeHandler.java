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
package fr.sirs.plugin.berge.map;

import fr.sirs.core.model.Berge;
import fr.sirs.map.ConvertGeomToTronconHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;

/**
 *
 * @author guilhem
 */
public class ConvertGeomToBergeHandler extends ConvertGeomToTronconHandler {
    
    public ConvertGeomToBergeHandler(FXMap map) {
        super(map);
    }
    
    protected void init() {
        this.typeClass = Berge.class;
        this.typeName = "berge";
        this.maleGender = false;
        this.parentClass = null; // should not be used
        this.parentLabel = ""; // should not be used
    }
}
