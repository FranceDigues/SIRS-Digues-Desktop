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

package fr.sirs.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.model.AvecGeometrie;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;

/**
 *
 * Mouse listener pour l'édition de géométrie depuis la carte.
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @param <G>
 */
public abstract class AbstractSIRSEditMouseListen<G extends AvecGeometrie> extends FXPanMouseListen {


    protected double startX;
    protected double startY;

    double initialX;
    double initialY;

    protected final ContextMenu popup = new ContextMenu();
    protected MouseButton pressed;
    protected final AbstractSIRSEditHandler editHandler;
    protected final FXMap map;
    protected final EditionHelper.EditionGeometry editGeometry;
    protected final FXGeometryLayer geomLayer;
    protected final Class<G> editedClass;
    protected G editedObjet = null;
    protected final SimpleObjectProperty<G> editedObjetProperty = new SimpleObjectProperty<>(editedObjet);
    protected final ObjectProperty<EditModeObjet> modeProperty = new SimpleObjectProperty<>(EditModeObjet.NONE);

    protected EditionHelper objetHelper;

    protected final List<Coordinate> coords = new ArrayList<>();

    /**
     * Définit le type de géométries à dessiner, pour les dépendances de types
     * "ouvrages de voirie" ou "autres" pour lesquelles plusieurs choix sont
     * possibles.
     */
    protected Class newGeomType = Point.class;

    /**
     * Vrai si la {@linkplain #coords liste des coordonnées} de la
     * {@linkplain #editGeometry géométrie} vient d'être créée.
     */
    protected boolean justCreated = false;

    public AbstractSIRSEditMouseListen(final AbstractSIRSEditHandler sirsEditHandler) {
        super(sirsEditHandler);
        editedClass = sirsEditHandler.objetClass;
        editHandler = sirsEditHandler;
        map = editHandler.getMap();
        editGeometry = editHandler.getEditionGeometry();
        geomLayer = editHandler.getGeometryLayer();

        editedObjetProperty.bindBidirectional(editHandler.getEditedObjetProperty());
        modeProperty.bindBidirectional(editHandler.getModeProperty());

        objetHelper = editHandler.helperObjet;

    }

    /**
     * Réinitialise la carte et vide la géométrie en cours d'édition.
     */
    void reset() {
        justCreated = false;
        geomLayer.getGeometries().clear();
        geomLayer.setNodeSelection(null);
        coords.clear();
        editGeometry.reset();
        editedObjet = null;
    }

}
