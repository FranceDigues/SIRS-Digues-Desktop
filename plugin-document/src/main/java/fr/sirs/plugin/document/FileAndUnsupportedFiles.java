/*
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

package fr.sirs.plugin.document;

import java.io.File;
import java.util.List;

/**
 *
 * @author Estelle Idée (Geomatys)
 */
public final class FileAndUnsupportedFiles {

    private final List<String> unsupportedFiles;
    private final File file;

    public FileAndUnsupportedFiles(List<String> unsupportedFiles, File file) {
        this.unsupportedFiles = unsupportedFiles;
        this.file = file;
    }

    public List<String> getUnsupportedFiles() {
        return unsupportedFiles;
    }

    public File getFile() {
        return file;
    }
}