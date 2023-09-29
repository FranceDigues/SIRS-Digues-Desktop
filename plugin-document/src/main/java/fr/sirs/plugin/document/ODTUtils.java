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
import fr.sirs.core.component.DigueRepository;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.model.report.ModeleRapport;
import static fr.sirs.plugin.document.DocumentPropertiesFileUtilities.getElements;
import static fr.sirs.plugin.document.DocumentPropertiesFileUtilities.getOrCreateDG;
import static fr.sirs.plugin.document.DocumentPropertiesFileUtilities.getOrCreateTR;
import static fr.sirs.plugin.document.DocumentPropertiesFileUtilities.setBooleanProperty;
import static fr.sirs.plugin.document.DocumentPropertiesFileUtilities.setProperty;
import static fr.sirs.plugin.document.ui.DocumentsPane.*;
import fr.sirs.plugin.document.ui.DocumentsPane;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.sis.measure.NumberRange;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ODTUtils extends fr.sirs.util.odt.ODTUtils {

    private static final String[] TABLE_HEADERS = new String[]{"Nom", "Taille", "N° Inventaire", "Lieu classement"};

    public static Task generateDocsForDigues(final String docName, boolean onlySe, final ModeleRapport modele,
            final Collection<TronconDigue> troncons, final File seDir, final String title, final NumberRange dateRange) {
        return TaskManager.INSTANCE.submit(new Task() {

            @Override
            protected Object call() throws Exception {
                updateTitle("Génération d'un rapport");
                updateMessage("Recherche des objets du rapport...");
                final DigueRepository digueRepo = Injector.getBean(DigueRepository.class);
                final int total = troncons.size() + 1;
                final AtomicInteger progress = new AtomicInteger(0);
                final ArrayList<Task> tasks = new ArrayList<>();
                for (TronconDigue troncon : troncons) {
                    final File digDir = getOrCreateDG(seDir, digueRepo.get(troncon.getDigueId()), LIBELLE, true, DOCUMENT_FOLDER);
                    final File docDir = new File(getOrCreateTR(digDir, troncon, LIBELLE, true, DOCUMENT_FOLDER), DOCUMENT_FOLDER);
                    final File newDoc = new File(docDir, docName);

                    List<Objet> elements = getElements(troncons, dateRange);
                    final Task reportGenerator = generateReport(modele, elements, newDoc.toPath(), title);
                    Platform.runLater(() -> reportGenerator.setOnSucceeded(event -> {
                        setBooleanProperty(newDoc, DYNAMIC, true);
                        setProperty(newDoc, MODELE, modele.getId());
                        setProperty(newDoc, DocumentsPane.DATE_RANGE_MIN, dateRange.getMinValue().toString());
                        setProperty(newDoc, DocumentsPane.DATE_RANGE_MAX, dateRange.getMaxValue().toString());
                        updateProgress(progress.incrementAndGet(), total);
                    }));
                    tasks.add(reportGenerator);
                }


                updateMessage("rapports en cours de construction...");
                for (final Task t : tasks) {
                    t.get();
                }

                return true;
            }
        });
    }

    public static Task<File> generateDoc(final ModeleRapport modele, final Collection<TronconDigue> troncons,
            final File outputDoc, final String title, final NumberRange dateRange) {
        return TaskManager.INSTANCE.submit(new Task() {

            @Override
            protected Object call() throws Exception {
                updateTitle("Génération d'un rapport");
                updateMessage("Recherche des objets du rapport...");

                List<Objet> elements = getElements(troncons, dateRange);
                final Task reportGenerator = generateReport(modele, elements, outputDoc.toPath(), title);
                Platform.runLater(() -> {
                    reportGenerator.messageProperty().addListener((obs, oldValue, newValue) -> updateMessage(newValue));
                    reportGenerator.workDoneProperty().addListener((obs, oldValue, newValue) -> updateProgress(newValue.doubleValue(), reportGenerator.getTotalWork()));
                });

                reportGenerator.get();
                setBooleanProperty(outputDoc, DYNAMIC, true);
                setProperty(outputDoc, MODELE, modele.getId());

                if(dateRange != null) {
                    setProperty(outputDoc, DocumentsPane.DATE_RANGE_MIN, dateRange.getMinValue().toString());
                    setProperty(outputDoc, DocumentsPane.DATE_RANGE_MAX, dateRange.getMaxValue().toString());
                }
                return outputDoc;
            }
        });
    }

    /**
     * Write index of intput tree content.
     * @param item Tree to parse.
     * @param file Location for output document.
     * @return Generated file (should be the same as input).
     */
    public static Task<File> writeSummary(final FileTreeItem item, File file) {
        return TaskManager.INSTANCE.submit(new Task<File>() {

            @Override
            protected File call() throws Exception {
                updateTitle("Génération d'un index : "+item.getLibelle());

                updateMessage("Initialisation du document");
                final TextDocument doc = TextDocument.newTextDocument();
                final List<FileTreeItem> children = item.listChildrenItem();
                final int total = children.size() + 1;
                int progress = 0;
                for (FileTreeItem child : children) {
                    updateProgress(progress++, total);
                    if (!child.getLibelle().equals(DocumentsPane.SAVE_FOLDER)) {
                        write(doc, child, false, null);
                    }
                }

                updateMessage("Sauvegarde du document");
                doc.save(file);

                return file;
            }
        });
    }

    /**
     * Write a synthesis about given tree content.
     * @param item Tree to parse.
     * @param file File to put synthesis into.
     * @return Input file.
     */
    public static Task<FileAndUnsupportedFiles> writeDoSynth(final FileTreeItem item, final File file) {
        return TaskManager.INSTANCE.submit(new Task<FileAndUnsupportedFiles>() {

            @Override
            protected FileAndUnsupportedFiles call() throws Exception {
                updateTitle("Génération d'une synthèse : "+item.getLibelle());

                // Define a string which will deliver task message.
                final StringProperty msgProperty = new SimpleStringProperty();
                Platform.runLater(() -> msgProperty.addListener((obs, oldValue, newValue) -> updateMessage(newValue)));

                msgProperty.set("Initialisation du document");
                final TextDocument doc = TextDocument.newTextDocument();
                final List<String> unsupportedFiles = write(doc, item, true, msgProperty);

                FileAndUnsupportedFiles fileAndUnsupportedFiles = new FileAndUnsupportedFiles(unsupportedFiles, file);

                msgProperty.set("Sauvegarde du document");
                doc.save(file);

                return fileAndUnsupportedFiles;
            }
        });
    }

    /**
     * Write index or synthesis of a tree item.
     * @param doc Document to append index / synthesis into.
     * @param item Tree item to parse.
     * @param doSynth True if we should write synthesis, false if we write index.
     * @param progressMessage A text property to put progress message into.
     * @throws Exception
     */
    private static List<String> write(final TextDocument doc, final FileTreeItem item, boolean doSynth, final StringProperty progressMessage) throws Exception {
        // title
        final int headingLevel;
        if (item.isSe()) {
            headingLevel = 2;
        } else if (item.isDg()) {
            headingLevel = 3;
        } else if (item.isTr()) {
            headingLevel = 4;
        } else {
            headingLevel = 5;
        }
        doc.addParagraph(item.getLibelle()).applyHeading(true, headingLevel);

        List<FileTreeItem> directories = item.listChildrenItem(true, doSynth);
        List<FileTreeItem> files       = item.listChildrenItem(false, doSynth);
        final List<String> unsupportedFiles = new ArrayList<>();
        if (!files.isEmpty()) {
            if (doSynth) {
                final String prefix = item.getLibelle() + System.lineSeparator() +"Concatenation des fichiers : ";
                final int n = files.size();
                int i = 1;
                for (FileTreeItem child : files) {
                    TextDocument tmpDoc = TextDocument.newTextDocument();
                    try {
                        final int I = i++;
                        Platform.runLater(() -> progressMessage.set(prefix + I + "/" + n));
                        tmpDoc.addParagraph(child.getLibelle()).applyHeading(true, 6);
                        append(tmpDoc, child.getValue());
                    } catch (Exception e) {
                        unsupportedFiles.add(child.getValue().getName());
                        continue;
                    }
                    //tmpDoc added to doc only if the append() has not failed
                    doc.insertContentFromDocumentAfter(tmpDoc, doc.addParagraph(""), true);
                }
            } else {
                final Table table = Table.newTable(doc, 1 + files.size(), TABLE_HEADERS.length);
                // header
                Row row = table.getRowByIndex(0);
                for (int i = 0 ; i < TABLE_HEADERS.length ; i++) {
                    row.getCellByIndex(i).setStringValue(TABLE_HEADERS[i]);
                }

                FileTreeItem file;
                for (int i = 0; i < files.size(); i++) {
                    file = files.get(i);
                    row = row.getNextRow();
                    row.getCellByIndex(0).setStringValue(file.getLibelle());
                    row.getCellByIndex(1).setStringValue(file.getSize());
                    row.getCellByIndex(2).setStringValue(file.getInventoryNumber());
                    row.getCellByIndex(3).setStringValue(file.getClassPlace());
                }
            }
        }

        for (FileTreeItem child : directories) {
            doc.addColumnBreak();
            write(doc, child, doSynth, progressMessage);
        }

        return unsupportedFiles;
    }
}
