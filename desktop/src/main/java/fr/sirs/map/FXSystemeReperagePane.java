/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.map;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.BorneDigueRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.util.FXNumberCell;
import org.geotoolkit.gui.javafx.util.FXTableCell;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.referencing.LinearReferencing;
import org.geotoolkit.referencing.LinearReferencing.ProjectedPoint;

import java.util.*;
import java.util.function.Function;

import static fr.sirs.core.LinearReferencingUtilities.asLineString;
import static fr.sirs.core.SirsCore.SR_ELEMENTAIRE;
import static org.geotoolkit.referencing.LinearReferencing.buildSegments;
import static org.geotoolkit.referencing.LinearReferencing.projectReference;

/**
 * @author Johann Sorel (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 * @author Estelle Idee (Geomatys)
 */
public class FXSystemeReperagePane extends FXAbstractEditOnTronconPane {

    @FXML
    Button uiAddObjet;
    @FXML
    private ChoiceBox<SystemeReperage> uiSrComboBox;
    @FXML
    private CheckBox uiDefaultSRCheckBox;
    @FXML
    private Button uiAddSr;
    @FXML
    private Button uiDeleteSR;
    @FXML
    Button uiProject;
    @FXML
    Button uiRename;

//    private final ObjectProperty<ObjetEditMode> mode = new SimpleObjectProperty<>(EditModeObjet.NONE);

    /**
     * A flag to indicate that selected {@link SystemeReperage} must be saved.
     */
    private final SimpleBooleanProperty saveSR = new SimpleBooleanProperty(false);

    /**
     * A comparator which sort bornes by apparition order (uphill to downhill) in
     * currently selected linear.
     */
    private final ObjectBinding<Comparator<SystemeReperageBorne>> defaultSRBComparator;

