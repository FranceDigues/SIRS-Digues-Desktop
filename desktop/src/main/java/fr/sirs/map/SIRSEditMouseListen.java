
package fr.sirs.map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AvecSettableGeometrie;
import fr.sirs.core.model.BorneDigue;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.internal.GeotkFX;

import static javafx.scene.control.ButtonType.NO;
import static javafx.scene.control.ButtonType.YES;
import static javafx.scene.control.Alert.AlertType.CONFIRMATION;
import static fr.sirs.map.EditModeObjet.*;
import javafx.event.EventHandler;
import static javafx.scene.control.Alert.AlertType.NONE;
import javafx.scene.input.MouseButton;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;

/**
 *
 * Mouse listener pour l'édition de géométrie depuis la carte.
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @param <G>
 */
public class SIRSEditMouseListen<G extends AvecSettableGeometrie> extends AbstractSIRSEditMouseListen<G> {

    private final boolean allowSurfaces;

    /**
     * Create a {@link FXPanMouseListen} for geometry on map edition.
     *
     * @param sirsEditHandler
     * @param allowSurfaces : indicate if the  listener allow to create objets
     * with surface elements. If false, Only Points and Linestring will be
     * editable.
     */
    public SIRSEditMouseListen(final AbstractSIRSEditHandler sirsEditHandler, final boolean allowSurfaces) {
        super(sirsEditHandler);
        this.allowSurfaces = allowSurfaces;
    }


    /**
     * Réinitialise la carte et vide la géométrie en cours d'édition.
     */
    void reset() {
        justCreated = false;
        geomLayer.getGeometries().clear();
        geomLayer.setNodeSelection(null);
        coords.clear();
        editGeometry.reset();
        editedObjet = null;
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        final double x = e.getX();
        final double y = e.getY();

        if (MouseButton.PRIMARY.equals(e.getButton())) {
            if ( (editedObjet == null) || (NONE.equals(modeProperty.get())) ) {
               selectObjet(x, y);
            } else {
                switch (modeProperty.get()) {
                    case CREATE_OBJET :
                        createNewGeometryForObjet(x, y);
                        break;

                    case EDIT_OBJET :
                        modifyObjetGeometry(e, x, y);
                        break;
                }
            }
        } else if (MouseButton.SECONDARY.equals(e.getButton())) {
            if (editedObjet == null) {
                chooseTypesAndCreate();
            } else {
                concludeTheEdition(x, y);
            }
        }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        pressed = e.getButton();

        if ((!Point.class.isAssignableFrom(newGeomType)) && (EDIT_OBJET.equals(modeProperty.get()) && pressed == MouseButton.PRIMARY)) {
            // On va sélectionner un noeud sur lequel l'utilisateur a cliqué, s'il y en a un.
            // Le helper peut être null si on a choisi d'activer ce handler pour une dépendance existante,
            // sans passer par le clic droit pour choisir un type de dépendance.
            objetHelper = editHandler.getHelperObjet();

            objetHelper.grabGeometryNode(e.getX(), e.getY(), editGeometry);
            geomLayer.setNodeSelection(editGeometry);
        }

        super.mousePressed(e);
    }

    @Override
    public void mouseDragged(final MouseEvent e) {

        if (EDIT_OBJET.equals(modeProperty.get()) && pressed == MouseButton.PRIMARY) {
            // On déplace le noeud sélectionné
            editGeometry.moveSelectedNode(objetHelper.toCoord(e.getX(), e.getY()));
            geomLayer.getGeometries().setAll(editGeometry.geometry.get());
            return;
        }

        super.mouseDragged(e);
    }

    // ==========================  UTILITIES  ==================================

    protected final void selectObjet(final double x, final double y) {
         // Recherche d'une couche de la carte qui contiendrait une géométrie là où l'utilisateur a cliqué
//                final Rectangle2D clickArea = new Rectangle2D.Double(e.getX() - 2, e.getY() - 2, 4, 4);

        //recherche d'un object a editer
        //selection d'un troncon
        if (objetHelper == null) {
            objetHelper = editHandler.getHelperObjet();
        }
        final Feature feature = objetHelper.grabFeature(x, y, false);
        if (feature != null) {
            Object bean = feature.getUserData().get(BeanFeature.KEY_BEAN);
            if (editedClass.isInstance(bean)) {
                editedObjet = editedClass.cast(bean);
                // On récupère la géométrie de cet objet pour passer en mode édition
                editGeometry.geometry.set((Geometry) editedObjet.getGeometry().clone());
                // Ajout de cette géométrie dans la couche d'édition sur la carte.
                geomLayer.getGeometries().setAll(editGeometry.geometry.get());
                modeProperty.setValue(EDIT_OBJET);

            }

        }
    }


