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
package fr.sirs.core.component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.sirs.core.ModuleDescription;
import fr.sirs.core.SirsCore;
import static fr.sirs.core.SirsCore.INFO_DOCUMENT_ID;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.authentication.AuthenticationWallet;
import fr.sirs.core.authentication.SIRSAuthenticator;
import fr.sirs.couchdb2.Couchdb2ReplicationTask;
import fr.sirs.index.ElasticSearchEngine;
import fr.sirs.util.ClosingDaemon;
import fr.sirs.util.property.SirsPreferences;
import static fr.sirs.util.property.SirsPreferences.PROPERTIES.COUCHDB_LOCAL_ADDR;
import static fr.sirs.util.property.SirsPreferences.PROPERTIES.NODE_NAME;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.params.HttpParams;
import org.apache.sis.util.ArgumentChecks;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;
import org.ektorp.ReplicationTask;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.IdleConnectionMonitor;
import org.ektorp.http.PreemptiveAuthRequestInterceptor;
import org.ektorp.http.RestTemplate;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDb2Instance;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.Filter;
import org.ektorp.util.Exceptions;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Create a wrapper for connections on a CouchDb service.
 *
 * Warning in order to avoid repetitive authentication, logins are saved by
 * {@link AuthenticationWallet} and {@link SIRSAuthenticator} in a authWallet.json
 * file.
 * In order to allow the backup of a valid authentication, this class is
 * responsible for alerting the {@link SIRSAuthenticator} class of successful
 * requests by calling {@link SIRSAuthenticator#validEntry(java.net.URL)} with
 * the requested Url.
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class DatabaseRegistry {

    private static final String SIRS_FILTER_NAME = "sirsInfoFilter";
    private static final String SIRS_FILTER = "function(doc, req) {"
            + "if (doc._id == \"$sirs\") return false;"
            + "return true;"
            + "}";

    /**
     * Define registry behavior when a connection to one database is queried.
     */
    public static enum DatabaseConnectionBehavior {
        /**
         * If database to connect to does not exists, an error will be thrown.
         */
        FAIL_IF_NOT_EXISTS,
        /**
         * If queried database does not exists, it will be created.
         */
        CREATE_IF_NOT_EXISTS,
        /**
         * A connector is made even if the database does not exists, but it will
         * not be created.
         */
        DEFAULT
    }

    // Httpd related configurations
    private static final String HTTPD_SECTION = "httpd";
    private static final String SOCKET_OPTIONS = "socket_options";
    private static final String NO_DELAY_KEY = "nodelay";
    private static final String BIND_ADDRESS_OPTION = "bind_address";
    private static final String ENABLE_CORS_OPTION = "enable_cors";

    // Cors configuration
    private static final String CORS_SECTION = "cors";
    private static final String CREDENTIALS_OPTIONS = "credentials";
    private static final String METHODS_OPTIONS = "methods";
    private static final String ORIGINS_OPTIONS = "origins";

    // Authentication options
    private static final String AUTH_SECTION = "couch_httpd_auth";
    private static final String REQUIRE_USER_OPTION = "require_valid_user";

    // Replication related configurations
    final static String REPLICATOR_SECTION = "replicator";

    /** Check if a given string is a database URL (i.e not a path / url). */
    private static final Pattern DB_NAME = Pattern.compile("^[a-z][\\w-]+/?");

    /**
     * Détection des départs d'URLs éventuellement suivis de paramètres d'authentification sur l'hôte
     *
     * Exemples :
     * http://localhost:5984
     * http://user:password@localhost:5984
     */
    private static final Pattern URL_START = Pattern.compile("(?i)^[A-Za-z]+://([^@]+@)?");
    /** Check if given string is an URL to local host. */
    private static final Pattern LOCALHOST_URL = Pattern.compile("(?i)^([A-Za-z]+://)?([^@]+@)?(localhost|127\\.0\\.0\\.1)(:\\d+)?");

    private static final int MAX_CONNECTIONS = 100;
    private static final int SOCKET_TIMEOUT = 45000;
    private static final int CONNECTION_TIMEOUT = 5000;

    /**
     * A pattern designed to find strings representing an URL pointing on
     * current CouchDB service.
     */
    private final Pattern hostPattern;

    /**
     * URL of the CouchDb service our registry is working with.
     */
    public final URL couchDbUrl;

    /**
     * Login for connection on CouchDb service.
     */
    private String username;
    /** Password for connection on CouchDb service. */
    private String userPass;

    private CouchDbInstance couchDbInstance;

    /**
     * Create a connection on local database, using address and login found in {@link SirsPreferences}.
     *
     * @throws IOException If URL found in configuration is not valid, or a connection problem occurs.
     */
    public DatabaseRegistry() throws IOException {
        this(null);
    }

    /**
     * Try to connect to CouchDb service pointed by input URL.
     * @param couchDbURL The URL to CouchDB server.
     * @throws IOException If URL found in configuration is not valid, or a connection problem occurs.
     */
    public DatabaseRegistry(final String couchDbURL) throws IOException {
        this(couchDbURL, null, null);
    }

    /**
     * Try to connect to CouchDb service pointed by input URL with given login.
     * @param urlParam The URL to CouchDB server.
     * @param userParam User name to log in with.
     * @param passParam Password for given user.
     * @throws IOException If URL found in configuration is not valid, or a connection problem occurs.
     */
    public DatabaseRegistry(final String urlParam, final String userParam, final String passParam) throws IOException {
        final boolean isLocal;
        if (urlParam == null || DB_NAME.matcher(urlParam).matches() || urlParam.equals(SirsPreferences.INSTANCE.getPropertySafe(COUCHDB_LOCAL_ADDR))) {
            this.couchDbUrl = toURL(SirsPreferences.INSTANCE.getProperty(COUCHDB_LOCAL_ADDR));
            isLocal = true;
        } else {
            this.couchDbUrl = toURL(urlParam);
            isLocal = false;
        }
        username = userParam;
        userPass = passParam;

        // No login, we try to extract it from URL.
        if (username == null || username.isEmpty()) {
            final String userInfo = this.couchDbUrl.getUserInfo();
            if (userInfo != null) {
                String[] split = userInfo.split(":");
                if (split.length > 0) {
                    username = split[0];
                }
                if (split.length > 1) {
                    userPass = split[1];
                }
            }

            AuthenticationWallet.Entry entry = AuthenticationWallet.getDefault().get(couchDbUrl);
            if (entry != null && entry.login != null) {
                username = entry.login;
                userPass = entry.password;
            }
        }

        // Create default user if it does not exists.
        if (username != null && isLocal) {
            try {
                createUserIfNotExists(couchDbUrl, username, userPass);
                SIRSAuthenticator.validEntry(couchDbUrl);
            } catch (Exception e) {
                // normal behavior if authentication is required.
                SirsCore.LOGGER.log(Level.FINE, "User check / creation failed.", e);
            }
        }

        connect();

        // last thing to do : we configure CouchDB wire related parameters.
        if (isLocal) {
            try {
                setAuthenticationRequired();
                setOpenWorld();
                setCorsUseless();
                ensureNoDelay(HTTPD_SECTION);
                ensureNoDelay(REPLICATOR_SECTION);
            } catch (DbAccessException e) {
                SirsCore.LOGGER.log(Level.WARNING, "CouchDB configuration cannot be overriden !", e);
            }
        }

        final String portToUse = (couchDbUrl.getPort() < 0 ? "\\d+" : String.valueOf(couchDbUrl.getPort()));
        if (LOCALHOST_URL.matcher(couchDbUrl.toExternalForm()).find()) {
            hostPattern = Pattern.compile("(?i)^([A-Za-z]+://)?([^@]+@)?(localhost|127\\.0\\.0\\.1)(:"+portToUse+")?");
        } else {
            hostPattern = Pattern.compile("(?i)^([A-Za-z]+://)?([^@]+@)?"+couchDbUrl.getHost()+"(:"+portToUse+")?");
        }
    }

    public CouchSGBD getSGBDInfo(){
        if(couchDbInstance!=null){
        return ((StdCouchDb2Instance) couchDbInstance).getSGBDInfo();
        }
        else {
            throw new IllegalStateException("aucune instance de CouchDB n'est connectée");
        }
    }

    /**
     * @return User name used to log in current CouchDb service. Can be null or empty.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return Password of the user we log in with. Can be null or empty.
     */
    public String getUserPass() {
        return userPass;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // CONNECTION MANAGEMENT
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create a new connection for input parameters. If we cannot connect because
     * provided user does not exists in given database, we will try to create it.
     * If we cannot, we'll ask user for authentication login.
     *
     * @throws IOException If input url is not valid, or a connection failure happens.
     * @throws DbAccessException If login information is invalid.
     * @throws IllegalArgumentException If login information is null.
     */
    private void connect() throws IOException {
        final AuthenticationWallet wallet = AuthenticationWallet.getDefault();

        // Configure http client
        final StdHttpClient.Builder builder = new SirsClientBuilder()
                .maxConnections(MAX_CONNECTIONS)
                .caching(false)
                .url(couchDbUrl)
                .connectionTimeout(CONNECTION_TIMEOUT)
                .socketTimeout(SOCKET_TIMEOUT)
                .relaxedSSLSettings(true);


               boolean unPassed=true;
               byte attempt =0;
        while(unPassed) {
            try {
                couchDbInstance = new StdCouchDb2Instance(builder.build(), SirsPreferences.INSTANCE.getProperty(NODE_NAME));
                couchDbInstance.getAllDatabases();
                //Si cette ligne est exécutée, cela signifie que la requête provoquée par getAllDatabases()
                //a réussie. On demande alors à la classe SIRSAuthenticator.java de sauvegarder les identifiants de connection utilisés.
                SIRSAuthenticator.validEntry(couchDbUrl);
                unPassed = false;
            } catch (DbAccessException dbexc) {
                if (++attempt==(byte)3){
                    throw Exceptions.propagate(dbexc);
                }

            }
        }
    }

    /**
     * List SIRS application databases found on current CouchDb server.
     * @return The name of all databases which contain SIRS data.
     */
    public List<String> listSirsDatabases() {
        final List<String> dbList = couchDbInstance.getAllDatabases();
        SIRSAuthenticator.validEntry(couchDbUrl);
        List<String> res = new ArrayList<>();
        for (String db : dbList) {
            if (db.startsWith("_"))
                continue;
            // If there's a "sirsInfo" document, it's an app db.
            CouchDbConnector connector = couchDbInstance.createConnector(db, false);
            try {
                getInfo(connector).map(info -> res.add(db));
            } catch (Exception e) {
                SirsCore.LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return res;
    }

    /**
     * Connect to database with given name on current CouchDb service.
     * @param dbName Name of the database to create.
     * @param createIfNotExists True to create the database if it does not exists. false otherwise.
     * @param initIndex True if we must start an elastic search index (creates an {@link ElasticSearchEngine}) on database.
     * @param initChangeListener True if we must start a {@link DocumentChangeEmiter} on database changes, false otherwise.
     * @return A connector to queried database.
     * @throws java.io.IOException If an error occurs while connecting to couchDb database.
     * @throws IllegalArgumentException If given database name is an URL to another CouchDB service.
     */
    public ConfigurableApplicationContext connectToSirsDatabase(String dbName,
            final boolean createIfNotExists, final boolean initIndex,
            final boolean initChangeListener) throws IOException {

        if (!isLocal(dbName)) {
            throw new IllegalArgumentException("Given database name is not on current service !");
        }

        CouchDbConnector connector = createConnector(dbName, DatabaseConnectionBehavior.CREATE_IF_NOT_EXISTS);

        // Initializing application context will load application repositories, which will publish their views on the new database.
        final ClassPathXmlApplicationContext parentContext = new ClassPathXmlApplicationContext();
        parentContext.refresh();
        final ConfigurableListableBeanFactory parentFactory = parentContext.getBeanFactory();
        parentFactory.registerSingleton(CouchDbConnector.class.getSimpleName(), connector);
        if (initChangeListener) {
            DocumentChangeEmiter changeEmiter = new DocumentChangeEmiter(connector);
            parentFactory.registerSingleton(DocumentChangeEmiter.class.getSimpleName(), changeEmiter);
            changeEmiter.start();
        }
        if (initIndex) {
            if (username == null) {  //Si aucun utilisateur n'est identifié, on vérifie qu'il n'a pas était créé depuis instanciation.
                AuthenticationWallet.Entry entry = AuthenticationWallet.getDefault().get(couchDbUrl);
                if (entry != null && entry.login != null) {
                    username = entry.login;
                    userPass = entry.password;
                }
            }
            ElasticSearchEngine elasticEngine = new ElasticSearchEngine(
                    couchDbUrl.getHost(), (couchDbUrl.getPort() < 0) ? 5984 : couchDbUrl.getPort(), connector.getDatabaseName(), username, userPass);
            parentFactory.registerSingleton(ElasticSearchEngine.class.getSimpleName(), elasticEngine);
        }

        parentFactory.registerSingleton(DatabaseRegistry.class.getSimpleName(), this);

        return new ClassPathXmlApplicationContext(new String[]{SirsCore.SPRING_CONTEXT}, parentContext);
    }

    /**
     * Delete the queried database of current CouchDb service.
     * @param dbName Name of the database (must be a name valid on current CouchDb service) to remove.
     * @throws IOException If an error happened while connecting to CouchDb service.
     */
    public void dropDatabase(String dbName) throws IOException {
        cancelAllSynchronizations(dbName);
        couchDbInstance.deleteDatabase(dbName);
    }


    ////////////////////////////////////////////////////////////////////////////
    //
    // REPLICATION UTILITIES
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Synchronize two databases content.
     *
     * Notes :
     * - "local" database information will be updated. Its "remote database"
     * information will be updated with "distant" database path.
     *
     * - Since the method return only when synchronisation is finished (except
     * if continuous, in case it returns after first synchronisation pass is over),
     * it can take quite a long time.
     *
     * - If client throws an error, but replication is still going on the couchdb
     * service, it means the replication is going as planned, but client cannot
     * wait any longer. In that case, the error is caught silently, allowing client
     * to go further while replication keep going.
     *
     * @param remoteDb The source database name if it's in current service, complete URL otherwise.
     * @param localDb Name of target database. It have to be in current service.
     * @param continuous True if we must keep databases synchronized over time. False for one shot synchronization.
     * @return Information about all initiated replications. Can be empty, but never null.
     * @throws IOException If an error occurs while trying to connect to one database.
     * @throws IllegalArgumentException If databases are incompatible (different SRID, or distant bdd is not a SIRS one).
     * @throws DbAccessException If an error occcurs during replication.
     */
    public List<ReplicationStatus> synchronizeSirsDatabases(final String remoteDb, final String localDb, boolean continuous) throws IOException {
        Optional<SirsDBInfo> distantInfo = getInfo(remoteDb);
        if (!distantInfo.isPresent()) {
            throw new IllegalArgumentException(new StringBuilder("Impossible de trouver une base de données à l'adresse suivante : ")
                    .append(remoteDb).append(".").append(System.lineSeparator())
                    .append("Vérifier l'hôte, le port, et le nom de la base de données dans l'adresse entrée.").toString());
        }

        final String distantNoAuth = removeAuthenticationInformation(remoteDb);
        final CouchDbConnector localConnector = createConnector(localDb, DatabaseConnectionBehavior.CREATE_IF_NOT_EXISTS);
        Optional<SirsDBInfo> info = getInfo(localConnector);
        boolean sameDistant = false;
        if (info.isPresent()) {
            SirsDBInfo localInfo = info.get();
            sameDistant = remoteDb.equals(localInfo.getRemoteDatabase()) || distantNoAuth.equals(localInfo.getRemoteDatabase());
            if (localInfo.getRemoteDatabase() != null && !sameDistant) {
                final TaskManager.MockTask<Optional<ButtonType>> confirmation = new TaskManager.MockTask<>(() -> {
                    final Alert alert = new Alert(
                            Alert.AlertType.WARNING,
                            "Vous êtes sur le point de changer le point de synchronisation de la base de données.",
                            ButtonType.CANCEL, ButtonType.OK);
                    alert.setResizable(true);
                    return alert.showAndWait();
                });

                if (Platform.isFxApplicationThread()) {
                    confirmation.run();
                } else {
                    Platform.runLater(confirmation);
                }

                final Optional<ButtonType> showAndWait;
                try {
                    showAndWait = confirmation.get(90, TimeUnit.SECONDS);
                } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                    throw new SirsCoreRuntimeException(ex);
                }
                if (showAndWait.isPresent() && showAndWait.get().equals(ButtonType.OK)) {
                    cancelAllSynchronizations(localConnector.getDatabaseName());
                } else {
                    return Collections.EMPTY_LIST;
                }
            }
        }

        // Force authentication on distant database. We can rely on wallet information
        // because a connection should have been opened already to retrieve SIRS information.
        final String remoteAuthDb = addAuthenticationInformation(remoteDb);
        final String localAuthDb = addAuthenticationInformation(localDb);

        final List<ReplicationStatus> result = new ArrayList<>(2);
        try {
            final ReplicationStatus status = copyDatabase(remoteAuthDb, localAuthDb, continuous);
            if (status != null) {
                result.add(status);
            }
        } catch (DbAccessException e) {
            checkReplicationError(e, remoteAuthDb, localAuthDb);
        }

        try {
            final ReplicationStatus status = copyDatabase(localAuthDb, remoteAuthDb, continuous);
            if (status != null) {
                result.add(status);
            }
        } catch (DbAccessException e) {
            checkReplicationError(e, localAuthDb, remoteAuthDb);
        }

        // Update local database information : remote db.
        if (!sameDistant) {
            new SirsDBInfoRepository(localConnector).setRemoteDatabase(distantNoAuth);
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Check that a replication task is still running for given databases. If not,
     * input error is thrown.
     * @param e The error to throw if no replication task is found.
     * @param sourceDb Source database of the replication task.
     * @param targetDb Target database of the replication tassk.
     * @throws DbAccessException If no replication is found between given databases.
     */
    private void checkReplicationError(final DbAccessException e, final String sourceDb, final String targetDb) throws DbAccessException {
        final long count;
        /* Replication status modify database url to hide password, so we have to truncate this part to allow comparison.
           We also remove end '/' which could appear in urls.
         */
        final String tmpSource = cleanDatabaseName(sourceDb);
        final String tmpTarget = cleanDatabaseName(targetDb);
        try {
            count = getReplicationTasks()
                    .filter(status -> {
                        return cleanDatabaseName(status.getSourceDatabaseName()).equals(tmpSource)
                                && cleanDatabaseName(status.getTargetDatabaseName()).equals(tmpTarget);
                    })
                    .count();
        } catch (Exception e1) {
            e.addSuppressed(e1);
            throw e;
        }

        if (count < 1) {
            throw e;
        } else {
            SirsCore.LOGGER.log(Level.FINE, e, () -> String.format("An error occured during a replication from %s to %s", sourceDb, targetDb));
        }
    }

    /**
     * Retrieve list of continuous synchronization tasks the input database is part of. Even
     * paused tasks are returned.
     *
     * @param dbUrl Url de la base à synchroniser
     * @return List of source database names or path for continuous copies on input database.
     * @throws java.io.IOException If we cannot get list of active synchronisation from CouchDB.
     */
    public Stream<ReplicationTask> getSynchronizationTasks(final String dbUrl) throws IOException {
        ArgumentChecks.ensureNonEmpty("Input database name", dbUrl);
        return getReplicationTasksBySourceOrTarget(dbUrl)
                .filter((ReplicationTask task)-> task.isContinuous());
    }

    /**
     * Copy source database content to destination database. If destination database
     * does not exists, it will be created.
     *
     * Note : $sirs document (which describes SIRS database information) will not
     * be copied. However, if given databases both exist, it will be analyzed to
     * ensure databases are compatibles (same srid, application / modules versions, etc.)
     *
     * @param sourceDb url complète de la base source de la copie, incluant les paramètres d'authentification
     * @param targetDb url complète de la base cible de la copie, incluant les paramètres d'authentification
     * @param continuous If true, target database will continuously retrieve changes happening in source database. If not, it's one shot copy.
     * @return A status of started replication task. Can be null if we failed
     * getting it from the server, but the replication looks like it's going fine.
     * @throws java.io.IOException If an error occurs while connecting to the databases.
     */
    public ReplicationStatus copyDatabase(final String sourceDb, final String targetDb, final boolean continuous) throws IOException {

        // Ensure database to copy is valid.
        final CouchDbConnector srcConnector = createConnector(sourceDb, DatabaseConnectionBehavior.FAIL_IF_NOT_EXISTS);
        Optional<SirsDBInfo> info = getInfo(srcConnector);

        /* Check if target database exists/ can be created, and if we have to
         * add authentication information. We don't create database now. We'll
         * do it later, reducing chances of keeping an empty database if an
         * error occurs.
         */
        final CouchDbConnector tgtConnector = createConnector(targetDb, DatabaseConnectionBehavior.DEFAULT);

        // If no info is found, the database is not a SIRS db. We cannot make any analysis.
        Map<String, ModuleDescription> modules = null;
        String sridToSet = null;
        if (info.isPresent()) {
            final SirsDBInfo srcInfo = info.get();
            final String srcSRID = srcInfo.getEpsgCode();
            info = getInfo(tgtConnector);

            if (info.isPresent()) {
                final SirsDBInfo dstInfo = info.get();
                final String dstSRID = dstInfo.getEpsgCode();
                if (srcSRID == null ? dstSRID != null : !srcSRID.equals(dstSRID)) {
                    throw new IllegalArgumentException(String.format(Locale.FRANCE,
                            "Impossible de synchroniser les bases de données car elles n'utilisent pas le même système de projection : (%s : %s) => (%s : %s)",
                            cleanDatabaseName(sourceDb), srcSRID, cleanDatabaseName(targetDb), dstSRID));
                }

                final Map<String, ModuleDescription> srcModules = srcInfo.getModuleDescriptions();
                final Map<String, ModuleDescription> dstModules = dstInfo.getModuleDescriptions();
                //final int dstModuleListSize = dstModules == null ? 0 : dstModules.size();

                if (dstModules == null && srcModules != null) {
                    modules = srcModules;
                }
                else if (srcModules != null && dstModules != null) {

                    final StringBuilder moduleError = new StringBuilder("Les bases de données ne peuvent être synchronisées"
                            + " car elles travaillent avec des versions différentes des modules suivants : ");
                    boolean throwException = false;
                    /* We compare databases modules (it includes core comparison).
                     Also, as $sirs will not be copied, but we have to merge
                     module descriptions, we make an union of the two databases.
                     */
                    for (final Map.Entry<String, ModuleDescription> entry : srcModules.entrySet()) {
                        final ModuleDescription desc = dstModules.get(entry.getKey());

                        if (desc == null) {
                            dstModules.put(entry.getKey(), entry.getValue());
                        //Currently ModuleChecker ensure this never happen (?)
                        } else if (!desc.getVersion().equals(entry.getValue().getVersion())) {
                            throwException = true;
                            moduleError.append(System.lineSeparator())
                                    .append(desc.title.get())
                                    .append(" : ")
                                    .append(desc.getVersion())
                                    .append(" (")
                                    .append(cleanDatabaseName(targetDb))
                                    .append(") / ")
                                    .append(entry.getValue().getVersion())
                                    .append(" (")
                                    .append(cleanDatabaseName(sourceDb))
                                    .append(')');
                        } else {
                            //Merge layers content
                            final String k = entry.getKey();
                            final ModuleDescription desc2 = entry.getValue();

                            this.mergeModuleDescriptions(desc, desc2);
                            dstModules.put(k, desc2);
                        }
                    }

                    if (throwException) {
                        throw new IllegalArgumentException(moduleError.toString());
                    }
                    modules = dstModules;
                }
            } else if (srcSRID != null) {
                sridToSet = srcSRID;
            }
        }

        // We're now sure that replication can be launched (source exists, etc.).
        // We create destination if it does not exists yet, and proceed.
        tgtConnector.createDatabaseIfNotExists();

        /*
         * If we're initiating a continuous copy, or the two databases already
         * contain their own $sirs document, we filter replication to keep them
         * distinct. Otherwise, we can make a one-shot replication without filter
         * to speed it up.
         */
        final ReplicationCommand cmd;
        if (info.isPresent() || continuous) {
            // Post required filter if it doesn't exist.
            new SirsFilterRepository(srcConnector);

            // Post required filter if it doesn't exist.
            new SirsFilterRepository(tgtConnector);

            cmd = new ReplicationCommand.Builder()
                    .source(sourceDb).target(targetDb)
                    .continuous(continuous)
                    .filter(SirsFilters.class.getSimpleName().concat("/").concat(SIRS_FILTER_NAME)) // Filter $sirs document.
                    .build();
        } else {
            cmd = new ReplicationCommand.Builder()
                    .source(sourceDb).target(targetDb)
                    .continuous(continuous)
                    .build();
        }

        ReplicationStatus status = null;
        int remainingAttempt = 5;
        while (status == null && remainingAttempt-- > 0) {
            try {
                status = couchDbInstance.replicate(cmd);
                // Si cette ligne est exécutée, cela signifie que la requête envoyée
                // a réussie. On demande alors à la classe SIRSAuthenticator.java
                // de sauvegarder les identifiants de connection utilisés.
                SIRSAuthenticator.validEntry(couchDbUrl);
            } catch (DbAccessException e) {
                checkReplicationError(e, sourceDb, targetDb);
                // If the error has not been thrown back, it means it was not
                // related to a replication execution. It can be an exception
                // related to status parsing failing, for example.

                // Add an additional attempt if the exception is a read timeout exception.
                Throwable cause = e.getCause();
                if (cause instanceof SocketTimeoutException) {
                    if (cause.getMessage() != null && cause.getMessage().equals("Read timed out")) {
                        remainingAttempt++;
                        SirsCore.LOGGER.log(Level.FINE, "Replication exceeds the socket timeout but continues nonetheless", e);
                    } else {
                        SirsCore.LOGGER.log(Level.WARNING, "An error happened while submitting replication", e);
                    }
                } else {
                    SirsCore.LOGGER.log(Level.WARNING, "An error happened while submitting replication", e);
                }
            }
        }

        // update $sirs if the two databases didn't used the same modules.
        if (modules != null || sridToSet != null) {
            final SirsDBInfoRepository infoRepo = new SirsDBInfoRepository(tgtConnector);
            if (modules != null) {
                infoRepo.updateModuleDescriptions(modules);
            }
            if (sridToSet != null) {
                infoRepo.setSRID(sridToSet);
            }
        }

        return status;
    }

    /**
     * Cancel the database copy pointed by given replication status.
     * @param operationStatus Status of the copy to stop.
     * @return The status of cancelled task.
     */
    public ReplicationStatus cancelCopy(final ReplicationStatus operationStatus) {
        return couchDbInstance.replicate(new ReplicationCommand.Builder()
                .id(operationStatus.getId())
                .cancel(true)
                .build());
    }

    public ReplicationStatus cancelCopy(final String dstDbName) {
        ReplicationStatus status = null;
        Optional<ReplicationTask> task = null;

        try {
            task = getReplicationTasksByTarget(dstDbName).findFirst();
        } catch (IOException ex) {
            SirsCore.LOGGER.log(Level.WARNING, "Cannot request active task.", ex);
        }
        if (task != null && task.isPresent()) {
            status = couchDbInstance.replicate(new ReplicationCommand.Builder()
                    .cancel(true)
                    .id(task.get().getReplicationId())
                    .build());
        }
        return status;
    }

    /**
     * Cancel all replication tasks implying input database.
     * @param dbName Name of the database to deconnect.
     * @return Number of cancelled replication tasks.
     * @throws IOException If a connection error happens while retrieving replication tasks.
     */
    public int cancelAllSynchronizations(final String dbName) throws IOException {
        final AtomicInteger counter = new AtomicInteger(0);
        final ReplicationCommand.Builder cancel = new ReplicationCommand.Builder().cancel(true);
        getReplicationTasksBySourceOrTarget(dbName).forEach(task -> {
            couchDbInstance.replicate(cancel
                    .id(task.getReplicationId())
                    .build());
            counter.incrementAndGet();
        });
        return counter.get();
    }

    /**
     *
     * @return All replications found on current CouchDb service.
     * @throws IOException If an error happens while connecting to couchdb, or while reading a replication status.
     */
    public Stream<ReplicationTask> getReplicationTasks() throws IOException {
        HttpResponse httpResponse = couchDbInstance.getConnection().get("/_active_tasks");

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final InputStream in = httpResponse.getContent();
        final Stream<ReplicationTask> stream;
        try {
            final JsonParser parser = mapper.getFactory().createParser(in);
            final JsonNode tasks = parser.readValueAsTree();

            if (!tasks.isArray()) {
                parser.close();
                in.close();
                return Stream.empty();
            }

            final Iterator<JsonNode> elements = tasks.elements();
            Spliterator<ReplicationTask> spliterator = new Spliterator<ReplicationTask>() {

                @Override
                public boolean tryAdvance(Consumer<? super ReplicationTask> action) {
                    while (elements.hasNext()) {
                        final JsonNode next = elements.next();
                        if (next.has("type") && "replication".equals(next.get("type").asText())) {
                            try {
                                action.accept(mapper.treeToValue(next, Couchdb2ReplicationTask.class));
                            } catch (JsonProcessingException ex) {
                                throw new UncheckedIOException(ex);
                            }
                            return true;
                        }
                    }

                    return false;
                }

                @Override
                public Spliterator<ReplicationTask> trySplit() {
                    return null;
                }

                @Override
                public long estimateSize() {
                    return Long.MAX_VALUE;
                }

                @Override
                public int characteristics() {
                    return Spliterator.IMMUTABLE | Spliterator.DISTINCT | Spliterator.NONNULL;
                }
            };

            stream = StreamSupport.stream(spliterator, false);
            stream.onClose(() -> {
                try {
                    parser.close();
                    in.close();
                } catch (IOException ex) {
                    SirsCore.LOGGER.log(Level.WARNING, "An http response stream cannot be closed.", ex);
                }
            });
            ClosingDaemon.watchResource(stream, in);
        } catch (Exception e) {
            try {
                in.close();
            } catch (Exception bis) {
                e.addSuppressed(bis);
            }
            throw e;
        }
        return stream;
    }

    private Stream<ReplicationTask> getReplicationTasksBySourceOrTarget(final String dst) throws IOException {
        final String cleanedDst = cleanDatabaseName(dst);
        return getReplicationTasks().filter(
                t -> (cleanDatabaseName(t.getSourceDatabaseName()).equals(cleanedDst)
                        || cleanDatabaseName(t.getTargetDatabaseName()).equals(cleanedDst)));
    }

    private Stream<ReplicationTask> getReplicationTasksByTarget(final String dst) throws IOException {
        final String cleanedDst = cleanDatabaseName(dst);
        return getReplicationTasks().filter(t -> (cleanDatabaseName(t.getTargetDatabaseName()).equals(cleanedDst)));
    }

    /**
     * Merge ModuleDescription layers into desc2
     * @param desc1
     * @param desc2
     */
    private void mergeModuleDescriptions(ModuleDescription desc1, ModuleDescription desc2) {
        mergeLayers(desc1.layers, desc2.layers);
    }

    /**
     * Merge layers1 and layers 2 into layers2
     * @param layers1
     * @param layers2
     */
    private void mergeLayers(List<ModuleDescription.Layer> layers1, List<ModuleDescription.Layer> layers2) {
        List<ModuleDescription.Layer> toAdd = new ArrayList<>();

        if (layers1.isEmpty()) return;

        for (ModuleDescription.Layer l1: layers1) {
            boolean found = false;

            for (ModuleDescription.Layer l2: layers2) {
                if (l1.getTitle() != null && l1.getTitle().equals(l2.getTitle())) {
                    found = true;
                    mergeLayers(l1.children, l2.children);
                }
            }
            if (!found) {
                toAdd.add(l1);
            }
        }
        layers2.addAll(toAdd);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // COUCHDB CONFIGURATION UTILITIES
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Try to create an user in the given database if he does not already exists.
     *
     * Note: We use a direct HTTP call, without any credential. We cannot use provided
     * login information, because if it describes a inexisting user, couchdb will
     * fail on authentication requests.
     *
     * Note 2 : Although we test user existence, its password is not checked (because hash).
     *
     * @param couchDbUrl URL to the CouchDB database on which to create user.
     * @param username The login of the user to check/create.
     * @param password The password to use if we have to create user.
     * @throws IOException If we have a problem while building PUT documents.
     */
    private static void createUserIfNotExists(final URL couchDbUrl, final String username, final String password) throws IOException {
        RestTemplate template = new RestTemplate(new StdHttpClient.Builder().url(couchDbUrl).build());

        final String adminConfig = String.format("/_node/%s/_config/admins/%s",
                SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.NODE_NAME), username);
        try {
            template.getUncached(adminConfig);
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.FINE, "No administrator can be found for login " + username, e);
            final String userConfig = "/_users/org.couchdb.user:" + username;
            try {
                template.getUncached(userConfig);
            } catch (Exception e2) {
                SirsCore.LOGGER.log(Level.FINE, "No user can be found for login " + username, e);
                SirsCore.LOGGER.fine("Attempt to create administrator " + username);

                // try to send user as database admin. If it's a fail, we will try to add him as simple user.
                try {
                    template.put(adminConfig, password == null ? "" : "\"" + password + "\"");
                } catch (DbAccessException e3) {
                    SirsCore.LOGGER.log(Level.FINE, "Cannot create administrator " + username, e);
                    SirsCore.LOGGER.fine("Attempt to create simple user " + username);

                    final String userContent;
                    try (final InputStreamReader in = new InputStreamReader(DatabaseRegistry.class.getResourceAsStream("/fr/sirs/launcher/user-put.json"), StandardCharsets.UTF_8);
                            final BufferedReader reader = new BufferedReader(in)) {
                        final StringBuilder builder = new StringBuilder();
                        reader.lines().forEach(builder::append);
                        userContent = builder.toString();
                    }
                    template.put(userConfig,
                            userContent.replaceAll("\\$ID", username)
                            .replaceAll("\\$PASSWORD", password == null ? "" : password));
                }
            }
        }
    }

    private void ensureNoDelay(final String section) {
        try {
            String socketConfig;
            // Catch exception at read because if property does not exists, an error is thrown.
            try {
                socketConfig = couchDbInstance.getConfiguration(section, SOCKET_OPTIONS);
            } catch (DbAccessException e) {
                socketConfig = null;
            }

            if (socketConfig == null || socketConfig.trim().matches("\\[\\s*\\]")) {
                socketConfig = "[{" + NO_DELAY_KEY + ", true}]";
            } else {
                final Matcher matcher = Pattern.compile(NO_DELAY_KEY + "\\s*,\\s*(true|false)").matcher(socketConfig);
                if (matcher.find()) {
                    if (matcher.group(1).equalsIgnoreCase("true")) {
                        SirsCore.LOGGER.info("SOCKET configuration found with right value.");
                        return;
                    } else {
                        socketConfig = matcher.replaceAll(NO_DELAY_KEY + ", true");
                    }
                } else {
                    socketConfig = socketConfig.replaceFirst("\\]$", ", {" + NO_DELAY_KEY + ", true}]");
                }
            }

            SirsCore.LOGGER.info("SOCKET configuration about to be SET");
            couchDbInstance.setConfiguration(section, SOCKET_OPTIONS, socketConfig);

            // Do not make application fail because of an optimisation.
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.WARNING, "Cannot check 'no delay' configuration", e);
        }
    }

    /**
     * Configure CouchDB to force client authentication. Not avoidable.
     *
     * @throws DbAccessException if unable to authenticate on the couchDB with the given user.
     */
    private void setAuthenticationRequired() {
        String authValue;
        try {
            authValue = couchDbInstance.getConfiguration(AUTH_SECTION, REQUIRE_USER_OPTION);
            SIRSAuthenticator.validEntry(couchDbUrl);
        } catch (DbAccessException e) {
            authValue = null;
        }
        if (!"true".equals(authValue)) {
            couchDbInstance.setConfiguration(AUTH_SECTION, REQUIRE_USER_OPTION, "true");
        }
    }

    /**
     * Allow current couchdb service to be requested by any host. The "bind_adress"
     * property, which lists allowed hosts, will be overrided only if it's null or
     * if it contains a single local address (default couchdb configuration). It
     * means you still can restrain accesses to localhost by setting it as follow :
     * "localhost, 127.0.0.1".
     */
    private void setOpenWorld() {
        String bindAdress;
        try {
            bindAdress = couchDbInstance.getConfiguration(HTTPD_SECTION, BIND_ADDRESS_OPTION);
        } catch (DbAccessException e) {
            bindAdress = null;
        }
        if (bindAdress == null || LOCALHOST_URL.matcher(bindAdress).matches()) {
            couchDbInstance.setConfiguration(HTTPD_SECTION, BIND_ADDRESS_OPTION, "0.0.0.0");
        }

        String corsValue;
        try {
            corsValue = couchDbInstance.getConfiguration(HTTPD_SECTION, ENABLE_CORS_OPTION);
        } catch (DbAccessException e) {
            corsValue = null;
        }
        if (!"true".equals(corsValue)) {
            couchDbInstance.setConfiguration(HTTPD_SECTION, ENABLE_CORS_OPTION, "true");
        }
    }

    /**
     * Make Cors filter accept all types of requests from any host.
     * Cors accepted origins will be overrided only if its not already present in
     * couchdb configuration (default configuration).
     *
     * Cors accepted methods will be overrided only if its not already present in
     * couchdb configuration (default configuration).
     */
    private void setCorsUseless() {
        String credentials;
        try {
            credentials = couchDbInstance.getConfiguration(CORS_SECTION, CREDENTIALS_OPTIONS);
        } catch (DbAccessException e) {
            credentials = null;
        }
        if (!"true".equals(credentials)) {
            couchDbInstance.setConfiguration(CORS_SECTION, CREDENTIALS_OPTIONS, "true");
        }

        try {
            couchDbInstance.getConfiguration(CORS_SECTION, ORIGINS_OPTIONS);
        } catch (Exception e) {
            couchDbInstance.setConfiguration(CORS_SECTION, ORIGINS_OPTIONS, "*");
        }

        try {
            couchDbInstance.getConfiguration(CORS_SECTION, METHODS_OPTIONS);
        } catch (Exception e) {
            couchDbInstance.setConfiguration(CORS_SECTION, METHODS_OPTIONS, "GET, POST, PUT, DELETE");
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    //
    // MINOR UTILITY METHODS
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Check input string syntax to find if it points on a local (i.e on current service)
     * database, or not.
     *
     * Note : this method purpose is NOT to know if the database exists on current
     * CouchDB, but only to know if a connector can be created here.
     *
     * @param dbNameOrPath Database name / url to check.
     * @return True if given string represents a database on current CouchDB service, false otherwise.
     */
    private boolean isLocal(final String dbNameOrPath) {
        if (DB_NAME.matcher(dbNameOrPath).matches() || hostPattern.matcher(dbNameOrPath).find()) {
            return true;
        } else try {
            /* If quick check using regex fails, we attempt a last verification
             * using strict URLs.
             */
            final URL tmpURL = new URL(dbNameOrPath);
            return tmpURL.getHost().equals(couchDbUrl.getHost()) && tmpURL.getPort() == couchDbUrl.getPort();
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    /**
     * If input string is an URL, and we can find authentication information for
     * it, we return the url with basic authentication information. Otherwise,
     * the initial string is returned.
     * @param str The string to analyze.
     * @return Input string if no authentication information can be found. An URL
     * with basic authentication information otherwise.
     */
    public static String addAuthenticationInformation(final String str) {
        // Force authentication on distant database. We can rely on wallet information
        // if a connection has already been opened to retrieve SIRS information.
        try {
            URL distantURL = toURL(str);
            if (distantURL.getUserInfo() == null) {
                AuthenticationWallet.Entry entry = AuthenticationWallet.getDefault().get(distantURL);
                if (entry != null && entry.login != null && !entry.login.isEmpty()) {
                    final String userInfo;
                    if (entry.password == null || entry.password.isEmpty()) {
                        userInfo = entry.login + "@";
                    } else {
                        userInfo = entry.login + ":" + entry.password + "@";
                    }
                    return distantURL.toExternalForm().replaceFirst("(^\\w+://)", "$1" + userInfo);
                }
            }
            else {
                SirsCore.LOGGER.log(Level.FINE, "Les paramètres d'authentification sont déjà présents");
            }
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.FINE, "Cannot add authentication information", e);
        }

        return str;
    }

    private static String removeAuthenticationInformation(final String str) {
        try {
            final URL url = toURL(str);
            if (url.getUserInfo() != null) {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile()).toExternalForm();
            }
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.WARNING, "Cannot remove authentication information", e);
        }

        return str;
    }

    /**
     * Build an {@link  URL} object from given string. Add protocol http prefix
     * if no prefix is defined in input string.
     * @param baseURL The string to make URL from.
     * @return An URL representing input path.
     * @throws MalformedURLException If input string does not represent a path.
     */
    public static URL toURL(final String baseURL) throws MalformedURLException {
        if (URL_START.matcher(baseURL).find()) {
            return new URL(baseURL);
        } else {
            return new URL("http://"+baseURL);
        }
    }

    /**
     * Try to make a simple connection over database pointed by given name / URL.
     *  It can be a simple name if the database is present in localhost. Otherwise,
     *  an URL must be given.
     *
     * /!\ Only a connector is provided. Repositories, session and other application
     * components are not created. This method purpose is only to make a quick
     * connection over a database to get back simple information, or in the
     * special case of module migration. If you want to work with SIRS objects,
     * you should use {@link #connectToSirsDatabase(java.lang.String, boolean, boolean, boolean) }
     * to get a complete working environment.
     *
     * @param dbNameOrPath Path to or name of the target database.
     * @param behavior Defines behavior to adopt when creating connector. If null,
     * {@link DatabaseConnectionBehavior#DEFAULT} is assumed.
     * @return A connector to the wanted database.
     * @throws IOException If an error occurs while connecting to CouchDB service.
     * @throws IllegalArgumentException If input string is syntaxically wrong, or
     * queried database does not exists and {@link DatabaseConnectionBehavior#FAIL_IF_NOT_EXISTS }
     * has been given as parameter.
     */
    public CouchDbConnector createConnector(final String dbNameOrPath, final DatabaseConnectionBehavior behavior) throws IOException, IllegalArgumentException {
        final boolean create = DatabaseConnectionBehavior.CREATE_IF_NOT_EXISTS.equals(behavior);
        final boolean fail = DatabaseConnectionBehavior.FAIL_IF_NOT_EXISTS.equals(behavior);

        if (DB_NAME.matcher(dbNameOrPath).matches()) {
            if (fail && !couchDbInstance.checkIfDbExists(dbNameOrPath)) {
                throw new IllegalArgumentException(String.format("La base de donnée %s n'existe pas !", dbNameOrPath));
            } else {
                return couchDbInstance.createConnector(dbNameOrPath, create);
            }
        }
        else {
            final Matcher matcher = hostPattern.matcher(dbNameOrPath);
            if (matcher.find()) {
                // An URL in current service has been given. We have to extract database path from it.
                final String[] splittedPath = matcher.replaceAll("").split("/");
                if (splittedPath.length < 1){
                    throw new IllegalArgumentException(String.format("L'adresse donnée (%s) ne représente pas une base donnée valide.", dbNameOrPath));
                }
                int splitIndex = splittedPath.length;
                final StringBuilder pathBuilder = new StringBuilder(splittedPath[--splitIndex]);
                CouchDbConnector connector = null;
                while (splitIndex > 0 && connector == null) {
                    try {
                        connector = couchDbInstance.createConnector(pathBuilder.toString(), create);
                    } catch (Exception e) {
                        pathBuilder.insert(0, '/').insert(0, splittedPath[--splitIndex]);
                    }
                }

                if (connector == null) {
                    throw new IllegalArgumentException(String.format("L'adresse donnée (%s) ne représente pas une base donnée valide.", dbNameOrPath));
                } else if (fail && !couchDbInstance.checkIfDbExists(connector.getDatabaseName())) {
                throw new IllegalArgumentException(String.format("La base de donnée %s n'existe pas !", dbNameOrPath));
                } else {
                    return connector;
                }

            } else {
                return new DatabaseRegistry(dbNameOrPath).createConnector(dbNameOrPath, behavior);
            }
        }
    }

    /**
     * Try to get $sirs document from given database name / url.
     * @param dbName The name of the database in current service, or a complete
     * URL to a database in a different service.
     * @return An empty optional if no sirs information can be found. Otherwise
     * it's filled with found document.
     */
    public Optional<SirsDBInfo> getInfo(final String dbName) {
        try {
            return getInfo(createConnector(dbName, DatabaseConnectionBehavior.DEFAULT));
        } catch (IOException ex) {
            SirsCore.LOGGER.log(Level.WARNING, null, ex);
            return Optional.empty();
        }
    }

    /**
     * Try to get $sirs document from given database connector.
     * @param connector CouchDB database connection.
     * @return An empty optional if no sirs information can be found. Otherwise
     * it's filled with found document.
     */
    public static Optional<SirsDBInfo> getInfo(CouchDbConnector connector) {
        if (connector.contains(INFO_DOCUMENT_ID)) {
            return Optional.of(connector.get(SirsDBInfo.class, INFO_DOCUMENT_ID));
        }
        return Optional.empty();
    }

    /**
     * Simplfiy database path (if it's an URL) to make it comparable without
     * authentication or protocol. If given database name represents a complete
     * URL, its start (protocol + authentication) is removed. We also remove any
     * '/' at the end of the name.
     *
     * @param sourceDbName Database name to simplify.
     * @return Cut database name.
     */
    public static String cleanDatabaseName(final String sourceDbName) {
        return URL_START.matcher(sourceDbName)
                // suppression de la partie protocole + éventuelle authentification
                .replaceFirst("")
                // suppression de l'éventuel "/" de fin d'URL
                .replaceFirst("/$", "")
                // normalisation de la désignation de la boucle locale
                .replaceAll("127.0.0.1", "localhost");
    }

    private static class SirsClientBuilder extends StdHttpClient.Builder {

        @Override
        public HttpClient configureClient() {
            // TODO : build our own non-deprecated http client. See https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
            HttpClient client = super.configureClient();
            if (client instanceof DefaultHttpClient) {
                final DefaultHttpClient tmpClient = (DefaultHttpClient) client;
                tmpClient.setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()));
                tmpClient.setCredentialsProvider(new SystemDefaultCredentialsProvider());
                tmpClient.addRequestInterceptor(new PreemptiveAuthRequestInterceptor(), 0);
            } else {
                throw new IllegalArgumentException("Cannot configure http connection parameters.");
            }
            return client;
        }

        @Override
        public ClientConnectionManager configureConnectionManager(
                HttpParams params) {

            if (conman == null) {
                SchemeRegistry schemeRegistry = new SchemeRegistry();
                schemeRegistry.register(configureScheme());
                if (enableSSL) {
                    /* In case where the proxy is accessible with a HTTP protocole
                    * and the initial request use HTTPS, scheme registry doen't
                    * recognized the requested Scheme 'http' - Redmine-7235.
                    * An other (better?) solution would be to search https scheme
                    * raiser than http one.
                     */
                    schemeRegistry.register(new Scheme("http", port, PlainSocketFactory.getSocketFactory()));
                }

                PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
                cm.setMaxTotal(maxConnections);
                cm.setDefaultMaxPerRoute(maxConnections);
                conman = cm;
            }

            if (cleanupIdleConnections) {
                IdleConnectionMonitor.monitor(conman);
            }
            return conman;
        }

    }

    private static class SirsFilters {}

    @Filter(name = SIRS_FILTER_NAME, function = SIRS_FILTER)
    private static class SirsFilterRepository extends CouchDbRepositorySupport<SirsFilters> {

        SirsFilterRepository(CouchDbConnector connector) {
            super(SirsFilters.class, connector);
            initStandardDesignDocument();
        }
    }
}
