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
package fr.sirs.util.javafx;

import fr.sirs.core.SirsCore;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

import java.util.logging.Level;

/**
 * HACK - Redmine 8070
 * Utility to force spinner's value to be updated with the input value even when the user does not press Enter.
 */
public final class SpinnerUtils {

    private SpinnerUtils() {}

    public static <T> void commitOnFocusLost(Spinner<T> spinner) {
        if (spinner == null || !spinner.isEditable()) return;

        spinner.getEditor().focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                commitEditorText(spinner);
            }
        });
    }

    private static <T> void commitEditorText(Spinner<T> spinner) {
        String text = spinner.getEditor().getText();
        SpinnerValueFactory<T> factory = spinner.getValueFactory();

        if (factory == null) return;

        StringConverter<T> converter = factory.getConverter();
        if (converter == null) return;

        try {
            T value = converter.fromString(text);
            factory.setValue(value);
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.WARNING, "Cannot update spinner's value", e);
        }
    }
}
