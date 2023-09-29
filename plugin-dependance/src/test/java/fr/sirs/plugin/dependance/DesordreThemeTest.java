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

package fr.sirs.plugin.dependance;

import fr.sirs.core.CouchDBTestCase;
import fr.sirs.core.component.DesordreDependanceRepository;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.theme.AbstractTheme;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.ObservableList;
import org.ektorp.DocumentNotFoundException;
import org.junit.Test;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
public class DesordreThemeTest extends CouchDBTestCase {

    @Autowired
    DesordreDependanceRepository desordreDependanceRepository;

    @Test
    public void generateThemeManagerTest() {
        // Creates a random element
        final DesordreDependance desordre = ElementCreator.createAnonymValidElement(DesordreDependance.class);
        desordre.setDesignation("testDesordre1");
        desordre.setAmenagementHydrauliqueId("1234567890");
        desordreDependanceRepository.add(desordre);

        final AbstractTheme.ThemeManager tm = DesordreTheme.generateThemeManager("tabTitle1", DesordreDependance.class);

        // Checks that an item is successfully recovered
        final Function<String, ObservableList<DesordreDependance>> byAhId = tm.getExtractor();
        final ObservableList<DesordreDependance> desordres = byAhId.apply("1234567890");
        Assert.assertEquals(1, desordres.size());
        Assert.assertEquals("testDesordre1", desordres.get(0).getDesignation());

        // Checks that the item is successfully deleted
        final Consumer deletor = tm.getDeletor();
        deletor.accept(desordre);
        try {
            DesordreDependance notFound = desordreDependanceRepository.get(desordre.getId());
            Assert.assertTrue(false);
        } catch (DocumentNotFoundException ex) {
            Assert.assertTrue(true);
        }
    }
}
