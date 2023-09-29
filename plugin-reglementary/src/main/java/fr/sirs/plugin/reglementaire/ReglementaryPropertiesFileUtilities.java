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
package fr.sirs.plugin.reglementaire;

import fr.sirs.Injector;
import fr.sirs.PropertiesFileUtilities;
import fr.sirs.core.component.SystemeEndiguementRepository;
import fr.sirs.core.model.SystemeEndiguement;

import java.io.File;
import java.util.List;
import java.util.Set;

import static fr.sirs.plugin.reglementaire.ui.RegistreDocumentsPane.LIBELLE;

/**
 * Utility class managing the properties file adding different properties to the filesystem objects.
 *
 * @author Estelle Idée (Geomatys)
 */
public final class ReglementaryPropertiesFileUtilities extends PropertiesFileUtilities {

    public static void updateFileSystem(final File rootDirectory) {

        final SystemeEndiguementRepository SErepo = Injector.getBean(SystemeEndiguementRepository.class);

        /*
         * On recupere tous les elements.
         */
        final List<SystemeEndiguement> ses    = SErepo.getAll();
        final Set<File> seFiles               = listModel(rootDirectory, SE);

        ses.forEach(se -> {
            final File seDir = getOrCreateSE(rootDirectory, se, LIBELLE, false, null);
            seFiles.remove(seDir);
        });
    }
}
