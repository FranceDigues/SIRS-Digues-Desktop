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
package fr.sirs.core.authentication;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Base64;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PasswordDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final String text = jp.getText();
        if (text == null || text.isEmpty())
            return "";
        try {
            return new String(SerialParameters.getDecoder().doFinal(Base64.getDecoder().decode(text)));
        } catch (Exception e) {
            throw new IOException("Cannot decode string", e);
        }
    }

    public static String decode(final String toDecode) {
        try {
            return new String(SerialParameters.getDecoder().doFinal(Base64.getDecoder().decode(toDecode)));
        } catch (Exception ex) {
            throw new RuntimeException("Cannot decode string.", ex);
        }
    }
}
