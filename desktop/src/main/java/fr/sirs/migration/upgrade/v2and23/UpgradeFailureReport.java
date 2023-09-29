/**
 *
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

package fr.sirs.migration.upgrade.v2and23;

import fr.sirs.core.model.Element;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class UpgradeFailureReport {

    private final Map<Element, Set<String>> failed = new HashMap<>();

    private final StringBuilder report = new StringBuilder();

    synchronized String addFailure(final Element element, final String id) {

        failed.compute(element, (elt, list) -> {
            if (list == null) {
                list = new HashSet<>();
            }
            list.add(id);
            return list;
        });

        return completeReport(element, id);
    }

    private String completeReport(final Element element, final String id) {
        final String message = new StringBuilder().append("Une erreur est survenue lors de la mise à jour de l'élément lié à l'identifiant : ")
                .append(id)
                .append(" depuis l'élément : ")
                .append(element.getDocumentId())
                .append(" de la classe ")
                .append(element.getClass())
                .append("\n").toString();

        report.append(message);
        return message;
    }

    public Map<Element, Set<String>> getFailed() {
        return failed;
    }

    public String getStringReport() {
        return (report.length()>0)? report.toString() : "Aucune erreur n'a été notifiée pendant la mise à Jour.";
    }

    Set<String> getFailsFor(final Element element) {
        return this.getFailed().get(element);
    }

}
