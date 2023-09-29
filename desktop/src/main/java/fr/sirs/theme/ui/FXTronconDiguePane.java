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
import fr.sirs.util.FXFreeTab;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.TronconUtils.ArchiveMode;
import fr.sirs.core.component.Previews;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.GardeTroncon;
import fr.sirs.core.model.GestionTroncon;
import fr.sirs.core.model.Photo;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.ProprieteTroncon;
import fr.sirs.core.model.RefRive;
import fr.sirs.core.model.RefTypeTroncon;
import fr.sirs.core.model.RefTypologieTroncon;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.digue.FXSystemeReperagePane;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXTronconDiguePane extends AbstractFXElementPane<TronconDigue> {

    @Autowired private Session session;
    private final Previews previewRepository;

    // Onglet "Information"
    @FXML FXValidityPeriodPane uiValidityPeriod;
    @FXML FXAmenagementHydrauliqueAttachementPane uiAmenagementHydrauliqueAttachement;
    @FXML TextField ui_libelle;
    @FXML TextArea ui_commentaire;
    @FXML ComboBox ui_digueId;
    @FXML protected Button ui_digueId_link;
    @FXML ComboBox ui_typeRiveId;
    @FXML ComboBox ui_typeTronconId;
    @FXML ComboBox ui_typologieTronconId;
    @FXML ComboBox ui_systemeRepDefautId;

    // Onglet "SR"
    @FXML private ListView<SystemeReperage> uiSRList;
    @FXML private Button uiSRDelete;
    @FXML private Button uiSRAdd;
    @FXML private BorderPane uiSrTab;
    private final FXSystemeReperagePane srController = new FXSystemeReperagePane();

    // Onglet "Contacts"
    @FXML private Tab uiGestionsTab;
    private final GestionsTable uiGestionsTable;
    @FXML private Tab uiProprietesTab;
    private final ForeignParentPojoTable<ProprieteTroncon> uiProprietesTable;
    @FXML private Tab uiGardesTab;
    private final ForeignParentPojoTable<GardeTroncon> uiGardesTable;
    @FXML protected FXFreeTab ui_photos;
    private PojoTable photosTable;

    // Booleen déterminant s'il est nécessaire de calculer l'état d'archivage du tronçon et des objets qui s'y réfèrent lors de l'enregistrement.
    protected LocalDate initialArchiveDate;
    protected ArchiveMode computeArchive = ArchiveMode.UNCHANGED;
    protected final ObjectProperty<LocalDate> endProperty = new SimpleObjectProperty<>();

    /**
     * Écouteur destiné à déterminer l'opération d'archivage à exécuter lors de l'enregistrement, ainsi que d'informer
     * l'utilisateur des conséquences de la modification de la date de fin du tronçon.
     */
    protected ChangeListener<LocalDate> changeListener = new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) {
                // Si le tronçon n'était pas archivé, mais le devient.
                if(initialArchiveDate==null && newValue!=null) {
                    SIRS.fxRun(false, () -> {
                        final Alert alert = new Alert(Alert.AlertType.INFORMATION, "En affectant une date de fin, vous activez l'archivage du tronçon et de tous les objets liés (bornes, ouvrages, etc.) au moment de la sauvegarde.", ButtonType.OK);
                        alert.setResizable(true);
                        alert.show();
                    });
                    computeArchive = ArchiveMode.ARCHIVE;
                }
                // Si le tronçon était archivé et devient désarchivé.
                else if(initialArchiveDate!=null && newValue==null){
                    SIRS.fxRun(false, () -> {
                        final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Vous supprimez la date de fin ("+initialArchiveDate+"). L'archivage du tronçon et tous les objets qui lui sont lié et ont été archivés le même jour seront annulés lors de la sauvegarde.", ButtonType.OK);
                        alert.setResizable(true);
                        alert.show();
                    });
                    computeArchive = ArchiveMode.UNARCHIVE;
                }
                // À ce stade, soit les deux dates sont nulles, soit aucune des deux.
                else if((initialArchiveDate==null && newValue==null) || initialArchiveDate.isEqual(newValue)){
                    computeArchive = ArchiveMode.UNCHANGED;
                }
                // Cas du changement de date d'archivage : les deux dates diffèrent.
                // Dans ce cas, la demande explicite de Jordan Perrin est de modifier l'archivage (commentaire du 22/05/2017 15:10).
                else if(initialArchiveDate!=null && newValue!=null){
                    SIRS.fxRun(false, () -> {
                        final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Lors de la sauvegarde, la date d'archivage du "+initialArchiveDate.toString()+" sera modifiée pour le tronçon et ses objets archivés le même jour.", ButtonType.OK);
                        alert.setResizable(true);
                        alert.show();
                    });
                    computeArchive = ArchiveMode.UPDATE_ARCHIVE;
                }
            }
        };

    public FXTronconDiguePane(final TronconDigue troncon) {
        SIRS.loadFXML(this, TronconDigue.class);
        Injector.injectDependencies(this);

        //mode edition
        previewRepository = Injector.getBean(Session.class).getPreviews();

        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());
        uiAmenagementHydrauliqueAttachement.targetProperty().bind(elementProperty());

        /*
         * Disabling rules.
         */
        ui_libelle.disableProperty().bind(disableFieldsProperty());
        ui_commentaire.disableProperty().bind(disableFieldsProperty());
        ui_digueId.disableProperty().bind(disableFieldsProperty());
        ui_digueId_link.disableProperty().bind(ui_digueId.getSelectionModel().selectedItemProperty().isNull());
        ui_digueId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_digueId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_digueId.getSelectionModel().getSelectedItem()));

        ui_typeRiveId.disableProperty().bind(disableFieldsProperty());
        ui_typeTronconId.disableProperty().bind(disableFieldsProperty());
        ui_typologieTronconId.disableProperty().bind(disableFieldsProperty());
        ui_systemeRepDefautId.disableProperty().bind(disableFieldsProperty());

        srController.editableProperty().bind(disableFieldsProperty().not());
        uiSRAdd.disableProperty().set(true);
        uiSRAdd.setVisible(false);
        uiSRDelete.disableProperty().set(true);
        uiSRDelete.setVisible(false);

        uiGestionsTable = new GestionsTable(elementProperty());
        uiGestionsTable.editableProperty().bind(disableFieldsProperty().not());
        uiProprietesTable = new ForeignParentPojoTable<>(ProprieteTroncon.class, "Période de propriété", elementProperty());
        uiProprietesTable.editableProperty().bind(disableFieldsProperty().not());
        uiGardesTable = new ForeignParentPojoTable<>(GardeTroncon.class, "Période de gardiennage", elementProperty());
        uiGardesTable.editableProperty().bind(disableFieldsProperty().not());

        ui_photos.setContent(() -> {
            photosTable = new PojoTable(Photo.class, null, elementProperty());
            photosTable.editableProperty().bind(disableFieldsProperty().not());
            updatePhotosTable(elementProperty.get());
            return photosTable;
        });
        ui_photos.setClosable(false);

        // Troncon change listener
        elementProperty.addListener(this::initFields);
        setElement(troncon);

        // Layout
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

        uiGestionsTab.setContent(uiGestionsTable);
        uiProprietesTab.setContent(uiProprietesTable);
        uiGardesTab.setContent(uiGardesTable);

    }

    @Override
    public void preRemove() {
        endProperty.removeListener(changeListener);
    }

    @Override
    final public void setElement(TronconDigue element) {
        super.setElement(element);
        initialArchiveDate = element.getDate_fin();
        computeArchive = ArchiveMode.UNCHANGED;

        endProperty.removeListener(changeListener); // On enlève l'écouteur éventuellement présent avant de manipuler la propriété surveillant la date de fin.
        // On surveille la date de fin du tronçon pour déterminer si l'archivage du tronçon doit être mis à jour.
        endProperty.unbind();// On arrête la surveillance de la date de fin de l'éventuel élément affiché dans le panneau.
        endProperty.bind(element.date_finProperty());
        // Un écouteur met à jour le scénario d'archivage au fur et à mesure des modifications de la date de fin du tronçon.
        endProperty.addListener(changeListener);
    }

    @FXML
    private void srAdd(ActionEvent event) {
        final SystemeReperageRepository repo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);

        final TronconDigue troncon = elementProperty.get();
        final SystemeReperage sr = Injector.getSession().getElementCreator().createElement(SystemeReperage.class);
        sr.setLibelle("Nouveau SR");
        sr.setLinearId(troncon.getId());
        repo.add(sr, troncon);

        //maj de la liste
        final List<SystemeReperage> srs = repo.getByLinear(troncon);
        uiSRList.setItems(SIRS.observableList(srs));
    }

    @FXML
    private void srDelete(ActionEvent event) {
        final SystemeReperage sr = uiSRList.getSelectionModel().getSelectedItem();
        if(sr==null) return;

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Confirmer la suppression ?", ButtonType.NO, ButtonType.YES);
        alert.setResizable(true);

        final ButtonType res = alert.showAndWait().get();
        if(ButtonType.YES != res) return;

        final TronconDigue troncon = elementProperty.get();

        //suppression du SR
        final SystemeReperageRepository repo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);
        repo.remove(sr, troncon);

        //maj de la liste
        final List<SystemeReperage> srs = repo.getByLinear(troncon);
        uiSRList.setItems(SIRS.observableList(srs));
    }

    private void initFields(ObservableValue<? extends TronconDigue> observable, TronconDigue oldValue, TronconDigue newElement) {

        // Unbind fields bound to previous element.
        if (oldValue != null) {
        // Propriétés de TronconDigue
            ui_libelle.textProperty().unbindBidirectional(oldValue.libelleProperty());
            ui_commentaire.textProperty().unbindBidirectional(oldValue.commentaireProperty());
        }

        if (newElement != null) {
            ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
            // * commentaire
            ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());

            SIRS.initCombo(ui_digueId, SIRS.observableList(
                    previewRepository.getByClass(Digue.class)).sorted(),
                    newElement.getDigueId() == null ? null : previewRepository.get(newElement.getDigueId()));
            SIRS.initCombo(ui_typeRiveId, SIRS.observableList(
                    previewRepository.getByClass(RefRive.class)).sorted(),
                    newElement.getTypeRiveId() == null ? null : previewRepository.get(newElement.getTypeRiveId()));
            SIRS.initCombo(ui_typeTronconId, SIRS.observableList(
                    previewRepository.getByClass(RefTypeTroncon.class)).sorted(),
                    newElement.getTypeTronconId() == null ? null : previewRepository.get(newElement.getTypeTronconId()));
            SIRS.initCombo(ui_typologieTronconId, SIRS.observableList(
                            previewRepository.getByClass(RefTypologieTroncon.class)).sorted(),
                    newElement.getTypologieTronconId() == null ? null : previewRepository.get(newElement.getTypologieTronconId()));

            final SystemeReperageRepository srRepo = Injector.getBean(SystemeReperageRepository.class);
            final SystemeReperage defaultSR = newElement.getSystemeRepDefautId() == null? null : srRepo.get(newElement.getSystemeRepDefautId());;
            final ObservableList<SystemeReperage> srList = SIRS.observableList(srRepo.getByLinear(newElement));

            SIRS.initCombo(ui_systemeRepDefautId, srList, defaultSR);

            //liste des systemes de reperage
            uiSRList.setItems(srList);
            uiGestionsTable.setParentElement(newElement);
            uiGestionsTable.setTableItems(() -> (ObservableList) newElement.gestions);
            uiProprietesTable.setForeignParentId(newElement.getId());
            uiProprietesTable.setTableItems(() -> (ObservableList) SIRS.observableList(session.getProprietesByTronconId(newElement.getId())));
            uiGardesTable.setForeignParentId(newElement.getId());
            uiGardesTable.setTableItems(() -> (ObservableList) SIRS.observableList(session.getGardesByTronconId(newElement.getId())));
        }

        updatePhotosTable(newElement);
    }

    private void updatePhotosTable(final TronconDigue newElement) {
        if (photosTable == null)
            return;

        if (newElement == null) {
            photosTable.setTableItems(null);
        } else {
            photosTable.setParentElement(newElement);
            photosTable.setTableItems(()-> (ObservableList) newElement.getPhotos());
        }
    }

    @Override
    public void preSave() {
        final TronconDigue element = elementProperty().get();

        Object cbValue;
        cbValue = ui_digueId.getValue();
        if (cbValue instanceof Preview) {
            element.setDigueId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setDigueId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setDigueId(null);
        }
        cbValue = ui_typeRiveId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypeRiveId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypeRiveId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypeRiveId(null);
        }
        cbValue = ui_typeTronconId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypeTronconId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypeTronconId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypeTronconId(null);
        }
        cbValue = ui_typologieTronconId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypologieTronconId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypologieTronconId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypologieTronconId(null);
        } else {
            SIRS.LOGGER.log(Level.WARNING, "Value not supported for TronconDigue ui_typologieTronconId : {0}", ui_typologieTronconId);
        }
        cbValue = ui_systemeRepDefautId.getValue();
        if (cbValue instanceof Preview) {
            element.setSystemeRepDefautId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setSystemeRepDefautId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setSystemeRepDefautId(null);
        }
        srController.save();

        //==============================================================================================================
        // Gestion de l'archivage.
        //==============================================================================================================

        // Si on a détecté que la date d'archivage du tronçon a changé d'une manière ou d'une autre.
        switch(computeArchive){
            case UNARCHIVE:
                final Predicate<AvecBornesTemporelles> unArchiveIf = new AvecBornesTemporelles.UnArchivePredicate(initialArchiveDate);
                TronconUtils.archiveSectionWithTemporalObjects(element, session, null, unArchiveIf);
                TronconUtils.archiveBornes(element.getBorneIds(), session, null, unArchiveIf);
                break;
            case ARCHIVE:
                final Predicate<AvecBornesTemporelles> archiveIf = new AvecBornesTemporelles.ArchivePredicate(null);
                TronconUtils.archiveSectionWithTemporalObjects(element, session, element.getDate_fin(), archiveIf);
                TronconUtils.archiveBornes(element.getBorneIds(), session, element.getDate_fin(), archiveIf);
                break;
            case UPDATE_ARCHIVE:
                final Predicate<AvecBornesTemporelles> updateIf = new AvecBornesTemporelles.UpdateArchivePredicate(initialArchiveDate);
                TronconUtils.archiveSectionWithTemporalObjects(element, session, element.getDate_fin(), updateIf);
                TronconUtils.archiveBornes(element.getBorneIds(), session, element.getDate_fin(), updateIf);
        }
        // Réinitialisation du processus d'archivage.
        computeArchive = ArchiveMode.UNCHANGED;
        initialArchiveDate = element.getDate_fin();
    }

    private final class GestionsTable extends PojoTable {

        public GestionsTable(ObjectProperty<? extends Element> container) {
            super(GestionTroncon.class, "Périodes de gestion", container);
        }

        @Override
        protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event) {
            //on ne sauvegarde pas, le formulaire conteneur s'en charge
        }
    }
}
