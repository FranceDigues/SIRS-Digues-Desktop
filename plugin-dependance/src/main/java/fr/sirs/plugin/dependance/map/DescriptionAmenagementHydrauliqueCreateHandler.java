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
import fr.sirs.core.component.DescriptionAmenagementHydrauliqueRepository;
import fr.sirs.core.model.DescriptionAmenagementHydraulique;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.OrganeProtectionCollective;
import fr.sirs.core.model.OuvrageAssocieAmenagementHydraulique;
import fr.sirs.core.model.PrestationAmenagementHydraulique;
import fr.sirs.core.model.StructureAmenagementHydraulique;
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

/**
 * {@link AbstractNavigationHandler} permettant le contrôle les actions
 * possibles pour le bouton d'édition et de modification de dépendances sur la
 * carte.
 *
 * Note : appelée dans l'application Sirs depuis le {@link ToggleButton} sur la
 * carte. Pourrait être regroupé avec {@link DesordreEditHandler}.
 *
 * @author Cédric Briançon (Geomatys)
 */
public class DescriptionAmenagementHydrauliqueCreateHandler extends AbstractNavigationHandler {
    private final MouseListen mouseInputListener = new MouseListen();
    private final FXGeometryLayer decorationLayer = new FXGeometryLayer();

    /**
     * Le descriptionAmenagementHydraulique en cours.
     */
    private DescriptionAmenagementHydraulique description;

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
     * Vrai si un désordre vient d'être créée.
     */
    private boolean newDescription = false;

    /**
     * Définit le type de géométries à dessiner.
     */
    private Class newGeomType = Point.class;

    /**
     * Vrai si la {@linkplain #coords liste des coordonnées} de la {@linkplain #editGeometry géométrie}
     * vient d'être créée.
     */
    private boolean justCreated = false;

    /**
     * Couches présentant les désordres sur la carte.
     */
    private FeatureMapLayer structuresLayer;
    private FeatureMapLayer ouvrageAssociesLayer;
    private FeatureMapLayer desordresLayer;
    private FeatureMapLayer prestationsLayer;
    private FeatureMapLayer organeProtectionCollectivesLayer;

    public DescriptionAmenagementHydrauliqueCreateHandler() {
        super();
    }

    @Override
    public void install(FXMap component) {
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(Cursor.CROSSHAIR);
        map.addDecoration(0, decorationLayer);

        structuresLayer = PluginDependance.getStructureLayer();
        ouvrageAssociesLayer = PluginDependance.getOuvrageAssocieLayer();
        desordresLayer = PluginDependance.getDesordreLayer();
        prestationsLayer = PluginDependance.getPrestationLayer();
        organeProtectionCollectivesLayer = PluginDependance.getOrganeProtectionLayer();
    }

