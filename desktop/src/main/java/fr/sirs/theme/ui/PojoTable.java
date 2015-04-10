package fr.sirs.theme.ui;

import org.geotoolkit.gui.javafx.util.FXNumberCell;
import org.geotoolkit.gui.javafx.util.FXStringCell;
import org.geotoolkit.gui.javafx.util.FXLocalDateTimeCell;
import org.geotoolkit.gui.javafx.util.FXBooleanCell;
import com.sun.javafx.property.PropertyReference;
import fr.sirs.Session;
import fr.sirs.SIRS;
import fr.sirs.Injector;
import fr.sirs.core.Repository;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.AbstractSIRSRepository;
import org.geotoolkit.gui.javafx.util.TaskManager;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.Role;
import static fr.sirs.core.model.Role.EXTERN;
import fr.sirs.core.model.ValiditySummary;
import fr.sirs.query.ElementHit;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.SirsTableCell;
import fr.sirs.util.property.Reference;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import jidefx.scene.control.field.NumberField;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.gui.javafx.util.FXEnumTableCell;
import org.geotoolkit.gui.javafx.util.FXPasswordTableCell;
import org.geotoolkit.gui.javafx.util.FXTableView;
import org.geotoolkit.internal.GeotkFX;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @author Samuel Andrés (Geomatys)
 */
public class PojoTable extends BorderPane {
    private static final List<String> SPECIAL_DISPLAY_COLUMNS = new ArrayList<>();
    static{
        SPECIAL_DISPLAY_COLUMNS.add("author");
        SPECIAL_DISPLAY_COLUMNS.add("valid");
        SPECIAL_DISPLAY_COLUMNS.add("designation");
    }
    
    protected final Class pojoClass;
    protected final AbstractSIRSRepository repo;
    protected final Session session = Injector.getBean(Session.class);
    protected final TableView<Element> uiTable = new FXTableView<>();
    private final LabelMapper labelMapper;
    
    // Editabilité du tableau (possibilité d'ajout et de suppression des éléments
    protected final BooleanProperty editableProperty = new SimpleBooleanProperty(true);
    // Editabilité des cellules tableau (possibilité d'ajout et de suppression des éléments
    protected final BooleanProperty cellEditableProperty = new SimpleBooleanProperty();
    // Parcours fiche par fiche
    protected final BooleanProperty fichableProperty = new SimpleBooleanProperty(true);
    // Accès à la fiche détaillée d'un élément particulier
    protected final BooleanProperty detaillableProperty = new SimpleBooleanProperty(true);
    // Possibilité de faire une recherche sur le contenu de la table
    protected final BooleanProperty searchableProperty = new SimpleBooleanProperty(true);
    // Ouvrir l'editeur sur creation d'un nouvel objet
    protected final BooleanProperty openEditorOnNewProperty = new SimpleBooleanProperty(true);
    // Créer un nouvel objet à l'ajout
    protected final BooleanProperty createNewProperty = new SimpleBooleanProperty(true);
    /* Rechercher les objets dans un tronçon donné (sert uniquement si on ne 
    crée pas les objets à l'ajout, mais si on cherche des objets préexisants.
    Cette propriété sert alors à limiter la recherche à un tronçon donné (de 
    manière à ne relier entre eux que des "objets" du même tronçon.*/
    protected final StringProperty tronconSourceProperty = new SimpleStringProperty(null);
    
        
    // Icônes de la barre d'action
    // Barre de droite : manipulation du tableau et passage en mode parcours de fiche
    protected final ImageView searchNone = new ImageView(SIRS.ICON_SEARCH);
    protected final Button uiSearch;
    protected final Button uiAdd = new Button(null, new ImageView(SIRS.ICON_ADD_WHITE));
    protected final Button uiDelete = new Button(null, new ImageView(SIRS.ICON_TRASH));
    protected final ImageView playIcon = new ImageView(SIRS.ICON_FILE);
    private final ImageView stopIcon = new ImageView(SIRS.ICON_TABLE);
    protected final ToggleButton uiFicheMode = new ToggleButton();
    protected final HBox searchEditionToolbar = new HBox();
    
