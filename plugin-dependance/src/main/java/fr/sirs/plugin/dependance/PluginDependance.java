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
package fr.sirs.plugin.dependance;

import static fr.sirs.CorePlugin.createDefaultSmallObjectRule;
import static fr.sirs.CorePlugin.createSelectionSmallObjectRule;
import static fr.sirs.CorePlugin.createDefaultLongRule;
import static fr.sirs.CorePlugin.createSelectionLongRule;
import static fr.sirs.CorePlugin.createDefaultPlanRule;
import static fr.sirs.CorePlugin.createSelectionPlanRule;
import fr.sirs.Plugin;
import fr.sirs.Session;
import fr.sirs.StructBeanSupplier;
import fr.sirs.core.SirsCore;
import static fr.sirs.core.SirsCore.DATE_DEBUT_FIELD;
import static fr.sirs.core.SirsCore.DATE_FIN_FIELD;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.AireStockageDependance;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.AutreDependance;
import fr.sirs.core.model.CheminAccesDependance;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.OuvrageVoirieDependance;
import fr.sirs.core.model.TraitAmenagementHydraulique;
import fr.sirs.core.model.PrestationAmenagementHydraulique;
import fr.sirs.core.model.StructureAmenagementHydraulique;
import fr.sirs.core.model.OuvrageAssocieAmenagementHydraulique;
import fr.sirs.core.model.OrganeProtectionCollective;
import fr.sirs.map.FXMapPane;
import fr.sirs.plugin.dependance.map.DependanceToolBar;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.bean.BeanStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import static org.geotoolkit.style.StyleConstants.DEFAULT_DESCRIPTION;
import static org.geotoolkit.style.StyleConstants.LITERAL_ONE_FLOAT;
import static org.geotoolkit.style.StyleConstants.LITERAL_ZERO_FLOAT;
import static org.geotoolkit.style.StyleConstants.STROKE_CAP_SQUARE;
import static org.geotoolkit.style.StyleConstants.STROKE_JOIN_BEVEL;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.Stroke;
import org.opengis.util.GenericName;

/**
 * Plugin correspondant au module dépendance et AH.
 *
 * @author Alexis Manin (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @author Maxime Gavens (Geomatys)
 */
public class PluginDependance extends Plugin {
    private static final String NAME = "plugin-dependance-ah";
    private static final String TITLE = "Module dépendance et AH";

    //doit avoir la meme valeur que dans le fichier Berge.properties classPlural
    public static final String LAYER_AH_NAME = "Aménagements hydrauliques";
    public static final String LAYER_TRAIT_NAME = "Traits d'aménagement hydraulique";

    private static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
    private static final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;

    private final HashMap<Class, BeanFeatureSupplier> suppliers = new HashMap<>();

    private static FeatureMapLayer aireLayer;
    private static FeatureMapLayer autreLayer;
    private static FeatureMapLayer cheminLayer;
    private static FeatureMapLayer ouvrageVoirieLayer;
    private static FeatureMapLayer amenagementLayer;
    private static FeatureMapLayer traitAmenagementLayer;

    private static FeatureMapLayer desordreLayer;
    private static FeatureMapLayer prestationLayer;
    private static FeatureMapLayer structureLayer;
    private static FeatureMapLayer ouvrageAssocieLayer;
    private static FeatureMapLayer organeProtectionLayer;

    public PluginDependance() {
        name = NAME;
        loadingMessage.set("module dépendance et AH");
        themes.add(new DependancesTheme());
        themes.add(new DescriptionDependanceAHTheme());
    }

    @Override
    public void load() throws Exception {
        getConfiguration();
        loadDataSuppliers();
    }

