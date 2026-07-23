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
package fr.sirs.plugin.lit;

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import fr.sirs.Plugin;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.StructBeanSupplier;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.component.TronconLitRepository;
import fr.sirs.core.model.*;
import fr.sirs.map.FXMapPane;
import fr.sirs.plugin.lit.map.LitToolBar;
import fr.sirs.util.FXFreeTab;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import fr.sirs.util.LinearIdFilterFunction;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import org.apache.sis.measure.Units;
import org.geotoolkit.cql.CQLException;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.bean.BeanStore;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
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
public class PluginLit extends Plugin {
    private static final String NAME = "plugin-lit";
    private static final String TITLE = "Module lit";

    private static final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;
    private static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
    private final SuiviLitTheme suiviTheme;

    public PluginLit() {
        name = NAME;
        loadingMessage.set("module lit");
        suiviTheme = new SuiviLitTheme();
        themes.add(suiviTheme);
        themes.add(new StructureDescriptionTheme());
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
        return Collections.singletonList(new LitToolBar(mapPane.getUiMap()));
    }

    @Override
    public boolean handleTronconType(final Class<? extends Element> element){
        return TronconLit.class.equals(element)
                || Lit.class.equals(element);
    }

    @Override
    public FXFreeTab openTronconPane(final Element element){
        final FXFreeTab tab = Injector.getSession().getOrCreateThemeTab(suiviTheme);
        suiviTheme.display(element);
        return tab;
    }

    public static String LAYER_NAME = LabelMapper.get(TronconLit.class).mapClassNamePlural();

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

        suppliers.put(TronconLit.class, getDefaultSupplierForClass.apply(TronconLit.class));

        // Ouvrages dans le lit
        suppliers.put(SeuilLit.class, getDefaultSupplierForClass.apply(SeuilLit.class));
        suppliers.put(PlageDepotLit.class, getDefaultSupplierForClass.apply(PlageDepotLit.class));
        suppliers.put(AutreOuvrageLit.class, getDefaultSupplierForClass.apply(AutreOuvrageLit.class));

        suppliers.put(IleBancLit.class, getDefaultSupplierForClass.apply(IleBancLit.class));

        suppliers.put(DesordreLit.class, getDefaultSupplierForClass.apply(DesordreLit.class));

        suppliers.put(Prestation.class, getDefaultSupplierForClass.apply(Prestation.class));

        suppliers.put(OuvrageAssocieLit.class, getDefaultSupplierForClass.apply(OuvrageAssocieLit.class));
        suppliers.put(OccupationRiveraineLit.class, getDefaultSupplierForClass.apply(OccupationRiveraineLit.class));
        suppliers.put(PenteLit.class, getDefaultSupplierForClass.apply(PenteLit.class));
        suppliers.put(LargeurLit.class, getDefaultSupplierForClass.apply(LargeurLit.class));
        suppliers.put(RegimeEcoulementLit.class, getDefaultSupplierForClass.apply(RegimeEcoulementLit.class));
        suppliers.put(DomanialiteLit.class, getDefaultSupplierForClass.apply(DomanialiteLit.class));
        suppliers.put(ZoneAtterrissementLit.class, getDefaultSupplierForClass.apply(ZoneAtterrissementLit.class));

        // Réseaux et voiries
        suppliers.put(VoieAcces.class, getDefaultSupplierForClass.apply(VoieAcces.class));
        suppliers.put(OuvrageFranchissement.class, getDefaultSupplierForClass.apply(OuvrageFranchissement.class));
        suppliers.put(OuvrageVoirie.class, getDefaultSupplierForClass.apply(OuvrageVoirie.class));

        // Réseaux et ouvrages
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
            final Function<Class<? extends Element>, StructBeanSupplier> getDefaultSupplierForClass = (Class<? extends Element> c) ->{
                return new StructBeanSupplier(c, () -> getSession().getRepositoryForClass(c).getAll());
            };
            //troncons
            final BeanStore tronconStore = new BeanStore(getDefaultSupplierForClass.apply(TronconLit.class));
            List<MapLayer> layers = CorePlugin.buildLayers(tronconStore, LAYER_NAME, createLitStyle(), createTronconSelectionStyle(false),true);

            MapItem container = MapBuilder.createItem();
            container.setName("Module lits");
            container.items().addAll(layers);


            final Map<String, String> nameMap = new HashMap<>();
            for(Class elementClass : suppliers.keySet()) {
                final LabelMapper mapper = LabelMapper.get(elementClass);
                nameMap.put(elementClass.getSimpleName(), mapper.mapClassName());
            }

            final Map<Class<? extends AbstractPositionDocument>, List<Class>> mapDesTypesDeDocs = getTypesDeDocsMap(nameMap);

            // Redmine 7631 : show positionable for TronconLit that are same type of Objects as for TronconDigue.
            // Use dynamic filter that evaluates at render time to include newly created TronconLit objects.
            final Filter filter = FF.equals(
                    FF.function(LinearIdFilterFunction.NAME, FF.literal(TronconLit.class.getCanonicalName())),
                    FF.literal(true)
            );

