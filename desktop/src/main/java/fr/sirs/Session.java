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
package fr.sirs;

import fr.sirs.core.ModuleDescription;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.component.SirsDBInfoRepository;
import fr.sirs.core.component.UtilisateurRepository;
import fr.sirs.core.model.*;
import fr.sirs.other.FXDesignationPane;
import fr.sirs.other.FXReferencePane;
import fr.sirs.other.FXValidationPane;
import fr.sirs.theme.Theme;
import fr.sirs.theme.ui.AbstractFXElementPane;
import fr.sirs.theme.ui.FXElementContainerPane;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.ui.report.FXModeleRapportsPane;
import fr.sirs.ui.ModeleElementTable;
import fr.sirs.util.FXFreeTab;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.property.SirsPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.display2d.ext.DecorationXMLParser;
import org.geotoolkit.display2d.ext.DefaultBackgroundTemplate;
import org.geotoolkit.display2d.ext.legend.DefaultLegendTemplate;
import org.geotoolkit.display2d.ext.northarrow.DefaultNorthArrowTemplate;
import org.geotoolkit.display2d.ext.northarrow.NorthArrowTemplate;
import org.geotoolkit.display2d.ext.scalebar.DefaultScaleBarTemplate;
import org.geotoolkit.display2d.ext.scalebar.ScaleBarTemplate;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.storage.coverage.CoverageReference;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.style.DefaultDescription;
import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * La session contient toutes les données chargées dans l'instance courante de
 * l'application.
 *
 * Notamment, elle doit réferencer l'ensemble des thèmes ouvert, ainsi que les
 * onglets associés. De même pour les {@link Element}s et leurs éditeurs.
 *
 * La session fournit également un point d'accès centralisé à tous les documents
 * de la base CouchDB.
 *
 * @author Johann Sorel
 */
@Component
public class Session extends SessionCore {

    public static String FLAG_SIRSLAYER = "SirsLayer";

    ////////////////////////////////////////////////////////////////////////////
    // GESTION et MISE À JOUR DES REFERENCES ET DES REQUÊTES PRÉPROGRAMMÉES
    ////////////////////////////////////////////////////////////////////////////
    private final ReferenceChecker referenceChecker;
    public ReferenceChecker getReferenceChecker(){return referenceChecker;}
    private final QueryChecker queryChecker;
    public QueryChecker getQueryChecker(){return queryChecker;}

    ////////////////////////////////////////////////////////////////////////////
    private MapContext mapContext;
    private final MapItem backgroundGroup = MapBuilder.createItem();

    private FXMainFrame frame = null;

    private final Cache<Object, FXFreeTab> openEditors = new Cache<>(12, 0, false);

    public enum AdminTab{VALIDATION, USERS}

    public enum PrintTab{DESORDRE, RESEAU_FERME, OUVRAGE_ASSOCIE, TEMPLATE, REPORT}

    //generate a template for the legend
    final DefaultLegendTemplate legendTemplate = new DefaultLegendTemplate(
            new DefaultBackgroundTemplate( //legend background
                    new BasicStroke(1), //stroke
                    Color.LIGHT_GRAY, //stroke paint
                    Color.WHITE, // fill paint
                    new Insets(10, 10, 10, 10), //border margins
                    8 //round border
            ),
            2, //gap between legend elements
            null, //glyph size, we can let it to null for the legend to use the best size
            new Font("Serial", Font.PLAIN, 11), //Font used for style rules
            true, // show layer names
            new Font("Serial", Font.BOLD, 13), //Font used for layer names
            true // display only visible layers
    );
    public static final ScaleBarTemplate SCALEBAR_KILOMETER_TEMPLATE = new DefaultScaleBarTemplate(
                            new DefaultBackgroundTemplate(
                                    new BasicStroke(1),
                                    new Color(0, 0, 0, 0),
                                    new Color(255,255,255,170),
                                    new Insets(6, 6, 0, 6), 20),
                            new Dimension(250,40),10,
                            false, 4, NumberFormat.getNumberInstance(),
                            Color.DARK_GRAY, Color.GRAY, Color.WHITE,
                            10,true,false, new Font("Serial", Font.BOLD, 10),true,
                            Units.KILOMETRE);
    public static final ScaleBarTemplate SCALEBAR_METER_TEMPLATE = new DefaultScaleBarTemplate(
                            new DefaultBackgroundTemplate(
                                    new BasicStroke(1),
                                    new Color(0, 0, 0, 0),
                                    new Color(255,255,255,170),
                                    new Insets(6, 6, 0, 6), 20),
                            new Dimension(250,40),10,
                            false, 4, NumberFormat.getNumberInstance(),
                            Color.DARK_GRAY, Color.GRAY, Color.WHITE,
                            10,true,false, new Font("Serial", Font.BOLD, 10),true,
                            Units.METRE);
    public static final NorthArrowTemplate NORTH_ARROW_TEMPLATE = new DefaultNorthArrowTemplate(
                     new DefaultBackgroundTemplate(
                                    new BasicStroke(1),
                                    new Color(0, 0, 0, 0),
                                    new Color(255,255,255,170),
                                    new Insets(4, 4, 4, 4), 500),
                    DecorationXMLParser.class.getResource("/org/geotoolkit/icon/boussole.svg"),
                    new Dimension(100,100));


