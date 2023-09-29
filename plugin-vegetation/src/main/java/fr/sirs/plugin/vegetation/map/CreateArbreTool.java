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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.*;
import fr.sirs.plugin.vegetation.PluginVegetation;
import fr.sirs.theme.ui.FXPositionableExplicitMode;
import fr.sirs.util.ResourceInternationalString;
import fr.sirs.util.SirsStringConverter;
import javafx.collections.FXCollections;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionTool;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionToolSpi;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.edition.EditionTool;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.logging.Level;

import static fr.sirs.SIRS.CSS_PATH;
import static fr.sirs.plugin.vegetation.map.EditVegetationUtils.*;
import static javafx.scene.layout.Region.USE_PREF_SIZE;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CreateArbreTool extends AbstractEditionTool {

    public static final Spi SPI = new Spi();

    public static final class Spi extends AbstractEditionToolSpi {

        public Spi() {
            super("CreateParcelle",
                    new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                            "fr.sirs.plugin.vegetation.map.CreateArbreTool.title", CreateArbreTool.class.getClassLoader()),
                    new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                            "fr.sirs.plugin.vegetation.map.CreateArbreTool.abstract", CreateArbreTool.class.getClassLoader()),
                    new Image("fr/sirs/plugin/vegetation/arbres.png"));
        }

        @Override
        public boolean canHandle(Object candidate) {
            return true;
        }

        @Override
        public EditionTool create(FXMap map, Object layer) {
            return new CreateArbreTool(map);
        }
    }


    //session and repo
    private final Session session;
    private final AbstractSIRSRepository<ParcelleVegetation> parcelleRepo;

    private final MouseListen mouseInputListener = new MouseListen();
    private final BorderPane wizard = new BorderPane();

    private ArbreVegetation arbre = Injector.getSession().getElementCreator().createElement(ArbreVegetation.class);
    private ParcelleVegetation parcelle = null;
    private final Label lblParcelle = new Label();
    private final Label lblPoint = new Label();

    private FeatureMapLayer parcelleLayer = null;

    //Add editable fields ticket redmine 7741
    private final TextField ui_Designation = new TextField();

    // Propriétés de ArbreVegetation
    private final ComboBox<Preview> diametreComboBox = new ComboBox<>();
    private final ComboBox<Preview> hauteurComboBox = new ComboBox<>();

    // Propriétés de ZoneVegetation
    private final CheckBox ui_contactEau =  new CheckBox();
    private final ComboBox<Preview> ui_typePositionId = new ComboBox<>();
    private final ComboBox<Preview> ui_typeCoteId = new ComboBox<>();
    private final ComboBox<Preview> ui_etatSanitaireId = new ComboBox<>();
    private final ComboBox<Preview> ui_typeArbreId = new ComboBox<>();
    private final ComboBox<Preview> ui_especeId = new ComboBox<>();
    private final TextArea ui_commentaire = new TextArea();

    public CreateArbreTool(FXMap map) {
        super(SPI);
        wizard.getStylesheets().add(CSS_PATH);

        session = Injector.getSession();
        parcelleRepo = session.getRepositoryForClass(ParcelleVegetation.class);

        wizard.getStyleClass().add("blue-light");
        lblParcelle.getStyleClass().add("label-text");
        lblPoint.getStyleClass().add("label-text");
        lblPoint.setWrapText(true);
        lblParcelle.setWrapText(true);

        if (arbre != null ) {
            ui_Designation.setText(arbre.getDesignation());
            ui_commentaire.setText(arbre.getCommentaire());
        }
        ui_commentaire.setPrefHeight(100);

        final GridPane attributeGrid = new GridPane();
        attributeGrid.setHgap(2);
        attributeGrid.setVgap(6);

        attributeGrid.add(generateHeaderLabel(LABEL_CONTACT_EAU),0,0);
        attributeGrid.add(ui_contactEau,1,0);
        attributeGrid.add(generateHeaderLabel(LABEL_POSITION_ID),0,1);
        attributeGrid.add(ui_typePositionId,1,1);
        attributeGrid.add(generateHeaderLabel(LABEL_COTE_ID),0,2);
        attributeGrid.add(ui_typeCoteId,1,2);

        final GridPane attributeGrid2 = new GridPane();
        attributeGrid2.setHgap(2);
        attributeGrid2.setVgap(6);
        attributeGrid2.add(generateHeaderLabel(LABEL_HAUTEUR),0,0);
        attributeGrid2.add(hauteurComboBox,1,0);
        attributeGrid2.add(generateHeaderLabel(LABEL_DIAMETRE),0,1);
        attributeGrid2.add(diametreComboBox,1,1);
        attributeGrid2.add(generateHeaderLabel(LABEL_ETAT_SANITAIRE_ID),0,2);
        attributeGrid2.add(ui_etatSanitaireId,1,2);
        attributeGrid2.add(generateHeaderLabel(LABEL_TYPE_ARBRE),0,3);
        attributeGrid2.add(ui_typeArbreId,1,3);
        attributeGrid2.add(generateHeaderLabel(LABEL_ESPECE_ID),0,4);
        attributeGrid2.add(ui_especeId,1,4);

        final VBox vbox = new VBox(15,
                new HBox(15, generateHeaderLabel("Parcelle :"), lblParcelle),
                new HBox(15, generateHeaderLabel("Géométrie :"), lblPoint),
                new HBox(15, generateHeaderLabel("ATTENTION, remplir les champs suivant AVANT de positionner l'arbre sur la carte.")),
                new HBox(15, generateHeaderLabel(LABEL_DESIGNATION), ui_Designation),
                attributeGrid,
                attributeGrid2,
                new HBox(15, generateHeaderLabel(LABEL_COMMENTAIRE), ui_commentaire)
        );
        vbox.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        wizard.setCenter(vbox);

        //Add editable fields ticket redmine 7741
        final Previews previewRepository = Injector.getSession().getPreviews();
        SIRS.initCombo(diametreComboBox, FXCollections.observableList(previewRepository.getByClass(RefDiametreVegetation.class)), arbre == null ? null : arbre.getDiametreId());
        SIRS.initCombo(hauteurComboBox,  FXCollections.observableList(previewRepository.getByClass(RefHauteurVegetation.class)), arbre == null ? null : arbre.getHauteurId());
        SIRS.initCombo(ui_typePositionId, FXCollections.observableList(previewRepository.getByClass(RefPosition.class)), arbre == null ? null : arbre.getTypePositionId());
        SIRS.initCombo(ui_typeCoteId,  FXCollections.observableList(previewRepository.getByClass(RefCote.class)), arbre == null ? null : arbre.getTypeCoteId());
        SIRS.initCombo(ui_etatSanitaireId,  FXCollections.observableList(previewRepository.getByClass(RefEtatSanitaireVegetation.class)), arbre == null ? null : arbre.getEtatSanitaireId());
        SIRS.initCombo(ui_typeArbreId,  FXCollections.observableList(previewRepository.getByClass(RefTypeArbreVegetation.class)), arbre == null ? null : arbre.getTypeArbreId());
        SIRS.initCombo(ui_especeId,  FXCollections.observableList(previewRepository.getByClass(RefEspeceArbreVegetation.class)), arbre == null ? null : arbre.getEspeceId());

    }


    private void saveAction(final Geometry ptToSet ) {
        // editedGeoCoordinate set to false by default when creating Positionable via carto.
        // not enough to differentiate from a positionable edited via its bornes or the carto.
        arbre.setCartoEdited(true);
        arbre.setExplicitGeometry(ptToSet);
        arbre.setGeometry(ptToSet);
        arbre.setGeometryType(GeometryType.PONCTUAL);
        arbre.setGeometryMode(FXPositionableExplicitMode.MODE);
        arbre.setValid(true);
        arbre.setForeignParentId(parcelle.getDocumentId());

        arbre.setDesignation(ui_Designation.getText());
        arbre.setHauteurId(getElementIdOrnull(hauteurComboBox));
        arbre.setDiametreId(getElementIdOrnull(diametreComboBox));
        arbre.setTypePositionId(getElementIdOrnull(ui_typePositionId));
        arbre.setTypeCoteId(getElementIdOrnull(ui_typeCoteId));
        arbre.setContactEau(ui_contactEau.isSelected());
        arbre.setEtatSanitaireId(getElementIdOrnull(ui_etatSanitaireId));
        arbre.setTypeArbreId(getElementIdOrnull(ui_typeArbreId));
        arbre.setEspeceId(getElementIdOrnull(ui_especeId));

        arbre.setCommentaire(ui_commentaire.getText());

        final AbstractSIRSRepository<ArbreVegetation> arbreRepo = session.getRepositoryForClass(ArbreVegetation.class);
        arbreRepo.add(arbre);
    }

    private void reset() {
        diametreComboBox.getSelectionModel().clearSelection();
        hauteurComboBox.getSelectionModel().clearSelection();
        ui_typePositionId.getSelectionModel().clearSelection();
        ui_typeCoteId.getSelectionModel().clearSelection();
        ui_etatSanitaireId.getSelectionModel().clearSelection();
        ui_typeArbreId.getSelectionModel().clearSelection();
        ui_especeId.getSelectionModel().clearSelection();
        ui_contactEau.setSelected(false);
        arbre = Injector.getSession().getElementCreator().createElement(ArbreVegetation.class);
        arbre.setTraitement(Injector.getSession().getElementCreator().createElement(TraitementZoneVegetation.class));
        ui_Designation.setText(arbre.getDesignation());
        parcelle = null;
        lblParcelle.setText("Sélectionner une parcelle sur la carte");
        lblPoint.setText("");
        ui_commentaire.setText(null);
        if (parcelleLayer != null) parcelleLayer.setSelectionFilter(null);
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
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, mouseInputListener);

        //on rend les couches troncon et borne selectionnables
        final MapContext context = component.getContainer().getContext();
        for (MapLayer layer : context.layers()) {
            if (layer.getName().equalsIgnoreCase(PluginVegetation.PARCELLE_LAYER_NAME)) {
                parcelleLayer = (FeatureMapLayer) layer;
            }
        }
        component.setCursor(Cursor.CROSSHAIR);
    }

    @Override
    public boolean uninstall(FXMap component) {
        super.uninstall(component);
        component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
        component.setCursor(Cursor.DEFAULT);
        reset();
        return true;
    }

    private class MouseListen extends FXPanMouseListen {

        private final SirsStringConverter cvt = new SirsStringConverter();

        public MouseListen() {
            super(CreateArbreTool.this);
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            mousebutton = event.getButton();
            if (mousebutton != MouseButton.PRIMARY) {
                super.mouseClicked(event);
                return;
            }

            final Rectangle2D clickArea = new Rectangle2D.Double(event.getX() - 2, event.getY() - 2, 4, 4);

            if (parcelle == null) {
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
                            lblParcelle.setText(cvt.toString(parcelle));
                            lblPoint.setText("Cliquer sur la carte pour créer la géométrie");
                            parcelleLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(Collections.singleton(graphic.getCandidate().getIdentifier())));

                        }
                    }

                    @Override
                    public boolean isStopRequested() {
                        return parcelle != null;
                    }

                    @Override
                    public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {
                    }
                }, VisitFilter.INTERSECTS);
            } else if (parcelle != null) {
                parcelleLayer.setSelectable(false);

                //on crée l'arbre
                final EditionHelper helper = new EditionHelper(map, parcelleLayer);
                final Coordinate coord = helper.toCoord(event.getX(), event.getY());
                Geometry pt = GO2Utilities.JTS_FACTORY.createPoint(coord);
                JTS.setCRS(pt, map.getCanvas().getObjectiveCRS2D());
                try {
                    //on s'assure d'etre dans le crs de la base
                    pt = JTS.transform(pt, CRS.findOperation(JTS.findCoordinateReferenceSystem(pt), session.getProjection(), null).getMathTransform());
                } catch (FactoryException | TransformException ex) {
                    SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }

                saveAction(pt);

                final ParcelleVegetation p = parcelle;
                reset();
                parcelle = p;
                lblParcelle.setText(cvt.toString(parcelle));
                lblPoint.setText("Cliquer sur la carte pour créer la géométrie");

            }

        }

    }

}
