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
package fr.sirs.plugin.vegetation.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import fr.sirs.Injector;
import static fr.sirs.SIRS.CSS_PATH;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.*;
import fr.sirs.plugin.vegetation.PluginVegetation;
import fr.sirs.theme.ui.FXPositionableExplicitMode;
import fr.sirs.ui.Growl;
import fr.sirs.util.SirsStringConverter;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import static fr.sirs.plugin.vegetation.map.EditVegetationUtils.*;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import javafx.scene.layout.VBox;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionTool;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;
import org.geotoolkit.internal.Loggers;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Maxime Gavens (Geomatys)
 * @param <T>
 */
public abstract class CreateVegetationPolygonTool<T extends ZoneVegetation> extends AbstractEditionTool {

    //session and repo
    private final Session session;
    private final AbstractSIRSRepository<ParcelleVegetation> parcelleRepo;
    private final SirsStringConverter cvt = new SirsStringConverter();

    private FXPanMouseListen currentMouseInputListener = new MouseListen();
    private MouseListen defaultMouseInputListener = null;
    private GeometryMouseListener selectOnMapMouseListener = null;

    protected final BorderPane wizard = new BorderPane();
    protected final Class<T> vegetationClass;

    protected T vegetation = null;
    protected ParcelleVegetation parcelle = null;
    private final Label lblParcelle = new Label();
    private final Label lblGeom = new Label();

    protected final Button end = new Button("Enregistrer");

    private FeatureMapLayer parcelleLayer = null;

    //geometry en cours
    private EditionHelper helper;
    private final FXGeometryLayer geomLayer = new FXGeometryLayer();
    private final EditionHelper.EditionGeometry editGeometry = new EditionHelper.EditionGeometry();
    private final List<Coordinate> coords = new ArrayList<>();
    private final BooleanProperty ended = new SimpleBooleanProperty(false);

    private static final String TO_SELECT_ON_MAP_TEXT = "Sélectionner une géométrie sur la carte";
    private static final String HOW_TO_CREATE_GEOM_TEXT = "Cliquer sur la carte pour créer la géométrie, faire un double clic pour terminer la géométrie, faire un clic droit pour supprimer le dernier point.";

    private static final String TO_DEFAULT_TEXT = "Retour à la sélection sur la carte";
    private static final String SELECT_ON_MAP_DESCRIPTION = "Sélectionner une géométrie existante sur la carte. Si la sélection ne fonctionne pas, assurez vous que la couche de la géométrie est bien sélectionnable (cadenas ouvert).";

    private final Button selectGeomOnMapButton = new Button(TO_SELECT_ON_MAP_TEXT);
    /** List of layers deactivated on tool install. They will be activated back at uninstallation. */
    private List<MapLayer> toActivateBack;

    //Add editable fields ticket redmine 7741
    private final TextField ui_Designation = new TextField();
    private final CheckBox checkContactEau = new CheckBox();
    private final ComboBox<Preview> ui_typeCoteId = new ComboBox();
    private final ComboBox<Preview> ui_typePositionId = new ComboBox<>();
    private final TextArea ui_commentaire = new TextArea();

