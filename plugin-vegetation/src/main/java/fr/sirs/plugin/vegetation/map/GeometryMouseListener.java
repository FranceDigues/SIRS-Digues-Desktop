package fr.sirs.plugin.vegetation.map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Positionable;
import fr.sirs.ui.Growl;
import fr.sirs.util.MouseSelectionUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.Light;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Property;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Component to allow to select geometries from features on Map.
 * <p>
 * Currently, duplicated and adapted from {@link  fr.sirs.map.FXOpenElementEditorAction}
 * TODO : remove duplication by adding a parent class for selection on map
 */
class GeometryMouseListener extends FXPanMouseListen {

    private static final double POINT_RADIUS = 1.;

    //Attributs liés au carré de sélection.
    final Rectangle selection = MouseSelectionUtils.defaultRectangle();
    final Light.Point anchor = new Light.Point();


    private static final String GEOMETRY_NAME = "geometry";
    private static final String GEOM_NAME = "geom";
    private static final String THE_GEOM = "the_geom";


    final ContextMenu choice = new ContextMenu();

    private Geometry selectedGeometry;

    private FXMap map;

    private final BooleanProperty selectionDone = new SimpleBooleanProperty(false);

    GeometryMouseListener(final AbstractNavigationHandler owner) {
        super(owner);
        choice.setAutoHide(true);
    }

    void uninstallFromMap(final FXMap fromMap) {
        fromMap.removeEventHandler(MouseEvent.MOUSE_CLICKED, this);
        fromMap.removeEventHandler(ScrollEvent.SCROLL, this);
         //Necessary to remove the eventHandlers
        fromMap.setOnMouseReleased(null);
        fromMap.setOnMouseDragged(null);
        fromMap.setOnMousePressed(null);
        this.map = null;
        SIRS.LOGGER.log(Level.INFO, "GeometryMouseListener Uninstalled.");
    }

    void installOnMap(final FXMap map, final Pane root) {
        map.addEventHandler(MouseEvent.MOUSE_CLICKED, this);
        map.addEventHandler(ScrollEvent.SCROLL, this);
        map.setCursor(Cursor.DEFAULT);

        //====================================
        //Mise en place du carré de sélection
        //====================================

        final EventHandler<? super MouseEvent> mousePressedSelection = MouseSelectionUtils.mousePressed(selection, anchor, root);
        final EventHandler<? super MouseEvent> mouseDraggedSelection = MouseSelectionUtils.mouseDragged(selection, anchor);
        final EventHandler<? super MouseEvent> mouseReleaseSelection = MouseSelectionUtils.mouseRelease(selection, root);

        map.setOnMousePressed(mousePressedSelection);
        map.setOnMouseDragged(mouseDraggedSelection);
        map.setOnMouseReleased(mouseReleaseSelection);
        //====================================
        this.map = map;
        SIRS.LOGGER.log(Level.FINE, "GeometryMouseListener installed.");
    }


    public BooleanProperty selectionDoneProperty() {
        return selectionDone;
    }

    public Geometry getSelectedGeometry() {
        return selectedGeometry;
    }

    private void setGeomSaveAndOpen() {
        if (selectedGeometry != null) {
            if (selectedGeometry instanceof Polygon) {
                new Growl(Growl.Type.INFO, "Géométrie sélectionnée à partir de l'élément : " + selectedGeometry + "\nGéométrie valide; Ouverture de la fiche.").showAndFade();
                selectionDone.set(true);
            } else if (selectedGeometry instanceof MultiPolygon) {
                final MultiPolygon multipolygon = (MultiPolygon) selectedGeometry;
                if (multipolygon.getNumGeometries() == 1) {
                    selectedGeometry = multipolygon.getGeometryN(0);
                    new Growl(Growl.Type.INFO, "La géométrie sélectionnée est un multipolygone avec 1 polygone. La zone de végétation est créée avec ce polygone :\n" + selectedGeometry).showAndFade();
                    setGeomSaveAndOpen();
                } else {
                    new Growl(Growl.Type.ERROR, "La géométrie sélectionnée est un multipolygone avec plus d'1 polygone.").showAndFade();
                }
            } else {
                new Growl(Growl.Type.ERROR, "La géométrie sélectionnée doit être de type polygone.").showAndFade();
                selectedGeometry = null;
                selectionDone.set(false);
            }
        }
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
                        final MenuItem item = new MenuItem(Session.generateElementTitle(current));
                        item.setOnAction((ActionEvent ae) -> selectElementGeometry(current));
                        items.add(item);
                    }

                    // Show other features in it.
                    if (!foundElements.isEmpty() && !externalFeatures.isEmpty()) {
                        items.add(new SeparatorMenuItem());
                    }

                    if (!externalFeatures.isEmpty()) {
                        for (final Map.Entry<String, Feature> entry : externalFeatures.entrySet()) {
                            final MenuItem item = new MenuItem(entry.getKey());
                            item.setOnAction(ae -> selectFeatureGeometry(entry.getKey(), entry.getValue()));
                            items.add(item);
                        }
                    }

