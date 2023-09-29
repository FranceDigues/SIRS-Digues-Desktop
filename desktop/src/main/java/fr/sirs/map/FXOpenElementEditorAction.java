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
package fr.sirs.map;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.Element;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import fr.sirs.util.MouseSelectionUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.effect.Light.Point;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXCanvasHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXMapAction;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.navigation.AbstractMouseHandler;
import org.opengis.feature.Feature;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXOpenElementEditorAction extends FXMapAction {

    /**
     * Half a side of the square to search in when user click on the map.
     */
    private static final double POINT_RADIUS = 1;

    public FXOpenElementEditorAction(final FXMap map) {
        this(map, "informations sur l'élément", "Ouvre la fiche du tronçon/de l'objet du thème");
    }

    public FXOpenElementEditorAction(final FXMap map, final String shortText, final String longText) {
        super(map, shortText, longText, SIRS.ICON_INFO_BLACK_16);

        map.getHandlerProperty().addListener(new ChangeListener<FXCanvasHandler>() {
            @Override
            public void changed(ObservableValue<? extends FXCanvasHandler> observable, FXCanvasHandler oldValue, FXCanvasHandler newValue) {
                selectedProperty().set(newValue instanceof OpenElementEditorHandler);
            }
        });
    }

    @Override
    public void accept(ActionEvent event) {
        if (map != null) {
            map.setHandler(new OpenElementEditorHandler(map));
        }
    }

    private static class OpenElementEditorHandler extends AbstractNavigationHandler {

        private final AbstractMouseHandler mouseListener;

        final Rectangle selection = MouseSelectionUtils.defaultRectangle();
        final Point anchor = new Point();

        public OpenElementEditorHandler(final FXMap map) {
            super();
            mouseListener = new InfoMouseListener(anchor, selection);
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public void install(final FXMap map) {
            super.install(map);
            map.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseListener);
            map.addEventHandler(ScrollEvent.SCROLL, mouseListener);
            map.setCursor(Cursor.DEFAULT);

            //====================================
            //Mise en place du carré de sélection
            //====================================

            Pane root = this.decorationPane;

            map.setOnMousePressed(MouseSelectionUtils.mousePressed(selection, anchor, root));
            map.setOnMouseDragged(MouseSelectionUtils.mouseDragged(selection, anchor));
            map.setOnMouseReleased(MouseSelectionUtils.mouseRelease(selection, root));
            //====================================

            SIRS.LOGGER.log(Level.FINE, "Information handler installed.");
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public boolean uninstall(final FXMap component) {
            super.uninstall(component);
            component.removeEventHandler(MouseEvent.MOUSE_CLICKED, mouseListener);
            component.removeEventHandler(ScrollEvent.SCROLL, mouseListener);
            component.setOnMousePressed(null);
            component.setOnMouseDragged(null);
            component.setOnMouseReleased(null);
            SIRS.LOGGER.log(Level.FINE, "Information handler UNinstalled.");
            return true;
        }

        private class InfoMouseListener extends FXPanMouseListen {

            final ContextMenu choice = new ContextMenu();
            //Attributs liés au carré de sélection.
            Rectangle selection;
            Point anchor;

            public InfoMouseListener(final Point anchor, final Rectangle selection) {
                super(OpenElementEditorHandler.this);
                choice.setAutoHide(true);
                this.selection = selection;
                this.anchor = anchor;
            }

            @Override
            public void mouseClicked(MouseEvent me) {
                super.mouseClicked(me);
                choice.getItems().clear();
                choice.hide();
                // Visitor which will perform action on selected elements.
                final AbstractGraphicVisitor visitor = new AbstractGraphicVisitor() {

                    final HashSet<Element> foundElements = new HashSet<>();
                    final Map<String, Feature> externalFeatures = new HashMap<>();

                    @Override
                    public void visit(ProjectedFeature feature, RenderingContext2D context, SearchAreaJ2D area) {
                        Object userData = feature.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                        if (userData instanceof Element) {
                            foundElements.add((Element) userData);
                        } else {
                            externalFeatures.put(feature.getFeatureId().getID(), feature.getCandidate());
                        }
                    }

                    @Override
                    public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {
                        SIRS.LOGGER.log(Level.FINE, "Coverage elements are not managed yet.");
                    }

                    @Override
                    public void endVisit() {
                        SIRS.LOGGER.log(Level.FINE, "End of visit.");
                        super.endVisit();
                        final int pickedNb = foundElements.size() + externalFeatures.size();
                        if (pickedNb > 1) {
                            // Show picked elements in a context menu
                            final Session session = Injector.getSession();
                            final Iterator<Element> it = foundElements.iterator();
                            ObservableList<MenuItem> items = choice.getItems();
                            while (it.hasNext()) {
                                final Element current = it.next();
                                final MenuItem item = new MenuItem(session.generateElementTitle(current));
                                item.setOnAction((ActionEvent ae) -> displayElement(current));
                                items.add(item);
                            }

                            // Show other features in it.
                            if (!foundElements.isEmpty() && !externalFeatures.isEmpty()) {
                                items.add(new SeparatorMenuItem());
                            }

                            if (!externalFeatures.isEmpty()) {
                                for (final Map.Entry<String, Feature> entry : externalFeatures.entrySet()) {
                                    final MenuItem item = new MenuItem(entry.getKey());
                                    item.setOnAction(ae -> displayFeature(entry.getKey(), entry.getValue()));
                                    items.add(item);
                                }
                            }

                            choice.show(map, me.getScreenX(), me.getScreenY());
                        } else if (foundElements.size() == 1) {
                            displayElement(foundElements.iterator().next());
                        } else if (externalFeatures.size() == 1) {
                            SIRS.LOGGER.log(Level.INFO, "Selection of external feature unsupported.");
                        }
                        //remise à 0 du carré de sélection
                        selection.setWidth(0);
                        selection.setHeight(0);
                    }
                };

                //Recherche sur la surface couverte par le carré de sélection.
                final Rectangle2D.Double searchArea;
                final double selectionWidth  = this.selection.getWidth();
                final double selectionHeight = this.selection.getHeight();
                if( (selectionWidth > POINT_RADIUS) || (selectionHeight >POINT_RADIUS) ) {
                    searchArea = new Rectangle2D.Double(
                        anchor.getX(), anchor.getY(), selection.getWidth(), selection.getHeight());
                }  else {
                    searchArea = new Rectangle2D.Double(
                        anchor.getX()-(POINT_RADIUS/2), anchor.getY() - (POINT_RADIUS/2), POINT_RADIUS, POINT_RADIUS);
                }
                map.getCanvas().getGraphicsIn(searchArea, visitor, VisitFilter.INTERSECTS);

            }
        }

        private void displayFeature(final String title, final Feature feature) {
            String toString = feature.toString();
            // Remove first lines, which displays technical information not useful for end user.
            final int secondLineSeparator = toString.indexOf(System.lineSeparator(), toString.indexOf(System.lineSeparator()) + 1);
            if (secondLineSeparator > 0) {
                toString = toString.substring(secondLineSeparator + 1);
            }
            final TextArea content = new TextArea(toString);
            content.setFont(Font.font("Monospaced"));
            content.setEditable(false);
            content.setPrefSize(700, 500);

            final Dialog d = new Dialog();
            d.initModality(Modality.NONE);
            d.initOwner(map.getScene().getWindow());
            d.setTitle(title);
            final DialogPane dialogPane = new DialogPane();
            dialogPane.setContent(content);
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);
            d.setDialogPane(dialogPane);
            d.setResizable(true);
            d.show();
        }

    }

    private static void displayElement(Element e) {
        Injector.getSession().showEditionTab(e);
    }
}
