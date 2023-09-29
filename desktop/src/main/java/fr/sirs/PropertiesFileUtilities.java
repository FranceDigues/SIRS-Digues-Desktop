/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs;

import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.SirsDBInfoRepository;
import fr.sirs.core.model.*;
import fr.sirs.ui.DatabaseVersionPane;
import fr.sirs.util.StreamingIterable;
import javafx.scene.control.*;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.collection.CloseableIterator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class managing the properties file adding different properties to the filesystem objects.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Estelle Idée (Geomatys)
 */
public class PropertiesFileUtilities {

    private static final Logger LOGGER = Logging.getLogger("fr.sirs");

    public static final String SE = "se";
    public static final String TR = "tr";
    public static final String DG = "dg";

    /**
     * Extract a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder corresponding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     */
    public static String getProperty(final File f, final String property) {
        final Properties prop = getSirsProperties(f, true);
        return prop.getProperty(f.getName() + "_" + property, "");
    }

    /**
     * Set a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder corresponding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     * @param value The value to set.
     */
    public static void setProperty(final File f, final String property, final String value) {
        final Properties prop = getSirsProperties(f, true);
        prop.put(f.getName() + "_" + property, value);
        storeSirsProperties(prop, f, true);
    }

    /**
     * Remove a property in the sirs.properties file coupled to the specified file.
     * @param f A file, can be a folder correspounding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     */
    public static void removeProperty(final File f, final String property) {
        final Properties prop = getSirsProperties(f, true);
        prop.remove(f.getName() + "_" + property);
        storeSirsProperties(prop, f, true);
    }

    /**
     * Extract a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder correspounding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     */
    public static Boolean getBooleanProperty(final File f, final String property) {
        final Properties prop = getSirsProperties(f, true);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_" + property, "false"));
    }

    /**
     * Set a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder correspounding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     * @param value The value to set.
     */
    public static void setBooleanProperty(final File f, final String property, boolean value) {
        final Properties prop = getSirsProperties(f, true);
        prop.put(f.getName() + "_" + property, Boolean.toString(value));

        storeSirsProperties(prop, f, true);
    }

    /**
     * Return true if the specified file correspound to a a SE, DG or TR folder.
     *
     * @param f A file.
     */
    public static Boolean getIsModelFolder(final File f) {
        return getIsModelFolder(f, SE) || getIsModelFolder(f, TR) || getIsModelFolder(f, DG);
    }

    /**
     * Return true if the specified file correspound to a a specific specified model (SE, DG or TR).
     * @param f A file.
     * @param model SE, DG or TR.
     */
    public static Boolean getIsModelFolder(final File f, final String model) {
        final Properties prop = getSirsProperties(f, true);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_" + model, "false"));
    }

    /**
     * Set the specific specified model (SE, DG or TR) for a folder.
     *
     * @param f A model folder.
     * @param model SE, DG or TR.
     * @param libelle The name that will be displayed in UI.
     */
    public static void setIsModelFolder(final File f, final String model, final String libelle, final String LIBELLE) {
        final Properties prop = getSirsProperties(f, true);
        prop.put(f.getName() + "_" + model, "true");
        prop.put(f.getName() + "_" + LIBELLE, libelle);

        storeSirsProperties(prop, f, true);
    }

    /**
     * Remove all properties coupled to the specified file.
     *
     * @param f A file.
     */
    public static void removeProperties(final File f) {
        final Properties prop = getSirsProperties(f, true);

        Set<Entry<Object, Object>> properties = new HashSet<>(prop.entrySet());
        for (Entry<Object, Object> entry : properties) {
            if (((String) entry.getKey()).startsWith(f.getName())) {
                prop.remove(entry.getKey());
            }
        }
        //save cleaned properties file
        storeSirsProperties(prop, f, true);
    }

