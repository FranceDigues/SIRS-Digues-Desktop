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
package fr.sirs.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.LinearReferencingUtilities;
import static fr.sirs.core.LinearReferencingUtilities.cut;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AvecSettableGeometrie;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.Positionable;
import static fr.sirs.map.EditModeObjet.CREATE_OBJET;
import fr.sirs.theme.ui.FXPositionableCoordMode;
import fr.sirs.ui.Growl;
import fr.sirs.util.ConvertPositionableCoordinates;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @param <T>
 */
public class ObjetOnTronconEditHandler<T extends Objet> extends AbstractOnTronconEditHandler<T> {


    /**
     * Same as
     * {@link #ObjetEditHandler(org.geotoolkit.gui.javafx.render2d.FXMap, java.lang.String, java.lang.Class, fr.sirs.map.FXObjetEditPane)}
     * but with a default Pane of edition {@link FXAbstractEditOnTronconPane}
     * and default type name (tronçon).
     *
     * @param map
     * @param clazz
     */
    public ObjetOnTronconEditHandler(final FXMap map, final Class<T> clazz) {
        this(map, clazz, new FXObjetEditPane(map, "troncon", clazz), true);
    }

    /**
     * {@link AbstractNavigationHandler} permettant l'édition d'{@link Objet} du
     * SIRS depuis une carte de l'application.
     *
     * @param map : carte à partir de laquelle on permet l'édition.
     * @param clazz : classe éditée.
     * @param editPane : panneau d'édition associé.
     * @param instantiateMouseEditListener : boolean indiquant si l'on souhaite
     * que ce constructeur instantie {@link #mouseInputListener} with a default
     * value.
     */
    public ObjetOnTronconEditHandler(final FXMap map, final Class<T> clazz, final FXAbstractEditOnTronconPane editPane, final boolean instantiateMouseEditListener) {
        super(map, clazz, editPane, instantiateMouseEditListener);
        ArgumentChecks.ensureNonNull("Panneau d'édition", editPane);

        this.editPane = editPane;

        //on ecoute la selection du troncon et des bornes pour les mettre en surbrillant
        editPane.tronconProperty().addListener(new ChangeListener<TronconDigue>() {
            @Override
            public void changed(ObservableValue<? extends TronconDigue> observable, TronconDigue oldValue, TronconDigue newValue) {
                if (tronconLayer == null) {
                    return;
                }

                updateGeometry();
                if (objetLayer != null) {
                    objetLayer.setSelectionFilter(null);
                }

                if (newValue == null) {
                    tronconLayer.setSelectionFilter(null);
                } else {
                    final Identifier id = new DefaultFeatureId(newValue.getDocumentId());
                    tronconLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(Collections.singleton(id)));
                }
            }
        });

