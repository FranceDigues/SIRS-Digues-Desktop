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
package fr.sirs.plugin.vegetation.map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.PositionableVegetation;
import fr.sirs.core.model.ZoneVegetation;
import fr.sirs.theme.ui.FXPositionableExplicitMode;
import fr.sirs.util.ResourceInternationalString;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionTool;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionToolSpi;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.edition.EditionTool;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static fr.sirs.SIRS.CSS_PATH;

/**
 * Outil d'édition de vegetation existante.
 *
 * @author Johann Sorel (Geomatys)
 */
public class EditVegetationTool extends AbstractEditionTool {

    public static final Spi SPI = new Spi();

    public static final class Spi extends AbstractEditionToolSpi {

        public Spi() {
            super("EditVegetation",
                    new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                            "fr.sirs.plugin.vegetation.map.EditVegetationTool.title", EditVegetationTool.class.getClassLoader()),
                    new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                            "fr.sirs.plugin.vegetation.map.EditVegetationTool.abstract", EditVegetationTool.class.getClassLoader()),
                    SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_PENCIL, 24, new Color(15, 112, 183)), null)
            );
        }

        @Override
        public boolean canHandle(Object candidate) {
            return true;
        }

        @Override
        public EditionTool create(FXMap map, Object layer) {
            return new EditVegetationTool(map);
        }
    }

    //session and repo
    private final MouseListen mouseInputListener = new MouseListen();
    private final BorderPane wizard = new BorderPane();
    private final FXPositionableForm form = new FXPositionableForm();

    //geometry en cours
    private EditionHelper helper;
    private final FXGeometryLayer decoration = new FXGeometryLayer();
    private final EditionHelper.EditionGeometry selection = new EditionHelper.EditionGeometry();
    private final Label lblMessage = new Label("Sélectionner une zone de végétation sur la carte.\n"
            + "La couche de données doit être dévérouillée pour être sélectionnable.");
    private boolean modified = false;
    private MouseButton pressed = null;

    private final Button uiClose = new Button("Fermer");

    public EditVegetationTool(FXMap map) {
        super(SPI);
        wizard.getStylesheets().add(CSS_PATH);
        wizard.setCenter(lblMessage);
        wizard.getStyleClass().add("blue-light");
        lblMessage.getStyleClass().add("label-header");
        lblMessage.setWrapText(true);

        //refresh geometry on change
        selection.geometry.addListener((ObservableValue<? extends Geometry> observable, Geometry oldValue, Geometry newValue) -> refreshDecoration());

        form.positionableProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                selection.geometry.unbindBidirectional(oldValue.geometryProperty());
                selection.geometry.set(null);
            }
            if (newValue != null) {
                wizard.setCenter(form);
                selection.geometry.bindBidirectional(newValue.geometryProperty());
            } else {
                reset();
            }
            refreshDecoration();
        });

        uiClose.setOnAction(event -> install(map));
        uiClose.visibleProperty().bind(form.positionableProperty().isNotNull());
        uiClose.managedProperty().bind(uiClose.visibleProperty());
        wizard.setTop(uiClose);
        wizard.setAlignment(uiClose, Pos.CENTER_RIGHT);
        uiClose.getStyleClass().add("btn-single");
    }


    private void reset() {
        pressed = null;
        selection.geometry.unbind();
        if (form.positionableProperty().get() != null) {
            selection.geometry.unbindBidirectional(form.positionableProperty().get().geometryProperty());
        }
        selection.reset();
        modified = false;
        decoration.getGeometries().clear();
        decoration.setNodeSelection(null);
        wizard.setCenter(lblMessage);
    }

    private void refreshDecoration() {
        decoration.getGeometries().setAll(this.selection.geometry.get());
        decoration.setNodeSelection(this.selection);
    }

    @Override
    public Node getConfigurationPane() {
        return wizard;
    }

    @Override
    public Node getHelpPane() {
        return null;
    }

    @Override
    public void install(FXMap component) {
        reset();
        form.positionableProperty().set(null);
        helper = null;
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        component.addEventHandler(KeyEvent.ANY, mouseInputListener);
        component.setCursor(Cursor.CROSSHAIR);
        component.addDecoration(decoration);
    }

    @Override
    public boolean uninstall(FXMap component) {
        super.uninstall(component);
        component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
        component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
        component.removeEventHandler(KeyEvent.ANY, mouseInputListener);
        component.setCursor(Cursor.DEFAULT);
        component.removeDecoration(decoration);
        reset();
        form.positionableProperty().set(null);
        helper = null;
        return true;
    }

    private class MouseListen extends FXPanMouseListen {

        private final ContextMenu menu = new ContextMenu();

        public MouseListen() {
            super(EditVegetationTool.this);
            menu.setAutoHide(true);
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            super.mouseEntered(me);
            ((Node) me.getSource()).requestFocus();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            menu.hide();

            final MouseButton button = e.getButton();

            if (button == MouseButton.PRIMARY) {
                if (form.positionableProperty().get() == null) {
                    final Rectangle2D clickArea = new Rectangle2D.Double(e.getX() - 2, e.getY() - 2, 4, 4);

                    //recherche d'un object a editer
                    map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                        @Override
                        public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                            final Object bean = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                            if (bean instanceof PositionableVegetation) {
                                form.positionableProperty().set((PositionableVegetation) bean);
                                helper = new EditionHelper(map, graphic.getLayer());
                            }
                        }

                        @Override
                        public boolean isStopRequested() {
                            return form.positionableProperty().get() != null;
                        }

                        @Override
                        public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {
                        }
                    }, VisitFilter.INTERSECTS);

                } else if (e.getClickCount() >= 2 && helper != null) {
                    //double click = add a node
                    final Geometry result;
                    final Positionable pos = form.positionableProperty().get();
                    final Geometry geom = pos.geometryProperty().get();
                    if (geom instanceof LineString) {
                        result = helper.insertNode((LineString) geom, e.getX(), e.getY());
                    } else if (geom instanceof Polygon) {
                        result = helper.insertNode((Polygon) geom, e.getX(), e.getY());
                    } else if (geom instanceof GeometryCollection) {
                        result = helper.insertNode((GeometryCollection) geom, e.getX(), e.getY());
                    } else {
                        result = geom;
                    }
                    modified = modified || result != geom;
                    pos.setGeometryMode(FXPositionableExplicitMode.MODE);
                    form.setCartoEditedMode((ZoneVegetation) pos);
                    pos.geometryProperty().set(result);
                    refreshDecoration();
                } else if (e.getClickCount() == 1 && helper != null) {
                    //single click with a geometry = select a node
                    helper.grabGeometryNode(e.getX(), e.getY(), selection);
                    refreshDecoration();
                }
            } else if (button == MouseButton.SECONDARY) {

                if (selection.geometry.get() == null) return;
                if (selection.numSubGeom < 0) return;
                if (selection.selectedNode[0] < 0) return;

                final MenuItem item = new MenuItem("Supprimer le noeud sélectionné");
                item.setOnAction((ActionEvent event) -> {
                    selection.deleteSelectedNode();
                    refreshDecoration();
                    modified = true;
                });
                menu.getItems().setAll(item);
                menu.show(getMap(), e.getScreenX(), e.getScreenY());
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            pressed = e.getButton();

            if (pressed == MouseButton.PRIMARY) {
                final Positionable pos = form.positionableProperty().get();
                final Geometry geom = pos == null ? null : pos.geometryProperty().get();
                if (geom != null && e.getClickCount() == 1) {
                    //single click with a geometry = select a node
                    helper.grabGeometryNode(e.getX(), e.getY(), selection);
                    refreshDecoration();
                }
            }

            super.mousePressed(e);
        }

        @Override
        public void mouseDragged(final MouseEvent e) {

            if (pressed == MouseButton.PRIMARY && helper != null) {
                //dragging node
                final Positionable pos = form.positionableProperty().get();
                if (pos != null) {
                    pos.setGeometryMode(FXPositionableExplicitMode.MODE);
                    form.setCartoEditedMode((ZoneVegetation) pos);
                    selection.moveSelectedNode(helper.toCoord(e.getX(), e.getY()), true);
                    modified = true;
                    return;
                }
            }

            super.mouseDragged(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (KeyCode.DELETE == e.getCode()) {
                //delete node
                Positionable pos = form.positionableProperty().get();
                pos.setGeometryMode(FXPositionableExplicitMode.MODE);
                form.setCartoEditedMode((ZoneVegetation) pos);
                selection.deleteSelectedNode();
                refreshDecoration();
                modified = true;
            }
        }
    }
}
