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

package fr.sirs.migration;

import fr.sirs.core.ModuleDescription;
import fr.sirs.core.SirsCore;
import static fr.sirs.core.SirsCore.INFO_DOCUMENT_ID;
import fr.sirs.core.SirsDBInfo;
import java.util.Map;
import javafx.concurrent.Task;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author maximegavens
 */
public class RemoveOldDependanceConf extends Task {
    final CouchDbConnector connector;

    public RemoveOldDependanceConf(final CouchDbConnector connector) {
        ArgumentChecks.ensureNonNull("Database connector", connector);
        this.connector = connector;
    }

    @Override
    protected Object call() throws Exception {
        try  {
            final SirsDBInfo info = connector.get(SirsDBInfo.class, INFO_DOCUMENT_ID);
            final Map<String, ModuleDescription> moduleDescriptions = info.getModuleDescriptions();

            if (moduleDescriptions.containsKey("plugin-dependance")) {
                updateTitle("Suppréssion de l'ancienne configuration du plugin dépendance");
                moduleDescriptions.remove("plugin-dependance");
                info.addModuleDescriptions(moduleDescriptions);
                updateMessage("Mise à jour de la base de données");
                connector.update(info);
            }
        } catch (Exception ex) {
            SirsCore.LOGGER.warning("Something goes wrong during RemoveOldDependanceConf Upgrade: " + ex);
        }
        return true;
    }
}