    @Override
    public CharSequence getTitle() {
        return TITLE;
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public List<ToolBar> getMapToolBars(final FXMapPane mapPane) {
        return Collections.singletonList(new DependanceToolBar(mapPane.getUiMap()));
    }

    @Override
    public List<MapItem> getMapItems() {
        try {
            // Layer colors
            final Color[] colors = new Color[]{
                Color.BLACK,
                Color.BLUE,
                Color.CYAN,
                Color.RED,
                Color.DARK_GRAY,
                Color.GREEN,
                Color.MAGENTA,
                Color.ORANGE,
                Color.PINK,
                Color.LIGHT_GRAY,
                Color.YELLOW,
                Color.GRAY
            };

            // Layer name map
            final Map<String, String> nameMap = new HashMap<>();
            for(Class elementClass : suppliers.keySet()) {
                final LabelMapper mapper = LabelMapper.get(elementClass);
                nameMap.put(elementClass.getSimpleName(), mapper.mapClassName());
            }
            // Replace name of specific layer
            nameMap.put(AmenagementHydraulique.class.getSimpleName(), LAYER_AH_NAME);
            nameMap.put(TraitAmenagementHydraulique.class.getSimpleName(), LAYER_TRAIT_NAME);

            /// Build item
            final MapItem depGroup = MapBuilder.createItem();
            depGroup.setName("Dépendances et AH");
            final BeanStore otherStore = new BeanStore(
                    suppliers.get(OuvrageVoirieDependance.class),
                    suppliers.get(TraitAmenagementHydraulique.class),
                    suppliers.get(CheminAccesDependance.class),
                    suppliers.get(AutreDependance.class),
                    suppliers.get(AireStockageDependance.class),
                    suppliers.get(AmenagementHydraulique.class),
                    suppliers.get(DesordreDependance.class),
                    suppliers.get(PrestationAmenagementHydraulique.class),
                    suppliers.get(StructureAmenagementHydraulique.class),
                    suppliers.get(OuvrageAssocieAmenagementHydraulique.class),
                    suppliers.get(OrganeProtectionCollective.class)
            );
            depGroup.items().addAll(buildLayers(otherStore, nameMap, colors, false));
            depGroup.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            return Collections.singletonList(depGroup);
        } catch (DataStoreException ex) {
            throw new SirsCoreRuntimeException(ex);
        }
    }

    public static FeatureMapLayer getAireLayer() {
        return aireLayer;
    }

    public static FeatureMapLayer getAutreLayer() {
        return autreLayer;
    }

    public static FeatureMapLayer getCheminLayer() {
        return cheminLayer;
    }

    public static FeatureMapLayer getOuvrageVoirieLayer() {
        return ouvrageVoirieLayer;
    }

    public static FeatureMapLayer getDesordreLayer() {
        return desordreLayer;
    }

    public static FeatureMapLayer getAmenagementLayer() {
        return amenagementLayer;
    }

    public static FeatureMapLayer getPrestationLayer() {
        return prestationLayer;
    }

    public static FeatureMapLayer getStructureLayer() {
        return structureLayer;
    }

    public static FeatureMapLayer getOuvrageAssocieLayer() {
        return ouvrageAssocieLayer;
    }

    public static FeatureMapLayer getOrganeProtectionLayer() {
        return organeProtectionLayer;
    }

    @Override
    public Optional<Image> getModelImage() throws IOException {
        final Image image;

        try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/sirs/dependanceModel.png")) {
            image = new Image(in);
        }
        return Optional.of(image);
    }

    private synchronized void loadDataSuppliers() {
        final Set<Class> toLoad = new HashSet<>(Arrays.asList(
                OuvrageVoirieDependance.class,
                TraitAmenagementHydraulique.class,
                CheminAccesDependance.class,
                AutreDependance.class,
                AireStockageDependance.class,
                AmenagementHydraulique.class,
                DesordreDependance.class,
                PrestationAmenagementHydraulique.class,
                StructureAmenagementHydraulique.class,
                OuvrageAssocieAmenagementHydraulique.class,
                OrganeProtectionCollective.class
        ));
        final Function<Class<? extends Element>, StructBeanSupplier> getDefaultSupplierForClass = (Class<? extends Element> c) ->{
            return new StructBeanSupplier(c, () -> getSession().getRepositoryForClass(c).getAllStreaming());
        };

        suppliers.clear();
        toLoad.forEach(clazz -> suppliers.put(clazz, getDefaultSupplierForClass.apply(clazz)));
    }

