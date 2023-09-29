/*
 * This file is part of SIRS-Digues 2.
 *
 *  Copyright (C) 2021, FRANCE-DIGUES,
 *
 *  SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any
 *  later version.
 *
 *  SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
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
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.AbstractAmenagementHydraulique;
import fr.sirs.core.model.AbstractDependance;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.OrganeProtectionCollective;
import fr.sirs.core.model.OuvrageAssocieAmenagementHydraulique;
import fr.sirs.core.model.PrestationAmenagementHydraulique;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.StructureAmenagementHydraulique;
import fr.sirs.plugin.dependance.PluginDependance;
import fr.sirs.plugin.dependance.ui.FXDependanceThemePane;
import fr.sirs.plugin.dependance.ui.FXDesordreThemePane;
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
 * {@link AbstractNavigationHandler} permettant le contrôle les actions
 * possibles pour le bouton d'édition et de modification des descriptions
 * d'aménagement hydraulique sur la carte.
 *
 * @author Maxime Gavens (Geomatys)
 */
public class AbstractAmenagementHydrauliqueEditHandler extends AbstractNavigationHandler {
    private final MouseListen mouseInputListener = new MouseListen();
    private final FXGeometryLayer decorationLayer = new FXGeometryLayer();
    private Stage creationDependanceAHObjectStage;
    private ComboBox<Preview> dependanceAhBox;

    /**
     * La abstractAmenagementHydraulique en cours.
     */
    private AbstractAmenagementHydraulique abstractAmenagement;

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

    public AbstractAmenagementHydrauliqueEditHandler() {
        super();
    }

