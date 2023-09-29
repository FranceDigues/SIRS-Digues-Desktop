/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.PlatformUtil;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.util.referencing.HackCRSFactory;
import fr.sirs.util.property.DocumentRoots;
import fr.sirs.util.property.Internal;
import fr.sirs.util.property.SirsPreferences;
import org.apache.sis.referencing.operation.HackCoordinateOperationFactory;
import java.awt.Desktop;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import fr.sirs.ui.ConfFolderPane;

import org.geotoolkit.gui.javafx.util.TaskManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javax.measure.Unit;
import javax.naming.NamingException;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;

import org.apache.sis.util.logging.Logging;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.internal.io.JNDI;
import org.geotoolkit.internal.referencing.CRSUtilities;
import org.geotoolkit.lang.Setup;
import org.hsqldb.jdbc.JDBCDataSource;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.util.FactoryException;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.apache.commons.io.FileUtils;


public class SirsCore {

    public static final ZoneId PARIS_ZONE_ID = ZoneId.of("Europe/Paris");

    private static final String PROJ4_RESOURCE = "/fr/sirs/core/proj4.json";

    /**
     * An useful variable which specify how many meters an object length should be
     * to be considered as a linear object.
     */
    public static final double LINE_MIN_LENGTH = 1.0;

    public static final String PASSWORD_ENCRYPT_ALGO="MD5";

