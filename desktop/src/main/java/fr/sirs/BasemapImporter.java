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

package fr.sirs;

import fr.sirs.core.SirsCore;
import fr.sirs.core.authentication.SIRSAuthenticator;
import fr.sirs.ui.FXBasemapEditor;
import fr.sirs.util.property.SirsPreferences;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.http.NameValuePair;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.osmtms.OSMTileMapClient;
import org.geotoolkit.security.ApiSecurity;
import org.geotoolkit.security.BasicAuthenticationSecurity;
import org.geotoolkit.security.ClientSecurity;
import org.geotoolkit.security.DefaultClientSecurity;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.coverage.CoverageStoreFactory;
import org.geotoolkit.utility.parameter.ParametersExt;
import org.geotoolkit.wms.WebMapClient;
import org.geotoolkit.wms.xml.WMSVersion;
import org.geotoolkit.wmts.WebMapTileClient;
import org.geotoolkit.wmts.xml.WMTSVersion;
import org.opengis.parameter.ParameterValueGroup;


/**
 * Provided the title and the store containing the basemap layer defined into the basemap preferences.
 *
 * @author maximegavens
 */
public class BasemapImporter {

    private Logger LOGGER = Logger.getLogger(BasemapImporter.class.getName());

    private String title;
    private CoverageStore store;

    public BasemapImporter() throws MalformedURLException, IOException {
        initDefaultStore();
    }

    public void loadFromPreferences() throws URISyntaxException, MalformedURLException, DataStoreException, IOException {
        final String storeType = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_CHOICE);

        if (storeType == null) {
            LOGGER.info("No default basemap choice are already made.");
            initDefaultStore();
        } else {
            switch (storeType) {
                case FXBasemapEditor.WMS_WMTS_CHOICE:
                    initStoreFromWebMapClientParameter();
                    break;
                case FXBasemapEditor.FILE_CHOICE:
                    initStoreFromLocalFile();
                    break;
                case FXBasemapEditor.OSM_TILE_CHOICE:
                    initStoreFromOsmTileMapClient();
                    break;
                default:
                    LOGGER.info("No default basemap choice are already made.");
                    initDefaultStore();
            }
        }
    }

    private void initStoreFromWebMapClientParameter() throws URISyntaxException, MalformedURLException {
        final String baseUrl = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_WM_URL);
        final String wmType = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_WM_TYPE);

        final Map<String, String> queryParams = parseQuery(baseUrl);
        final String withoutParam = getUrlWithoutParameters(baseUrl);
        final URL url = new URL(withoutParam);
        title = url.getHost();

        final ClientSecurity security;
        if (queryParams.containsKey("user") && queryParams.containsKey("password")) {
            security = new BasicAuthenticationSecurity(queryParams.get("user"), queryParams.get("password"));
        } else {
            security = DefaultClientSecurity.NO_SECURITY;
        }
        if (wmType.equals(FXBasemapEditor.WMS111)) {
            store = new WebMapClient(url, security, WMSVersion.v111, SirsCore.GEO_CLIENT_TIMEOUT);
        } else if (wmType.equals(FXBasemapEditor.WMS130)) {
            store = new WebMapClient(url, security, WMSVersion.v130, SirsCore.GEO_CLIENT_TIMEOUT);
        } else if (wmType.equals(FXBasemapEditor.WMTS100)) {
            store = new WebMapTileClient(url, security, WMTSVersion.v100, SirsCore.GEO_CLIENT_TIMEOUT);
        }
    }

    private void initStoreFromLocalFile() throws DataStoreException {
        String filePath = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_LOCAL_FILE);
        final String fileType = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_FILE_TYPE);

        final File f = new File(filePath);
        DataStoreFactory factory;
        if (FXBasemapEditor.COVERAGE_FILE_TYPE.equals(fileType)) {
            factory = DataStores.getFactoryById("coverage-file");
        } else if (FXBasemapEditor.HEXAGON_TYPE.equals(fileType)) {
            factory = DataStores.getFactoryById("hexagon");
        } else {
            throw new IllegalArgumentException("Type de fichier local non géré pour le chargement du fond de carte par défaut.");
        }
        if(factory instanceof CoverageStoreFactory) {
            CoverageStoreFactory coverageFactory = (CoverageStoreFactory) factory;
            final ParameterValueGroup params = coverageFactory.getParametersDescriptor().createValue();
            ParametersExt.getOrCreateValue(params, "path").setValue(f.toURI());
            title = f.getName();
            store = (CoverageStore) coverageFactory.create(params);
        }
    }

    private void initStoreFromOsmTileMapClient() throws URISyntaxException, MalformedURLException {
        final String baseUrl = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_OSM_TILE_URL);

        final Map<String, String> queryParams = parseQuery(baseUrl);
        final String withoutParam = getUrlWithoutParameters(baseUrl);

        final URL url = new URL(withoutParam);
        title = url.getHost();

        // Search in the query parameters a specific security
        final ClientSecurity security;
        if (queryParams.containsKey("user") && queryParams.containsKey("password")) {
            security = new BasicAuthenticationSecurity(queryParams.get("user"), queryParams.get("password"));
        } else if (queryParams.containsKey("apikey")) {
            security = new ApiSecurity(queryParams.get("apikey"));
        } else {
            security = null;
        }

        store = new OSMTileMapClient(url, security, 18, true);
    }

    private void initDefaultStore() throws MalformedURLException, IOException {
        title = "Thunderforest";
        //store = new OSMTileMapClient(new URL("http://tile.openstreetmap.org"), null, 18, true);
        //store = new OSMTileMapClient(new URL("http://c.tile.stamen.com/terrain"), null, 18, true);
        //store = new OSMTileMapClient(new URL("http://c.tile.stamen.com/toner"), null, 18, true);
        store = new OSMTileMapClient(new URL("https://tile.thunderforest.com/cycle"), new ApiSecurity(SIRSAuthenticator.getThunderForestApiKey()), 18, true);
    }

    /**
     * Retrieve query parameters for the given url.
     * @param url
     * @return
     * @throws URISyntaxException
     */
    private Map<String, String> parseQuery(final String url) throws URISyntaxException {
        final HashMap<String, String> queryMap = new HashMap<>();
        List<NameValuePair> parse = URLEncodedUtils.parse(new URI(url), "UTF-8");
        for (NameValuePair nvp: parse) {
            String name = nvp.getName();
            String value = nvp.getValue();
            queryMap.put(name.toLowerCase(), value);
        }
        return queryMap;
    }

    private String getUrlWithoutParameters(String url) throws URISyntaxException {
        URI uri = new URI(url);
        return new URI(uri.getScheme(),
                uri.getAuthority(),
                uri.getPath(),
                null, // Ignore the query part of the input url
                uri.getFragment()).toString();
    }

    public String getTitle() {
        return title;
    }

    public CoverageStore getStore() {
        return store;
    }
}
