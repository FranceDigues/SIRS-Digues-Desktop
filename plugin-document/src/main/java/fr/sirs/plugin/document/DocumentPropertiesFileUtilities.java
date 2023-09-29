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
package fr.sirs.plugin.document;

import fr.sirs.Injector;
import fr.sirs.PropertiesFileUtilities;
import fr.sirs.core.component.DigueRepository;
import fr.sirs.core.component.SystemeEndiguementRepository;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.SystemeEndiguement;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.plugin.document.ui.DocumentsPane;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.sirs.plugin.document.ui.DocumentsPane.*;

/**
 * Utility class managing the properties file adding different properties to the filesystem objects.
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class DocumentPropertiesFileUtilities extends PropertiesFileUtilities {

    public static File getOrCreateUnclassif(final File rootDirectory){
        final File unclassifiedDir = new File(rootDirectory, UNCLASSIFIED);
        if (!unclassifiedDir.exists()) {
            unclassifiedDir.mkdir();
        }

        final File docDir = new File(unclassifiedDir, DocumentsPane.DOCUMENT_FOLDER);
        if (!docDir.exists()) {
            docDir.mkdir();
        }
        return unclassifiedDir;
    }

    public static void updateFileSystem(final File rootDirectory) {

        final File saveDir = new File(rootDirectory, SAVE_FOLDER);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }

        final File unclassifiedDir = getOrCreateUnclassif(rootDirectory);

        final SystemeEndiguementRepository SErepo = Injector.getBean(SystemeEndiguementRepository.class);
        final DigueRepository Drepo = Injector.getBean(DigueRepository.class);
        final TronconDigueRepository TRrepo = Injector.getBean(TronconDigueRepository.class);

        /*
         * On recupere tous les elements.
         */
        final List<SystemeEndiguement> ses    = SErepo.getAll();
        final Set<Digue> digues               = new HashSet<>(Drepo.getAll());
        final Set<TronconDigue> troncons      = new HashSet<>(TRrepo.getAllLight());
        final Set<Digue> diguesFound          = new HashSet<>();
        final Set<TronconDigue> tronconsFound = new HashSet<>();
        final Set<File> seFiles               = listModel(rootDirectory, SE);
        final Set<File> digueMoved            = new HashSet<>();
        final Set<File> tronMoved             = new HashSet<>();

        for (SystemeEndiguement se : ses) {
            final File seDir = getOrCreateSE(rootDirectory, se, LIBELLE, true, DOCUMENT_FOLDER);
            seFiles.remove(seDir);

            final Set<File> digueFiles = listModel(seDir, DG);
            final List<Digue> diguesForSE = Drepo.getBySystemeEndiguement(se);
            for (Digue digue : digues) {
                if (!diguesForSE.contains(digue)) continue;
                diguesFound.add(digue);

                final File digueDir = getOrCreateDG(seDir, digue, LIBELLE, true, DOCUMENT_FOLDER);
                digueFiles.remove(digueDir);

                final Set<File> trFiles = listModel(digueDir, TR);

                final List<TronconDigue> tronconForDigue = TRrepo.getByDigue(digue);
                for (final TronconDigue td : troncons) {
                    if (!tronconForDigue.contains(td)) continue;
                    tronconsFound.add(td);

                    final File trDir = getOrCreateTR(digueDir, td, LIBELLE, true, DOCUMENT_FOLDER);
                    trFiles.remove(trDir);
                }

                // on place les tronçon disparus dans les fichiers deplacé
                tronMoved.addAll(trFiles);
            }

            // on place les digues disparues dans les fichiers deplacé
            digueMoved.addAll(digueFiles);
        }
        digues.removeAll(diguesFound);

        // on recupere les repertoire des digues / tronçons dans les SE detruits
        for (File seFile : seFiles) {
            digueMoved.addAll(listModel(seFile, DG));
            tronMoved.addAll(listModel(seFile, TR));
        }

        /*
         * On place toute les digues et troncons non trouvé dans un group a part.
         */
        final Set<File> digueFiles = listModel(unclassifiedDir, DG);

        for (final Digue digue : digues) {
            final File digueDir = getOrCreateDG(unclassifiedDir, digue, LIBELLE, true, DOCUMENT_FOLDER);
            digueFiles.remove(digueDir);

            final Set<File> trFiles = listModel(digueDir, TR);

            for (final TronconDigue td : troncons) {
                if (td.getDigueId()==null || !td.getDigueId().equals(digue.getDocumentId())) continue;
                tronconsFound.add(td);

                final File trDir = getOrCreateTR(digueDir, td, LIBELLE, true, DOCUMENT_FOLDER);
                trFiles.remove(trDir);
            }

            // on place les tronçon disparus dans les fichiers deplacé
            tronMoved.addAll(trFiles);
        }

        // on place les digues disparues dans les fichiers deplacé
        digueMoved.addAll(digueFiles);

        // on recupere les repertoire tronçons dans les digues detruites
        for (File digueFile : digueFiles) {
            tronMoved.addAll(listModel(digueFile, TR));
        }

        troncons.removeAll(tronconsFound);

        final Set<File> trFiles = listModel(unclassifiedDir, TR, false);

        for(final TronconDigue td : troncons){
            final File trDir = getOrCreateTR(unclassifiedDir, td, LIBELLE, true, DOCUMENT_FOLDER);
            trFiles.remove(trDir);
        }

        // on place les tronçon disparus dans les fichiers deplacé
        tronMoved.addAll(trFiles);

        /*
         * On restore les fichier deplacé dans leur nouvel emplacement.
         */
        final Set<File> tronMovedFound = new HashSet<>();
        for (File movedFile : tronMoved) {
            final File newFile = findFile(rootDirectory, movedFile);
            if (newFile != null) {
                backupDirectory(newFile.getParentFile(), movedFile, true, DOCUMENT_FOLDER);
                tronMovedFound.add(movedFile);
            }
        }
        tronMoved.removeAll(tronMovedFound);

        final Set<File> digueMovedFound = new HashSet<>();
        for (File movedFile : digueMoved) {
            final File newFile = findFile(rootDirectory, movedFile);
            if (newFile != null) {
                backupDirectory(newFile.getParentFile(), movedFile, true, DOCUMENT_FOLDER);
                digueMovedFound.add(movedFile);
            }
        }
        digueMoved.removeAll(digueMovedFound);

        /**
         * On place les fichiers deplacé non relocaliser dans le backup.
         */
        backupDirectories(saveDir, tronMoved, true, DOCUMENT_FOLDER);
        backupDirectories(saveDir, digueMoved, true, DOCUMENT_FOLDER);
        backupDirectories(saveDir, seFiles, true, DOCUMENT_FOLDER);
    }
}
