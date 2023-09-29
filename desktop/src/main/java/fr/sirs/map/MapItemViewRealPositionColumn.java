/**
 *
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

import fr.sirs.CorePlugin;
import fr.sirs.SIRS;
import fr.sirs.core.model.Positionable;
import fr.sirs.ui.Growl;
import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.contexttree.FXMapContextTree;
import org.geotoolkit.map.CollectionMapLayer;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.visitor.ListingColorVisitor;
import org.opengis.style.Style;

/**
 * Column of the {@link FXMapContextTree}'s {@link TreeTableCell} allowing the
 * user to display or not the "real positions" associated with a {@link MapLayer}.
 *
 * @see CorePlugin#createDefaultStyle(java.awt.Color, java.lang.String, boolean)
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class MapItemViewRealPositionColumn extends TreeTableColumn <MapItem, Boolean> {

    private static final Image ICON_REAL_POSITION_VISIBLE   = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CROSSHAIRS, 16, Color.DARK_GRAY),null);
    private static final Image ICON_REAL_POSITION_UNVISIBLE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CROSSHAIRS, 16, Color.LIGHT_GRAY),null);

    private static final Tooltip REAL_POSITION_VISIBLE_TOOLTIP   = new Tooltip("Afficher uniquement les géométries");
    private static final Tooltip REAL_POSITION_UNVISIBLE_TOOLTIP = new Tooltip("Afficher les positions réelles de début et de fin");

    private final static Set<String> LAYERS_IN_REALPOSITION = new HashSet<>();


    public MapItemViewRealPositionColumn() {
        setEditable(true);
        setPrefWidth(26);
        setMinWidth(26);
        setMaxWidth(26);


        setCellValueFactory((TreeTableColumn.CellDataFeatures<MapItem, Boolean> param) -> {
            return new SimpleBooleanProperty(Boolean.FALSE);
        });

        setCellFactory((param) -> new ViewRealPositionCell());
    }

    private static final class ViewRealPositionCell extends TreeTableCell<MapItem, Boolean> {

        /**
         * Image view contained in the cell.
         */
        private final ImageView cellContent = new ImageView();


        public ViewRealPositionCell() {
            setOnMouseClicked(this::mouseClicked);
            itemProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> setLogoTo(newValue));

        }

        private void mouseClicked(MouseEvent event){
            try {
                if (isEditing() && itemProperty().get() != null) {
                    final MapItem mitem = getTreeTableRow().getItem();
                    if (mitem != null) {
                        final boolean newValue = !itemProperty().get();
                        setRealPositionVisible(mitem, newValue);
                        itemProperty().set(newValue);
                    }
                }
            } catch(Exception e) {
                SIRS.LOGGER.warning(e.getMessage());
            }
        }

        private void setLogoTo(final Boolean visible){
                setGraphic(null);
                if(visible!= null) {
                    if (!visible) { //Should never be null
                        setTooltip(REAL_POSITION_UNVISIBLE_TOOLTIP);
                        cellContent.setImage(ICON_REAL_POSITION_UNVISIBLE);
                    } else { //Should never be null
                        setTooltip(REAL_POSITION_VISIBLE_TOOLTIP);
                        cellContent.setImage(ICON_REAL_POSITION_VISIBLE);
                    }
                    setGraphic(cellContent);
                } else {
                   setTooltip( new Tooltip("Impossible d'afficher la position \"réelle\" pour un répertoire du contexte cartographique."));
                   cellContent.setImage(null);
                }
            }

        @Override
        protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                cellContent.setImage(null);
            } else {
                cellContent.setImage(item? ICON_REAL_POSITION_VISIBLE : ICON_REAL_POSITION_UNVISIBLE);
            }
        }
    }

    private static void setRealPositionVisible(final MapItem target, final Boolean visible) {
        ArgumentChecks.ensureNonNull("MapItem", target);
        if (target instanceof MapLayer) {
            final MapLayer maplayer = (MapLayer) target;
            boolean appliable = false;

            if (maplayer instanceof CollectionMapLayer) {
                final CollectionMapLayer collectionmaplayer = (CollectionMapLayer) maplayer;
                final Collection col = collectionmaplayer.getCollection();
                if (!((col == null) || (col.isEmpty()))) {
                    Iterator iterator = col.iterator();
                    Object tested;
                    while (iterator.hasNext()) {
                        tested = iterator.next();
                        if (tested != null) {
                            if (tested instanceof Feature) {
                                Object objt = ((Feature) tested).getUserData().get(BeanFeature.KEY_BEAN);
                                if (objt instanceof Positionable) {
                                    appliable = true;
                                }
                            }
                            break;
                        }
                    }
                }
            }

            if (!appliable) {
                Growl growl = new Growl(Growl.Type.INFO, "La couche cartographique ne différentie pas la position réelle de la position dite projetée");
                growl.showAndFade();
                return;
            }

            if (LAYERS_IN_REALPOSITION.contains(maplayer.getName()) && visible) {
                Growl growl = new Growl(Growl.Type.INFO, "Les positions réelles associées à la couche cartographique ont déjà été affichées.");
                growl.showAndFade();
                return;
            }

            if (visible) {

                LAYERS_IN_REALPOSITION.add(maplayer.getName());

                final Color color = tryFindColorFromPrevious(maplayer.getStyle());

                /* Following commented code tried to only modificate the rules
                     * associated with the real positions. It worked for addition of
                     * the rule but not for the removal. Then we choose to re-create
                     * the full Style.
                 */
                maplayer.setStyle(CorePlugin.createRealPositionStyle(color));
            } else {

                final Color color = tryFindColorFromPrevious(maplayer.getStyle());
                maplayer.setStyle(CorePlugin.createDefaultStyle(color));

                LAYERS_IN_REALPOSITION.remove(maplayer.getName());

            }

            maplayer.setSelectionStyle(CorePlugin.createDefaultSelectionStyle(visible));

        } else {

            for (final MapItem child : target.items()) {
                setRealPositionVisible(child, visible);
            }
        }
    }

    /**
     * Try to find colors used to define the input {@link Style}.
     * Only rgb components are assesd here.
     *
     * @param style
     * @return identified colors. {@link Color#BLACK} if no color was found.
     */
    private static Color tryFindColorFromPrevious(final Style style) {
        ArgumentChecks.ensureNonNull("MutableFeatureTypeStyles", style);

        final ListingColorVisitor visitor = new ListingColorVisitor();
        style.accept(visitor, null);
        final Set<Integer> colors = visitor.getColors();
        final Stream<Color> colorStream;
        if (colors == null) {
            colorStream = Stream.empty();
        } else {
            colorStream = colors.stream().map(integer -> new Color(integer)); //We don't take into account the alpha component to improve comparisaon with white and black colors
        }

        return colorStream
                .filter(c -> !(c.equals(Color.BLACK) || c.equals(Color.WHITE))) //Borders and real positions' items contain black and white colors so we ignore it.
                .findAny()
                .orElse(Color.BLACK);

    }

}
