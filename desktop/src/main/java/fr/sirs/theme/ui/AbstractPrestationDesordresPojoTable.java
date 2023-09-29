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
import fr.sirs.core.model.Element;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 *
 * @author Estelle Idée (Geomatys)
 */
public abstract class AbstractPrestationDesordresPojoTable extends ListeningPojoTable {

    private final Button uiLink = new Button(null, new ImageView(SIRS.ICON_MAGNET_WHITE));

    /**
     * Creation of a @ListeningPojoTable including a button to link the container to the desordres of its parent
     * @param pojoClass
     * @param title
     * @param container
     */
    public AbstractPrestationDesordresPojoTable(Class pojoClass, String title, final ObjectProperty<? extends Element> container, boolean isOnTronconLit) {
        super(pojoClass, title, container);

        searchEditionToolbar.getChildren().add(6, uiLink);

        uiLink.managedProperty().bind(uiLink.visibleProperty());

        uiLink.getStyleClass().add(BUTTON_STYLE);
        uiLink.disableProperty().bind(editableProperty.not());
        uiLink.setTooltip(new Tooltip("Lier à l'ensemble des désordres ouverts"));

        uiLink.setOnAction(linkParentDesordres(container, isOnTronconLit));

        createNewProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                uiLink.visibleProperty().set(false);
            } else {
                uiLink.visibleProperty().set(true);
            }
        });
    }

    /**
     * Method to link all the container's parent's Desordres to the container
     * @param container
     * @param isOnTronconLit boolean specific used to get DesordreLit in core
     * @return @{@link java.beans.EventHandler}
     */
    protected abstract EventHandler linkParentDesordres(final ObjectProperty<? extends Element> container, final boolean isOnTronconLit);

    protected ButtonType showConfirmationAlertAndGetResult(String message) {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.YES, ButtonType.NO);
        alert.setResizable(true);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert.showAndWait().get();
    }

    protected void showWarningOrInformationAlert(String message, boolean isWarning) {
        final Alert alert;
        if (isWarning)
            alert = new Alert(Alert.AlertType.WARNING,
                message);
        else
            alert = new Alert(Alert.AlertType.INFORMATION,
                    message);
        alert.setResizable(true);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}
