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
package fr.sirs.plugin.dependance.map;

import fr.sirs.Injector;
import javafx.event.ActionEvent;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXMapAction;
import org.geotoolkit.internal.GeotkFX;

/**
 * @author Cédric Briançon (Geomatys)
 */
public class DependanceEditAction extends FXMapAction {
    public DependanceEditAction(FXMap map) {
        super(map,"Dépendance ou AH","Création / Modification de dépendance ou AH", GeotkFX.ICON_EDIT);

        this.disabledProperty().bind(Injector.getSession().geometryEditionProperty().not());

        map.getHandlerProperty().addListener((observable, oldValue, newValue) -> {
            selectedProperty().set(newValue instanceof DependanceEditHandler);
        });
    }

    @Override
    public void accept(ActionEvent event) {
        if (map != null && !(map.getHandler() instanceof DependanceEditHandler)) {
            map.setHandler(new DependanceEditHandler());
        }
    }
}
