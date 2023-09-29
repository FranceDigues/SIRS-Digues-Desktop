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
package fr.sirs;

import com.sun.javafx.PlatformUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import fr.sirs.core.Repository;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.*;
import fr.sirs.theme.ui.AbstractFXElementPane;
import fr.sirs.theme.ui.FXElementContainerPane;
import fr.sirs.theme.ui.columns.TableColumnsPreferences;
import fr.sirs.util.FXPreferenceEditor;
import fr.sirs.util.ReferenceTableCell;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.property.Reference;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Cache;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.util.ComboBoxCompletion;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Constants used for project.
 *
 * @author Johann Sorel
 */
public final class SIRS extends SirsCore {

    public static final CoordinateReferenceSystem CRS_WGS84 = CommonCRS.WGS84.normalizedGeographic();

    /** Cette géométrie sert de base pour tous les nouveaux troncons */
    public static final Geometry DEFAULT_TRONCON_GEOM_WGS84;
    static {
        DEFAULT_TRONCON_GEOM_WGS84 = new GeometryFactory().createLineString(new Coordinate[]{
            new Coordinate(0, 48),
            new Coordinate(5, 48)
        });
        JTS.setCRS(DEFAULT_TRONCON_GEOM_WGS84, CRS_WGS84);
    }

    public static final String COLOR_INVALID_ICON = "#aa0000";
    public static final String COLOR_VALID_ICON = "#00aa00";

    public static final Image ICON = new Image(SirsCore.class.getResource("/fr/sirs/icon.png").toString());