        editPane.objetProperties().addListener(new ListChangeListener<TronconDigue>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TronconDigue> c) {
                if (objetLayer == null) {
                    return;
                }

                final ObservableList<T> lst = editPane.objetProperties();
                final Set<Identifier> ids = new HashSet<>();
                for (T elt : lst) {
                    ids.add(new DefaultFeatureId(elt.getId()));
                }

                final Id filter = GO2Utilities.FILTER_FACTORY.id(ids);
                objetLayer.setSelectionFilter(filter);

                if (ids.size() == 1) {
                    //borne edition mode
                    final String id = lst.get(0).getId();
                    editedObjet = (T) session.getRepositoryForClass(objetClass).get(id);
                    updateGeometry();
                } else {
                    editedObjet = null;
                    updateGeometry();
                }

            }
        });

        editPane.getModeProperty().addListener((cl, o, n) -> mode = editPane.getMode());

        if (instantiateMouseEditListener) {
            mouseInputListener = new EditionOnTronconMouseListen(this);
        }
    }

    @Override
    String getObjetLayerName() {
        final LabelMapper mapper = LabelMapper.get(objetClass);
        return mapper.mapClassName();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap component) {
        super.install(component);

        //recuperation du layer de troncon
        tronconLayer = null;

        //on passe en mode sélection de troncon
        editPane.reset();

        final ContextContainer2D cc = (ContextContainer2D) map.getCanvas().getContainer();
        final MapContext context = cc.getContext();
        toActivateBack = new ArrayList<>();
        for (MapLayer layer : context.layers()) {
            if (layer.getName().equalsIgnoreCase(TRONCON_LAYER_NAME)) {
                tronconLayer = (FeatureMapLayer) layer;
                layer.setSelectable(true);
            } else if (layer.getName().equalsIgnoreCase(getObjetLayerName())) {
                objetLayer = (FeatureMapLayer) layer;
                layer.setSelectable(true);
            } else if (layer.isSelectable()) {
                toActivateBack.add(layer);
                layer.setSelectable(false);
            }
        }

        helperTroncon = new EditionHelper(map, tronconLayer);
        helperTroncon.setMousePointerSize(6);

        helperObjet = new EditionHelper(map, objetLayer);
        helperObjet.setMousePointerSize(6);

    }

    class EditionOnTronconMouseListen extends SIRSEditMouseListen<T> {



        public EditionOnTronconMouseListen(final ObjetOnTronconEditHandler editHandler) {
            super(editHandler, false);
            if (editPane != null) {
                if (editPane instanceof FXObjetEditPane) {
                    final FXObjetEditPane pane = (FXObjetEditPane) editPane;
                    pane.geometryTypeProperty.addListener((cl, o, n) -> {

                        justCreated = false;
                        geomLayer.getGeometries().clear();
                        geomLayer.setNodeSelection(null);
                        coords.clear();
                        editGeometry.reset();
                        this.updateGeometryType();
                    });
                    pane.selectedObjetProperty.addListener((ob, ol, ne) -> {
                        //HACK_REDMINE_7621 : The selected troncon is removed from the selection for more visibility
                        tronconLayer.setSelectionFilter(null);
                        updateSelectedObjet(ne);
                    });
                }

                editPane.tronconProperty().addListener((ob, ol, ne) -> chooseTypesAndCreate());
            }

        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (tronconLayer == null) {
                throw new IllegalStateException("Auncune couche cartographique associée aux tronçons");
            }

            startX = getMouseX(e);
            startY = getMouseY(e);
            mousebutton = e.getButton();

            if(modeProperty.get() ==null) {
                modeProperty.setValue(EditModeObjet.NONE);
            }

            if(EditModeObjet.NONE.equals(modeProperty.get())) {
                displayGrowl(Growl.Type.INFO, "Type d'action non sélectionné (création, modification, choix tronçon...)");
                return;
            } else if (EditModeObjet.PICK_TRONCON.equals(modeProperty.get())) {
                if (mousebutton == MouseButton.PRIMARY) {
                    //selection d'un troncon
                    final Feature feature = helperTroncon.grabFeature(e.getX(), e.getY(), false);
                    if (feature != null) {
                        Object bean = feature.getUserData().get(BeanFeature.KEY_BEAN);
                        if (bean instanceof TronconDigue && session.editionAuthorized((TronconDigue) bean)) {
                            bean = session.getRepositoryForClass(TronconDigue.class).get(((TronconDigue) bean).getDocumentId());
                            editPane.tronconProperty().set((TronconDigue) bean);
                        }
                    }
                }
            } else {
                final double x = e.getX();
                final double y = e.getY();

                if (MouseButton.PRIMARY.equals(mousebutton)) {
//
                    // L'objet existe, on peut travailler avec sa géométrie.
                    switch (modeProperty.get()) {
                        case CREATE_OBJET:
                            if (editedObjet == null) {
                                chooseTypesAndCreate();
                            } else {
                                createNewGeometryForObjet(x, y);
                            }
                            break;

                        case EDIT_OBJET:
                            if (editedObjet == null) {
                                selectObjet(x, y);
                            } else {
                                modifyObjetGeometry(e, x, y);
                            }
                            break;
                    }
                } else if (MouseButton.SECONDARY.equals(e.getButton())) {
                    if (editedObjet == null) {
                        chooseTypesAndCreate();
                    } else {
                        concludeTheEdition(x, y);
                    }
                }
            }

        }

        private void updateSelectedObjet(Object selected) {
            if (editedClass.isInstance(selected)) {
                editedObjet = editedClass.cast(selected);
                // erase the temporary geometry
                editGeometry.reset();
                // remove the temporary geometry from the edition layer
                geomLayer.getGeometries().clear();
            } else {
                SIRS.LOGGER.log(Level.WARNING, "L''\u00e9l\u00e9ment s\u00e9lectionn\u00e9 n''est pas une instance de la classe edit\u00e9e : {0}", editedClass);
            }

        }

        @Override
        protected void chooseTypesAndCreate() {

            objetHelper = getHelperObjet();
            editedObjet = session.getElementCreator().createElement(editedClass);
            final TronconDigue tronconParent = editPane.getTronconFromProperty();
            editedObjet.setForeignParentId(tronconParent==null?null:tronconParent.getId());
            modeProperty.setValue(CREATE_OBJET);

            updateGeometryType();

        }

        private void updateGeometryType() {
            switch (((FXObjetEditPane) editPane).getGeomType()) {
                case "Ponctuel":
                    newGeomType = Point.class;
                    break;
                case "Linéaire":
                    newGeomType = LineString.class;
                    break;
                default:
                    final Growl growl = new Growl(Growl.Type.WARNING, "Type de géométrie non renseignée.");
                    growl.showAndFade();
                    newGeomType = LineString.class;
            }
        }

        @Override
        EventHandler<ActionEvent> saveAndReset() {
            return (ActionEvent event) -> {
                final Geometry geometry = editGeometry.geometry.get();
                if (geometry == null) {
                    final Growl growl = new Growl(Growl.Type.WARNING, "Géométrie non renseignée, sauvegarde annulée.");
                    growl.showAndFade();
                    return;
                }

                projectOnTronçon(geometry, editPane.getTronconFromProperty());

                final AbstractSIRSRepository repo = Injector.getSession().getRepositoryForClass(editedObjet.getClass());

                if (editedObjet.getId() != null) {
                    repo.update(editedObjet);
                } else {
                    repo.add(editedObjet);
                }
                // Open the sheet of the created element
                Injector.getSession().showEditionTab(editedObjet, e -> true);

                // On quitte le mode d'édition.
                reset();
                uninstall(map);
            };
        }

        /**
         * Provide actions to apply when the geomtype is a point and a second
         * point was geometry is created.
         *
         * Ici ({@link ObjetOnTronconEditHandler}, on termine remplace le point précédent.
         */
        @Override
        protected void PointSecondGeometryStrategy() {

            final Geometry geometry = editGeometry.geometry.get();
            if (editedObjet instanceof AvecSettableGeometrie) {
                ((AvecSettableGeometrie) editedObjet).setGeometry(geometry);
            } else {
                throw new IllegalStateException("Impossible d'associer le type de géométrie éditée au type d'objet édité");
            }
        }

        /**
         * Affecte les points de début et de fin (position réelle de l'élément)
         * puis projette la géométrie créée sur le tronçon d'appartenance.
         *          *
         * Finallement, 'repaint' la carte.
         */
        void projectOnTronçon(final Geometry geometry, final TronconDigue troncon) {

            if (troncon == null) {
                displayGrowl(Growl.Type.WARNING, "Tronçon non renseigné.\n Impossible de projeter la géométrie créée.");
                return;
            }
            final LineString linear = LinearReferencingUtilities.asLineString(troncon.getGeometry());

//
//                final BorneDigue borne = repo.get(srb.getBorneId());
//            final Geometry geometry = editedObjet.getGeometry();
            final LinearReferencingUtilities.SegmentInfo[] segments ;

            final Point positionDebut , positionFin;
            final boolean isPositionable = editedObjet instanceof Positionable;

            if (geometry instanceof Point) {

                final Point pointProj = (Point) geometry;
//                final Point pointReal = EditionHelper.createPoint(pointProj.getCoordinate());
                final Point pointReal = (Point) pointProj.clone();
                segments = LinearReferencingUtilities.buildSegments(linear);
                final LinearReferencingUtilities.ProjectedPoint proj = LinearReferencingUtilities.projectReference(segments, pointProj);
                pointProj.getCoordinate().setCoordinate(proj.projected);
                final CoordinateReferenceSystem crs = session.getProjection();
                JTS.setCRS(pointProj, crs);
                JTS.setCRS(pointReal, crs);

                if(isPositionable) {
                    positionDebut = pointReal;
                    positionFin = positionDebut;
                    setPositionableGeometries(editedObjet, pointProj, positionDebut, positionFin, troncon.getSystemeRepDefautId());
                    ConvertPositionableCoordinates.computePositionableLinearCoordinate(editedObjet);
                } else {
                    editedObjet.setGeometry(pointProj);
                }
            } else if (geometry instanceof LineString) {
                final Coordinate[] coordinates = geometry.getCoordinates();
                if ((coordinates == null) || (coordinates.length == 0)) {
                    displayGrowl(Growl.Type.WARNING, "Pas de coordonnées associées à la géométrie créée.\n Impossible de projeter la géométrie créée.");
                    return;
                }
                final int length = coordinates.length;


//                try {

                positionDebut = EditionHelper.createPoint(coordinates[0]);
                positionFin   = EditionHelper.createPoint(coordinates[length - 1]);
                final CoordinateReferenceSystem crs = session.getProjection();

                JTS.setCRS(positionDebut, crs);
                JTS.setCRS(positionFin, crs);


                final Point pointProjDebut = (Point) positionDebut.clone();
                final Point pointProjFin   = (Point) positionFin.clone();

                segments = LinearReferencingUtilities.buildSegments(linear);

                LinearReferencingUtilities.ProjectedPoint proj = LinearReferencingUtilities.projectReference(segments, pointProjDebut);
                pointProjDebut.getCoordinate().setCoordinate(proj.projected);
                JTS.setCRS(pointProjDebut, crs);
                final double distanceDebut = proj.distanceAlongLinear;

                proj = LinearReferencingUtilities.projectReference(segments, pointProjFin);
                pointProjFin.getCoordinate().setCoordinate(proj.projected);
                JTS.setCRS(pointProjFin, crs);
                final double distanceFin = proj.distanceAlongLinear;

                final boolean reverse = distanceFin < distanceDebut;
                final LineString projLine;
                if (reverse) { // Need to reverse start and end
                    projLine = cut(linear, distanceFin, distanceDebut);

                } else {
                    projLine = cut(linear, distanceDebut, distanceFin);
                }

                if (isPositionable) {
                    if (reverse) {
                        setPositionableGeometries(editedObjet, projLine, positionFin, positionDebut, troncon.getSystemeRepDefautId());
                    } else {
                        setPositionableGeometries(editedObjet, projLine, positionDebut, positionFin, troncon.getSystemeRepDefautId());
                    }
                    ConvertPositionableCoordinates.computePositionableLinearCoordinate(editedObjet, true);

                } else {
                    editedObjet.setGeometry(projLine);
                }
                editGeometry.geometry.set(projLine);
                JTS.setCRS(editGeometry.geometry.get(), map.getCanvas().getObjectiveCRS2D());
                geomLayer.getGeometries().setAll(editGeometry.geometry.get());

            }

            displayGrowl(Growl.Type.INFO, "Géométrie projetée avec succès sur le tronçon d'appartenance.");


//            uiObjetTable.getSelectionModel().clearSelection();
//            map.getCanvas().repaint();
        }

    }

    /**
     *
     * @param positionable on wich it sets the geometries
     * @param projected projected geometry to set
     * @param positionDebut real position of the start point to set
     * @param positionFin real position of the end point to set
     */
    private void setPositionableGeometries(final Positionable positionable, final Geometry projected, final Point positionDebut, final Point positionFin, final String sr) {
        //PosInfo PosR
        positionable.setBorneDebutId(null);
        positionable.setBorneFinId(null);
        positionable.setBorne_debut_aval(false);
        positionable.setBorne_fin_aval(false);
        positionable.setBorne_debut_distance(0);
        positionable.setBorne_fin_distance(0);

        positionable.setPositionDebut(positionDebut);
        positionable.setPositionFin(positionFin);
        positionable.setGeometry(projected);
        positionable.setGeometryMode(FXPositionableCoordMode.MODE);
        positionable.setSystemeRepId(sr);

    }

    private static void displayGrowl(final Growl.Type type, final String text) {
        final Growl growl = new Growl(type, text);
                growl.showAndFade();
    }

}
