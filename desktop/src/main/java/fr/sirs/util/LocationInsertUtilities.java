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
package fr.sirs.util;

import fr.sirs.*;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.property.SirsPreferences;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.style.DefaultFont;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.util.NamesExt;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.*;
import org.opengis.util.GenericName;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import static fr.sirs.core.SirsCore.DATE_DEBUT_FIELD;
import static fr.sirs.core.SirsCore.DATE_FIN_FIELD;

/**
 * <p>This class provides utilities for two purposes:</p>
 * <ul>
 * <li>increasing the @{@link Symbolizer} size prior printing the Fiches</li>
 * <li>hiding the archived @{@link Element} prior printing</li>
 * <li>restoring the @{@link Symbolizer} size after printing the Fiches</li>
 * <li>restoring the visibility of the archived @{@link Element} after printing</li>
 * </ul>
 * @author Estelle Idée (Geomatys)
 */
public class LocationInsertUtilities {

    private final static Logger LOG = Logger.getLogger(LocationInsertUtilities.class.getName());
    private static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
    private static final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;

    /**
     * Filter to remove all the archived elements.
     * Used when the user has selected the app option (in Préférences)
     * to hide all the archived parents.
     */
    private static final  Filter FILTER_NOT_ARCHIVED = FF.and(
            FF.or(
                    FF.isNull(FF.property(DATE_FIN_FIELD)),
                    FF.greater(FF.property(DATE_FIN_FIELD), FF.literal(LocalDate.now()))),
            FF.or(
                    FF.isNull(FF.property(DATE_DEBUT_FIELD)),
                    FF.lessOrEqual(FF.property(DATE_DEBUT_FIELD), FF.literal(LocalDate.now()))));

    /**
     *  Modify the SelectionStyle size on a specific layer
     * @param multiplier how much the symbols size should be modified by
     * @param e to get the layer that should be modified
     * @return the original style prior modification - to be used to restore the layer back to normal after printing
     */
    protected static MutableStyle modifySelectionSymbolSize(final double multiplier, final Element e) {
        ArgumentChecks.ensureNonNull("Element", e);
        final MapLayer layer = CorePlugin.getMapLayerForElement(e);
        //create new style to replace the old one
        return modifyLayerSymbolsSizeAndGetBackUpStyle(layer, multiplier, true);
    }

    /**
     * Modify the Style size on visible layers of the context
     * Method used prior printing Fiches
     *
     * @param multiplier how much the symbols size should be modified by
     * @return Map of @{@link MutableStyle} of the modified layers prior modification : to be used to restore the layers back to normal after printing
     */
    protected static Map<MapLayer, MutableStyle> modifyContextSymbolsSizeAndGetBackUpStyles(final double multiplier) {
        final List<MapLayer> layers = CorePlugin.getMapLayers();
        final Map<MapLayer, MutableStyle> backUpStyles = new HashMap<>();

        layers.stream().filter(MapItem::isVisible).forEach(l ->
            backUpStyles.put(l, modifyLayerSymbolsSizeAndGetBackUpStyle(l, multiplier, false)));
        return backUpStyles;
    }

    /**
     * Modify a layer style or selectionStyle by modifying the size of @{@link PointSymbolizer},
     * @{@link LineSymbolizer} and @{@link TextSymbolizer}
     *
     * @param layer that will be modified
     * @param multiplier how much the symbols size should be modified by
     * @param selectionStyle to modify the layer style or selectionStyle
     * @return the initial layer's @MutableStyle prior being modified - to be used to restore style afterwards
     */
    private static MutableStyle modifyLayerSymbolsSizeAndGetBackUpStyle(final MapLayer layer, final double multiplier,
                                                                        final boolean selectionStyle) {
        ArgumentChecks.ensureNonNull("layer to modify", layer);

        //create new style to replace the old one
        final MutableFeatureTypeStyle newFts = SF.featureTypeStyle();

        final MutableStyle currentStyle = selectionStyle ? layer.getSelectionStyle() : layer.getStyle();
        MutableStyle backupStyle = null;
        boolean styleModified = false;
        if (currentStyle != null && currentStyle.featureTypeStyles() != null && !currentStyle.featureTypeStyles().isEmpty()) {
            Symbolizer sym;
            Symbolizer[] copiedSymbolizers;
            backupStyle = currentStyle;
            for (final FeatureTypeStyle ftStyle : currentStyle.featureTypeStyles()) {
                if (ftStyle != null && ftStyle.rules() != null && !ftStyle.rules().isEmpty()) {
                    for (int ri = 0; ri < ftStyle.rules().size(); ri++) {
                        final List<? extends Symbolizer> rSym = ftStyle.rules().get(ri).symbolizers();
                        if (rSym != null && !rSym.isEmpty()) {
                            copiedSymbolizers = rSym.toArray(new Symbolizer[rSym.size()]);
                            for (int i = 0; i < rSym.size(); i++) {
                                sym = rSym.get(i);

                                if (sym instanceof PointSymbolizer) {
                                    styleModified = true;
                                    copiedSymbolizers[i] = increasePointSymbolizerSize((PointSymbolizer) sym, multiplier);
                                }
                                if (sym instanceof LineSymbolizer) {
                                    styleModified = true;
                                    copiedSymbolizers[i] = increaseLineSymbolizerSize((LineSymbolizer) sym, multiplier);

                                } else if (sym instanceof TextSymbolizer) {
                                    styleModified = true;
                                    copiedSymbolizers[i] = increaseTextSymbolizerSize((TextSymbolizer) sym, multiplier);
                                } else {
                                    // we only want the log for debugging purpose
                                    LOG.log(Level.FINER, "This type of Symbolizer is not modified : " + sym.getClass().getSimpleName());
                                }
                            }
                            newFts.rules().add(SF.rule(copiedSymbolizers));
                        }
                    }
                }
            }
        }
        final MutableStyle style = SF.style();
        style.featureTypeStyles().add(newFts);

        if (selectionStyle) layer.setSelectionStyle(style);
        else layer.setStyle(style);

        // if the layer's style has not been modified, it's removed from the returned Map
        if (styleModified) {
            return backupStyle;
        } else
            return null;
    }