    public FXSystemeReperagePane(FXMap map, final String typeName) {
        super(map, typeName, BorneDigue.class, false);

        uiObjetTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        uiAddSr.setGraphic(new ImageView(SIRS.ICON_ADD_BLACK));
        uiDeleteSR.setGraphic(new ImageView(GeotkFX.ICON_DELETE));
        uiProject.setDisable(true);
        uiRename.setDisable(true);

        //on active le choix du sr si un troncon est sélectionné
        final BooleanBinding srEditBinding = tronconProp.isNull();
        uiSrComboBox.disableProperty().bind(srEditBinding);
        uiSrComboBox.setConverter(new SirsStringConverter());
        uiAddSr.disableProperty().bind(srEditBinding);

        //on active la table et bouton de creation si un sr est sélectionné
        final BooleanBinding borneEditBinding = uiSrComboBox.valueProperty().isNull();
        uiObjetTable.disableProperty().bind(borneEditBinding);
        uiAddObjet.disableProperty().bind(borneEditBinding);
        uiCreateObjet.disableProperty().bind(borneEditBinding);

        //on active le calcule de PR uniquement si 2 bornes sont sélectionnées

        uiObjetTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<SystemeReperageBorne>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends SystemeReperageBorne> c) {
                final int size = uiObjetTable.getSelectionModel().getSelectedItems().size();
                uiProject.setDisable(size < 1);
                uiRename.setDisable(size != 1);
            }
        });

        uiAddObjet.setOnAction(this::startAddObjet);
        uiAddSr.setOnAction(this::createSystemeReperage);
        uiDeleteSR.setOnAction(this::deleteSystemeReperage);
        uiProject.setOnAction(this::reprojectBorneToSection);

        //colonne de la table
        addColumToTable(new SRBDeleteColumn(), false);
        addColumToTable(new SRBNameColumn(), true);
        addColumToTable(new PRColumn(), true);

        // Initialize event listeners
        uiSrComboBox.valueProperty().addListener(this::updateBorneTable);
        uiSrComboBox.valueProperty().addListener(this::updateDefaultSRCheckBox);
        uiDefaultSRCheckBox.selectedProperty().addListener(this::updateTonconDefaultSR);

        // Update default comparator on linear change.
        defaultSRBComparator = Bindings.createObjectBinding(() -> {
            final TronconDigue td = getTronconFromProperty();
            if (td == null || td.getGeometry() == null)
                return null;
            return new SRBComparator(LinearReferencingUtilities.asLineString(td.getGeometry()));
        }, tronconProp);

        //etat des boutons sélectionné
        final ToggleGroup group = new ToggleGroup();
        uiPickTroncon.setToggleGroup(group);
        uiCreateObjet.setToggleGroup(group);

        mode.addListener((observable, oldValue, newValue) -> {
            switch ((EditModeObjet) newValue) {
                case CREATE_OBJET:
                    group.selectToggle(uiCreateObjet);
                    break;
                case PICK_TRONCON:
                    group.selectToggle(uiPickTroncon);
                    break;
                default:
                    group.selectToggle(null);
                    break;
            }
        });
    }

    @Override
    public void reset() {
        super.reset();
        systemeReperageProperty().set(null);
    }

    public void selectSRB(SystemeReperageBorne srb) {
        final int index = uiObjetTable.getItems().indexOf(srb);
        if (index >= 0) {
            uiObjetTable.getSelectionModel().clearAndSelect(index);
        } else {
            uiObjetTable.getSelectionModel().clearSelection();
        }
    }


    public ObjectProperty<SystemeReperage> systemeReperageProperty() {
        return uiSrComboBox.valueProperty();
    }


    @Override
    public void save() {
        save(uiSrComboBox.getValue(), getTronconFromProperty());
    }

    private void save(final SystemeReperage sr, final TronconDigue td) {
        final boolean mustSaveTd = saveTD.get();
        final boolean mustSaveSr = saveSR.get();

        if (mustSaveTd || mustSaveSr) {
            saveTD.set(false);
            saveSR.set(false);

            TaskManager.INSTANCE.submit("Sauvegarde...", () -> {
                if (td != null && mustSaveTd) {
                    ((AbstractSIRSRepository) session.getRepositoryForClass(td.getClass())).update(td);
                }

                if (sr != null && mustSaveSr) {
                    if (td != null) {
                        ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).update(sr, td);
                    } else {
                        ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).update(sr);
                    }
                }
            });
        }
    }


    /*
     * BORNE UTILITIES
     */


    /**
     * Constuit un composant graphique listant les bornes du tronçon.
     *
     * @param toExclude Liste des identifiants de bornes à exclure de la liste.
     * @return A list view of all bornes bound to currently selected troncon, or
     * null if no troncon is selected.
     */
    @Override
    ListView buildObjetList(final Set toExclude) {
        final TronconDigue troncon = getTronconFromProperty();
        if (troncon == null) return null;

        // Construction de la liste définitive des identifiants des bornes à afficher.
        final ObservableList<String> borneIds;
        if (toExclude != null && !toExclude.isEmpty()) {
            borneIds = FXCollections.observableArrayList(troncon.getBorneIds());
            borneIds.removeIf((borneId) -> toExclude.contains(borneId));
        } else {
            borneIds = troncon.getBorneIds();
        }

        // Récupération et tri des bornes.
        final List<BorneDigue> bornes = session.getRepositoryForClass(BorneDigue.class).get(borneIds);
        bornes.sort((BorneDigue b1, BorneDigue b2) -> {
            if (b1.getLibelle() == null) {
                return 1;
            }
            return b1.getLibelle().compareToIgnoreCase(b2.getLibelle());
        });

        // Construction du composant graphique.
        final ListView<BorneDigue> bornesView = new ListView<>();
        bornesView.setItems(FXCollections.observableArrayList(bornes));
        bornesView.setCellFactory(TextFieldListCell.forListView(new SirsStringConverter()));
        bornesView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        return bornesView;
    }

    /**
     * Ajout au SR d'une borne existante sur le tronçon.
     *
     * @param evt
     */
    void startAddObjet(ActionEvent evt) {
        final TronconDigue troncon = getTronconFromProperty();
        final SystemeReperage csr = systemeReperageProperty().get();
        if (csr == null || troncon == null) return;

        // Do not show bornes already present in selected SR.
        final Set<String> borneIdsAlreadyInSR = new HashSet<>();
        for (final SystemeReperageBorne srb : csr.systemeReperageBornes) {
            borneIdsAlreadyInSR.add(srb.getBorneId());
        }

        // Construction et affichage du composant graphique de choix des bornes à ajouter.
        final ListView<BorneDigue> bornesView = buildObjetList(borneIdsAlreadyInSR);
        final Dialog dialog = new Dialog();
        final DialogPane pane = new DialogPane();
        pane.setContent(bornesView);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setDialogPane(pane);

        // Récupération des bornes sélectionnées et ajout dans le SR.
        final Object res = dialog.showAndWait().get();
        if (ButtonType.OK.equals(res)) {
            final ObservableList<BorneDigue> selectedItems = bornesView.getSelectionModel().getSelectedItems();
            for (BorneDigue bd : selectedItems) {
                addBorneToSR(bd);
            }
        }
    }

    /**
     * Projection des bornes sélectionnées, sur le tronçon.
     *
     * @param evt
     */
    private void reprojectBorneToSection(ActionEvent evt) {

        // Récupération du tronçon et du SR.
        final TronconDigue troncon = getTronconFromProperty();
        final SystemeReperage sr = systemeReperageProperty().get();
        if (troncon == null || sr == null) return;

        final LineString linear = LinearReferencingUtilities.asLineString(troncon.getGeometry());
        final LinearReferencingUtilities.SegmentInfo[] segments = LinearReferencingUtilities.buildSegments(linear);

        final AbstractSIRSRepository<BorneDigue> repo = session.getRepositoryForClass(BorneDigue.class);

        // Parcours des bornes sélectionnées, projection de la géométrie sur la géométrie du tronçon et mise à jour de la borne en base.
        final ObservableList<SystemeReperageBorne> lst = uiObjetTable.getSelectionModel().getSelectedItems();
        for (final SystemeReperageBorne srb : lst) {

            final BorneDigue borne = repo.get(srb.getBorneId());
            final Point point = borne.getGeometry();

            final LinearReferencingUtilities.ProjectedPoint proj = LinearReferencingUtilities.projectReference(segments, point);
            point.getCoordinate().setCoordinate(proj.projected);

            repo.update(borne);
        }

        uiObjetTable.getSelectionModel().clearSelection();
        map.getCanvas().repaint();
    }

    private void createSystemeReperage(ActionEvent evt) {
        final TronconDigue troncon = getTronconFromProperty();
        if (troncon == null) return;

        final TextInputDialog dialog = new TextInputDialog("Nom du SR");
        dialog.getEditor().setPromptText("nom du système de repèrage");
        dialog.setTitle("Nouveau système de repèrage");
        dialog.setHeaderText("Nom du nouveau système de repèrage");

        final Optional<String> opt = dialog.showAndWait();
        if (!opt.isPresent() || opt.get().isEmpty()) return;


        final String srName = opt.get();
        final SystemeReperage sr = session.getRepositoryForClass(SystemeReperage.class).create();
        sr.setLibelle(srName);
        sr.setLinearId(troncon.getDocumentId());
        ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).add(sr, troncon);

        //maj de la liste des SR
        tronconChanged(tronconProperty(), troncon, troncon);

        //selection du SR
        uiSrComboBox.getSelectionModel().clearAndSelect(uiSrComboBox.getItems().indexOf(sr));
    }

    /**
     * Création d'une borne à partir d'un point.
     *
     * @param geom
     */
