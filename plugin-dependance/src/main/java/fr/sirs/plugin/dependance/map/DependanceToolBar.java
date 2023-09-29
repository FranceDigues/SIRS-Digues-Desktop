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

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import org.controlsfx.control.action.ActionUtils;
import org.geotoolkit.gui.javafx.render2d.FXMap;

/**
 * @author Cédric Briançon (Geomatys)
 * @author Maxime Gavens (Geomatys)
 */
public class DependanceToolBar extends ToolBar {
    private static final String LEFT = "buttongroup-left";
    private static final String CENTER = "buttongroup-center";
    private static final String RIGHT = "buttongroup-right";

    public DependanceToolBar(final FXMap map) {
        getStylesheets().add("/org/geotoolkit/gui/javafx/buttonbar.css");

        getItems().add(new Label("Dépendance et AH"));

        final ToggleButton buttonEdit = new DependanceEditAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        buttonEdit.getStyleClass().add(LEFT);

        final ToggleButton buttonTransform = new DependanceTransformAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        buttonTransform.getStyleClass().add(CENTER);

        final ToggleButton buttonCreateDesorder = new AbstractAmenagementHydrauliqueEditAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        buttonCreateDesorder.getStyleClass().add(CENTER);

        final ToggleButton butEditTrait = new TraitAmenagementHydrauliqueEditAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        butEditTrait.setMaxHeight(Double.MAX_VALUE);
        butEditTrait.getStyleClass().add(CENTER);

        final Button importTrait = new ConvertGeomToTraitAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        importTrait.setMaxHeight(Double.MAX_VALUE);
        importTrait.getStyleClass().add(RIGHT);

        getItems().add(new HBox(buttonEdit, buttonTransform, buttonCreateDesorder, butEditTrait, importTrait));
    }
}