            try {
                final MapItem structLayer = MapBuilder.createItem();
                structLayer.setName("Ouvrages dans le lit");
                final BeanStore ouvrageStore = new BeanStore(suppliers.get(SeuilLit.class),
                        suppliers.get(PlageDepotLit.class),
                        suppliers.get(AutreOuvrageLit.class));
                structLayer.items().addAll(buildLayers(ouvrageStore, nameMap, colors, createDefaultSelectionStyle(), false));
                structLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
                container.items().add(structLayer);

                final MapItem ilesLayer = MapBuilder.createItem();
                ilesLayer.setName("Îles et bancs");
                final BeanStore ilesStore = new BeanStore(suppliers.get(IleBancLit.class));
                ilesLayer.items().addAll(buildLayers(ilesStore, nameMap, colors, createDefaultSelectionStyle(), false));
                ilesLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
                container.items().add(ilesLayer);

                // Désordres
                final BeanStore desordreStore = new BeanStore(suppliers.get(DesordreLit.class));
                final MapItem desordreLayer = MapBuilder.createItem();
                desordreLayer.setName("Désordres (lit)");
                desordreLayer.items().addAll(buildLayers(desordreStore, nameMap, colors, createDefaultSelectionStyle(), false));
                desordreLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
                container.items().add(desordreLayer);

                desordreLayer.items().add(CorePlugin.createAndAddUrgenceGroup(DesordreLit.class, desordreStore, null, createDefaultSelectionStyle()));

                // Réseaux de voirie
                final BeanStore rvStore = new BeanStore(
                        suppliers.get(VoieAcces.class),
                        suppliers.get(OuvrageFranchissement.class),
                        suppliers.get(OuvrageVoirie.class));
                final MapItem rvLayer = MapBuilder.createItem();
                rvLayer.setName("Réseaux de voirie");
                rvLayer.items().addAll( buildLayers(rvStore, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
                rvLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
                container.items().add(rvLayer);

                // Réseaux et ouvrages
                final BeanStore roStore = new BeanStore(
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

                // Prestations
                final BeanStore prestaStore = new BeanStore(suppliers.get(Prestation.class));
                final MapItem prestaLayer = MapBuilder.createItem();
                prestaLayer.setName("Prestations");
                prestaLayer.items().addAll( buildLayers(prestaStore, nameMap, colors, createDefaultSelectionStyle(),false, filter) );
                prestaLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
                container.items().add(prestaLayer);

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

                final MapItem otherLayer = MapBuilder.createItem();
                otherLayer.setName("Autre (lit)");
                final BeanStore otherStore = new BeanStore(suppliers.get(OuvrageAssocieLit.class),
                        suppliers.get(OccupationRiveraineLit.class),
                        suppliers.get(PenteLit.class),
                        suppliers.get(LargeurLit.class),
                        suppliers.get(RegimeEcoulementLit.class),
                        suppliers.get(DomanialiteLit.class),
                        suppliers.get(ZoneAtterrissementLit.class));
                otherLayer.items().addAll(buildLayers(otherStore, nameMap, colors, createDefaultSelectionStyle(), false));
                otherLayer.setUserProperty(Session.FLAG_SIRSLAYER, Boolean.TRUE);
                container.items().add(otherLayer);


            } catch (Exception ex) {
                SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }


            return Collections.singletonList(container);
        } catch (Exception e) {
            throw new SirsCoreRuntimeException(e);
        }
    }

    public static MutableStyle createLitStyle() throws CQLException, URISyntaxException{
        final Stroke stroke1 = SF.stroke(SF.literal(new Color(0, 100, 80)),LITERAL_ONE_FLOAT,FF.literal(9),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null,LITERAL_ZERO_FLOAT);
        final LineSymbolizer line1 = SF.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,Units.POINT,stroke1,LITERAL_ONE_FLOAT);

        final Stroke stroke2 = SF.stroke(SF.literal(new Color(204, 230, 255)),LITERAL_ONE_FLOAT,FF.literal(7),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null,LITERAL_ZERO_FLOAT);
        final LineSymbolizer line2 = SF.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,Units.POINT,stroke2,LITERAL_ONE_FLOAT);

        final Stroke stroke3 = SF.stroke(SF.literal(new Color(0, 100, 80)),LITERAL_ONE_FLOAT,FF.literal(1),
                STROKE_JOIN_BEVEL, STROKE_CAP_SQUARE, null,LITERAL_ZERO_FLOAT);
        final LineSymbolizer line3 = SF.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,Units.POINT,stroke3,LITERAL_ONE_FLOAT);

        return SF.style(line1,line2,line3);
    }

    @Override
    public Optional<Image> getModelImage() throws IOException {
        final Image image;

        try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/sirs/litModel.png")) {
            image = new Image(in);
        }
        return Optional.of(image);
    }
}
