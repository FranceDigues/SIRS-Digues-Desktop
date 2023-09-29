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
public enum ShowCasePossibility {

    ABSTRACT("Abrégé"),
    FULL_NAME("Nom Complet"),
    BOTH("Abrégé : Nom Complet");

    public final String name;

    private ShowCasePossibility(final String name) {
        this.name = name;
    }

    /**
     * Renvoie l'énum ShowCasePossibility associé à la chaîne de caractère en
 paramètre.
     *
     */
    public static ShowCasePossibility getFromName(String searchedString) throws IllegalArgumentException {
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
    public static StringConverter<ShowCasePossibility> getConverter() {

        return new StringConverter<ShowCasePossibility>() {
            @Override
            public String toString(ShowCasePossibility object) {
                return object == null ? ShowCasePossibility.BOTH.name : object.name;
            }

            @Override
            public ShowCasePossibility fromString(String string) {
                try {
                    return ShowCasePossibility.getFromName(string);
                } catch (IllegalArgumentException | NullPointerException e) {
                    return ShowCasePossibility.BOTH;
                }
            }
        };
    }

}