    /**
     * create a copy of a @{@link PointSymbolizer} after modifying its graphic size
     *
     * @param sym @{@link PointSymbolizer} to copy
     * @param multiplier how much to multiply the graphic size by
     * @return the created @{@link PointSymbolizer} with new size
     */
    private static PointSymbolizer increasePointSymbolizerSize(final PointSymbolizer sym, final double multiplier) {
        ArgumentChecks.ensureNonNull("PointSymbolizer to modify", sym);

        Graphic tmpGraphic = sym.getGraphic();
        tmpGraphic = SF.graphic(tmpGraphic.graphicalSymbols(),
                tmpGraphic.getOpacity(),
                FF.literal(Double.parseDouble(tmpGraphic.getSize().toString()) * multiplier),
                tmpGraphic.getRotation(),
                tmpGraphic.getAnchorPoint(),
                tmpGraphic.getDisplacement());
        return SF.pointSymbolizer(sym.getName(), sym.getGeometry(), sym.getDescription(), sym.getUnitOfMeasure(), tmpGraphic);
    }

    /**
     * create a copy of a @{@link LineSymbolizer} after modifying its line width
     *
     * @param sym @{@link LineSymbolizer} to copy
     * @param multiplier how much to multiply the line width by
     * @return the created @{@link LineSymbolizer} with new width
     */
    private static LineSymbolizer increaseLineSymbolizerSize(final LineSymbolizer sym, final double multiplier) {
        ArgumentChecks.ensureNonNull("LineSymbolizer to modify", sym);
        Stroke tmpStroke = sym.getStroke();
        tmpStroke = SF.stroke(tmpStroke.getColor(),
                tmpStroke.getOpacity(),
                FF.literal(Double.parseDouble(tmpStroke.getWidth().toString()) * multiplier),
                tmpStroke.getLineJoin(),
                tmpStroke.getLineCap(),
                tmpStroke.getDashArray(),
                tmpStroke.getDashOffset());
        return SF.lineSymbolizer(sym.getName(), sym.getGeometry(), sym.getDescription(), sym.getUnitOfMeasure(), tmpStroke, sym.getPerpendicularOffset());
    }

    /**
     * create a copy of a @{@link TextSymbolizer} after modifying its font size
     *
     * @param sym @{@link TextSymbolizer} to copy
     * @param multiplier how much to multiply the font size by
     * @return the created @{@link TextSymbolizer} with new font size
     */
    private static TextSymbolizer increaseTextSymbolizerSize(final TextSymbolizer sym, final double multiplier) {
        ArgumentChecks.ensureNonNull("TextSymbolizer to modify", sym);
        Font font = sym.getFont();
        return SF.textSymbolizer(sym.getFill(),
                new DefaultFont(font.getFamily(),
                        font.getStyle(),
                        font.getWeight(),
                        FF.literal(Double.parseDouble(font.getSize().toString()) * multiplier)),
                SF.halo(sym.getHalo().getFill(),
                        FF.literal(Double.parseDouble(sym.getHalo().getRadius().toString()) * multiplier)),
                FF.property("libelle"),
                sym.getLabelPlacement(), null);
    }

