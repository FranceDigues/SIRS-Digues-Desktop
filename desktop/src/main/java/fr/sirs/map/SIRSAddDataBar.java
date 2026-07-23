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
package fr.sirs.map;

import fr.sirs.SIRS;

import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.gui.javafx.render2d.FXAddDataBar;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.MutableStyle;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Graphic;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.Rule;
import org.opengis.style.Symbolizer;

/**
 * A toolbar with a single button which allow to display a form to add new data on map.
 * See {@link FXAddDataBar} for original data import panel.
 *
 * @author Alexis Manin (Geomatys)
 */
public class SIRSAddDataBar extends ToolBar implements ListChangeListener<MapLayer> {

    private final FXMap map;
    private final Stage importStage = new Stage();
    private final FXDataImportPane importPane;

    public SIRSAddDataBar(FXMap map) {
        super();
        ArgumentChecks.ensureNonNull("Input map", map);
        this.map = map;

        getStylesheets().add("/org/geotoolkit/gui/javafx/buttonbar.css");

        final Button button = new Button(null, new ImageView(GeotkFX.ICON_ADD));
        button.setTooltip(new Tooltip("Ajouter une donnée sur la carte"));
        button.setOnAction(this::onAction);
        getItems().add(button);

        /*
         * INIT DIALOG FOR LAYER CHOICE.
         */
        importStage.getIcons().add(SIRS.ICON);
        importStage.setTitle("Importer une donnée");
        importStage.setResizable(true);
        importStage.initModality(Modality.NONE);
        importStage.initOwner(map.getScene() != null? map.getScene().getWindow() : null);

        importStage.setMaxWidth(Double.MAX_VALUE);
        importStage.sizeToScene();

        importPane = new FXDataImportPane();
        importPane.configurationProperty().addListener((observable, oldValue, newValue) -> importStage.sizeToScene());
        importStage.setScene(new Scene(importPane));

        // We listen on import panel to be noticed when new layers are added.
        importPane.mapLayers.addListener(this);
    }

    private void onAction(ActionEvent e) {
        importPane.forceRecreateParameters(null);
        importStage.show();
        importStage.requestFocus();
    }

    @Override
    public void onChanged(Change<? extends MapLayer> c) {
        if (map.getContainer() != null && map.getContainer().getContext() != null) {
            List<MapItem> items = map.getContainer().getContext().items();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (final MapLayer layer : c.getAddedSubList()) {
                        replaceAnchor(layer);
                        items.add(layer);
                    }
                }
            }
        }
        importStage.hide();
    }

    private static void replaceAnchor(final MapLayer layer) {
        final MutableStyle style = layer.getStyle();
        if (style != null && style.featureTypeStyles() != null && !style.featureTypeStyles().isEmpty()) {
            Symbolizer sym;
            PointSymbolizer tmpPoint;
            Graphic tmpGraphic;
            for (final FeatureTypeStyle ftStyle : style.featureTypeStyles()) {
                for (final Rule r : ftStyle.rules()) {
                    for (int i = 0 ; i < r.symbolizers().size() ; i++) {
                        sym = r.symbolizers().get(i);
                        if (sym instanceof PointSymbolizer) {
                            tmpPoint = (PointSymbolizer) sym;
                            tmpGraphic = tmpPoint.getGraphic();
                            tmpGraphic = GO2Utilities.STYLE_FACTORY.graphic(
                                    tmpGraphic.graphicalSymbols(),
                                    tmpGraphic.getOpacity(),
                                    tmpGraphic.getSize(),
                                    tmpGraphic.getRotation(),
                                    GO2Utilities.STYLE_FACTORY.anchorPoint(0.5, 0.5),
                                    tmpGraphic.getDisplacement());

                            r.symbolizers().remove(i);
                            ((List) r.symbolizers()).add(i, GO2Utilities.STYLE_FACTORY.pointSymbolizer(
                                    tmpPoint.getName(),
                                    tmpPoint.getGeometry(),
                                    tmpPoint.getDescription(),
                                    tmpPoint.getUnitOfMeasure(),
                                    tmpGraphic));
                        }
                    }
                }
            }
        }
    }
}