    public AbstractAmenagementHydrauliqueEditHandler(final AbstractAmenagementHydraulique abstractAmenagement) {
        this();
        this.abstractAmenagement = abstractAmenagement;
        newDescription = true;

        if (abstractAmenagement.getGeometry() != null) {
            editGeometry.geometry.set((Geometry)abstractAmenagement.getGeometry().clone());
            decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
            newDescription = false;
        } else {
            // on demande à l'utilisateur le type de géométrie
            final ChoiceDialog<String> choice = new ChoiceDialog<>("Ponctuel","Linéaire","Surfacique");
            choice.setHeaderText("Choix de la forme géométrique de l'objet lié à un AH ou une dépendance.");
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
            super(AbstractAmenagementHydrauliqueEditHandler.this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            final double x = e.getX();
            final double y = e.getY();
            final Class clazz = initHelper();

            if (MouseButton.PRIMARY.equals(e.getButton())) {
                if (abstractAmenagement == null) {
                    // Recherche d'une couche de la carte qui contiendrait une géométrie là où l'utilisateur a cliqué
                    final Rectangle2D clickArea = new Rectangle2D.Double(e.getX()-2, e.getY()-2, 4, 4);

                    //recherche d'un object a editer
                    map.getCanvas().getGraphicsIn(clickArea, new AbstractGraphicVisitor() {
                        @Override
                        public void visit(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D area) {
                            final Object candidate = graphic.getCandidate().getUserData().get(BeanFeature.KEY_BEAN);
                            if(candidate instanceof AbstractAmenagementHydraulique){
                                abstractAmenagement = (AbstractAmenagementHydraulique)candidate;
                                // On récupère la géométrie de cet objet pour passer en mode édition
                                editGeometry.geometry.set((Geometry)abstractAmenagement.getGeometry().clone());
                                // Ajout de cette géométrie dans la couche d'édition sur la carte.
                                decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                                newDescription = false;
                            }
                        }
                        @Override
                        public boolean isStopRequested() {
                            return abstractAmenagement!=null;
                        }
                        @Override
                        public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
                    }, VisitFilter.INTERSECTS);
                } else {
                    // L'aménagement hydraulique existe, on peut travailler avec sa géométrie.
                    if (newDescription) {
                        // On vient de créer la description d'aménagement hydraulique, le clic gauche va permettre d'ajouter des points.
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
                            abstractAmenagement.setGeometry(editGeometry.geometry.get());
                            final AbstractSIRSRepository repo = Injector.getSession().getRepositoryForClass(clazz);
                            if (abstractAmenagement.getDocumentId() != null) {
                                repo.update(abstractAmenagement);
                            } else {
                                repo.add(abstractAmenagement);
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
                if (abstractAmenagement == null) {
                    if (creationDependanceAHObjectStage == null) initCreationDependanceAHObjectStage();
                    creationDependanceAHObjectStage.showAndWait();
                } else {
                    // popup :
                    // -suppression d'un noeud
                    // -sauvegarder
                    // -annuler édition
                    // -supprimer désordre
                    popup.getItems().clear();

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
                    // Sauvegarde de l' abstractAmenagement ainsi que sa géométrie qui a éventuellement été éditée.
                    final AbstractSIRSRepository repo = Injector.getSession().getRepositoryForClass(clazz);
                    final MenuItem saveItem = new MenuItem("Sauvegarder");
                    saveItem.setOnAction((ActionEvent event) -> {
                        abstractAmenagement.setGeometry(editGeometry.geometry.get());
                        if (abstractAmenagement.getDocumentId() != null) {
                            repo.update(abstractAmenagement);
                        } else {
                            repo.add(abstractAmenagement);
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
                            repo.remove(abstractAmenagement);
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
            initHelper();

            if(abstractAmenagement != null && !newDescription && pressed == MouseButton.PRIMARY){
                // On va sélectionner un noeud sur lequel l'utilisateur a cliqué, s'il y en a un.
                helper.grabGeometryNode(e.getX(), e.getY(), editGeometry);
                decorationLayer.setNodeSelection(editGeometry);
            }

            super.mousePressed(e);
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            initHelper();

            if(abstractAmenagement != null && !newDescription && pressed == MouseButton.PRIMARY){
                // On déplace le noeud sélectionné
                editGeometry.moveSelectedNode(helper.toCoord(e.getX(), e.getY()));
                decorationLayer.getGeometries().setAll(editGeometry.geometry.get());
                return;
            }

            super.mouseDragged(e);
        }
    }

    private Class initHelper() {
        if (abstractAmenagement != null) {
            if (abstractAmenagement instanceof StructureAmenagementHydraulique) {
                helper = new EditionHelper(map, structuresLayer);
                return StructureAmenagementHydraulique.class;
            } else if (abstractAmenagement instanceof OuvrageAssocieAmenagementHydraulique) {
                helper = new EditionHelper(map, ouvrageAssociesLayer);
                return OuvrageAssocieAmenagementHydraulique.class;
            } else if (abstractAmenagement instanceof DesordreDependance) {
                helper = new EditionHelper(map, desordresLayer);
                return DesordreDependance.class;
            } else if (abstractAmenagement instanceof PrestationAmenagementHydraulique) {
                helper = new EditionHelper(map, prestationsLayer);
                return PrestationAmenagementHydraulique.class;
            } else if (abstractAmenagement instanceof OrganeProtectionCollective) {
                helper = new EditionHelper(map, organeProtectionCollectivesLayer);
                return OrganeProtectionCollective.class;
            } else {
                throw new IllegalArgumentException("Unexpected subclass of AbstractAmenagementHydraulique.");
            }
        }
        return null;
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
        abstractAmenagement = null;
        creationDependanceAHObjectStage = null;
    }

    private void initCreationDependanceAHObjectStage() {
        // Le désordre n'existe pas, on en créé un nouveau après avoir choisi le type de géométrie à dessiner
        creationDependanceAHObjectStage = new Stage();
        creationDependanceAHObjectStage.getIcons().add(SIRS.ICON);
        creationDependanceAHObjectStage.setTitle("Création d'un objet lié à un AH ou une dépendance");
        creationDependanceAHObjectStage.initModality(Modality.WINDOW_MODAL);
        creationDependanceAHObjectStage.setAlwaysOnTop(true);
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(5);
        gridPane.setPadding(new Insets(10));

        gridPane.add(new Label("Choisir un type d'objet lié à un AH ou une dépendance"), 0, 0);
        final ComboBox<String> abstractAmenagementTypeBox = new ComboBox<>();
        abstractAmenagementTypeBox.setItems(FXCollections.observableArrayList("Structure", "Ouvrage associé", "Désordre", "Prestation", "Organe de protection collective"));
        abstractAmenagementTypeBox.getSelectionModel().selectFirst();
        gridPane.add(abstractAmenagementTypeBox, 1, 0);

        gridPane.add(new Label("Choisir une forme géométrique"), 0, 1);
        final ComboBox<String> geomTypeBox = new ComboBox<>();
        geomTypeBox.setItems(FXCollections.observableArrayList("Ponctuel", "Linéaire", "Surfacique"));
        geomTypeBox.getSelectionModel().selectFirst();
        gridPane.add(geomTypeBox, 1, 1);

        gridPane.add(new Label("Choisir un AH (une dépendance ou un AH pour un désordre)"), 0, 2);
        dependanceAhBox = new ComboBox();
        initAhDependanceBox(true);
        gridPane.add(dependanceAhBox, 1, 2);
        abstractAmenagementTypeBox.valueProperty()
                .addListener((observable, oldValue, newValue) -> initAhDependanceBox(!("Désordre".equals(newValue))));

        final Button validateBtn = new Button("Valider");
        validateBtn.setOnAction((event) -> {
            creationDependanceAHObjectStage.close();
            AbstractSIRSRepository repo;
            switch (abstractAmenagementTypeBox.getSelectionModel().getSelectedItem()) {
                case "Structure":
                    repo = Injector.getSession().getRepositoryForClass(StructureAmenagementHydraulique.class);
                    helper = new EditionHelper(map, structuresLayer);
                    break;
                case "Ouvrage associé":
                    repo = Injector.getSession().getRepositoryForClass(OuvrageAssocieAmenagementHydraulique.class);
                    helper = new EditionHelper(map, ouvrageAssociesLayer);
                    break;
                case "Désordre":
                    repo = Injector.getSession().getRepositoryForClass(DesordreDependance.class);
                    helper = new EditionHelper(map, desordresLayer);
                    break;
                case "Prestation":
                    repo = Injector.getSession().getRepositoryForClass(PrestationAmenagementHydraulique.class);
                    helper = new EditionHelper(map, prestationsLayer);
                    break;
                case "Organe de protection collective":
                    repo = Injector.getSession().getRepositoryForClass(OrganeProtectionCollective.class);
                    helper = new EditionHelper(map, organeProtectionCollectivesLayer);
                    break;
                default:
                    repo = Injector.getSession().getRepositoryForClass(DesordreDependance.class);
                    helper = new EditionHelper(map, desordresLayer);
            }

            abstractAmenagement = (AbstractAmenagementHydraulique) repo.create();
            newDescription = true;

            final Preview dependanceAhPreview = dependanceAhBox.getSelectionModel().getSelectedItem();
            if (dependanceAhPreview != null) {
                if (abstractAmenagement instanceof DesordreDependance) {
                    final Class clazz = dependanceAhPreview.getJavaClassOr(null);
                    if (AmenagementHydraulique.class.equals(clazz)) {
                        abstractAmenagement.setAmenagementHydrauliqueId(dependanceAhPreview.getElementId());
                    } else if (AbstractDependance.class.isAssignableFrom(AbstractDependance.class)) {
                        ((DesordreDependance) abstractAmenagement).setDependanceId(dependanceAhPreview.getElementId());
                    } else {
                        SIRS.LOGGER.warning("Unexpected behavior: unexpected value for dependanceAhBox combobox");
                    }
                } else {
                    abstractAmenagement.setAmenagementHydrauliqueId(dependanceAhPreview.getElementId());
                }
            }

            switch (geomTypeBox.getSelectionModel().getSelectedItem()) {
                case "Ponctuel" : newGeomType = Point.class; break;
                case "Linéaire" : newGeomType = LineString.class; break;
                case "Surfacique" : newGeomType = Polygon.class; break;
                default: newGeomType = Point.class;
            }
        });
        gridPane.add(validateBtn, 2, 3);

        final Scene sceneChoices = new Scene(gridPane);
        creationDependanceAHObjectStage.setScene(sceneChoices);
        creationDependanceAHObjectStage.setOnCloseRequest((onCloseEvent) -> {
            helper = null;
        });
    }

    private void initAhDependanceBox(final boolean onlyAh) {
        final Previews previewRepository = Injector.getSession().getPreviews();
        final List<Preview> previews;

        if (onlyAh) {
            previews = previewRepository.getByClass(AmenagementHydraulique.class);
            if (!previews.contains(FXDependanceThemePane.EMPTY_PREVIEW)) previews.add(FXDependanceThemePane.EMPTY_PREVIEW);
        } else {
            previews = previewRepository.getByClass(AbstractDependance.class);
            if (!previews.contains(FXDesordreThemePane.EMPTY_PREVIEW)) previews.add(FXDesordreThemePane.EMPTY_PREVIEW);
        }
        SIRS.initCombo(dependanceAhBox, FXCollections.observableList(previews), null);
        dependanceAhBox.setEditable(false);
        dependanceAhBox.getSelectionModel().selectFirst();
    }
}