//    @Override
    public final void createObjet(final Point geom) {

        // Formulaire de renseignement du libellé de la borne.
        final TextInputDialog dialog = new TextInputDialog("");
        dialog.getEditor().setPromptText("borne ...");
        dialog.setTitle("Nouvelle borne");
        dialog.setGraphic(null);
        dialog.setHeaderText("Libellé de la nouvelle borne");

        final Optional<String> opt = dialog.showAndWait();
        if (!opt.isPresent() || opt.get().isEmpty()) return;

        //creation de la borne
        final String borneLbl = opt.get();

        // On vérifie que le libellé renseigné pour la borne ne fait pas partie des libellés utilisés par le SR élémentaire.
        if (BorneUtils.isStartOrEndBorneLibelle.test(borneLbl)) {
            BorneUtils.openAlert("Le libellé de borne \"" + borneLbl + "\" est réservé au SR élémentaire.");
            return;
        }

        final BorneDigue borne = session.getRepositoryForClass(BorneDigue.class).create();
        borne.setLibelle(borneLbl);
        borne.setGeometry(geom);
        session.getRepositoryForClass(BorneDigue.class).add(borne);
        final TronconDigue tr = getTronconFromProperty();
        if (tr != null) {
            tr.getBorneIds().add(borne.getId());
        }

        // Ajout de la borne au SR.
        addBorneToSR(borne);
    }

    /**
     * Ajout d'une borne au système de repérage.
     *
     * @param borne Borne à ajouter au système de repérage.
     */
    private void addBorneToSR(final BorneDigue borne) {
       //sauvegarde du SR
        saveSR.set(BorneUtils.addBorneToSR(borne, getTronconFromProperty(), systemeReperageProperty().get(), session));
    }


    /**
     * Open a {@link ListView} to allow user to select one or more {@link BorneDigue}
     * to delete.
     * <p>
     * Note : Once suppression is confirmed, we're forced to check all {@link SystemeReperage}
     * defined on the currently edited {@link TronconDigue}, and update them if
     * they use chosen bornes.
     *
     * @param e Event fired when deletion button has been fired.
     */
    @FXML
    @Override
    void deleteObjets(ActionEvent e) {
        final ListView<BorneDigue> borneList = buildObjetList(null);
        if (borneList == null) return;

        final Stage stage = new Stage();
        stage.setTitle("Sélectionnez les bornes à supprimer");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(this.getScene().getWindow());

        final Separator blankSpace = new Separator();
        blankSpace.setVisible(false);

        final Button cancelButton = new Button("Annuler");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> stage.hide());
        final Button deleteButton = new Button("Supprimer");
        deleteButton.disableProperty().bind(borneList.getSelectionModel().selectedItemProperty().isNull());

        final HBox buttonBar = new HBox(10, blankSpace, cancelButton, deleteButton);
        buttonBar.setPadding(new Insets(5));
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        final VBox content = new VBox(borneList, buttonBar);

        stage.setScene(new Scene(content));

        deleteButton.setOnAction(event -> {
            final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Attention, les bornes sélectionnées seront effacées définitivement. Si elles sont utilisées par un système de repérage, cela entrainera le recalcul des positions liées à ce dernier. Continuer ?", ButtonType.NO, ButtonType.YES);
            confirmation.setResizable(true);
            final ButtonType userDecision = confirmation.showAndWait().orElse(ButtonType.NO);
            if (ButtonType.YES.equals(userDecision)) {
                final BorneDigue[] selectedItems = borneList.getSelectionModel().getSelectedItems().toArray(new BorneDigue[0]);
                if (checkObjetSuppression(selectedItems)) {
                    final TaskManager.MockTask deletor = new TaskManager.MockTask("Suppression de bornes", () -> {
                        InjectorCore.getBean(BorneDigueRepository.class).remove(selectedItems);
                    });

                    deletor.setOnSucceeded(evt -> Platform.runLater(() -> borneList.getItems().removeAll(selectedItems)));
                    deletor.setOnFailed(evt -> Platform.runLater(() -> GeotkFX.newExceptionDialog("Une erreur est survenue lors de la suppression des bornes.", deletor.getException()).show()));
                    content.disableProperty().bind(deletor.runningProperty());

                    TaskManager.INSTANCE.submit(deletor);
                }
            }
        });

        stage.show();
    }

    /**
     * Detect if any available SR would be emptied if input {@link BorneDigue}s
     * were deleted from database. If it's the case, we ask user to confirm his
     * will to remove them.
     *
     * @param bornes Bornes to delete.
     * @return True if we can proceed to borne deletion, false if not.
     */
