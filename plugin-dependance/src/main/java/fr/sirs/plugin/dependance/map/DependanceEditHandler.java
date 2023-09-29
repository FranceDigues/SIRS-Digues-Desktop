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
package fr.sirs.plugin.dependance.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.AireStockageDependanceRepository;
import fr.sirs.core.component.AmenagementHydrauliqueRepository;
import fr.sirs.core.component.AutreDependanceRepository;
import fr.sirs.core.component.CheminAccesDependanceRepository;
import fr.sirs.core.component.OuvrageVoirieDependanceRepository;
import fr.sirs.core.model.AbstractDependance;
import fr.sirs.core.model.AireStockageDependance;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.AutreDependance;
import fr.sirs.core.model.CheminAccesDependance;
import fr.sirs.core.model.OuvrageVoirieDependance;
import fr.sirs.plugin.dependance.PluginDependance;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javafx.scene.control.Alert.AlertType.CONFIRMATION;
import static javafx.scene.control.ButtonType.NO;
import static javafx.scene.control.ButtonType.YES;
import javafx.scene.control.ChoiceDialog;

/**
 * Contrôle les actions possibles pour le bouton d'édition et de modification de dépendances
 * sur la carte.
 *
 * @author Cédric Briançon (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class DependanceEditHandler extends AbstractNavigationHandler {
    private final MouseListen mouseInputListener = new MouseListen();
    private final FXGeometryLayer decorationLayer = new FXGeometryLayer();
    private Stage creationDependanceStage;

    /**
     * Couches présentant les dépendances sur la carte.
     */
    private FeatureMapLayer aireLayer;
    private FeatureMapLayer autreLayer;
    private FeatureMapLayer cheminLayer;
    private FeatureMapLayer ouvrageVoirieLayer;
    private FeatureMapLayer amenagementLayer;

    /**
     * La dépendance en cours.
     */
    private AbstractDependance dependance;

    /**
     * Outil d'aide pour éditer une {@linkplain #editGeometry géométrie} existante.
     */
    private EditionHelper helper;

    /**
     * Géométrie en cours d'édition.
     */
    private final EditionHelper.EditionGeometry editGeometry = new EditionHelper.EditionGeometry();

    /**
     * Coordonnées de la {@linkplain #editGeometry géométrie}.
     */
    private final List<Coordinate> coords = new ArrayList<>();

    /**
     * Vrai si une dépendance vient d'être créée.
     */
    private boolean newDependance = false;

    /**
     * Définit le type de géométries à dessiner, pour les dépendances de types "ouvrages de voirie" ou "autres"
     * pour lesquelles plusieurs choix sont possibles.
     */
    private Class newGeomType = Point.class;

    /**
     * Vrai si la {@linkplain #coords liste des coordonnées} de la {@linkplain #editGeometry géométrie}
     * vient d'être créée.
     */
    private boolean justCreated = false;

    public DependanceEditHandler() {
        super();
    }

    public DependanceEditHandler(final AbstractDependance dependance) {
        this();
        this.dependance = dependance;
        newDependance = true;

        if (dependance.getGeometry() != null) {
            editGeometry.geometry.set((Geometry)dependance.getGeometry().clone());
            decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
            newDependance = false;
        }else{
            //choix du type de géometrie
            if(dependance instanceof AireStockageDependance){
                newGeomType = Polygon.class;
            }else if(dependance instanceof CheminAccesDependance){
                newGeomType = LineString.class;
            }else {
                //on demande a l'utilisateur
                final ChoiceDialog<String> choice = new ChoiceDialog<>("Ponctuel","Linéaire","Surfacique");
                choice.setHeaderText("Choix de la forme géométrique de la dépendance.");
                choice.setTitle("Type de géométrie");
                final Optional<String> showAndWait = choice.showAndWait();
                if(showAndWait.isPresent()){
                    switch (showAndWait.get()) {
                        case "Ponctuel" : newGeomType = Point.class; break;
                        case "Linéaire" : newGeomType = LineString.class; break;
                        case "Surfacique" : newGeomType = Polygon.class; break;
                        default: newGeomType = Point.class;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(FXMap component) {
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(Cursor.CROSSHAIR);
        map.addDecoration(0, decorationLayer);

        aireLayer = PluginDependance.getAireLayer();
        autreLayer = PluginDependance.getAutreLayer();
        cheminLayer = PluginDependance.getCheminLayer();
        ouvrageVoirieLayer = PluginDependance.getOuvrageVoirieLayer();
        amenagementLayer = PluginDependance.getAmenagementLayer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uninstall(FXMap component) {
        if (editGeometry.geometry.get() == null) {
            super.uninstall(component);
            component.removeDecoration(decorationLayer);
            component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
            component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
            return true;
        }

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la fin du mode édition ? Les modifications non sauvegardées seront perdues.",
                ButtonType.YES,ButtonType.NO);
        if (ButtonType.YES.equals(alert.showAndWait().get())) {
            super.uninstall(component);
            component.removeDecoration(decorationLayer);
            component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
            component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
            return true;
        }

        return false;
    }


    /**
     * Ecoute les évènements lancés par la souris.
     */
    private class MouseListen extends FXPanMouseListen {
        private final ContextMenu popup = new ContextMenu();
        private MouseButton pressed;

        public MouseListen() {
            super(DependanceEditHandler.this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            final double x = e.getX();
            final double y = e.getY();

            if (MouseButton.PRIMARY.equals(e.getButton())) {
                if (dependance == null) {
                    // Recherche d'une couche de la carte qui contiendrait une géométrie là où l'utilisateur a cliqué
                    final Rectangle2D clickArea = new Rectangle2D.Double(x - 2, y - 2, 4, 4);

                    //recherche d'un object a editer
                    map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                        @Override
                        public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                            final Object bean = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                            if(bean instanceof AbstractDependance){
                                helper = new EditionHelper(map, graphic.getLayer());
                                dependance = (AbstractDependance)bean;
                                // On récupère la géométrie de cet objet pour passer en mode édition
                                editGeometry.geometry.set((Geometry)dependance.getGeometry().clone());
                                // Ajout de cette géométrie dans la couche d'édition sur la carte.
                                decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                                newDependance = false;
                            }
                        }
                        @Override
                        public boolean isStopRequested() {
                            return helper!=null;
                        }
                        @Override
                        public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
                    }, VisitFilter.INTERSECTS);
                } else {
                    // La dépendance existe, on peut travailler avec sa géométrie.
                    if (newDependance) {
                        // Le helper peut être null si on a choisi d'activer ce handler pour une dépendance existante,
                        // sans passer par le clic droit pour choisir un type de dépendance.
                        final Class clazz = dependance.getClass();
                        if (helper == null) {
                            if (AireStockageDependance.class.isAssignableFrom(clazz)) {
                                helper = new EditionHelper(map, aireLayer);
                            } else if (AutreDependance.class.isAssignableFrom(clazz)) {
                                helper = new EditionHelper(map, autreLayer);
                            } else if (CheminAccesDependance.class.isAssignableFrom(clazz)) {
                                helper = new EditionHelper(map, cheminLayer);
                            } else if (OuvrageVoirieDependance.class.isAssignableFrom(clazz)) {
                                helper = new EditionHelper(map, ouvrageVoirieLayer);
                            } else if (AmenagementHydraulique.class.isAssignableFrom(clazz)) {
                                helper = new EditionHelper(map, amenagementLayer);
                            }
                        }

                        // La classe de l'objet à dessiner est un polygone pour une aire de stockage, une ligne pour un chemin d'accès
                        // sinon c'est le choix fait par l'utilisateur dans le panneau de création de dépendance.
                        final Class geomClass = AireStockageDependance.class.isAssignableFrom(clazz) ? Polygon.class :
                                                CheminAccesDependance.class.isAssignableFrom(clazz) ? LineString.class : newGeomType;

                        // On vient de créer la dépendance, le clic gauche va permettre d'ajouter des points.
                        if (Point.class.isAssignableFrom(geomClass)) {
                            coords.clear();
                            coords.add(helper.toCoord(x,y));
                        } else {
                            if (justCreated) {
                                justCreated = false;
                                //we must modify the second point since two point where added at the start
                                if (Polygon.class.isAssignableFrom(geomClass)) {
                                    coords.remove(2);
                                }
                                coords.remove(1);
                                coords.add(helper.toCoord(x, y));
                                if (Polygon.class.isAssignableFrom(geomClass)) {
                                    coords.add(helper.toCoord(x, y));
                                }
                            } else if (coords.isEmpty()) {
                                justCreated = true;
                                //this is the first point of the geometry we create
                                //add 3 points that will be used when moving the mouse around for polygons,
                                //for lines just add 2 points.
                                coords.add(helper.toCoord(x, y));
                                coords.add(helper.toCoord(x, y));
                                if (Polygon.class.isAssignableFrom(geomClass)) {
                                    coords.add(helper.toCoord(x, y));
                                }
                            } else {
                                // On ajoute le point en plus.
                                justCreated = false;
                                coords.add(helper.toCoord(x, y));
                            }
                        }

                        // Création de la géométrie à éditer à partir des coordonnées
                        if (Polygon.class.isAssignableFrom(geomClass)) {
                            editGeometry.geometry.set(EditionHelper.createPolygon(coords));
                        } else if (LineString.class.isAssignableFrom(geomClass)) {
                            editGeometry.geometry.set(EditionHelper.createLine(coords));
                        } else {
                            editGeometry.geometry.set(EditionHelper.createPoint(coords.get(0)));
                        }
                        JTS.setCRS(editGeometry.geometry.get(), map.getCanvas().getObjectiveCRS2D());
                        decorationLayer.getGeometries().setAll(editGeometry.geometry.get());


                        if (Point.class.isAssignableFrom(geomClass)) {
                            // Pour un nouveau point ajouté, on termine l'édition directement.
                            dependance.setGeometry(editGeometry.geometry.get());
                            final AbstractSIRSRepository repodep = Injector.getSession().getRepositoryForClass(dependance.getClass());

                            if (dependance.getDocumentId() != null) {
                                repodep.update(dependance);
                            } else {
                                repodep.add(dependance);
                            }
                            // On quitte le mode d'édition.
                            reset();
                        }
                    } else {
                        // On réédite une géométrie existante, le double clic gauche va nous permettre d'ajouter un nouveau
                        // point à la géométrie, si ce n'est pas un point.
                        final Geometry tempEditGeom = editGeometry.geometry.get();
                        if (!Point.class.isAssignableFrom(tempEditGeom.getClass()) && e.getClickCount() >= 2) {
                            final Geometry result;
                            if (tempEditGeom instanceof Polygon) {
                                result = helper.insertNode((Polygon)editGeometry.geometry.get(), x, y);
                            } else {
                                result = helper.insertNode((LineString)editGeometry.geometry.get(), x, y);
                            }
                            editGeometry.geometry.set(result);
                            decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                        }
                    }
                }
            } else if (MouseButton.SECONDARY.equals(e.getButton())) {
                if (dependance == null) {
                    if (creationDependanceStage == null) initCreationDependanceStage();
                    creationDependanceStage.showAndWait();
                } else {
                    // popup :
                    // -suppression d'un noeud
                    // -sauvegarder
                    // -annuler édition
                    // -supprimer dépendance
                    popup.getItems().clear();

                    final Class clazz = dependance.getClass();
                    if (AireStockageDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, aireLayer);
                    } else if (AutreDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, autreLayer);
                    } else if (CheminAccesDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, cheminLayer);
                    } else if (OuvrageVoirieDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, ouvrageVoirieLayer);
                    } else if (AmenagementHydraulique.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, amenagementLayer);
                    }

                    //action : suppression d'un noeud
                    if (editGeometry.geometry.get() != null){
                        helper.grabGeometryNode(x, y, editGeometry);
                        if (editGeometry.selectedNode[0] >= 0) {
                            final MenuItem item = new MenuItem("Supprimer noeud");
                            item.setOnAction((ActionEvent event) -> {
                                // coord is fill only if suppression concerns a new item
                                if (coords.size() > editGeometry.selectedNode[0]) {
                                    coords.remove(editGeometry.selectedNode[0]);
                                }
                                editGeometry.deleteSelectedNode();
                                decorationLayer.setNodeSelection(null);
                                decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                            });
                            popup.getItems().add(item);
                        }
                    }

                    // action : sauvegarde
                    // Sauvegarde de la dépendance de stockage ainsi que sa géométrie qui a éventuellement été éditée.
                    final MenuItem saveItem = new MenuItem("Sauvegarder");
                    saveItem.setOnAction((ActionEvent event) -> {
                        dependance.setGeometry(editGeometry.geometry.get());
                        final AbstractSIRSRepository repodep = Injector.getSession().getRepositoryForClass(dependance.getClass());

                        if (dependance.getDocumentId() != null) {
                            repodep.update(dependance);
                        } else {
                            repodep.add(dependance);
                        }
                        // On quitte le mode d'édition.
                        reset();
                    });
                    popup.getItems().add(saveItem);

                    // action : annuler édition
                    final MenuItem cancelItem = new MenuItem("Annuler l'édition");
                    cancelItem.setOnAction(event -> {
                        reset();
                    });
                    popup.getItems().add(cancelItem);

                    // action : suppression dépendance
                    final MenuItem deleteItem = new MenuItem("Supprimer dépendance", new ImageView(GeotkFX.ICON_DELETE));
                    deleteItem.setOnAction((ActionEvent event) -> {
                        final Alert alert = new Alert(CONFIRMATION, "Voulez-vous vraiment supprimer la dépendance sélectionnée ?", YES, NO);
                        final Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get()==YES) {
                            if (AireStockageDependance.class.isAssignableFrom(dependance.getClass())) {
                                Injector.getBean(AireStockageDependanceRepository.class).remove((AireStockageDependance)dependance);
                            } else if (AutreDependance.class.isAssignableFrom(dependance.getClass())) {
                                Injector.getBean(AutreDependanceRepository.class).remove((AutreDependance)dependance);
                            } else if (CheminAccesDependance.class.isAssignableFrom(dependance.getClass())) {
                                Injector.getBean(CheminAccesDependanceRepository.class).remove((CheminAccesDependance)dependance);
                            } else if (OuvrageVoirieDependance.class.isAssignableFrom(dependance.getClass())) {
                                Injector.getBean(OuvrageVoirieDependanceRepository.class).remove((OuvrageVoirieDependance)dependance);
                            } else if (AmenagementHydraulique.class.isAssignableFrom(dependance.getClass())) {
                                Injector.getBean(AmenagementHydrauliqueRepository.class).remove((AmenagementHydraulique)dependance);
                            }
                            // On quitte le mode d'édition.
                            reset();
                        }
                    });
                    popup.getItems().add(deleteItem);

                    popup.show(decorationLayer, Side.TOP, x, y);
                }
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            pressed = e.getButton();

            if(dependance != null && !newDependance && pressed == MouseButton.PRIMARY){
                // On va sélectionner un noeud sur lequel l'utilisateur a cliqué, s'il y en a un.

                // Le helper peut être null si on a choisi d'activer ce handler pour une dépendance existante,
                // sans passer par le clic droit pour choisir un type de dépendance.
                final Class clazz = dependance.getClass();
                if (helper == null) {
                    if (AireStockageDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, aireLayer);
                    } else if (AutreDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, autreLayer);
                    } else if (CheminAccesDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, cheminLayer);
                    } else if (OuvrageVoirieDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, ouvrageVoirieLayer);
                    } else if (AmenagementHydraulique.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, amenagementLayer);
                    }
                }
                helper.grabGeometryNode(e.getX(), e.getY(), editGeometry);
                decorationLayer.setNodeSelection(editGeometry);
            }

            super.mousePressed(e);
        }

        @Override
        public void mouseDragged(final MouseEvent e) {

            if(dependance != null && !newDependance && pressed == MouseButton.PRIMARY){
                // On déplace le noeud sélectionné
                editGeometry.moveSelectedNode(helper.toCoord(e.getX(), e.getY()));
                decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                return;
            }

            super.mouseDragged(e);
        }
    }

    /**
     * Réinitialise la carte et vide la géométrie en cours d'édition.
     */
    private void reset() {
        newDependance = false;
        justCreated = false;
        decorationLayer.getGeometries().clear();
        decorationLayer.setNodeSelection(null);
        coords.clear();
        editGeometry.reset();
        dependance = null;
        helper = null;
        creationDependanceStage = null;
    }

    private void initCreationDependanceStage() {
        // La dépendance n'existe pas, on en créé une nouvelle après avoir choisi son type et le type de géométrie
        // à dessiner
        creationDependanceStage = new Stage();
        creationDependanceStage.getIcons().add(SIRS.ICON);
        creationDependanceStage.setTitle("Création de dépendance");
        creationDependanceStage.initModality(Modality.WINDOW_MODAL);
        creationDependanceStage.setAlwaysOnTop(true);
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(5);
        gridPane.setPadding(new Insets(10));
        gridPane.add(new Label("Choisir un type de dépendance"), 0, 0);
        final ComboBox<String> dependanceTypeBox = new ComboBox<>();
        dependanceTypeBox.setItems(FXCollections.observableArrayList("Ouvrages de voirie", "Chemins d'accès", "Aires de stockage", "Aménagements hydrauliques", "Autres"));
        dependanceTypeBox.getSelectionModel().selectFirst();
        gridPane.add(dependanceTypeBox, 1, 0);

        final ComboBox<String> geomTypeBox = new ComboBox<>();
        geomTypeBox.setItems(FXCollections.observableArrayList("Ponctuel", "Linéaire", "Surfacique"));
        geomTypeBox.getSelectionModel().selectFirst();
        geomTypeBox.visibleProperty().bind(dependanceTypeBox.getSelectionModel().selectedItemProperty().isEqualTo("Ouvrages de voirie")
                .or(dependanceTypeBox.getSelectionModel().selectedItemProperty().isEqualTo("Aménagements hydrauliques"))
                .or(dependanceTypeBox.getSelectionModel().selectedItemProperty().isEqualTo("Autres")));
        final Label geomChoiceLbl = new Label("Choisir une forme géométrique");
        geomChoiceLbl.visibleProperty().bind(geomTypeBox.visibleProperty());
        gridPane.add(geomChoiceLbl, 0, 1);
        gridPane.add(geomTypeBox, 1, 1);

        final Button validateBtn = new Button("Valider");
        validateBtn.setOnAction((event) -> {
            creationDependanceStage.close();
            final Class clazz;
            switch (dependanceTypeBox.getSelectionModel().getSelectedItem()) {
                case "Aires de stockage": clazz = AireStockageDependance.class; break;
                case "Autres": clazz = AutreDependance.class; break;
                case "Chemins d'accès": clazz = CheminAccesDependance.class; break;
                case "Ouvrages de voirie": clazz = OuvrageVoirieDependance.class; break;
                case "Aménagements hydrauliques": clazz = AmenagementHydraulique.class; break;
                default: clazz = AireStockageDependance.class;
            }

            if (AireStockageDependance.class.isAssignableFrom(clazz)) {
                helper = new EditionHelper(map, aireLayer);
            } else if (AutreDependance.class.isAssignableFrom(clazz)) {
                helper = new EditionHelper(map, autreLayer);
            } else if (CheminAccesDependance.class.isAssignableFrom(clazz)) {
                helper = new EditionHelper(map, cheminLayer);
            } else if (OuvrageVoirieDependance.class.isAssignableFrom(clazz)) {
                helper = new EditionHelper(map, ouvrageVoirieLayer);
            } else if (AmenagementHydraulique.class.isAssignableFrom(clazz)) {
                helper = new EditionHelper(map, amenagementLayer);
            }

            final AbstractSIRSRepository<AbstractDependance> repodep = Injector.getSession().getRepositoryForClass(clazz);
            dependance = repodep.create();
            newDependance = true;

            switch (geomTypeBox.getSelectionModel().getSelectedItem()) {
                case "Ponctuel" : newGeomType = Point.class; break;
                case "Linéaire" : newGeomType = LineString.class; break;
                case "Surfacique" : newGeomType = Polygon.class; break;
                default: newGeomType = Point.class;
            }
        });
        gridPane.add(validateBtn, 2, 3);

        final Scene sceneChoices = new Scene(gridPane);
        creationDependanceStage.setScene(sceneChoices);
        creationDependanceStage.setOnCloseRequest((onCloseEvent) -> {
            helper = null;
        });
    }
}
