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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Base64;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PasswordSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        try {
            jgen.writeString(Base64.getEncoder().withoutPadding().encodeToString(SerialParameters.getEncoder().doFinal(value.getBytes())));
        } catch (Exception ex) {
            throw new IOException("Cannot encode string.", ex);
        }
    }

    public static String encode(final String toEncode) {
        try {
            return Base64.getEncoder().withoutPadding().encodeToString(SerialParameters.getEncoder().doFinal(toEncode.getBytes()));
        } catch (Exception ex) {
            throw new RuntimeException("Cannot encode string.", ex);
        }
    }
}
