
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
import fr.sirs.core.model.Contact;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.Organisme;
import fr.sirs.core.model.Positionable;
import fr.sirs.index.SearchEngine;
import fr.sirs.other.FXContactPane;
import fr.sirs.other.FXOrganismePane;
import fr.sirs.util.SirsTableCell;
import fr.sirs.util.property.Reference;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.util.Callback;
import jidefx.scene.control.field.NumberField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.gui.javafx.util.FXTableView;
import org.geotoolkit.internal.GeotkFX;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class PojoTable extends BorderPane{
    
    private static final Comparator COMPARATOR = new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                if(o1==null && o2==null) return 0;
                if(o1==null) return -1;
                if(o2==null) return +1;

                if(o1 instanceof Boolean){
                    return Boolean.compare((Boolean)o1, (Boolean)o2);
                }else if(o1 instanceof Number){
                    final double d1 = ((Number)o1).doubleValue();
                    final double d2 = ((Number)o2).doubleValue();
                    return Double.compare(d1, d2);
                }else if(o1 instanceof String){
                    return ((String)o1).compareToIgnoreCase((String)o2);
                }else if(o1 instanceof LocalDateTime){
                    return ((LocalDateTime)o1).compareTo((LocalDateTime)o2);
                }else {
                    return 0;
                }
            }
        };
    
    private final TableView<Element> uiTable = new FXTableView<>();
    protected final ScrollPane uiScroll = new ScrollPane(uiTable);
    protected final Class pojoClass;
    private final Repository repo;
    protected final Session session = Injector.getBean(Session.class);
    
    //valeurs affichées
    private final BooleanProperty editableProperty = new SimpleBooleanProperty(true);
    private final ImageView searchNone = new ImageView(SIRS.ICON_SEARCH);
    private final ProgressIndicator searchRunning = new ProgressIndicator();
    private ObservableList<Element> allValues;
    private ObservableList<Element> filteredValues;
    private final Button uiSearch;
    private final StringProperty currentSearch = new SimpleStringProperty("");
    
    public PojoTable(Class pojoClass, String title) {
        this(pojoClass, title, null);
    }
    
    public PojoTable(Repository repo, String title) {
        this(repo.getModelClass(), title, repo);
    }
    
    private PojoTable(Class pojoClass, String title, Repository repo) {
        getStylesheets().add(SIRS.CSS_PATH);
        this.pojoClass = pojoClass;
        this.repo = repo;
        
        searchRunning.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        searchRunning.setPrefSize(22, 22);
        searchRunning.setStyle(" -fx-progress-color: white;");
        uiScroll.setFitToHeight(true);
        uiScroll.setFitToWidth(true);
        uiTable.setEditable(true);
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                
        setCenter(uiScroll);
        
        //contruction des colonnes editable
        final List<PropertyDescriptor> properties = Session.listSimpleProperties(pojoClass);
        uiTable.getColumns().add(new DeleteColumn());
        uiTable.getColumns().add(new EditColumn());
        for(PropertyDescriptor desc : properties){
            final PropertyColumn col = new PropertyColumn(desc); 
             uiTable.getColumns().add(col);
             //sauvegarde sur chaque changement dans la table
             col.addEventHandler(TableColumn.editCommitEvent(), (TableColumn.CellEditEvent<Element, Object> event) -> {
                 elementEdited(event);
             });
        }
        
        
        //barre d'outils
        final Button uiAdd = new Button(null, new ImageView(SIRS.ICON_ADD));
        uiAdd.getStyleClass().add("btn-without-style");
        uiAdd.setOnAction((ActionEvent event) -> {createPojo();});
        
        final Button uiDelete = new Button(null, new ImageView(SIRS.ICON_TRASH));
        uiDelete.getStyleClass().add("btn-without-style");
        uiDelete.setOnAction((ActionEvent event) -> {
            final Element[] elements = ((List<Element>)uiTable.getSelectionModel().getSelectedItems()).toArray(new Element[0]);
            if(elements.length>0){
                final ButtonType res = new Alert(Alert.AlertType.CONFIRMATION,"Confirmer la suppression ?", 
                        ButtonType.NO, ButtonType.YES).showAndWait().get();
                if(ButtonType.YES == res){
                    deletePojos(elements);
                }
                
            }
        });
        
        uiSearch = new Button(null, searchNone);
        uiSearch.textProperty().bind(currentSearch);
        uiSearch.getStyleClass().add("btn-without-style");        
        uiSearch.setOnAction((ActionEvent event) -> {search();});
        uiSearch.getStyleClass().add("label-header");
        
        final Label uiTitle = new Label(title);
        uiTitle.getStyleClass().add("pojotable-header");   
        uiTitle.setAlignment(Pos.CENTER);
        
        uiTable.editableProperty().bind(editableProperty);
        uiAdd.visibleProperty().bind(editableProperty);
        uiDelete.visibleProperty().bind(editableProperty);
        
        final HBox toolbar = new HBox(uiAdd,uiDelete,uiSearch);     
        toolbar.getStyleClass().add("buttonbar");
        final BorderPane top = new BorderPane(uiTitle,null,toolbar,null,null);
        setTop(top);
        
        if(repo!=null){
            updateTable();
        }
        
    }

    public BooleanProperty editableProperty(){
        return editableProperty;
    }
    
    protected void search(){
        if(uiSearch.getGraphic()!= searchNone){
            //une recherche est deja en cours
            return;
        }
        
        final Popup popup = new Popup();
        final TextField textField = new TextField(currentSearch.get());
        popup.getContent().add(textField);
        
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
    
    public TableView getUiTable() {
        return uiTable;
    }
    
    public void setTableItems(Supplier<ObservableList<Element>> producer){
        uiSearch.setGraphic(searchRunning);
        
        new Thread(){
            @Override
            public void run() {
                allValues = producer.get();
                final String str = currentSearch.get();
                if(str == null || str.isEmpty()){
                    filteredValues = allValues;
                }else{
                    final SearchEngine searchEngine = Injector.getSearchEngine();
                    final String type = pojoClass.getSimpleName();
                    final Set<String> result = new HashSet<>();
                    try {
                        result.addAll(searchEngine.search(type, str.split(" ")));
                    } catch (ParseException | IOException ex) {
                        SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }

                    filteredValues = allValues.filtered((Element t) -> {
                        return result.contains(t.getDocumentId());
                    });
                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        uiTable.setItems(filteredValues);
                        uiSearch.setGraphic(searchNone);
                    }
                });
            }
        }.start();
    }
        
    
    protected void deletePojos(Element ... pojos){
        if(repo!=null){
            for(Element pojo : pojos){
                repo.remove(pojo);
            }
            updateTable(); 
        }
    }
    
    protected void editPojo(Element pojo){
        final Tab tab = new Tab();
        
        Node content = new BorderPane();
        if(pojo instanceof Positionable){
            content = new FXStructurePane((Objet) pojo);
        }else if(pojo instanceof Contact){
            content = new FXContactPane((Contact) pojo);
        }else if(pojo instanceof Organisme){
            content = new FXOrganismePane((Organisme)pojo);
        }
        tab.setContent(content);
        
        
        tab.setText(pojo.getClass().getSimpleName());
        tab.setOnSelectionChanged(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if(tab.isSelected()){
                    session.prepareToPrint(pojo);
                }
            }
        });
        session.getFrame().addTab(tab);
    }
    
    protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event){
        if(repo!=null){
            final Element obj = event.getRowValue();
            if(obj == null) return;
            repo.update(obj);
        }
    }
    
    protected void createPojo() {
        if(repo!=null){
            repo.add(repo.create());
            updateTable();
        }
    }
        
    private void updateTable(){
        if(repo!=null){
            setTableItems(()-> FXCollections.observableList(repo.getAll()));
        }
    }
    
    
    public static class PropertyColumn extends TableColumn<Element,Object>{

        private final PropertyDescriptor desc;

        public PropertyColumn(final PropertyDescriptor desc) {
            super(desc.getDisplayName());
            this.desc = desc;
            setEditable(true);
            
            final Reference ref = desc.getReadMethod().getAnnotation(Reference.class);
                        
            addEventHandler(TableColumn.editCommitEvent(), (CellEditEvent<Object, Object> event) -> {
                final Object rowElement = event.getRowValue();
                new PropertyReference<>(rowElement.getClass(), desc.getName()).set(rowElement, event.getNewValue());
            });
            
            //choix de l'editeur en fonction du type de données          
            if(ref!=null){
                //reference vers un autre objet
                setEditable(false);
                setCellValueFactory(new CellLinkValueFactory(desc, ref.ref()));
                setCellFactory((TableColumn<Element, Object> param) -> new SirsTableCell());
                
            }else{
                final Class type = desc.getReadMethod().getReturnType();  
                
                setCellValueFactory(new PropertyValueFactory<>(desc.getName()));
                if(Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)){
                    setCellFactory((TableColumn<Element, Object> param) -> new FXBooleanCell());
                }else if(String.class.isAssignableFrom(type)){
                    setCellFactory((TableColumn<Element, Object> param) -> new FXStringCell());
                }else if(Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)){
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(NumberField.NumberType.Integer));
                }else if(Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)){
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(NumberField.NumberType.Normal));
                }else if(Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)){
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(NumberField.NumberType.Normal));
                }else if(LocalDateTime.class.isAssignableFrom(type)){
                    setCellFactory((TableColumn<Element, Object> param) -> new FXLocalDateTimeCell());
                }
                
                setComparator(COMPARATOR);
            }
            
        }  
    }
    
    public static class CellLinkValueFactory implements Callback<TableColumn.CellDataFeatures<Element, Object>, ObservableValue<Object>>{

        private final Session session;
        private final PropertyDescriptor desc;
        private final Class refClass;

        public CellLinkValueFactory(PropertyDescriptor desc, Class refClass) {
            this.session = Injector.getBean(Session.class);
            this.desc = desc;
            this.refClass = refClass;
        }
        
        @Override
        public ObservableValue<Object> call(CellDataFeatures<Element, Object> param) {
            Object obj = null;
            try {
                obj = desc.getReadMethod().invoke(param.getValue());
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
            
            final SimpleObjectProperty res = new SimpleObjectProperty();
            
            if(obj instanceof String && !((String)obj).isEmpty()){
                final String id = (String)obj;
                res.set(session.getConnector().get(refClass,id));
            }
            
            return res;
        }
        
    }
    
    public class DeleteColumn extends TableColumn<Element,Element>{

        public DeleteColumn() {
            super();            
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_DELETE));
            DeleteColumn.this.editableProperty().bind(editableProperty);
            DeleteColumn.this.visibleProperty().bind(editableProperty);
            
            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Element, Element>, ObservableValue<Element>>() {
                @Override
                public ObservableValue<Element> call(TableColumn.CellDataFeatures<Element, Element> param) {
                    return new SimpleObjectProperty<>(param.getValue());
                }
            });
            setCellFactory((TableColumn<Element, Element> param) -> new ButtonTableCell<>(
                    false,new ImageView(GeotkFX.ICON_DELETE), (Element t) -> true, new Function<Element, Element>() {
                @Override
                public Element apply(Element t) {
                    final ButtonType res = new Alert(Alert.AlertType.CONFIRMATION,"Confirmer la suppression ?", 
                            ButtonType.NO, ButtonType.YES).showAndWait().get();
                    if(ButtonType.YES == res){
                        deletePojos(t);
                    }
                    return null;
                }
            }));
        }  
    }
    
    public class EditColumn extends TableColumn<Element,Element>{

        public EditColumn() {
            super();            
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_EDIT));
            EditColumn.this.editableProperty().bind(editableProperty);
            EditColumn.this.visibleProperty().bind(editableProperty);
            
            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Element, Element>, ObservableValue<Element>>() {
                @Override
                public ObservableValue<Element> call(TableColumn.CellDataFeatures<Element, Element> param) {
                    return new SimpleObjectProperty<>(param.getValue());
                }
            });
            setCellFactory((TableColumn<Element, Element> param) -> new ButtonTableCell<>(
                    false,new ImageView(GeotkFX.ICON_EDIT), (Element t) -> true, new Function<Element, Element>() {
                @Override
                public Element apply(Element t) {
                    editPojo(t);
                    return t;
                }
            }));
        }  
    }
    
}