    public static final Image ICON_ADD_WHITE    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_PLUS,22,Color.WHITE),null);
    public static final Image ICON_CHAIN_WHITE    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CHAIN_ALIAS,22,Color.WHITE),null);
    public static final Image ICON_COPY_WHITE   = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_COPY_ALIAS,22,Color.WHITE),null);
    public static final Image ICON_ADD_BLACK    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_PLUS,16,Color.BLACK),null);
    public static final Image ICON_ARROW_RIGHT_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_ARROW_RIGHT,16,Color.BLACK),null);
    public static final Image ICON_CLOCK_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CLOCK_O,22,Color.WHITE),null);
    public static final Image ICON_SEARCH_WHITE       = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_SEARCH,22,Color.WHITE),null);
    public static final Image ICON_ARCHIVE_WHITE       = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_ARCHIVE,22,Color.WHITE),null);
    public static final Image ICON_TRASH_WHITE        = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TRASH_O,22,Color.WHITE),null);
    public static final Image ICON_CROSSHAIR_BLACK= SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CROSSHAIRS,22,Color.BLACK),null);
    public static final Image ICON_CARET_UP_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_UP,16,Color.BLACK),null);
    public static final Image ICON_CARET_DOWN_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_DOWN,16,Color.BLACK),null);
    public static final Image ICON_CARET_LEFT = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_LEFT,22,Color.WHITE),null);
    public static final Image ICON_CARET_RIGHT = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_RIGHT,22,Color.WHITE),null);
    public static final Image ICON_FILE_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILE,22,Color.WHITE),null);
    public static final Image ICON_FILE_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILE,22,Color.BLACK),null);
    public static final Image ICON_TABLE_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TABLE,22,Color.WHITE),null);
    public static final Image ICON_UNDO_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_UNDO, 22, Color.BLACK),null);
    public static final Image ICON_INFO_BLACK_16 = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_INFO, 16, Color.BLACK),null);
    public static final Image ICON_INFO_CIRCLE_BLACK_16 = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_INFO_CIRCLE, 16, Color.BLACK),null);
    public static final Image ICON_EYE_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EYE, 16, Color.BLACK),null);
    public static final Image ICON_COMPASS_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_COMPASS, 22, Color.WHITE),null);
    public static final Image ICON_EDIT_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILE_O, 16, Color.BLACK),null);
    public static final Image ICON_EDITION = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EDIT_ALIAS, 16, Color.BLACK),null);
    public static final Image ICON_EDITION_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EDIT_ALIAS, 22, Color.WHITE),null);
    public static final Image ICON_PRINT_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_PRINT, 16, Color.BLACK),null);
    public static final Image ICON_ROTATE_LEFT_ALIAS = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_ROTATE_LEFT_ALIAS, 16, Color.BLACK),null);
    public static final Image ICON_IMPORT_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_DOWNLOAD,22,Color.WHITE),null);
    public static final Image ICON_EXPORT_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_SAVE_ALIAS,22,Color.WHITE),null);
    public static final Image ICON_VIEWOTHER_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_BARS,22,Color.WHITE),null);
    public static final Image ICON_FILTER_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILTER,22,Color.WHITE),null);

    public static final Image ICON_EXCLAMATION_CIRCLE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_CIRCLE, 16, Color.decode(COLOR_INVALID_ICON)),null);
    public static final Image ICON_CHECK_CIRCLE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CHECK_CIRCLE, 16, Color.decode(COLOR_VALID_ICON)),null);
    public static final String COLOR_WARNING_ICON = "#EEB422";
    public static final Image ICON_EXCLAMATION_TRIANGLE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_TRIANGLE, 16, Color.decode(COLOR_WARNING_ICON)),null);
    public static final Image ICON_EXCLAMATION_TRIANGLE_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_TRIANGLE, 16, Color.BLACK),null);

    public static final Image ICON_CHECK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CHECK, 16, Color.decode(COLOR_VALID_ICON)),null);

    public static final Image ICON_LINK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXTERNAL_LINK, 16, Color.BLACK),null);
    public static final Image ICON_WARNING = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_TRIANGLE, 16, Color.BLACK),null);
    public static final Image ICON_REFRESH_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_REFRESH,22,Color.WHITE),null);
    public static final Image ICON_MAGNET_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_MAGNET,22,Color.WHITE),null);

    public static final String CSS_PATH = "/fr/sirs/theme.css";
    public static final String CSS_PATH_CALENDAR = "/fr/sirs/calendar.css";


    private static final Cache<String, Callback<TableColumn.CellDataFeatures, ObservableValue>> CELL_VALUE_FACTORY = new Cache<>(50, 0, false);

    /** Cache row factories for reference properties, preventing from creating a new one for each column. */
    private static final Cache<Class, Callback<TableColumn, TableCell>> REF_CELL_FACTORIES = new Cache<>(50, 0, false);

    /**
     * Cache photographs loaded in memory, to avoid multiple panels to load the
     * same image concurrently. No soft reference used here, because the aim is
     * to use as little memory as possible, so images will be garbaged as soon
     * as possible.
     */
    private static final Cache<String, Image> IMAGE_CACHE = new Cache<>(16, 0, false);

    public static final Predicate<Element> CONSULTATION_PREDICATE = e -> false;
    public static final Predicate<Element> EDITION_PREDICATE = e -> true;

    private static AbstractRestartableStage LAUNCHER;
    public static void setLauncher(AbstractRestartableStage currentWindow) {
        LAUNCHER=currentWindow;
    }
    public static AbstractRestartableStage getLauncher() {
        return LAUNCHER;
    }

    public static Loader LOADER;

    private static Stage PREFERENCE_EDITOR;

    private SIRS(){};

    public static synchronized Stage getPreferenceEditor() {
        if (PREFERENCE_EDITOR == null) {
            PREFERENCE_EDITOR = new FXPreferenceEditor();
            PREFERENCE_EDITOR.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, (evt) -> {
                synchronized (SIRS.class) {
                    PREFERENCE_EDITOR = null;
                }
            });
        }
        return PREFERENCE_EDITOR;
    }

    public static void loadFXML(Parent candidate) {
        final Class modelClass = null;
        loadFXML(candidate, modelClass);
    }

    /**
     * Load FXML document matching input controller. If a model class is given,
     * we'll try to load a bundle for text internationalization.
     * @param candidate The controller object to get FXMl for.
     * @param modelClass A class which will be used for bundle loading.
     */
    public static void loadFXML(Parent candidate, final Class modelClass) {
        ResourceBundle bundle = null;
        if (modelClass != null) {
            try{
                bundle = ResourceBundle.getBundle(modelClass.getName(), Locale.FRENCH,
                        Thread.currentThread().getContextClassLoader());
            }catch(MissingResourceException ex){
                LOGGER.log(Level.INFO, "Missing bundle for : {0}", modelClass.getName());
            }
        }
        loadFXML(candidate, bundle);
    }

    public static void loadFXML(Parent candidate, final ResourceBundle bundle) {
        loadFXML(candidate, candidate.getClass(), bundle);
    }

    public static void loadFXML(Parent candidate, final Class fxmlClass, final ResourceBundle bundle) {
        ArgumentChecks.ensureNonNull("JavaFX controller object", candidate);
        final String fxmlpath = "/"+fxmlClass.getName().replace('.', '/')+".fxml";
        final URL resource = fxmlClass.getResource(fxmlpath);
        if (resource == null) {
            throw new RuntimeException("No FXMl document can be found for path : "+fxmlpath);
        }
        final FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(candidate);
        loader.setRoot(candidate);
        //in special environement like osgi or other, we must use the proper class loaders
        //not necessarly the one who loaded the FXMLLoader class
        loader.setClassLoader(fxmlClass.getClassLoader());

        if(bundle!=null) loader.setResources(bundle);

        fxRunAndWait(() -> {
            try {
                loader.load();
            } catch (IOException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        });
    }

    /**
     * Reconstruit une liste d'éléments depuis la liste en entrée et le {@link Repository} donné.
     * Si la liste en paramètre est nulle ou vide, une liste vide est renvoyée.
     * Si elle contient des éléments, elle est renvoyée telle quel.
     * Si c'est une liste d'ID, on construit une liste des élements correspondants.
     *
     * @param sourceList La liste depuis laquelle on doit reconstruire la liste des éléments.
     * @param repo Le repository servant à retrouver les éléments depuis leur ID.
     * @return Une liste d'éléments. Peut être vide, mais jamais nulle.
     */
    public static ObservableList<Element> toElementList(final List sourceList, final AbstractSIRSRepository repo) {
        if (sourceList == null) {
            return FXCollections.observableArrayList();

        } else if (!sourceList.isEmpty() && sourceList.get(0) instanceof Element) {
            return observableList(sourceList);
        } else if (repo == null) {
            return FXCollections.observableArrayList();
        } else {
            return observableList(repo.get(sourceList));
        }
    }


    /**
     * Tente de trouver un éditeur d'élément compatible avec l'objet passé en paramètre.
     * Contrairement à la méthode {@link #createFXPaneForElement(fr.sirs.core.model.Element) }, l'éditeur
     * retourné ici est décoré avec un bandeau pour la sauvegarde.
     *
     * @param pojo L'objet pour lequel trouver un éditeur.
     * @return Un éditeur pour l'objet d'entrée, ou null si aucun ne peut être
     * trouvé. L'éditeur aura déjà été initialisé avec l'objet en paramètre.
     */
    public static AbstractFXElementPane generateEditionPane(final Element pojo) {
        return generateEditionPane(pojo, CONSULTATION_PREDICATE);
    }

    /**
     * Tente de trouver un éditeur d'élément compatible avec l'objet passé en paramètre.
     * Contrairement à la méthode {@link #createFXPaneForElement(fr.sirs.core.model.Element) }, l'éditeur
     * retourné ici est décoré avec un bandeau pour la sauvegarde.
     *
     * @param pojo L'objet pour lequel trouver un éditeur.
     * @param editionPredicate Prédicat d'édition du panneau à l'ouverture
     * @return Un éditeur pour l'objet d'entrée, ou null si aucun ne peut être
     * trouvé. L'éditeur aura déjà été initialisé avec l'objet en paramètre.
     */
    public static AbstractFXElementPane generateEditionPane(final Element pojo, final Predicate<Element> editionPredicate) {
        return new FXElementContainerPane((Element) pojo, editionPredicate);
    }

    /**
     *
     * @param element Trouve un éditeur pour l'objet donné
     * @return L'objet pour lequel trouver un éditeur.
     * @throws ClassNotFoundException Si aucun éditeur n'est trouvé pour l'objet demandé.
     * @throws NoSuchMethodException Si aucun constructeur prenant l'objet en paramètre n'est disponible pour l'éditeur choisi.
     * @throws InstantiationException Si une erreur survient pendant l'initialisation de l'éditeur.
     * @throws IllegalAccessException Si une règle de sécurité empêche l'initialisation de l'éditeur.
     * @throws IllegalArgumentException S le constructeur de l'éditeur ne peut traiter l'objet en paramètre.
     * @throws InvocationTargetException Si une erreur survient pendant l'initialisation de l'éditeur.
     */
    public static AbstractFXElementPane createFXPaneForElement(final Element element)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Choose the pane adapted to the specific structure.
        final String className = "fr.sirs.theme.ui.FX" + element.getClass().getSimpleName() + "Pane";
        final Class controllerClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        final Constructor cstr = controllerClass.getConstructor(element.getClass());
        return (AbstractFXElementPane) cstr.newInstance(element);
    }

    /**
     * initialize ComboBox items using input list. We also activate completion.
     * @param comboBox The combo box to set value on.
     * @param items The items we want into the ComboBox.
     * @param current the element to select by default.
     */
    public static void initCombo(final ComboBox comboBox, final ObservableList items, final Object current) {
        initCombo(comboBox, items, current, new SirsStringConverter());
    }

    /**
     * initialize ComboBox items using input list. We also activate completion.
     * @param comboBox The combo box to set value on.
     * @param items The items we want into the ComboBox.
     * @param current the element to select by default.
     * @param hideArchived determine whether the combo shall include or not the archived element from the items list
     */
    public static void initCombo(final ComboBox comboBox, final ObservableList items, final Object current, final boolean hideArchived, final boolean useCurrent) {
        initCombo(comboBox, items, current, new SirsStringConverter(), hideArchived, useCurrent);
    }

    /**
     * initialize ComboBox items using input list. We also activate completion.
     * @param comboBox The combo box to set value on.
     * @param items The items we want into the ComboBox.
     * @param current the element to select by default.
     * @param converter the StringConverter to use to identify the items.
     */
    public static void initCombo(final ComboBox comboBox, final ObservableList items, final Object current, final StringConverter converter) {
        initCombo(comboBox, items, current, converter, false, true);
    }

    /**
     * initialize ComboBox items using input list. We also activate completion.
     * @param comboBox The combo box to set value on.
     * @param items The items we want into the ComboBox.
     * @param current the element to select by default.
     * @param converter the StringConverter to use to identify the items.
     * @param hideArchived determine whether the combo shall include or not the archived element from the items list
     */
    public static void initCombo(final ComboBox comboBox, final ObservableList items, final Object current, final StringConverter converter, final boolean hideArchived, final boolean useCurrent) {
        ArgumentChecks.ensureNonNull("items", items);
        comboBox.setConverter(converter);
        ObservableList finalItems = null;
        if (hideArchived && items != null && !items.isEmpty()) {
            Object item = items.get(0);

            if (item instanceof AvecFinTemporelle) {
                // List of classes that are not TronconDigue children and are impacted by the "hide archived element" option in the SIRS app.
                List<String> classesToFilterArchived = Arrays.asList("fr.sirs.core.model.TronconLit",
                        "fr.sirs.core.model.AireStockageDependance", "fr.sirs.core.model.AmenagementHydraulique", "fr.sirs.core.model.AutreDependance",
                        "fr.sirs.core.model.CheminAccesDependance", "fr.sirs.core.model.OuvrageVoirieDependance");

                final Class clazz;
                final String className;
                if (item instanceof Preview) {
                    Preview prev = (Preview) item;
                    clazz = prev.getJavaClassOr(null);
                    className = prev.getElementClass();
                } else {
                    clazz = item.getClass();
                    className = clazz.getCanonicalName();
                }

                if ((clazz != null) && (TronconDigue.class.isAssignableFrom(clazz)) || (classesToFilterArchived.contains(className))) {
                    List<? extends AvecFinTemporelle> list = items;
                    finalItems = SIRS.observableList(list.stream().filter(new AvecFinTemporelle.IsNotArchivedPredicate()).collect(Collectors.toList()));
                }
            }
        }
        if (finalItems == null) finalItems = items;
        if (finalItems instanceof SortedList) {
            comboBox.setItems(finalItems);
        } else {
            comboBox.setItems(finalItems.sorted(Comparator.comparing(converter::toString)));
        }
        comboBox.setEditable(true);
        if (useCurrent)
            comboBox.getSelectionModel().select(current);
        else
            comboBox.getSelectionModel().select(finalItems == null ? null : finalItems.get(0));
        ComboBoxCompletion.autocomplete(comboBox);
    }

    /**
     * Convert byte number given in parameter in a human readable string. It tries
     * to fit the best unit. Ex : if you've got a number higher than a thousand,
     * input byte number will be expressed in kB. If you've got more than a million,
     * you've got it as MB
     * @param byteNumber Byte quantity to display
     * @return A string displaying byte number converted in fitting unit, along with unit symbol.
     */
    public static String toReadableSize(final long byteNumber) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        if (byteNumber < 0) {
            return "inconnu";
        } else if (byteNumber < 1e3) {
            return "" + byteNumber + " octets";
        } else if (byteNumber < 1e6) {
            return "" + format.format(byteNumber / 1e3) + " ko";
        } else if (byteNumber < 1e9) {
            return "" + format.format(byteNumber / 1e6) + " Mo";
        } else if (byteNumber < 1e12) {
            return "" + format.format(byteNumber / 1e9) + " Go";
        } else {
            return "" + (byteNumber / 1e12) + " To";
        }
    }

    public static ObservableList view(ObservableList ... listes){
        return new ViewList(listes);
    }

    private static final class ViewList extends ObservableListBase implements ListChangeListener{

        private final ObservableList[] listes;

        public ViewList(ObservableList ... listes) {
            this.listes = listes;

            for(ObservableList lst : listes){
                lst.addListener(this);
            }
        }

        @Override
        public Object get(int index) {
            for(int i=0;i<listes.length;i++){
                int size = listes[i].size();
                if(size<=index){
                    index -= size;
                }else{
                    return listes[i].get(index);
                }
            }
            throw new ArrayIndexOutOfBoundsException(index);
        }

        @Override
        public int size() {
            int size = 0;
            for (ObservableList liste : listes) {
                size += liste.size();
            }
            return size;
        }

        private int getOffset(ObservableList lst){
            int size = 0;
            for (ObservableList liste : listes) {
                if(lst==liste) break;
                size += liste.size();
            }
            return size;
        }

        @Override
        public void onChanged(ListChangeListener.Change c) {
            final int offset = getOffset(c.getList());

            beginChange();
            while (c.next()) {
                if (c.wasPermutated()) {
                    //permutate
                    beginChange();
                    final int[] perms = new int[c.getTo()-c.getFrom()];
                    for (int i = c.getFrom(),k=0; i < c.getTo(); ++i,k++) {
                        perms[k] = c.getPermutation(i);
                    }
                    nextPermutation(offset+c.getFrom(), offset+c.getTo(), perms);
                    endChange();
                } else if (c.wasUpdated()) {
                    //update item
                    beginChange();
                    nextUpdate(offset+c.getFrom());
                    endChange();
                } else {
                    beginChange();
                    if(c.wasUpdated()){
                        throw new UnsupportedOperationException("Update events not supported.");
                    }else if(c.wasAdded()){
                        nextAdd(offset+c.getFrom(), offset+c.getTo());
                    }else if(c.wasRemoved()){
                        nextReplace(offset+c.getFrom(), offset+c.getTo(), c.getRemoved());
                    }
                    endChange();
                }
            }
            endChange();
        }
    }

    /**
     * Try to open given file on system.
     * @param toOpen The file open on underlying system.
     * @return True if we succeeded opening file on system, false otherwise.
     */
    public static Task<Boolean> openFile(final Path toOpen) {
        return openFile(toOpen.toAbsolutePath().toFile());
    }

    /**
     * Try to open given file on system.
     * @param toOpen The file open on underlying system.
     * @return True if we succeeded opening file on system, false otherwise.
     */
    public static Task<Boolean> openFile(final File toOpen) {
        return TaskManager.INSTANCE.submit("Ouverture d'un fichier", () -> {
            try {
                final Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(toOpen);
                } else if (desktop.isSupported(Desktop.Action.EDIT)) {
                    desktop.edit(toOpen);
                } else {
                    throw new IOException("Impossible de communiquer avec l'OS.");
                }
            } catch (IOException e) {
                /*
                 * HACK : on Windows, it seems that some file associations cannot be found
                 * using above method (Ex : .odt), so we try running following command as
                 * a fallback.
                 */
                if (PlatformUtil.isWindows()) {
                    Runtime.getRuntime().exec(new String[]{"cmd /c start", toOpen.toURI().toString()});
                } else {
                    throw e;
                }
            }
            return true;
        });
    }

    /**
     * Attach a listener on iven table {@link TableView#widthProperty() } to set
     * automatically itss columns preferred size to fill entire table.
     * Note : Only resizable / visible columns are affected.
     *
     * @param target Table view to operate on.
     * @return Listener attached to given table to manage its column sizes.
     */
    public static InvalidationListener setColumnResize(final TableView target) {
        return setColumnResize(target, null);
    }



    public static InvalidationListener setColumnResize(final TableView target, final TableColumnsPreferences preferences) {
        final ColumnAutomaticResize resize = new ColumnAutomaticResize(target, preferences);
        target.widthProperty().addListener(resize);
        return resize;
    }
    /**
     * An invalidation listener to put on a table width property to set its column's
     * preferred size to fill entire width.
     */
    private static class ColumnAutomaticResize implements InvalidationListener {

        private final WeakReference<TableView> target;
        private final WeakReference<TableColumnsPreferences> preferences;

        public ColumnAutomaticResize(TableView target) {
            this(target, null);
        }

        public ColumnAutomaticResize(TableView target, final TableColumnsPreferences preferences) {
            this.target = new WeakReference<>(target);
            this.preferences = new WeakReference<>(preferences);
        }

        @Override
        public void invalidated(Observable observable) {
            final TableView<?> tView = target.get();
            if (tView == null)
                return;
            final TableColumnsPreferences pref = preferences.get();
            final boolean notNull = pref != null;

            double unresizable = 0;
            final ArrayList<TableColumn> resizableColumns = new ArrayList<>();
            for (final TableColumn col : tView.getColumns()) {
                if (!col.isResizable() || (notNull && (pref.withPreferences(tView.getColumns().indexOf(col))) ) ){
                    // Afin d'appliquer la largeur sauvegardé par l'utilisateur,
                    // on considère les colonnes avec préférences de la même manière
                    // que les colonnes non-redimensionnable.
                    unresizable += col.getWidth();
                } else if (col.isVisible()) {
                    resizableColumns.add(col);
                }
            }

            final double prefWidth = (tView.getWidth() - unresizable) / resizableColumns.size();
            for (final TableColumn col : resizableColumns) {
                if (prefWidth > col.getPrefWidth() && prefWidth <= col.getMaxWidth()) {
                    col.setPrefWidth(prefWidth);
                }
            }
        }
    }

    /**
     * Find a cell factory to handle given reference.
     *
     * @param ref Reference to the type being held in returned {@link ReferenceTableCell}.
     * @return A {@link ReferenceTableCell}, got from cache or newly created. never null.
     */
    public static Callback getOrCreateTableCellFactory(final Reference ref) {
        final Class refClass = ref.ref();
        try {
            return REF_CELL_FACTORIES.getOrCreate(refClass, () -> (TableColumn param) -> new ReferenceTableCell(ref.ref()));
        } catch (Exception ex) {
            throw new SirsCoreRuntimeException(ex);
        }
    }

    public static Callback getOrCreateCellValueFactory(final String pName) {
        try {
            return CELL_VALUE_FACTORY.getOrCreate(pName, () -> (Callback) new PropertyValueFactory<>(pName));
        } catch (Exception e) {
            throw new SirsCoreRuntimeException(e);
        }
    }

    /**
     * Load image pointed by given path, or return it directly if it's already
     * cached.
     *
     * /!\ Image dimension is forced to ~ 512x512 pixels to limit memory usage.
     * However, we don't know the exact amount of memory used, as we preserve
     * image ratio.
     *
     * @param uri Path to the image.
     * @return Loaded image.
     * @throws RuntimeException If loading fails.
     */
    public static Image getOrLoadImage(final String uri) {
        try {
            return IMAGE_CACHE.getOrCreate(uri, () -> {
                final Image img = new Image(uri, 512, 512, true, false, false);
                if (img.isError()) {
                    return null;
                }
                return img;
            });
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            throw new SirsCoreRuntimeException("Unloadable image", e);
        }
    }
}
