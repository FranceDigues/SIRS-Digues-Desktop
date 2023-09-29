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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.Previews;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.*;
import fr.sirs.digue.FXSystemeReperagePane;
import fr.sirs.theme.AbstractTheme;
import fr.sirs.theme.AbstractTheme.ThemeManager;
import fr.sirs.theme.TronconTheme;
import fr.sirs.theme.ui.pojotable.ElementCopier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */

public class FXTronconLitPane extends AbstractFXElementPane<TronconLit> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    @FXML private FXValidityPeriodPane uiValidityPeriod;


    // Propriétés de TronconDigue
    @FXML protected TextField ui_libelle;
    @FXML protected TextArea ui_commentaire;
    @FXML protected ComboBox ui_litId;
    @FXML protected Button ui_litId_link;
    @FXML ComboBox ui_typologieTronconId;
    @FXML protected ComboBox ui_systemeRepDefautId;


    // Onglet "SR"
    @FXML private ListView<SystemeReperage> uiSRList;
    @FXML private Button uiSRDelete;
    @FXML private Button uiSRAdd;
    @FXML private BorderPane uiSrTab;
    private final FXSystemeReperagePane srController = new FXSystemeReperagePane();

    @FXML protected ListView ui_LeftList;
    @FXML protected VBox ui_mainBox;
    @FXML protected BorderPane ui_centerPane;


    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXTronconLitPane() {
        SIRS.loadFXML(this, TronconLit.class);
        previewRepository = Injector.getBean(Session.class).getPreviews();
        elementProperty().addListener(this::initFields);

        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());

        /*
         * Disabling rules.
         */
        ui_litId.disableProperty().bind(disableFieldsProperty());
        ui_litId_link.disableProperty().bind(ui_litId.getSelectionModel().selectedItemProperty().isNull());
        ui_litId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_litId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_litId.getSelectionModel().getSelectedItem()));
        ui_libelle.disableProperty().bind(disableFieldsProperty());
        ui_commentaire.disableProperty().bind(disableFieldsProperty());
        ui_typologieTronconId.disableProperty().bind(disableFieldsProperty());
        ui_systemeRepDefautId.disableProperty().bind(disableFieldsProperty());

        srController.editableProperty().bind(disableFieldsProperty().not());
        uiSRAdd.disableProperty().set(true);
        uiSRAdd.setVisible(false);
        uiSRDelete.disableProperty().set(true);
        uiSRDelete.setVisible(false);

        uiSrTab.setCenter(srController);
        uiSRDelete.setGraphic(new ImageView(SIRS.ICON_TRASH_WHITE));
        uiSRAdd.setGraphic(new ImageView(SIRS.ICON_ADD_WHITE));

        uiSRList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        uiSRList.setCellFactory(new Callback<ListView<SystemeReperage>, ListCell<SystemeReperage>>() {
            @Override
            public ListCell<SystemeReperage> call(ListView<SystemeReperage> param) {
                return new ListCell(){
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(null);
                        if(!empty && item!=null){
                            setText(((SystemeReperage)item).getLibelle());
                        }else{
                            setText("");
                        }
                    }
                };
            }
        });

        uiSRList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SystemeReperage>() {
            @Override
            public void changed(ObservableValue<? extends SystemeReperage> observable, SystemeReperage oldValue, SystemeReperage newValue) {
                srController.getSystemeReperageProperty().set(newValue);
            }
        });

    }

    public FXTronconLitPane(final TronconLit tronconLit){
        this();
        this.elementProperty().set(tronconLit);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends TronconLit > observableElement, TronconLit oldElement, TronconLit newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
        // Propriétés de TronconLit
        // Propriétés de AvecGeometrie
        // Propriétés de TronconDigue
            ui_libelle.textProperty().unbindBidirectional(oldElement.libelleProperty());
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
        }

        final Session session = Injector.getBean(Session.class);

        /*
         * Bind control properties to Element ones.
         */
        // Propriétés de TronconLit
        SIRS.initCombo(ui_litId, FXCollections.observableList(
            previewRepository.getByClass(Lit.class)),
            newElement.getLitId() == null? null : previewRepository.get(newElement.getLitId()));
        // Propriétés de AvecGeometrie
        // Propriétés de TronconDigue
        // * libelle
        ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
        // * commentaire
        ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());
        // * Typologie
        SIRS.initCombo(ui_typologieTronconId, SIRS.observableList(
                        previewRepository.getByClass(RefTypologieTroncon.class)).sorted(),
                newElement.getTypologieTronconId() == null ? null : previewRepository.get(newElement.getTypologieTronconId()));

        final SystemeReperageRepository srRepo = Injector.getBean(SystemeReperageRepository.class);
        final SystemeReperage defaultSR = newElement.getSystemeRepDefautId() == null? null : srRepo.get(newElement.getSystemeRepDefautId());;
        final ObservableList<SystemeReperage> srList = FXCollections.observableArrayList(srRepo.getByLinear(newElement));

        SIRS.initCombo(ui_systemeRepDefautId, srList, defaultSR);

        uiSRList.setItems(srList);

        final List<String> items = new ArrayList<>();
        items.add("Description générale");
        items.add("Ouvrages associés");
        items.add("Type d'occupation riveraine");
        items.add("Pente moyenne");
        items.add("Largeur moyenne");
        items.add("Type d'écoulement");
        items.add("Domanialité");
        items.add("Zone d'atterrissement");
        ui_LeftList.setItems(FXCollections.observableList(items));
        ui_LeftList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue.equals("Description générale")) {
                    ui_centerPane.setCenter(ui_mainBox);
                } else if (newValue.equals("Ouvrages associés")) {
                    ThemeManager manager = AbstractTheme.generateThemeManager("Tableau des ouvrages associés", OuvrageAssocieLit.class);
                    final LitThemePojoTable table = new LitThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
                    table.setDeletor(manager.getDeletor());
                    table.setForeignParentId(newElement.getId());
                    ui_centerPane.setCenter(table);
                } else if (newValue.equals("Type d'occupation riveraine")) {
                    ThemeManager manager = AbstractTheme.generateThemeManager("Tableau des occupations riveraines", OccupationRiveraineLit.class);
                    final LitThemePojoTable table = new LitThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
                    table.setDeletor(manager.getDeletor());
                    table.setForeignParentId(newElement.getId());
                    ui_centerPane.setCenter(table);
                } else if (newValue.equals("Pente moyenne")) {
                    ThemeManager manager = AbstractTheme.generateThemeManager("Tableau des pentes", PenteLit.class);
                    final LitThemePojoTable table = new LitThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
                    table.setDeletor(manager.getDeletor());
                    table.setForeignParentId(newElement.getId());
                    ui_centerPane.setCenter(table);
                } else if (newValue.equals("Largeur moyenne")) {
                    ThemeManager manager = AbstractTheme.generateThemeManager("Tableau des largeurs", LargeurLit.class);
                    final LitThemePojoTable table = new LitThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
                    table.setDeletor(manager.getDeletor());
                    table.setForeignParentId(newElement.getId());
                    ui_centerPane.setCenter(table);
                } else if (newValue.equals("Type d'écoulement")) {
                    ThemeManager manager = AbstractTheme.generateThemeManager("Tableau des régimes d'écoulement", RegimeEcoulementLit.class);
                    final LitThemePojoTable table = new LitThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
                    table.setDeletor(manager.getDeletor());
                    table.setForeignParentId(newElement.getId());
                    ui_centerPane.setCenter(table);
                } else if (newValue.equals("Domanialité")) {
                    ThemeManager manager = AbstractTheme.generateThemeManager("Tableau des domanialité", DomanialiteLit.class);
                    final LitThemePojoTable table = new LitThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
                    table.setDeletor(manager.getDeletor());
                    table.setForeignParentId(newElement.getId());
                    ui_centerPane.setCenter(table);
                } else if (newValue.equals("Zone d'atterrissement")) {
                    ThemeManager manager = AbstractTheme.generateThemeManager("Tableau des zones d'atterrissement", ZoneAtterrissementLit.class);
                    final LitThemePojoTable table = new LitThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
                    table.setDeletor(manager.getDeletor());
                    table.setForeignParentId(newElement.getId());
                    ui_centerPane.setCenter(table);
                }
            }
        });
    }

    @FXML
    private void srAdd(ActionEvent event) {
        final Session session = Injector.getBean(Session.class);
        final SystemeReperageRepository repo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);

        final TronconDigue troncon = elementProperty.get();
        final SystemeReperage sr = Injector.getSession().getElementCreator().createElement(SystemeReperage.class);
        sr.setLibelle("Nouveau SR");
        sr.setLinearId(troncon.getId());
        repo.add(sr, troncon);

        //maj de la liste
        final List<SystemeReperage> srs = repo.getByLinear(troncon);
        uiSRList.setItems(FXCollections.observableArrayList(srs));
    }

    @FXML
    private void srDelete(ActionEvent event) {
        final Session session = Injector.getBean(Session.class);
        final SystemeReperage sr = uiSRList.getSelectionModel().getSelectedItem();
        if(sr==null) return;

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Confirmer la suppression ?",
                ButtonType.NO, ButtonType.YES);
        alert.setResizable(true);

        final ButtonType res = alert.showAndWait().get();
        if(ButtonType.YES != res) return;

        final TronconDigue troncon = elementProperty.get();

        //suppression du SR
        final SystemeReperageRepository repo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);
        repo.remove(sr, troncon);

        //maj de la liste
        final List<SystemeReperage> srs = repo.getByLinear(troncon);
        uiSRList.setItems(FXCollections.observableArrayList(srs));
    }

    @Override
    public void preSave() {
        final TronconLit element = elementProperty().get();

        Object cbValue;
        cbValue = ui_litId.getValue();
        if (cbValue instanceof Preview) {
            element.setLitId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setLitId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setLitId(null);
        }

        cbValue = ui_systemeRepDefautId.getValue();
        if (cbValue instanceof Preview) {
            element.setSystemeRepDefautId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setSystemeRepDefautId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setSystemeRepDefautId(null);
        }

        cbValue = ui_typologieTronconId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypologieTronconId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypologieTronconId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypologieTronconId(null);
        } else {
            SIRS.LOGGER.log(Level.WARNING, "Value not supported for TronconLit ui_typologieTronconId : {0}", ui_typologieTronconId);
        }

    }

    protected class LitThemePojoTable<T extends AvecForeignParent> extends ForeignParentPojoTable<T>{

        private final TronconTheme.ThemeManager<T> group;

        public LitThemePojoTable(TronconTheme.ThemeManager<T> group, final ObjectProperty<? extends Element> container) {
            super(group.getDataClass(), group.getTableTitle(), container);
            foreignParentIdProperty.addListener(this::updateTable);
            this.group = group;
            this.elementCopier = new ElementCopier(this.pojoClass, container, this.session, this.repo, TronconLit.class);
        }

        private void updateTable(ObservableValue<? extends String> observable, String oldValue, String newValue){
            if(newValue==null || group==null) {
                setTableItems(FXCollections::emptyObservableList);
            } else {
                //JavaFX bug : sortable is not possible on filtered list
                // http://stackoverflow.com/questions/17958337/javafx-tableview-with-filteredlist-jdk-8-does-not-sort-by-column
                // https://javafx-jira.kenai.com/browse/RT-32091
//                setTableItems(() -> {
//                    final SortedList<T> sortedList = new SortedList<>(group.getExtractor().apply(newValue));
//                    sortedList.comparatorProperty().bind(getUiTable().comparatorProperty());
//                    return sortedList;
//                });
                setTableItems(() -> (ObservableList) group.getExtractor().apply(newValue));
            }
        }
    }
}
