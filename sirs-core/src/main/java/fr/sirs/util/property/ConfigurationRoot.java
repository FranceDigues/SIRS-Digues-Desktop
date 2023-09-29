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

import fr.sirs.core.SirsCore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.lang.IllegalArgumentException;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author maximegavens
 */
public class ConfigurationRoot {
    
     /**
     * Return preference node which contains the path to the configuration folder .sirs.
     *
     * @return Root node. If it does not exists yet, it is created and returned
     * (empty).
     */
    public static Preferences getRootNode() {
        return Preferences.userNodeForPackage(SirsCore.class);
    }

    public static String getRoot() {
        Preferences prefs = getRootNode();
        String rootStr = getPathOrNull(prefs);
        if (!Files.isDirectory(Paths.get(rootStr))) {
            throw new RuntimeException("Behavior unexpected. The path provided is not a directory.");
        }
        return rootStr;
    }

    public static void setRootAndCopy(String from, String to) throws IOException {
        ConfigurationRoot.copy(from, to);
        setRoot(to);
    }

    public static void setRoot(String toSet) {
        getRootNode().put("CONFIGURATION_FOLDER_PATH", toSet);
    }

    public static void copy(String from, String to) throws IOException {
        if (from == null || from.isEmpty()) {
            throw new IllegalArgumentException("from argument can't be null or empty");
        }
        if (to == null || to.isEmpty()) {
            throw new IllegalArgumentException("to argument can't be null or empty");
        }

        Path src = Paths.get(from, "." + SirsCore.NAME);
        Path dest = Paths.get(to, "." + SirsCore.NAME);

        FileUtils.copyDirectory(src.toFile(), dest.toFile());
    }
    
    /**
     * Try to extract the path to configuration folder from node. If the node is null, a
     * null value is returned.
     *
     * @param node the collection that contains the CONFIGURATION_FOLDER_PATH variable.
     * @return The read path, or null if an error happened.
     */
    private static String getPathOrNull(final Preferences node) {
        if (node != null) {
            final String strValue = node.get("CONFIGURATION_FOLDER_PATH", null);
            if (strValue != null) {
                Path pathValue = Paths.get(strValue);
                if (!Files.isDirectory(pathValue)) {
                    throw new RuntimeException("Behavior unexpected. The path provided is not a directory.");
                }
                return strValue;
            } else {
                throw new RuntimeException("Behavior unexpected. No value associate with CONFIGURATION_FOLDER_PATH key. This value should be provide when the application is launched.");
            }
        }
        return null;
    }
    
    public static void delete(final Path toDelete) throws IOException {
        FileUtils.deleteDirectory(toDelete.toFile());
    }

    public static void clear() throws BackingStoreException {
        getRootNode().clear();
    }

    public static void flush() throws BackingStoreException {
        getRootNode().flush();
    }
}
