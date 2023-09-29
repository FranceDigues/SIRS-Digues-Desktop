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
package fr.sirs.digue;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.component.BorneDigueRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.*;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.theme.ui.pojotable.RenameBorneColumn;
import fr.sirs.ui.Growl;
import fr.sirs.util.BorneUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.apache.sis.util.logging.Logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Estelle Idee (Geomatys)
 */
public class FXSystemeReperagePane extends BorderPane {

    protected static final Logger LOGGER = Logging.getLogger("Systeme Reperage pane - digue");
    @FXML private TextField uiNom;
    @FXML private TextArea uiComment;
    @FXML private DatePicker uiDate;

    private final ObjectProperty<SystemeReperage> srProperty = new SimpleObjectProperty<>();
    private final BorneTable borneTable = new BorneTable(srProperty);
    private final BooleanProperty editableProperty = new SimpleBooleanProperty(true);

    public FXSystemeReperagePane(){
        SIRS.loadFXML(this);

        setCenter(borneTable);

        srProperty.addListener(this::updateFields);

        this.visibleProperty().bind(srProperty.isNotNull());

        uiNom.editableProperty().bind(editableProperty);
        uiComment.disableProperty().bind(editableProperty.not());
        uiDate.setDisable(true);
        // Client query...
        borneTable.editableProperty().bind(editableProperty);
    }

    public BooleanProperty editableProperty(){
        return editableProperty;
    }

    public ObjectProperty<SystemeReperage> getSystemeReperageProperty() {
        return srProperty;
    }

    private void updateFields(ObservableValue<? extends SystemeReperage> observable, SystemeReperage oldValue, SystemeReperage newValue) {
        if (oldValue != null) {
            uiNom.textProperty().unbindBidirectional(oldValue.libelleProperty());
            uiComment.textProperty().unbindBidirectional(oldValue.commentaireProperty());
            uiDate.valueProperty().unbindBidirectional(oldValue.dateMajProperty());
            borneTable.getUiTable().setItems(FXCollections.emptyObservableList());
        }

        if(newValue==null) return;
        uiNom.textProperty().bindBidirectional(newValue.libelleProperty());
        uiDate.valueProperty().bindBidirectional(newValue.dateMajProperty());
        uiComment.textProperty().bindBidirectional(newValue.commentaireProperty());

        borneTable.getUiTable().setItems(newValue.systemeReperageBornes.sorted((sr1, sr2) -> Float.compare(sr1.getValeurPR(), sr2.getValeurPR())));
    }


    public void save(){
        final SystemeReperage sr = srProperty.get();
        if(sr==null) return;

        final Session session = Injector.getBean(Session.class);
        final SystemeReperageRepository repo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);

        final String tcId = sr.getLinearId();
        if (tcId == null || tcId.isEmpty()) {
            throw new IllegalArgumentException("Aucun tronçon n'est associé au SR. Sauvegarde impossible.");
        }
        final TronconDigue troncon = session.getRepositoryForClass(TronconDigue.class).get(tcId);
        repo.update(sr, troncon);
    }

    private class BorneTable extends PojoTable {

        private final Button uiEditBorne = new Button(null, new ImageView(SIRS.ICON_EDITION_WHITE));

        private int selectedItemsCount = 0;

        // listener to disable/enable the borne renaming buttons.
        // Can only rename one borne at a time.
        ListChangeListener<Element> selectionListener =  c -> uiEditBorne.setDisable(selectedItemsCount != 1);

        public BorneTable(final ObjectProperty<? extends Element> container) {
            super(SystemeReperageBorne.class, "Liste des bornes", container);
            getColumns().remove((TableColumn) editCol);
            fichableProperty.set(false);
            uiAdd.setVisible(false);
            uiDelete.setVisible(false);
            uiFicheMode.setVisible(false);
            deleteColumn.setVisible(false);

            // HACK-redmine-6551 : rename a borne
            uiTable.getSelectionModel().select(null);

            uiEditBorne.setTooltip(new Tooltip("Renommer une borne"));
            uiEditBorne.setOnAction(event -> renameBorne(uiTable.getSelectionModel().getSelectedItem()));
            uiEditBorne.getStyleClass().add(BUTTON_STYLE);

            // Add button to edition bar
            searchEditionToolbar.getChildren().add(2, uiEditBorne);

            // listener to count the number of selected items. Can only rename one borne at a time.
            uiTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super Element>) c -> {
                selectedItemsCount = uiTable.getSelectionModel().getSelectedItems().size();
            });

            RenameBorneColumn renameBorneCol = new RenameBorneColumn(getDefaultValueFactory(), this::renameBorne, getDefaultVisiblePredicate());

            editableProperty.addListener((
                    ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if (newValue) {
                    uiTable.getColumns().add(1, renameBorneCol);
                    uiTable.getSelectionModel().getSelectedItems().addListener(selectionListener);
                    // Can only rename one borne at a time.
                    uiEditBorne.setDisable(selectedItemsCount != 1);
                }
                else {
                    uiTable.getColumns().remove(renameBorneCol);
                    uiTable.getSelectionModel().getSelectedItems().removeListener(selectionListener);
                    uiEditBorne.setDisable(true);
                }
            });
        }

        /**
         * Method to rename a borne from the @{@link SystemeReperageBorne} pojo table
         * @param pojo the pojo of the selected line - null if action from button bar
         * @return the updated @{@link BorneDigue}
         */
        private Object renameBorne(Object pojo) {

            final int index;
            if (pojo == null) return pojo;

            if ((index = uiTable.getItems().indexOf(pojo)) == -1) return pojo;
            TableView.TableViewSelectionModel<Element> selectionModel =  uiTable.getSelectionModel();
            selectIndex(selectionModel, index);

            if (!(pojo instanceof SystemeReperageBorne))
                return pojo;

            SystemeReperageBorne srBorne = (SystemeReperageBorne) pojo;
            BorneDigue selectedItem = InjectorCore.getBean(BorneDigueRepository.class).get(srBorne.getBorneId());

            if (selectedItem == null) {
                LOGGER.log(Level.WARNING, "There is no BorneDigue with id " + srBorne.getBorneId());
                return srBorne;
            }

            if (!BorneUtils.openDialogAndRenameBorne(selectedItem)) return srBorne;

            // Force update pojo table
            SystemeReperage sr = srProperty.get();
            sr.getSystemeReperageBornes().remove((SystemeReperageBorne) pojo);
            SystemeReperageBorne newSrBorne = srBorne.copy();
            sr.systemeReperageBornes.add(newSrBorne);
            Platform.runLater(() -> {
                Growl growl = new Growl(Growl.Type.WARNING, "Attention, la désignation de la borne est différente\nde la désignation de la borne dans le système de repérage.");
                growl.show(Duration.seconds(10));
            });

            borneTable.getUiTable().setItems(sr.systemeReperageBornes.sorted((sr1, sr2) -> Float.compare(sr1.getValeurPR(), sr2.getValeurPR())));
            selectBorne(selectionModel, newSrBorne);

            return srBorne;
        }

        @Override
        protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event) {
            //on ne sauvegarde pas, le formulaire conteneur s'en charge
        }

        private void selectIndex(TableView.TableViewSelectionModel<Element> selectionModel, int index) {
            selectionModel.clearSelection();
            selectionModel.select(index);
        }

        private void selectBorne(TableView.TableViewSelectionModel<Element> selectionModel, Element srBorne) {
            selectionModel.clearSelection();
            selectionModel.select(srBorne);
        }
    }
}
