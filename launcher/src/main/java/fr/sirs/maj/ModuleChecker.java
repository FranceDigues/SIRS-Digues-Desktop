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
package fr.sirs.maj;

import fr.sirs.Plugin;
import fr.sirs.PluginInfo;
import fr.sirs.Plugins;
import fr.sirs.SIRS;
import fr.sirs.core.ModuleDescription;
import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.core.component.UtilisateurRepository;
import fr.sirs.core.plugins.PluginLoader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.map.MapItem;

/**
 * Designed to verify compatibility between installed plugins and database modules.
 *
 * @author Alexis Manin (Geomatys)
 */
public class ModuleChecker extends Task<Boolean> {

    private final DatabaseRegistry dbRegistry;
    private final String dbName;

    private CouchDbConnector connector;
    private List<Upgrade> upgrades;
    private Map<PluginInfo, ModuleDescription> obsoletePlugins;

    public ModuleChecker(final DatabaseRegistry dbService, final String dbName) {
        ArgumentChecks.ensureNonNull("CouchDB service", dbService);
        ArgumentChecks.ensureNonNull("Database name", dbName);
        dbRegistry = dbService;
        this.dbName = dbName;
    }

    @Override
    public Boolean call() throws IOException, InterruptedException, ExecutionException {
        updateTitle("Analyse des modules");
        analyzeModules();
        if (alertIfObsolete()) {
            cancel();
        }

        if (!upgrades.isEmpty()) {
            if (askForUpgrade()) {
                upgrade();
            } else {
                cancel();
            }
        }

        return true;
    }

    private void upgrade() throws InterruptedException, ExecutionException {
        final ChangeListener<String> msgListener = (obs, oldMsg, newMsg) -> updateMessage(newMsg);
        final String titleFormat = "Mise à jour %d sur %d";
        for (int i = 0; i < upgrades.size(); i++) {
            updateTitle(String.format(titleFormat, i + 1, upgrades.size()));
            final Upgrade upgrade = upgrades.get(i);
            for (Task t : upgrade.upgradeTasks ) {
                final ChangeListener<Number> progressListener = (obs, oldValue, newValue) -> {
                    updateProgress(t.getWorkDone(), t.getTotalWork());
                };
                final ChangeListener<State> cancelListener = (obs, oldState, newState) -> t.cancel();

                SIRS.fxRun(false, () -> {
                    t.progressProperty().addListener(progressListener);
                    t.messageProperty().addListener(msgListener);
                    stateProperty().addListener(cancelListener);
                });

                t.run();

                SIRS.fxRun(false, () -> {
                    t.progressProperty().removeListener(progressListener);
                    t.messageProperty().removeListener(msgListener);
                    stateProperty().removeListener(cancelListener);
                });

                // ensure no error occurred
                t.get();
            }

            //Retrieve info on each updagre cause upgrades can eventually update info object, avoiding conflicting updates
            SirsDBInfo info = DatabaseRegistry.getInfo(connector).orElse(null);
            if (info == null) throw new IllegalStateException("Chosen database is not SIRS database !"); // should never happen...
            info.getModuleDescriptions().get(upgrade.toUpgrade.getConfiguration().getName()).setVersion(getVersion(upgrade.toUpgrade));
            connector.update(info);
        }
    }

    /**
     * Ask user if he wants to proceed to detected module upgrade.
     * @return True if user has validated module upgrade. False if user refused / cancelled.
     */
    private boolean askForUpgrade() {
        final StringBuilder message = new StringBuilder("Les modules suivants requièrent une mise à jour de la base de donnée :");
        for (final Upgrade upgrade : upgrades) {
            final PluginInfo conf = upgrade.toUpgrade.getConfiguration();
                message.append(System.lineSeparator()).append('\t').append(upgrade.toUpgrade.getTitle())
                        .append(" : mise à jour de ").append(upgrade.oldVersion.stringVersion)
                        .append(" vers ").append(conf.getVersionMajor()).append('.').append(conf.getVersionMinor());
                if((conf.getVersionMajor()==2)&&(conf.getVersionMinor()==23)) {
                    message.append("\nMise à jour importante, il est conseillé d'effectuer une copie (sauvegarde) de la base de données avant sa mise à jour.\n");
                }
        }
        message.append(System.lineSeparator()).append(System.lineSeparator()).append("La mise à jour nécessite l'arrêt des synchronisations en cours sur la base. Confirmer ?");

        final ButtonType result = SIRS.fxRunAndWait(() -> {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message.toString(), ButtonType.CANCEL, ButtonType.OK);
            alert.setResizable(true);

            return alert.showAndWait().orElse(ButtonType.CANCEL);
        });