    private void attributeLayer(final FeatureMapLayer l, final String name) {
        if (OuvrageVoirieDependance.class.getSimpleName().equals(name)) {
            ouvrageVoirieLayer = l;
        } else if (TraitAmenagementHydraulique.class.getSimpleName().equals(name)) {
            traitAmenagementLayer = l;
        } else if (CheminAccesDependance.class.getSimpleName().equals(name)) {
            cheminLayer = l;
        } else if (AutreDependance.class.getSimpleName().equals(name)) {
            autreLayer = l;
        } else if (AireStockageDependance.class.getSimpleName().equals(name)) {
            aireLayer = l;
        } else if (AmenagementHydraulique.class.getSimpleName().equals(name)) {
            amenagementLayer = l;
        } else if (DesordreDependance.class.getSimpleName().equals(name)) {
            desordreLayer = l;
        } else if (PrestationAmenagementHydraulique.class.getSimpleName().equals(name)) {
            prestationLayer = l;
        } else if (StructureAmenagementHydraulique.class.getSimpleName().equals(name)) {
            structureLayer = l;
        } else if (OuvrageAssocieAmenagementHydraulique.class.getSimpleName().equals(name)) {
            ouvrageAssocieLayer = l;
        } else if (OrganeProtectionCollective.class.getSimpleName().equals(name)) {
            organeProtectionLayer = l;
        }
    }

    private MutableStyle attributeSelectionStyle(final String name) {
        if (OuvrageVoirieDependance.class.getSimpleName().equals(name)
                || AutreDependance.class.getSimpleName().equals(name)
                || AmenagementHydraulique.class.getSimpleName().equals(name)
                || DesordreDependance.class.getSimpleName().equals(name)
                || PrestationAmenagementHydraulique.class.getSimpleName().equals(name)
                || StructureAmenagementHydraulique.class.getSimpleName().equals(name)
                || OuvrageAssocieAmenagementHydraulique.class.getSimpleName().equals(name)
                || OrganeProtectionCollective.class.getSimpleName().equals(name)) {
            return createSelectionStyleType3();
        } else if (TraitAmenagementHydraulique.class.getSimpleName().equals(name)) {
            return createSelectionStyleType2();
        } else if (CheminAccesDependance.class.getSimpleName().equals(name)) {
            return createSelectionLineStyle();
        } else if (AireStockageDependance.class.getSimpleName().equals(name)) {
            return createSelectionPlanStyle();
        } else {
            return null;
        }
    }

    private MutableStyle attributeStyle(final Color color, final String name) {
        if (OuvrageVoirieDependance.class.getSimpleName().equals(name)
                || AutreDependance.class.getSimpleName().equals(name)
                || AmenagementHydraulique.class.getSimpleName().equals(name)
                || DesordreDependance.class.getSimpleName().equals(name)
                || PrestationAmenagementHydraulique.class.getSimpleName().equals(name)
                || StructureAmenagementHydraulique.class.getSimpleName().equals(name)
                || OuvrageAssocieAmenagementHydraulique.class.getSimpleName().equals(name)
                || OrganeProtectionCollective.class.getSimpleName().equals(name)) {
            return createStyleType3(color);
        } else if (TraitAmenagementHydraulique.class.getSimpleName().equals(name)) {
            return createTraitAHStyle();
        } else if (CheminAccesDependance.class.getSimpleName().equals(name)) {
            return createLineStyle(color);
        } else if (AireStockageDependance.class.getSimpleName().equals(name)) {
            return createPlanStyle(color);
        } else {
            return null;
        }
    }

