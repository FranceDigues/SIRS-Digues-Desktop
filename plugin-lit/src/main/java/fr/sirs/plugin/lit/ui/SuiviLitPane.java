
package fr.sirs.plugin.lit.ui;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.DocumentListener;
import fr.sirs.core.component.TronconLitRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Lit;
import fr.sirs.core.model.TronconLit;
import fr.sirs.index.ElasticSearchEngine;
import fr.sirs.theme.Theme;
import fr.sirs.theme.ui.AbstractFXElementPane;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Popup;
import org.apache.sis.referencing.CommonCRS;
import org.elasticsearch.index.query.QueryBuilders;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author guilhem
 */
public class SuiviLitPane extends SplitPane implements DocumentListener {

    /** Cette géométrie sert de base pour tous les nouveaux troncons */
    private static final Geometry TRONCON_GEOM_WGS84;
    static {
        TRONCON_GEOM_WGS84 = new GeometryFactory().createLineString(new Coordinate[]{
            new Coordinate(0, 48),
            new Coordinate(5, 48)
        });
        JTS.setCRS(TRONCON_GEOM_WGS84, CommonCRS.WGS84.normalizedGeographic());
    }

    private static final String[] SEARCH_CLASSES = new String[]{
        TronconLit.class.getCanonicalName(),
        Lit.class.getCanonicalName()
    };

    @Autowired
    private Session session;

    @FXML private TreeView uiTree;
    @FXML private BorderPane uiRight;
    @FXML private Button uiSearch;
    @FXML private Button uiDelete;
    @FXML private ToggleButton uiArchived;
    @FXML private MenuButton uiAdd;

    //etat de la recherche
    private final ImageView searchNone = new ImageView(SIRS.ICON_SEARCH_WHITE);
    private final ProgressIndicator searchRunning = new ProgressIndicator();
    private final StringProperty currentSearch = new SimpleStringProperty("");

    public SuiviLitPane() {
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);

        uiTree.setShowRoot(false);
        uiTree.setCellFactory((Object param) -> new CustomizedTreeCell());

        uiTree.getSelectionModel().getSelectedIndices().addListener((ListChangeListener.Change c) -> {
            Object obj = uiTree.getSelectionModel().getSelectedItem();
            if (obj instanceof TreeItem) {
                obj = ((TreeItem) obj).getValue();
            }

            if (obj instanceof Lit) {
                displayElement((Element) obj);
            } else if (obj instanceof TronconLit) {
                //le troncon dans l'arbre est une version 'light'
                displayElement(session.getRepositoryForClass(TronconLit.class).get(((TronconLit) obj).getDocumentId()));
            }
        });

        searchRunning.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        searchRunning.setPrefSize(22, 22);
        searchRunning.setStyle(" -fx-progress-color: white;");

