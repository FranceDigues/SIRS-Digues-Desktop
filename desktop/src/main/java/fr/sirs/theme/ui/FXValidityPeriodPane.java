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

import fr.sirs.SIRS;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.ui.Growl;
import fr.sirs.util.DatePickerConverter;
import java.time.LocalDate;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.BorderPane;

/**
 * A simple editor for edition of temporal bornes of an object.
 * @author Alexis Manin (Geomatys)
 */
public class FXValidityPeriodPane extends BorderPane {

    @FXML private DatePicker uiDateDebut;
    @FXML private DatePicker uiDateFin;

    private final SimpleObjectProperty<AvecBornesTemporelles> target = new SimpleObjectProperty<>();

    private final SimpleBooleanProperty disableFieldsProperty = new SimpleBooleanProperty(false);
    // do not use directly. Use endDateWeakListener.
    private ChangeListener<LocalDate> endDateListener = (obs, oldVal, newVal) -> checkEndDateOk(oldVal, newVal);
    private WeakChangeListener<LocalDate> endDateWeakListener = new WeakChangeListener<>(endDateListener);

    public FXValidityPeriodPane() {
        super();
        SIRS.loadFXML(this);
        target.addListener(this::targetChanged);

        uiDateDebut.disableProperty().bind(disableFieldsProperty);
        uiDateFin.disableProperty().bind(disableFieldsProperty);

        uiDateDebut.setDayCellFactory((p) -> new FilteredDayCell((date) -> {
            final LocalDate fin = uiDateFin.valueProperty().get();
            return fin == null? true : (fin.isAfter(date) || fin.isEqual(date));
        }));

        uiDateFin.setDayCellFactory((p) -> new FilteredDayCell((date) -> {
            final LocalDate debut = uiDateDebut.valueProperty().get();
            return debut == null? true : (debut.isBefore(date) || debut.isEqual(date));
        }));

        uiDateDebut.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                final LocalDate dateFin = uiDateFin.getValue();
                if (dateFin != null && dateFin.isBefore(newVal)) {
                    new Growl(Growl.Type.WARNING, "Impossible d'avoir une date de fin antérieure à la date de début.").showAndFade();
                    uiDateDebut.setValue(oldVal);
                }
            }
        });

        uiDateFin.valueProperty().addListener(endDateWeakListener);
        DatePickerConverter.register(uiDateDebut);
        DatePickerConverter.register(uiDateFin);
    }

    protected boolean checkEndDateOk(LocalDate oldVal, LocalDate newVal) {
        if (newVal != null) {
            final LocalDate dateDebut = uiDateDebut.getValue();
            if (dateDebut != null && dateDebut.isAfter(newVal)) {
                new Growl(Growl.Type.WARNING, "Impossible d'avoir une date de fin antérieure à la date de début.").showAndFade();
                uiDateFin.setValue(oldVal);
                return false;
            }
        }
        return true;
    }

    private void targetChanged(ObservableValue<? extends AvecBornesTemporelles> observable, AvecBornesTemporelles oldTarget, AvecBornesTemporelles newTarget) {
        if (oldTarget != null) {
            uiDateDebut.valueProperty().unbindBidirectional(oldTarget.date_debutProperty());
            uiDateFin.valueProperty().unbindBidirectional(oldTarget.date_finProperty());
        }

        if (newTarget != null) {
            uiDateDebut.valueProperty().bindBidirectional(newTarget.date_debutProperty());
            uiDateFin.valueProperty().bindBidirectional(newTarget.date_finProperty());
        }
    }

    public BooleanProperty disableFieldsProperty(){
        return disableFieldsProperty;
    }

    public SimpleObjectProperty<AvecBornesTemporelles> targetProperty() {
        return target;
    }

    /**
     * A cell activating only if a date matches input predicate.
     */
    private static class FilteredDayCell extends DateCell {

        private final Predicate<LocalDate> filter;

        public FilteredDayCell(final Predicate<LocalDate> p) {
            this.filter = p;
        }

        @Override
        public void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null && !filter.test(item)) {
                setDisable(true);
            }
        }
    }

    DatePicker getDateFinPicker() {
        return uiDateFin;
    }

    WeakChangeListener<LocalDate> getEndDateListener() {
        return endDateWeakListener;
    }
    public void removeListeners() {
        uiDateFin.valueProperty().removeListener(endDateWeakListener);
    }
}