    // Barre de gauche : navigation dans le parcours de fiches
    protected FXElementPane elementPane = null;
    private final Button uiPrevious = new Button("",new ImageView(SIRS.ICON_CARET_LEFT));
    private final Button uiNext = new Button("",new ImageView(SIRS.ICON_CARET_RIGHT));
    private final Button uiCurrent = new Button();
    protected final HBox navigationToolbar = new HBox();
    
    
    protected final ProgressIndicator searchRunning = new ProgressIndicator();
    protected ObservableList<Element> allValues;
    protected ObservableList<Element> filteredValues;
    
    protected final StringProperty currentSearch = new SimpleStringProperty("");
    protected final BorderPane topPane;
    
    /** The element to set as parent for any created element using {@linkplain #createPojo() }. */
    protected final SimpleObjectProperty<Element> parentElementProperty = new SimpleObjectProperty<>();
    /** The element to set as owner for any created element using {@linkplain #createPojo() }. 
     On the contrary to the parent, the owner purpose is not to contain the created pojo, but to reference it.*/
    protected final SimpleObjectProperty<Element> ownerElementProperty = new SimpleObjectProperty<>();
    
    /** Task object designed for asynchronous update of the elements contained in the table. */
    protected Task tableUpdater;
    
    public PojoTable(final Class pojoClass, final String title) {
        this(pojoClass, title, null);
    }
    
    public PojoTable(final AbstractSIRSRepository repo, final String title) {
        this(repo.getModelClass(), title, repo);
    }
    
