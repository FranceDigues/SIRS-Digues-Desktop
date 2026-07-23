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
package fr.sirs.plugin.berge;

import fr.sirs.CorePlugin;
import fr.sirs.Plugin;
import fr.sirs.Session;
import fr.sirs.StructBeanSupplier;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.*;
import fr.sirs.util.LinearIdFilterFunction;
import fr.sirs.map.FXMapPane;
import fr.sirs.plugin.berge.map.BergeToolBar;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;

import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import org.apache.sis.measure.Units;
import org.geotoolkit.cql.CQLException;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.bean.BeanStore;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;

import static fr.sirs.CorePlugin.*;
import static org.geotoolkit.style.StyleConstants.DEFAULT_DESCRIPTION;
import static org.geotoolkit.style.StyleConstants.LITERAL_ONE_FLOAT;
import static org.geotoolkit.style.StyleConstants.LITERAL_ZERO_FLOAT;
import static org.geotoolkit.style.StyleConstants.STROKE_CAP_SQUARE;
import static org.geotoolkit.style.StyleConstants.STROKE_JOIN_BEVEL;

import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.Stroke;

/**
 * Minimal example of a plugin.
 *
 * @author Alexis Manin (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
public class PluginBerge extends Plugin {
    private static final String NAME = "plugin-berge";
    private static final String TITLE = "Module berge";

    private static final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;
    private static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;

    public PluginBerge() {
        name = NAME;
        loadingMessage.set("module berge");
        themes.add(new SuiviBergeTheme());
        themes.add(new StructuresDescriptionTheme());
    }

    @Override
    public void load() throws Exception {
        getConfiguration();
    }

    @Override
    public CharSequence getTitle() {
        return TITLE;
    }

    @Override
    public Image getImage() {
        // TODO: choisir une image pour ce plugin
        return null;
    }

    @Override
    public List<ToolBar> getMapToolBars(final FXMapPane mapPane) {
        return Collections.singletonList(new BergeToolBar(mapPane.getUiMap()));
    }

    //doit avoir la meme valeur que dans le fichier Berge.properties classPlural
    public static final String LAYER_BERGE_NAME = "Berges";
    public static final String LAYER_TRAIT_NAME = "Traits de berge";

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
                Color.RED
            };

    private final HashMap<Class, BeanFeatureSupplier> suppliers = new HashMap<>();

    private synchronized void loadDataSuppliers() {
        suppliers.clear();

        final Function<Class<? extends Element>, StructBeanSupplier> getDefaultSupplierForClass = (Class<? extends Element> c) ->{
            return new StructBeanSupplier(c, () -> getSession().getRepositoryForClass(c).getAllStreaming());
        };

        suppliers.put(Berge.class, getDefaultSupplierForClass.apply(Berge.class));
        suppliers.put(TraitBerge.class, getDefaultSupplierForClass.apply(TraitBerge.class));

        //
        suppliers.put(PiedBerge.class, getDefaultSupplierForClass.apply(PiedBerge.class));
        suppliers.put(SommetBerge.class, getDefaultSupplierForClass.apply(SommetBerge.class));
        suppliers.put(EpiBerge.class, getDefaultSupplierForClass.apply(EpiBerge.class));
        suppliers.put(TalusBerge.class, getDefaultSupplierForClass.apply(TalusBerge.class));
        suppliers.put(OuvrageRevancheBerge.class, getDefaultSupplierForClass.apply(OuvrageRevancheBerge.class));

        suppliers.put(Desordre.class, getDefaultSupplierForClass.apply(Desordre.class));
        suppliers.put(Prestation.class, getDefaultSupplierForClass.apply(Prestation.class));

        // Mesures d'événements hydro
        suppliers.put(LaisseCrue.class, getDefaultSupplierForClass.apply(LaisseCrue.class));
        suppliers.put(MonteeEaux.class, getDefaultSupplierForClass.apply(MonteeEaux.class));
        suppliers.put(LigneEau.class, getDefaultSupplierForClass.apply(LigneEau.class));

        // Réseaux et voiries
        suppliers.put(VoieAcces.class, getDefaultSupplierForClass.apply(VoieAcces.class));
        suppliers.put(OuvrageFranchissement.class, getDefaultSupplierForClass.apply(OuvrageFranchissement.class));
        suppliers.put(OuvertureBatardable.class, getDefaultSupplierForClass.apply(OuvertureBatardable.class));
        suppliers.put(VoieDigue.class, getDefaultSupplierForClass.apply(VoieDigue.class));
        suppliers.put(OuvrageVoirie.class, getDefaultSupplierForClass.apply(OuvrageVoirie.class));

        // Réseaux et ouvrages
        suppliers.put(StationPompage.class, getDefaultSupplierForClass.apply(StationPompage.class));
        suppliers.put(ReseauHydrauliqueFerme.class, getDefaultSupplierForClass.apply(ReseauHydrauliqueFerme.class));
        suppliers.put(OuvrageHydrauliqueAssocie.class, getDefaultSupplierForClass.apply(OuvrageHydrauliqueAssocie.class));
        suppliers.put(ReseauTelecomEnergie.class, getDefaultSupplierForClass.apply(ReseauTelecomEnergie.class));
        suppliers.put(OuvrageTelecomEnergie.class, getDefaultSupplierForClass.apply(OuvrageTelecomEnergie.class));
        suppliers.put(ReseauHydrauliqueCielOuvert.class, getDefaultSupplierForClass.apply(ReseauHydrauliqueCielOuvert.class));
        suppliers.put(OuvrageParticulier.class, getDefaultSupplierForClass.apply(OuvrageParticulier.class));
        suppliers.put(EchelleLimnimetrique.class, getDefaultSupplierForClass.apply(EchelleLimnimetrique.class));

        // Documents
        suppliers.put(PositionDocument.class, getDefaultSupplierForClass.apply(PositionDocument.class));
        suppliers.put(PositionProfilTravers.class, getDefaultSupplierForClass.apply(PositionProfilTravers.class));
        suppliers.put(ProfilLong.class, getDefaultSupplierForClass.apply(ProfilLong.class));

    }


    @Override
    public List<MapItem> getMapItems() {
        loadDataSuppliers();
        try {
            final MapItem container = MapBuilder.createItem();
            container.setName("Module berges");

            final BeanStore bergeStore = new BeanStore(suppliers.get(Berge.class));
            final BeanStore traitStore = new BeanStore(suppliers.get(TraitBerge.class));

            container.items().addAll(CorePlugin.buildLayers(bergeStore, LAYER_BERGE_NAME,
                    createBergeStyle(), createTronconSelectionStyle(false),true));
            container.items().addAll(CorePlugin.buildLayers(traitStore, LAYER_TRAIT_NAME,
                    createTraitBergeStyle(), createTronconSelectionStyle(false),true));

            final Map<String, String> nameMap = new HashMap<>();
            for(Class elementClass : suppliers.keySet()) {
                final LabelMapper mapper = LabelMapper.get(elementClass);
                nameMap.put(elementClass.getSimpleName(), mapper.mapClassName());
            }

            final Map<Class<? extends AbstractPositionDocument>, List<Class>> mapDesTypesDeDocs = getTypesDeDocsMap(nameMap);

            // Redmine 7631 : show positionable for Berge that are same type of Objects as for TronconDigue.
            // Use dynamic filter that evaluates at render time to include newly created Berge objects.
            final Filter filter = FF.equals(
                    FF.function(LinearIdFilterFunction.NAME, FF.literal(Berge.class.getCanonicalName())),
                    FF.literal(true)
            );

            final MapItem structLayer = MapBuilder.createItem();
            structLayer.setName("Autre (berge)");
            final BeanStore otherStore = new BeanStore(suppliers.get(PiedBerge.class),
                    suppliers.get(SommetBerge.class),
                    suppliers.get(EpiBerge.class),
                    suppliers.get(TalusBerge.class),
                    suppliers.get(OuvrageRevancheBerge.class));
            structLayer.items().addAll(buildLayers(otherStore, nameMap, colors, createDefaultSelectionStyle(), false));
            structLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            container.items().add(structLayer);

            // Réseaux de voirie
            final BeanStore rvStore = new BeanStore(
                    suppliers.get(VoieAcces.class),
                    suppliers.get(OuvrageFranchissement.class),
                    suppliers.get(OuvertureBatardable.class),
                    suppliers.get(VoieDigue.class),
                    suppliers.get(OuvrageVoirie.class));
            final MapItem rvLayer = MapBuilder.createItem();
            rvLayer.setName("Réseaux de voirie");
            rvLayer.items().addAll( buildLayers(rvStore, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
            rvLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            container.items().add(rvLayer);

            // Réseaux et ouvrages
            final BeanStore roStore = new BeanStore(
                    suppliers.get(StationPompage.class),
                    suppliers.get(ReseauHydrauliqueFerme.class),
                    suppliers.get(OuvrageHydrauliqueAssocie.class),
                    suppliers.get(ReseauTelecomEnergie.class),
                    suppliers.get(OuvrageTelecomEnergie.class),
                    suppliers.get(ReseauHydrauliqueCielOuvert.class),
                    suppliers.get(OuvrageParticulier.class),
                    suppliers.get(EchelleLimnimetrique.class));
            final MapItem roLayer = MapBuilder.createItem();
            roLayer.setName("Réseaux et ouvrages");
            roLayer.items().addAll( buildLayers(roStore, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
            roLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            container.items().add(roLayer);

            // Désordres
            final MapItem desordresLayer = MapBuilder.createItem();
            desordresLayer.setName("Désordres");
            final BeanStore desordreStore = new BeanStore(suppliers.get(Desordre.class));
            desordresLayer.items().addAll( buildLayers(desordreStore, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
            desordresLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            container.items().add(desordresLayer);

            desordresLayer.items().add(createAndAddUrgenceGroup(Desordre.class, desordreStore, null, createDefaultSelectionStyle()));

            // Prestations
            final BeanStore prestaStore = new BeanStore(suppliers.get(Prestation.class));
            final MapItem prestaLayer = MapBuilder.createItem();
            prestaLayer.setName("Prestations");
            prestaLayer.items().addAll( buildLayers(prestaStore, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
            prestaLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            container.items().add(prestaLayer);

            // Mesures d'événements
            final BeanStore mesuresStore = new BeanStore(
                    suppliers.get(LaisseCrue.class),
                    suppliers.get(MonteeEaux.class),
                    suppliers.get(LigneEau.class));
            final MapItem mesuresLayer = MapBuilder.createItem();
            mesuresLayer.setName("Mesures d'événement hydro");
            mesuresLayer.items().addAll( buildLayers(mesuresStore, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
            mesuresLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            container.items().add(mesuresLayer);

            // Positionnement des documents
            final BeanStore documentsStore = new BeanStore(
                    suppliers.get(PositionDocument.class),
                    suppliers.get(PositionProfilTravers.class),
                    suppliers.get(ProfilLong.class));
            final MapItem documentsLayer = MapBuilder.createItem();
            documentsLayer.setName("Documents");
            documentsLayer.items().addAll(buildLayers(documentsStore, mapDesTypesDeDocs, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
            documentsLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
            container.items().add(documentsLayer);

            return Collections.singletonList(container);
        } catch (Exception e) {
            throw new SirsCoreRuntimeException(e);
        }
    }

    public static MutableStyle createBergeStyle() throws CQLException, URISyntaxException{
        final Stroke stroke1 = SF.stroke(SF.literal(Color.BLACK),LITERAL_ONE_FLOAT,FF.literal(9),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null,LITERAL_ZERO_FLOAT);
        final LineSymbolizer line1 = SF.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,Units.POINT,stroke1,LITERAL_ONE_FLOAT);

        final Stroke stroke2 = SF.stroke(SF.literal(new Color(230, 179, 77)),LITERAL_ONE_FLOAT,FF.literal(7),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null,LITERAL_ZERO_FLOAT);
        final LineSymbolizer line2 = SF.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,Units.POINT,stroke2,LITERAL_ONE_FLOAT);

        final Stroke stroke3 = SF.stroke(SF.literal(Color.BLACK),LITERAL_ONE_FLOAT,FF.literal(1),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null,LITERAL_ZERO_FLOAT);
        final LineSymbolizer line3 = SF.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,Units.POINT,stroke3,LITERAL_ONE_FLOAT);

        return SF.style(line1,line2,line3);
    }

    public static MutableStyle createTraitBergeStyle() throws CQLException, URISyntaxException{
        final Stroke stroke1 = SF.stroke(SF.literal(new Color(255,204,128)),LITERAL_ONE_FLOAT,FF.literal(1.5),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null,LITERAL_ZERO_FLOAT);
        final LineSymbolizer line1 = SF.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,Units.POINT,stroke1,LITERAL_ONE_FLOAT);

        return SF.style(line1);
    }

    @Override
    public Optional<Image> getModelImage() throws IOException {
        final Image image;

        try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/sirs/bergeModel.png")) {
            image = new Image(in);
        }
        return Optional.of(image);
    }

}
