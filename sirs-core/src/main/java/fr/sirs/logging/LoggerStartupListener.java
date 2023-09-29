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
package fr.sirs.logging;

import ch.qos.logback.core.Context;
import fr.sirs.core.SirsCore;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is used to configure the output of logs in the configuration folder.
 * 
 * @author Maxime Gavens - Geomatys
 */
public class LoggerStartupListener extends LevelChangePropagator {

    @Override
    public void start() {
        if (!isStarted()) {
            final Context context = getContext();
            Path logPath = new File(System.getProperty("java.io.tmpdir")).toPath();
            try {
                final Preferences prefs = Preferences.userNodeForPackage(SirsCore.class);
                final String rootPath = prefs.get("CONFIGURATION_FOLDER_PATH", "none");

                if (!rootPath.equals("none")) {
                    logPath = Paths.get(rootPath, "."+SirsCore.NAME);
                }
            } catch (Exception ex) {
                SirsCore.LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            } finally {
                context.putProperty("CONF_PATH", logPath.toAbsolutePath().toString());
            }
        }
        super.start();
    }
}