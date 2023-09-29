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
package fr.sirs.util;

import fr.sirs.SIRS;
import fr.sirs.core.SirsCore;
import fr.sirs.core.authentication.AuthenticationWallet;
import fr.sirs.ui.FXBasemapEditor;
import fr.sirs.ui.FXDocumentRootEditor;
import fr.sirs.ui.FXSirsPreferenceEditor;
import java.util.logging.Level;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.util.ArrayList;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;

/**
 * Une fenêtre permettant d'éditer les préferences de l'application
 * (installation locale).
 *
 * TODO : replace spacing rules by CSS, and add styling rules.
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXPreferenceEditor extends Stage {

    final BorderPane root = new BorderPane();

    final Button cancelBtn = new Button("Annuler");

    final Button saveBtn = new Button("Appliquer");

    final Button okBtn = new Button("OK");

    final ProgressBar uiProgress = new ProgressBar();

    final ArrayList<SaveableConfiguration> configurations = new ArrayList<>();

    public FXPreferenceEditor() {
        root.getStylesheets().add(SIRS.CSS_PATH);
        setScene(new Scene(root));
        root.setPadding(new Insets(5));

        setTitle("Préférences");
        initializeCenter();
        initializeBottom();
    }

    private synchronized void clear() {
        root.setCenter(null);
        configurations.clear();
    }

    private void initializeBottom() {
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction((ActionEvent e) -> {
            close();
            initializeCenter();
        });

        saveBtn.setOnAction((ActionEvent e) -> this.save(false));
        okBtn.setOnAction((ActionEvent e) -> this.save(true));

        uiProgress.setVisible(false);
        final HBox btnBar = new HBox(uiProgress, cancelBtn, saveBtn, okBtn);
        HBox.setHgrow(uiProgress, Priority.ALWAYS);
        btnBar.setSpacing(10);
        btnBar.setPadding(new Insets(10));
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        root.setBottom(btnBar);
    }

    private synchronized void initializeCenter() {
        clear();

        final TabPane tPane = new TabPane();
        final FXSirsPreferenceEditor sirsEditor = new FXSirsPreferenceEditor();
        final FXDocumentRootEditor docConfig = new FXDocumentRootEditor();
        final FXBasemapEditor basemapConfig = new FXBasemapEditor();
        tPane.getTabs().addAll(
                new Tab(sirsEditor.getTitle(), sirsEditor),
                new Tab(docConfig.getTitle(), docConfig),
                new Tab(basemapConfig.getTitle(), basemapConfig)
        );

        configurations.add(sirsEditor);
        configurations.add(docConfig);
        configurations.add(basemapConfig);

        AuthenticationWallet wallet = AuthenticationWallet.getDefault();
        if (wallet != null) {
            final FXAuthenticationWalletEditor authEditor = new FXAuthenticationWalletEditor(wallet);
            tPane.getTabs().add(new Tab(authEditor.getTitle(), authEditor));
            configurations.add(authEditor);
        }

        root.setCenter(tPane);
    }

    /**
     * Save all preferences of registered tabs.
     * @param closeOnSuccess If the stage should close once all preferences have been saved.
     */
    private synchronized void save(final boolean closeOnSuccess) {
        final Task saveTask = new TaskManager.MockTask("Sauvegarde", () -> {
            for (final  SaveableConfiguration conf : configurations) {
                try {
                    conf.save();
                } catch (Exception e) {
                    Platform.runLater(() -> GeotkFX.newExceptionDialog(
                            "Les préférences ne peuvent être sauvegardées pour l'onglet ".concat(conf.getTitle()), e)
                            .show());
                }
            }
        });

        uiProgress.visibleProperty().bind(saveTask.runningProperty());
        saveBtn.disableProperty().bind(saveTask.runningProperty());
        okBtn.disableProperty().bind(saveTask.runningProperty());
        cancelBtn.disableProperty().bind(saveTask.runningProperty());

        saveTask.setOnFailed(evt -> Platform.runLater(() -> {
            GeotkFX.newExceptionDialog("Les préférences ne peuvent être sauvegardées.", saveTask.getException()).show();
            SirsCore.LOGGER.log(Level.SEVERE, "Preferences cannot be saved.", saveTask.getException());
        }));

        if (closeOnSuccess) {
            saveTask.setOnSucceeded(evt -> Platform.runLater(() -> close()));
        }

        TaskManager.INSTANCE.submit(saveTask);
    }
}