    /**
     * Store the updated properties to the sirs file.
     *
     * @param prop the updated properties. (will replace the previous one in the file).
     * @param f The file adding properties (not the sirs file).
     * @param parent {@code true} if the file f is not the root directory.
     */
    private static void storeSirsProperties(final Properties prop, final File f, boolean parent) {
        final File sirsPropFile;
        try {
            sirsPropFile = getSirsPropertiesFile(f, parent);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while accessing sirs properties file.", ex);
            return;
        }

        if (sirsPropFile != null && sirsPropFile.exists()) {
            try (final FileWriter writer = new FileWriter(sirsPropFile)) {
                prop.store(writer, "");
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while writing sirs properties file.", ex);
            }
        }
    }

    /**
     * Get or create a sirs.properties file next to the specified one (or in the directory if parent is set to false)
     *
     * @param f A file.
     * @param parent {@code true} if the file f is not the root directory.
     *
     * @return A sirs.properties file.
     */
    private static File getSirsPropertiesFile(final File f, final boolean parent) throws IOException {
        final File parentFile;
        if (parent) {
            parentFile = f.getParentFile();
        } else {
            parentFile = f;
        }
        if (parentFile != null) {
            final File sirsPropFile = new File(parentFile, "sirs.properties");
            if (!sirsPropFile.exists()) {
                sirsPropFile.createNewFile();
            }
            return sirsPropFile;
        }
        return null;
    }

    /**
     * Return the Properties associated with all the files next to the one specified (or in the directory if parent is set to false).
     *
     * @param f A file.
     * @param parent {@code true} if the file f is not the root directory.
     */
    private static Properties getSirsProperties(final File f, final boolean parent) {
        final Properties prop = new Properties();
        File sirsPropFile = null;
        try {
            sirsPropFile = getSirsPropertiesFile(f, parent);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while loading/creating sirs properties file.", ex);
        }

        if (sirsPropFile != null) {
            try (final FileReader reader = new FileReader(sirsPropFile)) {
                prop.load(reader);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while reading sirs properties file.", ex);
            }
        }

        return prop;
    }

    /**
     * Return a label for a file size (if it is a directory the all the added size of its children).
     *
     * @param f A file.
     */
    public static String getStringSizeFile(final File f) {
        final long size        = getFileSize(f);
        final DecimalFormat df = new DecimalFormat("0.0");
        final float sizeKb     = 1024.0f;
        final float sizeMo     = sizeKb * sizeKb;
        final float sizeGo     = sizeMo * sizeKb;
        final float sizeTerra  = sizeGo * sizeKb;

        if (size < sizeKb) {
            return df.format(size) + " o";
        } else if (size < sizeMo) {
            return df.format(size / sizeKb) + " Ko";
        } else if (size < sizeGo) {
            return df.format(size / sizeMo) + " Mo";
        } else if (size < sizeTerra) {
            return df.format(size / sizeGo) + " Go";
        }
        return "";
    }

    /**
     * Return the size of a file (if it is a directory the all the added size of its children).
     */
    private static long getFileSize(final File f) {
        if (f.isDirectory()) {
            long result = 0;
            for (File child : f.listFiles()) {
                result += getFileSize(child);
            }
            return result;
        } else {
            return f.length();
        }
    }

    public static File getOrCreateSE(final File rootDirectory, final SystemeEndiguement sd, final String LIBELLE,
                                     final boolean createDocumentFolder, final String DOCUMENT_FOLDER) {
        ArgumentChecks.ensureNonNull("Systeme d'endiguement sd", sd);
        if (createDocumentFolder && DOCUMENT_FOLDER == null)
            throw new IllegalArgumentException("The attribute DOCUMENT_FOLDER must not be null when createDocumentFolder is true");

        return getOrCreateForLibelledElement(rootDirectory, SE, sd, LIBELLE, createDocumentFolder, DOCUMENT_FOLDER);
    }

    public static File getOrCreateDG(final File rootDirectory, final Digue digue, final String LIBELLE,
                                     final boolean createDocumentFolder, final String DOCUMENT_FOLDER) {
        ArgumentChecks.ensureNonNull("Digue", digue);
        if (createDocumentFolder && DOCUMENT_FOLDER == null)
            throw new IllegalArgumentException("The attribute DOCUMENT_FOLDER must not be null when createDocumentFolder is true");
        return getOrCreateForLibelledElement(rootDirectory, DG, digue, LIBELLE, createDocumentFolder, DOCUMENT_FOLDER);
    }

