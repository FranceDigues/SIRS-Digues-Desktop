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
package fr.sirs;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.core.model.Element;
import fr.sirs.map.FXMapPane;
import fr.sirs.theme.Theme;
import fr.sirs.util.FXFreeTab;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.map.MapItem;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Un plugin est un ensemble de thèmes et de couches de données cartographique.
 * - Les thèmes se retrouvent dans les menus de la barre d'outil principale de l'application.
 * - Les couches cartographiques seront ajoutées dans la vue cartographique.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class Plugin {

    public static String PLUGIN_FLAG = "pluginSirs";

    protected String name;
    /** Message affiché lors du chargement du plugin */
    protected final SimpleStringProperty loadingMessage = new SimpleStringProperty("");
    /** Liste des themes géré par le plugin */
    protected final List<Theme> themes = new ArrayList<>();

    /**
     * Récupérer la session SIRS en cours.
     *
     * @return Session, jamais nulle
     */
    public Session getSession() {
        return Injector.getBean(Session.class);
    }

    /**
     * Récupérer la liste des couches de données à ajouter dans la vue
     * cartographique.
     *
     * @return Liste de couches cartographique, jamais nulle
     */
    public List<MapItem> getMapItems() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Message affiché lors du chargement du plugin.
     *
     * @return SimpleStringProperty, jamais nulle
     */
    public final ReadOnlyStringProperty getLoadingMessage() {
        return loadingMessage;
    }

    /**
     * Liste des themes géré par le plugin.
     *
     * @return Liste de Theme, jamais nulle
     */
    public List<Theme> getThemes() {
        return themes;
    }

    /**
     * Récupère les actions disponibles pour un object selectionné sur la carte.
     *
     * @param candidate objet selectionné
     * @return Liste d'action possible, jamais nulle
     */
    public List<MenuItem> getMapActions(Object candidate) {
        return Collections.EMPTY_LIST;
    }

    /**
     * Barre d'outils complémentaire pour la carte.
     *
     * @param mapPane Panneau de la carte.
     * @return list, peut etre null
     */
    public List<ToolBar> getMapToolBars(final FXMapPane mapPane){
        return null;
    }

    /**
     * Récupère le titre du plugin.
     *
     * @return Le titre du plugin, jamais nulle ou vide.
     */
    public abstract CharSequence getTitle();

    /**
     * Renvoie l'image du plugin, si une image a été fournie. Peut être {@code null}.
     * @return Une image à afficher dans le menu de sélection des modules, ou nulle.
     */
    public abstract Image getImage();

    /**
     * Renvoie une éventuelle représentation du modèle du module sous forme d'image.
     *
     * @return Un optional contenant l'image éventuelle du modèle du module.
     *
     * @throws java.io.IOException Si le chargement de l'image échoue.
     */
    public Optional<Image> getModelImage() throws IOException {
        return Optional.empty();
    }

    /**
     * Chargement du plugin.
     * Cette méthode est appelée lorsque l'on se connecte à une base de données
     * et que le le contexte de travail est mis en place.
     * Il est recommandé de remplir et de mettre à jour la valeur de 'loadingMessage'
     * au cours du chargement.
     *
     * @throws java.lang.Exception : en cas d'erreur de chargement du plugin
     */
    public abstract void load() throws Exception;

    /**
     * Opérations à effectuer après importation. Il s'agit par exemple de la
     * génération des vues.
     *
     * Par défaut, on ne fait rien.
     *
     * Note : lors de l'appel à cette méthode, la fonction {@link #load() } n'a
     * pas encore été appelée !
     *
     * @throws Exception Si l'opération éxécutée a échouée.
     */
    public void afterImport() throws Exception {}

    /**
     * This method declares the plugin is able to display the type of TronconDigue
     * given as a parameter, using openTronconPane() method.
     *
     * @param tronconType Type of element to check.
     * @return True if the plugin provide a special management for this type of object.
     */
    public boolean handleTronconType(final Class<? extends Element> tronconType){
        return false;
    }

    /**
     * This method opens a pane for the TronconDigue given as a parameter. It
     * garantees to open the right pane if and only if the method
     * handleTronconType() had returned "true" for the runtime type of the
     * troncon.
     *
     * Note if handleTronconType had returned "false", this method result is
     * undefined. The default implementation returns null, but other
     * redefinitions could return a pane without error but which doesn't exactly
     * match the runtime type of the given TronconDigue.
     *
     * For instance, it may be possible to open a Berge (which inherits
     * TronconDigue) using openTronconPane() of the CorePlugin, which is
     * designed for TronconDigue. To avoid this "inconsistent" case,
     * handleTronconType() of the CorePlugin must return false for Berge class.
     *
     * @param element Object to get an editor for.
     * @return A {@link Tab} to add in main frame.
     */
    public FXFreeTab openTronconPane(final Element element){
        return null;
    }

    /**
     * Cherche une configuration valide pour le plugin courant. Par défaut, la
     * méthode cherche un JSON descriptif dans le dossier des plugins. Si aucun
     * fichier ne peut être utilisé, on essaie de construire un descriptif grâce
     * aux informations de la classe Java.
     * @return Les informations générales du module (version, description, etc.)
     */
    public PluginInfo getConfiguration() {
        final String pluginName = name == null? this.getClass().getSimpleName() : name;
        final Path pluginPath = SirsCore.PLUGINS_PATH.resolve(pluginName);
        final Pattern jsonPattern = Pattern.compile("(?i)" + pluginName + ".*(\\.json)$");
        try {
            Optional<Path> pluginDescriptor = Files.walk(pluginPath, 1)
                    .filter((Path p) -> jsonPattern.matcher(p.getFileName().toString()).matches())
                    .findAny();
            if (pluginDescriptor.isPresent()) {
                return new ObjectMapper().readValue(pluginDescriptor.get().toFile(), PluginInfo.class);
            }
        } catch (IOException e) {
            SirsCore.LOGGER.log(Level.FINE, "Plugin "+name +" has not any json descriptor.", e);
        }

        final PluginInfo info = new PluginInfo();
        info.setName(pluginName);

        final Matcher versionMatcher;
        final String jarVersion = this.getClass().getPackage().getImplementationVersion();
        if (jarVersion == null || jarVersion.isEmpty()) {
            final String jarLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm();
            versionMatcher = Pattern.compile("(\\d+)\\.(\\d+)\\.jar$").matcher(jarLocation);
        } else {
            versionMatcher = Pattern.compile("(\\d+)\\.(\\d+)").matcher(jarVersion);
        }
        if (versionMatcher.find()) {
            info.setVersionMajor(Integer.parseInt(versionMatcher.group(1)));
            info.setVersionMinor(Integer.parseInt(versionMatcher.group(2)));
        }
        return info;
    }

    /**
     * Override this method to provide a migration process from a given version
     * of the plugin to the current one.Notes : this method is called only when the current plugin version is
 newer than the one given in database.It is called before {@link #load()}.
     *
     * The returned task should not be submitted yet. Application will do it
 after user has confirmed his will to upgrade.
     *
     * @param fromMajor Major version of the plugin found in database.
     * @param fromMinor Minor version of the plugin found in database.
     * @param dbConnector Connector to the database to upgrade.
     * @param upgradeTasks LinkedHashSet of Tasks to be filled with needed tasks
     * which will perform updates (database objects, etc.)
     * @param dbRegistry optional parameter which will be used if an
     * {@linkplain ConfigurableApplicationContext application context}
     * is needed, should with length {@literal <=} 1.
     *
     * -> no more valid comments let if we want to change the returned type.
     * //true if input upgradeTasks was filled with ready to be submitted tasks, which will perform updates (database objects, etc.)
     * //to make database and application ready to work with current plugin version.
     */
    public void findUpgradeTasks(final int fromMajor, final int fromMinor, final CouchDbConnector dbConnector, final LinkedHashSet<Task> upgradeTasks, final DatabaseRegistry... dbRegistry) {
    }

    public enum MigrationNames {
        dependanceAddAH("plugin-dependance","plugin-dependance-ah"),
        desordreAddAH("Désordre (dépendance)", "Désordre (dépendance et AH)");

        private final String oldName;
        private final String newName;

        MigrationNames (String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }

        public String getOldName() {
            return oldName;
        }

        public String getNewName() {
            return newName;
        }

        public static String migrate(String name) {
            for (Plugin.MigrationNames migName : MigrationNames.values()) {
                if (migName.getOldName().equalsIgnoreCase(name)) return migName.getNewName();
            }
            return name;
        }
    }
}