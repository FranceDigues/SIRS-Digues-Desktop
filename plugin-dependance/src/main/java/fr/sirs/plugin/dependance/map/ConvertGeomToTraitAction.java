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
package fr.sirs.plugin.dependance.map;

import fr.sirs.Injector;
import fr.sirs.Session;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import org.geotoolkit.gui.javafx.render2d.FXCanvasHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXMapAction;

/**
 * Copy of the class ConvertGeomToTraitAction from the module Berge
 *
 * @author Maxime Gavens (Geomatys)
 */
public class ConvertGeomToTraitAction extends FXMapAction {
        
    public ConvertGeomToTraitAction(FXMap map) {
        super(map,"Géometrie vers Trait d'aménagement hydraulique","Convertir une géométrie en trait d'aménagement hydraulique",new Image("/fr/sirs/plugin/dependance/geometrietraitamenagementhydraulique.png"));

        final Session session = Injector.getSession();
        this.disabledProperty().bind(session.geometryEditionProperty().not());
        
        map.getHandlerProperty().addListener(new ChangeListener<FXCanvasHandler>() {
            @Override
            public void changed(ObservableValue<? extends FXCanvasHandler> observable, FXCanvasHandler oldValue, FXCanvasHandler newValue) {
                selectedProperty().set(newValue instanceof ConvertGeomToTraitHandler);
            }
        });
    }

    @Override
    public void accept(ActionEvent event) {
        if (map != null) {
            map.setHandler(new ConvertGeomToTraitHandler(map));
        }
    }
    
}
