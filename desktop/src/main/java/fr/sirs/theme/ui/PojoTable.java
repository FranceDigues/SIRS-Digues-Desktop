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
package fr.sirs.theme.ui;

import com.sun.javafx.property.PropertyReference;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.*;

import static fr.sirs.SIRS.AUTHOR_FIELD;
import static fr.sirs.SIRS.COMMENTAIRE_FIELD;
import static fr.sirs.SIRS.DATE_MAJ_FIELD;
import static fr.sirs.SIRS.FOREIGN_PARENT_ID_FIELD;
import static fr.sirs.SIRS.GEOMETRY_MODE_FIELD;
import static fr.sirs.SIRS.LATITUDE_MAX_FIELD;
import static fr.sirs.SIRS.LATITUDE_MIN_FIELD;
import static fr.sirs.SIRS.LONGITUDE_MAX_FIELD;
import static fr.sirs.SIRS.LONGITUDE_MIN_FIELD;
import static fr.sirs.SIRS.VALID_FIELD;
import static fr.sirs.SIRS.ID_FIELD;
import static fr.sirs.SIRS.REVISION_FIELD;
import static fr.sirs.SIRS.NEW_FIELD;
import static fr.sirs.core.SirsCore.*;

import fr.sirs.core.Repository;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.*;
import fr.sirs.theme.ColumnOrder;
import fr.sirs.util.*;
import fr.sirs.theme.ui.columns.ColumnState;
import fr.sirs.theme.ui.columns.TableColumnsPreferences;
import fr.sirs.theme.ui.pojotable.ChoiceStage;
import fr.sirs.theme.ui.pojotable.CopyElementException;
import fr.sirs.theme.ui.pojotable.DeleteColumn;
import fr.sirs.theme.ui.pojotable.Deletor;
import fr.sirs.theme.ui.pojotable.DistanceComputedPropertyColumn;
import fr.sirs.theme.ui.pojotable.EditColumn;
import fr.sirs.theme.ui.pojotable.ElementCopier;
import fr.sirs.theme.ui.pojotable.ExportAction;
import fr.sirs.theme.ui.pojotable.ImportAction;
import fr.sirs.theme.ui.pojotable.ShowOnMapColumn;
import fr.sirs.theme.ui.pojotable.SimpleCell;
import fr.sirs.ui.Growl;
import fr.sirs.util.odt.ODTUtils;
import fr.sirs.util.property.Computed;
import fr.sirs.util.property.Internal;
import fr.sirs.util.property.Reference;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.util.ArgumentChecks;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.filter.FXFilterBuilder;
import org.geotoolkit.gui.javafx.util.FXBooleanCell;
import org.geotoolkit.gui.javafx.util.FXEnumTableCell;
import org.geotoolkit.gui.javafx.util.FXLocalDateCell;
import org.geotoolkit.gui.javafx.util.FXLocalDateTimeCell;
import org.geotoolkit.gui.javafx.util.FXNumberCell;
import org.geotoolkit.gui.javafx.util.FXStringCell;
import org.geotoolkit.gui.javafx.util.FXTableView;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.odftoolkit.simple.TextDocument;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @author Samuel Andrés (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 * @author Maxime Gavens (Geomatys)
 */
public class PojoTable extends BorderPane implements Printable {

    private static final Callback<TableColumn.CellDataFeatures, ObservableValue> DEFAULT_VALUE_FACTORY = param -> new SimpleObjectProperty(param.getValue());
    private static final Predicate DEFAULT_VISIBLE_PREDICATE = o -> o != null;

    protected static final String BUTTON_STYLE = "buttonbar-button";
    private static final Image ICON_SHOWONMAP = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_GLOBE, 16, FontAwesomeIcons.DEFAULT_COLOR), null);

    public static final String[] COLUMNS_TO_IGNORE = new String[]{
            AUTHOR_FIELD, VALID_FIELD, FOREIGN_PARENT_ID_FIELD, LONGITUDE_MIN_FIELD,
            LONGITUDE_MAX_FIELD, LATITUDE_MIN_FIELD, LATITUDE_MAX_FIELD, DATE_MAJ_FIELD,
            COMMENTAIRE_FIELD, GEOMETRY_MODE_FIELD, REVISION_FIELD, ID_FIELD, NEW_FIELD,
            TIMESTAMP_START_DATE_START_VALIDITY, TIMESTAMP_END_DATE_START_VALIDITY, TIMESTAMP_END_DATE_END_VALIDITY
    };

    /**
     * Design rows for {@link Element} objects, making its look change according
     * to {@link Element#validProperty() }
     */
    private final Callback<TableView<Element>, TableRow<Element>> rowFactory = (TableView<Element> param) -> new ValidityRow();

    protected final Class pojoClass;
    protected final AbstractSIRSRepository repo;
    protected final Session session = Injector.getBean(Session.class);
    protected final TableView<Element> uiTable = new FXTableView<>();
    private final LabelMapper labelMapper;

    /**
     * Editabilité du tableau (possibilité d'ajout et de suppression des
     * éléments via la barre d'action sur la droite, plus édition des cellules)
     */
    protected final BooleanProperty editableProperty = new SimpleBooleanProperty(true);
    /**
     * Editabilité des cellules tableau
     */
    protected final BooleanProperty cellEditableProperty = new SimpleBooleanProperty();
    /**
     * Parcours fiche par fiche
     */
    protected final BooleanProperty fichableProperty = new SimpleBooleanProperty(true);
    /**
     * Accès à la fiche détaillée d'un élément particulier
     */
    protected final BooleanProperty detaillableProperty = new SimpleBooleanProperty(true);
    /**
     * Possibilité de faire une recherche sur le contenu de la table
     */
    protected final BooleanProperty searchableProperty = new SimpleBooleanProperty(true);
    /**
     * Ouvrir l'editeur sur creation d'un nouvel objet
     */
    protected final BooleanProperty openEditorOnNewProperty = new SimpleBooleanProperty(true);
    /**
     * Créer un nouvel objet à l'ajout.
     *
     * Si cette propriété contient "vrai", l'action sur le bouton d'ajout sera
     * de créer de nouvelles instances ajoutées dans le tableau. Si elle
     * contient "faux", l'action sur le bouton d'ajout sera de proposer l'ajout
     * de liens vers des objets préexistants.
     */
    protected final BooleanProperty createNewProperty = new SimpleBooleanProperty(true);
    /**
     * Importer des points. Default : false
     */
    protected final BooleanProperty importPointProperty = new SimpleBooleanProperty(false);

    /**
     * Composant de filtrage. Propose de filtrer la liste d'objets actuels en
     * éditant des contraintes sur leur propriété.
     */
    protected FXFilterBuilder uiFilterBuilder;
