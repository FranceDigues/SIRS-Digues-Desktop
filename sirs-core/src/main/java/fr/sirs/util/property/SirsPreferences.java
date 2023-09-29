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

import static fr.sirs.core.SirsCore.CONFIGURATION_PATH;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Définit les préférences liées à l'installation locale de l'application.
 *
 * @author Alexis Manin (Geomatys)
 */
public class SirsPreferences extends Properties {

    private static final Path PREFERENCES_PATH = CONFIGURATION_PATH.resolve("preferences.properties");
    private static final String COMMENTS = null;

    public enum PROPERTIES {
        REFERENCE_URL("Adresse des références", "Url à laquelle se trouvent les différents fichiers centralisés des références de l'application.", "https://sirs-digues.info/wp-content/tablesReferences/"),
        PREPROGRAMMED_QUERIES_URL("Adresse des requêtes préprogrammées", "Url du fichier des requêtes préprogrammées.", "https://sirs-digues.info/wp-content/requetesPreprogrammees/preprogrammedQueries.properties"),
        UPDATE_CORE_URL("Mise à jour de l'application", "Url à laquelle se trouve le service de mise à jour de l'application.", "https://sirs-digues.info/wp-content/updates/core.json"),
        UPDATE_PLUGINS_URL("Mise à jour des plugins", "Url à laquelle se trouve le service de mise à jour des plugins.", "https://sirs-digues.info/wp-content/updates/plugins.json"),
        COUCHDB_LOCAL_ADDR("Addresse de la base CouchDB locale", "Addresse d'accès à la base CouchDB locale, pour les réplications sur le poste.", "http://127.0.0.1:5984/"),
        /*
        On initialise par défaut le nom du nœud local à nonode@nohost, nom qui semble utilisé par défaut par couchDB.
        Mais cela peut provoquer des erreurs lorsqu'on s'adresse à un nœud qui porte un autre nom (cela arrive par exemple avec le serveur distant de Geomatys
        dont le nœud se nomme : couchdb@couchdb-couchdb-0.couchdb-couchdb.demos.svc.cluster.local
        Or, http://docs.couchdb.org/en/stable/api/server/configuration.html précise que le nom du nœud local peut être
        remplacé de façon générique par "_local"
        NODE_NAME("Nom du nœud de l'instance CouchDB", "Nom du nœud utilisé pour la configuration dans l'instance CouchDB.", "nonode@nohost"),
        */
        NODE_NAME("Nom du nœud de l'instance CouchDB", "Nom du nœud utilisé pour la configuration dans l'instance CouchDB.", "_local"),
        CHECK_COUCHDB_VERSION("Vérifie la version de CouchDB", "Permet de vérifier ou d'ignorer la version de CouchDB au lancement de l'application.", Boolean.TRUE.toString()),
        DESIGNATION_AUTO_INCREMENT("Auto-incrément des désignations", "Lorsqu'un nouvel élément sera créé, sa désignation sera automatiquement remplie avec une valeur numérique"
                + " déterminée à partir de l'objet du même type ayant une désignation de forme numérique la plus haute trouvée dans la base de données, + 1.", Boolean.FALSE.toString()),
        HIDE_ARCHIVED_PARENT("Masquer les tronçons, lits, berges, Dépendances et AHs archivés", "Par défaut les éléments archivés sont masqués. \n" +
                "Lorsque cette option est désactivée, les éléments archivés seront visibles dans :\n" +
                "   - les bandeaux de sélection, \n" +
                "   - l'onglet Tronçon des fiches d'impression,\n" +
                "   - l'onglet Tronçons des AHs.",
                Boolean.TRUE.toString()),
        ABSTRACT_SHOWCASE("Préférence pour la désignation des objets de l'application","Choix entre abrégé, nom complet ou les 2 pour la désignation des objet dans l'application.",
           ShowCasePossibility.BOTH.name),
        /*
        Préférences utilisés pour paramétrer le fond de carte par défaut.
        */
        BASEMAP_WM_URL("Adresse WebMap du fond de carte", "Url du fond de carte, provenant d'un service WMS/WMTS, utilisée par défaut au démarage du SIRS.", ""),
        BASEMAP_WM_TYPE("Type du service WMS/WMTS utilisé", "Type du service WMS/WMTS utilisé pour le chargement du fond de carte par défaut", "WMS - 1.3.0"),
        BASEMAP_OSM_TILE_URL("Adresse de tuile OSM du fond de carte", "Url du fond de carte, provenant d'un service tuile Open Street Map, utilisée par défaut au démarage du SIRS.", ""),
        BASEMAP_LOCAL_FILE("Chemin du fond de carte", "Chemin vers le fond de carte, provenant d'un fichier local, utilisée par défaut au démarage du SIRS.", ""),
        BASEMAP_FILE_TYPE("Type du fichier local", "Type du fichier local utilisé pour le chargement de fond de carte par défaut", "File coverage"),
        BASEMAP_CHOICE("Choix du type de fond de carte", "Choix du type de fond de carte: wms/wmts, OSMTileMap, fichier ou defaut", "defaut");
        public final String title;
        public final String description;
        public final String defaultValue;
        PROPERTIES(final String title, final String description, final String defaultValue) {
            this.title = title;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue(){return defaultValue;}
    }

