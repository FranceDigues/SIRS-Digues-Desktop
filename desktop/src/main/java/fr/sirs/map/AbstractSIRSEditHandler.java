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
import fr.sirs.core.model.AvecGeometrie;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;
import org.geotoolkit.map.FeatureMapLayer;

/**
 * Abstract base Classe for Edition on map of {@link AvecGeometrie} elements in Sirs
 * application. This Classe extends {@link AbstractNavigationHandler}.
 *
 * Warning this constructor doesn't initialize {@link #objetLayer} nor
 * {@link #helper}.It is expected those initializations to be done by the
 * inheriting classes.
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @param <T> edited {@link Class}
 */
public abstract class AbstractSIRSEditHandler<T extends AvecGeometrie> extends AbstractNavigationHandler {

    private final int CROSS_SIZE = 5;

    protected final Class<T> objetClass;
    protected T editedObjet = null;
    protected final ObjectProperty<T> editedObjetProperty;

    protected EditModeObjet mode = EditModeObjet.NONE;
    protected final ObjectProperty<EditModeObjet> modeProperty;

    protected FeatureMapLayer objetLayer = null;
    protected EditionHelper helperObjet;

    protected final EditionHelper.EditionGeometry editGeometry;
    protected final FXGeometryLayer geomLayer;

    /**
     * Warning this constructor doesn't initialize {@link #objetLayer} nor
     * {@link #helper}. It is expected those initializations to be done by the
     * inheriting classes.
     *
     * @param clazz
     */
    public AbstractSIRSEditHandler(final Class<T> clazz) {
        objetClass = clazz;
        editGeometry = new EditionHelper.EditionGeometry();
//        geomLayer = geometrylayer;
        geomLayer = new FXGeometryLayer() {
            @Override
            protected Node createVerticeNode(Coordinate c, boolean selected) {
                final Line h = new Line(c.x - CROSS_SIZE, c.y, c.x + CROSS_SIZE, c.y);
                final Line v = new Line(c.x, c.y - CROSS_SIZE, c.x, c.y + CROSS_SIZE);
                h.setStroke(Color.RED);
                v.setStroke(Color.RED);
                return new Group(h, v);
            }
        };
        editedObjetProperty = new SimpleObjectProperty<>(editedObjet);
        modeProperty = new SimpleObjectProperty<>(mode);

    }

    protected ObjectProperty<T> getEditedObjetProperty() {
        return editedObjetProperty;
    }
    protected ObjectProperty<EditModeObjet> getModeProperty() {
        return modeProperty;
    }

    public EditionHelper.EditionGeometry getEditionGeometry() {
        return editGeometry;
    }

    /**
     * Return {@link #helperObjet}. If null, try to initialize it using {@link EditionHelper#EditionHelper(org.geotoolkit.gui.javafx.render2d.FXMap, org.geotoolkit.map.FeatureMapLayer)
     * }
     *
     * @return
     */
    public EditionHelper getHelperObjet() {
        if (helperObjet == null) {
            helperObjet = new EditionHelper(map, objetLayer);
            helperObjet.setMousePointerSize(6);
        }
        return helperObjet;
    }

    public FXGeometryLayer getGeometryLayer() {
        return geomLayer;
    }

    public T getEditedObjet() {
        return editedObjet;
    }

    protected abstract FXPanMouseListen getMouseInputListener();

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap component) {
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, getMouseInputListener());
        component.addEventHandler(ScrollEvent.ANY, getMouseInputListener());
        map.setCursor(Cursor.CROSSHAIR);
        map.addDecoration(0, getGeometryLayer());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean uninstall(final FXMap component) {
            super.uninstall(component);
            component.removeDecoration(getGeometryLayer());
            component.removeEventHandler(MouseEvent.ANY, getMouseInputListener());
            component.removeEventHandler(ScrollEvent.ANY, getMouseInputListener());
            component.setBottom(null);

            if (objetLayer != null) {
                objetLayer.setSelectionFilter(null);
            }

            return true;
    }

    protected void updateGeometry() {
        if (editedObjet == null) {
            editGeometry.reset();
        } else {
            editGeometry.geometry.set(editedObjet.getGeometry());
        }

        if (editGeometry.geometry == null) {
            geomLayer.getGeometries().clear();
        } else {
            geomLayer.getGeometries().setAll(editGeometry.geometry.get());
        }
    }

}
