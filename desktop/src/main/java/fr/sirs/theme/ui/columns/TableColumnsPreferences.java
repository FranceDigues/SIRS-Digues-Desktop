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
package fr.sirs.theme.ui.columns;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.sirs.SIRS;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Element;
import fr.sirs.theme.ui.PojoTable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.scene.control.TableColumn;

/**
 * Classe utilisée pour stocker les préférence de l'utilisateur quant à la
 * présentation des PojoTable : largeur des colonnes, colonnes visibles...
 *
 * -> Objectif : sauvegarder en local les changements apportés par l'utilisateur
 * à la table.
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
 public class TableColumnsPreferences {

    private Class pojoClass;

    final Path filePrefPath;

    final private ObjectMapper objectMapper = new ObjectMapper();

    // Map associant la position d'une colonne (keys) aux préférences de l'utilisateur (values).
    final private Map<Integer, ColumnState> withPreferencesColumns = new HashMap<>();

    public TableColumnsPreferences() {
        this(null);
    }

    public TableColumnsPreferences(final Class pojoClass) {
        this.pojoClass = pojoClass;
        Path directory;
        try {
            directory = SirsCore.CONFIGURATION_PATH.resolve("columns_preferences");
            if (!Files.isDirectory(directory)) {
                Files.createDirectory(SirsCore.CONFIGURATION_PATH.resolve(directory));
            }
        } catch (IOException ioe) {
            SIRS.LOGGER.log(Level.WARNING, "IOException pendant l'identification du chemin d'accès au fichier de préférences utilisateur.", ioe);
            directory = SirsCore.CONFIGURATION_PATH;
        }

        final String fileName = pojoClass.getName().replace(".", "_").replace(" ", "_") + "_preferences.json";
        this.filePrefPath = directory.resolve(fileName);
        this.loadReferencesFromJsonPath();
    }

    //=========
    //Methodes
    //=========
    /**
     * Application des préférences utilisateurs aux colonnes d'une TableView de
     * en attribut d'une PojoTable
     *
     * - Ne marche pas s'il y a suppression de colonnes mais à priori pas
     * possible.
     *
     * @param columns
     */
    public void applyPreferencesToTableColumns(List<TableColumn<Element, ?>> columns) {

        try{
        Map<String, TableColumn<Element, ?>> changedColumns = new HashMap<>();
        withPreferencesColumns.forEach((preferedPosition, columnState) -> {

            columns.stream()
                    .filter(col -> {
                        return (getColumnRef(col) != null) && (getColumnRef(col).equals(columnState.getName()));
                            })
                    .findFirst()
                    .ifPresent(col -> {
                        //Affectations des préférences d'épaisseur et de visibilité
                        col.setPrefWidth(columnState.getWidth());
                        col.setVisible(columnState.isVisible());

                        //Prise en compte des modification de l'ordre d'affichage
                        //des colonnes: 
                        if (preferedPosition != columns.indexOf(col)) {
                            TableColumn<Element, ?> changedCol = col;
                            changedColumns.put(columnState.getName(), col);
                            columns.remove(col);
                            if (preferedPosition > columns.size()) {
                                columns.add(col);
                            } else {
                                columns.add(preferedPosition, col);
                            }
                        }
                    });
        });
        }catch(RuntimeException re){
                SIRS.LOGGER.log(Level.WARNING, "Exception lors de l'application des préférences utilisateurs.",re);
        }
        
    }

    /**
     * Méthode static permettant d'identifier une colonne par sont Id ou par le
     * nom de sa classe.
     *
     * En effet la plupart des colonnes d'une PojoTable ont un nom permettant de
     * les identifier. Lorsque ce n'est pas le cas (classe spécifique de
     * colonnes) on les identifie par leur nom de classe.
     *
     * @param column
     * @return
     */
    public static String getColumnRef(TableColumn<Element, ?> column) {
        try {
            return ((PojoTable.PropertyColumn) column).getName();
        } catch (ClassCastException cce) {
            return column.getClass().toString();
        }
    }

    /**
     * Ajout ou mise à jour de préférences pour une colonne.
     *
     * @param newColumnPreference
     */
    public void addColumnPreference(ColumnState newColumnPreference) {
        this.withPreferencesColumns.put(newColumnPreference.getPosition(), newColumnPreference);
    }

    /**
     * Récupère les préférences pour un nom de colonne donné.
     *
     * Attention il s'agit des préférences inclues dans la
     * Map<String, ColumnState> withPreferencesColumns ; Pas celles du fichier
     * Json.
     *
     * @param columnPosition : Position de la colonne dont on cherche les
     * préférences.
     * @return ColumnState indiquant les préférences de la colonne 'columnName'.
     */
    public ColumnState getPreferencesFor(Integer columnPosition) {
        return this.withPreferencesColumns.get(columnPosition);
    }
    
    /**
     * Indique si la colonne à l'indice 'columnPosition' est dotée de préférences 
     * utilisateur.
     * 
     * @param columnPosition : position de la colonne à vérifier.
     * @return true s'il y a des préférences associées à la position spécifiée.
     */
    public boolean withPreferences(Integer columnPosition){
        if(withPreferencesColumns == null || withPreferencesColumns.isEmpty()){
            return false;
        }
        return withPreferencesColumns.containsKey(columnPosition);
    }

    /**
     * Méthode permettant de sauvegarder les préférences (ergonomie) des
     * colonnes portées par l'instance de TableColumnsPreferences.
     *
     * Cette méthode repose sur l'usage de Jackson.
     *
     * @return boolean : indiquant si la sauvegarde à réussie (if true).
     */
    public boolean saveInJson() {

        try (final OutputStream preferencesStream = Files.newOutputStream(filePrefPath)) {

            objectMapper.writeValue(preferencesStream, withPreferencesColumns);
            return true;

        } catch (IOException ioe) {
            SIRS.LOGGER.log(Level.WARNING, "Echec lors de l'écriture des préférences de la PojoTable.", ioe);
            return false;
        }

    }

    /**
     * Charge les préférences utilisateur pour les colonnes d'une PojoTable
     * depuis un fichier Json.
     *
     * @return boolean indiquant si le chargement c'est bien déroulé (true).
     */
    final public boolean loadReferencesFromJsonPath() {

        if (filePrefPath!=null && Files.exists(filePrefPath)) {
            try (final InputStream preferencesStream = Files.newInputStream(filePrefPath)) {

                final Map<Integer, ColumnState> readPref = objectMapper.readValue(preferencesStream, new TypeReference<Map<Integer, ColumnState>>() {
                });

                //Si on trouve des préférences on met à jour la Map withPreferencesColumns
                if ((readPref == null) || (readPref.isEmpty())) {
                    SIRS.LOGGER.log(Level.INFO, "Fichier {0} vide.", filePrefPath.toString());
                } else {
                    readPref.forEach((colPosition, state) -> {
                        this.withPreferencesColumns.put(colPosition, state);
                    });
                }
                return true;
            } catch (IOException ioe) {
                SIRS.LOGGER.log(Level.WARNING, "Exception loading columns preferences for a PojoTable.", ioe);
                return false;
            }
        } else {
             SIRS.LOGGER.log(Level.INFO, "No preference files found for columns preferences at {0}", filePrefPath.toString());
             return false;
        }

    }

    //===================
    //Getter and Setter 
    //===================
    public Class getPojoClass() {
        return pojoClass;
    }

    // Utilisable uniquement pour les TableColumnsPreferences 
    public void setPojoClass(Class pojoClass) {
        this.pojoClass = pojoClass;
    }

    public Map<Integer, ColumnState> getWithPreferencesColumns() {
        return withPreferencesColumns;
    }

}