    private List<MapLayer> buildLayers(BeanStore store, Map<String,String> nameMap, Color[] colors, boolean visible) throws DataStoreException {
        final List<MapLayer> layers = new ArrayList<>();
        final org.geotoolkit.data.session.Session symSession = store.createSession(false);
        int i = 0;

        for (GenericName name : store.getNames()) {
            final String className = name.tip().toString();
            final FeatureCollection col = symSession.getFeatureCollection(QueryBuilder.all(name));
            final Color color = colors[i % colors.length];
            final MutableStyle style = attributeStyle(color, className);
            final FeatureMapLayer fml = MapBuilder.createFeatureLayer(col, style);

            if(col.getFeatureType().getDescriptor(DATE_DEBUT_FIELD)!=null && col.getFeatureType().getDescriptor(DATE_FIN_FIELD)!=null){
                final FeatureMapLayer.DimensionDef datefilter = new FeatureMapLayer.DimensionDef(
                        CommonCRS.Temporal.JAVA.crs(),
                        GO2Utilities.FILTER_FACTORY.property(DATE_DEBUT_FIELD),
                        GO2Utilities.FILTER_FACTORY.property(DATE_FIN_FIELD)
                );
                fml.getExtraDimensions().add(datefilter);
            }
            fml.setVisible(visible);
            fml.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);

            final String str = nameMap.get(className);
            fml.setName(str!=null ? str : className);

            fml.setSelectionStyle(attributeSelectionStyle(className));

            layers.add(fml);
            i++;
            this.attributeLayer(fml, className);
        }
        return layers;
    }

    private static MutableStyle createStyleType1(final Color color) {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();

        fts.rules().add(createDefaultSmallObjectRule(color, null));
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createStyleType2(final Color color) {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule longRule = createDefaultLongRule(color, null);

        fts.rules().add(longRule);
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createStyleType3(final Color color) {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule longRule          = createDefaultLongRule(color, null);
        final MutableRule smallObjectRule   = createDefaultSmallObjectRule(color, null);
        final MutableRule planRule          = createDefaultPlanRule(color, null);

        longRule.setFilter(
                FF.and(
                        FF.equals(FF.function("geometryType", FF.property("geometry")),FF.literal("LineString")),
                        FF.greater(
                            FF.function("length", FF.property("geometry")),
                            FF.literal(SirsCore.LINE_MIN_LENGTH)
                        )
                )
        );
        smallObjectRule.setFilter(
                FF.lessOrEqual(
                        FF.function("length", FF.property("geometry")),
                        FF.literal(SirsCore.LINE_MIN_LENGTH)
                )
        );
        planRule.setFilter(
                FF.and(
                        FF.equals(FF.function("geometryType", FF.property("geometry")),FF.literal("Polygon")),
                        FF.greater(
                            FF.function("length", FF.property("geometry")),
                            FF.literal(SirsCore.LINE_MIN_LENGTH)
                        )
                )
        );
        fts.rules().add(longRule);
        fts.rules().add(smallObjectRule);
        fts.rules().add(planRule);
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createSelectionStyleType1() {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();

        fts.rules().add(createSelectionSmallObjectRule());
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createSelectionStyleType2() {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule longRule = createSelectionLongRule();

        fts.rules().add(longRule);
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createSelectionStyleType3() {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule longRule          = createSelectionLongRule();
        final MutableRule planRule          = createSelectionPlanRule();
        final MutableRule smallObjectRule   = createSelectionSmallObjectRule();

        longRule.setFilter(
                FF.and(
                        FF.equals(FF.function("geometryType", FF.property("geometry")),FF.literal("LineString")),
                        FF.greater(
                                FF.function("length", FF.property("geometry")),
                                FF.literal(2.0)
                        )
                )
        );
        planRule.setFilter(
                FF.and(
                        FF.equals(FF.function("geometryType", FF.property("geometry")),FF.literal("Polygon")),
                        FF.greater(
                                FF.function("length", FF.property("geometry")),
                                FF.literal(2.0)
                        )
                )
        );
        smallObjectRule.setFilter(
                FF.lessOrEqual(
                        FF.function("length", FF.property("geometry")),
                        FF.literal(2.0)
                )
        );
        fts.rules().add(longRule);
        fts.rules().add(smallObjectRule);
        fts.rules().add(planRule);
        style.featureTypeStyles().add(fts);
        return style;
    }

    private static MutableStyle createTraitAHStyle() {
        final Stroke stroke = SF.stroke(SF.literal(new Color(212, 33, 206)), LITERAL_ONE_FLOAT, FF.literal(1.5),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null, LITERAL_ZERO_FLOAT);
        final LineSymbolizer line1 = SF.lineSymbolizer("symbol",
                (String)null, DEFAULT_DESCRIPTION, Units.POINT, stroke, LITERAL_ONE_FLOAT);

        return SF.style(line1);
    }

    public static MutableStyle createSelectionLineStyle() {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule longRule          = createSelectionLongRule();

        fts.rules().add(longRule);
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createSelectionPlanStyle() {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule planRule          = createSelectionPlanRule();

        fts.rules().add(planRule);
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createLineStyle(final Color color) {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule longRule          = createDefaultLongRule(color, null);

        fts.rules().add(longRule);
        style.featureTypeStyles().add(fts);
        return style;
    }

    public static MutableStyle createPlanStyle(final Color color) {
        final MutableFeatureTypeStyle fts   = SF.featureTypeStyle();
        final MutableStyle style            = SF.style();
        final MutableRule planRule          = createDefaultPlanRule(color, null);

        fts.rules().add(planRule);
        style.featureTypeStyles().add(fts);
        return style;
    }
}