                    choice.show(map, me.getScreenX(), me.getScreenY());
                } else if (foundElements.size() == 1) {
                    selectElementGeometry(foundElements.iterator().next());
                } else if (externalFeatures.size() == 1) {
                    Map.Entry<String, Feature> entry = externalFeatures.entrySet().iterator().next();
                    selectFeatureGeometry(entry.getKey(), entry.getValue());
                }
                //remise à 0 du carré de sélection
                selection.setWidth(0);
                selection.setHeight(0);
            }
        };

        //Recherche sur la surface couverte par le carré de sélection.
        final Rectangle2D.Double searchArea;
        final double selectionWidth = this.selection.getWidth();
        final double selectionHeight = this.selection.getHeight();
        if ((selectionWidth > POINT_RADIUS) || (selectionHeight > POINT_RADIUS)) {
            searchArea = new Rectangle2D.Double(
                    anchor.getX(), anchor.getY(), selection.getWidth(), selection.getHeight());
        } else {
            searchArea = new Rectangle2D.Double(
                    anchor.getX() - (POINT_RADIUS / 2), anchor.getY() - (POINT_RADIUS / 2), POINT_RADIUS, POINT_RADIUS);
        }
        map.getCanvas().getGraphicsIn(searchArea, visitor, VisitFilter.INTERSECTS);

    }


    private void selectElementGeometry(final Element current) {
        if (!(current instanceof Positionable)) {
            new Growl(Growl.Type.WARNING, "L'élément sélectionné est null ou n'est pas un Positionable.\nImpossible de sélectionner sa géométrie").showAndFade();
            return;
        }
        selectedGeometry = ((Positionable) current).getGeometry();
        setGeomSaveAndOpen();
    }

    private void selectFeatureGeometry(final String title, final Feature feature) {
        if (feature == null) {
            new Growl(Growl.Type.WARNING, "Le feature sélectionné est null").showAndFade();
            return;
        }

        SIRS.LOGGER.log(Level.INFO, "External feature selected : " + title + " - " + feature);
        try {

            if (feature instanceof org.geotoolkit.feature.Feature) {
                SIRS.LOGGER.log(Level.INFO, "External feature is org.geotoolkit.feature.Feature : ");
                selectedGeometry = (Geometry) ((org.geotoolkit.feature.Feature) feature).getDefaultGeometryProperty().getValue();

                SIRS.LOGGER.log(Level.INFO, "Selected geometry is  : " + selectedGeometry);
            } else {

                SIRS.LOGGER.log(Level.INFO, "External feature is NOT a  org.geotoolkit.feature.Feature ");
                Property geomFromFeature = (feature.getProperty(GEOMETRY_NAME) != null) ? feature.getProperty(GEOMETRY_NAME) :
                        (feature.getProperty(GEOM_NAME) != null) ? feature.getProperty(GEOM_NAME) :
                                (feature.getProperty(THE_GEOM) != null) ? feature.getProperty(THE_GEOM) : null;

                if (geomFromFeature == null) {
                    SIRS.LOGGER.log(Level.INFO, "External feature doesn't have any of the properties : " + GEOMETRY_NAME + " - " + GEOM_NAME + " - " + THE_GEOM);
                    final FeatureType featureType = feature.getType();
                    if (featureType != null) {
                        final List<String> propertyNames = featureType.getProperties(true).stream().map(type -> type.getName().tip().toString()).collect(Collectors.toList());

                        //choice by user
                        ComboBox<String> choiceComboBox = new ComboBox<>();
                        SIRS.initCombo(choiceComboBox, SirsCore.observableList(propertyNames), null);
                        Alert alert = new Alert(Alert.AlertType.NONE, null, ButtonType.CANCEL, ButtonType.YES);
                        alert.getDialogPane().setContent(choiceComboBox);
                        // Forced to do that because of linux bug.
                        alert.setResizable(true);
                        alert.setWidth(400);
                        alert.setHeight(300);
                        ButtonType response = alert.showAndWait().orElse(ButtonType.CANCEL);
                        if (ButtonType.YES.equals(response)) {
                            geomFromFeature = feature.getProperty(choiceComboBox.getValue());
                        } else {
                            // User cancelled dialog.
                            return;
                        }
                    }
                }
                if (geomFromFeature == null) {
                    SIRS.LOGGER.log(Level.INFO, "Fail to retrieve geometry from the selected feature : \n" + feature);
                    new Growl(Growl.Type.WARNING, "Fail to retrieve geometry from the selected feature : \n" + feature).showAndFade();
                    return;
                } else {
                    final Object geom = geomFromFeature.getValue();
                    if (geom instanceof Geometry) {
                        selectedGeometry = (Geometry) geom;
                    } else {
                        throw new UnsupportedOperationException("Not able to generate Jts Geometry from this kind of object : " + geom);
                    }
                }
            }
            if (selectedGeometry != null) setGeomSaveAndOpen();
        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Failed to retrieve Geometry from external feature", e);
            throw new BackingStoreException(e);
        }

    }

}