        if (ButtonType.OK.equals(result)) {
            updateMessage("Vérification des droits de l'utilisateur");
            boolean isAdmin = isAdmin();
            if (!isAdmin) {
                SIRS.fxRun(false, () -> new Alert(Alert.AlertType.ERROR, "Echec d'identification, ou droits insuffisants.", ButtonType.OK).show());
            }
            updateMessage("");
            return isAdmin;
        }

        return false;
    }

    /**
     * Check if any obsolete plugin have been found in previous analysis. If it's
     * the case, an alert is displayed to inform user he cannot connect on wanted
     * database because of module incompatibility.
     * @return True if any obsolete module is installed.
     */
    private boolean alertIfObsolete() {
        final boolean obsoleteFound = obsoletePlugins.size() > 0;
        if (obsoleteFound) {
                final StringBuilder messenger = new StringBuilder("Certains modules installés doivent être mis à jour ou supprimés car la base de données choisie nécessite des versions plus récentes :");
                final String lineSep = System.lineSeparator();
                for (final Map.Entry<PluginInfo, ModuleDescription> entry : obsoletePlugins.entrySet()) {
                    final PluginInfo installed = entry.getKey();
                    final ModuleDescription dbModule = entry.getValue();
                    messenger.append(lineSep).append(installed.getName()).append(" :")
                            .append(lineSep).append('\t').append("Installé :            ").append(installed.getVersionMajor()).append('.').append(installed.getVersionMinor())
                            .append(lineSep).append('\t').append("Utilisé par la base : ").append(dbModule.getVersion());
                }

                SIRS.fxRun(false, () -> {
                    final Alert alert = new Alert(Alert.AlertType.WARNING, messenger.toString(), ButtonType.OK);
                    alert.setResizable(true);
                    alert.showAndWait();
                });
        }
        return obsoleteFound;
    }

    /**
     * Compares installed modules with the ones described in database.
     * It's an essential task, as it initializes most of checker's attributes.
     */
    private void analyzeModules() throws IOException {
        synchronized (dbRegistry) {
            connector = dbRegistry.createConnector(dbName, DatabaseRegistry.DatabaseConnectionBehavior.FAIL_IF_NOT_EXISTS);
        }

        final SirsDBInfo info = DatabaseRegistry.getInfo(connector).orElse(null);
        if (info == null) {
            // should never happen...
            throw new IllegalStateException("Chosen database is not SIRS database !");
        }

        final ArrayList<Upgrade> tmpUpgrades = new ArrayList<>();
        final HashMap<PluginInfo, ModuleDescription> tmpObsoletePlugins = new HashMap<>();

        if (info.getModuleDescriptions() != null) {

            // First, we ensure modules classes are ready for use.
            final ClassLoader scl = ClassLoader.getSystemClassLoader();
            if (scl instanceof PluginLoader) {
                ((PluginLoader) scl).loadPlugins();
            }

            Map<String, Plugin> plugins = Plugins.getPluginMap();

            for (final ModuleDescription desc : info.getModuleDescriptions().values()) {
                Plugin appModule = plugins.get(desc.getName());
                if (appModule != null) {
                    final PluginInfo modConf = appModule.getConfiguration();
                    final int comparison = new ModuleVersion(desc.getVersion()).compareTo(modConf); //>0 si desc.getVersion() > modConf
                    if (comparison < 0) {
                        try {
                            tmpUpgrades.add(new Upgrade(appModule, desc));
                        } catch (IllegalArgumentException e) {
                            SIRS.LOGGER.log(Level.FINE, "No upgrade available for plugin".concat(modConf.getName()), e);
                            // No upgrade available. We set version according to installed module.
                            desc.setVersion(getVersion(appModule));
                            connector.update(info);
                        }
                    } else if (comparison > 0) {
                        tmpObsoletePlugins.put(modConf, desc);
                    }
                }
            }
        }

        obsoletePlugins = Collections.unmodifiableMap(tmpObsoletePlugins);
        upgrades = Collections.unmodifiableList(tmpUpgrades);
    }

    /**
     * Contains information relative to the upgrade process of a plugin.
     */
    public class Upgrade {
        /** Version description of the module found in database (i.e the old version to upgrade) */
        public final ModuleVersion oldVersion;
        /** Installed (i.e new version) plugin which will handle migration process. */
        public final Plugin toUpgrade;
        /** Upgrade process, provided by installed plugin. */
        public final LinkedHashSet<Task> upgradeTasks;

        protected Upgrade(final Plugin toUpgrade, final ModuleDescription oldModule) {
            ArgumentChecks.ensureNonNull("Plugin providing update process", toUpgrade);
            ArgumentChecks.ensureNonNull("Module description of old version used by database", oldModule);

            this.toUpgrade = toUpgrade;
            oldVersion = new ModuleVersion(oldModule.getVersion());

            if (oldVersion.version.length < 2) {
                throw new IllegalArgumentException("Given module description does not contain any acceptable version.");
            }

            this.upgradeTasks = new LinkedHashSet<>();
            toUpgrade.findUpgradeTasks(oldVersion.version[0], oldVersion.version[1], connector, upgradeTasks, dbRegistry);
            if (upgradeTasks.isEmpty()) {
                throw new IllegalArgumentException("Input plugin is not upgradable.");
            }
        }
    }

    /**
     * @param p A plugin to extract version from.
     * @return String representation of the plugin version.
     */
    public static String getVersion(final Plugin p) {
        final PluginInfo conf = p.getConfiguration();
        if (conf.getVersionMinor() < 0) {
            return Integer.toString(conf.getVersionMajor());
        } else {
            return new StringBuilder().append(conf.getVersionMajor()).append('.').append(conf.getVersionMinor()).toString();
        }
    }

    public static class ModuleVersion implements Comparable<PluginInfo> {

        final String stringVersion;
        final int[] version;

        public ModuleVersion(String moduleVersion) {
            stringVersion = moduleVersion == null? "" : moduleVersion;

            String[] splitted = stringVersion.split("[^\\d]+", 3);
            if (splitted.length < 2) {
                version = new int[0];
            } else {
                version = new int[]{Integer.parseInt(splitted[0]), Integer.parseInt(splitted[1])};
            }
        }

        @Override
        public int compareTo(PluginInfo o) {
            if (version.length < 2) {
                return stringVersion.compareTo(new StringBuilder(o.getVersionMajor()).append('.').append(o.getVersionMinor()).toString());
            } else {
                final int majorComp = version[0] - o.getVersionMajor();
                return majorComp == 0 ? version[1] - o.getVersionMinor() : majorComp;
            }
        }

        @Override
        public String toString() {
            return stringVersion;
        }
    }

    private boolean isAdmin() {
        final TextField userInput = new TextField();
        final PasswordField passInput = new PasswordField();
        final boolean accepted = SIRS.fxRunAndWait(() -> {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.CANCEL, ButtonType.OK);
            alert.setHeaderText("Veuillez confirmer vos droit d'administration avant la migration :");

            final GridPane gPane = new GridPane();
            gPane.add(new Label("Login : "), 0, 0);
            gPane.add(userInput, 1, 0);
            gPane.add(new Label("Mot de passe : "), 0, 1);
            gPane.add(passInput, 1, 1);

            alert.getDialogPane().setContent(gPane);
            alert.setWidth(400);
            alert.setHeight(400);
            alert.setResizable(true);
            return ButtonType.OK.equals(alert.showAndWait().orElse(null));
        });

        if (accepted) {
            final String username = userInput.textProperty().getValueSafe();
            final String userpass = passInput.textProperty().getValueSafe();
            return UtilisateurRepository.isAdmin(connector, username, userpass);
        }
        return false;
    }
}