    protected final void createNewGeometryForObjet(final double x, final double y) {

        // Le helper peut être null si on a choisi d'activer ce handler pour un objet existant,
        final Class clazz = editedObjet.getClass();
        if (objetHelper == null) {
            if (editedClass.isAssignableFrom(clazz)) {
                objetHelper = editHandler.getHelperObjet();
            }
        }

        // le choix fait par l'utilisateur dans le panneau de création.
        final Class geomClass = newGeomType;

        // On vient de créer l'objet, le clic gauche va permettre d'ajouter des points.
        if (Point.class.isAssignableFrom(geomClass)) {
            coords.clear();
            coords.add(objetHelper.toCoord(x, y));
        } else {
            if (justCreated) {
                justCreated = false;
                //we must modify the second point since two point where added at the start
                if (Polygon.class.isAssignableFrom(geomClass)) {
                    coords.remove(2);
                }
                coords.remove(1);
                coords.add(objetHelper.toCoord(x, y));
                if (Polygon.class.isAssignableFrom(geomClass)) {
                    coords.add(objetHelper.toCoord(x, y));
                }
            } else if (coords.isEmpty()) {
                justCreated = true;
                //this is the first point of the geometry we create
                //add 3 points that will be used when moving the mouse around for polygons,
                //for lines just add 2 points.
                coords.add(objetHelper.toCoord(x, y));
                coords.add(objetHelper.toCoord(x, y));
                if (Polygon.class.isAssignableFrom(geomClass)) {
                    coords.add(objetHelper.toCoord(x, y));
                }
            } else {
                // On ajoute le point en plus.
                justCreated = false;
                //On remplace le dernier point
                coords.remove(1);
                coords.add(objetHelper.toCoord(x, y));
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
        geomLayer.getGeometries().setAll(editGeometry.geometry.get());

//        if (Point.class.isAssignableFrom(geomClass)) {
//            PointSecondGeometryStrategy();
//        }
    }

    /**
     * On réédite une géométrie existante, le double clic gauche va nous
     * permettre d'ajouter un nouveau point à la géométrie, si ce n'est pas un point.
     * @param e
     * @param x
     * @param y
     */
    protected void modifyObjetGeometry(final MouseEvent e, final double x, final double y) {
        final Geometry tempEditGeom = editGeometry.geometry.get();
        // If the selected object has not been modified yet, we add it into the edition layer of the map
        if ((tempEditGeom == null)) {
            if (!Point.class.isAssignableFrom(newGeomType)) {
                // On récupère la géométrie de l'objet sélectionné pour pouvoir la modifier
                editGeometry.geometry.set((Geometry) editedObjet.getGeometry().clone());
                // Ajout de cette géométrie dans la couche d'édition sur la carte.
                geomLayer.getGeometries().setAll(editGeometry.geometry.get());
                modeProperty.setValue(EDIT_OBJET);
                return;
            }
        } else if (!Point.class.isAssignableFrom(tempEditGeom.getClass()) && e.getClickCount() >= 2) {
            final Geometry result;
            if (tempEditGeom instanceof Polygon) {
                result = objetHelper.insertNode((Polygon) editGeometry.geometry.get(), x, y);
            } else {
                result = objetHelper.insertNode((LineString) editGeometry.geometry.get(), x, y);
            }
            editGeometry.geometry.set(result);
            geomLayer.getGeometries().setAll(editGeometry.geometry.get());
            return;
        }
        if (Point.class.isAssignableFrom(newGeomType)) {
            createNewGeometryForObjet(x, y);
        }
    }

    /**
     * L'objet n'existe pas, on en créé une nouvelle après avoir choisi son type
     * et le type de géométrie à dessiner.
     */
    protected void chooseTypesAndCreate() {

        final Stage stage = new Stage();
        stage.getIcons().add(SIRS.ICON);
        stage.setTitle("Création d'objet");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setAlwaysOnTop(true);
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(5);
        gridPane.setPadding(new Insets(10));
        gridPane.add(new Label("Choisir un type de d'objet"), 0, 0);

        final ComboBox<String> geomTypeBox = new ComboBox<>();
        if (allowSurfaces) {
            geomTypeBox.setItems(FXCollections.observableArrayList("Ponctuel", "Linéaire", "Surfacique"));
        } else {
            geomTypeBox.setItems(FXCollections.observableArrayList("Ponctuel", "Linéaire"));
        }
        geomTypeBox.getSelectionModel().selectFirst();
        final Label geomChoiceLbl = new Label("Choisir une forme géométrique");
        geomChoiceLbl.visibleProperty().bind(geomTypeBox.visibleProperty());
        gridPane.add(geomChoiceLbl, 0, 1);
        gridPane.add(geomTypeBox, 1, 1);

        final Button validateBtn = new Button("Valider");
        validateBtn.setOnAction(event -> stage.close());
        gridPane.add(validateBtn, 2, 3);

        final Scene sceneChoices = new Scene(gridPane);
        stage.setScene(sceneChoices);
        stage.showAndWait();

        objetHelper = editHandler.getHelperObjet();

        final AbstractSIRSRepository<G> repo = Injector.getSession().getRepositoryForClass(editedClass);
        editedObjet = repo.create();
        modeProperty.setValue(CREATE_OBJET);

        switch (geomTypeBox.getSelectionModel().getSelectedItem()) {
            case "Ponctuel":
                newGeomType = Point.class;
                break;
            case "Linéaire":
                newGeomType = LineString.class;
                break;
            case "Surfacique":
                newGeomType = Polygon.class;
                break;
            default:
                newGeomType = Point.class;
        }
    }


    protected final void concludeTheEdition(final double x, final double y) {

                // popup :
                // -suppression d'un noeud
                // -sauvegarder
                // -annuler édition
                // -supprimer dépendance
                popup.getItems().clear();

                objetHelper = editHandler.getHelperObjet();

                //action : suppression d'un noeud
                if (editGeometry.geometry.get() != null) {
                    objetHelper.grabGeometryNode(x, y, editGeometry);
                    if (editGeometry.selectedNode[0] >= 0) {
                        final MenuItem item = new MenuItem("Supprimer noeud");
                        item.setOnAction((ActionEvent event) -> {
                            editGeometry.deleteSelectedNode();
                            geomLayer.setNodeSelection(null);
                            geomLayer.getGeometries().setAll(editGeometry.geometry.get());
                        });
                        popup.getItems().add(item);
                    }
                }

                // action : sauvegarde
                // Sauvegarde de l'objet de stockage ainsi que sa géométrie qui a éventuellement été éditée.
                final MenuItem saveItem = new MenuItem("Sauvegarder");
                saveItem.setOnAction(saveAndReset());
                popup.getItems().add(saveItem);

                // action : annuler édition
                final MenuItem cancelItem = new MenuItem("Annuler l'édition");
                cancelItem.setOnAction(event -> {
                    reset();
                });
                popup.getItems().add(cancelItem);

                // action : suppression de l'objet
                final MenuItem deleteItem = new MenuItem("Supprimer l'élément", new ImageView(GeotkFX.ICON_DELETE));
                deleteItem.setOnAction((ActionEvent event) -> {
                    final Alert alert = new Alert(CONFIRMATION, "Voulez-vous vraiment supprimer l'élément sélectionné ?", YES, NO);
                    final Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == YES) {
                        Injector.getSession().getRepositoryForClass(editedClass).remove(editedObjet);
                        // On quitte le mode d'édition.
                        reset();
                    }
                });
                popup.getItems().add(deleteItem);

                popup.show(geomLayer, Side.TOP, x, y);
    }


    EventHandler<ActionEvent> saveAndReset() {
        return (ActionEvent event) -> {
                    ((AvecSettableGeometrie) editedObjet).setGeometry(editGeometry.geometry.get());
                    final AbstractSIRSRepository repo = Injector.getSession().getRepositoryForClass(editedObjet.getClass());

                    if (editedObjet.getDocumentId() != null) {
                        repo.update(editedObjet);
                    } else {
                        repo.add(editedObjet);
                    }
                    // On quitte le mode d'édition.
                    reset();
                };
    }

    /**
     * Provide actions to apply when the geomtype is a point and a second point was
     * geometry is created.
     *
     * Ici ({@link SIRSEditMouseListen}, on termine l'édition directement.
     *
     */
    protected void PointSecondGeometryStrategy(){

            // Pour un nouveau point ajouté, on termine l'édition directement.
            final Geometry geometry = editGeometry.geometry.get();
            if (editedObjet instanceof AvecSettableGeometrie) {
                ((AvecSettableGeometrie) editedObjet).setGeometry(geometry);
            } else if ((editedObjet instanceof BorneDigue) && (geometry instanceof Point)) {
                ((BorneDigue) editedObjet).setGeometry((Point) geometry);
            } else {
                throw new IllegalStateException("Impossible d'associer le type de géométrie éditée au le type d'objet édité");
            }
            final AbstractSIRSRepository repo = Injector.getSession().getRepositoryForClass(editedObjet.getClass());

            if (editedObjet.getDocumentId() != null) {
                repo.update(editedObjet);
            } else {
                repo.add(editedObjet);
            }
            // On quitte le mode d'édition.
            reset();
    }

    //============================ End Utilities ===============================
}