    @Autowired
    public Session(CouchDbConnector couchDbConnector) {
        super(couchDbConnector);
        referenceChecker = new ReferenceChecker(
            SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.REFERENCE_URL)
        );
        queryChecker = new QueryChecker(
            SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.PREPROGRAMMED_QUERIES_URL)
        );
        printManager = new PrintManager();
    }

    void setFrame(FXMainFrame frame) {
        this.frame = frame;
    }

    public FXMainFrame getFrame() {
        return frame;
    }

    /**
     * MapContext affiché pour toute l'application.
     *
     * @return MapContext
     */
    public synchronized MapContext getMapContext() {
        if(mapContext==null){
            mapContext = MapBuilder.createContext(getProjection());
            mapContext.setName("Carte");

            try {
                //modules layers
                final Plugin[] plugins = Plugins.getPlugins();
                final HashMap<String, ModuleDescription> moduleDescriptions = new HashMap<>(plugins.length);
                for(Plugin plugin : plugins){
                    final ModuleDescription d = new ModuleDescription();
                    d.setName(plugin.name);
                    d.setTitle(plugin.getTitle().toString());
                    d.setVersion(plugin.getConfiguration().getVersionMajor()+"."+plugin.getConfiguration().getVersionMinor());

                    List<MapItem> mapItems = plugin.getMapItems();
                    for (final MapItem item : mapItems) {
                        setPluginProvider(item, plugin);
                        ModuleDescription.getLayerDescription(item).ifPresent(desc -> d.layers.add(desc));
                    }
                    mapContext.items().addAll(mapItems);
                    moduleDescriptions.put(d.getName(), d);
                }
                final Envelope bounds = mapContext.getBounds(true);
                mapContext.setAreaOfInterest(bounds);
                SirsDBInfoRepository infoRepo = getApplicationContext().getBean(SirsDBInfoRepository.class);
                infoRepo.updateModuleDescriptions(moduleDescriptions);
                infoRepo.setEnvelope(bounds);

            } catch (Exception ex) {
                SirsCore.LOGGER.log(Level.WARNING, "Cannot retrieve sirs layers.", ex);
                final Runnable r = () -> GeotkFX.newExceptionDialog("Impossible de construire la liste des couches cartographiques", ex).show();
                if (Platform.isFxApplicationThread()) {
                    r.run();
                } else {
                    Platform.runLater(r);
                }
            }

            //Fond de plan
            backgroundGroup.setName("Fond de plan");
            mapContext.items().add(0, backgroundGroup);
            final CoverageMapLayer basemapLayer = getBasemapLayer();
            if (basemapLayer != null) {
                backgroundGroup.items().add(basemapLayer);
            }
        }
        return mapContext;
    }

    public static CoverageMapLayer getBasemapLayer() {
        try{
            final BasemapImporter importer = new BasemapImporter();
            importer.loadFromPreferences();
            return getFirstCoverageMapLayer(importer.getStore(), importer.getTitle());
        } catch(Exception ex){
            SirsCore.LOGGER.log(Level.WARNING, "Cannot retrieve background layers.", ex);
            final Runnable r = () -> GeotkFX.newExceptionDialog("Impossible de construire le fond de carte. Veuillez vérifier le paramétrage du fond de carte par défaut. Chargement du fond de carte intial", ex).show();
            if (Platform.isFxApplicationThread()) {
                r.run();
            } else {
                Platform.runLater(r);
            }
            // Loading of initial basemap
            try {
                final BasemapImporter importer = new BasemapImporter();
                return getFirstCoverageMapLayer(importer.getStore(), importer.getTitle());
            } catch (Exception ex2) {
                SIRS.LOGGER.log(Level.SEVERE, "Impossible to load the initial basemap.", ex2);
                final Runnable r2 = () -> GeotkFX.newExceptionDialog("Impossible de charger le fond de carte initial.", ex2).show();
                if (Platform.isFxApplicationThread()) {
                    r2.run();
                } else {
                    Platform.runLater(r2);
                }
                return null;
            }
        }
    }

    private static CoverageMapLayer getFirstCoverageMapLayer(final CoverageStore store, final String title) throws DataStoreException {
        for (GenericName n : store.getNames()) {
            final CoverageReference cr = store.getCoverageReference(n);
            final CoverageMapLayer cml = MapBuilder.createCoverageLayer(cr);
            cml.setName(title);
            cml.setDescription(new DefaultDescription(
                    new SimpleInternationalString(title),
                    new SimpleInternationalString(title)));
            cml.setVisible(false);
            return cml;
        }
        return null;
    }

    /**
     * Mark the given map item and all of its layers as provided by input plugin.
     * @param mapItem The map item to mark. Cannot be null
     * @param provider The plugin which provided the item. Cannot be null.
     */
    private static void setPluginProvider(final MapItem mapItem, final Plugin provider) {
        mapItem.getUserProperties().put(Plugin.PLUGIN_FLAG, provider.name);
        for (final MapItem child : mapItem.items()) {
            setPluginProvider(child, provider);
        }
    }

    public synchronized MapItem getBackgroundLayerGroup() {
        getMapContext();
        return backgroundGroup;
    }

    ////////////////////////////////////////////////////////////////////////////
    // GESTION DES IMPRESSIONS PDF
    ////////////////////////////////////////////////////////////////////////////
    private final PrintManager printManager;
    public final PrintManager getPrintManager(){return printManager;}

    ////////////////////////////////////////////////////////////////////////////
    // GESTION DES PANNEAUX
    ////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param object The object the edition tab is requested for.
     */
    public void showEditionTab(final Object object) {
        showEditionTab(object, SIRS.CONSULTATION_PREDICATE);
    }

    /**
     *
     * @param object The object the edition tab is requested for.
     * @param editionPredicate Prédicat d'édition du panneau à l'ouverture
     */
    public void showEditionTab(final Object object, final Predicate<Element> editionPredicate) {
        final Optional<? extends Element> element = getElement(object);
        if (element.isPresent()){
            if(element.get() instanceof ReferenceType) {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Les références ne sont pas éditables.", ButtonType.CLOSE);
                alert.setResizable(true);
                alert.showAndWait();
            } else {
                getFrame().addTab(getOrCreateElementTab(element.get(), editionPredicate));
            }
        }
    }

    public FXFreeTab getOrCreatePrintTab(final PrintTab printTab, final String title) {
        return getOrCreateTab(printTab, () -> {
            final FXFreeTab tab = new FXFreeTab(title);
            switch (printTab) {
                case DESORDRE:
                    tab.setContent(new FXDisorderPrintPane());
                    break;
                case RESEAU_FERME:
                    tab.setContent(new FXReseauFermePrintPane());
                    break;
                case OUVRAGE_ASSOCIE:
                    tab.setContent(new FXOuvrageAssociePrintPane());
                    break;
                case TEMPLATE:
                    tab.setContent(new ModeleElementTable());
                    break;
                case REPORT:
                    final FXModeleRapportsPane reportPane = new FXModeleRapportsPane();
                    reportPane.setRight(reportPane.editor);
                    tab.setContent(reportPane);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown print type : "+printTab);
            }
            return tab;
        });
    }

    public FXFreeTab getOrCreateAdminTab(final AdminTab adminTab, final String title) {
            switch(adminTab){
            case USERS:
                return getOrCreateTab(AdminTab.USERS, () -> {
                    final FXFreeTab tab = new FXFreeTab(title);
                    final PojoTable usersTable = new PojoTable(getRepositoryForClass(Utilisateur.class), "Table des utilisateurs", (ObjectProperty<? extends Element>) null) {
                        @Override
                        protected void deletePojos(final Element... pojos) {
                            final List<Element> pojoList = new ArrayList<>();
                            for (final Element pojo : pojos) {
                                if (pojo instanceof Utilisateur) {
                                    final Utilisateur utilisateur = (Utilisateur) pojo;
                                    // On interdit la suppression de l'utilisateur courant !
                                    if (utilisateur.equals(session.getUtilisateur())) {
                                        final Alert alert = new Alert(Alert.AlertType.ERROR, "Vous ne pouvez pas supprimer votre propre compte.", ButtonType.CLOSE);
                                        alert.setResizable(true);
                                        alert.showAndWait();
                                    } // On interdit également la suppression de l'invité par défaut !
                                    else if (UtilisateurRepository.GUEST_USER.equals(utilisateur)) {
                                        final Alert alert = new Alert(Alert.AlertType.ERROR, "Vous ne pouvez pas supprimer le compte de l'invité par défaut.", ButtonType.CLOSE);
                                        alert.setResizable(true);
                                        alert.showAndWait();
                                    } else {
                                        pojoList.add(pojo);
                                    }
                                }
                            }
                            super.deletePojos(pojoList.toArray(new Element[0]));
                        }
                    };
                    usersTable.cellEditableProperty().unbind();
                    usersTable.cellEditableProperty().set(false);
                    tab.setContent(usersTable);
                    return tab;
                });
            case VALIDATION:
                return getOrCreateTab(AdminTab.VALIDATION, () -> {
                    final FXFreeTab tab = new FXFreeTab(title);
                    tab.setContent(new FXValidationPane());
                    return tab;
                });
            default:
                throw new UnsupportedOperationException("Unsupported administration pane.");
        }
    }

    public FXFreeTab getOrCreateThemeTab(final Theme theme) {
        if (theme.isCached()) {
            return getOrCreateTab(theme, () -> {
                final Parent parent = theme.createPane();
                if (parent == null) {
                    return null;
                } else {
                    final FXFreeTab tab = new FXFreeTab(theme.getName());
                    if (SirsCore.REGISTRE_THEME_NAME.equals(theme.getName())) {
                        tab.setOnClosed(event -> theme.resetContent());
                        tab.setOnSelectionChanged(event -> {
                            if (((FXFreeTab) event.getSource()).isSelected()) {
                                theme.refresh();
                            }
                        });
                    }
                    tab.setContent(parent);
                    tab.selectedProperty().addListener(theme.getSelectedPropertyListener());
                    return tab;
                }
            });
        }
        /*
        Certains thèmes doivent pouvoir être ouverts depuis plusieus origines et
        ne doivent donc pas être mis en cache.

        Par exemple, pour le plugin AOT/COT, le thème de consultation d'une
        convention pour un élément dépend de l'élément courant. Si on est sur un
        élément "A" lié à une convention "1", le panneau de thème liste la
        convention "1". Mais si on ne ferme pas le panneau de thème et qu'on
        souhaite ensuite consulter les conventions d'un élément "B" lié à une
        autre convention "2", le fait de refaire appel au thème, s'il est mis en
        cache, ne fait que donner le focus au panneau précédant listant la
        convention "1". En supprimant le cache, on ouvre une nouvelle fenêtre
        avec la convention "2".
         */
        else {
            final Parent parent = theme.createPane();
            if(parent==null)
                return null;
            else {
                final FXFreeTab tab = new FXFreeTab(theme.getName());
                tab.setContent(parent);
                tab.selectedProperty().addListener(theme.getSelectedPropertyListener());
                return tab;
            }
        }
    }

    public FXFreeTab getOrCreateDesignationTab(final Class<? extends Element> clazz) {
            return getOrCreateTab(clazz, () -> {
                final FXFreeTab tab = new FXFreeTab("Désignations du type " + LabelMapper.get(clazz).mapClassName());
                tab.setContent(new FXDesignationPane(clazz));
                return tab;
            });
    }

    public FXFreeTab getOrCreateReferenceTypeTab(final Class<? extends ReferenceType> clazz){
        return getOrCreateTab(clazz, () -> {
            final FXFreeTab tab = new FXFreeTab(LabelMapper.get(clazz).mapClassName());
            tab.setContent(new FXReferencePane(clazz));
            return tab;
        });
    }

    public FXFreeTab getOrCreateElementTab(final Element element, final Predicate<Element> editionPredicate) {
        // On commence par regarder si un plugin spécifie une ouverture particulière.
        for(final Plugin plugin : Plugins.getPlugins()){
            if(plugin.handleTronconType(element.getClass())){
                return plugin.openTronconPane(element);
            }
        }

        // Si on a affaire à un élément qui n'est pas un tronçon, ou bien d'un type de tronçon qu'aucun plugin n'ouvre de manière particulière, on ouvre l'élément de manière standard.
        return getOrCreateTab(element, new ElementTabCreator(element, editionPredicate));
    }

    public FXFreeTab getOrCreateTab(final Object target, final Callable<FXFreeTab> tabCreator) {
        try {
            return openEditors.getOrCreate(target, () -> {
                final FXFreeTab newTab = tabCreator.call();
                return newTab;
            });
        } catch (Exception e) {
            throw new SirsCoreRuntimeException(e);
        }
    }

    /**
     * Create a title for a given element.
     * @param element Object to get title for.
     * @return a title, never null, but can be empty.
     */
    public static String generateElementTitle(final Element element) {
        String title="";
        final String libelle = new SirsStringConverter().toString(element);
        if (libelle != null && !libelle.isEmpty()) {
            title += libelle;
        }

        final Element parent = element.getParent();
        if (parent instanceof AvecLibelle) {
            final String parentLibelle = ((AvecLibelle)parent).getLibelle();
            if(parentLibelle!=null){
                title+=" ("+parentLibelle+")";
            }
        }
        return title;
    }

    public void focusOnMap(Element target) {
        if (target == null || frame == null || frame.getMapTab() == null || frame.getMapTab().getMap() == null) {
            return;
        }
        frame.getMapTab().getMap().focusOnElement(target);
    }

    public DefaultLegendTemplate getLegendTemplate() {
        return legendTemplate;
    }


    /**
     * Create a new tab containing an editor to work on input target.
     */
    private static class ElementTabCreator implements Callable<FXFreeTab> {

        private final Element target;
        private final Predicate<Element> editionPredicate;

        public ElementTabCreator(final Element target, final Predicate<Element> editionPredicate) {
            ArgumentChecks.ensureNonNull("Target element", target);
            this.target = target;
            this.editionPredicate = editionPredicate;
        }

        @Override
        public FXFreeTab call() throws Exception {
            final FXFreeTab tab = new FXFreeTab();

            // TODO : remove
            final ProgressIndicator wait = new ProgressIndicator();
            wait.setMaxSize(200, 200);
            wait.setProgress(-1);
            final BorderPane content = new BorderPane(wait);
            tab.setContent(content);

            Injector.getSession().getTaskManager().submit(() -> {
                Node edit = (Node) SIRS.generateEditionPane(target, editionPredicate);
                if (edit == null) {
                    edit = new BorderPane(new Label("Pas d'éditeur pour le type : " + target.getClass().getSimpleName()));
                }
                final Node n = edit;
                FadeTransition ft = new FadeTransition(Duration.millis(1000), n);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);

                tab.setOnClosed(event -> {
                    // line moved from method getOrCreateTab(final Object target, final Callable<FXFreeTab> tabCreator)
                    // since more complex setOnClosed event required.
                    Injector.getSession().openEditors.remove(target);

                    if (!(n instanceof FXElementContainerPane)) return;

                    // Force to remove listeners before closing the tab.
                    FXElementContainerPane containerPane = (FXElementContainerPane) n;
                    final ObservableList<Node> children = containerPane.getChildren();
                    if (children.isEmpty()) return;

                    children.stream().filter(child -> child instanceof AbstractFXElementPane)
                            .map(child -> (AbstractFXElementPane) child)
                            .forEach(pane -> pane.removeListenersBeforeClosingTab());
                });

                Platform.runLater(() -> {
                    content.setCenter(n);
                    n.requestFocus();
                    ft.play();
                });
            });


            ChangeListener<String> listenDesignation = (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                tab.setTextAbrege(generateElementTitle(target));
            };

            tab.hack = listenDesignation;

            // Update tab title if element designation changes.
            if (target instanceof PositionDocument) {
                final PositionDocument positionDocument = (PositionDocument) target;
                positionDocument.sirsdocumentProperty().addListener(new WeakChangeListener<>(listenDesignation));
            }

            target.designationProperty().addListener(new WeakChangeListener<>(listenDesignation));

            tab.setTextAbrege(generateElementTitle(target));

            return tab;
        }
    }

    /**
     * Try to retrieve ALREADY OPENED editors corresponding to the elements with
     * given Ids.
     * @param ids Ids of the elements to find an open editor for.
     * @return A set of opened editors for the wanted elements. There's no
     * guarantee that we will succeed to find an editor for each or any of them.
     * Accordingly, the result can be an empty set (never null). Moreover, this
     * result set can contain more elements than input one, because if we find
     * an editor whose target is a sub-element (i.e {@link Element#getDocumentId() }
     * is contained in given set) is of one of the wanted ids, it will be
     * included in the result.
     */
    public Set<FXFreeTab> findEditors(Set<String> ids) {
        final HashSet<FXFreeTab> result = new HashSet<>();
        for (final Map.Entry<Object, FXFreeTab> entry : openEditors.entrySet()) {
            Element e;
            String id, docId;
            if (entry.getKey() instanceof Element) {
                e = (Element)entry.getKey();
                id = e.getId();
                if (id != null && ids.contains(id) || (docId = e.getDocumentId()) != null && ids.contains(docId))
                    result.add(entry.getValue());
            }
        }

        return result;
    }
}
