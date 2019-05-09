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
package fr.sirs.util.property;

import javafx.util.StringConverter;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public enum ShowCase_Possibility {

    ABSTRACT("Abrégé", Boolean.TRUE),
    FULL_NAME("Nom Complet", Boolean.FALSE),
    BOTH("Abrégé : Nom Complet", null);

    public final String name;
    public final Boolean booleanValue;

    private ShowCase_Possibility(final String name, final Boolean value) {
        this.name = name;
        this.booleanValue = value;
    }

    /**
     * Renvoie l'énum ShowCase_Possibility associé à la chaîne de caractère en
     * paramètre.
     *
     * @param searchedString
     * @return
     * @throws IllegalArgumentException
     */
    public static ShowCase_Possibility getFromName(String searchedString) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("searchedString", searchedString);
        if (searchedString.equals(ABSTRACT.name)) {
            return ABSTRACT;
        } else if (searchedString.equals(BOTH.name)) {
            return BOTH;
        } else if (searchedString.equals(FULL_NAME.name)) {
            return FULL_NAME;
        }
        throw new IllegalArgumentException(searchedString + " n'est pas un nom valide pour l'énum ShowCase_Possibility");
    }

    /**
     * Renvoie un StringConverter permettant la conversion de
     * ShowCase_Possibility en String et inversement. L'énum
     * ShowCase_Possibility.BOTH est associée à n'importe quelle chaîne de
     * caractère ne correspondant pas aux valeurs de l'énum.
     * 
     * @return StringConverter<ShowCase_Possibility>
     */
    public static StringConverter<ShowCase_Possibility> getConverter() {

        return new StringConverter<ShowCase_Possibility>() {
            @Override
            public String toString(ShowCase_Possibility object) {
                return object == null ? ShowCase_Possibility.BOTH.name : object.name;
            }

            @Override
            public ShowCase_Possibility fromString(String string) {
                try {
                    return ShowCase_Possibility.getFromName(string);
                } catch (IllegalArgumentException | NullPointerException e) {
                    return ShowCase_Possibility.BOTH;
                }
            }
        };
    }

}
