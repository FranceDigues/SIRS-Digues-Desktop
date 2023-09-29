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
package org.geotoolkit.gui.javafx.util;

import fr.sirs.util.DatePickerConverter;
import java.time.LocalDate;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;

/**
 * Hack : Overrided from Geotk to provide feature depicted in JIRA SYM-1433.
 * See {@link DatePickerConverter} for details.
 *
 * @author Samuel Andrés (Geomatys)
 *
 * @param <S>
 */
public class FXLocalDateCell<S> extends TableCell<S, LocalDate> {
    public static final Image ICON_REMOVE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TIMES_CIRCLE, 16, FontAwesomeIcons.DEFAULT_COLOR), null);
    private final Button del = new Button(null, new ImageView(ICON_REMOVE));
    private final DatePicker field = new DatePicker();
    private final BorderPane pane = new BorderPane(field, null, del, null, null);

    public FXLocalDateCell() {
        setGraphic(field);
        setAlignment(Pos.CENTER);
        setContentDisplay(ContentDisplay.CENTER);
        del.setPrefSize(16, 16);
        del.setFocusTraversable(false);
        del.setOnAction(event -> {
            if (isEditing()) {
                commitEdit(null);
            }
        });
        del.setStyle("-fx-background-color:transparent; -fx-focus-color: transparent;");
        field.setOnAction(event -> commitEdit(field.getValue()));
        DatePickerConverter.register(field);
    }

    public FXLocalDateCell(final boolean blockDateInFuture) {
        this();
        if (blockDateInFuture) blockDateInFuture();
    }


    @Override
    public void startEdit() {
        LocalDate time = getItem();
        if (time == null) {
            time = LocalDate.now();
        }
        field.setValue(time);
        super.startEdit();
        setText(null);
        setGraphic(pane);
        field.requestFocus();
    }

    @Override
    public void commitEdit(LocalDate newValue) {
        itemProperty().set(newValue);
        super.commitEdit(newValue);
        updateItem(newValue, false);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        updateItem(getItem(), false);
    }

    @Override
    protected void updateItem(LocalDate item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(null);
        if(item==null || empty){
            setText(null);
        }
        else {
            setText(item.toString());
        }
    }

    protected void blockDateInFuture() {
        field.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }
}

