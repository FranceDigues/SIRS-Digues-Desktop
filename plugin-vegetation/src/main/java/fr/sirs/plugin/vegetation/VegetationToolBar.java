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
package fr.sirs.plugin.vegetation;

import fr.sirs.Injector;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.plugin.vegetation.map.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.edition.EditionTool;
import org.geotoolkit.gui.javafx.render2d.edition.FXToolBox;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.MapBuilder;

import java.util.Iterator;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class VegetationToolBar extends ToolBar {

    private static final String LEFT = "buttongroup-left";
    private static final String CENTER = "buttongroup-center";
    private static final String RIGHT = "buttongroup-right";

    private Stage editionVegetationStage;
    private Stage searchVegetationStage;

    public VegetationToolBar() {
        getStylesheets().add("/org/geotoolkit/gui/javafx/buttonbar.css");
        getItems().add(new Label("Végétation"));

        final Button buttonParcelle = new Button(null, new ImageView(GeotkFX.ICON_EDIT));
        buttonParcelle.disableProperty().bind(Injector.getSession().geometryEditionProperty().not());
        buttonParcelle.setTooltip(new Tooltip("Outil d'édition de végétation"));
        buttonParcelle.getStyleClass().add(LEFT);
        buttonParcelle.setOnAction(this::showEditor);

        final Button recherche = new Button(null, new ImageView(SwingFXUtils.toFXImage(IconBuilder.createImage(
                FontAwesomeIcons.ICON_GEARS_ALIAS, 16, FontAwesomeIcons.DEFAULT_COLOR), null)));
        recherche.setTooltip(new Tooltip("Analyse de la végétation"));
        recherche.setOnAction(this::showSearchDialog);
        recherche.getStyleClass().add(RIGHT);

        getItems().add(new HBox(buttonParcelle, recherche));
    }

    /**
     * Vérifie s'il existe un plan de végétation actif et avertit l'utilisateur au moyen d'une fenêtre informative dans
     * le cas contraire.
     *
     * @return Vrai si la session de végétation indique un plan de gestion actif. Faux dans le cas contraire.
     */
    private boolean checkPlan() {
        //on vérifie qu'il y a un plan de gestion actif
        final PlanVegetation plan = VegetationSession.INSTANCE.planProperty().get();
        if (plan == null) {
            final Dialog dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setContentText("Veuillez activer un plan de gestion avant de commencer l'édition.");
            dialog.showAndWait();
            return false;
        }
        return true;
    }

    private void showEditor(final ActionEvent act) {
        if (!checkPlan()) return;
        if (editionVegetationStage == null) initEditionVegetationStage();
        editionVegetationStage.show();
        editionVegetationStage.setWidth(950);
        editionVegetationStage.setHeight(1000);
        editionVegetationStage.requestFocus();
    }

    private void showSearchDialog(final ActionEvent act) {
        if (!checkPlan()) return;
        if (searchVegetationStage == null)
            searchVegetationStage = createDialog("Recherche de végétation", new FXPlanLayerPane());
        searchVegetationStage.show();
        searchVegetationStage.requestFocus();
    }

    private Stage createDialog(final String title, final Node content) {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.NONE);
        final Scene toolScene = getScene();
        final Window window;
        if (toolScene != null && (window = toolScene.getWindow()) != null) {
            dialog.initOwner(window);
        } else {
            /*
             * If we cannot set it as a child of main frame (making it appear
             * above the main frame), we force its display on the first plan,
             * to avoid user losing it.
             */
            dialog.setAlwaysOnTop(true);
        }
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(title);

        final BorderPane pane = new BorderPane(content);
        pane.setPadding(new Insets(10, 10, 10, 10));

        dialog.setScene(new Scene(pane));
        dialog.setResizable(true);
        dialog.sizeToScene();

        return dialog;
    }

    private void initEditionVegetationStage() {
        final FXMap uiMap = Injector.getSession().getFrame().getMapTab().getMap().getUiMap();
        final FXToolBox toolbox = new FXToolBox(uiMap, MapBuilder.createEmptyMapLayer());

        toolbox.commitRollbackVisibleProperty().setValue(false);
        toolbox.getToolPerRow().set(6);
        toolbox.getTools().add(CreateParcelleTool.SPI);
        toolbox.getTools().add(CreatePeuplementTool.SPI);
        toolbox.getTools().add(CreateInvasiveTool.SPI);
        toolbox.getTools().add(CreateHerbaceTool.SPI);
        toolbox.getTools().add(CreateArbreTool.SPI);
        toolbox.getTools().add(EditVegetationTool.SPI);

        final Iterator<EditionTool.Spi> ite = EditionHelper.getToolSpis();
        while (ite.hasNext()) {
            toolbox.getTools().add(ite.next());
        }

        editionVegetationStage = createDialog("Edition de végétation", toolbox);
        editionVegetationStage.setMinHeight(360);
        editionVegetationStage.setMinWidth(360);
        editionVegetationStage.setOnHidden(evt -> {
            if (uiMap.getHandler() != null && uiMap.getHandler().getClass().getPackage().getName().contains("vegetation"))
                uiMap.setHandler(new FXPanHandler(true));
        });
    }
}