    @Override
    public boolean uninstall(FXMap component) {
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
            super(DescriptionAmenagementHydrauliqueCreateHandler.this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            final double x = e.getX();
            final double y = e.getY();

            if (MouseButton.PRIMARY.equals(e.getButton())) {
                if (description == null) {
                    // Recherche d'une couche de la carte qui contiendrait une géométrie là où l'utilisateur a cliqué
                    final Rectangle2D clickArea = new Rectangle2D.Double(e.getX()-2, e.getY()-2, 4, 4);

                    //recherche d'un object a editer
                    map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                        @Override
                        public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                            final Object candidate = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                            if(candidate instanceof DescriptionAmenagementHydraulique){
                                description = (DescriptionAmenagementHydraulique)candidate;
                                // On récupère la géométrie de cet objet pour passer en mode édition
                                editGeometry.geometry.set((Geometry)description.getGeometry().clone());
                                // Ajout de cette géométrie dans la couche d'édition sur la carte.
                                decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                                newDescription = false;
                            }
                        }
                        @Override
                        public boolean isStopRequested() {
                            return description!=null;
                        }
                        @Override
                        public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
                    }, VisitFilter.INTERSECTS);
                } else {
                    // Le désordre existe, on peut travailler avec sa géométrie.
                    if (newDescription) {
                        // On vient de créer le désordre, le clic gauche va permettre d'ajouter des points.
                        if (Point.class.isAssignableFrom(newGeomType)) {
                            coords.clear();
                            coords.add(helper.toCoord(x,y));
                        } else {
                            if (justCreated) {
                                justCreated = false;
                                //we must modify the second point since two point where added at the start
                                if (Polygon.class.isAssignableFrom(newGeomType)) {
                                    coords.remove(2);
                                }
                                coords.remove(1);
                                coords.add(helper.toCoord(x, y));
                                if (Polygon.class.isAssignableFrom(newGeomType)) {
                                    coords.add(helper.toCoord(x, y));
                                }
                            } else if (coords.isEmpty()) {
                                justCreated = true;
                                //this is the first point of the geometry we create
                                //add 3 points that will be used when moving the mouse around for polygons,
                                //for lines just add 2 points.
                                coords.add(helper.toCoord(x, y));
                                coords.add(helper.toCoord(x, y));
                                if (Polygon.class.isAssignableFrom(newGeomType)) {
                                    coords.add(helper.toCoord(x, y));
                                }
                            } else {
                                // On ajoute le point en plus.
                                justCreated = false;
                                coords.add(helper.toCoord(x, y));
                            }
                        }

                        // Création de la géométrie à éditer à partir des coordonnées
                        if (Polygon.class.isAssignableFrom(newGeomType)) {
                            editGeometry.geometry.set(EditionHelper.createPolygon(coords));
                        } else if (LineString.class.isAssignableFrom(newGeomType)) {
                            editGeometry.geometry.set(EditionHelper.createLine(coords));
                        } else {
                            editGeometry.geometry.set(EditionHelper.createPoint(coords.get(0)));
                        }
                        JTS.setCRS(editGeometry.geometry.get(), map.getCanvas().getObjectiveCRS2D());
                        decorationLayer.getGeometries().setAll(editGeometry.geometry.get());


                        if (Point.class.isAssignableFrom(newGeomType)) {
                            // Pour un nouveau point ajouté, on termine l'édition directement.
                            description.setGeometry(editGeometry.geometry.get());
                            final DescriptionAmenagementHydrauliqueRepository repodes = Injector.getBean(DescriptionAmenagementHydrauliqueRepository.class);
                            if (description.getDocumentId() != null) {
                                repodes.update(description);
                            } else {
                                repodes.add(description);
                            }
                            // On quitte le mode d'édition.
                            reset();
                        }
                    } else {
                        // On réédite une géométrie existante, le double clic gauche va nous permettre d'ajouter un nouveau
                        // point à la géométrie.
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
                if (description == null) {
                    // Le désordre n'existe pas, on en créé un nouveau après avoir choisi le type de géométrie à dessiner
                    final Stage stage = new Stage();
                    stage.getIcons().add(SIRS.ICON);
                    stage.setTitle("Création de désordre");
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.setAlwaysOnTop(true);
                    final GridPane gridPane = new GridPane();
                    gridPane.setVgap(10);
                    gridPane.setHgap(5);
                    gridPane.setPadding(new Insets(10));
                    gridPane.add(new Label("Choisir un type de description d'aménagement hydraulique"), 0, 0);
                    final ComboBox<String> descriptionTypeBox = new ComboBox<>();
                    descriptionTypeBox.setItems(FXCollections.observableArrayList("Structure", "Ouvrage associé", "Désordre", "Prestation", "Organe de protection collective"));
                    descriptionTypeBox.getSelectionModel().selectFirst();
                    gridPane.add(descriptionTypeBox, 1, 0);

                    final Label geomChoiceLbl = new Label("Choisir une forme géométrique");
                    gridPane.add(geomChoiceLbl, 0, 1);
                    final ComboBox<String> geomTypeBox = new ComboBox<>();
                    geomTypeBox.setItems(FXCollections.observableArrayList("Ponctuel", "Linéaire", "Surfacique"));
                    geomTypeBox.getSelectionModel().selectFirst();
                    gridPane.add(geomTypeBox, 1, 1);

                    final Button validateBtn = new Button("Valider");
                    validateBtn.setOnAction(event -> stage.close());
                    gridPane.add(validateBtn, 2, 3);

                    final Scene sceneChoices = new Scene(gridPane);
                    stage.setScene(sceneChoices);
                    stage.showAndWait();

                    final Class clazz;
                    switch (descriptionTypeBox.getSelectionModel().getSelectedItem()) {
                        case "Structure":
                            clazz = StructureAmenagementHydraulique.class;
                            helper = new EditionHelper(map, structuresLayer);
                            break;
                        case "Ouvrage associé":
                            clazz = OuvrageAssocieAmenagementHydraulique.class;
                            helper = new EditionHelper(map, ouvrageAssociesLayer);
                            break;
                        case "Désordre":
                            clazz = DesordreDependance.class;
                            helper = new EditionHelper(map, desordresLayer);
                            break;
                        case "Prestation":
                            clazz = PrestationAmenagementHydraulique.class;
                            helper = new EditionHelper(map, prestationsLayer);
                            break;
                        case "Organe de protection collective":
                            clazz = OrganeProtectionCollective.class;
                            helper = new EditionHelper(map, organeProtectionCollectivesLayer);
                            break;
                        default:
                            clazz = DesordreDependance.class;
                            helper = new EditionHelper(map, desordresLayer);
                    }

                    final AbstractSIRSRepository repodep = Injector.getSession().getRepositoryForClass(clazz);
                    description = (DescriptionAmenagementHydraulique) repodep.create();
                    newDescription = true;

                    switch (geomTypeBox.getSelectionModel().getSelectedItem()) {
                        case "Ponctuel" : newGeomType = Point.class; break;
                        case "Linéaire" : newGeomType = LineString.class; break;
                        case "Surfacique" : newGeomType = Polygon.class; break;
                        default: newGeomType = Point.class;
                    }
                } else {
                    // popup :
                    // -suppression d'un noeud
                    // -sauvegarder
                    // -annuler édition
                    // -supprimer désordre
                    popup.getItems().clear();

                    Class clazz = description.getClass();
                    AbstractSIRSRepository repo;
                    if (StructureAmenagementHydraulique.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, structuresLayer);
                        repo = Injector.getSession().getRepositoryForClass(StructureAmenagementHydraulique.class);
                    } else if (OuvrageAssocieAmenagementHydraulique.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, ouvrageAssociesLayer);
                        repo = Injector.getSession().getRepositoryForClass(OuvrageAssocieAmenagementHydraulique.class);
                    } else if (DesordreDependance.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, desordresLayer);
                        repo = Injector.getSession().getRepositoryForClass(DesordreDependance.class);
                    } else if (PrestationAmenagementHydraulique.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, prestationsLayer);
                        repo = Injector.getSession().getRepositoryForClass(PrestationAmenagementHydraulique.class);
                    } else if (OrganeProtectionCollective.class.isAssignableFrom(clazz)) {
                        helper = new EditionHelper(map, organeProtectionCollectivesLayer);
                        repo = Injector.getSession().getRepositoryForClass(OrganeProtectionCollective.class);
                    } else {
                        throw new RuntimeException("Unexpected class (" + clazz.getSimpleName() + ") in the creation process.");
                    }

                    //action : suppression d'un noeud
                    helper.grabGeometryNode(e.getX(), e.getY(), editGeometry);
                    if (editGeometry.selectedNode[0] >= 0) {
                        final MenuItem item = new MenuItem("Supprimer noeud");
                        item.setOnAction((ActionEvent event) -> {
                            editGeometry.deleteSelectedNode();
                            decorationLayer.setNodeSelection(null);
                            decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                        });
                        popup.getItems().add(item);
                    }

                    // action : sauvegarde
                    // Sauvegarde de la description d'aménagement hydraulique ainsi que sa géométrie qui a éventuellement été éditée.
                    final MenuItem saveItem = new MenuItem("Sauvegarder");
                    saveItem.setOnAction((ActionEvent event) -> {
                        description.setGeometry(editGeometry.geometry.get());
                        if (description.getDocumentId() != null) {
                            repo.update(description);
                        } else {
                            repo.add(description);
                        }
                        // On quitte le mode d'édition.
                        reset();
                    });
                    popup.getItems().add(saveItem);

                    // action : annuler édition
                    final MenuItem cancelItem = new MenuItem("Annuler l'édition");
                    cancelItem.setOnAction(event -> reset());
                    popup.getItems().add(cancelItem);

                    // action : suppression dépendance
                    final MenuItem deleteItem = new MenuItem("Supprimer " + clazz.getSimpleName(), new ImageView(GeotkFX.ICON_DELETE));
                    deleteItem.setOnAction((ActionEvent event) -> {
                        final Alert alert = new Alert(CONFIRMATION, "Voulez-vous vraiment supprimer l'objet sélectionné ?", YES, NO);
                        final Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == YES) {
                            repo.remove(description);
                        }
                        // On quitte le mode d'édition.
                        reset();
                    });
                    popup.getItems().add(deleteItem);

                    popup.show(decorationLayer, Side.TOP, e.getX(), e.getY());
                }
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            pressed = e.getButton();

            if(description != null && !newDescription && pressed == MouseButton.PRIMARY){
                // On va sélectionner un noeud sur lequel l'utilisateur a cliqué, s'il y en a un.
                helper.grabGeometryNode(e.getX(), e.getY(), editGeometry);
                decorationLayer.setNodeSelection(editGeometry);
            }

            super.mousePressed(e);
        }

        @Override
        public void mouseDragged(final MouseEvent e) {

            if(description != null && !newDescription && pressed == MouseButton.PRIMARY){
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
        newDescription = false;
        justCreated = false;
        decorationLayer.getGeometries().clear();
        decorationLayer.setNodeSelection(null);
        coords.clear();
        editGeometry.reset();
        description = null;
    }
}
