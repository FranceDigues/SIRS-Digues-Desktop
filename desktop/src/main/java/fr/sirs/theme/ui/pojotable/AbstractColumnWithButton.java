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
package fr.sirs.theme.ui.pojotable;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Abstract class for TableColumn
 * @author Estelle Idée (Geomatys)
 */
public abstract class AbstractColumnWithButton<S, T> extends TableColumn<S, T> {

    public AbstractColumnWithButton(Callback cellValueFactory, Function editFct, Predicate visiblePredicate, final String text, final Image iconImage, final String tooltipText) {
        super(text);
        setSortable(false);
        setResizable(false);
        setPrefWidth(24);
        setMinWidth(24);
        setMaxWidth(24);
        setGraphic(new ImageView(iconImage));

        final Tooltip tooltip = new Tooltip(tooltipText);

        setCellValueFactory(cellValueFactory);

        setCellFactory(param -> {
            ButtonTableCell button = new ButtonTableCell(
                    false, new ImageView(iconImage), visiblePredicate, editFct);
            button.setTooltip(tooltip);
            return button;
        });
    }
}
