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

import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import org.junit.Assert;
import fr.sirs.core.CouchDBTestCase;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
public class PluginDependanceTest extends CouchDBTestCase {

    @Test
    public void integrityTest() throws IOException {
        PluginDependance plugin = new PluginDependance();

        Assert.assertEquals(plugin.getTitle(), "Module dépendance et AH");
        Assert.assertEquals(plugin.getImage(), null);
        Assert.assertEquals(plugin.getLoadingMessage().get(), "module dépendance et AH");
        Assert.assertEquals(plugin.getMapActions(null), Collections.EMPTY_LIST);
        Assert.assertNotNull(plugin.getModelImage().get());
        Assert.assertEquals(plugin.getThemes().size(), 2);

        //Must initialize database first
        // TODO once a database as been created for the tests, uncomment the two lines below
//        Assert.assertEquals(plugin.getMapItems().size(), 1);
//        Assert.assertEquals(plugin.getMapItems().get(0).items().size(), 11);
    }
}
