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
package fr.sirs.core;

import static fr.sirs.core.SirsCore.NAME;
import fr.sirs.core.authentication.SIRSAuthenticator;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.core.component.SirsDBInfoRepository;
import fr.sirs.util.SystemProxySelector;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.util.HashSet;
import java.util.Iterator;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import javafx.embed.swing.JFXPanel;
import org.apache.sis.test.TestCase;
import java.nio.file.Path;

import org.ektorp.DocumentNotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

public abstract class CouchDBTestCase extends TestCase {

    /**
     * A list of databases to delete at the end of test class.
     */
    private static final Set<String> DBS_TO_DELETE = new HashSet<>();

    /**
     * Default test database name.
     */
    protected static String DB_NAME;

    /**
     * Application context associated with default test database.
     */
    private static ConfigurableApplicationContext APP_CTX;

    /**
     * Local service registry.
     */
    protected static DatabaseRegistry REGISTRY;

    /**
     * Session asociated with default test database.
     */
    @Autowired
    protected SessionCore session;
    
    @BeforeClass
    public static synchronized void initEnvironment() throws Exception {
        // Set http proxy / authentication managers
        ProxySelector.setDefault(new SystemProxySelector());
        Authenticator.setDefault(new SIRSAuthenticator());

        SirsCore.initEpsgDB();

        // initialize fx toolkit
        new JFXPanel();
        // Create / connect to sirs database.
        DB_NAME = "sirs-test".concat(UUID.randomUUID().toString());
        DBS_TO_DELETE.add(DB_NAME);
        REGISTRY = new DatabaseRegistry();
        APP_CTX = REGISTRY.connectToSirsDatabase(DB_NAME, true, true, true);
        final SirsDBInfoRepository sirsDBInfoRepository = APP_CTX.getBean(SirsDBInfoRepository.class);
        Optional<SirsDBInfo> init = sirsDBInfoRepository.get();
        if (!init.isPresent()) {
            sirsDBInfoRepository.setSRID("EPSG:2154");
        }
    }

    @BeforeClass
    public static void initConfigurationPreferences() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(SirsCore.class);
        Path tmpPath = new File(System.getProperty("java.io.tmpdir")).toPath();
        prefs.put("CONFIGURATION_FOLDER_PATH", tmpPath.toString());
        prefs.flush();
    }

    @Before
    public void initTestDatabase() {
        APP_CTX.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @AfterClass
    public static synchronized void clearDatabases() throws IOException {
        final Iterator<String> it = DBS_TO_DELETE.iterator();
        while (it.hasNext()) {
            try {
                REGISTRY.dropDatabase(it.next());
                it.remove();
            } catch (DocumentNotFoundException e) {
                SirsCore.LOGGER.log(Level.FINE, "Database to delete does not exist !", e);
            } catch (Exception e) {
                SirsCore.LOGGER.log(Level.WARNING, "A database cannot be deleted after tests !", e);
            }
        }
    }

    @AfterClass
    public static void clearConfigurationPreferences() throws BackingStoreException {
        Preferences prefs = Preferences.userNodeForPackage(SirsCore.class);
        prefs.clear();
        prefs.flush();
    }

    /**
     * Mark given database to be deleted after this class tests have been performed.
     * @param dbName Name of the database to delete.
     */
    protected static synchronized void deleteAfterClass(final String dbName) {
        DBS_TO_DELETE.add(dbName);
    }
}