    /**
     * Restore the context layers style
     * @param backUpStyles map with the layers' styles to be restored
     */
    protected static void changeBackLayersSymbolSize(final Map<MapLayer, MutableStyle> backUpStyles) {
        if (backUpStyles == null || backUpStyles.isEmpty()) return;
        final List<MapLayer> layers = CorePlugin.getMapLayers();
        MutableStyle toRestore;
        for (MapLayer layer : layers) {
            toRestore = backUpStyles.get(layer);
            if (toRestore != null) layer.setStyle(toRestore);

        }
    }

    /**
     * Restore the SelectionStyle for an @{@link Element} layer
     * @param e element for which the layer should be modified
     * @param backUpSelectionStyle the selectionStyle to be restored
     */
    protected static void changeBackSelectionSymbolSize(final Element e, final MutableStyle backUpSelectionStyle) {
        if (backUpSelectionStyle != null) {
            final MapLayer layer = CorePlugin.getMapLayerForElement(e);
            layer.setSelectionStyle(backUpSelectionStyle);
        }
    }

    /**
     * To hide the archived elements because the extraDimension would also hide the selected archived elements
     * @param elementsToShow list of selected elements to print
     * @return the backup queries to restore the queries after printing
     */
    protected static Map<FeatureMapLayer, Query> hideArchivedElements(final List<Objet> elementsToShow) {
        final List<FeatureMapLayer> fml = new ArrayList<>();
        final List<MapLayer> layers = CorePlugin.getMapLayers();
        // to collect backup queries
        final Map<FeatureMapLayer, Query> oldQueries = new HashMap<>();

        // to collect all layers containing Elements than can be archived
        for (MapLayer l : layers) {
            if (l instanceof FeatureMapLayer && l.isVisible() && !((FeatureMapLayer) l).getCollection().isEmpty()) {
                if (((FeatureMapLayer) l).getCollection().iterator().next().getUserData().get("bean") instanceof AvecBornesTemporelles)
                    fml.add((FeatureMapLayer) l);
            }
        }
        if (!fml.isEmpty()) {
            final TronconDigueRepository tronconRepository = (TronconDigueRepository) Injector.getSession().getRepositoryForClass(TronconDigue.class);
            // collect the documentIds of the troncons the elements are linked to
            final List<String> tronconsIds = new ArrayList<>();
            if (elementsToShow != null && !elementsToShow.isEmpty()) {
                for (Objet e : elementsToShow) {
                    String linearId = e.getLinearId();
                    if (linearId != null) {
                        if (tronconRepository.get(linearId) != null) {
                            tronconsIds.add(tronconRepository.get(linearId).getDocumentId());
                        }
                    }
                }
            }
            Query currentQuery;
            GenericName typeName;
            Filter filter;
            QueryBuilder queryBuilder;
            GeometryDescriptor geomDescriptor;
            GenericName[] genericNames;
            for (FeatureMapLayer layer : fml) {
                currentQuery = layer.getQuery();
                oldQueries.put(layer, currentQuery);
                FeatureType ft = layer.getCollection().getFeatureType();
                typeName = ft.getName();
                filter = FILTER_NOT_ARCHIVED;
                // the archived selected elements should be visible on the location insert
                if (!tronconsIds.isEmpty() && elementsToShow.get(0).getClass().getSimpleName().equalsIgnoreCase(typeName.toString())) {
                    for (String tId : tronconsIds) {
                        filter = FF.or(
                                FF.equals(FF.property("linearId"), FF.literal(tId)),
                                filter);
                    }
                }
                // the tronçons of the selected elements should be visible on the location insert even though they are archived
                else if (layer.getName().equalsIgnoreCase(CorePlugin.TRONCON_LAYER_NAME) && !tronconsIds.isEmpty()) {
                    // HACK-REDMINE-4408 : hide archived Tronçons depending on preference option
                    if (!SirsPreferences.getHideArchivedProperty()) {
                        filter = null;
                    }
                }
                queryBuilder = new QueryBuilder(
                        NamesExt.create(typeName.scope().toString(), typeName.head().toString()));
                if (filter != null)
                    queryBuilder.setFilter(filter);
                geomDescriptor = ft.getGeometryDescriptor();
                if (geomDescriptor != null) {
                    genericNames = new GenericName[]{geomDescriptor.getName()};
                    queryBuilder.setProperties(genericNames);
                }
                layer.setQuery(queryBuilder.buildQuery());
            }
        }
        return oldQueries;
    }

    /**
     * Method to restore the initial layers' queries prior hiding archived elements
     * @param backupQueries the output of the method @hideArchivedElements
     */
    protected static void showBackArchivedElements(final Map<FeatureMapLayer, Query>  backupQueries){
        if (backupQueries != null && !backupQueries.isEmpty()) {
            final List<MapLayer> layers = CorePlugin.getMapLayers();
            Query toRestore;
            for (MapLayer layer : layers) {
                toRestore = backupQueries.get(layer);
                if (toRestore != null) ((FeatureMapLayer) layer).setQuery(toRestore);
            }
        }
    }
}
