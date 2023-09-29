/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2021, Maxime Gavens (Geomatys)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;

/**
 * Client Security which append the query parameter apikey.
 * 
 * @author Maxime Gavens (Geomatys)
 */
public class ApiSecurity extends DefaultClientSecurity {

    protected static final Logger LOGGER = Logging.getLogger("org.geotoolkit.security");

    private String apikey;

    public ApiSecurity() {
    }

    public ApiSecurity(String apikey) {
        ArgumentChecks.ensureNonNull("apikey", apikey);
        this.apikey = apikey;
    }

    @Override
    public URL secure(URL url) {
        url = super.secure(url);

        try {
            final String new_string_url = url.toString() + "?apikey=" + apikey;
            url = new URL(new_string_url);
        } catch (MalformedURLException ex) {
            LOGGER.severe("ApiSecurity cannot secure the url with the api key: " + apikey);
        }
        return url;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }
}