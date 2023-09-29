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
package fr.sirs.launcher;

import fr.sirs.SIRS;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCore.UpdateInfo;
import static fr.sirs.core.SirsCore.browseURL;
import fr.sirs.core.authentication.SIRSAuthenticator;
import fr.sirs.util.SystemProxySelector;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import org.ektorp.DbAccessException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;

/**
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class Launcher extends Application {

    private static final String EULA_NODE = "user_agreement";
    private static final String EULA_VALUE = "agreed";
    private static final String EULA_VERSION = "version";


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
                if ((e instanceof OutOfMemoryError) || (e.getCause() instanceof OutOfMemoryError)) {
                    try {
                        SirsCore.LOGGER.log(Level.SEVERE, null, e);
                    } finally {
                        System.exit(1);
                    }
                } else {
                    SirsCore.LOGGER.log(Level.SEVERE, "Uncaught error !", e);
                }
            });
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.SEVERE, "Cannot initialize uncaught exception management.", e);
            // We allow starting program without that feature.
        }

        String version = null;
        try {
            version = SIRS.getVersion();
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.WARNING, "Cannot retrieve application version.", e);
        }
        if (version == null)
            version = "";

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setBackground(Background.EMPTY);
        final Label splashLabel = new Label();
        final VBox vbox = new VBox(progressIndicator, splashLabel);
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.CENTER);
        StackPane stackPane = new StackPane(vbox);
        stackPane.setBackground(Background.EMPTY);
        final Scene scene = new Scene(stackPane);
        scene.setFill(null);

        final Stage splashStage = new Stage();
        splashStage.getIcons().add(SIRS.ICON);
        splashStage.setTitle("SIRS "+version);
        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashStage.setScene(scene);

        primaryStage.getIcons().add(SIRS.ICON);
        primaryStage.setTitle("SIRS "+version);
        primaryStage.setOnCloseRequest((WindowEvent event) -> {
            System.exit(0);
        });

        splashStage.show();

        /*
         * Initialize / create EPSG db. A loader is displayed while the task is
         * running, preventing application launch.
         */
        final Task<Boolean> initer = new Initer();
        splashLabel.textProperty().bind(initer.messageProperty());
        splashLabel.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            splashStage.sizeToScene();
        });

        initer.setOnSucceeded((WorkerStateEvent event) -> {
            FXLauncherPane launcherPane;
            try {
                launcherPane = new FXLauncherPane();
                primaryStage.setScene(new Scene(launcherPane));
                splashStage.close();
                primaryStage.show();
            } catch (DbAccessException ex) {
                SirsCore.LOGGER.log(Level.SEVERE, "Problème d'accès au CouchDB, utilisateur n'ayant pas les droits administrateur.", ex);
                GeotkFX.newExceptionDialog("Probléme d'accès à la base de donnée. Vérifier que CouhDB est bien lancé ou que l'utilisateur de la base CouchDB a les bons droits. " +
                        "Réinstaller CouchDB ou supprimer cet utilisateur \"geouser\" des administrateurs de CouchDB, " +
                        "puis relancer l'application.", ex).showAndWait();
                System.exit(1);
            } catch (RuntimeException ex) {
                SirsCore.LOGGER.log(Level.SEVERE, "Impossible d'initialiser le launcher avec l'URL fournie.", ex);
                GeotkFX.newExceptionDialog("Impossible de se connecter au serveur CouchDb local.", ex).showAndWait();
                System.exit(1);
            } catch (IOException ex) {
                SirsCore.LOGGER.log(Level.SEVERE, "Impossible d'initialiser le launcher.", ex);
                GeotkFX.newExceptionDialog(
                        ex instanceof MalformedURLException ?
                        "URL de connexion à la base de données couchDB invalide" :
                        "Impossible de se connecter au serveur CouchDb local.", ex)
                        .showAndWait();
                System.exit(1);
            }
        });

        initer.setOnFailed((WorkerStateEvent event) -> {
            final Throwable tr = event.getSource().getException();
            final String trText;
            if (tr instanceof SecurityException) {
                trText = "Configuration du proxy invalide.";
            } else if (tr instanceof IllegalStateException) {
                trText = "Problème rencontré au chargement d'un plugin, vérifier l'URL fournie";
            } else {
                trText = "Impossible de se connecter à la base EPSG.";
            }

            GeotkFX.newExceptionDialog(trText, event.getSource().getException()).showAndWait();
            System.exit(1);
        });

        initer.setOnCancelled((WorkerStateEvent event) -> {
            System.exit(0);
        });
        TaskManager.INSTANCE.submit(initer);
    }

    @Override
    public void stop() throws Exception {
        SirsCore.getTaskManager().close();
    }

    /**
     * Check if there's any update available on SIRS server. If any, user is asked for download.
     */
    private static boolean checkUpdate() throws InterruptedException, ExecutionException {
        final UpdateInfo info;
        try {
            info = SirsCore.checkUpdate().get();
        } catch (Exception ex) {
            SirsCore.LOGGER.log(Level.WARNING, "Impossible de charger le numéro de version de l'application.", ex);
            return false;
        }
        if (info != null) {
            // Now that we found that an update is available, we can redirect user on package URL.
            final Task<Boolean> askForUpdate = new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {
                    final Alert alert = new Alert(
                            Alert.AlertType.INFORMATION,
                            "Une mise à jour de l'application est disponible (" + info.localVersion + " vers " + info.distantVersion + "). Voulez-vous l'installer ?",
                            ButtonType.NO, ButtonType.YES);
                    alert.setWidth(400);
                    alert.setHeight(300);
                    alert.setResizable(true);
                    final Optional<ButtonType> choice = alert.showAndWait();
                    if (ButtonType.YES.equals(choice.orElse(ButtonType.NO))) {
                        browseURL(info.updateURL, "Mise à jour", true);
                        Thread.sleep(500);
                        // Once downloaded, we stop the system to allow user to install its new package.
                        return true;
                    }
                    return false;
                }
            };
            Platform.runLater(() -> askForUpdate.run());
            // Shutdown program to allow user installing software update without any conflict.
            if (Boolean.TRUE.equals(askForUpdate.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Task which takes care of components initialisation, as EPSG database and plugins.
     */
    private static final class Initer extends Task<Boolean> {

        @Override
        protected Boolean call() throws Exception {
            updateMessage("Analyse des configurations réseau");
            ProxySelector.setDefault(new SystemProxySelector());
            Authenticator.setDefault(new SIRSAuthenticator());

            updateMessage("Vérification des mises à jour");
            boolean updateRequired = checkUpdate();
            if (updateRequired) {
                cancel();
                return false;
            }

            // Ask agreement acceptance while EPSG Database init.
            final Task agreement = new Task() {

                @Override
                protected Object call() throws Exception {
                    acceptEULA();
                    return null;
                }
            };

            SIRS.fxRun(false, agreement);

            updateMessage("Initialisation de la base EPSG");
            SirsCore.initEpsgDB();

            agreement.get();

            updateMessage("Lancement de l'application");
            return true;
        }
    }

    /**
     * Check End User License Agreement. If user has not validated it yet, we
     * display the license until he accepts it.
     */
    private static void acceptEULA() {
        Preferences node = Preferences.userNodeForPackage(Launcher.class).node(EULA_NODE);
        final boolean accepted = node.getBoolean(EULA_VALUE, false);

        // Already accepted EULA
        final String appVersion = SirsCore.getVersion();
        if (accepted && (appVersion == null || appVersion.equals(node.get(EULA_VERSION, "")))) {
            return;
        }

        final String agreement;
        try (final InputStream eula = Launcher.class.getResourceAsStream("CLUF");
                final InputStreamReader iReader = new InputStreamReader(eula, StandardCharsets.UTF_8);
                final BufferedReader reader = new BufferedReader(iReader)) {

            final StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }

            agreement = builder.toString();
        } catch (IOException e) {
            SirsCore.LOGGER.log(Level.SEVERE, "Cannot read EULA.", e);
            new Alert(Alert.AlertType.ERROR, "Impossible d'afficher les conditions d'utilisation. Votre installation est peut-être corrompue.", ButtonType.OK).showAndWait();
            System.exit(1);
            throw new Error(e);
        }

        final TextArea area = new TextArea(agreement);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(451);
        area.setPrefWidth(777);

        final ButtonType accept = new ButtonType("J'accepte");
        final ButtonType refuse = new ButtonType("Je refuse");
        final Alert alert = new Alert(Alert.AlertType.INFORMATION, null,refuse, accept);
        alert.setHeaderText("Veuillez accepter le contrat d'utilisation du programme avant de continuer.");
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.getDialogPane().setContent(area);

        if (accept.equals(alert.showAndWait().orElse(refuse))) {
            if (appVersion != null) {
                node.put(EULA_VERSION, appVersion);
            }
            node.putBoolean(EULA_VALUE, true);

        } else {
            System.exit(1);
        }
    }
}