//    @Override
    public boolean checkObjetSuppression(final BorneDigue... bornes) {
        final HashSet<String> borneIds = new HashSet<>();
        for (final BorneDigue bd : bornes) {
            borneIds.add(bd.getId());
        }

        // Find all Sr which would be emptied by suppression of input bornes.
        FilteredList<SystemeReperage> emptiedSrs = uiSrComboBox.getItems().filtered(sr
                -> sr.systemeReperageBornes.filtered(srb -> !borneIds.contains(srb.getBorneId())).isEmpty()
        );

        if (emptiedSrs.isEmpty()) {
            return true;
        }

        final StringBuilder msg = new StringBuilder("La suppression des bornes sélectionnées va entièrement vider les systèmes de repérage suivants :");
        for (final SystemeReperage sr : emptiedSrs) {
            msg.append(System.lineSeparator()).append(uiSrComboBox.getConverter().toString(sr));
        }
        msg.append(System.lineSeparator()).append("Voulez-vous tout de même supprimer ces bornes ?");

        final Alert alert = new Alert(Alert.AlertType.WARNING, msg.toString(), ButtonType.NO, ButtonType.YES);
        alert.setResizable(true);
        return ButtonType.YES.equals(alert.showAndWait().orElse(ButtonType.NO));
    }

    /*
     * SR UTILITIES
     */

    /**
     * Delete the {@link SystemeReperage} selected in @uiObjetTable
     *
     * @param evt
     */
    private void deleteSystemeReperage(ActionEvent evt) {
        final TronconDigue troncon = getTronconFromProperty();
        if (troncon == null) return;

        SystemeReperage toDelete = uiSrComboBox.getValue();
        if (toDelete == null || toDelete.getId() == null) {
            return;
        }

        // We cannot delete default SR, because all PRs on the troncon are based on it.
        if (toDelete.getId().equals(troncon.getSystemeRepDefautId())) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Impossible de supprimer le système de repérage par défaut.\n "
                    + "Avant de pouvoir supprimer le système courant, vous devez en sélectionner un autre comme système par défaut du tronçon.", ButtonType.OK);
            // Forced to do that because of linux bug.
            alert.setResizable(true);
            alert.setWidth(400);
            alert.setHeight(300);
            alert.showAndWait();

        } else {
            /*
             Before deleting SR, we propose user to choose another SR to update
             position of objects defined on SR to delete.
             */
            final SystemeReperage alternative;
            if (uiSrComboBox.getItems().size() > 1) {

                Alert alert = new Alert(Alert.AlertType.WARNING, "Les positions linéaires des structures définies sur ce système de repérage seront invalidées.\n"
                        + "Voulez-vous définir un SR pour mettre à jour la position linéaire des objets affectés ?"
                        + toDelete.getLibelle() + " ?", ButtonType.NO, ButtonType.YES);
                // Forced to do that because of linux bug.
                alert.setResizable(true);
                alert.setWidth(400);
                alert.setHeight(300);
                ButtonType response = alert.showAndWait().orElse(null);
                // User choose to use an alternative SR.
                if (ButtonType.YES.equals(response)) {
                    ObservableList<SystemeReperage> otherSRs = FXCollections.observableArrayList(uiSrComboBox.getItems());
                    otherSRs.remove(toDelete);
                    final ComboBox<SystemeReperage> chooser = new ComboBox();
                    SIRS.initCombo(chooser, otherSRs, otherSRs.get(0));
                    alert = new Alert(Alert.AlertType.NONE, null, ButtonType.CANCEL, ButtonType.YES);
                    alert.getDialogPane().setContent(chooser);
                    // Forced to do that because of linux bug.
                    alert.setResizable(true);
                    alert.setWidth(400);
                    alert.setHeight(300);
                    response = alert.showAndWait().orElse(ButtonType.CANCEL);
                    if (ButtonType.YES.equals(response)) {
                        alternative = chooser.getValue();
                    } else {
                        // User cancelled dialog.
                        return;
                    }
                } else if (ButtonType.NO.equals(response)) {
                    alternative = null;
                } else {
                    // User cancelled dialog.
                    return;
                }
            } else {
                alternative = null;
            }

            // Current selected SR will be removed, we don't need to update it.
            saveSR.set(false);
            InjectorCore.getBean(SystemeReperageRepository.class).remove(toDelete, troncon, alternative);
            uiSrComboBox.getItems().remove(toDelete);
            if (alternative != null) {
                uiSrComboBox.getSelectionModel().select(alternative);
            }
        }
    }

    /**
     * Method to rename a borne from the @{@link FXSystemeReperagePane}
     * Invoked by action on Renommer button.
     */
    @FXML
    void renameBorne() {

        ObservableList<SystemeReperageBorne> list = uiObjetTable.getSelectionModel().getSelectedItems();
        SystemeReperageBorne selectedSeBorne = list.get(0);
        int index = uiObjetTable.getItems().indexOf(selectedSeBorne);

        BorneDigueRepository repo = (BorneDigueRepository) session.getRepositoryForClass(BorneDigue.class);
        BorneDigue selectedItem = repo.get(selectedSeBorne.getBorneId());

        if (selectedItem == null)
            throw new NullPointerException("There is no BorneDigue with id : " + selectedSeBorne.getBorneId());

        if (!BorneUtils.openDialogAndRenameBorne(selectedItem)) return;

        // Force to update the Borne libelle in the uiObjetTable.
        SystemeReperage sr = uiSrComboBox.getValue();
        sr.getSystemeReperageBornes().removeIf(b -> b.getBorneId().equals(selectedItem.getId()));
        sr.systemeReperageBornes.add(selectedSeBorne.copy());
        uiObjetTable.getSelectionModel().clearSelection();
        uiObjetTable.getSelectionModel().select(index);
    }

    @Override
    void tronconChanged(ObservableValue observable, TronconDigue oldValue, TronconDigue newValue) {
        if (oldValue != null) {
            save(uiSrComboBox.getValue(), oldValue);
        }

        if (newValue == null) {
            uiSrComboBox.setItems(FXCollections.emptyObservableList());
        } else {
            mode.set(EditModeObjet.NONE);
            final List<SystemeReperage> srs = ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).getByLinear(newValue);
            uiSrComboBox.setItems(FXCollections.observableArrayList(srs));

            final String defaultSRID = newValue.getSystemeRepDefautId();
            if (defaultSRID != null) {
                for (final SystemeReperage sr : srs) {
                    if (defaultSRID.equals(sr.getId())) {
                        uiSrComboBox.getSelectionModel().select(sr);
                        break;
                    }
                }
            }

            // In case default SR change from another panel
            newValue.systemeRepDefautIdProperty().addListener((ObservableValue<? extends String> srObservable, String oldSR, String newSR) -> {
                uiDefaultSRCheckBox.setSelected(newSR == null ? false : newSR.equals(uiSrComboBox.getValue() == null ? null : uiSrComboBox.getValue().getId()));
            });

            // On met à jour le SR élémentaire s'il esxiste.
            TronconUtils.updateSRElementaireIfExists(newValue, session);
        }
    }

    private void updateDefaultSRCheckBox(ObservableValue<? extends SystemeReperage> observable, SystemeReperage oldValue, SystemeReperage newValue) {
        if (newValue != null && tronconProp.get() != null &&
                newValue.getId().equals(getTronconFromProperty().getSystemeRepDefautId())) {
            uiDefaultSRCheckBox.setSelected(true);
        } else {
            uiDefaultSRCheckBox.setSelected(false);
        }
    }

    private void updateTonconDefaultSR(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        TronconDigue td = getTronconFromProperty();
        final SystemeReperage selectedSR = uiSrComboBox.getSelectionModel().getSelectedItem();
        final String srid = selectedSR == null ? null : selectedSR.getId();
        if (td != null && srid != null) {
            if (Boolean.TRUE.equals(newValue) && !srid.equals(td.getSystemeRepDefautId())) {
                td.setSystemeRepDefautId(srid);
                saveTD.set(true);
            } else if (Boolean.FALSE.equals(newValue) && srid.equals(td.getSystemeRepDefautId())) {
                td.setSystemeRepDefautId(null);
                saveTD.set(true);
            }
        }
    }

    /*
     * TABLE UTILITIES
     */

    private void updateBorneTable(ObservableValue<? extends SystemeReperage> observable, SystemeReperage oldValue, SystemeReperage newValue) {
        if (oldValue != null) {
            save(oldValue, null);
        }

        if (newValue == null) {
            uiObjetTable.setItems(FXCollections.emptyObservableList());
        } else {
            final EditModeObjet current = getMode();
            if (current.equals(EditModeObjet.CREATE_OBJET) || current.equals(EditModeObjet.EDIT_OBJET)) {
                //do nothing
            } else {
                mode.set(EditModeObjet.EDIT_OBJET);
            }

            // By default, we'll sort bornes from uphill to downhill, but alow user to sort them according to available table columns.
            final Comparator defaultComparator = defaultSRBComparator.get();
            final SortedList sortedItems;
            if (defaultComparator != null) {
                sortedItems = newValue.getSystemeReperageBornes().sorted(defaultComparator).sorted();
            } else {
                sortedItems = newValue.getSystemeReperageBornes().sorted();
            }

            sortedItems.comparatorProperty().bind(uiObjetTable.comparatorProperty());
            uiObjetTable.setItems(sortedItems);
        }
    }


    /**
     * Colonne de suppression d'une borne d'un système de repérage.
     */
    private class SRBDeleteColumn extends SimpleButtonColumn<SystemeReperageBorne, SystemeReperageBorne> {

        SRBDeleteColumn() {
            super(GeotkFX.ICON_UNLINK,
                    (TableColumn.CellDataFeatures<SystemeReperageBorne, SystemeReperageBorne> param) -> new SimpleObjectProperty<>(param.getValue()),
                    (SystemeReperageBorne t) -> true,
                    new Function<SystemeReperageBorne, SystemeReperageBorne>() {

                        public SystemeReperageBorne apply(SystemeReperageBorne srb) {
                            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression (seule l'association entre la borne et le système de repérage est brisée) ?",
                                    ButtonType.NO, ButtonType.YES);
                            alert.setResizable(true);
                            final ButtonType res = alert.showAndWait().get();
                            if (ButtonType.YES == res) {
                                saveSR.set(systemeReperageProperty().get().getSystemeReperageBornes().remove(srb));
                            }
                            return null;
                        }
                    },
                    "Enlever du système de repérage"
            );
        }
    }


    /**
     * Colonne d'affichage et de mise à jour du nom d'une borne.
     */
    private static class SRBNameColumn extends TableColumn<SystemeReperageBorne, SystemeReperageBorne> {

        public SRBNameColumn() {
            super("Nom");
            setSortable(false);

            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SystemeReperageBorne, SystemeReperageBorne>, ObservableValue<SystemeReperageBorne>>() {
                @Override
                public ObservableValue<SystemeReperageBorne> call(TableColumn.CellDataFeatures<SystemeReperageBorne, SystemeReperageBorne> param) {
                    return new SimpleObjectProperty<>(param.getValue());
                }
            });

            final SirsStringConverter sirsStringConverter = new SirsStringConverter();
            setCellFactory((TableColumn<SystemeReperageBorne, SystemeReperageBorne> param) -> {
                final FXTableCell<SystemeReperageBorne, SystemeReperageBorne> tableCell = new FXTableCell<SystemeReperageBorne, SystemeReperageBorne>() {

                    @Override
                    protected void updateItem(SystemeReperageBorne item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setGraphic(new ImageView(ReferenceTableCell.ICON_LINK));
                            setText(sirsStringConverter.toString(item));
                        }
                    }

                };
                tableCell.setEditable(false);
                return tableCell;
            });

            setComparator(new LabelComparator());
        }
    }


    /**
     * Colonne d'affichage et de mise à jour du PR d'une borne dans un SR.
     */
    private class PRColumn extends TableColumn<SystemeReperageBorne, Number> {

        public PRColumn() {
            super("PR");
            setSortable(false);
            setEditable(true);

            setCellValueFactory(new Callback<CellDataFeatures<SystemeReperageBorne, Number>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(CellDataFeatures<SystemeReperageBorne, Number> param) {
                    return (ObservableValue) param.getValue().valeurPRProperty();
                }
            });

            setCellFactory(new Callback<TableColumn<SystemeReperageBorne, Number>, TableCell<SystemeReperageBorne, Number>>() {
                @Override
                public TableCell<SystemeReperageBorne, Number> call(TableColumn<SystemeReperageBorne, Number> param) {
                    return new FXNumberCell<SystemeReperageBorne>(Double.class);
                }
            });

            addEventHandler(TableColumn.editCommitEvent(), (TableColumn.CellEditEvent<SystemeReperageBorne, Object> event) -> {
                final SystemeReperageBorne srb = event.getRowValue();
                final TronconDigue troncon = getTronconFromProperty();

                if ((srb != null) && (troncon != null)) {

                    final float oldValue = srb.getValeurPR();

                    // On met à jour le PR de la borne modifiée dans le SR sélectionné.
                    srb.setValeurPR(((Number) event.getNewValue()).floatValue());

                    // CAS PARTICULIER :
                    // Dans le SR élémentaire, la valeur du PR équivaut à la distance depuis la borne de début et le calcul est automatique.
                    if (SR_ELEMENTAIRE.equals(systemeReperageProperty().get().getLibelle())) {

                        // Récupération des informations de la borne.
                        final BorneDigue bd = Injector.getSession().getRepositoryForClass(BorneDigue.class).get(srb.getBorneId());
                        if (bd != null) {
                            // Les bornes de début et de fin sont spécifiques
                            if (SirsCore.SR_ELEMENTAIRE_START_BORNE.equals(bd.getLibelle())) {
                                /*
                                On autorise la modification du PR de la borne de début car ce dernier sert d'offset pour
                                les PRs des autres bornes dans le SR élémentaire. Mais on ne déplace jamais la borne de
                                début lorsque son PR est édité.
                                En revanche, il faut mettre à jour les PRs de toutes les bornes dans ce SR.
                                 */
                                final double offset = TronconUtils.getPRStart(troncon, systemeReperageProperty().get(), session);
                                final List<BorneDigue> tronconBornes = session.getRepositoryForClass(BorneDigue.class).get(troncon.getBorneIds());
                                for (final SystemeReperageBorne currentSRB : systemeReperageProperty().get().getSystemeReperageBornes()) {
                                    for (final BorneDigue currentBorne : tronconBornes) {
                                        if (currentBorne.getId().equals(currentSRB.getBorneId())) {
                                            // On met à jour les PRs de toutes les bornes du SR élémentaire, sauf celui de la borne de début.
                                            if (!SirsCore.SR_ELEMENTAIRE_START_BORNE.equals(currentBorne.getLibelle())) {
                                                final ProjectedPoint proj = projectReference(buildSegments(asLineString(getTronconFromProperty().getGeometry())), currentBorne.getGeometry());
                                                currentSRB.setValeurPR((float) (proj.distanceAlongLinear + offset));
                                            }
                                            break;
                                        }
                                    }
                                }


                            } else if (SirsCore.SR_ELEMENTAIRE_END_BORNE.equals(bd.getLibelle())) {
                                /*
                                On ne déplace jamais la borne de fin lorsque son PR est édité, mais de plus ce dernier
                                ne dépend pas de l'utilisateur, mais de la longueur du tronçon et du PR de la borne de
                                début. On restaure donc la valeur calculée et on ignore la valeur modifiée.
                                */
                                Platform.runLater(() -> {
                                    final Alert alert = new Alert(Alert.AlertType.ERROR, "La borne de fin d'un SR élémentaire "
                                            + "\nest fixé à la fin du tronçon et son PR \nne peut être modifié car dépendant seulement "
                                            + "\nde la géométrie du tronçon et du PR de \nla borne de début.", ButtonType.CLOSE);
                                    alert.setResizable(true);
                                    alert.showAndWait();
                                    final double offset = TronconUtils.getPRStart(troncon, systemeReperageProperty().get(), session);
                                    srb.setValeurPR((float) (troncon.getGeometry().getLength() + offset));
                                });

                            } else {
                                // Dans le cas général des autres bornes du SR élémentaire, on veut maintenir la position
                                // de la borne sur le tronçon en cohérence avec son PR modifié.

                                // On a besoin du PR de la borne de début.
                                final double offset = TronconUtils.getPRStart(troncon, systemeReperageProperty().get(), session);

                                /*
                                On vérifie que le PR modifié d'une borne intermédiaire est bien inclus entre celui de la
                                borne de début et celui de la borne de fin.

                                Ce choix est dû au fait qu'une fois le PR d'une borne modifié dans le SR élémentaire, celle-ci
                                est reprojetée sur le tronçon de manière à ce que sa position reste cohérente avec son PR.

                                Le mécanisme de projection n'empêche pas de projeter un point en-deçà ni au-delà des
                                extrémités du tronçon. : le point projeté est alors positionné dans le prolongement du
                                premier ou du dernier segment.

                                Néanmoins, il se pose ensuite un problème de cohérence car le PR calculé de ces points
                                se base non pas sur leur position mais sur leur projection sur le tronçon qui sont respectivement
                                les points de début et de fin.

                                Ainsi, on pourrait provoquer des situations étranges pour l'utilisateur. Par exemple, sur
                                tronçon de 4000m dont les PRs des bornes de début et de fin seraient respectivement de
                                1500 et 1500+4000=5500.

                                On peut créer une borne intermédiare dont le PR est 3000. Si on modifiait ensuite le PR de
                                cette borne à 7000, elle irait se placer dans le prolongement du dernier segment du tronçon,
                                au-delà de la fin de la géométrie.

                                Mais à la première occasion à laquelle le PR de cette borne serait recalculé, sa valeur
                                deviendrait alors 5500 (soit le PR de l'extrémité de fin).

                                Il semble ainsi plus raisonable d'éviter ces incohérences en restreignant la modification
                                du PR des bornes intermédiaires à des valeurs qui ne diffèreront pas des calculs automatiques
                                ultérieurs.
                                */
                                if (srb.getValeurPR() < offset || srb.getValeurPR() > troncon.getGeometry().getLength() + offset) {
                                    Platform.runLater(() -> {
                                        final Alert alert = new Alert(Alert.AlertType.ERROR, "Le PR d'une borne intermédiaire\n"
                                                + " dans le SR élémentaire doit obligatoirement\n être supérieur au PR de la borne de début\n"
                                                + " et inférieur à la longueur du tronçon.");
                                        alert.setResizable(true);
                                        alert.showAndWait();
                                        srb.setValeurPR(oldValue);
                                    });
                                } else {
                                    final LinearReferencing.SegmentInfo[] buildSegments = buildSegments(asLineString(troncon.getGeometry()));
                                    final Point computeCoordinate = LinearReferencingUtilities.computeCoordinate(buildSegments,
                                            GO2Utilities.JTS_FACTORY.createPoint(buildSegments[0].segmentCoords[0]), srb.getValeurPR() - offset, 0.);
                                    bd.setGeometry(computeCoordinate);
                                    Injector.getSession().getRepositoryForClass(BorneDigue.class).update(bd);
                                }
                            }
                        } else {
                            throw new IllegalStateException("Aucune borne n'a été trouvée pour l'identifiant " + srb.getBorneId());
                        }
                    }

                    saveSR.set(true);
                }
            });
        }
    }
}