    private PojoTable(final Class pojoClass, final String title, final AbstractSIRSRepository repo) {
        if (pojoClass == null && repo == null) {
            throw new IllegalArgumentException("Pojo class to expose and Repository parameter are both null. At least one of them must be valid.");
        }
        if (pojoClass == null) {
            this.pojoClass = repo.getModelClass();
        } else {
            this.pojoClass = pojoClass;
        }
        getStylesheets().add(SIRS.CSS_PATH);
        this.labelMapper = new LabelMapper(this.pojoClass);
        if (repo == null) {
            AbstractSIRSRepository tmpRepo;
            try {
                tmpRepo = session.getRepositoryForClass(pojoClass);
            } catch (IllegalArgumentException e) {
                SIRS.LOGGER.log(Level.FINE, e.getMessage());
                tmpRepo = null;
            }
            this.repo = tmpRepo;
        } else {
            this.repo = repo;
        }
        
        searchRunning.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        searchRunning.setPrefSize(22, 22);
        searchRunning.setStyle("-fx-progress-color: white;");
        
        uiTable.setRowFactory(new Callback<TableView<Element>, TableRow<Element>>() {

            @Override
            public TableRow<Element> call(TableView<Element> param) {
                return new ValidatedTableRow();
            }
        });
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiTable.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Element> observable, Element oldValue, Element newValue) -> {
            session.prepareToPrint(newValue);
        });
                
        // Colonnes de suppression et d'ouverture d'éditeur.
        final DeleteColumn deleteColumn = new DeleteColumn();
        final EditColumn editCol = new EditColumn(this::editPojo);
                
        /* We cannot bind visible properties of those columns, because TableView 
         * will set their value when user will request to hide them.
         */
        editableProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            deleteColumn.setVisible(newValue);
            //editCol.setVisible(newValue && detaillableProperty.get());
        });
        cellEditableProperty.bind(editableProperty);
        detaillableProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            editCol.setVisible(newValue && detaillableProperty.get());
        });
        
        uiTable.getColumns().add(deleteColumn);
        uiTable.getColumns().add((TableColumn)editCol);
        
        //contruction des colonnes editable
        final List<PropertyDescriptor> properties = Session.listSimpleProperties(this.pojoClass);
        
        //On commence par la colonne des désignations
        for(final PropertyDescriptor desc : properties){
            if(desc.getName().equals("designation")){
                uiTable.getColumns().add(new PropertyColumn(desc));
            }
        }
        
        for (final PropertyDescriptor desc : properties) {
            if (!SPECIAL_DISPLAY_COLUMNS.contains(desc.getName())) {
                final TableColumn col;
                // Colonne de mots de passe simplifiée : ne marche pas très bien.
                if ("password".equals(desc.getDisplayName())) {
                    col = new PasswordColumn(desc);
                } else if (desc.getReadMethod().getReturnType().isEnum()) {
                    col = new EnumColumn(desc);
                } else {

                    col = new PropertyColumn(desc);
                }
                uiTable.getColumns().add(col);
            }
        }
        
        uiTable.editableProperty().bind(editableProperty);
        
        /* barre d'outils. Si on a un accesseur sur la base, on affiche des
         * boutons de création / suppression.
         */
        uiSearch = new Button(null, searchNone);
        uiSearch.textProperty().bind(currentSearch);
        uiSearch.getStyleClass().add("btn-without-style");
        uiSearch.setOnAction((ActionEvent event) -> {search();});
        uiSearch.getStyleClass().add("label-header");
        uiSearch.setTooltip(new Tooltip("Rechercher un terme dans la table"));
        uiSearch.disableProperty().bind(searchableProperty.not());
        
        final Label uiTitle = new Label(title==null? labelMapper.mapClassName() : title);
        uiTitle.getStyleClass().add("pojotable-header");
        uiTitle.setAlignment(Pos.CENTER);
        
        searchEditionToolbar.getStyleClass().add("buttonbar");
        searchEditionToolbar.getChildren().add(uiSearch);
                
        uiAdd.getStyleClass().add("btn-without-style");
        uiAdd.setOnAction((ActionEvent event) -> {
            final Object p;
            if(createNewProperty.get()){
                p = createPojo();
            
                if (p != null && openEditorOnNewProperty.get()) {
                    editPojo(p);
                }
            }
            else{
                final ChoiceStage stage = new ChoiceStage();
                stage.showAndWait();
                p=stage.getRetrievedElement().get();
            }
        });
        uiAdd.disableProperty().bind(editableProperty.not());

        uiDelete.getStyleClass().add("btn-without-style");
        uiDelete.setOnAction((ActionEvent event) -> {
            final Element[] elements = ((List<Element>) uiTable.getSelectionModel().getSelectedItems()).toArray(new Element[0]);
            if (elements.length > 0) {
                final Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression ?",
                        ButtonType.NO, ButtonType.YES).showAndWait();
                if (res.isPresent() && ButtonType.YES.equals(res.get())) {
                        deletePojos(elements);
                }
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Aucune entrée sélectionnée. Pas de suppression possible.").showAndWait();
            }
        });
        uiDelete.disableProperty().bind(editableProperty.not());

        searchEditionToolbar.getChildren().addAll(uiAdd, uiDelete);
        
        topPane = new BorderPane(uiTitle,null,searchEditionToolbar,null,null);
        setTop(topPane);
        uiTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        uiTable.setMaxWidth(Double.MAX_VALUE);
        uiTable.setPrefSize(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        uiTable.setPrefSize(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        uiTable.setPlaceholder(new Label(""));
        uiTable.setTableMenuButtonVisible(true);
        // Load all elements only if the user gave us the repository.
        if (repo != null) {
            setTableItems(()-> FXCollections.observableList(repo.getAll()));
        }
        
        final FXCommentPhotoView commentPhotoView = new FXCommentPhotoView();
        commentPhotoView.valueProperty().bind(uiTable.getSelectionModel().selectedItemProperty());
                
        final SplitPane sPane = new SplitPane();
        sPane.setOrientation(Orientation.VERTICAL);
        sPane.getItems().addAll(uiTable, commentPhotoView);
        sPane.setDividerPositions(0.9);
        setCenter(sPane);
        
        //
        // NAVIGATION FICHE PAR FICHE
        //
        navigationToolbar.getStyleClass().add("buttonbarleft");
        
        uiCurrent.setFont(Font.font(20));
        uiCurrent.getStyleClass().add("btn-without-style"); 
        uiCurrent.setAlignment(Pos.CENTER);
        uiCurrent.setTextFill(Color.WHITE);
        uiCurrent.setOnAction(this::goTo);
        
        uiPrevious.getStyleClass().add("btn-without-style"); 
        uiPrevious.setTooltip(new Tooltip("Fiche précédente."));
        uiPrevious.setOnAction((ActionEvent event) -> {
            uiTable.getSelectionModel().selectPrevious();
        });        

        uiNext.getStyleClass().add("btn-without-style"); 
        uiNext.setTooltip(new Tooltip("Fiche suivante."));
        uiNext.setOnAction((ActionEvent event) -> {
            uiTable.getSelectionModel().selectNext();
        });
        navigationToolbar.getChildren().addAll(uiPrevious, uiCurrent, uiNext);
        navigationToolbar.visibleProperty().bind(uiFicheMode.selectedProperty());

        uiFicheMode.setGraphic(playIcon);
        uiFicheMode.getStyleClass().add("btn-without-style"); 
        uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches."));
        
        // Update counter when we change selected element.
        final ChangeListener<Number> selectedIndexListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            uiCurrent.setText(""+(newValue.intValue()+1) + " / " + uiTable.getItems().size());
        };
        uiFicheMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    // If there's no selection, initialize on first element.
                    if (uiTable.getSelectionModel().getSelectedIndex() < 0) {
                        uiTable.getSelectionModel().select(0);
                    }
                    
                    // Prepare editor and bind its content to selection model. 
                    if (elementPane == null) {
                        elementPane = SIRS.generateEditionPane(uiTable.getSelectionModel().getSelectedItem());
                    }
                    elementPane.elementProperty().bind(uiTable.getSelectionModel().selectedItemProperty());
                    
                    // Prepare display
                    uiFicheMode.setGraphic(stopIcon);
                    uiFicheMode.setTooltip(new Tooltip("Passer en mode de tableau synoptique."));
                    
                    uiCurrent.setText("" + (uiTable.getSelectionModel().getSelectedIndex()+1) + " / " + uiTable.getItems().size());
                    uiTable.getSelectionModel().selectedIndexProperty().addListener(selectedIndexListener);
                    setCenter((Node) elementPane);
                    
                } else {
                    // Deconnect editor.
                    if (elementPane != null) {
                        elementPane.elementProperty().unbind();
                    }
                    
                    // Update display
                    uiTable.getSelectionModel().selectedIndexProperty().removeListener(selectedIndexListener);
                    setCenter(uiTable);
                    uiFicheMode.setGraphic(playIcon);
                    uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches."));
                }
            }
        });
        uiFicheMode.disableProperty().bind(fichableProperty.not());

        searchEditionToolbar.getChildren().add(0, uiFicheMode);
        
        topPane.setLeft(navigationToolbar);
    }
    
    /**
     * Définit l'élément en paramètre comme parent de tout élément créé via cette table.
     * 
     * Note : Ineffectif dans le cas où les éléments de la PojoTable sont créés 
     * et listés directement depuis un repository couchDB, ou que l'élément créé  
     * est déjà un CouchDB document. 
     * @param parentElement L'élément qui doit devenir le parent de tout objet créé via 
     * la PojoTable.
     */
    public void setParentElement(final Element parentElement) {
        parentElementProperty.set(parentElement);
    }
    
    /**
     * 
     * @return L'élément à affecter en tant que parent de tout élément créé via 
     * cette table. Peut être nul.
     */
    public Element getParentElement() {
        return parentElementProperty.get();
    }
        
    /**
     * 
     * @return La propriété contenant l'élément à affecter en tant que parent de
     *  tout élément créé via cette table. Jamais nulle, mais peut-être vide.
     */
    public SimpleObjectProperty<Element> parentElementProperty() {
        return parentElementProperty;
    }
    
    /**
     * Définit l'élément en paramètre comme principal référent de tout élément créé via cette table.
     * 
     * @param parentElement L'élément qui doit devenir le principal référent de tout objet créé via 
     * la PojoTable.
     */
    public void setOwnerElement(final Element parentElement) {
        ownerElementProperty.set(parentElement);
    }
    
    /**
     * 
     * @return L'élément principal référent de tout élément créé via 
     * cette table. Peut être nul.
     */
    public Element getOwnerElement() {
        return ownerElementProperty.get();
    }
        
    /**
     * 
     * @return La propriété contenant l'élément à affecter en tant que principal référent de
     *  tout élément créé via cette table. Jamais nulle, mais peut-être vide.
     */
    public SimpleObjectProperty<Element> ownerElementProperty() {
        return ownerElementProperty;
    }
    
    protected ObservableList<Element> getAllValues(){return allValues;}

    public BooleanProperty editableProperty(){
        return editableProperty;
    }
    public BooleanProperty cellEditableProperty(){
        return cellEditableProperty;
    }
    public BooleanProperty detaillableProperty(){
        return detaillableProperty;
    }
    public BooleanProperty fichableProperty(){
        return fichableProperty;
    }
    public BooleanProperty searchableProperty(){
        return searchableProperty;
    }
    public BooleanProperty openEditorOnNewProperty() {
        return openEditorOnNewProperty;
    }
    public BooleanProperty createNewProperty() {
        return createNewProperty;
    }
    public StringProperty tronconSourceProperty() {
        return tronconSourceProperty;
    }
    
    /**
     * Called when user click on the search icon. Prepare the popup with the textfield
     * to type research into. 
     */
    protected void search(){
        if(uiSearch.getGraphic()!= searchNone){
            //une recherche est deja en cours
            return;
        }
        
        final Popup popup = new Popup();
        final TextField textField = new TextField(currentSearch.get());
        popup.getContent().add(textField);
        popup.setAutoHide(true);
        textField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                currentSearch.set(textField.getText());
                popup.hide();
                setTableItems(() -> allValues);
            }
        });
        final Point2D sc = uiSearch.localToScreen(0, 0);
        popup.show(uiSearch, sc.getX(), sc.getY());
        
    }
    
    protected void goTo(ActionEvent event){
        final Popup popup = new Popup();
        NumberField indexEditor = new NumberField(NumberField.NumberType.Integer);
        
        popup.getContent().add(indexEditor);
        popup.setAutoHide(false);

        indexEditor.setOnAction((ActionEvent event1) -> {
            final Number indexToSelect = indexEditor.valueProperty().get();
            if (indexToSelect != null) {
                final int index = indexToSelect.intValue();
                if (index >= 0 && index < uiTable.getItems().size()) {
                    uiTable.getSelectionModel().select(index);
                }
            }
            popup.hide();
        });
        final Point2D sc = uiCurrent.localToScreen(0, 0);
        popup.show(uiSearch, sc.getX(), sc.getY());
    }
    
    /**
     * @return {@link TableView} used for element display.
     */
    public TableView getUiTable() {
        return uiTable;
    }
    
    /**
     * Start an asynchronous task which will update table content with the elements
     * provided by input supplier.
     * @param producer Data provider.
     */
    public void setTableItems(Supplier<ObservableList<Element>> producer) {        
        if (tableUpdater != null && !tableUpdater.isDone()) {
            tableUpdater.cancel();
        }
        
        tableUpdater = new TaskManager.MockTask("Recherche...", () -> {

            allValues = producer.get();

            final Thread currentThread = Thread.currentThread();
            if (currentThread.isInterrupted()) {
                return;
            }
            final String str = currentSearch.get();
            if (str == null || str.isEmpty() || allValues == null || allValues.isEmpty()) {
                filteredValues = allValues;
            } else {
                final Set<String> result = new HashSet<>();
                SearchResponse search = Injector.getElasticSearchEngine().search(QueryBuilders.queryString(str));
                Iterator<SearchHit> iterator = search.getHits().iterator();
                while (iterator.hasNext() && !currentThread.isInterrupted()) {
                    result.add(iterator.next().getId());
                }

                if (currentThread.isInterrupted()) {
                    return;
                }
                filteredValues = allValues.filtered((Element t) -> {
                    return result.contains(t.getDocumentId());
                });
            }
        });
        
        tableUpdater.stateProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (Worker.State.SUCCEEDED.equals(newValue)) {
                    Platform.runLater(() -> {
                        uiTable.setItems(filteredValues);
                        uiSearch.setGraphic(searchNone);
                    });
                } else if (Worker.State.FAILED.equals(newValue) || Worker.State.CANCELLED.equals(newValue)) {
                    Platform.runLater(() -> {
                        uiSearch.setGraphic(searchNone);
                    });
                } else if (Worker.State.RUNNING.equals(newValue)) {
                    Platform.runLater(() -> uiSearch.setGraphic(searchRunning));
                }
            }
        });
        tableUpdater = TaskManager.INSTANCE.submit("Recherche...", tableUpdater);
    }
    
    /**
     * Check if the input element can be deleted by current user. If not, an 
     * alert is displyed on screen.
     * @param pojo The element we want to delete.
     * @return True if we can delete the element in parameter, false otherwise.
     */
    protected boolean authoriseElementDeletion(final Element pojo) {
        if (session.getRole() == EXTERN) {
            if (!session.getUtilisateur().getId().equals(pojo.getAuthor())
                    || pojo.getValid()) {
                new Alert(Alert.AlertType.INFORMATION, "En tant qu'utilisateur externe, vous ne pouvez supprimer que des éléments invalidés dont vous êtes l'auteur.", ButtonType.OK).showAndWait();
                return false;
            }
        }
        return true;
    }
    
    /**
     * Delete the elements given in parameter. They are suppressed from the table
     * list, and if a {@link Repository} exists for the current table, elements 
     * are also suppressed from database. If a parent element has been set using
     * {@linkplain #setParentElement(fr.sirs.core.model.Element) } method, we will 
     * try to remove them from the parent as well.
     * @param pojos The {@link Element}s to delete.
     */
    protected void deletePojos(Element... pojos) {
        ObservableList<Element> items = uiTable.getItems();
        for (Element pojo : pojos) {
            // Si l'utilisateur est un externe, il faut qu'il soit l'auteur de 
            // l'élément et que celui-ci soit invalide, sinon, on court-circuite
            // la suppression.
            if(!authoriseElementDeletion(pojo)) continue;
            if (repo != null && createNewProperty.get()) {
                repo.remove(pojo);
            }
            
            if (parentElementProperty.get() != null) {
                parentElementProperty.get().removeChild(pojo);
            }else if(ownerElementProperty.get() != null){
                ownerElementProperty.get().removeChild(pojo);
                repo.remove(pojo);
            }
            items.remove(pojo);
        }
    }
    
    /**
     * Try to find and display a form to edit input object.
     * @param pojo The object we want to edit.
     */
    protected void editPojo(Object pojo){
        editElement(pojo);
    }
    
    /**
     * A method called when an element displayed in the table has been modified 
     * in the table.
     * @param event The table event refering to the edition action.
     */
    protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event){
        if(repo!=null){
            final Element obj = event.getRowValue();
            if(obj == null) return;
            repo.update(obj);
        }
    }
    
    protected Object addExistingPojo(final ValiditySummary summary) {
        Object result = null;
        if (repo != null) {
            result = repo.get(summary.getDocId());
        } 
        else {
            final Set<String> id = new HashSet<>();
            id.add(summary.getElementId());
            result = SIRS.getStructures(id, pojoClass).get(0);
        }
        
        
        if (result!=null) {
            uiTable.getItems().add((Element)result);
        } else {
            new Alert(Alert.AlertType.INFORMATION, "Aucune entrée ne peut être créée.").showAndWait();
        }
        return result;
    }
    
    /**
     * Create a new element and add it to table items. If the table {@link Repository}
     * is not null, we also add the element to the database. We also set its parent
     * if it's not a contained element and the table {@linkplain #parentElementProperty}
     * is set.
     * @return The newly created object. 
     */
    protected Object createPojo() {
        Object result = null;
        if (repo != null) {
            result = repo.create();
            if(result instanceof Element) {
                ((Element)result).setAuthor(session.getUtilisateur().getId());
                ((Element)result).setValid(!(session.getRole()==Role.EXTERN));
            }
            repo.add(result);
        } 
        
        else if (pojoClass != null) {
            try {
                result = pojoClass.newInstance();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        // TODO : check and set date début
        if (result instanceof Element) {
            final Element newlyCreated = (Element)result;
            newlyCreated.setAuthor(session.getUtilisateur().getId());
            newlyCreated.setValid(!(session.getRole()==Role.EXTERN));
            
            /* Dans le cas où on a un parent, il n'est pas nécessaire de faire
            addChild(), car la liste des éléments de la table est directement 
            cette liste d'éléments enfants, sur laquelle on fait un add().*/
            if (parentElementProperty.get() != null) {
                // this should do nothing for new 
                newlyCreated.setParent(parentElementProperty.get());
            } 
            /* Mais dans le cas où on a un référant principal, il faut faire un
            addChild(), car la liste des éléments de la table n'est pas une 
            liste d'éléments enfants. Le référant principal n'a qu'une liste 
            d'identifiants qu'il convient de mettre à jour avec addChild().*/
            else if(ownerElementProperty.get() != null){
                ownerElementProperty.get().addChild(newlyCreated);
            }
            uiTable.getItems().add((Element)result);
        } else {
            new Alert(Alert.AlertType.INFORMATION, "Aucune entrée ne peut être créée.").showAndWait();
        }
        return result;
    }
        
    public static void editElement(Object pojo) {
        try {
            Injector.getSession().showEditionTab(pojo);
        } catch (Exception ex) {
            Dialog d = new Alert(Alert.AlertType.ERROR, "Impossible d'afficher un éditeur", ButtonType.OK);
            d.showAndWait();
            throw new UnsupportedOperationException("Failed to load panel : " + ex.getMessage(), ex);
        }
    }
    
    private class EnumColumn extends TableColumn<Element, Role>{
        private EnumColumn(PropertyDescriptor desc){
            super(labelMapper.mapPropertyName(desc.getDisplayName()));
            setEditable(false);
            setCellValueFactory(new PropertyValueFactory<>(desc.getName()));
            setCellFactory(new Callback<TableColumn<Element, Role>, TableCell<Element, Role>>() {

                @Override
                public TableCell<Element, Role> call(TableColumn<Element, Role> param) {
                    final TableCell<Element, Role> cell = new FXEnumTableCell<>(Role.class, new SirsStringConverter());
                    cell.setEditable(false);
                    return cell;
                }

            });     
            addEventHandler(TableColumn.editCommitEvent(), new EventHandler<CellEditEvent<Element, Object>>() {

                @Override
                public void handle(CellEditEvent<Element, Object> event) {
                    final Object rowElement = event.getRowValue();
                    new PropertyReference<>(rowElement.getClass(), desc.getName()).set(rowElement, event.getNewValue());
                    elementEdited(event);
                }
            });
        }
    }

    private class PasswordColumn extends TableColumn<Element, String>{
        private PasswordColumn(PropertyDescriptor desc){
            super(labelMapper.mapPropertyName(desc.getDisplayName()));
            setEditable(false);
            setCellValueFactory(new PropertyValueFactory<>("password"));
            setCellFactory(new Callback<TableColumn<Element, String>, TableCell<Element, String>>() {

                @Override
                public TableCell<Element, String> call(TableColumn<Element, String> param) {
                    MessageDigest messageDigest = null;
                    try {
                        messageDigest = MessageDigest.getInstance("MD5");
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(PojoTable.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    final TableCell<Element, String> cell = new FXPasswordTableCell<>(messageDigest);
                    cell.setEditable(false);
                    return cell;
                }
            });
            addEventHandler(TableColumn.editCommitEvent(), new EventHandler<CellEditEvent<Element, Object>>() {

                @Override
                public void handle(CellEditEvent<Element, Object> event) {
                    final Element rowElement = event.getRowValue();
                    new PropertyReference<>(rowElement.getClass(), "password").set(rowElement, event.getNewValue());
                    elementEdited(event);
                }
            });
        }
    }
        
        
    public class PropertyColumn extends TableColumn<Element,Object>{
        
        public PropertyColumn(final PropertyDescriptor desc) {
            super(labelMapper.mapPropertyName(desc.getDisplayName()));
            
            final Reference ref = desc.getReadMethod().getAnnotation(Reference.class);
                        
            addEventHandler(TableColumn.editCommitEvent(), new EventHandler<CellEditEvent<Element, Object>>() {

                @Override
                public void handle(CellEditEvent<Element, Object> event) {
                    final Object rowElement = event.getRowValue();
                    new PropertyReference<>(rowElement.getClass(), desc.getName()).set(rowElement, event.getNewValue());
                    elementEdited(event);
                }
            });
            
            //choix de l'editeur en fonction du type de données          
            if (ref != null) {
                //reference vers un autre objet
                setEditable(false);
                setCellFactory((TableColumn<Element, Object> param) -> new SirsTableCell());            
                try {
                    final Method propertyAccessor = pojoClass.getMethod(desc.getName()+"Property");
                    setCellValueFactory((CellDataFeatures<Element, Object> param) -> {
                        try {
                            return (ObservableValue) propertyAccessor.invoke(param.getValue());
                        } catch (Exception ex) {
                            SirsCore.LOGGER.log(Level.WARNING, null, ex);
                            return null;
                        }
                    });
                } catch (Exception ex) {
                    setCellValueFactory(new PropertyValueFactory<>(desc.getName()));
                } 

            } else {
                setCellValueFactory(new PropertyValueFactory<>(desc.getName()));
                final Class type = desc.getReadMethod().getReturnType();
                boolean isEditable = true;
                if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXBooleanCell());
                } else if (String.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXStringCell());
                } else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(NumberField.NumberType.Integer));
                } else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell.Float(NumberField.NumberType.Normal));
                } else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(NumberField.NumberType.Normal));
                } else if (LocalDateTime.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXLocalDateTimeCell());
                }else {
                    isEditable = false;
                }
                
                setEditable(isEditable);
                if (isEditable) {
                    editableProperty().bind(cellEditableProperty);
                }
            }
        }  
    }

    /**
     * A column allowing to delete the {@link Element} of a row. Two modes possible :
     * - Concrete deletion, which remove the element from database
     * - unlink mode, which dereference element from current list and parent element.
     */
    public class DeleteColumn extends TableColumn<Element,Element>{
        
        public DeleteColumn() {
            super("Suppression");            
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_DELETE));
            
            setCellValueFactory((TableColumn.CellDataFeatures<Element, Element> param) -> new SimpleObjectProperty<>(param.getValue()));
            setCellFactory((TableColumn<Element, Element> param) -> {
                final boolean realDelete = createNewProperty.get();
                return new ButtonTableCell<>(
                        false, realDelete? new ImageView(GeotkFX.ICON_DELETE) : new ImageView(GeotkFX.ICON_UNLINK), (Element t) -> true, (Element t) -> {
                            final Alert confirm;
                            if (realDelete) {
                                confirm = new Alert(Alert.AlertType.WARNING, "Vous allez supprimer DEFINITIVEMENT l'entrée de la base de données. Êtes-vous sûr ?", ButtonType.NO, ButtonType.YES);
                            } else {
                                confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le lien ?", ButtonType.NO, ButtonType.YES);
                            }
                            final Optional<ButtonType> res = confirm.showAndWait();
                            if (res.isPresent() && ButtonType.YES.equals(res.get())) {
                                deletePojos(t);
                            }
                            return null;
                        });
            });
        }  
    }
    
    public static class EditColumn extends TableColumn<Object,Object>{

        public EditColumn(Consumer editFct) {
            super("Edition");        
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(SIRS.ICON_EDIT));
            
            setCellValueFactory((TableColumn.CellDataFeatures<Object, Object> param) -> new SimpleObjectProperty<>(param.getValue()));
            setCellFactory((TableColumn<Object,Object> param) -> new ButtonTableCell(
                    false,new ImageView(SIRS.ICON_EDIT), (Object t) -> true, new Function<Object, Object>() {
                @Override
                public Object apply(Object t) {
                    editFct.accept(t);
                    return t;
                }
            }));
        }  
    }
    
    protected class ValidatedTableRow extends TableRow<Element>{

        @Override
        protected void updateItem(Element item, boolean empty) {
            super.updateItem(item, empty);

            if(item!=null && !item.getValid()){
                    getStyleClass().add("invalidRow");
                }
                else{
                    getStyleClass().removeAll("invalidRow");
                }
        }
    }
    
    private class ChoiceStage extends Stage{
        
        private ObjectProperty retrievedElement = new SimpleObjectProperty();
        
        private ChoiceStage(){
            super();
            setTitle("Choix de l'élément");
            
            final ResourceBundle bundle = ResourceBundle.getBundle(pojoClass.getName());
            final String prefix = bundle.getString("classAbrege")+" : ";
            final ComboBox<ValiditySummary> comboBox;
            if(tronconSourceProperty.get()==null){
                comboBox = new ComboBox<ValiditySummary>(FXCollections.observableArrayList(Injector.getSession().getValiditySummaryRepository().getDesignationsForClass(pojoClass)));
            }
            else{
                
                comboBox = new ComboBox<ValiditySummary>(FXCollections.observableArrayList(Injector.getSession().getValiditySummaryRepository().getDesignationsForClass(pojoClass)).filtered(new Predicate<ValiditySummary>() {

                    @Override
                    public boolean test(ValiditySummary t) {
                        return tronconSourceProperty.get().equals(t.getDocId());
                    }
                }));
            }
            comboBox.setConverter(new StringConverter<ValiditySummary>() {

                @Override
                public String toString(ValiditySummary object) {
                    return prefix+object.getDesignation() + ((object.getLabel()==null) ? "" : " - "+object.getLabel());
                }

                @Override
                public ValiditySummary fromString(String string) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
            
            final Button cancel = new Button("Annuler");
            cancel.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    retrievedElement.set(null);
                    hide();
                }
            });
            final Button add = new Button("Ajouter");
            add.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    retrievedElement.set(addExistingPojo(comboBox.valueProperty().get()));
                    hide();
                }
            });
            final HBox hBox = new HBox(cancel, add);
            hBox.setAlignment(Pos.CENTER);
            hBox.setPadding(new Insets(20));
            
            final VBox vBox = new VBox(comboBox, hBox);
            vBox.setAlignment(Pos.CENTER);
            vBox.setPadding(new Insets(20));
            setScene(new Scene(vBox));
        }
        
        private ObjectProperty getRetrievedElement(){
            return retrievedElement;
        }
    }
    
}
