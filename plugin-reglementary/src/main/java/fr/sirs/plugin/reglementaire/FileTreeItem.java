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

import fr.sirs.plugin.reglementaire.ui.RegistreDocumentsPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static fr.sirs.plugin.reglementaire.ReglementaryPropertiesFileUtilities.getBooleanProperty;
import static fr.sirs.plugin.reglementaire.ui.RegistreDocumentsPane.HIDDEN;

/**
 * Tree item used in the tree-table representing the documents.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileTreeItem extends TreeItem<File> {

    public boolean rootShowHiddenFile;

    public final BooleanProperty hidden = new SimpleBooleanProperty();


    /**
     * Constructor for shared root instance.
     * @param rootShowHiddenFile
     */
    public FileTreeItem(boolean rootShowHiddenFile) {
        super(null);
        this.rootShowHiddenFile = rootShowHiddenFile;
    }

    public FileTreeItem(File item, boolean showHiddenFile) {
        super(item);
        hidden.setValue(getBooleanProperty(item, HIDDEN));
        hidden.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            ReglementaryPropertiesFileUtilities.setBooleanProperty(getValue(), HIDDEN, newValue);
        });

        if (item != null && item.isDirectory()) {
            for (File f : listFiles(item, showHiddenFile)) {
                if (!f.getName().equals("sirs.properties")) {
                    getChildren().add(new FileTreeItem(f, showHiddenFile));
                }
            }
            getChildren().sort(new FileTreeItemComparator());
        }
    }

    @Override
    public boolean isLeaf() {
        final File f = getValue();
        if (f != null) {
            return !getValue().isDirectory();
        }
        return true;
    }

    /**
     * List the files in the specified directory and order them.
     *
     * @param directory
     * @return
     */
    private List<File> listFiles(File directory, boolean showHiddenFile) {
        final List<File> result = new ArrayList<>(Arrays.asList(directory.listFiles()));

        if (!showHiddenFile) {
            final List<File> toRemove = new ArrayList<>();
            for (File f : result) {
                if (getBooleanProperty(f, HIDDEN)) {
                    toRemove.add(f);
                }
            }
            result.removeAll(toRemove);
        }
        return result;
    }

    /**
     * Update the current items.
     * @param showHiddenFile
     */
    public void update(boolean showHiddenFile) {
        if (getValue().isDirectory()) {
            List<FileTreeItem> children = listChildrenItem();
            for (File f : listFiles(getValue(), showHiddenFile)) {
                if (!f.getName().equals("sirs.properties")) {
                    FileTreeItem item = getChildrenItem(f);
                    if (item == null) {
                        getChildren().add(new FileTreeItem(f, showHiddenFile));
                    } else {
                        children.remove(item);
                        item.update(showHiddenFile);
                    }
                }
            }
            // remove the destroyed node
            for (FileTreeItem item : children) {
                getChildren().remove(item);
            }
            getChildren().sort(new FileTreeItemComparator());
        }
    }

    /**
     * Return The Tree item correspounding to the specified file in the current node children.
     *
     * @param f
     * @return
     */
    private FileTreeItem getChildrenItem(final File f) {
        for (TreeItem item : getChildren()) {
            FileTreeItem fitem = (FileTreeItem) item;
            if (fitem.getValue().getPath().equals(f.getPath())) {
                return fitem;
            }
        }
        return null;
    }

    /**
     * Return a list of the children items.
     *
     * @return
     */
    public List<FileTreeItem> listChildrenItem() {
        final List<FileTreeItem> results = new ArrayList<>();
        for (TreeItem item : getChildren()) {
            results.add((FileTreeItem) item);
        }
        return results;
    }

    public List<FileTreeItem> listChildrenItem(boolean directory) {
        final List<FileTreeItem> results = new ArrayList<>();
        for (TreeItem item : getChildren()) {
            final FileTreeItem fitem = (FileTreeItem) item;
            if ( fitem.isDirectory() &&  directory ||
                !fitem.isDirectory() && !directory) {
                results.add(fitem);
            }
        }
        return results;
    }

    public String getLibelle() {
        final File f = getValue();
        String name = ReglementaryPropertiesFileUtilities.getProperty(getValue(), RegistreDocumentsPane.LIBELLE);
        if (name.isEmpty()) {
            name = f.getName();
        }
        return name;
    }

    public String getSize() {
        return ReglementaryPropertiesFileUtilities.getStringSizeFile(getValue());
    }

    public String getTimeStampDate() {
        return ReglementaryPropertiesFileUtilities.getProperty(getValue(), RegistreDocumentsPane.TIMESTAMP_DATE);
    }

    public boolean isDirectory() {
        return getValue().isDirectory();
    }

    public boolean isSe() {
        return ReglementaryPropertiesFileUtilities.getIsModelFolder(getValue(), ReglementaryPropertiesFileUtilities.SE);
    }

    public boolean isDg() {
        return ReglementaryPropertiesFileUtilities.getIsModelFolder(getValue(), ReglementaryPropertiesFileUtilities.DG);
    }

    public boolean isTr() {
        return ReglementaryPropertiesFileUtilities.getIsModelFolder(getValue(), ReglementaryPropertiesFileUtilities.TR);
    }

    private static class FileTreeItemComparator implements Comparator<TreeItem<File>> {

        @Override
        public int compare(TreeItem<File> o1, TreeItem<File> o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 != null && o2 != null && o1.getValue() != null && o2.getValue() != null) {
                return o1.getValue().getName().compareTo(o2.getValue().getName());
            } else if (o1 == null || o1.getValue() == null){
                return -1;
            } else if (o2 == null || o2.getValue() == null){
                return 1;
            }
            // should never happen
            throw new IllegalStateException("Error in file comparator");
        }

    }
}
