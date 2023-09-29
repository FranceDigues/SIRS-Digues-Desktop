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
package fr.sirs.plugin.carto;

import fr.sirs.Injector;
import fr.sirs.Plugin;
import fr.sirs.Session;
import fr.sirs.core.model.BookMark;
import fr.sirs.map.FXMapTab;
import static fr.sirs.theme.ui.FXBookMarkPane.parseSecurityParameters;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import javafx.scene.image.Image;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.security.ClientSecurity;
import org.geotoolkit.security.DefaultClientSecurity;
import org.geotoolkit.storage.DataStore;
import org.geotoolkit.storage.coverage.CoverageReference;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.wms.WebMapClient;
import org.geotoolkit.wms.xml.WMSVersion;
import org.geotoolkit.wmts.WebMapTileClient;
import org.geotoolkit.wmts.xml.WMTSVersion;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class PluginCarto extends Plugin {
    private static final String NAME = "plugin-carto";
    private static final String TITLE = "Module cartographie";
    private static final int TIMEOUT = 60000;

    /**
     * List available service types.
     * TODO : transform this shit into SPI/ Factory.
     */
    public static enum SERVICE implements BiFunction<URL, ClientSecurity, DataStore> {
        WMS_111("WMS - 1.1.1", (url, auth) -> new WebMapClient(url, auth, WMSVersion.v111, TIMEOUT)),
        WMS_130("WMS - 1.3.0", (url, auth) -> new WebMapClient(url, auth, WMSVersion.v130, TIMEOUT)),
        WMTS_100("WMTS - 1.0.0", (url, auth) -> new WebMapTileClient(url, auth, WMTSVersion.v100, TIMEOUT));

        public final String title;
        private final BiFunction<URL, ClientSecurity, DataStore> connector;
        private SERVICE(final String title, final BiFunction<URL, ClientSecurity, DataStore> connector) {
            this.title = title;
            this.connector = connector;
        }

        @Override
        public DataStore apply(URL t, ClientSecurity u) {
            return connector.apply(t, u);
        }

        public static SERVICE findValue(String value) {
            if (value == null || (value = value.trim()).isEmpty())
                return null;
            SERVICE serv = valueOf(value);
            if (serv != null)
                return serv;

            for (final SERVICE service : SERVICE.values()) {
                if (service.title.equalsIgnoreCase(value.trim()))
                    return service;
            }

            return null;
        }
    }

    public PluginCarto() {
        name = NAME;
        loadingMessage.set("Chargement du module de cartographie");
        themes.add(new AddLayerTheme());
        themes.add(new BookMarkTheme());
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
    public Optional<Image> getModelImage() throws IOException {
        final Image image;

        try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/sirs/cartoModel.png")) {
            image = new Image(in);
        }
        return Optional.of(image);
    }

    /**
     * Connect to the service described by given bookmark, and send back a list
     * of all layers available in it.
     *
     * @param bm Bookmark to use for connection
     * @return All map layers found.
     * @throws MalformedURLException If given bookmark contains a malformed URL.
     * @throws IOException If an error occurs while connecting to the service.
     */
    public static List<MapLayer> listLayers(BookMark bm) throws MalformedURLException, IOException {
        final String service = bm.getTypeService();
        if (service ==  null)
            throw new IllegalArgumentException("Le type de service n'est pas renseigné !");
        String params = bm.getParametres();
        if (params == null || (params = params.trim()).isEmpty())
            throw new IllegalArgumentException("L'URL du service n'est pas renseignée !");

        final URL url = new URL(params);
        final ClientSecurity security = parseSecurityParameters(bm.getIdentifiant(), bm.getMotDePasse())
                .orElse(DefaultClientSecurity.NO_SECURITY);

        final SERVICE connector = SERVICE.findValue(service);
        if (connector == null)
            throw new IllegalArgumentException("Le type de service spéccifié n'est pas géré : "+service);

        final DataStore store = connector.apply(url, security);
        final List<MapLayer> layers = new ArrayList<>();
        try {
            if (store instanceof CoverageStore) {
                final CoverageStore cStore = (CoverageStore) store;
                for (GenericName n : cStore.getNames()) {
                    final CoverageReference cref = cStore.getCoverageReference(n);
                    final CoverageMapLayer layer = MapBuilder.createCoverageLayer(cref);
                    layer.setName(n.tip().toString());
                    layers.add(layer);
                }
            } else {
                throw new UnsupportedOperationException("Only imagery services are supported for now.");
            }
        } catch (Exception ex) {
            // Wrap exception to identify it as an error due to connection failure.
            throw new IOException("Echec de connection :\n" + ex.getLocalizedMessage(), ex);
        }

        return layers;
    }

    /**
     * Add all given map layers on map. They will be put in a group who is named
     * as specified.
     *
     * @param groupName THe name of the group to put layers into. If we cannot
     * find any matching group in session map context, we'll create a new one.
     * @param layers Layers to add in session's map.
     */
    public static void showOnMap(String groupName, final Collection<MapLayer> layers) {
        if (groupName == null)
            groupName = "";

        final Session session = Injector.getSession();
        final FXMapTab mapTab = session.getFrame().getMapTab();
        final Collection<MapItem> root = mapTab.getMap().getUiMap().getContainer().getContext().items();

        MapItem parent = null;
        for (MapItem mi : root) {
            if (groupName.equals(mi.getName())) {
                parent = mi;
                break;
            }
        }
        if (parent == null) {
            parent = MapBuilder.createItem();
            parent.setName(groupName);
            root.add(parent);
        }

        parent.items().addAll(layers);
        mapTab.show();
    }
}