    /**
     * L'instance unique à utiliser pour travailler avec les propriétés.
     */
    public static final SirsPreferences INSTANCE;
    static {
        try {
            INSTANCE = new SirsPreferences();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final SimpleObjectProperty<ShowCasePossibility> showCaseProp;

    /**
     * Charge les préférences depuis le système.
     * @throws IOException Si on échoue à lire ou créer le fichier contenant les propriétés.
     */
    private SirsPreferences() throws IOException {
        super();

        if (!Files.isRegularFile(PREFERENCES_PATH)) {
            Files.createFile(PREFERENCES_PATH);
        }
        showCaseProp = new SimpleObjectProperty(ShowCasePossibility.BOTH);
        reload();
    }

    public SimpleObjectProperty<ShowCasePossibility> showCasePropProperty() {
        return showCaseProp;
    }

    public ShowCasePossibility getShowCase() {
        return showCaseProp.get();
    }

    public void setShowCase(final ShowCasePossibility showCase) {
        showCaseProp.setValue(showCase);
    }

    /**
     * Retourne la valeur de la propriété indiquée en paramètre, ou, en son absence, sa valeur par défaut.
     * @param property propriété
     * @return valeur de la propriété si elle existe ou valeur par défaut dans le cas contraire.
     */
    public String getPropertySafeOrDefault(SirsPreferences.PROPERTIES property){
        if(SirsPreferences.INSTANCE.getPropertySafe(property)!=null){
            return SirsPreferences.INSTANCE.getPropertySafe(property);
        } else {
            return property.getDefaultValue();
        }
    }

    /**
     * Recharge les propriétés depuis le disque.
     * @throws IOException Si on échoue à lire le fichier contenant les propriétés.
     */
    private void reload() throws IOException {
        try (final InputStream stream = Files.newInputStream(PREFERENCES_PATH, StandardOpenOption.READ)) {
            this.load(stream);
        }

        for (final PROPERTIES prop : PROPERTIES.values()) {
            try {
                if(prop == PROPERTIES.ABSTRACT_SHOWCASE) {
                    setShowCase(ShowCasePossibility.getFromName(getProperty(prop)));
                } else {
                    getProperty(prop);
                }
            } catch (IllegalStateException e) {
                if (prop.getDefaultValue() != null) {
                    setProperty(prop.name(), prop.getDefaultValue());
                }
            }
        }
    }

    /**
     * Ecris les propriétés courantes sur le disque.
     * @throws IOException Si on échoue à écrire le fichier contenant les propriétés.
     */
    public void store() throws IOException {
        try (OutputStream propertyFile = Files.newOutputStream(PREFERENCES_PATH)) {
            store(propertyFile, COMMENTS);
        }
    }

    /**
     * Enregistre les préferences données dans le fichier de propriétés lié.
     *
     * @param values Une table dont les clés sont les propriétés à mettre à jour,
     * accompagnées de leur valeur.
     * @throws IOException Si une erreur survient lors de la persistence des prpriétés.
     */
    public void store(final Map<PROPERTIES, String> values) throws IOException {
        for (final Map.Entry<PROPERTIES, String> entry : values.entrySet()) {
            setProperty(entry.getKey().name(), entry.getValue());
        }

        store();
        reload();
    }

    /**
     * Override {@link java.util.Properties#getProperty(String)} method to forbid returning null or empty value.
     * @param key key of the value to retrieve.
     * @return The value stored for queried property. Never null or empty.
     * @throws java.lang.IllegalStateException if queried property has no value.
     */
    @Override
    public String getProperty(String key) throws IllegalStateException {
        final String property = super.getProperty(key);
        if (property == null || property.isEmpty()) {
            throw new IllegalStateException("No valid "+key+ " property defined in "+PREFERENCES_PATH);
        } else {
            return property;
        }
    }

    /**
     * Override {@linkplain java.util.Properties#getProperty(String)} method to forbid returning null or empty value.
     * @param key key for the value to retrieve.
     * @return The value stored for queried property. Never null or empty.
     * @throws java.lang.IllegalStateException if queried property has no value.
     */
    public String getProperty(PROPERTIES key) throws IllegalStateException {
        return getProperty(key.name());
    }

    /**
     * @param key key of the value to retrieve.
     * @return The value stored for queried property. Can be null or empty.
     */
    public String getPropertySafe(String key) {
        return super.getProperty(key);
    }

    /**
     * @param key key for the value to retrieve.
     * @return The value stored for queried property. Can be null or empty
     */
    public String getPropertySafe(PROPERTIES key) {
        return getPropertySafe(key.name());
    }

    public static boolean getHideArchivedProperty() {
        return Boolean.parseBoolean(INSTANCE.getProperty(SirsPreferences.PROPERTIES.HIDE_ARCHIVED_PARENT));
    }
}
