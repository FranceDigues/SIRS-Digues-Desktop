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

import fr.sirs.SIRS;
import javafx.util.Callback;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Column used in BorneDigue pojo table present in @{@link fr.sirs.digue.FXSystemeReperagePane}
 * Used to edit a BorneDigue's libelle and designation.
 *
 * @author Estelle Idée (Geomatys)
 */
public class RenameBorneColumn extends AbstractColumnWithButton {

    public RenameBorneColumn(Callback cellValueFactory, Function editFct, Predicate visiblePredicate) {
        super(cellValueFactory, editFct, visiblePredicate, "Renommer", SIRS.ICON_EDITION,"Renommer l'élément");
    }
}
