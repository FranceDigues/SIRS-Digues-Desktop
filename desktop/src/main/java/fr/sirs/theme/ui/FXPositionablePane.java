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
package fr.sirs.theme.ui;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.CRS_WGS84;
import static fr.sirs.SIRS.ICON_VIEWOTHER_WHITE;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Identifiable;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.ConvertPositionableCoordinates;
import static fr.sirs.util.SIRSAreaComputer.getGeometryInfo;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.LinearReferencing;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Form editor allowing to update linear and geographic/projected position of a
 * {@link Positionable} element.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXPositionablePane extends BorderPane {

    private static final NumberFormat DISTANCE_FORMAT = new DecimalFormat("0.#");
    private static final NumberFormat PR_FORMAT = new DecimalFormat("0.00");

    private final List<FXPositionableMode> modes = new ArrayList<>();

    @FXML
    private Button uiView;
    @FXML
    private HBox uiExtraContainer;
    @FXML
    private HBox uiModeContainer;

    // PR Information
    @FXML
    private Label uiSR;
    @FXML
    private Label uiPRDebut;
    @FXML
    private Label uiPRFin;
    @FXML
    private Label uiGeomInfo;

    //Bouton permettant le rafraîchissement du SR par défaut manuellement.
    @FXML
    protected Button uiRefreshCoord = new Button(null);

    private final ObjectProperty<Positionable> posProperty = new SimpleObjectProperty<>();
    private final BooleanProperty disableFieldsProperty = new SimpleBooleanProperty(true);
    private final CoordinateReferenceSystem baseCrs = Injector.getSession().getProjection();

    public FXPositionablePane() {
        this(Arrays.asList(new FXPositionableCoordMode(), new FXPositionableLinearMode()));
    }

    public FXPositionablePane(final List<FXPositionableMode> lstModes) {
        this(lstModes, Positionable.class);
    }

    public FXPositionablePane(final List<FXPositionableMode> lstModes, final Class<? extends Positionable> clazz) {
        SIRS.loadFXML(this, clazz);

        uiView.setGraphic(new ImageView(ICON_VIEWOTHER_WHITE));

        uiRefreshCoord.setGraphic(new ImageView(SIRS.ICON_REFRESH_WHITE));
        uiRefreshCoord.setTooltip(new Tooltip("Actualiser SR par défaut"));

        modes.addAll(lstModes);

        //pour chaque mode un toggle button
        final ToggleGroup group = new ToggleGroup();
        for (int i = 0, n = modes.size(); i < n; i++) {
            final FXPositionableMode mode = modes.get(i);

            final ToggleButton button = new ToggleButton(mode.getTitle());
            button.setToggleGroup(group);
            button.getStyleClass().add((i == 0) ? "state-button-left" : (i == n - 1) ? "state-button-right" : "state-button-center");
            button.setUserData(mode);
            uiModeContainer.getChildren().add(button);
        }

        //on change les panneaux visibles pour le mode actif
        group.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {
            if (oldValue != null) {
                final FXPositionableMode mode = (FXPositionableMode) oldValue.getUserData();
                setCenter(null);
                uiExtraContainer.getChildren().clear();
                mode.disablingProperty().unbind();
                mode.positionableProperty().unbind();
            }

            if (newValue == null) {
                group.selectToggle(group.getToggles().get(0));
            } else {
                final FXPositionableMode mode = (FXPositionableMode) newValue.getUserData();
                setCenter(mode.getFXNode());
                uiExtraContainer.getChildren().addAll(mode.getExtraButton());
                mode.disablingProperty().bind(disableFieldsProperty);
                mode.positionableProperty().bind(posProperty);
                if (posProperty.get() != null) {
                    posProperty.get().setGeometryMode(mode.getID());
                }

                final Preferences prefNode = Preferences.userNodeForPackage(FXPositionableMode.class);
                prefNode.put("default", mode.getID());
                try {
                    prefNode.flush();
                } catch (Exception e) {
                    SIRS.LOGGER.log(Level.FINE, "Cannot flush user preferences for toggle selection.", e);
                }
            }
        });

        // Update SR-PR information
        final ChangeListener<Geometry> geomListener = new ChangeListener<Geometry>() {
            @Override
            public void changed(ObservableValue<? extends Geometry> observable, Geometry oldValue, Geometry newValue) {
                updateSRAndPRInfo();
            }
        };

        posProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.geometryProperty().removeListener(geomListener);
                oldValue.positionDebutProperty().removeListener(geomListener);
                oldValue.positionFinProperty().removeListener(geomListener);
            }
            if (newValue != null) {
                newValue.geometryProperty().addListener(geomListener);
                newValue.positionDebutProperty().addListener(geomListener);
                newValue.positionFinProperty().addListener(geomListener);
                ConvertPositionableCoordinates.COMPUTE_MISSING_COORD.test(newValue);

                //on active le mode dont le type correspond
                final String modeName = Preferences.userNodeForPackage(FXPositionableMode.class).get("default", null);
                Toggle active = null;
                boolean first =true;
                for (Toggle t : group.getToggles()) {
                    if (first) {
                        active = t;
                        first =false;
                    }
                    final FXPositionableMode mode = (FXPositionableMode) t.getUserData();
                    if (mode != null && mode.getID().equalsIgnoreCase(modeName)) {
                        active = t;
                        break;
                    }
                }
                group.selectToggle(active);
                newValue.setGeometryMode(((FXPositionableMode) active.getUserData()).getID());
            }
            updateSRAndPRInfo();
        });

    }

    private LinearReferencing.SegmentInfo[] getSourceLinear(final SystemeReperage source) {
        return ConvertPositionableCoordinates.getSourceLinear(source, posProperty.get());
    }

    /**
     * Méthode de changement de linéaire sur lequel est situé le positionable.
     *
     * @param linear nouveau linéaire à affecter au positionable (on attend un
     * {@link Preview}
     */
    public void updateLinear(Object linear) {

        // Cas du Preview (type normalement renvoyé par la liste déroulante de l'UI)
        if (linear instanceof Preview) {
            final Preview preview = (Preview) linear;
            SIRS.LOGGER.log(Level.FINE, "changement de linéaire pour {0}", preview);

            // On vérifie qu'on a bien toutes les informations nécessaires pour éviter les NPE etc.
            if (preview.getElementId() != null && preview.getElementClass() != null) {

                // On prévient les éventuels problèmes de chargement de classe qui ne devraient pas se produire
                try {
                    final Class elementClass = Class.forName(preview.getElementClass());
                    final Identifiable identifiable = Injector.getSession().getRepositoryForClass(elementClass).get(preview.getElementId());

                    // On s'assure qu'on obtient bien un tronçon de digue et on opère le changement
                    if (identifiable instanceof TronconDigue) {
                        changeTroncon((TronconDigue) identifiable);
                    } else {
                        SIRS.LOGGER.log(Level.WARNING, "type de linéaire inattendu pour {0}", identifiable);
                    }
                } catch (ClassNotFoundException ex) {
                    // On loggue ET on propage cette exception importante
                    SIRS.LOGGER.log(Level.WARNING, "impossible de trouver la classe correspondant à {0}", preview);
                    throw new IllegalStateException(ex);
                }
            } else {
                SIRS.LOGGER.log(Level.WARNING, "tous les éléments ne sont pas disponibles pour un changement de linéaire : {0}", preview);
            }
        } else {
            SIRS.LOGGER.log(Level.WARNING, "changement de linéaire non supporté pour {0}", linear);
        }
    }

    /**
     * Méthode d'affectation d'un nouveau {@link TronconDigue} au
     * {@link Positionable}
     *
     * @param troncon nouveau {@link TronconDigue}
     */
    protected void changeTroncon(final TronconDigue troncon) {
        final Positionable positionable = posProperty.get();

        if (positionable instanceof Objet) {

            // Modification du tronçon référencé
            ((Objet) positionable).setLinearId(troncon.getId());

            // Réinitialisation de la géométrie du positionable à celle du tronçon
            resetToLinearGeometry(positionable, troncon);

            // Mise à jour de l'affichage du SR et des PRs
            updateSRAndPRInfo();

            // Mise à jour des différents modes de positionnement géographiques
            for (final FXPositionableMode mode : modes) {
                mode.updateFields();
            }

        } else {
            SIRS.LOGGER.log(Level.WARNING, "élément positionable inattendu {0}", positionable);
        }
    }

    /**
     * Réinitialise la géométrie d'un positionnable à celle du tronçon indiqué.
     *
     * @param positionable
     * @param troncon
     */
    protected void resetToLinearGeometry(final Positionable positionable, final TronconDigue troncon) {

        // Annulation de toute autre information spatiale
        positionable.setBorneDebutId(null);
        positionable.setBorne_debut_aval(true);
        positionable.setBorne_debut_distance(0.);
        positionable.setPositionDebut(null);
        positionable.setBorneFinId(null);
        positionable.setBorne_fin_aval(true);
        positionable.setBorne_fin_distance(0.);
        positionable.setPositionFin(null);

        /*On ne peut pas deviner non plus le nouveau SR
        On lui affecte celui du nouveau tronçon.
        Attention : il faut initialiser cette propritété AVANT la mise à jour de
        la géométrie car les panneaux d'affichage des différents modes sont
        susceptibles de mettre des écouteurs sur la géométrie qui relancent
        le calcul des champs. Or, ce calcul utilise le SR du positionnable.
         */
        positionable.setSystemeRepId(troncon.getSystemeRepDefautId());

        // On ne peut pas deviner la nouvelle géométrie de l'objet.
        // On lui affecte celle du nouveau tronçon.
        positionable.setGeometry(troncon.getGeometry());
    }

    /**
     * Au clic du bouton permet de mettre à jour le SR par défaut et les PR associés.
     * @param event
     */
    @FXML
    void refreshSRAndPRInfo(ActionEvent event) {
            this.updateSRAndPRInfo();
    }

    /**
     * Mise à jour de l'affichage du SR et des PRs sur le bandeau informatif.
     */
    public final void updateSRAndPRInfo() {
        final Positionable pos = getPositionable();
        final SystemeReperage sr;

        if (pos == null) {
            SIRS.LOGGER.log(Level.WARNING, "Impossible de mettre à jour le SR et PR, pour Positionable null");
            uiSR.setText("No SR found.");
            uiPRDebut.setText("");
            uiPRFin.setText("");
            return;
        }

        final SystemeReperageRepository srRepo = (SystemeReperageRepository) Injector.getSession().getRepositoryForClass(SystemeReperage.class);
        final TronconDigue troncon = ConvertPositionableCoordinates.getTronconFromPositionable(pos);

        if (srRepo == null) {
            SIRS.LOGGER.log(Level.WARNING, "Impossible de mettre à jour le SR et PR, Repository srRepo null");
            sr = null;
        } else if (troncon == null) {
            SIRS.LOGGER.log(Level.WARNING, "Impossible de mettre à jour le SR et PR, TronconDigue troncon null");
            sr = null;
        } else {
            sr = srRepo.get(troncon.getSystemeRepDefautId());
        }

        if (sr != null) {
            final LinearReferencing.SegmentInfo[] segments = getSourceLinear(sr);
            final TronconUtils.PosInfo posInfo = new TronconUtils.PosInfo(pos, troncon, segments);

            final Geometry geometry = posInfo.getGeometry();
            if (geometry == null) {
                SIRS.LOGGER.log(Level.WARNING, "Impossible de calculer la g\u00e9om\u00e9trie du positionable sur le sr : \n{0}", sr.toString());
                uiSR.setText(sr.getLibelle());
                uiPRDebut.setText("");
                uiPRFin.setText("");
            } else {
                final Point startPoint, endPoint;
                if (geometry instanceof LineString) {
                    LineString ls = (LineString) geometry;
                    startPoint = ls.getStartPoint();
                    endPoint = ls.getEndPoint();
                } else {
                    startPoint = posInfo.getGeoPointStart();
                    endPoint = posInfo.getGeoPointEnd();
                }

                final AbstractSIRSRepository<BorneDigue> borneRepo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
                final float startPr = TronconUtils.computePR(segments, sr, startPoint, borneRepo);
                final float endPr = TronconUtils.computePR(segments, sr, endPoint, borneRepo);

                uiSR.setText(sr.getLibelle());
                uiPRDebut.setText(PR_FORMAT.format(startPr));
                uiPRFin.setText(PR_FORMAT.format(endPr));
                //on sauvegarde les PR dans le positionable.
                pos.setPrDebut(startPr);
                pos.setPrFin(endPr);
            }

        } else {
            uiSR.setText("No SR found.");
            uiPRDebut.setText("");
            uiPRFin.setText("");
            pos.setPrDebut(0);
            pos.setPrFin(0);
        }

        if (pos.getGeometry() != null) {
            uiGeomInfo.setText(getGeometryInfo(pos.getGeometry()));
        }
    }

    public ObjectProperty<Positionable> positionableProperty() {
        return posProperty;
    }

    public Positionable getPositionable() {
        return posProperty.get();
    }

    public void setPositionable(Positionable positionable) {
        posProperty.set(positionable);
    }

    public BooleanProperty disableFieldsProperty() {
        return disableFieldsProperty;
    }

    public void preSave() {

    }

    /**
     * TODO : check null pointer and computing
     *
     * @param event
     */
    @FXML
    void viewAllSR(ActionEvent event) {
        if (posProperty.get() == null) {
            return;
        }

        final StringBuilder page = new StringBuilder();
        page.append("<html><body>");

        //calcul de la position geographique
        final Positionable pos = getPositionable();
        final TronconDigue troncon = ConvertPositionableCoordinates.getTronconFromPositionable(pos);
        final SystemeReperageRepository srRepo = (SystemeReperageRepository) Injector.getSession().getRepositoryForClass(SystemeReperage.class);

        final SystemeReperage defaultSr;
        if (pos.getSystemeRepId() != null) {
            defaultSr = srRepo.get(pos.getSystemeRepId());
        } else if (troncon.getSystemeRepDefautId() != null) {
            defaultSr = srRepo.get(troncon.getSystemeRepDefautId());
        } else {
            defaultSr = null;
        }

        final LinearReferencing.SegmentInfo[] defaultSegments = getSourceLinear(defaultSr);
        final TronconUtils.PosInfo posInfo = new TronconUtils.PosInfo(pos, troncon, defaultSegments);

        Point startPoint = posInfo.getGeoPointStart();
        Point endPoint = posInfo.getGeoPointEnd();

        if (startPoint == null && endPoint == null) {
            page.append("<h2>No sufficient position information</h2>");
        } else {

            if (startPoint == null) {
                startPoint = endPoint;
            }
            if (endPoint == null) {
                endPoint = startPoint;
            }

            //DataBase coord
            page.append("<h2>Projection de la base (").append(baseCrs.getName()).append(")</h2>");
            page.append("<b>Début</b><br/>");
            page.append("X : ").append(startPoint.getX()).append("<br/>");
            page.append("Y : ").append(startPoint.getY()).append("<br/>");
            page.append("<b>Fin</b><br/>");
            page.append("X : ").append(endPoint.getX()).append("<br/>");
            page.append("Y : ").append(endPoint.getY()).append("<br/>");
            page.append("<br/>");

            //WGS84 coord
            try {
                final MathTransform trs = CRS.findOperation(baseCrs, CRS_WGS84, null).getMathTransform();
                Point ptStart = (Point) JTS.transform(startPoint, trs);
                Point ptEnd = (Point) JTS.transform(endPoint, trs);

                page.append("<h2>Coordonnées géographique (WGS-84, EPSG:4326)</h2>");
                page.append("<b>Début</b><br/>");
                page.append("Longitude : ").append(ptStart.getX()).append("<br/>");
                page.append("Latitude&nbsp : ").append(ptStart.getY()).append("<br/>");
                page.append("<b>Fin</b><br/>");
                page.append("Longitude : ").append(ptEnd.getX()).append("<br/>");
                page.append("Latitude&nbsp : ").append(ptEnd.getY()).append("<br/>");
                page.append("<br/>");
            } catch (FactoryException | TransformException ex) {
                SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }

            final AbstractSIRSRepository<BorneDigue> borneRepo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
            final List<SystemeReperage> srs = srRepo.getByLinear(troncon);

            //pour chaque systeme de reperage
            for (SystemeReperage sr : srs) {

                //============
                // Méthode initiale :
                //============
//                final LinearReferencing.SegmentInfo[] segments = getSourceLinear(sr);
//                Map.Entry<BorneDigue, Double> computedLinear = ConvertPositionableCoordinates.computeLinearFromGeo(segments, sr, startPoint);
//                boolean aval = true;
//                double distanceBorne = computedLinear.getValue();
//                if (distanceBorne < 0) {
//                    distanceBorne = -distanceBorne;
//                    aval = false;
//                }
//                float computedPR = TronconUtils.computePR(getSourceLinear(sr), sr, startPoint, borneRepo);
//
//                page.append("<h2>SR : ").append(sr.getLibelle()).append("</h2>");
//                page.append("<b>Début </b>");
//                page.append(computedLinear.getKey().getLibelle()).append(" à ");
//                page.append(DISTANCE_FORMAT.format(distanceBorne)).append("m ");
//                page.append(aval ? "en aval" : "en amont").append('.');
//                page.append(" Valeur du PR : ").append(computedPR).append('.');
//                page.append("<br/>");
//
//                if (!startPoint.equals(endPoint)) {
//                    computedLinear = ConvertPositionableCoordinates.computeLinearFromGeo(segments, sr, endPoint);
//                    aval = true;
//                    distanceBorne = computedLinear.getValue();
//                    if (distanceBorne < 0) {
//                        distanceBorne = -distanceBorne;
//                        aval = false;
//                    }
//                    computedPR = TronconUtils.computePR(getSourceLinear(sr), sr, endPoint, borneRepo);
//                }
//
//                page.append("<b>Fin&nbsp&nbsp </b>");
//                page.append(computedLinear.getKey().getLibelle()).append(" à ");
//                page.append(DISTANCE_FORMAT.format(distanceBorne)).append("m ");
//                page.append(aval ? "en aval" : "en amont").append('.');
//                page.append(" Valeur du PR : ").append(computedPR).append('.');
//                page.append("<br/><br/>");
                //============
                // Méthode 2 :
                //============
                try {
                    final TronconUtils.PosSR posSr = posInfo.getForSR(sr);
                    float computedPR = TronconUtils.computePR(getSourceLinear(sr), sr, startPoint, borneRepo);

                    page.append("<h2>SR : ").append(sr.getLibelle()).append("</h2>");
                    page.append("<b>Début </b>");
                    page.append(posSr.borneDigueStart.getLibelle()).append(" à ");
                    page.append(DISTANCE_FORMAT.format(posSr.distanceStartBorne)).append("m ");
                    page.append(!posSr.startAval ? "en aval" : "en amont").append('.');
                    page.append(" Valeur du PR : ").append(computedPR).append('.');
                    page.append("<br/>");

                    if (!startPoint.equals(endPoint)) {
                        computedPR = TronconUtils.computePR(getSourceLinear(sr), sr, endPoint, borneRepo);
                    }

                    page.append("<b>Fin&nbsp&nbsp </b>");
                    page.append(posSr.borneDigueEnd.getLibelle()).append(" à ");
                    page.append(DISTANCE_FORMAT.format(posSr.distanceEndBorne)).append("m ");
                    page.append(!posSr.endAval ? "en aval" : "en amont").append('.');  // '!' : Le Positionable indique la position (aval/amont) de la borne. Ici on place le Positionable par rapport à la borne.
                    page.append(" Valeur du PR : ").append(computedPR).append('.');
                    page.append("<br/><br/>");

                } catch (Exception e) {
                    SIRS.LOGGER.log(Level.WARNING, "Echec du calcul de PR pour le système de repérage : " + sr.getLibelle(), e);
                }
            }
        }
        page.append("</html></body>");

        final WebView view = new WebView();
        view.getEngine().loadContent(page.toString());
        view.getEngine().userStyleSheetLocationProperty().set(FXPositionablePane.class.getResource("/fr/sirs/web.css").toString());

        final Dialog dialog = new Dialog();
        final DialogPane pane = new DialogPane();
        pane.setContent(view);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.setDialogPane(pane);
        dialog.setTitle("Position");
        dialog.setOnCloseRequest(event1 -> dialog.hide());
        dialog.show();
    }

}
