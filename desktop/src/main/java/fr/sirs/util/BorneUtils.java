package fr.sirs.util;

import fr.sirs.Injector;
import fr.sirs.Session;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.BorneDigueRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.referencing.LinearReferencing;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static fr.sirs.core.LinearReferencingUtilities.asLineString;
import static fr.sirs.core.SirsCore.SR_ELEMENTAIRE;
import static org.geotoolkit.referencing.LinearReferencing.buildSegments;
import static org.geotoolkit.referencing.LinearReferencing.projectReference;

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

/**
 *
 * @author Estelle Idée (Geomatys)
 */
public class BorneUtils {

    public static Predicate<String> isStartOrEndBorneLibelle = libelle -> SirsCore.SR_ELEMENTAIRE_START_BORNE.equals(libelle) || SirsCore.SR_ELEMENTAIRE_END_BORNE.equals(libelle);

    /**
     * Opens a @{@link Dialog} to update the borne's libelle and designation.
     * @param selectedItem the borne to be modified : change designation and libelle.
     * @return true if the borne has been updated. False otherwise.
     */
    public static boolean openDialogAndRenameBorne(BorneDigue selectedItem) {
        String oldLibelle = selectedItem.getLibelle();

        if (isStartOrEndBorneLibelle.test(oldLibelle)) {
            openAlert("Les bornes de début et de fin d'un tronçon ne peuvent être renommées.");
            return false;
        }

        String oldDesignation = selectedItem.getDesignation();
        AtomicReference<String> newLibelle = new AtomicReference<>("");
        AtomicReference<String> newDesignation = new AtomicReference<>(oldDesignation);

        // Form to fill in the new borne libelle and designation.
        final Dialog dialog = new Dialog<Boolean>();
        dialog.setResizable(true);
        dialog.setTitle("Borne existante : " + oldDesignation + " : " + oldLibelle);
        dialog.setGraphic(null);
        dialog.setHeaderText("Modification des informations");

        final GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6 * 2);
        grid.setPadding(new Insets(6));

        TextField designationInput = addTextField(grid, "Désignation", oldDesignation, 0);
        TextField libelleInput = addTextField(grid, "Libellé", oldLibelle, 1);

        final DialogPane pane = dialog.getDialogPane();

        pane.setContent(grid);
        pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);


        dialog.setResultConverter((button) -> {
            if (!ButtonType.OK.equals(button)) {
                return null;
            }
            newLibelle.set(libelleInput.getText());
            newDesignation.set(designationInput.getText());
            String newLib = newLibelle.get();
            String newDes = newDesignation.get();
            // if the designation and libelle haven't been modified, then we want to quit the renaming process.
            if (oldLibelle.equals(newLib) && oldDesignation.equals(newDes))
                return null;
            // if the designation and libelle are both empty, then we want to quit the renaming process.
            if (!((newLib != null && !newLib.trim().isEmpty()) || (newDes != null && !newDes.trim().isEmpty()))) return null;

            return true;
        });

        Optional result = dialog.showAndWait();

        // if the dialog result is empty, then we want to stop the renaming process.
        if (!result.isPresent()) return false;

        String newLib = newLibelle.get();
        String newDes = newDesignation.get();

        // We check that the given libelle is not part of the libelles used for the elementary SR.
        if (isStartOrEndBorneLibelle.test(newLib)) {
            openAlert("Le libellé de borne " + newLib + " est réservé au SR élémentaire.");
            return false;
        }

        // if the libelle nor the designation has been updated,
        // then the selectedItem shall not be updated in the database

        final TaskManager.MockTask renamer = new TaskManager.MockTask("Changement de libellé et de désignation d'une borne", () -> {
            if (!newLib.trim().isEmpty())
                selectedItem.setLibelle(newLib);
            if (!newDes.trim().isEmpty())
                selectedItem.setDesignation(newDes);
            InjectorCore.getBean(BorneDigueRepository.class).update(selectedItem);
        });

        renamer.setOnFailed(evt ->
                Platform.runLater(() ->
                        GeotkFX.newExceptionDialog("Une erreur est survenue lors du changement de libellé/désignation de la borne.", renamer.getException()).show()));

        TaskManager.INSTANCE.submit(renamer);

        return true;
    }

    private static TextField addTextField(GridPane grid, String labeltext, String initValue, int rowIndex) {
        Label label = new Label(labeltext);
        TextField textField = new TextField(initValue);
        label.setLabelFor(textField);
        grid.add(label, 0, rowIndex);
        grid.add(textField, 1, rowIndex);
        return textField;
    }

    public static void openAlert(String alertMessage) {
        final Alert alert = new Alert(Alert.AlertType.ERROR, alertMessage, ButtonType.CLOSE);
        alert.setResizable(true);
        alert.showAndWait();
    }

    /**
     * Ajout d'une borne au système de repérage.
     *
     * @param borne @{@link BorneDigue} à ajouter au @{@link SystemeReperage}.
     * @param troncon @{@link TronconDigue} auquel appartient le @{@link SystemeReperage}.
     * @param sr @{@link SystemeReperage} auquel on souhaite ajouter la borne.
     * @param session
     * @return true si la @{@link BorneDigue} a bien été ajoutée au @{@link SystemeReperage}, false sinon.
     */
    public static boolean addBorneToSR(final BorneDigue borne, final TronconDigue troncon, final SystemeReperage sr, final Session session) {
        //on vérifie que la borne n'est pas deja dans la liste
        for (final SystemeReperageBorne srb : sr.getSystemeReperageBornes()) {
            if (borne.getDocumentId().equals(srb.borneIdProperty().get())) {
                //la borne fait deja partie de ce SR
                return false;
            }
        }

        //reference dans le SR
        final SystemeReperageBorne srb = Injector.getSession().getElementCreator().createElement(SystemeReperageBorne.class);
        srb.borneIdProperty().set(borne.getDocumentId());

        // Si on est dans le SR élémentaire, il faut calculer le PR de la borne de manière automatique (SYM-1429).
        if (troncon != null && SR_ELEMENTAIRE.equals(sr.getLibelle())) {
            final LinearReferencing.ProjectedPoint proj = projectReference(buildSegments(asLineString(troncon.getGeometry())), borne.getGeometry());

            // Pour obtenir le PR calculé dans le SR élémentaire, il faut ajouter le PR de la borne de départ à la distance du point projeté sur le linéaire.
            srb.setValeurPR((float) proj.distanceAlongLinear + TronconUtils.getPRStart(troncon, sr, session));
        } else {
            srb.setValeurPR(0.f);
        }

        // sauvegarde du SR
        return sr.systemeReperageBornes.add(srb);
    }}