    public static File getOrCreateTR(final File rootDirectory, final TronconDigue tronconDigue, final String LIBELLE,
                                     final boolean createDocumentFolder, final String DOCUMENT_FOLDER) {
        ArgumentChecks.ensureNonNull("TronconDigue", tronconDigue);
        if (createDocumentFolder && DOCUMENT_FOLDER == null)
            throw new IllegalArgumentException("The attribute DOCUMENT_FOLDER must not be null when createDocumentFolder is true");
        return getOrCreateForLibelledElement(rootDirectory, TR, tronconDigue, LIBELLE, createDocumentFolder, DOCUMENT_FOLDER);
    }

    /**
     * @param model SE, DG or TR.
     */
    public static <T extends Element & AvecLibelle> File getOrCreateForLibelledElement(final File rootDirectory, final String model, final T libelledElement, final String LIBELLE,
                                                                                       final boolean createDocumentFolder, final String DOCUMENT_FOLDER) {
        switch (model) {
            case SE:
            case DG:
            case TR:
                break;
            default:
                throw new IllegalArgumentException(" Input model " + model + " must be one of the following values : " + SE + ", " + DG + ", " + TR);
        }

        final File trDir = new File(rootDirectory, libelledElement.getId());
        if (!trDir.exists()) {
            trDir.mkdir();
        }
        String name = libelledElement.getLibelle();
        if (name == null) {
            name = "null";
        }

        setIsModelFolder(trDir, model, name, LIBELLE);
        if (createDocumentFolder) {
            if (DOCUMENT_FOLDER == null)
                throw new IllegalArgumentException("The attribute DOCUMENT_FOLDER must not be null when createDocumentFolder is true");
            final File docDir = new File(trDir, DOCUMENT_FOLDER);
            if (!docDir.exists()) {
                docDir.mkdir();
            }
        }

        return trDir;
    }

    public static String getExistingDatabaseIdentifier(final File rootDirectory) {
        final Properties prop = getSirsProperties(rootDirectory, false);
        return (String) prop.get("database_identifier");
    }

    public static void updateDatabaseIdentifier(final File rootDirectory) {
        final String key = getDatabaseIdentifier();
        final Properties prop = getSirsProperties(rootDirectory, false);
        prop.put("database_identifier", key);

        storeSirsProperties(prop, rootDirectory, false);
    }

    public static void backupDirectories(final File saveDir, final Collection<File> files, final boolean createDocumentFolder, final String DOCUMENT_FOLDER) {
        if (createDocumentFolder && DOCUMENT_FOLDER == null)
            throw new IllegalArgumentException("The attribute DOCUMENT_FOLDER must not be null when createDocumentFolder is true");
        for (File f : files) {
            backupDirectory(saveDir, f, createDocumentFolder, DOCUMENT_FOLDER);
        }
    }

