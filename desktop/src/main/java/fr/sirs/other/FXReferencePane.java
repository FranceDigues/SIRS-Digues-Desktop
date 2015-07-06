
package fr.sirs.other;

import fr.sirs.Injector;
import fr.sirs.ReferenceChecker;
import static fr.sirs.SIRS.BUNDLE_KEY_CLASS;
import static fr.sirs.SIRS.ICON_CHECK_CIRCLE;
import static fr.sirs.SIRS.ICON_EXCLAMATION_CIRCLE;
import fr.sirs.Session;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.FXFreeTab;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public class FXReferencePane<T extends ReferenceType> extends BorderPane {
    
    private final ReferencePojoTable references;
    private final Session session = Injector.getSession();
    private final ReferenceChecker referenceChecker;
        
    public FXReferencePane(final Class<T> type) {
        final ResourceBundle bundle = ResourceBundle.getBundle(type.getName(), Locale.getDefault(), Thread.currentThread().getContextClassLoader());
        referenceChecker = session.getReferenceChecker();
        references = new ReferencePojoTable(type, bundle.getString(BUNDLE_KEY_CLASS));
        references.editableProperty().set(false);
        references.fichableProperty().set(false);
        references.detaillableProperty().set(false);
        references.searchableProperty().set(false);
        this.setCenter(references);
    }
    
    
    private class ReferencePojoTable extends PojoTable{
        
        private final List<? extends ReferenceType> serverInstanceNotLocal;
        private final List<ReferenceType> localInstancesNotOnTheServer;

        public ReferencePojoTable(Class<T> pojoClass, String title) {
            super(pojoClass, title);
            
            serverInstanceNotLocal = referenceChecker.getServerInstancesNotLocal().get(pojoClass);
            localInstancesNotOnTheServer = referenceChecker.getLocalInstancesNotOnTheServer().get(pojoClass);
            
            getColumns().replaceAll((TableColumn<Element, ?> t) -> {
                    if(t instanceof DeleteColumn) return new StateColumn();
                    else return t;
                });
            
            getTable().setRowFactory((TableView<Element> param) -> {
                    return new ReferenceTableRow();
                });
            
            final ObservableList allItems = FXCollections.observableArrayList(repo.getAll());
            if(serverInstanceNotLocal!=null && !serverInstanceNotLocal.isEmpty()){
                final List<Element> newServerInstances = new ArrayList<>();
                for(final Object asObject : serverInstanceNotLocal){
                    if(asObject instanceof Element){
                        newServerInstances.add((Element) asObject);
                    }
                }
                allItems.addAll(newServerInstances);
            }
            
            setTableItems(() -> {return allItems;});
        }

        private class ReferenceTableRow extends TableRow<Element>{

            @Override
            protected void updateItem(Element item, boolean empty) {
                super.updateItem(item, empty);
                
                if(item!=null){
                    // La mise à jour des références nouvelles et incohérentes est automatique.
//                    if(incoherentReferences!=null
//                            && incoherentReferences.get(item)!=null){
//                        getStyleClass().add("incoherentReferenceRow");
//                        
//                    } else if(serverInstanceNotLocal!=null
//                            && serverInstanceNotLocal.contains(item)){
//                        getStyleClass().add("newReferenceRow");
//                    } else 
                        if(localInstancesNotOnTheServer!=null
                            && localInstancesNotOnTheServer.contains(item)){
                        getStyleClass().add("deprecatedReferenceRow");
                    }
                    else{
                        getStyleClass().removeAll("incoherentReferenceRow", "newReferenceRow", "deprecatedReferenceRow");
                    }
                }
            }
        }
        
        private class StateButtonTableCell extends ButtonTableCell<Element, ReferenceType>{

            private final Node defaultGraphic;

            public StateButtonTableCell(Node graphic) {
                super(true, graphic, (ReferenceType t) -> true, new Function<ReferenceType, ReferenceType>() {
                    @Override
                    public ReferenceType apply(ReferenceType t) {

//                        if (localInstancesNotOnTheServer != null
//                                && localInstancesNotOnTheServer.contains(t)) {
                            final Tab tab = new FXFreeTab("Analyse de la base");
                            tab.setContent(new FXReferenceAnalysePane(t));
                            Injector.getSession().getFrame().addTab(tab);
//                        }
//                        else{
//                            new Alert(Alert.AlertType.INFORMATION, "Cette référence est à jour.", ButtonType.OK).showAndWait();
//                        }
                        return t;
                    }
                });
                defaultGraphic = graphic;
            }

            @Override
            protected void updateItem(ReferenceType item, boolean empty) {
                super.updateItem(item, empty); 
                
                if(item!=null){
                    // La mise à jour des références incohérentes et nouvelles est automatique.
//                    if(incoherentReferences!=null 
//                            && incoherentReferences.get(item)!=null){
//                        button.setGraphic(new ImageView(ICON_EXCLAMATION_TRIANGLE));
//                        button.setText("Incohérente");
//                        decorate(true);
//                    } 
//                    else if(serverInstanceNotLocal!=null
//                            && serverInstanceNotLocal.contains(item)){
//                        button.setGraphic(new ImageView(ICON_DOWNLOAD));
//                        button.setText("Nouvelle");
//                        decorate(false);
//                    } 
//                    else 
                        if(localInstancesNotOnTheServer!=null
                            && localInstancesNotOnTheServer.contains(item)){
                        button.setGraphic(new ImageView(ICON_EXCLAMATION_CIRCLE));
                        button.setText("Dépréciée");
                    }
                    else{
                        button.setGraphic(defaultGraphic);
                        button.setText("À jour");
                    }
                }
            }
        }
        
        
    
    private class StateColumn extends TableColumn<Element, ReferenceType>{

        public StateColumn() {
            super("État");     
            setEditable(false);
            setSortable(false);
            setResizable(true);
            setPrefWidth(70);
            
            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Element, ReferenceType>, ObservableValue<ReferenceType>>() {
                @Override
                public ObservableValue<ReferenceType> call(TableColumn.CellDataFeatures<Element, ReferenceType> param) {
                    return new SimpleObjectProperty<>((ReferenceType)param.getValue());
                }
            });
            
            setCellFactory(new Callback<TableColumn<Element, ReferenceType>, TableCell<Element, ReferenceType>>() {

                @Override
                public TableCell<Element, ReferenceType> call(TableColumn<Element,ReferenceType> param) {
                    
                    return new StateButtonTableCell(new ImageView(ICON_CHECK_CIRCLE)); 
                }
            });
        }  
    }
    }
}