        uiArchived.setSelected(false);
        uiArchived.setGraphic(new ImageView(SIRS.ICON_ARCHIVE_WHITE));
        uiArchived.setOnAction(event -> updateTree());
        uiArchived.setTooltip(new Tooltip("Voir les troncons de lit archivés"));
        uiArchived.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(uiArchived!=null && uiArchived.getTooltip()!=null)
                    uiArchived.getTooltip().setText(uiArchived.isSelected() ? "Masquer les troncons de lit archivés" : "Voir les troncons de lit archivés");
            }
        });

        uiSearch.setGraphic(searchNone);
        uiSearch.textProperty().bind(currentSearch);

        uiDelete.setGraphic(new ImageView(SIRS.ICON_TRASH_WHITE));
        uiDelete.setOnAction(this::deleteSelection);
        uiDelete.setDisable(!session.nonGeometryEditionProperty().get());
        uiAdd.setGraphic(new ImageView(SIRS.ICON_ADD_WHITE));
        uiAdd.getItems().add(new NewLitMenuItem(null));
        uiAdd.setDisable(!session.nonGeometryEditionProperty().get());

        updateTree();

        //listen to changes in the db to update tree
        Injector.getDocumentChangeEmiter().addListener(this);

    }

    /**
     * Affiche un éditeur pour l'élément en entrée.
     * @param obj L'élément à éditer.
     */
    public void displayElement(final Element obj) {
        AbstractFXElementPane ctrl = SIRS.generateEditionPane(obj);
        uiRight.setCenter(ctrl);
        session.getPrintManager().prepareToPrint(obj);
    }

    private void deleteSelection(ActionEvent event) {
        Object obj = uiTree.getSelectionModel().getSelectedItem();
        if(obj instanceof TreeItem){
            obj = ((TreeItem)obj).getValue();
        }

        if(obj instanceof Lit){
            final Lit lit = (Lit) obj;
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "La suppression du lit " + lit.getLibelle() + " ne supprimera pas les tronçons qui le compose, "
                   +"ceux ci seront déplacés dans le groupe 'Non classés. Confirmer la suppression ?",
                    ButtonType.YES, ButtonType.NO);
            alert.setResizable(true);
            final ButtonType res = alert.showAndWait().get();
            if (res == ButtonType.YES) {
                //on enleve la reference au lit dans les troncons
                final List<TronconLit> troncons = ((TronconLitRepository)session.getRepositoryForClass(TronconLit.class)).getByLit(lit);
                for (final TronconLit td : troncons) {
                    td.setLitId(null);
                    session.getRepositoryForClass(TronconLit.class).update(td);
                }
                //on supprime le lit
                session.getRepositoryForClass(Lit.class).remove(lit);
            }
        }else if(obj instanceof TronconLit){
            final TronconLit td = (TronconLit) obj;
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Confirmer la suppression du tronçon de lit" + td.getLibelle() + " ?",
                    ButtonType.YES, ButtonType.NO);
            alert.setResizable(true);
            final ButtonType res = alert.showAndWait().get();
            if (res == ButtonType.YES) {
                session.getRepositoryForClass(TronconLit.class).remove(td);
            }
        }
    }

    @FXML
    private void openSearchPopup(ActionEvent event) {
        if (uiSearch.getGraphic() != searchNone) {
            //une recherche est deja en cours
            return;
        }

        final Popup popup = new Popup();
        final TextField textField = new TextField(currentSearch.get());
        popup.setAutoHide(true);
        popup.getContent().add(textField);

        textField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                currentSearch.set(textField.getText());
                popup.hide();
                updateTree();
            }
        });
        final Point2D sc = uiSearch.localToScreen(0, 0);
        popup.show(uiSearch, sc.getX(), sc.getY());
    }

    private final Predicate<TronconLit> nonArchivedPredicate = (TronconLit t) -> {
        final boolean result = (t.getDate_fin()==null || t.getDate_fin().isAfter(LocalDate.now()));

//        System.out.println("Le prédicat vaut "+ t.getLibelle() +" est archivé : "+!result);
        return result;
    };

    private final Predicate<TronconLit> searchedPredicate = (TronconLit t) -> {
        final String str = currentSearch.get();
        if (str != null && !str.isEmpty()) {
            final ElasticSearchEngine searchEngine = Injector.getElasticSearchEngine();
            HashMap<String, HashSet<String>> foundClasses = searchEngine.searchByClass(QueryBuilders.queryString(str));
            final HashSet resultSet = new HashSet();
            HashSet tmp;
            for (final String className : SEARCH_CLASSES) {
                tmp = foundClasses.get(className);
                if (tmp != null && !tmp.isEmpty()) {
                    resultSet.addAll(tmp);
                }
            }
            return resultSet.contains(t.getDocumentId());
        }
        else return true;
    };

    private void updateTree() {

        new Thread(){
            @Override
            public void run() {
                Platform.runLater(() -> {
                    uiSearch.setGraphic(searchRunning);
                });

                //on stoque les noeuds ouverts
                final Set extendeds = new HashSet();
                searchExtended(uiTree.getRoot(),extendeds);

                //creation des filtres
                Predicate<TronconLit> filter = searchedPredicate;
                if(!uiArchived.isSelected()) {
//                    System.out.println("uiArchived n'est pas sélectionné !");
                    filter = filter.and(nonArchivedPredicate);
                }

                //creation de l'arbre
                final TreeItem treeRootItem = new TreeItem("root");

                //on recupere tous les elements
                final Iterable<Lit> lits = session.getRepositoryForClass(Lit.class).getAllStreaming();
                final Set<TronconLit> tronconLits = new HashSet<>(session.getRepositoryForClass(TronconLit.class).getAll());
                final Set<TronconLit> tronconFound = new HashSet<>();

                for (Lit lit : lits) {
                    final TreeItem litItem = new TreeItem(lit);
                    treeRootItem.getChildren().add(litItem);
                    litItem.setExpanded(extendeds.contains(lit));

                    final List<TronconLit> tronconIds = ((TronconLitRepository) session.getRepositoryForClass(TronconLit.class)).getByLit(lit);
                    
                    for(TronconLit trl : tronconLits){
                        if(!tronconIds.contains(trl)) continue;
                        tronconFound.add(trl);
                        
                        if (filter == null || filter.test(trl)) {
                            final TreeItem tronconItem = new TreeItem(trl);
                            litItem.getChildren().add(tronconItem);
                        }
                    }
                }

                //on place toute les tronçons non trouvé dans un group a part
                tronconLits.removeAll(tronconFound);
                final TreeItem ncItem = new TreeItem("Non classés");
                ncItem.setExpanded(extendeds.contains(ncItem.getValue()));
                treeRootItem.getChildren().add(ncItem);

                for(final TronconLit trl : tronconLits){
                    if (filter == null || filter.test(trl)) {
                        final TreeItem tronconItem = new TreeItem(trl);
                        ncItem.getChildren().add(tronconItem);
                    }
                }

                Platform.runLater(() -> {
                    uiTree.setRoot(treeRootItem);
                    uiSearch.setGraphic(searchNone);
                });
            }
        }.start();

    }

    private static void searchExtended(TreeItem<?> ti, Set objects){
        if(ti==null) return;
        if(ti.isExpanded()){
            objects.add(ti.getValue());
        }
        for(TreeItem t : ti.getChildren()){
            searchExtended(t, objects);
        }
    }

    @Override
    public void documentCreated(Map<Class, List<Element>> candidate) {
        if(candidate.get(Lit.class) != null  ||
           candidate.get(TronconLit.class) != null) {
            updateTree();
        }
    }

    @Override
    public void documentChanged(Map<Class, List<Element>> candidate) {
        if(candidate.get(Lit.class) != null  ||
           candidate.get(TronconLit.class) != null) {
           updateTree();
        }
    }

    @Override
    public void documentDeleted(Map<Class, List<Element>> candidate) {
        if(candidate.get(Lit.class) != null  ||
           candidate.get(TronconLit.class) != null) {
           updateTree();
        }
    }

    private class NewTronconMenuItem extends MenuItem {

        public NewTronconMenuItem(TreeItem parent) {
            super("Créer un nouveau tronçon de lit",new ImageView(SIRS.ICON_ADD_WHITE));
            this.setOnAction((ActionEvent t) -> {
                final TronconLit troncon = session.getElementCreator().createElement(TronconLit.class);
                troncon.setLibelle("Tronçon de lit vide");
                if (parent != null) {
                    final Lit lit = (Lit) parent.getValue();
                    troncon.setLitId(lit.getId());
                }

                try {
                    //on crée un géométrie au centre de la france
                    final Geometry geom = JTS.transform(TRONCON_GEOM_WGS84,
                            CRS.findMathTransform(CommonCRS.WGS84.normalizedGeographic(),session.getProjection(),true));
                    troncon.setGeometry(geom);
                } catch (FactoryException | TransformException | MismatchedDimensionException ex) {
                    SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
                    troncon.setGeometry((Geometry) TRONCON_GEOM_WGS84.clone());
                }

                session.getRepositoryForClass(TronconLit.class).add(troncon);
                //mise en place du SR élémentaire
                TronconUtils.updateSRElementaire(troncon, session);
            });
        }
    }

    private class NewLitMenuItem extends MenuItem {

        public NewLitMenuItem(TreeItem parent) {
            super("Créer un nouveau lit",new ImageView(SIRS.ICON_ADD_WHITE));
            this.setOnAction((ActionEvent t) -> {
                final Lit lit = session.getElementCreator().createElement(Lit.class);
                lit.setLibelle("Lit vide");
                session.getRepositoryForClass(Lit.class).add(lit);
            });
        }
    }

    private class CustomizedTreeCell extends TreeCell {

        private final ContextMenu addMenu;

        public CustomizedTreeCell() {
            addMenu = new ContextMenu();
        }

        @Override
        protected void updateItem(Object obj, boolean empty) {
            super.updateItem(obj, empty);
            setContextMenu(null);

            if (obj instanceof TreeItem) {
                obj = ((TreeItem) obj).getValue();
            }

            if (obj instanceof Lit) {
                this.setText(((Lit) obj).getLibelle() + " (" + getTreeItem().getChildren().size() + ") ");
                addMenu.getItems().clear();
                if(session.nonGeometryEditionProperty().get()){
                    addMenu.getItems().add(new NewTronconMenuItem(getTreeItem()));
                    setContextMenu(addMenu);
                }
            } else if (obj instanceof TronconLit) {
                this.setText(((TronconLit) obj).getLibelle() + " (" + getTreeItem().getChildren().size() + ") ");
                setContextMenu(null);
            } else if (obj instanceof Theme) {
                setText(((Theme) obj).getName());
            } else if( obj instanceof String){
                setText((String)obj);
            } else {
                setText(null);
            }
        }
    }
}