    public static void backupDirectory(final File saveDir, final File f, final boolean createDocumentFolder, final String DOCUMENT_FOLDER) {
        if (createDocumentFolder && DOCUMENT_FOLDER == null)
            throw new IllegalArgumentException("The attribute DOCUMENT_FOLDER must not be null when createDocumentFolder is true");

        // extract properties
        final Map<Object, Object> extracted = new HashMap<>();
        final Properties prop = getSirsProperties(f, true);
        Set<Entry<Object, Object>> properties = new HashSet<>(prop.entrySet());
        for (Entry<Object, Object> entry : properties) {
            if (((String) entry.getKey()).startsWith(f.getName())) {
                extracted.put(entry.getKey(), entry.getValue());
                prop.remove(entry.getKey());
            }
        }

        //save cleaned properties file
        storeSirsProperties(prop, f, true);


        final File newDir = new File(saveDir, f.getName());
        try {
            // we copy only the "dossier d'ouvrage" directory
            if (!newDir.exists()) {
                newDir.mkdir();
            }

            if (createDocumentFolder) {
                final File doFile = new File(f, DOCUMENT_FOLDER);
                final File newDoFile = new File(newDir, DOCUMENT_FOLDER);

                Files.copy(doFile.toPath(), newDoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                IOUtilities.deleteRecursively(f.toPath());

                // save new properties
                final Properties newProp = getSirsProperties(newDir, true);
                newProp.putAll(extracted);

                storeSirsProperties(newProp, newDir, true);
            }

        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while moving destroyed obj to backup folder", ex);
        }
    }

    public static Set<File> listModel(final File rootDirectory, final String model) {
        Set<File> modelList = new HashSet<>();
        listModel(rootDirectory, modelList, model, true);
        return modelList;
    }

    public static Set<File> listModel(final File rootDirectory, final String model, final boolean deep) {
        Set<File> modelList = new HashSet<>();
        listModel(rootDirectory, modelList, model, deep);
        return modelList;
    }

    private static void listModel(final File rootDirectory, Set<File> modelList, String model, final boolean deep) {
        for (File f : rootDirectory.listFiles()) {
            if (f.isDirectory()) {
                if (getIsModelFolder(f, model)) {
                    modelList.add(f);
                } else if (deep) {
                    listModel(f, modelList, model, deep);
                }
            }
        }
    }

    public static File findFile(final File rootDirectory, File file) {
        for (File f : rootDirectory.listFiles()) {
            if (f.getName().equals(file.getName()) && !f.getPath().equals(file.getPath())) {
                return f;
            } else if (f.isDirectory()) {
                File child = findFile(f, file);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    public static boolean verifyDatabaseVersion(final File rootDirectory) {
        final String key = getDatabaseIdentifier();
        final String existingKey = getExistingDatabaseIdentifier(rootDirectory);
        if (existingKey == null) {
            return true;
        } else if (!existingKey.equals(key)) {
            return showBadVersionDialog(existingKey, key);
        }
        return true;
    }

    private static boolean showBadVersionDialog(final String existingKey, final String dbKey) {
        final Dialog dialog = new Alert(Alert.AlertType.ERROR);
        final DialogPane pane = new DialogPane();
        final DatabaseVersionPane ipane = new DatabaseVersionPane(existingKey, dbKey);
        pane.setContent(ipane);
        pane.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Version de la base différente");
        dialog.setContentText("Le système de fichier que vous tentez d'ouvrir correspond à une autre base de données.\n Souhaitez-vous tout de même l'ouvrir ?");
        final Optional opt = dialog.showAndWait();
        return opt.isPresent() && ButtonType.YES.equals(opt.get());
    }

    public static String getDatabaseIdentifier() {
        final SirsDBInfoRepository dbRepo = Injector.getBean(SirsDBInfoRepository.class);
        final Optional<SirsDBInfo> info = dbRepo.get();
        if (info.isPresent()) {
            final SirsDBInfo dbInfo = info.get();
            return dbInfo.getUuid() + "|" + dbInfo.getEpsgCode() + "|" + dbInfo.getVersion() + "|" + dbInfo.getRemoteDatabase();
        }
        return null;
    }

    public static List<Objet> getElements(Collection<TronconDigue> troncons, final NumberRange dateRange) {
        final ArrayList<Objet> elements = new ArrayList<>();
        final Collection<AbstractPositionableRepository<Objet>> repos = (Collection) Injector.getSession().getRepositoriesForClass(Objet.class);

        for (TronconDigue troncon : troncons) {
            if (troncon == null) {
                continue;
            }

            for (final AbstractPositionableRepository<Objet> repo : repos) {
                StreamingIterable<Objet> tmpElements = repo.getByLinearIdStreaming(troncon.getId());
                try (final CloseableIterator<Objet> it = tmpElements.iterator()) {
                    while (it.hasNext()) {
                        Objet next = it.next();
                        if (dateRange != null) {
                            //on vérifie la date
                            final LocalDate objDateDebut = next.getDate_debut();
                            final LocalDate objDateFin = next.getDate_fin();
                            final long debut = objDateDebut == null ? 0 : objDateDebut.atTime(0, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
                            final long fin = objDateFin == null ? Long.MAX_VALUE : objDateFin.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();
                            final NumberRange objDateRange = NumberRange.create(debut, true, fin, true);
                            if (!dateRange.intersectsAny(objDateRange)) {
                                continue;
                            }
                        }

                        elements.add(next);
                    }
                }
            }
        }
        return elements;
    }

    public static void showErrorDialog(final String errorMsg) {
        showErrorDialog(errorMsg, null, 0, 0);
    }

    /**
     *
     * @param errorMsg the message inside the alert window
     * @param title the title of the alert window
     * @param width the width of the alert window
     * @param height the height of the alert window
     */
    public static void showErrorDialog(final String errorMsg, final String title, final int width, final int height) {
        createAlert(Alert.AlertType.ERROR, title, errorMsg, width, height);
    }

    public static void showInformationDialog(final String errorMsg) {
        createAlert(Alert.AlertType.INFORMATION, errorMsg, 0, 0);
    }

    public static void showInformationDialog(final String errorMsg, final String title, final int width, final int height) {
        createAlert(Alert.AlertType.INFORMATION, title, errorMsg, width, height);
    }

    public static void showSuccessDialog(final String errorMsg) {
        createAlert(Alert.AlertType.INFORMATION, "succès", errorMsg, 0, 0);
    }

    public static void showSuccessDialog(final String errorMsg, String title, final int width, final int height) {
        if (title == null) title = "Succès";
        createAlert(Alert.AlertType.INFORMATION, title, errorMsg, width, height);
    }

    public static Optional showConfirmationDialog(final String errorMsg) {
        return createAlert(Alert.AlertType.INFORMATION, errorMsg, 0, 0);
    }

    public static Optional showConfirmationDialog(final String errorMsg, final String title, final int width, final int height, final boolean addNoYesButton) {
        return createAlert(Alert.AlertType.CONFIRMATION, title, errorMsg, width, height, addNoYesButton);
    }

    public static void showWarningDialog(final String errorMsg) {
        createAlert(Alert.AlertType.WARNING, errorMsg, 0, 0);
    }

    public static void showWarningDialog(final String errorMsg, final String title, final int width, final int height) {
        createAlert(Alert.AlertType.WARNING, title, errorMsg, width, height, false);
    }

    private static Optional createAlert(final Alert.AlertType type, final String contentMsg, final int width, final int height) {
        return createAlert(type, null, contentMsg, width, height);
    }

    private static Optional createAlert(final Alert.AlertType type, final String title, final String contentMsg, final int width, final int height) {
        return createAlert(type, title, contentMsg, width, height, false);
    }

    private static Optional createAlert(final Alert.AlertType type, final String contentMsg, final int width, final int height, final boolean addNoYesButton) {
        return createAlert(type, null, contentMsg, width, height, addNoYesButton);
    }

    private static Optional createAlert(final Alert.AlertType type, final String title, final String contentMsg, final int width, final int height, final boolean addNoYesButton) {
        if (addNoYesButton)
            return createAlert(type, title, contentMsg, width, height, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        else
            return createAlert(type, title, contentMsg, width, height, null);
    }

    public static Optional createAlert(final Alert.AlertType type, final String title, final String contentMsg, final int width, final int height, ButtonType... buttons) {
        final Alert dialog = new Alert(type, contentMsg, buttons);
        if (buttons != null && buttons.length > 1) {
            ButtonBar buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
            buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
        }
        dialog.setResizable(true);
        if (width != 0) dialog.getDialogPane().setPrefWidth(width);
        if (height != 0) dialog.getDialogPane().setPrefHeight(height);
        if (title != null) dialog.setTitle(title);
        dialog.setHeaderText(null);
        return dialog.showAndWait();
    }
}
