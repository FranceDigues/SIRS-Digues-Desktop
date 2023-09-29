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
package fr.sirs;

import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.util.DatePickerConverter;
import java.time.LocalDate;
import java.util.function.Predicate;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import org.apache.sis.measure.Range;

/**
 *
 * Classe abstraite permettant de restreindre la sélection d'objets à imprimer à
 * une période de temps.
 *
 * @author Samuel Andrés (Geomatys)
 * @author Maxime Gavens (Geomatys)
 */
public abstract class TemporalTronconChoicePrintPane extends TronconChoicePrintPane {

    @FXML
    protected DatePicker uiOptionDebut;
    @FXML
    protected DatePicker uiOptionFin;

    @FXML
    protected DatePicker uiOptionDebutLastObservation;
    @FXML
    protected DatePicker uiOptionFinLastObservation;

    @FXML
    protected CheckBox uiOptionNonArchive;
    @FXML
    protected DatePicker uiOptionDebutNonArchive;
    @FXML
    protected DatePicker uiOptionFinNonArchive;

    @FXML
    protected CheckBox uiOptionArchive;
    @FXML
    protected DatePicker uiOptionDebutArchive;
    @FXML
    protected DatePicker uiOptionFinArchive;

    public TemporalTronconChoicePrintPane(final Class forBundle) {
        super(forBundle);

        uiOptionNonArchive.disableProperty().bind(uiOptionArchive.selectedProperty());
        uiOptionArchive.disableProperty().bind(uiOptionNonArchive.selectedProperty());
        uiOptionNonArchive.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                if (uiOptionArchive.isSelected()) {
                    uiOptionArchive.setSelected(false);
                }
                uiOptionDebutArchive.setValue(null);
                uiOptionFinArchive.setValue(null);
            } else {
                uiOptionDebutNonArchive.setValue(null);
                uiOptionFinNonArchive.setValue(null);
            }
        });
        uiOptionArchive.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                if (uiOptionNonArchive.isSelected()) {
                    uiOptionNonArchive.setSelected(false);
                }
                uiOptionDebutNonArchive.setValue(null);
                uiOptionFinNonArchive.setValue(null);
            } else {
                uiOptionDebutArchive.setValue(null);
                uiOptionFinArchive.setValue(null);
            }
        });

        uiOptionDebutArchive.disableProperty().bind(uiOptionNonArchive.selectedProperty());
        uiOptionFinArchive.disableProperty().bind(uiOptionNonArchive.selectedProperty());
        uiOptionDebutNonArchive.disableProperty().bind(uiOptionArchive.selectedProperty());
        uiOptionFinNonArchive.disableProperty().bind(uiOptionArchive.selectedProperty());

        DatePickerConverter.register(uiOptionDebut);
        DatePickerConverter.register(uiOptionFin);
        DatePickerConverter.register(uiOptionDebutArchive);
        DatePickerConverter.register(uiOptionFinArchive);
        DatePickerConverter.register(uiOptionDebutNonArchive);
        DatePickerConverter.register(uiOptionFinNonArchive);
        DatePickerConverter.register(uiOptionDebutLastObservation);
        DatePickerConverter.register(uiOptionFinLastObservation);
    }

    /**
     * Check miscellaneous user options related to objects temporal lifecycle.
     */
    class TemporalPredicate implements Predicate<AvecBornesTemporelles> {

        /**
         * A flag which role is to tell what we should do with archived objects.
         * There're 3 possible states :
         * - Negative : We must exclude archived objects.
         * - Zero : We must include all objects, archived or not.
         * - Positive : We must include archived objects only.
         */
        final byte archiveOption;

        final Range<LocalDate> selectedRange;
        final Range<LocalDate> archiveRange;
        final Range<LocalDate> nonArchiveRange;

        TemporalPredicate() {
            if (uiOptionNonArchive.isSelected())
                archiveOption = -1;
            else if (uiOptionArchive.isSelected())
                archiveOption = 1;
            else
                archiveOption = 0;

            LocalDate start = uiOptionDebutArchive.getValue();
            LocalDate end = uiOptionFinArchive.getValue();
            if (archiveOption == -1 || (start == null && end == null)) {
                archiveRange = null;
            } else {
                archiveRange = new Range<>(LocalDate.class, start == null? LocalDate.MIN : start, true, end == null? LocalDate.MAX : end, true);
            }

            start = uiOptionDebutNonArchive.getValue();
            end = uiOptionFinNonArchive.getValue();
            if (archiveOption == 1 || (start == null && end == null)) {
                nonArchiveRange = null;
            } else {
                nonArchiveRange = new Range<>(LocalDate.class, start == null? LocalDate.MIN : start, true, end == null? LocalDate.MAX : end, true);
            }

            start = uiOptionDebut.getValue();
            end = uiOptionFin.getValue();
            if (start == null && end == null) {
                selectedRange = null;
            } else {
                selectedRange = new Range<>(LocalDate.class, start == null? LocalDate.MIN : start, true, end == null? LocalDate.MAX : end, true);
            }
        }

        @Override
        public boolean test(AvecBornesTemporelles input) {
            // Si on choisit "exclure les éléments archivés"..
            if (archiveOption == -1) {
                // on retire l'élément s'il est archivé et..
                if (input.getDate_fin() != null
                        // que l'intervalle est nulle ou qu'il contient l'élément
                        && (nonArchiveRange == null || nonArchiveRange.contains(input.getDate_fin())))
                    return false;
            // Si on choisit "inclure les éléments archivés"..
            } else if (archiveOption == 1) {
                // On rétire les éléments non archivés
                if (input.getDate_fin() == null) return false;
                // Si un intervalle temporel est donné, on rétire les éléments
                // archivés en dehors de l'intervalle.
                if (archiveRange != null && !archiveRange.contains(input.getDate_fin())) return false;
            }

            return selectedRange == null || selectedRange.contains(input.getDate_debut());
        }
    }
}
