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
package fr.sirs.plugin.vegetation.map;

import fr.sirs.ui.Growl;
import javafx.scene.control.TextField;

/**
 * Based on https://stackoverflow.com/questions/8381374/how-to-implement-a-numberfield-in-javafx-2-0
 */
class NumberTextField extends TextField {

    NumberTextField() {
        super("0");
    }

    public double getValue() {
        return Double.parseDouble(this.getText());
    }

    @Override
    public void replaceText(int start, int end, String text) {
        if (text == null) {
            super.replaceText(start, end, "0");
        } else if (text.matches("^[0-9]*\\.?[0-9]*$")) {
            super.replaceText(start, end, text);
        } else {
            new Growl(Growl.Type.WARNING, "Valeur non valide, une valeur numérique est attendue.").showAndFade();
        }
    }
}