    public static final String INFO_DOCUMENT_ID = "$sirs";
    public static final Logger LOGGER;
    static {
        LOGGER = Logging.getLogger("fr.sirs");
        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
            SLF4JBridgeHandler.install();
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.SEVERE, "Cannot initialize log dumping (logbak)", e);
            // We allow starting program without log writing.
        }
    }

    public static final String NAME = "sirs";

    public static final String MODEL_PACKAGE="fr.sirs.core.model";
    public static final String COMPONENT_PACKAGE="fr.sirs.core.component";

    public static final String SPRING_CONTEXT = "classpath:/fr/sirs/spring/application-context.xml";

    public static final Path CONFIGURATION_PATH;
    static {
        CONFIGURATION_PATH = SirsCore.initConfigurationFolderPath();
    }

    public static final Path DATABASE_PATH = CONFIGURATION_PATH.resolve("database");

    public static final Path H2_PATH = CONFIGURATION_PATH.resolve("h2");

    public static final Path PLUGINS_PATH = CONFIGURATION_PATH.resolve("plugins");

    public static final Path EPSG_PATH = CONFIGURATION_PATH.resolve("EPSG");

    public static final Path ELASTIC_SEARCH_PATH = CONFIGURATION_PATH.resolve("elasticSearch");

    public static final Path LOCAL_QUERIES_PATH = CONFIGURATION_PATH.resolve("queries.properties");

    /**
     * Les requêtes préprogrammées de référence sont désormatis enregistrées sur le serveur de France-Digues et sont
     * téléchargées à chaque nouvelle connexion lorsque le réseau est disponible.
     * <p>
     * Mais afin que les requêtes préprogrammées soient utilisables même hors-ligne (du moins dans leur dernière version
     * connue), ce n'est pas le fichier distant qui est directement utilisé pour la lecture et l'exécution des
     * requêtes, mais une copie locale mise à jour à chaque nouvelle connexion avec le contenu du fichier distant.
     */
    public static final Path PREPROGRAMMED_QUERIES_PATH = CONFIGURATION_PATH.resolve("preprogrammedQueries.properties");

    public static final Path IMPORT_ERROR_DIR = CONFIGURATION_PATH.resolve("importErrors");

    public static final Path NTV2_DIR = CONFIGURATION_PATH.resolve("ntv2");
    public static final List<String> NTV2_FILES = Collections.unmodifiableList(Arrays.asList(new String[]{
        "gr3df97a.txt",
        "rgf93_ntf.gsb"
    }));

    //--------------------------------------------------------------------------
    // Champs spéciaux des classes utilisés dans le code
    //--------------------------------------------------------------------------
    public static final String NEW_FIELD = "new";

    // Champs de contrôle des dates
    public static final String DATE_DEBUT_FIELD = "date_debut";
    public static final String DATE_FIN_FIELD = "date_fin";
    public static final String DATE_MAJ_FIELD = "dateMaj";

    public static final String BORNE_DEBUT_AVAL = "borne_debut_aval";
    public static final String BORNE_FIN_AVAL = "borne_fin_aval";

    public static final String BORNE_DEBUT_DISTANCE = "borne_debut_distance";  //Doit correspondre au nom de la Propriété de la classe Positionable
    public static final String BORNE_FIN_DISTANCE = "borne_fin_distance";  //Doit correspondre au nom de la Propriété de la classe Positionable


    public static final String BORNE_DEBUT_ID = "borneDebutId";  //Doit correspondre au nom de la Propriété de la classe Positionable
    public static final String BORNE_FIN_ID = "borneFinId";  //Doit correspondre au nom de la Propriété de la classe Positionable

    public static final String DISTANCE_DEBUT_MIN = "distanceDebutMin"; // PositionableVegetation property
    public static final String DISTANCE_DEBUT_MAX = "distanceDebutMax"; // PositionableVegetation property
    public static final String DISTANCE_FIN_MIN = "distanceFinMin"; // PositionableVegetation property
    public static final String DISTANCE_FIN_MAX = "distanceFinMax"; // PositionableVegetation property

    public static final String COMMENTAIRE_FIELD = "commentaire";
    public static final String GEOMETRY_FIELD = "geometry";
    public static final String GEOMETRY_MODE_FIELD = "geometryMode";

    public static final String DESIGNATION_FIELD = "designation";
    public static final String VALID_FIELD = "valid";
    public static final String AUTHOR_FIELD = "author";

    public static final String DOCUMENT_ID_FIELD = "documentId";
    public static final String ID_FIELD = "id";
    public static final String REVISION_FIELD = "revision";
    public static final String PARENT_FIELD = "parent";
    public static final String COUCH_DB_DOCUMENT_FIELD = "couchDBDocument";

    public static final String LINEAR_ID_FIELD = "linearId";
    public static final String DIGUE_ID_FIELD = "digueId";
    public static final String FOREIGN_PARENT_ID_FIELD = "foreignParentId";

    public static final String PR_DEBUT_FIELD = "prDebut";
    public static final String PR_FIN_FIELD = "prFin";
    public static final String POSITION_DEBUT_FIELD = "positionDebut";
    public static final String POSITION_FIN_FIELD = "positionFin";
    public static final String LONGITUDE_MIN_FIELD = "longitudeMin";
    public static final String LONGITUDE_MAX_FIELD = "longitudeMax";
    public static final String LATITUDE_MIN_FIELD = "latitudeMin";
    public static final String LATITUDE_MAX_FIELD = "latitudeMax";

    public static final String SIRSDOCUMENT_REFERENCE = "sirsdocument";
    public static final String BORNE_IDS_REFERENCE = "borneIds";

    /** Dénomination du système de repérage élémentaire. */
    public static final String SR_ELEMENTAIRE = "Elémentaire";

    /** Libellé de la borne de départ du SR élémentaire. */
    public static final String SR_ELEMENTAIRE_START_BORNE = "Début du tronçon";

    /** Libellé de la borne de fin du SR élémentaire. */
    public static final String SR_ELEMENTAIRE_END_BORNE = "Fin du tronçon";

    /** Timestamp specific properties' names **/
    public static final String TIMESTAMP_START_DATE_START_VALIDITY = "horodatageDateDebutStart";
    public static final String TIMESTAMP_END_DATE_START_VALIDITY = "horodatageDateDebutEnd";
    public static final String TIMESTAMP_END_DATE_END_VALIDITY = "horodatageDateFinEnd";
    public static final String TIMESTAMP_START_DATE = "horodatageStartDate";
    public static final String TIMESTAMP_END_DATE = "horodatageEndDate";

    //--------------------------------------------------------------------------
    // Champs particuliers aux désordres
    //--------------------------------------------------------------------------
    public static class DesordreFields {
        // Observations des désordres
        public static final String OBSERVATIONS_REFERENCE = "observations";

        // Photos des observations
        public static final String PHOTOS_OBSERVATION_REFERENCE = "photos";

        // Réseaux et ouvrages
        public static final String ECHELLE_LIMINIMETRIQUE_REFERENCE = "echelleLimnimetriqueIds";
        public static final String OUVRAGE_PARTICULIER_REFERENCE = "ouvrageParticulierIds";
        public static final String RESEAU_TELECOM_ENERGIE_REFERENCE = "reseauTelecomEnergieIds";
        public static final String OUVRAGE_TELECOM_ENERGIE_REFERENCE = "ouvrageTelecomEnergieIds";
        public static final String OUVRAGE_HYDRAULIQUE_REFERENCE = "ouvrageHydrauliqueAssocieIds";
        public static final String RESEAU_HYDRAULIQUE_FERME_REFERENCE = "reseauHydrauliqueFermeIds";
        public static final String RESEAU_HYDRAULIQUE_CIEL_OUVERT_REFERENCE = "reseauHydrauliqueCielOuvertIds";

        // Voiries
        public static final String OUVRAGE_VOIRIE_REFERENCE = "ouvrageVoirieIds";
        public static final String VOIE_DIGUE_REFERENCE = "voieDigueIds";

        // Prestations
        public static final String PRESTATION_REFERENCE = "prestationIds";


        public static final String ARTICLE_REFERENCE = "articleIds";
    }

    // Champs spéciaux des ResourceBundles
    public static final String BUNDLE_KEY_CLASS = "class";
    public static final String BUNDLE_KEY_CLASS_ABREGE = "classAbrege";

    // Bundle des previews
    public static final String PREVIEW_BUNDLE_KEY_DOC_ID = "docId";
    public static final String PREVIEW_BUNDLE_KEY_DOC_CLASS = "docClass";
    public static final String PREVIEW_BUNDLE_KEY_ELEMENT_ID = "elementId";
    public static final String PREVIEW_BUNDLE_KEY_ELEMENT_CLASS = "elementClass";
    public static final String PREVIEW_BUNDLE_KEY_LIBELLE = "libelle";
    public static final String PREVIEW_BUNDLE_KEY_DESIGNATION = "designation";
    public static final String PREVIEW_BUNDLE_KEY_AUTHOR = "author";

    // Méthodes utilisées pour les références
    public static final String REFERENCE_GET_ID = "getId";
    public static final String REFERENCE_SET_DESIGNATION = "setDesignation";

    protected static final String NTV2_RESOURCE = "/fr/sirs/ntv2/";

    public static final int GEO_CLIENT_TIMEOUT = 60000;

    public static final String REGISTRE_THEME_NAME = "Registre";

    /**
     * Configuration directory root folder.
     *
     * @return le chemin vers le dossier de configuration .sirs
     */
    public static String getConfigPath(){
        return CONFIGURATION_PATH.toString();
    }

    public static File getConfigFolder(){
        return CONFIGURATION_PATH.toFile();
    }

    public static Path getDatabaseFolder(){
        return DATABASE_PATH;
    }

    /**
     * Initialise la base EPSG et la grille NTV2 utilisée par l'application. Si
     * elles n'existent pas, elles seront créées. Dans tous les cas, on force le
     * chargement de la base EPSG dans le système, ce qui permet de lever les
     * potentiels problèmes au démarrage.
     * Si la création de la bdd EPSG rate, on renvoie une exception, car aucun
     * réferencement spatial ne peut être effecctué sans elle. En revanche, la
     * grille NTV2 n'est utile que pour des besoins de précision. Si son installation
     * rate, on n'afficche juste un message d'avertissement.
     * @throws FactoryException Si aucun driver n'est trouvé pour la connection
     * à la base de données EPSG.
     * @throws IOException Si une erreur survient pendant la création / connexion
     * à la base de données.
     * @throws javax.naming.NamingException Si il est impossible d'enregistrer
     * la bdd EPSG auprès de JNDI.
     */
    public static void initEpsgDB() throws FactoryException, IOException, NamingException {
        // create a database in user directory
        Files.createDirectories(SirsCore.EPSG_PATH);

        // Geotoolkit startup. We specify that we don't want to use Java preferences,
        // because it does not work on most systems anyway.
        final Properties noJavaPrefs = new Properties();
        noJavaPrefs.put("platform", "server");
        Setup.initialize(noJavaPrefs);

        final String url = "jdbc:hsqldb:" + SirsCore.EPSG_PATH.resolve("db").toUri();
        final String decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
        final JDBCDataSource ds = new JDBCDataSource();
        ds.setDatabase(decodedUrl);
        JNDI.setEPSG(ds);

        // On tente d'installer la grille NTV2 pour améliorer la précision du géo-réferencement.
        try {
            Files.createDirectories(NTV2_DIR);
            for (final String file : NTV2_FILES) {
                final Path ntv2File = NTV2_DIR.resolve(file);
                if (Files.exists(ntv2File))
                    continue;
                try (final InputStream resource = SirsCore.class.getResourceAsStream(NTV2_RESOURCE.concat(file))) {
                    Files.copy(resource, ntv2File);
                }
            }

            Field dirField = DataDirectory.class.getDeclaredField("directory");
            dirField.setAccessible(true);
            dirField.set(DataDirectory.DATUM_CHANGES, NTV2_DIR);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "NTV2 data for RGF93 cannot be installed.", ex);
            Platform.runLater(()
                    -> GeotkFX.newExceptionDialog("La grille de transformation NTV2 ne peut être installée. Des erreurs de reprojection pourraient apparaître au sein de l'application.", ex).show()
            );
        }

        try {
            // HACK : replace default CRS factories with our own one, to manage cases
            // where ESRI data define bad CRS WKT.
            final Field field = DefaultFactories.class.getDeclaredField("FACTORIES");
            field.setAccessible(true);
            // No class check, we want it to crash if not what expected.
            final Map factories = (Map)field.get(null);

            final HackCRSFactory hackCRSFactory = new HackCRSFactory();
            factories.put(CRSFactory.class, hackCRSFactory);
            if (!DefaultFactories.isDefaultInstance(CRSFactory.class, hackCRSFactory)) {
                throw new IllegalStateException("Submitted CRS factory is not the default one !");
            }

            final HackCoordinateOperationFactory hcoFactory = new HackCoordinateOperationFactory();
            factories.put(CoordinateOperationFactory.class, hcoFactory);
            if (!DefaultFactories.isDefaultInstance(CoordinateOperationFactory.class, hcoFactory)) {
                throw new IllegalStateException("Submitted CRS factory is not the default one !");
            }

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Hack on CRS factory cannot be applied !", ex);
        }

        // force loading epsg
        CRS.forCode("EPSG:3395");
    }

    public static String getVersion() {
        return SirsCore.class.getPackage().getImplementationVersion();
    }

    /**
     * Récupère le chemin absolu vers le fichier référencé par l'objet en entrée.
     *
     * @param ref Un objet faisant réference à un document.
     * @return Un chemin absolu vers la réference passée en paramètre.
     * @throws IllegalStateException Si aucune racine n'est configurée ou ne dénote un chemin valide.
     * Dans ce cas, il est FORTEMENT conseillé d'attraper l'exception, et de proposer à l'utilisateur de vérifier la valeur de cette propriété dans les
     * préférences de l'application.
     * @throws InvalidPathException Si il est impossible de construire un chemin valide avec le paramètre d'entrée.
     *
     * Note : les deux exceptions ci-dessus ne sont pas lancées dans le cas où le
     * chemin créé dénote un fichier inexistant. Elles sont invoquées uniquement
     * si les chemins sont incorrects syntaxiquement.
     */
    public static Path getDocumentAbsolutePath(final SIRSFileReference ref) throws IllegalStateException, InvalidPathException {
        ArgumentChecks.ensureNonEmpty("Document relative path", ref.getChemin());
        final Optional<Path> docRoot = DocumentRoots.getRoot(ref);
        if (!docRoot.isPresent()) {
            throw new IllegalStateException("Aucun dossier racine n'existe ou "
                    + "ne dénote un chemin valide. Vous pouvez vérifier sa valeur "
                    + "depuis les préférences de l'application (Fichier > Preferences).");
        }

        return concatenatePaths(docRoot.get(), ref.getChemin());
    }

    /**
     * Résout les chemins donnés comme le ferait la méthode {@link Path#resolve(java.lang.String) },
     * mais applique un pré-traitement pour s'assurer que les chemins donnés utilisent les mêmes
     * séparateurs.
     * @param first Le chemin racine pour la résolution.
     * @param more Les chemins enfants à résoudre.
     * @return Un chemin correspondant à la concaténation des chemins donnés.
     */
    public static Path concatenatePaths(final Path first, final String... more) {
        if (more.length <= 0)
            return first;

        Path builtPath = first;
        for (final String str : more) {
            if (str != null && !str.isEmpty()) {
                if (PlatformUtil.isWindows()) {
                    builtPath = builtPath.resolve(str.replaceAll("/+", "\\\\") // On remplace les séparateurs issus d'un autre système.
                            .replaceFirst("^\\\\+", "")); // On enlève tout séparateur en début de chaîne qui tendrait à signifier que le chemin n'est pas relatif.
                } else {
                    builtPath = builtPath.resolve(str.replaceAll("\\\\+", File.separator) // On remplace les séparateurs issus d'un autre système.
                            .replaceFirst("^" + File.separator + "+", "")); // On enlève tout séparateur en début de chaîne qui tendrait à signifier que le chemin n'est pas relatif.
                }
            }
        }

        return builtPath;
    }

    /**
     *
     * @return the application task manager, designed to start users tasks in a
     * separate thread pool.
     */
    public static TaskManager getTaskManager() {
        return TaskManager.INSTANCE;
    }

    /**
     * Try to expand a little an envelope. Main purpose is to ensure we won't
     * have an envelope which is merely a point.
     * @param input The input to expand.
     * @return An expanded envelope. If we cannot analyze CRS or it's unit on
     * horizontal axis, the same envelope is returned.
     */
    public static Envelope pseudoBuffer(final Envelope env) {
        return pseudoBuffer(env, 1, 0.01);
    }

    public static Envelope pseudoBuffer(final Envelope env, final double distance, double defaultDistance) {
        if (env.getCoordinateReferenceSystem() != null) {
            CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
            int firstAxis = CRSUtilities.firstHorizontalAxis(crs);

            if (firstAxis >=0) {
                Unit unit = crs.getCoordinateSystem().getAxis(firstAxis).getUnit();
                if (unit != null && Units.METRE.isCompatible(unit)) {
                    defaultDistance = Units.METRE.getConverterTo(unit).convert(distance);
                }

                final GeneralEnvelope result = new GeneralEnvelope(env);
                result.setRange(firstAxis,
                        result.getLower(firstAxis) - defaultDistance,
                        result.getUpper(firstAxis) + defaultDistance);
                final int secondAxis = firstAxis + 1;
                result.setRange(secondAxis,
                        result.getLower(secondAxis) - defaultDistance,
                        result.getUpper(secondAxis) + defaultDistance);
                return result;
            }
        }
        return env;
    }

    private static final Class[] SUPPORTED_TYPES = new Class[]{
        Boolean.class,
        String.class,
        Number.class,
        boolean.class,
        int.class,
        float.class,
        double.class,
        LocalDateTime.class,
        LocalDate.class,
        Point.class
    };

    /**
     * Récupération des attributs simple pour affichage dans les tables.
     *
     * @param clazz La classe à analyser
     * @return liste des propriétés simples
     * @throws java.beans.IntrospectionException Si une erreur survient pendant
     * l'analyse.
     */
    public static LinkedHashMap<String, PropertyDescriptor> listSimpleProperties(Class clazz) throws IntrospectionException {
        final LinkedHashMap<String, PropertyDescriptor> properties = new LinkedHashMap<>();
        for (java.beans.PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
            final Method m = pd.getReadMethod();

            if (m == null || m.getAnnotation(Internal.class) != null) {
                continue;
            }

            final Class propClass = m.getReturnType();
            if (propClass.isEnum()) {
                properties.put(pd.getName(), pd);
            } else
                for (Class c : SUPPORTED_TYPES) {
                    if (c.isAssignableFrom(propClass)) {
                        properties.put(pd.getName(), pd);
                        break;
                    }
                }
        }
        return properties;
    }

    /**
     * Utility method which will check for application updates. The check is
     * done in an asynchronous task, to avoid freeze of the application while
     * searching for updates. If an update has been found, result of the task
     * will be information about upgrade, including update package download URL.
     *
     * @return The task which has been submitted for update check.
     */
    public static Task<UpdateInfo> checkUpdate() {
        return TaskManager.INSTANCE.submit("Vérification des mises à jour", () -> {
            final String localVersion = getVersion();
            if (localVersion == null || localVersion.isEmpty())
                throw new IllegalStateException("La version locale de l'application est illisible !");
            final String updateAddr = SirsPreferences.INSTANCE.getPropertySafe(SirsPreferences.PROPERTIES.UPDATE_CORE_URL);
            try {
                final URL updateURL = new URL(updateAddr);
                final Map config = new ObjectMapper().readValue(updateURL, Map.class);
                final Object distantVersion = config.get("version");
                if (distantVersion instanceof String) {
                    boolean updateAvailable = false;
                    try {
                        final Version currentVersion = new Version(localVersion);
                        final Version availableVersion = new Version((String) distantVersion);
                        updateAvailable = availableVersion.compareTo(currentVersion) > 0;
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Cannot determine if an update is available. Bad version syntax ?", e);
                    }
                    if (updateAvailable) {
                        final Object packageUrl = config.get("url");
                        if (packageUrl instanceof String) {
                            return new UpdateInfo(localVersion, (String) distantVersion, new URL((String) packageUrl));
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cannot access update service : " + updateAddr, e);
            }
            return null;
        });
    }

    /**
     * Try to open a system browser to visit given address. If system browser cannot
     * open input URL, we'll try with a web view.
     * @param toBrowse The address of the page to display. If null, the method does nothing.
     */
    public static void browseURL(final URL toBrowse) {
        browseURL(toBrowse, null);
    }

    /**
     * Try to open a system browser to visit given address. If system browser cannot
     * open input URL, we'll try with a web view. A new stage will be opened for
     * the web view, and input title will be set on the stage.
     *
     * @param toBrowse The address of the page to display. If null, the method does nothing.
     * @param title A title set to displayed web view. If null, no title will be set.
     */
    public static void browseURL(final URL toBrowse, final String title) {
        browseURL(toBrowse, title, false);
    }


    /**
     * Try to open a system browser to visit given address. If system browser cannot
     * open input URL, we'll try with a web view. A new stage will be opened for
     * the web view, and input title will be set on the stage.
     *
     * @param toBrowse The address of the page to display. If null, the method does nothing.
     * @param title A title set to displayed web view. If null, no title will be set.
     * @param showAndWait If true, and a webview is opened, application stays locked while its opened. If false,
     * webview will be launched in a non-modal window.
     */
    public static void browseURL(final URL toBrowse, final String title, final boolean showAndWait) {
        if (toBrowse == null)
            return;
        if (Desktop.isDesktopSupported()) {
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(toBrowse.toURI());
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Cannot browse following URL with system browser : "+toBrowse, ex);
                }
            }).start();
        } else {
            final Runnable webUI = () -> {
                final WebView webView = new WebView();
                final Stage infoStage = new Stage();
                infoStage.getIcons().add(new Image(SirsCore.class.getResource("/fr/sirs/icon.png").toString()));
                if (title != null)
                    infoStage.setTitle(title);
                infoStage.setScene(new Scene(webView));
                webView.getEngine().load(toBrowse.toExternalForm());
                if (showAndWait)
                    infoStage.showAndWait();
                else
                    infoStage.show();
            };

            if (Platform.isFxApplicationThread()) {
                webUI.run();
            } else {
                Platform.runLater(webUI);
            }
        }
    }

    /**
     * Encrypte la chaîne de caractères donnée en MD5.
     * @param toEncrypt La chaîne de caractère à encoder.
     * @return La représentation héxadécimale du MD5 créé.
     */
    public static String hexaMD5(final String toEncrypt){
        StringBuilder sb = new StringBuilder();
        try {
            byte[] encrypted = MessageDigest.getInstance(PASSWORD_ENCRYPT_ALGO).digest(toEncrypt.getBytes());
            for (byte b : encrypted) {
                sb.append(String.format("%02X", b));
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new SirsCoreRuntimeException("Cannot hash input password !", ex);
        }
        return sb.toString();
    }

    /**
     * Encrypte la chaîne de caractères donnée en MD5.
     * @param toEncrypt La chaîne de caractère à encoder.
     * @return La représentation binaire du MD5 créé.
     */
    public static String binaryMD5(final String toEncrypt){
        try {
            return new String(MessageDigest.getInstance(PASSWORD_ENCRYPT_ALGO).digest(toEncrypt.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            throw new SirsCoreRuntimeException("Cannot hash input password !", ex);
        }
    }

    /**
     * Simple structure to store information about an update.
     * TODO : add changelog
     */
    public static final class UpdateInfo {
        /** Version of currently installed application. */
        public final String localVersion;
        /** Version of found update. */
        public final String distantVersion;
        /** Download URL for update package. */
        public final URL updateURL;

        public UpdateInfo(String localVersion, String distantVersion, URL updateURL) {
            this.localVersion = localVersion;
            this.distantVersion = distantVersion;
            this.updateURL = updateURL;
        }
    }

    /**
     * Same as {@link FXCollections#observableList(java.util.List) }, but if input
     * is already an observable list, it is returned directly.
     * @param <T> Type of object contained in the list.
     * @param toWrap A list to wrap.
     * @return The input list itself if it's an observable list. An observable view
     * of the list otherwise.
     */
    public static <T> ObservableList<T> observableList(List<T> toWrap) {
        ArgumentChecks.ensureNonNull("List to wrap", toWrap);
        if (toWrap instanceof ObservableList)
            return (ObservableList) toWrap;
        else
            return FXCollections.observableList(toWrap);
    }

    /**
     * Try to build a WKT representation of the {@link CoordinateReferenceSystem}
     * whose code is given as parameter.
     * @param crsCode Identifier of the CRS to get WKT representation for.
     * @return WKT string. Never null. Version 1, using {@link Convention#WKT1_COMMON_UNITS}.
     * @throws org.opengis.util.FactoryException If no driver can be found for EPSG database.
     */
    public static String getWkt1Common(final String crsCode) throws FactoryException {
        final WKTFormat wktFormat = new WKTFormat(null, null);
        wktFormat.setConvention(Convention.WKT1_COMMON_UNITS);
        wktFormat.setIndentation(WKTFormat.SINGLE_LINE);
        return wktFormat.format(CRS.forCode(crsCode));
    }

    /**
     * Search for Proj4 representation of {@link CoordinateReferenceSystem} with
     * given identifier.
     * @param crsCode Identifier of the CRS to get WKT representation for (Mostly EPSG).
     * @return A filled optional if we found a Proj4 string for given identifier, an empty one otherwise.
     * @throws IOException If an error occurs while reading Proj4 resource.
     */
    public static Optional<String> getProj4(final String crsCode) throws IOException {
        try (
                final InputStream stream = SirsCore.class.getResourceAsStream(PROJ4_RESOURCE);
                final JsonParser parser = new JsonFactory().createParser(stream)) {

            boolean found = false;
            JsonToken currentToken;
            while (!found && (currentToken = parser.nextToken()) != null) {
                if (currentToken.id() == JsonToken.FIELD_NAME.id()) {
                    found = crsCode.equalsIgnoreCase(parser.getText());
                }
            }

            if (found) {
                return Optional.of(parser.nextTextValue());
            }
        }

        return Optional.empty();
    }

    /**
     *
     * @return Set of CRS identifier for which application can provide a Proj4
     * string representation.
     * @throws IOException If an error occurs while reading Proj4 resource.
     */
    public static Set<String> getProj4ManagedProjections() throws IOException {
        try (
                final InputStream stream = SirsCore.class.getResourceAsStream(PROJ4_RESOURCE);
                final JsonParser parser = new JsonFactory().createParser(stream)) {

            final HashSet<String> codes = new HashSet<>();
            codes.add("EPSG:3857");// No need for proj4 string for this one. Natively managed by mobile app.
            JsonToken currentToken;
            while ((currentToken = parser.nextToken()) != null) {
                if (currentToken.id() == JsonToken.FIELD_NAME.id()) {
                    codes.add(parser.getText());
                }
            }
            return codes;
        }
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it),
     * and wait for its result before returning.
     *
     * @param <T> Return type of the input task.
     * @param toRun Task to run in JavaFX thread.
     * @return Result of the input task.
     */
    public static <T> T fxRunAndWait(final Callable<T> toRun) {
        return fxRunAndWait(new TaskManager.MockTask<>(toRun));
    }

    /**
     *
     * @param toRun The task to run.
     */
    public static void fxRunAndWait(final Runnable toRun) {
        fxRunAndWait(new TaskManager.MockTask(toRun));
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it),
     * and wait for its result before returning.
     *
     * @param <T> Return type of the input task.
     * @param toRun Task to run in JavaFX thread.
     * @return Result of the input task.
     */
    public static <T> T fxRunAndWait(final Task<T> toRun) {
        return fxRun(true, toRun);
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it).
     * According to input boolean, we will return immediately or wait for the task to
     * be over.
     * @param wait True if we must wait for the task to end before returning, false
     * to return immediately after submission.
     * @param toRun The task to run into JavaFX application thread.
     */
    public static void fxRun(final boolean wait, final Runnable toRun) {
        fxRun(wait, new TaskManager.MockTask(toRun));
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it).
     * According to input boolean, we will return immediately or wait for the task to
     * be over.
     * @param <T> Return type of input task.
     * @param wait True if we must wait for the task to end before returning, false
     * to return immediately after submission.
     * @return The task return value if we must wait, or we're in platform thread. Otherwise null.
     * @param toRun The task to run into JavaFX application thread.
     */
    public static <T> T fxRun(final boolean wait, final Callable<T> toRun) {
        return fxRun(wait, new TaskManager.MockTask<>(toRun));
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it).
     * According to input boolean, we will return immediately or wait for the task to
     * be over.
     * @param <T> Return type of input task.
     * @param wait True if we must wait for the task to end before returning, false
     * to return immediately after submission.
     * @return The task return value if we must wait, or we're in platform thread. Otherwise null.
     * @param toRun The task to run into JavaFX application thread.
     */
    public static <T> T fxRun(final boolean wait, final Task<T> toRun) {
        if (Platform.isFxApplicationThread())
            toRun.run();
        else
            Platform.runLater(toRun);

        if (wait || Platform.isFxApplicationThread()) {
            try {
                return toRun.get();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) ex.getCause();
                } else {
                    throw new SirsCoreRuntimeException(ex.getCause());
                }
            } catch (Exception e) {
                throw new SirsCoreRuntimeException(e);
            }
        } else
            return null;
    }

    private static Path initConfigurationFolderPath() {
        //On récupère la préférence utilisateur cencée contenir le chemin vers dossier de configuration
        Preferences prefs = Preferences.userNodeForPackage(SirsCore.class);
        String rootPath = prefs.get("CONFIGURATION_FOLDER_PATH", "none");

        //Au premier lancement de l'application, on ouvre une popup afin de renseigner l'emplacement
        //du dossier de configuration, puis on enregistre son chemin dans le fichier de properties
        Path confPath = Paths.get("");
        if (rootPath.equals("none")) {
            try {
                final Dialog dialog    = new Dialog();
                final DialogPane pane  = new DialogPane();
                final ConfFolderPane ipane = new ConfFolderPane();
                pane.setContent(ipane);
                pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
                dialog.setDialogPane(pane);
                dialog.setResizable(true);
                dialog.setTitle("Emplacement du dossier de configuration");

                final Optional opt = dialog.showAndWait();
                if(opt.isPresent() && ButtonType.OK.equals(opt.get())){
                    final String currentPath = ipane.rootFolderField.getText();

                    //Vérifie si le dossier de conf n'existe pas déjà, si oui proposer de le supprimer.
                    Path potentialConf = Paths.get(currentPath, "."+NAME);
                    if (Files.isDirectory(potentialConf)) {
                        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Le dossier de configuration ." + SirsCore.NAME + " existe déjà dans le répertoire indiqué. Souhaitez-vous écraser le dossier existant ?", ButtonType.NO, ButtonType.YES);
                        alert.setResizable(true);
                        alert.getDialogPane().setPrefWidth(500);
                        alert.getDialogPane().setPrefHeight(200);
                        final Optional<ButtonType> res = alert.showAndWait();
                        if (res.isPresent() && ButtonType.YES.equals(res.get())) {
                            try {
                                FileUtils.deleteDirectory(potentialConf.toFile());
                            } catch (IOException ex) {
                                SirsCore.LOGGER.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                            }
                        }
                    }

                    File f = new File(currentPath);
                    if (f.isDirectory()) {
                        rootPath = f.getPath();
                        prefs.put("CONFIGURATION_FOLDER_PATH", rootPath);
                    } else {
                        throw new ExceptionInInitializerError("The location " + currentPath + " is not a folder.");
                    }
                    confPath = Paths.get(rootPath, "."+NAME);
                } else {
                    SirsCore.LOGGER.log(Level.INFO, "The configuration folder dialog is canceled.");
                    System.exit(0);
                }
            } catch (ExceptionInInitializerError ex) {
                throw new ExceptionInInitializerError(ex);
            }
        } else {
            confPath = Paths.get(rootPath, "."+NAME);
        }
        SirsCore.LOGGER.log(Level.INFO, "The current configuration path is {0}.", rootPath);

        if (!Files.isDirectory(confPath)) {
            try {
                Files.createDirectories(confPath);
            } catch (IOException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        return confPath;
    }
}
