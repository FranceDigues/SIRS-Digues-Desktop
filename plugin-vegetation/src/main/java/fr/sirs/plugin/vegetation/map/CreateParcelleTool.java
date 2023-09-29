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

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import static fr.sirs.SIRS.CSS_PATH;
import fr.sirs.Session;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.ParcelleVegetation;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import static fr.sirs.plugin.vegetation.PluginVegetation.ajustPlanifSize;
import fr.sirs.plugin.vegetation.VegetationSession;
import fr.sirs.theme.ui.FXPositionableLinearMode;
import fr.sirs.util.ResourceInternationalString;
import fr.sirs.util.SirsStringConverter;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import static fr.sirs.plugin.vegetation.map.EditVegetationUtils.LABEL_DESIGNATION;
import static fr.sirs.plugin.vegetation.map.EditVegetationUtils.generateHeaderLabel;
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
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionTool;
import org.geotoolkit.gui.javafx.render2d.edition.AbstractEditionToolSpi;
import org.geotoolkit.gui.javafx.render2d.edition.EditionTool;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.opengis.filter.identity.FeatureId;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CreateParcelleTool extends AbstractEditionTool{

    public static final Spi SPI = new Spi();
    public static final class Spi extends AbstractEditionToolSpi{

        public Spi() {
            super("CreateParcelle",
                new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                        "fr.sirs.plugin.vegetation.map.CreateParcelleTool.title",CreateParcelleTool.class.getClassLoader()),
                new ResourceInternationalString("fr/sirs/plugin/vegetation/bundle",
                        "fr.sirs.plugin.vegetation.map.CreateParcelleTool.abstract",CreateParcelleTool.class.getClassLoader()),
                new Image("fr/sirs/plugin/vegetation/parcelle.png"));
        }

        @Override
        public boolean canHandle(Object candidate) {
            return true;
        }

        @Override
        public EditionTool create(FXMap map, Object layer) {
            return new CreateParcelleTool(map);
        }
    }


    //session and repo
    private final Session session = Injector.getSession();
    private final AbstractSIRSRepository<BorneDigue> borneRepo = session.getRepositoryForClass(BorneDigue.class);
    private final AbstractSIRSRepository<TronconDigue> tronconRepo = session.getRepositoryForClass(TronconDigue.class);
    private final AbstractSIRSRepository<SystemeReperage> srRepo = session.getRepositoryForClass(SystemeReperage.class);
    private final AbstractSIRSRepository<ParcelleVegetation> parcelleRepo = session.getRepositoryForClass(ParcelleVegetation.class);

    private final MouseListen mouseInputListener = new MouseListen();
    private final BorderPane wizard = new BorderPane();

    private PlanVegetation plan;
    private ParcelleVegetation parcelle = parcelleRepo.create();
    private TronconDigue tronconDigue = null;
    private final Label lblTroncon = new Label();
    private final TextField ui_Designation = new TextField();
    private final Label lblFirstPoint = new Label();
    private final Label lblLastPoint = new Label();
    private final Button end = new Button("Enregistrer");

    private FeatureMapLayer tronconLayer = null;
    private FeatureMapLayer borneLayer = null;

    /** List of layers deactivated on tool install. They will be activated back at uninstallation. */
    private List<MapLayer> toActivateBack;
    
    public CreateParcelleTool(FXMap map) {
        super(SPI);
        wizard.getStylesheets().add(CSS_PATH);

        end.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                parcelle.setValid(true);
                parcelle.setPlanId(plan.getId());
                parcelle.setModeAuto(true);
                parcelle.setGeometryMode(FXPositionableLinearMode.MODE);

                parcelle.setDesignation(ui_Designation.getText());

                //calcule de la geometrie
                parcelle.setGeometry(LinearReferencingUtilities.buildGeometry(
                        tronconDigue.getGeometry(), parcelle, session.getRepositoryForClass(BorneDigue.class)));

                //recuperation des PR
                final String srId = tronconDigue.getSystemeRepDefautId();
                final SystemeReperage sr = srRepo.get(srId);

                for(SystemeReperageBorne srb : sr.getSystemeReperageBornes()){
                    if(srb.getBorneId().equals(parcelle.getBorneDebutId())){
                        parcelle.setPrDebut(srb.getValeurPR());
                    }else if(srb.getBorneId().equals(parcelle.getBorneFinId())){
                        parcelle.setPrFin(srb.getValeurPR());
                    }
                }

                //configuration de base des planifs
                ajustPlanifSize(parcelle, plan.getAnneeFin() - plan.getAnneeDebut());

                //sauvegarde
                parcelleRepo.add(parcelle);
                map.setHandler(new FXPanHandler(true));
                reset();
            }
        });
        Button cancel = new Button("Annuler");
        cancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                reset();
                map.setHandler(new FXPanHandler(true));
            }
        });

        lblTroncon.getStyleClass().add("label-text");
        lblFirstPoint.getStyleClass().add("label-text");
        lblLastPoint.getStyleClass().add("label-text");
        end.getStyleClass().add("btn-single");
        cancel.getStyleClass().add("btn-single");
        wizard.getStyleClass().add("blue-light");

        final VBox vbox = new VBox(15,
                generateHeaderLabel("Tronçon :"),
                lblTroncon,
                generateHeaderLabel(LABEL_DESIGNATION),
                ui_Designation,
                generateHeaderLabel("Borne de début :"),
                lblFirstPoint,
                generateHeaderLabel("Borne de fin :"),
                lblLastPoint,
                new HBox(30, end, cancel));
        vbox.setMaxSize(USE_PREF_SIZE,USE_PREF_SIZE);
        wizard.setCenter(vbox);
    }

    private void reset(){
        parcelle = parcelleRepo.create();
        ui_Designation.setText(parcelle.getDesignation());
        parcelle.setModeAuto(true);
        end.disableProperty().unbind();
        end.disableProperty().bind( parcelle.linearIdProperty().isNull()
                                .or(parcelle.borneDebutIdProperty().isNull()
                                .or(parcelle.borneFinIdProperty().isNull())));
        lblTroncon.setText("Sélectionner un tronçon sur la carte");
        lblFirstPoint.setText("");
        lblLastPoint.setText("");

        if(tronconLayer!=null) tronconLayer.setSelectionFilter(null);
        if(borneLayer!=null) borneLayer.setSelectionFilter(null);
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

        //on vérifie qu'il y a une plan de gestion actif
        plan = VegetationSession.INSTANCE.planProperty().get();
        if(plan==null){
            final Dialog dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setContentText("Veuillez activer un plan de gestion avant de commencer l'édition.");
            dialog.showAndWait();

            component.setHandler(new FXPanHandler(true));
            return;
        }

        component.addEventHandler(MouseEvent.ANY, mouseInputListener);

        // On instancie une nouvelle liste pour les couches à désactiver provisoirement (le temps de l'activation de l'outil)
        toActivateBack = new ArrayList<>();
        
        //on rend les couches troncon et borne selectionnables
        final MapContext context = component.getContainer().getContext();
        for(MapLayer layer : context.layers()){
            if(layer.getName().equalsIgnoreCase(CorePlugin.TRONCON_LAYER_NAME)){
                tronconLayer = (FeatureMapLayer) layer;
            } else if(layer.getName().equalsIgnoreCase(CorePlugin.BORNE_LAYER_NAME)){
                borneLayer = (FeatureMapLayer) layer;
            } else if (layer.isSelectable()) {
                layer.setSelectable(false);
                toActivateBack.add(layer);
            }
        }
        component.setCursor(Cursor.CROSSHAIR);
    }

    @Override
    public boolean uninstall(FXMap component) {
        super.uninstall(component);
        if (toActivateBack != null) {
            for (final MapLayer layer : toActivateBack) {
                layer.setSelectable(true);
            }
        }
        component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
        component.setCursor(Cursor.DEFAULT);
        reset();
        return true;
    }

    private class MouseListen extends FXPanMouseListen {

        private final SirsStringConverter cvt = new SirsStringConverter();

        public MouseListen() {
            super(CreateParcelleTool.this);
        }

        @Override
        public void mouseClicked(MouseEvent event) {

            final Rectangle2D clickArea = new Rectangle2D.Double(event.getX()-2, event.getY()-2, 4, 4);

            if(parcelle.getLinearId()==null || parcelle.getLinearId().isEmpty()){
                tronconLayer.setSelectable(true);
                borneLayer.setSelectable(false);
                //recherche un troncon sous la souris
                map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                    @Override
                    public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                        final Object bean = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                        if(bean instanceof TronconDigue){
                            tronconDigue = (TronconDigue) bean;
                            //on recupere l'object complet
                            tronconDigue = tronconRepo.get(tronconDigue.getDocumentId());
                            parcelle.setLinearId(tronconDigue.getId());
                            lblTroncon.setText(cvt.toString(tronconDigue));
                            lblFirstPoint.setText("Sélectionner une borne sur la carte");

                            //troncon et bornes sur la carte
                            tronconLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(Collections.singleton(graphic.getCandidate().getIdentifier())));
                            final Set<FeatureId> borneIds = new HashSet<>();
                            for(String str : tronconDigue.getBorneIds()) borneIds.add(new DefaultFeatureId(str));
                            borneLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(borneIds));
                        }
                    }
                    @Override
                    public boolean isStopRequested() {
                        return parcelle.getLinearId()!=null && !parcelle.getLinearId().isEmpty();
                    }
                    @Override
                    public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
                }, VisitFilter.INTERSECTS);
            }else if(parcelle.getBorneDebutId()==null || parcelle.getBorneDebutId().isEmpty()){
                tronconLayer.setSelectable(false);
                borneLayer.setSelectable(true);
                //recherche une borne sous la souris
                map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                    @Override
                    public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                        final Object bean = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                        if(bean instanceof BorneDigue && tronconDigue.getBorneIds().contains(((BorneDigue)bean).getId())){
                            parcelle.setBorneDebutId(((BorneDigue)bean).getId());
                            lblFirstPoint.setText(cvt.toString(bean));
                            lblLastPoint.setText("Sélectionner une borne sur la carte");
                        }
                    }
                    @Override
                    public boolean isStopRequested() {
                        return parcelle.getBorneDebutId()!=null && !parcelle.getBorneDebutId().isEmpty();
                    }
                    @Override
                    public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
                }, VisitFilter.INTERSECTS);
            }
            else if(parcelle.getBorneFinId()==null || parcelle.getBorneFinId().isEmpty()){
                tronconLayer.setSelectable(false);
                borneLayer.setSelectable(true);
                //recherche une borne sous la souris
                map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                    @Override
                    public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                        final Object bean = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                        if(bean instanceof BorneDigue && tronconDigue.getBorneIds().contains(((BorneDigue)bean).getId())){
                            parcelle.setBorneFinId(((BorneDigue)bean).getId());
                            lblLastPoint.setText(cvt.toString(bean));
                        }
                    }
                    @Override
                    public boolean isStopRequested() {
                        return parcelle.getBorneFinId()!=null && !parcelle.getBorneFinId().isEmpty();
                    }
                    @Override
                    public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
                }, VisitFilter.INTERSECTS);
            }

        }

    }

}