    public CreateVegetationPolygonTool(FXMap map, Spi spi, Class<T> clazz) {
        super(spi);
        vegetationClass = clazz;
        wizard.getStylesheets().add(CSS_PATH);

        if (vegetation != null) {
            ui_Designation.setText(vegetation.getDesignation());
            ui_commentaire.setText(vegetation.getCommentaire());
        }
        ui_commentaire.setPrefHeight(100);

        initRefPreviewComboBox(ui_typeCoteId, RefCote.class, vegetation == null ? null : vegetation.getTypeCoteId());
        initRefPreviewComboBox(ui_typePositionId, RefPosition.class, vegetation == null ? null : vegetation.getTypePositionId());

        end.disableProperty().bind(ended.not());
        end.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                saveAction(true);
            }
        });
        Button cancel = new Button("Annuler");
        cancel.setOnAction(event -> {
            reset();
            map.setHandler(new FXPanHandler(true));
        });
        end.getStyleClass().add("btn-single");
        cancel.getStyleClass().add("btn-single");


        session = Injector.getSession();
        parcelleRepo = session.getRepositoryForClass(ParcelleVegetation.class);

        final Label lbl1 = generateHeaderLabel("Parcelle :");
        final Label lbl2 = generateHeaderLabel("Géométrie :");
        wizard.getStyleClass().add("blue-light");
        lblParcelle.getStyleClass().add("label-text");
        lblParcelle.setWrapText(true);
        lblGeom.getStyleClass().add("label-text");
        lblGeom.setWrapText(true);

        selectGeomOnMapButton.disableProperty().set(true);
        selectGeomOnMapButton.setOnAction(e -> changeMouseListener());

        final GridPane attributeGrid = new GridPane();
        attributeGrid.setHgap(2);
        attributeGrid.setVgap(3);
        attributeGrid.setHgap(15);
        attributeGrid.add(generateHeaderLabel(LABEL_CONTACT_EAU),0,0);
        attributeGrid.add(checkContactEau,1,0);
        attributeGrid.add(generateHeaderLabel(LABEL_POSITION_ID),0,1);
        attributeGrid.add(ui_typePositionId,1,1);
        attributeGrid.add(generateHeaderLabel(LABEL_COTE_ID),0,2);
        attributeGrid.add(ui_typeCoteId,1,2);

        final VBox vbox = new VBox(15,
                new HBox(15, lbl1, lblParcelle),
                new HBox(15, generateHeaderLabel(LABEL_DESIGNATION), ui_Designation),
                 new VBox(15, lbl2, new HBox(15, selectGeomOnMapButton), lblGeom),
                attributeGrid,
                new HBox(15, generateHeaderLabel(LABEL_COMMENTAIRE), ui_commentaire),
                new HBox(30, end, cancel));
        vbox.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        wizard.setCenter(vbox);
    }

    void saveAction(final boolean saveInBase) {
        // editedGeoCoordinate set to false by default when creating Positionable via carto.
        // not enough to differentiate from a positionable edited via its bornes or the carto.
        vegetation.setCartoEdited(true);
        vegetation.setGeometryMode(FXPositionableExplicitMode.MODE);
        vegetation.setValid(true);
        vegetation.setForeignParentId(parcelle.getDocumentId());
        setEditedAttributes(vegetation, checkContactEau, ui_typePositionId, ui_typeCoteId);
        vegetation.setCommentaire(ui_commentaire.getText());
        final String designation = ui_Designation.getText();
        if (designation != null && !designation.isEmpty()) vegetation.setDesignation(designation);
        if (saveInBase) {
            saveInBase();
        }
    }

    protected void saveInBase() {
        final AbstractSIRSRepository vegetationRepo = session.getRepositoryForClass(vegetationClass);
        vegetationRepo.add(vegetation);
        startGeometry();
    }

    private void changeMouseListener() {
        if (this.selectOnMapMouseListener == null) {
            this.selectOnMapMouseListener = new GeometryMouseListener(this);
        }

        if (this.defaultMouseInputListener == null) { // then the default mouselistener must be use as currentMouseInputListener
            this.defaultMouseInputListener = (MouseListen) this.currentMouseInputListener;
        }

        //Change to select on map Mode
        if (this.currentMouseInputListener == this.defaultMouseInputListener) {
            toSelectOnMapMode();
            //Roll back on default geom edition to select on map Mode
        } else {
            toDefaultGeometryMode();
        }
    }

    private void toSelectOnMapMode() {
        uninstallCurrentMouseListener(map);
        selectOnMapMouseListener.installOnMap(map, this.decorationPane);
        this.currentMouseInputListener = selectOnMapMouseListener;
        selectOnMapMouseListener.selectionDoneProperty().addListener((ch, old, nw) -> {
            if (nw) {
                handleSelectedGeometry();
            }
        });
        selectGeomOnMapButton.setText(TO_DEFAULT_TEXT);
        lblGeom.setText(SELECT_ON_MAP_DESCRIPTION);
    }

    private void toDefaultGeometryMode() {
        this.currentMouseInputListener = defaultMouseInputListener;
        selectOnMapMouseListener.uninstallFromMap(map);
        installCurrentMouseListener(map);
        selectGeomOnMapButton.setText(TO_SELECT_ON_MAP_TEXT);
        lblGeom.setText(HOW_TO_CREATE_GEOM_TEXT);
    }


    private void handleSelectedGeometry() {
        changeMouseListener();
        acceptSelectedGeom();
        selectOnMapMouseListener = null;
        refreshGeometryFromCoords();
    }

    /**
     * Get the geometry selected to the user and use it for the default geometry edition
     */
    private void acceptSelectedGeom() {
        editGeometry.reset();
        coords.clear();
        ended.set(false);
        geomLayer.getGeometries().clear();
        //Keep the helper

        this.editGeometry.geometry.set(selectOnMapMouseListener.getSelectedGeometry());
        setGeometryCRSAndAddToLayer();
        final Geometry geomToSet = this.reprojectOnParcelle(editGeometry.geometry.get());

        coords.addAll(Arrays.asList(geomToSet.getCoordinates()));

        final ButtonType result = SIRS.fxRunAndWait(() -> {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Souhaitez vous utiliser cette géométrie telle quelle?\n(Sinon, vous pourrez continuer son édition)", ButtonType.YES, ButtonType.NO);
            alert.setResizable(true);

            return alert.showAndWait().orElse(ButtonType.YES);
        });

        if (ButtonType.YES.equals(result)) {
            concludeGeometry();
        } else {
            toDefaultGeometryMode();
            removeLastPoint(); // -> as the last point is the first point of the polygon to close it, we remove it to continue the edition
        }
    }

    private Geometry reprojectOnParcelle(final Geometry toReproject) {

        if (this.helper.getConstraint() == null) {
            final Geometry constraint = getParcelleConstraints(parcelle);
            if (constraint == null) {
                SIRS.LOGGER.log(Level.WARNING, "No contraint from parcelle found during vegetation creation.");
                return toReproject;
            }
            this.helper.setConstraint(getParcelleConstraints(parcelle));
        }
        final Geometry reprojected = toReproject.intersection(this.helper.getConstraint());

        if (!reprojected.equals(toReproject)) {
            new Growl(Growl.Type.WARNING, "Au moins un point de la géométrie est hors de la parcelle.\nCe(s) point et reprojeté sur la parcelle.").showAndFade();
            editGeometry.geometry.set(reprojected);
            setGeometryCRSAndAddToLayer();
            return reprojected;
        }
        return toReproject;
    }

    static Geometry getParcelleConstraints(final ParcelleVegetation parcelle) {
        if (parcelle == null) return null;
        final Geometry constraint = parcelle.getGeometry().buffer(1000000, 10, BufferParameters.CAP_FLAT);
        try {
            JTS.setCRS(constraint, JTS.findCoordinateReferenceSystem(parcelle.getGeometry()));
        } catch (FactoryException ex) {
            Loggers.JAVAFX.log(Level.WARNING, ex.getMessage(), ex);
        }
        return constraint;
    }

    protected T newVegetation() {
        final T candidate = Injector.getSession().getElementCreator().createElement(vegetationClass);
        candidate.setTraitement(Injector.getSession().getElementCreator().createElement(TraitementZoneVegetation.class));
        return candidate;
    }

    void reset() {
        checkContactEau.setSelected(false);
        ui_typeCoteId.getSelectionModel().clearSelection();
        ui_typePositionId.getSelectionModel().clearSelection();
        vegetation = newVegetation();
        ui_Designation.setText(vegetation.getDesignation());
        parcelle = null;
        lblParcelle.setText("Sélectionner une parcelle sur la carte");
        ui_commentaire.setText(null);
        lblGeom.setText("");
        selectGeomOnMapButton.disableProperty().set(true);
        selectOnMapMouseListener = null;
        defaultMouseInputListener = null;
        if (parcelleLayer != null) parcelleLayer.setSelectionFilter(null);

        editGeometry.reset();
        coords.clear();
        ended.set(false);
        geomLayer.getGeometries().clear();
    }

    protected void startGeometry() {
        editGeometry.reset();
        coords.clear();
        ended.set(false);
        geomLayer.getGeometries().clear();

        vegetation = newVegetation();
        lblParcelle.setText(cvt.toString(parcelle));
        lblGeom.setText(HOW_TO_CREATE_GEOM_TEXT);
        selectGeomOnMapButton.disableProperty().set(false);
        parcelleLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(
                Collections.singleton(new DefaultFeatureId(parcelle.getDocumentId()))));

        final Geometry constraint = getParcelleConstraints(parcelle);
        helper.setConstraint(constraint);
    }

    @Override
    public Node getConfigurationPane() {
        return wizard;
    }

    @Override
    public Node getHelpPane() {
        return null;
    }

    private void installCurrentMouseListener(final FXMap component) {

        component.addEventHandler(MouseEvent.ANY, currentMouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, currentMouseInputListener);

        if (this.helper == null)
            helper = new EditionHelper(map, parcelleLayer); //else reuse it to keep the current constraint.

        component.setCursor(Cursor.CROSSHAIR);
        component.addDecoration(geomLayer);
    }

    private void uninstallCurrentMouseListener(final FXMap component) {

        component.removeEventHandler(MouseEvent.ANY, currentMouseInputListener);
        component.removeEventHandler(ScrollEvent.ANY, currentMouseInputListener);
        component.setCursor(Cursor.DEFAULT);
        component.removeDecoration(geomLayer);

    }

    @Override
    public void install(FXMap component) {
        reset();
        super.install(component);

        // On instancie une nouvelle liste pour les couches à désactiver provisoirement (le temps de l'activation de l'outil)
        toActivateBack = new ArrayList<>();

        //on rend les couches troncon et borne selectionnables
        final MapContext context = component.getContainer().getContext();
        for (MapLayer layer : context.layers()) {
            if (layer.getName().equalsIgnoreCase(PluginVegetation.PARCELLE_LAYER_NAME)) {
                parcelleLayer = (FeatureMapLayer) layer;
            } else if (layer.isSelectable()) {
                toActivateBack.add(layer);
                layer.setSelectable(false);
            }
        }
        installCurrentMouseListener(component);
    }

    @Override
    public boolean uninstall(FXMap component) {
        super.uninstall(component);
        if (toActivateBack != null) {
            for (final MapLayer layer : toActivateBack) {
                layer.setSelectable(true);
            }
        }
        uninstallCurrentMouseListener(component);
        reset();
        return true;
    }


    private void setGeometryCRSAndAddToLayer() {
        JTS.setCRS(editGeometry.geometry.get(), map.getCanvas().getObjectiveCRS2D());
        geomLayer.getGeometries().setAll(editGeometry.geometry.get());
    }

    private void removeLastPoint() {
        if (parcelle != null) {
            if (coords.size() == 2) {
                coords.clear();
            } else if (coords.size() >= 3) {
                coords.remove(coords.size() - 1);
            }
            refreshGeometryFromCoords();
        }
    }

    private void refreshGeometryFromCoords() {
        if (coords.isEmpty()) {
            editGeometry.reset();
            ended.set(false);
            geomLayer.getGeometries().clear();
        } else if (coords.size() == 1) {
            editGeometry.geometry.set(EditionHelper.createPoint(coords.get(0)));
            setGeometryCRSAndAddToLayer();
        } else if (coords.size() == 2) {
            editGeometry.geometry.set(EditionHelper.createLine(coords));
            setGeometryCRSAndAddToLayer();
        } else {
            editGeometry.geometry.set(EditionHelper.createPolygon(coords));
            setGeometryCRSAndAddToLayer();
        }
    }

    /**
     * Conclude the edition of the geometry
     */
    private void concludeGeometry() {
        //on sauvegarde
        if (editGeometry.geometry.get() == null || editGeometry.geometry.get().isEmpty() || !editGeometry.geometry.get().isValid() || !(editGeometry.geometry.get() instanceof Polygon)) {
            //il faut un polygon valide
            new Growl(Growl.Type.WARNING, "Géométrie éditée non valide.\nUn polygone non nul est attendu.").showAndFade();
            return;
        }

        vegetation.setExplicitGeometry(editGeometry.geometry.get());
        vegetation.setGeometry(editGeometry.geometry.get());
        ended.set(true);
    }

    private class MouseListen extends FXPanMouseListen {

        public MouseListen() {
            super(CreateVegetationPolygonTool.this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (ended.get()) return;

            mousebutton = e.getButton();
            if (MouseButton.PRIMARY.equals(mousebutton)) {
                if (parcelle == null) {
                    final Rectangle2D clickArea = new Rectangle2D.Double(e.getX() - 2, e.getY() - 2, 4, 4);

                    parcelleLayer.setSelectable(true);
                    //recherche une parcelle sous la souris
                    map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                        @Override
                        public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                            final Object bean = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                            if (bean instanceof ParcelleVegetation) {
                                //on recupere l'object complet
                                parcelle = (ParcelleVegetation) bean;
                                //on recupere l'object complet
                                parcelle = parcelleRepo.get(parcelle.getDocumentId());
                                startGeometry();
                            }
                        }

                        @Override
                        public boolean isStopRequested() {
                            return parcelle != null;
                        }

                        @Override
                        public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
                    }, VisitFilter.INTERSECTS);
                } else if (parcelle != null) {
                    if (e.getClickCount() > 1) {
                        concludeGeometry();
                    } else {
                        final double x = getMouseX(e);
                        final double y = getMouseY(e);

                        if (coords.isEmpty()) {
                            coords.add(helper.toCoord(x, y));
                            coords.add(helper.toCoord(x, y));
                        } else {
                            coords.add(helper.toCoord(x, y));
                        }
                    }
                }
            } else if (MouseButton.SECONDARY.equals(mousebutton)) {
                removeLastPoint();
            } else {
                super.mouseClicked(e);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (ended.get()) return;
            final MouseButton button = e.getButton();
            if (button != MouseButton.PRIMARY) super.mouseMoved(e);
            final double x = getMouseX(e);
            final double y = getMouseY(e);

            if (coords.size() >= 2) {
                coords.remove(coords.size() - 1);
                coords.add(helper.toCoord(x, y));
            } else {
                super.mouseMoved(e);
            }
            refreshGeometryFromCoords();
        }

    }
}