//    protected final TitledPane uiFilterPane = new TitledPane();
    private final Button resetFilterBtn = new Button("Réinitialiser");
    private final Button applyFilterBtn = new Button("Filtrer");

    // Icônes de la barre d'action
    protected final ImageView searchNone = new ImageView(SIRS.ICON_SEARCH_WHITE);

    // Barre de droite : manipulation du tableau et passage en mode parcours de fiche
    protected final Button uiRefresh = new Button(null, new ImageView(SIRS.ICON_REFRESH_WHITE));
    protected final ToggleButton uiFicheMode = new ToggleButton(null, new ImageView(SIRS.ICON_FILE_WHITE));
    protected final Button uiImport = new Button(null, new ImageView(SIRS.ICON_IMPORT_WHITE));
    protected final Button uiExport = new Button(null, new ImageView(SIRS.ICON_EXPORT_WHITE));
    protected final Button uiSearch = new Button(null, searchNone);
    protected final Button uiAdd = new Button(null, new ImageView(SIRS.ICON_ADD_WHITE));
    protected final ToggleButton uiCopyTo = new ToggleButton(null, new ImageView(SIRS.ICON_COPY_WHITE));
    protected final Button uiDelete = new Button(null, new ImageView(SIRS.ICON_TRASH_WHITE));
    protected final ToggleButton uiFilter = new ToggleButton(null, new ImageView(SIRS.ICON_FILTER_WHITE));
    protected final HBox searchEditionToolbar = new HBox(uiRefresh, uiFicheMode, uiImport, uiExport, uiSearch, uiAdd, uiCopyTo, uiDelete, uiFilter);

    // Barre de gauche : navigation dans le parcours de fiches
    private final Button uiPrevious = new Button("", new ImageView(SIRS.ICON_CARET_LEFT));
    private final Button uiNext = new Button("", new ImageView(SIRS.ICON_CARET_RIGHT));
    private final Button uiCurrent = new Button();
    protected final HBox navigationToolbar = new HBox(uiPrevious, uiCurrent, uiNext);

    protected final ProgressIndicator searchRunning = new ProgressIndicator();
    /**
     * Supplier providing table data. Returned list will be used to set
     * {@link #allValues}
     */
    private final SimpleObjectProperty<Supplier<ObservableList<Element>>> dataSupplierProperty = new SimpleObjectProperty<>();
    /**
     * Brut values returned by {@link #dataSupplier}.
     */
    private ObservableList<Element> allValues;
    /**
     * Values from {@link #allValues}, after applying text/property filters.
     */
    private ObservableList<Element> filteredValues;
    //Cette liste est uniquement pour de la visualisation, elle peut contenir un enregistrement en plus
    //afin d'afficher la barre de scroll horizontale.
    private SortedList<Element> decoratedValues;

    protected final StringProperty currentSearch = new SimpleStringProperty("");
    protected final BorderPane topPane;

    private final VBox filterContent;

    // Colonnes de suppression et d'ouverture d'éditeur.
    protected final DeleteColumn deleteColumn = new DeleteColumn(createNewProperty, this::deletePojos, DEFAULT_VALUE_FACTORY, DEFAULT_VISIBLE_PREDICATE);
    protected final EditColumn editCol = new EditColumn(DEFAULT_VALUE_FACTORY, this::editPojo, DEFAULT_VISIBLE_PREDICATE);

    /**
     * The element to set as parent for any created element using {@linkplain #createPojo()
     * }.
     */
    protected final ObjectProperty<Element> parentElementProperty = new SimpleObjectProperty<>();
    /**
     * The element to set as owner for any created element using {@linkplain #createPojo()
     * }. On the contrary to the parent, the owner purpose is not to contain the
     * created pojo, but to reference it.
     */
    protected final ObjectProperty<Element> ownerElementProperty = new SimpleObjectProperty<>();

    //Partie basse pour les commentaires et photos
    private final FXCommentPhotoView commentPhotoView;

    /**
     * Task object designed for asynchronous update of the elements contained in
     * the table.
     */
    protected final SimpleObjectProperty<Task> tableUpdaterProperty = new SimpleObjectProperty<>();

    protected final StackPane notifier = new StackPane();

    protected final StringProperty titleProperty = new SimpleStringProperty();

    // Default deletor
    private Consumer deletor;

    protected ElementCopier elementCopier;

    //---------------------------------------------
    // Attributs pour les préférences utilisateur :
    //---------------------------------------------
    // Préférences utilisateur pour cette PojoTable.
    TableColumnsPreferences columnsPreferences;
    //A déplacer avec les autres attributs :
    private BooleanProperty isColumnModifying = new SimpleBooleanProperty(false);
    // ScheduledExecutorService permettant d'instancier un délais avant de sauvegarder les modifications apportées à des colonnes
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    //Liste des colonnes modifiées :
    private Set<Integer> modifiedColumnsIndices = new HashSet<>();

    /**
     * Un bug de javafx est constaté lorsque l'on déplace des colonnes d'une
     * TableView possédant certaines colonnes non-visibles. Le déplacement est
     * alors erronné. Cette variable indique que ce bug n'a pas encore était
     * corrigé dans javafx. Plusieurs corrections sont apportées dans le projet
     * pour palier à ce bug. Elle doivent utiliser cette variable. Si ce bug
     * javafx est corrigé, on peut chercher les références à cette variable.
     */
    public static final boolean BUG_JAVAFX_COLUMN_MOVE = true;

    /*
    Objet auquel sont rattachés les éléments de la pojoTable.
    On utilise une référence différente de ownerElementProperty et parentProperty car ces deux attributs sont déjà utilisés à des fins spécifiques
    et on risque de créer des dysfonctionnement si on leur affecte une valeur quand ils n'en ont pas jusqu'à présent.
    La propriété "container" est particulièrement destinée à être capable de déterminer l'entité dans laquelle est incluse
    la PojoTable afin de filter les éléments à ajouter sur un tronçon identique.
     */
    private final ObjectProperty<? extends Element> container;

    public PojoTable(final Class pojoClass, final String title, final ObjectProperty<? extends Element> container) {
        this(pojoClass, title, container, null, true);
    }

    public PojoTable(final Class pojoClass, final String title, final ObjectProperty<? extends Element> container, final boolean applyPreferences) {
        this(pojoClass, title, container, null, applyPreferences);
    }

    public PojoTable(final AbstractSIRSRepository repo, final String title, final ObjectProperty<? extends Element> container) {
        this(repo.getModelClass(), title, container, repo, true);
    }

    public PojoTable(final Class pojoClass, final String title) {
        this(pojoClass, title, (ObjectProperty<? extends Element>) null);
    }

    public PojoTable(final AbstractSIRSRepository repo, final String title) {
        this(repo, title, (ObjectProperty<? extends Element>) null);
    }

    /**
     * @param applyPreferences : boolean value use to indicate to not apply
     * preferences in the constructor. It is used in {@link ParamPojoTable}
     * which add columns to uiTable after super constructor's call.
     */
    private PojoTable(final Class pojoClass, final String title, final ObjectProperty<? extends Element> container, final AbstractSIRSRepository repo, final boolean applyPreferences) {
        if (pojoClass == null && repo == null) {
            throw new IllegalArgumentException("Pojo class to expose and Repository parameter are both null. At least one of them must be valid.");
        }

        createNewProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                uiAdd.setGraphic(new ImageView(SIRS.ICON_ADD_WHITE));
            } else {
                uiAdd.setGraphic(new ImageView(SIRS.ICON_CHAIN_WHITE));
            }
        });

        this.container = container;

        if (pojoClass == null) {
            this.pojoClass = repo.getModelClass();
        } else {
            this.pojoClass = pojoClass;
        }

        // Préférences utilisateur pour cette PojoTable.
        columnsPreferences = new TableColumnsPreferences(this.pojoClass);

        if (BUG_JAVAFX_COLUMN_MOVE) {
            SIRS.setColumnResize(uiTable, columnsPreferences);   //Entre en concurrence avec l'application des préférences utilisateurs.
        } else {
            SIRS.setColumnResize(uiTable); //initialement placé avant l'instanciation de pojoclass
        }

        setFocusTraversable(true);

        dataSupplierProperty.addListener(this::updateTableItems);

        this.labelMapper = LabelMapper.get(this.pojoClass);
        if (repo == null) {
            AbstractSIRSRepository tmpRepo;
            try {
                tmpRepo = session.getRepositoryForClass(pojoClass);
            } catch (IllegalArgumentException e) {
                SIRS.LOGGER.log(Level.FINE, e.getMessage());
                tmpRepo = null;
            }
            this.repo = tmpRepo;
        } else {
            this.repo = repo;
        }
        deletor = new Deletor(createNewProperty, parentElementProperty, ownerElementProperty, this.repo);

        searchRunning.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        searchRunning.setPrefSize(22, 22);
        searchRunning.setStyle("-fx-progress-color: white;");

        uiRefresh.managedProperty().bind(uiRefresh.visibleProperty());
        uiFicheMode.managedProperty().bind(uiFicheMode.visibleProperty());
        uiSearch.managedProperty().bind(uiSearch.visibleProperty());
        uiAdd.managedProperty().bind(uiAdd.visibleProperty());
        uiCopyTo.managedProperty().bind(uiCopyTo.visibleProperty());
        uiDelete.managedProperty().bind(uiDelete.visibleProperty());
        uiImport.managedProperty().bind(uiImport.visibleProperty());
        uiExport.managedProperty().bind(uiExport.visibleProperty());

        uiTable.setRowFactory(rowFactory);
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        /* We cannot bind visible properties of those columns, because TableView
         * will set their value when user will request to hide them.
         */
        editableProperty.addListener((
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            deleteColumn.setVisible(newValue);
            //editCol.setVisible(newValue && detaillableProperty.get());
        });
        cellEditableProperty.bind(editableProperty);

        detaillableProperty.addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> editCol.setVisible(newValue));

        uiTable.getColumns().add(deleteColumn);
        uiTable.getColumns().add((TableColumn) editCol);

        if (AvecGeometrie.class.isAssignableFrom(pojoClass)) {
            uiTable.getColumns().add(new ShowOnMapColumn(DEFAULT_VALUE_FACTORY, ICON_SHOWONMAP, DEFAULT_VISIBLE_PREDICATE));
        }

        try {
            //contruction des colonnes editable
            final Map<String, PropertyDescriptor> properties = SIRS.listSimpleProperties(this.pojoClass);

            // On enlève les propriétés inutiles pour l'utilisateur
            for (final String key : COLUMNS_TO_IGNORE) {
                properties.remove(key);
            }

            final List<String> colNames = new ArrayList<>(properties.keySet());
            final List<TableColumn> cols = new ArrayList<>();

            // On donne toutes les informations de position.
            if (Positionable.class.isAssignableFrom(this.pojoClass)) {
                final Set<String> positionableKeys = SIRS.listSimpleProperties(Positionable.class).keySet();
                positionableKeys.remove(SIRS.DESIGNATION_FIELD);
                final List<TableColumn> positionColumns = new ArrayList<>();
                for (final String key : positionableKeys) {
                    getPropertyColumn(properties.remove(key)).ifPresent(column -> {
                        cols.add(column);
                        positionColumns.add(column);
                    });
                }

                // On permet de cacher toutes les infos de position d'un coup.
                final ImageView viewOn = new ImageView(SIRS.ICON_COMPASS_WHITE);
                final ToggleButton uiPositionVisibility = new ToggleButton(null, viewOn);
                uiPositionVisibility.setSelected(true); // Prepare to be forced to change.
                uiPositionVisibility.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    if (newValue == null) {
                        return;
                    }
                    for (final TableColumn col : positionColumns) {
                        col.setVisible(newValue);
                    }
                    if (newValue) {
                        uiPositionVisibility.setTooltip(new Tooltip("Cacher les informations de position"));
                    } else {
                        uiPositionVisibility.setTooltip(new Tooltip("Afficher les informations de position"));
                    }
                });
                uiPositionVisibility.visibleProperty().bind(uiFicheMode.selectedProperty().not());
                uiPositionVisibility.managedProperty().bind(uiPositionVisibility.visibleProperty());
                uiPositionVisibility.getStyleClass().add(BUTTON_STYLE);
                uiPositionVisibility.setSelected(false);// Force to change.
                searchEditionToolbar.getChildren().add(uiPositionVisibility);
            }

            for (final PropertyDescriptor desc : properties.values()) {
                getPropertyColumn(desc).ifPresent(column -> cols.add(column));
            }

            //on trie les colonnes
            final List<String> order = ColumnOrder.sort(this.pojoClass.getSimpleName(), colNames);
            for (String colName : order) {
                final TableColumn column = getColumn(colName, cols);
                if (column != null) {
                    uiTable.getColumns().add(column);
                }
            }

        } catch (IntrospectionException ex) {
            SIRS.LOGGER.log(Level.WARNING, "property columns cannot be created.", ex);
        }

        uiTable.editableProperty().bind(editableProperty);

        /* barre d'outils. Si on a un accesseur sur la base, on affiche des
         * boutons de création / suppression.
         */
        uiSearch.textProperty().bind(currentSearch);
        uiSearch.getStyleClass().add(BUTTON_STYLE);
        uiSearch.setOnAction((ActionEvent event) -> searchText());
        uiSearch.getStyleClass().add("label-header");
        uiSearch.disableProperty().bind(searchableProperty.not());

        titleProperty.set(title == null ? labelMapper == null ? null : labelMapper.mapClassName() : title);
        final Label uiTitle = new Label();
        uiTitle.textProperty().bind(titleProperty);
        uiTitle.getStyleClass().add("pojotable-header");
        uiTitle.setAlignment(Pos.CENTER);

        searchEditionToolbar.getStyleClass().add("buttonbar");

        uiRefresh.getStyleClass().add(BUTTON_STYLE);
        uiRefresh.setOnAction((ActionEvent event) -> updateTableItems(dataSupplierProperty, null, dataSupplierProperty.get()));

        final EventHandler<ActionEvent> addHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                final Element p;
                if (createNewProperty.get()) {
                    p = createPojo();
                    if (p != null && openEditorOnNewProperty.get()) {
                        editPojo(p, SIRS.EDITION_PREDICATE);
                    }
                } else {
                    final ObservableList<Preview> choices;
                    final Element cont = PojoTable.this.container.get();
                    if (Objet.class.isAssignableFrom(pojoClass) && cont instanceof Objet) {

                        // récupération de tous les éléments du type compatible avec le tableau courant
                        final List<Preview> possiblePreviews = new ArrayList<>(session.getPreviews().getByClass(pojoClass));

                        // récupération des identifiants
                        final List<String> possibleIds = possiblePreviews.stream().map(e -> e.getElementId()).collect(Collectors.toList());

                        // recupération des objets correspondant aux identifiants
                        final List<Objet> entities = PojoTable.this.session.getRepositoryForClass(pojoClass).get(possibleIds);

                        // retrait des entités qui ne sont pas sur le même tronçon que le "conteneur"
                        entities.removeIf(new Predicate<Objet>() {
                            @Override
                            public boolean test(Objet t) {
                                return !((Objet) cont).getLinearId().equals(t.getLinearId());
                            }
                        });

                        // récupération des identifiants des éléments sur le même tronçon
                        final List<String> sameContainerIds = entities.stream().map(o -> o.getId()).collect(Collectors.toList());

                        // filtrage des previews correspondants
                        possiblePreviews.removeIf(new Predicate<Preview>() {
                            @Override
                            public boolean test(Preview t) {
                                return !sameContainerIds.contains(t.getElementId());
                            }
                        });

                        choices = SIRS.observableList(possiblePreviews);

                    } else {
                        choices = SIRS.observableList(session.getPreviews().getByClass(pojoClass)).sorted();
                    }

                    final PojoTableChoiceStage<Element> stage = new ChoiceStage(
                            PojoTable.this.repo, choices, null, "Choix de l'élément", "Ajouter");
                    stage.showAndWait();
                    p = stage.getRetrievedElement().get();
                    if (p != null) {
                        if (getAllValues().contains(p)) {
                            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Le lien que vous souhaitez ajouter est déjà présent dans la table.");
                            alert.setResizable(true);
                            alert.showAndWait();
                        } else {
                            getAllValues().add(p);
                        }
                    } else {
                        final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune entrée ne peut être créée.");
                        alert.setResizable(true);
                        alert.showAndWait();
                    }
                }
            }
        };

        uiAdd.getStyleClass().add(BUTTON_STYLE);
        uiAdd.setOnAction(addHandler);
        uiAdd.disableProperty().bind(editableProperty.not());

        elementCopier = new ElementCopier(pojoClass, container, session, this.repo);

        // Copie des éléments sélectionnés.
        uiCopyTo.getStyleClass().add(BUTTON_STYLE);
        uiCopyTo.setOnAction((ActionEvent event) -> {
            final Element[] elements = ((List<Element>) uiTable.getSelectionModel().getSelectedItems()).toArray(new Element[0]);
            if (elements.length > 0) {
                try {

                    //Choix du destinataire de la copie.
                    final Element target = elementCopier.askForCopyTarget();

                    //Copie des éléments sélectionnés vers la cible identifiée.
                    this.elementCopier.copyPojosTo(target, elements);

                    if (elementCopier.getAvecForeignParent() || elementCopier.getRapportEtude()) {
                        //On rafraîchie les éléments du tableau.
                        updateTableItems(dataSupplierProperty, null, dataSupplierProperty.get());
                    }

                    final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Eléments copiés avec succès");
                    alert.setResizable(true);
                    alert.showAndWait();

                } catch (CopyElementException e) {
                    e.openAlertWindow();
                    SIRS.LOGGER.log(Level.WARNING, "CopyElementException lors de la copie d'éléments !", e);
                } catch (Exception e) {
                    SIRS.LOGGER.log(Level.WARNING, "Exception inattendue lors de la copie d'éléments !", e);
                }

            } else {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Copie impossible, aucun élément sélectionné.");
                alert.setResizable(true);
                alert.showAndWait();
            }
        });
        uiCopyTo.disableProperty().bind(editableProperty.not());

        uiDelete.getStyleClass().add(BUTTON_STYLE);
        uiDelete.setOnAction((ActionEvent event) -> {
            final Element[] elements = ((List<Element>) uiTable.getSelectionModel().getSelectedItems()).toArray(new Element[0]);
            if (elements.length > 0) {
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression ?", ButtonType.NO, ButtonType.YES);
                alert.setResizable(true);
                final Optional<ButtonType> res = alert.showAndWait();
                if (res.isPresent() && ButtonType.YES.equals(res.get())) {
                    deletePojos(elements);
                    if (uiFicheMode.isSelected()) {
                        updateFiche();
                    }
                }
            } else {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune entrée sélectionnée. Pas de suppression possible.");
                alert.setResizable(true);
                alert.showAndWait();
            }
        });
        uiDelete.disableProperty().bind(editableProperty.not());

        uiTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        uiTable.setMaxWidth(Double.MAX_VALUE);
        uiTable.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        uiTable.setPlaceholder(new Label(""));
        uiTable.setTableMenuButtonVisible(true);
        // Load all elements only if the user gave us the repository.
        if (repo != null) {
            setTableItems(() -> SIRS.observableList(this.repo.getAll()));
        }

        if (AvecPhotos.class.isAssignableFrom(this.pojoClass) || AbstractPhoto.class.isAssignableFrom(this.pojoClass) || Desordre.class.isAssignableFrom(this.pojoClass)) {
            commentPhotoView = new FXCommentPhotoView();
            commentPhotoView.valueProperty().bind(uiTable.getSelectionModel().selectedItemProperty());
            commentPhotoView.visibleProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    updateView();
                }
            });
        } else {
            commentPhotoView = null;
        }

        //
        // NAVIGATION FICHE PAR FICHE
        //
        navigationToolbar.getStyleClass().add("buttonbarleft");

        uiCurrent.setFont(Font.font(16));
        uiCurrent.getStyleClass().add(BUTTON_STYLE);
        uiCurrent.setAlignment(Pos.CENTER);
        uiCurrent.setTextFill(Color.WHITE);
        uiCurrent.setOnAction(this::goTo);

        uiPrevious.getStyleClass().add(BUTTON_STYLE);
        uiPrevious.setOnAction((ActionEvent event) -> {
            uiTable.getSelectionModel().selectPrevious();
            updateFiche();
        });

        uiNext.getStyleClass().add(BUTTON_STYLE);
        uiNext.setOnAction((ActionEvent event) -> {
            final TableView.TableViewSelectionModel<Element> sModel = uiTable.getSelectionModel();
            if (sModel.getSelectedItem() == null) {
                sModel.selectFirst();
            } else {
                sModel.selectNext();
            }
            updateFiche();
        });
        navigationToolbar.visibleProperty().bind(uiFicheMode.selectedProperty());

        uiFicheMode.getStyleClass().add(BUTTON_STYLE);

        // Update counter when we change selected element.
        final ChangeListener<Number> selectedIndexListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            uiCurrent.setText("" + (newValue.intValue() + 1) + " / " + uiTable.getItems().size());
        };
        uiFicheMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    uiTable.getSelectionModel().selectedIndexProperty().addListener(selectedIndexListener);
                    uiTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                } else {
                    uiTable.getSelectionModel().selectedIndexProperty().removeListener(selectedIndexListener);
                    uiTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                }
                updateView();
            }
        });
        uiFicheMode.disableProperty().bind(fichableProperty.not());

        uiImport.getStyleClass().add(BUTTON_STYLE);
        uiImport.disableProperty().bind(editableProperty.not());
        uiImport.visibleProperty().bind(importPointProperty);
        uiImport.managedProperty().bind(importPointProperty);
        uiImport.setOnAction(new ImportAction(pojoClass, this));

        if (ProfilTravers.class.isAssignableFrom(pojoClass)) {
            importPointProperty.set(true);
        }

        uiExport.getStyleClass().add(BUTTON_STYLE);
        uiExport.disableProperty().bind(Bindings.isNull(uiTable.getSelectionModel().selectedItemProperty()));
        uiExport.setOnAction(new ExportAction(getStructBeanSupplier(), getColumns()));

        if (PointZ.class.isAssignableFrom(pojoClass)) {
            uiTable.getColumns().add(new DistanceComputedPropertyColumn(DOUBLE_CELL_FACTORY, parentElementProperty, uiTable));
        }

        final HBox titleBoxing = new HBox(uiTitle);
        titleBoxing.setAlignment(Pos.CENTER);
        final VBox titleAndFilterBox = new VBox(titleBoxing);

        uiFilter.getStyleClass().add(BUTTON_STYLE);
        uiFilter.managedProperty().bind(uiFilter.visibleProperty());
        uiFilter.disableProperty().bind(uiFicheMode.selectedProperty());
        resetFilterBtn.getStyleClass().addAll("label-header", "buttonbar-button", "white-rounded");
        applyFilterBtn.getStyleClass().addAll("label-header", "buttonbar-button", "white-rounded");
        final Separator separator = new Separator(Orientation.VERTICAL);
        separator.setMaxWidth(Double.MAX_VALUE);
        separator.setVisible(false);
        final HBox confirmationBox = new HBox(5, separator, resetFilterBtn, applyFilterBtn);
        HBox.setHgrow(separator, Priority.ALWAYS);
        filterContent = new VBox(10);
        filterContent.getStyleClass().add("filter-root");
        filterContent.visibleProperty().bind(uiFilter.selectedProperty().and(uiFilter.visibleProperty()).and(uiFilter.disableProperty().not()));
        filterContent.managedProperty().bind(filterContent.visibleProperty());

        try {
            initFilterBuilder();
            filterContent.getChildren().addAll(uiFilterBuilder, confirmationBox);

            applyFilterBtn.managedProperty().bind(filterContent.visibleProperty());
            resetFilterBtn.managedProperty().bind(filterContent.visibleProperty());
            uiFilterBuilder.managedProperty().bind(filterContent.visibleProperty());

            resetFilterBtn.setOnAction(event -> resetFilter(filterContent));
            applyFilterBtn.setOnAction(event -> updateTableItems(dataSupplierProperty, null, dataSupplierProperty.get()));
        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Filter panel cannot be initialized !", e);
        }

        titleAndFilterBox.setFillWidth(true);
        topPane = new BorderPane(notifier, titleAndFilterBox, searchEditionToolbar, filterContent, navigationToolbar);
        setTop(topPane);

        /*
         * TOOLTIPS
         */
        uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches"));
        uiSearch.setTooltip(new Tooltip("Rechercher un terme dans la table"));
        uiImport.setTooltip(new Tooltip("Importer des points"));
        uiExport.setTooltip(new Tooltip("Sauvegarder en CSV"));
        uiPrevious.setTooltip(new Tooltip("Fiche précédente"));
        uiNext.setTooltip(new Tooltip("Fiche suivante"));
        uiRefresh.setTooltip(new Tooltip("Recharger la table"));
        uiAdd.setTooltip(new Tooltip(createNewProperty.get() ? "Créer un nouvel élément" : "Ajouter un élément existant"));
        createNewProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                uiAdd.setTooltip(new Tooltip("Créer un nouvel élément"));
            } else {
                uiAdd.setTooltip(new Tooltip("Ajouter un élément existant"));
            }
        });
        uiCopyTo.setTooltip(new Tooltip("Copier vers..."));
        uiCurrent.setTooltip(new Tooltip("Aller au numéro..."));
        uiDelete.setTooltip(new Tooltip("Supprimer les éléments sélectionnés"));
        uiFilter.setTooltip(new Tooltip("Filtrer les données"));
        if (ProfilTravers.class.isAssignableFrom(pojoClass)) {
            uiImport.setTooltip(new Tooltip("Importer des paramètres hydrauliques"));
        }

        if (BUG_JAVAFX_COLUMN_MOVE) {
            //Place toutes les colonnes non visible en fin de tableau (excepté le bouton de suppression)
            // afin de permettre un déplacement (manuel) de colonne intelligible.
            // En effet, la version de javaFX utilisée (lors de la version 2.16 de
            // l'appli) gère mal le déplacement de colonnes lorsque des colonnes ne
            // sont pas visibles.
            //    -> voir aussi col.visibleProperty().addListener ~l.880
            TableColumn<Element, ?> deplacedCol = null;
            for (int i = uiTable.getColumns().size() - 1; i > 0; i--) {//On ne veut pas déplacer la première colonne (suppression)
                if ((uiTable.getColumns().get(i).visibleProperty().get() == false)) {
                    deplacedCol = uiTable.getColumns().get(i);
                    uiTable.getColumns().remove(i);
                    uiTable.getColumns().add(deplacedCol);
                }
            }
        }


        updateView();

        //Application des préférence utilisateur (position, visibilité et largeur
        // de colonne. Le booléan applyPreferences permet de ne pas appliqué ces
        // préférence depuis ce contructeur.
        if (applyPreferences) {
            applyPreferences();
            listenPreferences();
        }

        //===============================================================
        // Suivi des préférences utilisateur pour les colonnes affichées.
        //===============================================================


    }// FIN Constructeur.
    //==========================================================================

    //==========================================================================

    /**
     * Méthode permettant de lancer une sauvegarde de préférences utilisateur
     * lorsqu'un changement de la tableView {@code  uiTable} a été détécté.
     *
     * Un délai de 4 seconde est mis en place pour n'effectuer qu'une seule
     * sauvegarde lors de plusieurs changements ce succédant.
     */
    private void modifiedColumn() {
        //Changement de position de colonne.
        if (!isColumnModifying.getValue()) {
            isColumnModifying.set(true);
            scheduledExecutorService.schedule(saveColumnsPreferences, 4, TimeUnit.SECONDS);
        }
    }

    /**
     * Runnable permettant d'exécuter la sauvegarde des préférences utilisateur
     * {@link modifiedColumn}.
     */
    private Runnable saveColumnsPreferences = new Runnable() {
        @Override
        public void run() {

            isColumnModifying.set(false);
            SIRS.LOGGER.log(Level.INFO, "Sauvegarde des préférences de la pojoTable {0} pour les colonnes :\n", modifiedColumnsIndices);

            //Pour chaque colonne modifiée, on met à jours les préférences.
            modifiedColumnsIndices.forEach(colIndice -> {

                TableColumn<Element, ?> column = uiTable.getColumns().get(colIndice);
                try {
                    //TODO empêcher la castException -> S'il y a problème de cast on met un nom vide.
                    // ColumnState newState = new ColumnState(((PropertyColumn) column).getName(), column.isVisible(), colIndice, (float) column.widthProperty().get());
                    ColumnState newState = new ColumnState(TableColumnsPreferences.getColumnRef(column), column.isVisible(), colIndice, (float) column.widthProperty().get());
                    columnsPreferences.addColumnPreference(newState);
                } catch (Exception e) {
                    SIRS.LOGGER.log(Level.WARNING, "Modification de la colonne {0} non enregistr\u00e9e dans les pr\u00e9f\u00e9rences.", colIndice);
                }
            });

            //Nettoyage de l'indicateur des colonnes modifiées.
            modifiedColumnsIndices.clear();

            //Sauvegarde des préférences au format Json.
            columnsPreferences.saveInJson();

            SIRS.LOGGER.log(Level.INFO, "Fin Sauvegarde de préférences PojoTable.");

        }
    };

    /**
     * Set listeners for visibility, width and position changes of the uiTable's
     * columns.
     *
     * TODO : should call 3 methods : one by listener.
     *
     */
    final protected void listenPreferences() {
        //Ajout Listener pour identifier et sauvegarder les modifications de colonnes par l'utilisateur :
        //Identification des changements d'épaisseur.
        uiTable.getColumns().forEach(col -> col.widthProperty().addListener((ov, t, t1) -> {

            modifiedColumnsIndices.add(this.uiTable.getColumns().indexOf(col));
            modifiedColumn();
        }));

        //Identification des changements de visibilité des colonnes.
        uiTable.getColumns().forEach(col -> col.visibleProperty().addListener((ov, oldVisibility, newVisibility) -> {
            if ((newVisibility == null)) {
                SIRS.LOGGER.log(Level.INFO, "Une visibilité nulle est attribuée à une colonne. Ce cas n'est pas géré par la sauvegarde des préférences de la classe PojoTable.");
                return;
            }

            int colIndex = this.uiTable.getColumns().indexOf(col);

            if (BUG_JAVAFX_COLUMN_MOVE) {
                //Pour permettre un déplacement instinctif des colonnes par l'utilisateur
                // - Les colonnes non visibles sont déplacées en fin de tableau
                // - Les colonnes visibles sont déplacées à l'index 3 de la table
                //  ( l'index 3 permet de placer les colonnes rendues visibles en
                //  début de tableau)
                // - ce traitement n'es pas appliqué au colonnes sans id
                // (suppression, accès aux fixhes et à la carto).
                if (col.getId() != null) {
                    if ((newVisibility.equals(Boolean.FALSE))) {
                        TableColumn<Element, ?> deplacedColumn = col;
                        uiTable.getColumns().remove(col);
                        uiTable.getColumns().add(deplacedColumn);
                        //On sauvegarde toutes les colonnes de l'indice identifié jusqu'à la fin de la table où la colonne sera réintroduite.
                        for (int j = colIndex; j < uiTable.getColumns().size(); j++) {
                            modifiedColumnsIndices.add(j);
                            modifiedColumn();
                        }
                        deplacedColumn = null;
                    } else if ((newVisibility.equals(Boolean.TRUE))) {
                        TableColumn<Element, ?> deplacedColumn = col;
                        uiTable.getColumns().remove(col);
                        uiTable.getColumns().add(3, deplacedColumn);
                        deplacedColumn = null;
                        //On sauvegarde toutes les colonnes de l'indice identifié jusqu'à la 4e colonne (3+1) de la table où la colonne sera réintroduite.
                        for (int j = 3; j <= colIndex; j++) {
                            modifiedColumnsIndices.add(j);
                            modifiedColumn();
                        }
                    }
                }
            } else {
                modifiedColumnsIndices.add(colIndex);
            }

        }));

        // Suivi des changement de position des colonnes.
        uiTable.getColumns().addListener((Change<? extends TableColumn<Element, ?>> change) -> {
            if (change.next()) {

                List<TableColumn<Element, ?>> listInit = (List<TableColumn<Element, ?>>) change.getRemoved();
                int nbreCol = change.getTo();
                // Si la table avant changement n'était pas définie ou disposer de
                // moins de colonnes que la nouvelle table, ce n'est pas un changement
                // provoqué par l'utilisateur.
                if (listInit == null || listInit.size() < nbreCol) {
                    SIRS.LOGGER.log(Level.INFO, "Une colonne a été supprimée.\n S'il ne s'agit pas d'une suppression due à un changement de visibilité, ce changement n'est pas sauvegardé.");

                } else {

//                ObservableListWrapper
                    List<TableColumn<Element, ?>> listEnd = (List<TableColumn<Element, ?>>) change.getList();

                    // Lors d'un changement parmi les colonnes de uiTable,
                    // on compare les Id des colonnes avant et après le changement
                    // pour identifier les changements de position.
                    for (int i = 0; i < nbreCol; i++) {
                        TableColumn<Element, ?> colInit = listInit.get(i);
                        String colInitId = colInit.getId();
                        //Comparaison avec == ou != car on compare les instances.
                        if (listEnd.get(i).getId() != colInit.getId()) {

                            if (BUG_JAVAFX_COLUMN_MOVE) {
                                //Si la colonne est déplacée en fin de table, on la replace
                                //avant les colonnes non-visibles afin d'éviter
                                // le bug JavaFX
                                try {
                                    int maxVisible = nbreCol - 1;
                                    if (colInitId == listEnd.get(maxVisible).getId()) {
                                        ;
                                        for (int j = maxVisible - 1; j > 0; j--) {
                                            if (listEnd.get(j).isVisible()) {
                                                break;
                                            }
                                            maxVisible = j;
                                        }
                                        listEnd.remove(colInit);
                                        listEnd.add(maxVisible, colInit);
                                    }
                                } catch (RuntimeException re) {
                                    //Do nothing
                                }
                            }
                            modifiedColumnsIndices.add(i);
                        }
                    }
                    modifiedColumn();
                }
            }
        });
    }

    final protected void applyPreferences() {
        if (!columnsPreferences.getWithPreferencesColumns().isEmpty()) {
            columnsPreferences.applyPreferencesToTableColumns(uiTable.getColumns());
        }
    }

    //==========================================================================
    protected StructBeanSupplier getStructBeanSupplier() {
        return new StructBeanSupplier(pojoClass, () -> new ArrayList(uiTable.getSelectionModel().getSelectedItems()));
    }

    public StringProperty titleProperty() {
        return titleProperty;
    }

    protected Callback<TableColumn.CellDataFeatures, ObservableValue> getDefaultValueFactory() { return DEFAULT_VALUE_FACTORY;}
    protected Predicate getDefaultVisiblePredicate() { return DEFAULT_VISIBLE_PREDICATE;}

    protected final ObservableList<TableColumn<Element, ?>> getColumns() {
        return uiTable.getColumns();
    }

    public final ObservableList<Element> getSelectedItems() {
        return uiTable.getSelectionModel().getSelectedItems();
    }

    public final ReadOnlyObjectProperty<? extends Element> selectedItemProperty() {
        return getTable().getSelectionModel().selectedItemProperty();
    }

    public final TableView<Element> getTable() {
        return uiTable;
    }

    /**
     * Mise à jour de l'interface en mode "fiche".
     *
     * SYM-1764 : On crée un nouveau panneau à chaque fois qu'on change de
     * fiche. Sinon le rechargement des positions provoque des anomalies de
     * recalcul des positions.
     */
    private void updateFiche() {
        if (uiTable.getSelectionModel().getSelectedIndex() < 0) {
            uiTable.getSelectionModel().select(0);
        }

        setCenter(SIRS.generateEditionPane(uiTable.getSelectionModel().getSelectedItem(), SIRS.EDITION_PREDICATE));

        uiCurrent.setText("" + (uiTable.getSelectionModel().getSelectedIndex() + 1) + " / " + uiTable.getItems().size());
    }

    private void updateView() {

        if (uiFicheMode.isSelected()) {
            uiFicheMode.setTooltip(new Tooltip("Passer en mode de tableau synoptique."));
            updateFiche();
        } else {
            uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches."));

            if (commentPhotoView != null) {
                final SplitPane sPane = new SplitPane();
                sPane.setOrientation(Orientation.VERTICAL);
                sPane.getItems().addAll(uiTable, commentPhotoView);
                sPane.setDividerPositions(0.9);
                setCenter(sPane);
            } else {
                setCenter(uiTable);
            }
        }
    }

    /**
     * Définit l'élément en paramètre comme parent de tout élément créé via
     * cette table.
     *
     * Note : Ineffectif dans le cas où les éléments de la PojoTable sont créés
     * et listés directement depuis un repository couchDB, ou que l'élément créé
     * est déjà un CouchDB document.
     *
     * @param parentElement L'élément qui doit devenir le parent de tout objet
     * créé via la PojoTable.
     */
    public void setParentElement(final Element parentElement) {
        parentElementProperty.set(parentElement);
    }

    /**
     *
     * @return L'élément à affecter en tant que parent de tout élément créé via
     * cette table. Peut être nul.
     */
    public Element getParentElement() {
        return parentElementProperty.get();
    }

    /**
     *
     * @return La propriété contenant l'élément à affecter en tant que parent de
     * tout élément créé via cette table. Jamais nulle, mais peut-être vide.
     */
    public ObjectProperty<Element> parentElementProperty() {
        return parentElementProperty;
    }

    /**
     * Définit l'élément en paramètre comme principal référent de tout élément
     * créé via cette table.
     *
     * @param parentElement L'élément qui doit devenir le principal référent de
     * tout objet créé via la PojoTable.
     */
    public void setOwnerElement(final Element parentElement) {
        ownerElementProperty.set(parentElement);
    }

    /**
     *
     * @return L'élément principal référent de tout élément créé via cette
     * table. Peut être nul.
     */
    public Element getOwnerElement() {
        return ownerElementProperty.get();
    }

    /**
     *
     * @return La propriété contenant l'élément à affecter en tant que principal
     * référent de tout élément créé via cette table. Jamais nulle, mais
     * peut-être vide.
     */
    public ObjectProperty<Element> ownerElementProperty() {
        return ownerElementProperty;
    }

    public synchronized ObservableList<Element> getAllValues() {
        return allValues;
    }

    public BooleanProperty editableProperty() {
        return editableProperty;
    }

    public BooleanProperty cellEditableProperty() {
        return cellEditableProperty;
    }

    public BooleanProperty detaillableProperty() {
        return detaillableProperty;
    }

    public BooleanProperty fichableProperty() {
        return fichableProperty;
    }

    public BooleanProperty searchableProperty() {
        return searchableProperty;
    }

    public BooleanProperty openEditorOnNewProperty() {
        return openEditorOnNewProperty;
    }

    public BooleanProperty createNewProperty() {
        return createNewProperty;
    }

    public BooleanProperty importPointProperty() {
        return importPointProperty;
    }

    public BooleanProperty commentAndPhotoProperty() {
        if (commentPhotoView == null) {
            return new ReadOnlyBooleanWrapper(false);
        }
        return commentPhotoView.visibleProperty();
    }

    public BooleanProperty exportVisibleProperty() {
        return uiExport.visibleProperty();
    }

    public BooleanProperty searchVisibleProperty() {
        return uiSearch.visibleProperty();
    }

    public BooleanProperty ficheModeVisibleProperty() {
        return uiFicheMode.visibleProperty();
    }

    public BooleanProperty filterVisibleProperty() {
        return uiFilter.visibleProperty();
    }

    public ObjectProperty<Supplier<ObservableList<Element>>> dataSupplierProperty() {
        return dataSupplierProperty;
    }

    public VBox getFilterUI() {
        return filterContent;
    }

    protected void initFilterBuilder() throws IntrospectionException {
        if (uiFilterBuilder == null) {
            uiFilterBuilder = new FXFilterBuilder() {
                @Override
                protected String getTitle(PropertyType candidate) {
                    if (candidate == null || candidate.getName() == null || candidate.getName().head() == null) {
                        return "";
                    }

                    if (new SEClassementEqualsOperator().canHandle(candidate)) {
                        return "classe";
                    }

                    final String pName = candidate.getName().head().toString();
                    if (labelMapper != null) {
                        final String pTitle = labelMapper.mapPropertyName(pName);
                        if (pTitle != null) {
                            return pTitle;
                        }
                    }
                    return pName;
                }
            };
        }

        // If no property has been given for filtering, we analyze model class to find them.
        final ObservableList<PropertyType> props = uiFilterBuilder.getAvailableProperties();
        if (props.isEmpty()) {
            final BeanInfo info = Introspector.getBeanInfo(pojoClass);
            for (final PropertyDescriptor desc : info.getPropertyDescriptors()) {
                final Method readMethod = desc.getReadMethod();
                if (readMethod != null) {

                    // Do not filter on java standard property like getClass(), etc.
                    if (readMethod.getAnnotation(Internal.class) != null
                            || readMethod.getDeclaringClass().equals(Object.class)) {
                        continue;
                    }

                    final HashMap identification = new HashMap(1);
                    identification.put(AbstractIdentifiedType.NAME_KEY, desc.getName());

                    // If we've got a reference to another document, property is declared as an association.
                    final Reference annot = readMethod.getAnnotation(Reference.class);
                    if (annot != null) {
                        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
                        builder.setName(desc.getName());
                        builder.add(FXReferenceEqualsOperator.CLASS_ATTRIBUTE, annot.ref());
                        props.add(new DefaultAssociationRole(
                                identification, builder.buildFeatureType(), 0, 1));
                        if (SystemeEndiguement.class.equals(annot.ref())) {
                            builder.reset();
                            builder.setName("classement");
                            builder.add(SEClassementEqualsOperator.CLASSEMENT_ATTRIBUTE, String.class);
                            final HashMap tmpIdent = new HashMap(1);
                            tmpIdent.put(AbstractIdentifiedType.NAME_KEY, "classement");
                            props.add(new DefaultAssociationRole(identification, builder.buildFeatureType(), 0, 1));
                        }
                    } else {
                        props.add(new DefaultAttributeType(
                                identification, desc.getPropertyType(), 0, 1, null, null));
                    }
                }
            }
        }
    }

    /**
     * Called when user click on the search icon. Prepare the popup with the
     * textfield to type research into.
     */
    protected void searchText() {
        if (uiSearch.getGraphic() != searchNone) {
            //une recherche est deja en cours
            return;
        }

        final Popup popup = new Popup();
        final TextField textField = new TextField(currentSearch.get());
        popup.getContent().add(textField);
        popup.setAutoHide(true);
        textField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                currentSearch.set(textField.getText());
                popup.hide();
                updateTableItems(dataSupplierProperty, null, dataSupplierProperty.get());
            }
        });
        final Point2D sc = uiSearch.localToScreen(0, 0);
        popup.show(uiSearch, sc.getX(), sc.getY());
    }

    protected void goTo(ActionEvent event) {
        final int tableSize = uiTable.getItems().size();
        final int currentIndex = uiTable.getSelectionModel().getSelectedIndex() + 1;
        final Popup popup = new Popup();
        final Spinner<Integer> numberField = new Spinner<>();

        numberField.setEditable(true);
        numberField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, tableSize, currentIndex));
        popup.getContent().add(numberField);
        popup.setAutoHide(true);
        numberField.setOnKeyReleased((k) -> {
            if (k.getCode() == KeyCode.ENTER) {
                final int index = numberField.getValue() - 1;
                if (index >= 0 && index < tableSize) {
                    TableView.TableViewSelectionModel<Element> selectionModel = uiTable.getSelectionModel();
                    selectionModel.select(index);
                }
                popup.hide();
                updateFiche();
            }
        });
        final Point2D sc = uiCurrent.localToScreen(0, 0);
        popup.show(uiSearch, sc.getX(), sc.getY());
    }

    /**
     * @return {@link TableView} used for element display.
     * Deprecated - to be removed but first fix {@link FXSystemeReperagePane} method updateFields (I could not solve it)
     * Duplicate with method getTable()
     */
    @Deprecated
    public TableView getUiTable() {
        return uiTable;
    }

    /**
     * Reset the filter pane at its initial state.
     *
     * @param filterContent
     */
    public void resetFilter(final VBox filterContent) {
        final ObservableList<Node> vBoxChildren = filterContent.getChildren();
        final int indexOfBuilder = vBoxChildren.indexOf(uiFilterBuilder);
        if (vBoxChildren.size() > indexOfBuilder) {
            vBoxChildren.remove(indexOfBuilder);
            uiFilterBuilder = null;
            try {
                initFilterBuilder();
                vBoxChildren.add(indexOfBuilder, uiFilterBuilder);
            } catch (Exception e) {
                GeotkFX.newExceptionDialog("Une erreur inattendue est survenue.", e).show();
            }
            applyFilterBtn.fire();
        }
    }

    /**
     * Get the filter which will be applied on the pojo table values. Subclasses
     * can override this method to extend the filter used.
     *
     * @return The filter to use, or {@code null} if none.
     */
    public Filter getFilter() {
        // Apply filter on properties
        Filter tmpFilter = null;
        if (uiFilterBuilder != null) {
            try {
                tmpFilter = uiFilterBuilder.getFilter();
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.FINE, "No filter can be built for pojo table on " + pojoClass, e);
            }
        }
        return tmpFilter;
    }

    /**
     * Start an asynchronous task which will update table content with the
     * elements provided by input supplier.
     *
     * @param producer Data provider.
     */
    public void setTableItems(Supplier<ObservableList<Element>> producer) {
        dataSupplierProperty.set(producer);
    }

    protected final void updateTableItems(
            final ObservableValue<? extends Supplier<ObservableList<Element>>> obs,
            final Supplier<ObservableList<Element>> oldSupplier,
            final Supplier<ObservableList<Element>> newSupplier) {

        if (tableUpdaterProperty.get() != null && !tableUpdaterProperty.get().isDone()) {
            tableUpdaterProperty.get().cancel();
        }

        final Task updater = new TaskManager.MockTask("Recherche...", (Runnable) () -> {
            synchronized (PojoTable.this) {
                try {
                    if (newSupplier == null) {
                        allValues = FXCollections.observableArrayList();
                    } else {
                        allValues = newSupplier.get();
                    }
                } catch (Throwable ex) {
                    allValues = FXCollections.observableArrayList();
                    filteredValues = allValues.filtered((Element t) -> true);
                    decoratedValues = new SortedList<>(filteredValues);
                    decoratedValues.comparatorProperty().bind(uiTable.comparatorProperty());
                    throw ex;
                }
                if (allValues == null) {
                    allValues = FXCollections.observableArrayList();
                }
            }
            if (allValues.isEmpty()) {
                Platform.runLater(() -> uiSearch.setGraphic(searchNone));
            }

            // Apply filter on properties
            final Filter firstFilter = getFilter();

            final Thread currentThread = Thread.currentThread();
            if (currentThread.isInterrupted()) {
                return;
            }

            // Apply "Plain text" filter
            final String searched = currentSearch.get();
            if ((searched == null || searched.isEmpty()) && firstFilter == null) {
                filteredValues = allValues.filtered((Element t) -> true);
            } else {
                final Set<String> result = new HashSet<>();
                // Remove wildcard analyze as it returned too much result without obvious link with the searched text.
                // Handle case in getSearchTextInCellsPredicate method
                SearchResponse search = Injector.getElasticSearchEngine().search(QueryBuilders.simpleQueryStringQuery("*" + searched + "*").analyzeWildcard(false).lenient(true));
                Iterator<SearchHit> iterator = search.getHits().iterator();
                while (iterator.hasNext() && !currentThread.isInterrupted()) {
                    result.add(iterator.next().getId());
                }

                if (currentThread.isInterrupted()) {
                    return;
                }

                final Predicate<Element> filterPredicate;
                if (firstFilter == null) {
                    final SirsStringConverter converter = new SirsStringConverter();
                    filterPredicate = element -> element == null || result.contains(element.getId())
                            || getSearchTextInCellsPredicate(searched, converter).test(element);
                } else if (searched == null || searched.isEmpty()) {
                    filterPredicate = element -> element == null || firstFilter.evaluate(element);
                } else {
                    filterPredicate = element -> element == null || result.contains(element.getId()) && firstFilter.evaluate(element);
                }
                filteredValues = allValues.filtered(filterPredicate);
            }

            //list contenant zero ou un element null en fonction du contenue de la liste filtrée
            //NOTE : bug javafx ici, la premiere ligne n'est plus editable a cause de ca
            // probleme avec la selection/focus qui cause trop d'événement
            //            final ObservableList<Element> emptyRecord = FXCollections.observableArrayList();
            //            filteredValues.addListener(new ListChangeListener<Element>() {
            //                @Override
            //                public void onChanged(ListChangeListener.Change<? extends Element> c) {
            //                    if(filteredValues.isEmpty()){
            //                        if(emptyRecord.isEmpty()) emptyRecord.add(null);
            //                    }else{
            //                        emptyRecord.clear();
            //                    }
            //                }
            //            });
            //            if(filteredValues.isEmpty()) emptyRecord.add(null);
            //            decoratedValues = SIRS.view(filteredValues,emptyRecord);
            decoratedValues = new SortedList<>(filteredValues);
            decoratedValues.comparatorProperty().bind(uiTable.comparatorProperty());
        });

        updater.stateProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            if (Worker.State.SUCCEEDED.equals(newValue)) {
                Platform.runLater(() -> {
                    uiTable.setItems(decoratedValues);
                    uiSearch.setGraphic(searchNone);
                });
            } else if (Worker.State.FAILED.equals(newValue) || Worker.State.CANCELLED.equals(newValue)) {
                final Throwable ex = updater.getException();
                if (ex != null) {
                    SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
                Platform.runLater(() -> uiSearch.setGraphic(searchNone));
            } else if (Worker.State.RUNNING.equals(newValue)) {
                Platform.runLater(() -> uiSearch.setGraphic(searchRunning));
            }
        });

        tableUpdaterProperty.set(TaskManager.INSTANCE.submit("Recherche...", updater));
    }

    /**
     * @param searched : searched text
     * @param converter : converter to convert {@link PropertyColumn#getCellData(Object)} )} in String;
     * @return a {@link Predicate<Element>} testing if the cells of {@link #getUiTable()} associated with the tested element
     *        refer to the searched text.
     */
    private Predicate<Element> getSearchTextInCellsPredicate(final String searched, final StringConverter converter) {
        return element -> uiTable.getColumns().stream()//Search in tableview's cells
                .filter(col -> col instanceof PropertyColumn)
                .map(col -> {
                    final Object cellData = col.getCellData(element);
                    if (cellData != null) {
                        if (((PropertyColumn) col).getReference() == null) {
                            final String res = converter.toString(cellData);
                            return ((res == null) || res.isEmpty()) ? cellData.toString() : res; //try to handle not Sirs Object like LocalDate  todo include it in SirsStringConverter?
                        } else if (cellData instanceof String) {
                            return toDisplayedText((String) cellData);
                        }
                    }
                    return null;
                })
                .filter(s -> s != null && !s.isEmpty())
                .anyMatch(str -> str.toLowerCase().contains(searched.toLowerCase()));
    }

    private String toDisplayedText(final String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            final Preview tmpPreview = Injector.getSession().getPreviews().get(value);
            if (tmpPreview != null) {
                return new SirsStringConverter().toString(tmpPreview);
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Failed to convert input value to cell text; return input value;", e);
        }
        return value;
    }


    /**
     * Delete the elements given in parameter. They are suppressed from the
     * table list, and if a {@link Repository} exists for the current table,
     * elements are also suppressed from database. If a parent element has been
     * set using {@linkplain #setParentElement(fr.sirs.core.model.Element) }
     * method, we will try to remove them from the parent as well.
     *
     * @param pojos The {@link Element}s to delete.
     */
    protected void deletePojos(Element... pojos) {
        final ObservableList<Element> items = getAllValues();
        boolean unauthorized = false;
        for (Element pojo : pojos) {
            // Si l'utilisateur est un externe, il faut qu'il soit l'auteur de
            // l'élément et que celui-ci soit invalide, sinon, on court-circuite
            // la suppression.
            if (session.editionAuthorized(pojo)) {
                deletor.accept(pojo);
                items.remove(pojo);
            } else {
                unauthorized = true;
            }
        }

        if (unauthorized) {
            new Growl(Growl.Type.WARNING, "Certains éléments n'ont pas été supprimés car vous n'avez pas les droits nécessaires.").showAndFade();
        }
    }

    // Change the default deletor
    public void setDeletor(final Consumer deletor) {
        this.deletor = deletor;
    }

    /**
     * Same as {@link PojoTable#editPojo(Object, Predicate)} with {@link SIRS#CONSULTATION_PREDICATE}.
     */
    protected Object editPojo(Object pojo) {
        return editPojo(pojo, SIRS.CONSULTATION_PREDICATE);
    }

    /**
     *
     * Try to find and display a form to edit input object.
     *
     * @param pojo The object we want to edit.
     */
    protected Object editPojo(Object pojo, Predicate<Element> editionPredicate) {
        final int index;
        if (uiFicheMode.isSelected() && (index = uiTable.getItems().indexOf(pojo)) >= 0) {
            uiTable.getSelectionModel().select(index);
            updateFiche();
        } else {
            editElement(pojo, editionPredicate);
        }
        return pojo;
    }

    /**
     * A method called when an element displayed in the table has been modified
     * in the table.
     *
     * @param event The table event refering to the edition action.
     */
    protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event) {
        if (repo != null) {
            final Element obj = event.getRowValue();
            if (obj == null) {
                return;
            }
            repo.update(obj);
        }
    }

    /**
     * Create a new element and add it to table items. If the table
     * {@link Repository} is not null, we also add the element to the database.
     * We also set its parent if it's not a contained element and the table
     * {@linkplain #parentElementProperty} is set.
     *
     * @return The newly created object.
     */
    protected Element createPojo() {
        return createPojo(null);
    }

    protected Element createPojo(final Element foreignParent) {
        Element result = null;

        if (repo != null) {
            result = (Element) repo.create();
        } else if (pojoClass != null) {
            try {
                result = session.getElementCreator().createElement(pojoClass);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (foreignParent != null && result instanceof AvecForeignParent) {
            ((AvecForeignParent) result).setForeignParentId(foreignParent.getId());
        }

        if (result instanceof Element) {
            final Element newlyCreated = (Element) result;

            AvecBornesTemporelles timePeriod = null;
            final Element parent = parentElementProperty.get();
            final Element owner = ownerElementProperty.get();

            boolean mustAddToNotNullOwner = false;

            /* Dans le cas où on a un parent, il n'est pas nécessaire de faire
            addChild(), car la liste des éléments de la table est directement
            cette liste d'éléments enfants, sur laquelle on fait un add().*/
            if (parent != null) {
                // this should do nothing for new
                newlyCreated.setParent(parent);
                if (parent instanceof AvecBornesTemporelles) {
                    timePeriod = (AvecBornesTemporelles) parent;
                }
            } /*
            ADD TO OWNER
            Mais dans le cas où on a un référant principal, il faut faire un
            addChild(), car la liste des éléments de la table n'est pas une
            liste d'éléments enfants. Le référant principal n'a qu'une liste
            d'identifiants qu'il convient de mettre à jour avec addChild().*/ else if (owner != null) {
                mustAddToNotNullOwner = true;
                if (owner instanceof AvecBornesTemporelles) {
                    timePeriod = (AvecBornesTemporelles) owner;
                }
            }

            // Force les bornes temporelles à respecter celles du parent.
            if (timePeriod != null) {
                final AvecBornesTemporelles fTimePeriod = timePeriod;
                if (newlyCreated instanceof AvecBornesTemporelles) {
                    final AvecBornesTemporelles child = (AvecBornesTemporelles) newlyCreated;
                    child.date_debutProperty().addListener((obs, oldValue, newValue) -> {
                        if (newValue != null && fTimePeriod.getDate_fin() != null && fTimePeriod.getDate_fin().isBefore(newValue)) {
                            child.setDate_debut(oldValue);
                            SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date de début après la fin de validité du parent.").showAndFade());
                        }
                    });
                    child.date_finProperty().addListener((obs, oldValue, newValue) -> {
                        if (newValue != null && fTimePeriod.getDate_debut() != null && fTimePeriod.getDate_debut().isAfter(newValue)) {
                            child.setDate_debut(oldValue);
                            SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date de fin antérieure à la validité du parent.").showAndFade());
                        }
                    });
                } else if (newlyCreated instanceof AbstractObservation) {
                    final AbstractObservation observation = (AbstractObservation) newlyCreated;
                    observation.dateProperty().addListener((obs, oldValue, newValue) -> {
                        final LocalDate parentDebut = fTimePeriod.getDate_debut();
                        final LocalDate parentFin = fTimePeriod.getDate_fin();
                        if (newValue != null) {
                            if (parentDebut != null && parentDebut.isAfter(newValue)) {
                                observation.setDate(oldValue);
                                SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date antérieure à la validité du parent.").showAndFade());

                            } else if (parentFin != null && parentFin.isBefore(newValue)) {
                                observation.setDate(oldValue);
                                SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date plus récente que la fin de validité du parent.").showAndFade());
                            }
                        }
                    });
                }
            }

            /*
             * If we've got the repository, we create the document immediately
             * in database.
             * HACK : we wait for a short moment to allow potential end-user to
             * get creation event and eventually update item list by himself.
             */
            if (repo != null) {
                repo.add(result);
                synchronized (this) {
                    try {
                        wait(100);
                    } catch (InterruptedException ex) {
                        SirsCore.LOGGER.log(Level.FINE, "Interrupted while waiting", ex);
                        Thread.currentThread().interrupt();
                    }
                }
            }
            /*
            ADD TO OWNER
            -> le 'addChild' est effectué après avoir ajouté l'élément en base
            afin de récupérer l'id attribué, nécessaire au addChild de certain
            'owner'.
            */
            if (mustAddToNotNullOwner) {
                owner.addChild(newlyCreated); //We already know that owner is not null.
                final Class ownerClaz = owner.getClass();
                Injector.getSession().getRepositoryForClass(ownerClaz).update(ownerClaz.cast(owner));
            }

            /*
             * We add created element in table items. However, as input list can
             * listen on database change, maybe the element is already present,
             * so we check if it's the case or not. Note that we compare only
             * references, because repositories should provide unique instance of
             * database objects, and a real equal() could send back different
             * items with same data.
             */
            synchronized (this) {
                synchronized (allValues) {
                    boolean mustAdd = true;
                    for (final Object o : allValues) {
                        if (o == newlyCreated) {
                            mustAdd = false;
                            break;
                        }
                    }
                    if (mustAdd) {
                        allValues.add(newlyCreated);
                    }
                }
            }
        } else {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune entrée ne peut être créée.");
            alert.setResizable(true);
            alert.showAndWait();
        }
        return (Element) result;
    }

    /**
     *
     * @param pojo The object the edition is requested for.
     * @param editionPredicate Prédicat d'étition du panneau à l'ouverture
     */
    public static void editElement(Object pojo, Predicate<Element> editionPredicate) {
        try {
            Injector.getSession().showEditionTab(pojo, editionPredicate);
        } catch (Exception ex) {
            Dialog d = new Alert(Alert.AlertType.ERROR, "Impossible d'afficher un éditeur", ButtonType.OK);
            d.setResizable(true);
            d.showAndWait();
            throw new UnsupportedOperationException("Failed to load panel : " + ex.getMessage(), ex);
        }
    }

    private static TableColumn getColumn(final String name, Collection<TableColumn> cols) {
        for (TableColumn col : cols) {
            if (name.equals(col.getId())) {
                return col;
            }
        }
        return null;
    }

    public Optional<TableColumn> getPropertyColumn(final PropertyDescriptor desc) {
        if (desc == null) {
            return Optional.empty();
        }

        final TableColumn col;
        if (desc.getReadMethod().getReturnType().isEnum()) {
            col = new EnumColumn(desc);
        } else {
            col = new PropertyColumn(desc);
            col.sortableProperty().setValue(Boolean.TRUE);
        }
        col.setId(desc.getName());
        return Optional.of(col);
    }

    @Override
    public String getPrintTitle() {
        return titleProperty.get();
    }

    @Override
    public ObjectProperty getPrintableElements() {
        final List selection = uiTable.getSelectionModel().getSelectedItems();

        return new SimpleObjectProperty(selection.isEmpty() ? new ArrayList<>(filteredValues) : new ArrayList(selection));
    }

    /**
     * Control of the columns to print into the ODT document.
     *
     * @return a list of property names to print
     * @see PojoTable#print()
     */
    protected List<String> propertyNamesToPrint() {

        final List<String> propertyNames = new ArrayList<>();
        for (final TableColumn column : getColumns()) {
            if (column.isVisible()) {
                if (column instanceof PropertyColumn) {
                    propertyNames.add(((PropertyColumn) column).name);
                } else if (column instanceof EnumColumn) {
                    propertyNames.add(((EnumColumn) column).name);
                }
            }
        }
        return propertyNames;
    }

    protected Map<String, Function<Element, String>> getPrintMapping() {
        return Collections.emptyMap();
    }

    @Override
    public boolean print() {
        boolean nothingToPrint = filteredValues.isEmpty();
        final List<String> propertyNames = propertyNamesToPrint();

        nothingToPrint = nothingToPrint || propertyNames.isEmpty();
        if (nothingToPrint) {
            SIRS.fxRun(false, new TaskManager.MockTask<>(() -> new Growl(Growl.Type.WARNING, "Aucun contenu à imprimer.").showAndFade()));
            return true;
        }

        final String title = titleProperty.get();
        // Choose output file
        final Path outputFile = SIRS.fxRunAndWait(() -> {
            final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Document OpenOffice", "*.odt");
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setSelectedExtensionFilter(extFilter);
            final File result = fileChooser.showSaveDialog(PojoTable.this.getScene().getWindow());
            if (result == null) {
                return null;
            } else {
                return result.toPath();
            }
        });

        if (outputFile == null) {
            return true; // Printing aborted. Return true to avoid another component to print instead of us.
        }
        final Task<Boolean> printTask = new Task() {
            @Override
            protected Object call() throws Exception {
                updateTitle("Impression : " + title);
                try (final TextDocument doc = TextDocument.newTextDocument()) {
                    doc.addParagraph(title).applyHeading();

                    final long totalWork = filteredValues.size();
                    final AtomicLong work = new AtomicLong(0);
                    ODTUtils.appendTable(doc, filteredValues.stream().peek(e -> updateProgress(work.getAndIncrement(), totalWork)).iterator(), propertyNames, getPrintMapping());

                    try (final OutputStream out = Files.newOutputStream(outputFile)) {
                        doc.save(out);
                    }
                }

                return true;
            }
        };

        // Events to launch when task finishes
        SIRS.fxRun(false, new TaskManager.MockTask(() -> {
            printTask.setOnFailed(event -> Platform.runLater(() -> GeotkFX.newExceptionDialog("Impossible d'imprimer la table \"" + title + "\"", printTask.getException()).show()));
            printTask.setOnCancelled(event -> Platform.runLater(() -> new Growl(Growl.Type.WARNING, "L'impression de la table \"" + title + "\" a été annulée").showAndFade()));
            printTask.setOnSucceeded(event -> Platform.runLater(() -> {
                new Growl(Growl.Type.INFO, "L'impression de la table \"" + title + "\" la carte est terminée").showAndFade();
                SIRS.openFile(outputFile).setOnSucceeded(evt -> {
                    if (!Boolean.TRUE.equals(evt.getSource().getValue())) {
                        Platform.runLater(() -> {
                            new Growl(Growl.Type.WARNING, "Impossible de trouver un programme pour ouvrir la table \"" + title + "\"").showAndFade();
                        });
                    }
                });
            }));
        }));

        // Print map.
        TaskManager.INSTANCE.submit(printTask);
        return true;
    }

////////////////////////////////////////////////////////////////////////////////
//
// INTERNAL CLASSES
//
////////////////////////////////////////////////////////////////////////////////
    public class EnumColumn extends TableColumn<Element, Object> {

        private final String name;

        public String getName() {
            return name;
        }

        private EnumColumn(final PropertyDescriptor desc) {
            super(labelMapper.mapPropertyName(desc.getDisplayName()));
            this.name = desc.getName();
            setEditable(false);
            setCellValueFactory(SIRS.getOrCreateCellValueFactory(desc.getName()));
            setCellFactory((TableColumn<Element, Object> param) -> {
                final TableCell<Element, Object> cell = new FXEnumTableCell(desc.getReadMethod().getReturnType(), new SirsStringConverter());
                editableProperty().bind(cellEditableProperty);
                return cell;
            });
            addEventHandler(TableColumn.editCommitEvent(), (CellEditEvent<Element, Object> event) -> {
                final Object rowElement = event.getRowValue();
                new PropertyReference<>(rowElement.getClass(), desc.getName()).set(rowElement, event.getNewValue());
                elementEdited(event);
            });
        }
    }

    private static final Callback<TableColumn<Element, Double>, TableCell<Element, Double>> DOUBLE_CELL_FACTORY = param -> {
        return new FXNumberCell(Double.class);
    };

    /**
     * Column used to display / edit simple attributes or references of an
     * element.
     */
    public final class PropertyColumn extends TableColumn<Element, Object> {

        private final Reference ref;
        private final String name;

        public String getName() {
            return name;
        }

        public PropertyColumn(final PropertyDescriptor desc) {
            super();
            this.name = desc.getName();

            String pName = labelMapper == null ? null : labelMapper.mapPropertyName(name);
            if (pName != null) {
                setText(pName);
            } else {
                setText(name);
            }

            //choix de l'editeur en fonction du type de données
            boolean isEditable = true;
            this.ref = desc.getReadMethod().getAnnotation(Reference.class);
            if (ref != null) {
                //reference vers un autre objet
                setCellFactory(SIRS.getOrCreateTableCellFactory(ref));
                try {
                    final Method propertyAccessor = ref.ref().getMethod(name + "Property");
                    setCellValueFactory((CellDataFeatures<Element, Object> param) -> {
                        if (param != null && param.getValue() != null && propertyAccessor != null) {
                            try {
                                return (ObservableValue) propertyAccessor.invoke(param.getValue());
                            } catch (Exception ex) {
                                SirsCore.LOGGER.log(Level.WARNING, null, ex);
                                return null;
                            }
                        } else {
                            return null;
                        }
                    });
                } catch (Exception ex) {
                    setCellValueFactory(SIRS.getOrCreateCellValueFactory(name));
                }

                // HACK : Needed to avoid comparison on ids.
                final Previews previews = session.getPreviews();
                final LabelComparator<Object> labelComparator = new LabelComparator<>(false);
                setComparator((o1, o2) -> {
                    if (o1 != null && o2 != null) {
                        final List<Preview> tmpPreviews = previews.get(new String[]{o1.toString(), o2.toString()});
                        if (tmpPreviews.size() == 2) {
                            o1 = tmpPreviews.get(0);
                            o2 = tmpPreviews.get(1);
                        }
                    }

                    return labelComparator.compare(o1, o2);
                });

            } else {
                setCellValueFactory(SIRS.getOrCreateCellValueFactory(name));
                final Method readMethod = desc.getReadMethod();

                // On conditionne a priori l'édition au fait que la méthode de lecture n'est pas indiquée comme autocalculée.
                isEditable = !readMethod.isAnnotationPresent(Computed.class);

                final Class type = readMethod.getReturnType();
                if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXBooleanCell());
                } else if (String.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXStringCell());
                } else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(Integer.class));
                } else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell.Float());
                } else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(Double.class));
                } else if (LocalDateTime.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXLocalDateTimeCell());
                } else if (LocalDate.class.isAssignableFrom(type)) {
                    if (TIMESTAMP_START_DATE.equalsIgnoreCase(name) || TIMESTAMP_END_DATE.equalsIgnoreCase(name)) {
                        setCellFactory((TableColumn<Element, Object> param) -> new FXLocalDateCell(true));
                    } else {
                        setCellFactory((TableColumn<Element, Object> param) -> new FXLocalDateCell());
                    }
                } else if (Point.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXPointCell());
                } else {
                    setCellFactory((TableColumn<Element, Object> param) -> new SimpleCell());
                    isEditable = false;
                }
            }

            if (isEditable) {
                editableProperty().bind(cellEditableProperty);
                setOnEditCommit(PojoTable.this::setOnPropertyCommit);
            } else {
                setEditable(false);
            }
        }

        public Reference getReference() {
            return ref;
        }
    }

    public void setOnPropertyCommit(final TableColumn.CellEditEvent<Element, Object> event) {
        ArgumentChecks.ensureNonNull("Event event", event);
        /*
         * We try to update data. If it's a failure, we store exception
         * to give more information to user. In all cases, a notification
         * is requested to inform user if its modification has succeded
         * or not.
         */
        Exception tmpError = null;
        final Object rowElement = event.getRowValue();
        if (rowElement == null) {
            return;
        }

        // Check / update value
        TablePosition<Element, Object> pos = event.getTablePosition();
        final TableColumn<Element, Object> col = pos.getTableColumn();
        ObservableValue<Object> value = col.getCellObservableValue(pos.getRow());
        if (value instanceof WritableValue) {
            final Object oldValue = value.getValue();

            try {
                final Object newValue = event.getNewValue();
                ((WritableValue) value).setValue(newValue);
                final Class<?> rowClass = rowElement.getClass();

                //On recalcule les coordonnées si la colonne modifiée correspond à une des propriétées de coordonnées géo ou linéaire.
                String modifiedPropretieName = ((PojoTable.PropertyColumn) col).getName();
                if ((rowElement != null) && (Positionable.class.isAssignableFrom(rowClass))) {

                    ConvertPositionableCoordinates.computeForModifiedPropertie((Positionable) rowElement, modifiedPropretieName);

                }

                if (rowElement instanceof Prestation) {

                    Prestation prestation = (Prestation) rowElement;
                    // REDMINE 7782 - Update prestation's registreAttribution value depending on the typePrestationId.
                    if ("typePrestationId".equals(modifiedPropretieName)
                            && (newValue instanceof String && !newValue.equals(oldValue))) {
                        FXPrestationPane.autoSelectRegistre(prestation, (String) newValue);
                    } else if (SirsCore.DATE_FIN_FIELD.equals(modifiedPropretieName)) {
                        // REDMINE 7782 - when changing prestation's end validity date, check the end timestamp date and show warnings if necessary.
                        final String refHoroId = prestation.getHorodatageStatusId();

                        // prestation in the SE's registre :
                        // - if "Non horodaté" -> no message, no effect
                        // - if "En attente" -> message to warn the user it is "En attente" and ask if reset status to "Non horodaté"
                        // - if "Horodaté" and "date d'horodatage de fin" not null -> message to warn the user it has already been timestamped for the end date
                        //   and ask if reset status to "Non horodaté"
                        if (prestation.getRegistreAttribution() && !HorodatageReference.getRefNonTimeStampedStatus().equals(refHoroId)) {
                            final StringBuilder message = FXPrestationPane.constructMessage(refHoroId, prestation.getHorodatageEndDate());

                            final Optional optional = PropertiesFileUtilities.showConfirmationDialog(message.toString(), "Modification date de fin", 600, 450, true);
                            if (optional.isPresent()) {
                                final Object result = optional.get();
                                if (ButtonType.YES.equals(result)) {
                                    prestation.setHorodatageStatusId(HorodatageReference.getRefNonTimeStampedStatus());
                                } else if (ButtonType.NO.equals(result)) {
                                    // do nothing
                                } else if (ButtonType.CANCEL.equals(result)) {
                                    prestation.setDate_fin((LocalDate) oldValue);
                                }
                            }
                        }
                    }
                }

                elementEdited(event);
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "Cannot update field.", e);
                tmpError = e;
                // rollback value in case of error.
                ((WritableValue) value).setValue(oldValue);
                event.getTableView().refresh(); // To ensure cell rendering is aware we've rollbacked data.
            }
        } else {
            tmpError = new IllegalStateException(new StringBuilder("Cannot affect read-only property in column ").append(col.getText()).append("from table ").append(titleProperty.get()).toString());
        }

        // Inform user
        final Exception error = tmpError;
        final String message = (error == null)
                ? "Le champs " + event.getTableColumn().getText() + " a été modifié avec succès"
                : "Erreur pendant la mise à jour du champs " + event.getTableColumn().getText();
        final ImageView graphic = new ImageView(error == null ? SIRS.ICON_CHECK_CIRCLE : SIRS.ICON_EXCLAMATION_TRIANGLE);
        final Label messageLabel = new Label(message, graphic);
        if (error == null) {
            showNotification(messageLabel);
        } else {
            final Hyperlink errorLink = new Hyperlink("Voir l'erreur");
            errorLink.setOnMouseClicked(linkEvent -> GeotkFX.newExceptionDialog(message, error).show());
            final HBox container = new HBox(5, messageLabel, errorLink);
            container.setAlignment(Pos.CENTER);
            container.setPadding(Insets.EMPTY);
            showNotification(container);
        }
    }

    /**
     * Display input node into the notification popup. All previous content will
     * be removed from popup.
     *
     * @param toShow The node to show in notification popup.
     */
    public void showNotification(final Node toShow) {
        if (toShow == null) {
            showNotification(Collections.EMPTY_LIST);
        } else {
            showNotification(Collections.singletonList(toShow));
        }
    }

    /**
     * Display input nodes into a popup stack pane. All previous content will be
     * removed from popup.
     *
     * @param toShow nodes to display in notification popup.
     */
    public void showNotification(final List<Node> toShow) {
        if (toShow == null || toShow.isEmpty()) {
            notifier.getChildren().clear();
        } else {
            notifier.getChildren().setAll(toShow);
        }
        // transition allows to see a difference when two identic message are queried in line.
        final FadeTransition transition = new FadeTransition(new Duration(1000), notifier);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.play();
    }

    /**
     * Change row style according to input {@link Element#validProperty() }.
     */
    private class ValidityRow extends TableRow<Element> {

        ValidityRow() {
            final BooleanBinding authorizedBinding = Bindings.createBooleanBinding(() -> getItem() == null ? false : session.editionAuthorized(getItem()), itemProperty());
            this.editableProperty().bind(cellEditableProperty.and(authorizedBinding));
            // Hack : Apparently, forbidding row editability is not enough to prevent
            // the contained cells to be edited, so we have to force disability on thes cases.
            this.disableProperty().bind(cellEditableProperty.and(authorizedBinding.not()));
        }

        @Override
        protected void updateItem(Element item, boolean empty) {
            super.updateItem(item, empty);

            if (item != null && !item.getValid()) {
                getStyleClass().add("invalidRow");
            } else {
                getStyleClass().removeAll("invalidRow");
            }
        }
    }
}
