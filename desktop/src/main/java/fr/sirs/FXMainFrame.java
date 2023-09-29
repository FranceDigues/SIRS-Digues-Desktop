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

import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.core.component.SirsDBInfoRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.core.model.Role;
import fr.sirs.core.model.Utilisateur;
import fr.sirs.digue.DiguesTab;
import fr.sirs.map.FXMapTab;
import fr.sirs.query.FXSearchPane;
import fr.sirs.theme.Theme;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import fr.sirs.ui.AlertItem;
import static fr.sirs.ui.AlertItem.AlertItemLevel.HIGH;
import static fr.sirs.ui.AlertItem.AlertItemLevel.INFORMATION;
import fr.sirs.ui.AlertManager;
import fr.sirs.ui.Growl;
import fr.sirs.util.FXFreeTab;
import fr.sirs.util.SynchroTask;
import fr.sirs.util.property.SirsPreferences;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.util.ProgressMonitor;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;

/**
 *
 * @author Geomatys
 */
public class FXMainFrame extends BorderPane {

    public static final Image ICON_ALL  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TABLE,16,FontAwesomeIcons.DEFAULT_COLOR),null);
    public static final Image ICON_ALERT  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_BELL,16,FontAwesomeIcons.DEFAULT_COLOR),null);

    private static final String BUNDLE_KEY_ADMINISTATION = "administration";
    private static final String BUNDLE_KEY_USERS = "users";
    private static final String BUNDLE_KEY_VALIDATION = "validation";
    private static final String BUNDLE_KEY_REFERENCES = "references";
    private static final String BUNDLE_KEY_DESIGNATIONS = "designations";
    private static final String BUNDLE_KEY_SEARCH = "search";

    private static final String CSS_POPUP_ALERTS = "popup-alerts";
    private static final String CSS_POPUP_RAPPEL_TITLE = "popup-alerts-rappel-title";
    private static final String CSS_POPUP_ALERT = "popup-alert";
    private static final String CSS_POPUP_ALERT_NORMAL = "popup-alert-normal";
    private static final String CSS_POPUP_ALERT_HIGH = "popup-alert-high";
    private static final String CSS_POPUP_ALERT_INFORMATION = "popup-alert-information";

    private static WeakReference<Stage> MODEL_STAGE;

    private final Session session = Injector.getBean(Session.class);
    private final ResourceBundle bundle = ResourceBundle.getBundle(FXMainFrame.class.getName(), Locale.getDefault(), Thread.currentThread().getContextClassLoader());
    private final Popup alertPopup = new Popup();
    private final IntegerBinding numberOfAlerts;
    private final ObjectProperty<Plugin> activePlugin = new SimpleObjectProperty<>();

    @FXML private MenuButton uiThemesLocalized;
    @FXML private MenuButton uiThemesUnlocalized;
    @FXML private MenuButton uiPlugins;
    @FXML private ToggleButton uiAlertsBtn;
    @FXML Button uiPrintButton;
    @FXML private ImageView uiPluginsImg;
    @FXML private HBox uiToolBarPlugins;
    @FXML private TabPane uiTabs;
    @FXML private MenuBar uiMenu;
    @FXML private MenuItem uiSynchroDb;

    private FXMapTab mapTab;
    private DiguesTab diguesTab;
    private FXFreeTab searchTab;

    public FXMainFrame() {
        SIRS.loadFXML(this, FXMainFrame.class);
        final ProgressMonitor progressMonitor = new ProgressMonitor(TaskManager.INSTANCE);
        progressMonitor.getStylesheets().add("/fr/sirs/ProgressMonitor.css");
        initProgressMonitor(progressMonitor);
        final ToolBar pm = new ToolBar(progressMonitor);
        pm.setPadding(Insets.EMPTY);
        pm.prefHeightProperty().bind(uiMenu.heightProperty());
        ((HBox) uiMenu.getParent()).getChildren().add(pm);

        // Load themes
        final Theme[] themes = Plugins.getThemes();
        for(final Theme theme : themes){
            if (Theme.Type.LOCALIZED.equals(theme.getType())) {
                uiThemesLocalized.getItems().add(toMenuItem(theme));
            } else if (Theme.Type.UNLOCALIZED.equals(theme.getType())) {
                uiThemesUnlocalized.getItems().add(toMenuItem(theme));
            }
        }

        // Load plugins
        for (Plugin plugin : Plugins.getPlugins()) {
            if (plugin.name.equals(CorePlugin.NAME)) {
                continue;
            }
            uiPlugins.getItems().add(toMenuItem(plugin));
        }
        uiPlugins.visibleProperty().bind(Bindings.isNotEmpty(uiPlugins.getItems()));
        uiPlugins.managedProperty().bind(uiPlugins.visibleProperty());

        final Menu uiAdmin = new Menu(bundle.getString(BUNDLE_KEY_ADMINISTATION));
        uiMenu.getMenus().add(1, uiAdmin);

        final MenuItem uiUserAdmin = new MenuItem(bundle.getString(BUNDLE_KEY_USERS));
        uiUserAdmin.setOnAction((ActionEvent event) -> {
            addTab(Injector.getSession().getOrCreateAdminTab(Session.AdminTab.USERS, bundle.getString(BUNDLE_KEY_USERS)));
        });

        final MenuItem uiValidation = new MenuItem(bundle.getString(BUNDLE_KEY_VALIDATION));
        uiValidation.setOnAction((ActionEvent event) -> {
            addTab(Injector.getSession().getOrCreateAdminTab(Session.AdminTab.VALIDATION, bundle.getString(BUNDLE_KEY_VALIDATION)));
        });

        final Menu uiReference = new Menu(bundle.getString(BUNDLE_KEY_REFERENCES));
        final Menu uiDesignation = new Menu(bundle.getString(BUNDLE_KEY_DESIGNATIONS));

        final List<Class<? extends Element>> elementTypes = Session.getConcreteSubTypes(Element.class);
        for (final Class c : elementTypes) {
            if (ReferenceType.class.isAssignableFrom(c)) {
                uiReference.getItems().add(toMenuItem(c, Choice.REFERENCE));
            } else {
                uiDesignation.getItems().add(toMenuItem(c, Choice.MODEL));
            }
        }

        // A simple comparator to sort menu items according to their title.
        final Collator collator = Collator.getInstance();
        final Comparator<MenuItem> menuComparator = (o1, o2) -> {
            final String t1, t2;
            if (o1 != null && (t1 = o1.getText()) != null) {
                if (o2 != null && (t2 = o2.getText()) != null) {
                    return collator.compare(t1, t2);
                } else {
                    return -1;
                }
            }

            return 0; // None has title to compare.
        };

        uiReference.getItems().sort(menuComparator);
        uiDesignation.getItems().sort(menuComparator);

        uiAdmin.getItems().addAll(uiUserAdmin, uiValidation, uiReference, uiDesignation);
        uiAdmin.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            Utilisateur user = session.utilisateurProperty().get();
            return user != null && Role.ADMIN.equals(user.getRole());
        }, session.utilisateurProperty()));

        final ObservableSet<AlertItem> alerts = AlertManager.getInstance().getAlerts();
        alerts.addListener((Observable obs) -> updateAlertPopupContent());
        numberOfAlerts = Bindings.size(alerts);
        numberOfAlerts.addListener(obs -> {
            final int alertNumber = ((IntegerBinding)obs).get();
            if (alertNumber < 1) {
                uiAlertsBtn.setText("pas d'alerte");
            } else if (alertNumber == 1) {
                uiAlertsBtn.setText("1 alerte");
            } else uiAlertsBtn.setText(new StringBuilder().append(alertNumber).append(" alertes").toString());
        });
        numberOfAlerts.invalidate();

        uiAlertsBtn.setGraphic(new ImageView(ICON_ALERT));
        uiAlertsBtn.managedProperty().bind(uiAlertsBtn.visibleProperty());
        uiAlertsBtn.visibleProperty().bind(AlertManager.getInstance().alertsEnabledProperty());

        alertPopup.addEventHandler(WindowEvent.WINDOW_HIDING, evt -> uiAlertsBtn.setSelected(false));
        uiAlertsBtn.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue && !alertPopup.isShowing()) {
                showAlertPopup();
            } else if (!newValue && alertPopup.isShowing()) {
                alertPopup.hide();
            }
        });

        alertPopup.getScene().setUserAgentStylesheet(SIRS.CSS_PATH);
        alertPopup.setAutoHide(true);
        alertPopup.setConsumeAutoHidingEvents(false);
        // set alert popup to keep its position near alert button.
        final InvalidationListener trListener = (obs) -> {
            if (alertPopup.isShowing())
                showAlertPopup();
        };
        uiAlertsBtn.localToSceneTransformProperty().addListener(trListener);
        updateAlertPopupContent();

        SIRS.LOGGER.log(Level.FINE, org.apache.sis.setup.About.configuration().toString());

        //on change les boutons de la barre en fonction du plugin actif.
        activePlugin.addListener((ObservableValue<? extends Plugin> observable, Plugin oldValue, Plugin newValue) -> {
            uiPlugins.setText(newValue.getTitle().toString());
            uiToolBarPlugins.getChildren().clear();
            for (Theme theme : newValue.getThemes()) {
                uiToolBarPlugins.getChildren().add(toButton((AbstractPluginsButtonTheme) theme));
            }
        });

        //on ecoute le changement d'element imprimable
        uiPrintButton.setAlignment(Pos.CENTER);
        uiPrintButton.setTextAlignment(TextAlignment.CENTER);

        final ReadOnlyObjectProperty<Printable> printableProperty = PrintManager.printableProperty();
        uiPrintButton.disableProperty().bind(printableProperty.isNull());

        final ChangeListener<Printable> printListener = (obs, oldValue, newValue) -> {
            if(newValue==null){
                uiPrintButton.setText("Impression");
            }else{
                String title = newValue.getPrintTitle();
                if(title==null || title.isEmpty()){
                    uiPrintButton.setText("Impression");
                }else{
                    final StringBuilder builder = new StringBuilder("Impression");
                    uiPrintButton.setText(builder.append(System.lineSeparator()).append(title).toString());
                }
            }
        };
        PrintManager.printButton = uiPrintButton;

        printableProperty.addListener(printListener);
        printListener.changed(printableProperty, null, printableProperty.get());

        SirsDBInfoRepository infoRepo = session.getApplicationContext().getBean(SirsDBInfoRepository.class);
        Optional<String> remoteDb = infoRepo.get()
                .map(SirsDBInfo::getRemoteDatabase);

        if (remoteDb.isPresent()) {
            final String distant = remoteDb.get();
            final String localDb =  SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.COUCHDB_LOCAL_ADDR) + session.getConnector().getDatabaseName();
            final DatabaseRegistry registry = session.getApplicationContext().getBean(DatabaseRegistry.class);
            uiSynchroDb.setOnAction(clic -> {
                uiSynchroDb.setDisable(true);
                final Task t = new SynchroTask(registry, localDb, distant, true);

                t.setOnSucceeded(evt -> Platform.runLater(() -> uiSynchroDb.setDisable(false)));
                t.setOnFailed(evt -> Platform.runLater(() -> new Growl(Growl.Type.ERROR,
                        "La synchronisation n'a pu aboutir. Vérifiez l'état de la tâche (voir ci-dessus) pour plus d'informations.").showAndFade()));

                TaskManager.INSTANCE.submit(t);
            });
        } else {
            uiSynchroDb.setVisible(false);
        }
    }

    /**
     * Update content of the popup displaying alerts, using data from {@link AlertManager#getAlerts() }
     */
    public void updateAlertPopupContent() {
        final ObservableSet<AlertItem> alerts = AlertManager.getInstance().getAlerts();
        final VBox vbox = new VBox();
        vbox.getStyleClass().add(CSS_POPUP_ALERTS);

        final DateTimeFormatter dfFormat = DateTimeFormatter.ofPattern("d MMMM uuuu");
        for (final AlertItem alert : alerts) {
            final Label label = new Label(alert.getTitle());
            label.getStyleClass().add(CSS_POPUP_RAPPEL_TITLE);
            final Button closeAlert = new Button("x");
            closeAlert.setCancelButton(true);
            closeAlert.getStyleClass().add("btn-without-style");
            closeAlert.setOnAction(evt -> alerts.remove(alert));
            final HBox closeBox = new HBox(closeAlert);
            closeBox.setAlignment(Pos.TOP_RIGHT);

            final VBox alertBox = new VBox(closeBox, label, new Label("Echéance " + alert.getDate().format(dfFormat)));
            alertBox.getStyleClass().add(CSS_POPUP_ALERT);
            if(alert.getLevel()== HIGH)
                alertBox.getStyleClass().add(CSS_POPUP_ALERT_HIGH);
            else if(alert.getLevel()==INFORMATION)
                alertBox.getStyleClass().add(CSS_POPUP_ALERT_INFORMATION);
            else
                alertBox.getStyleClass().add(CSS_POPUP_ALERT_NORMAL);
            vbox.getChildren().add(alertBox);
        }

        if (alerts.size() > 0) {
            final Button clearList = new Button("Vider la liste d'alertes");
            clearList.setMaxWidth(Double.MAX_VALUE);
            clearList.setAlignment(Pos.CENTER);
            clearList.getStyleClass().add("buttonbar-button");
            clearList.setOnAction(evt -> alerts.clear());
            vbox.getChildren().add(clearList);
        }

        final ScrollPane scroll = new ScrollPane(vbox);
        scroll.setMaxHeight(600);
        alertPopup.getContent().setAll(scroll);
        if (alertPopup.isShowing()) {
            showAlertPopup(); // Force alert popup to stay near alert button.
        }
    }

    public void showAlertPopup() {
        final Point2D popupPos = uiAlertsBtn.localToScreen(uiAlertsBtn.getWidth(), 0);
        if (popupPos != null) {
            alertPopup.show(uiAlertsBtn, popupPos.getX() - alertPopup.getWidth() - 5, popupPos.getY() - alertPopup.getHeight());
        }
    }

    public TabPane getUiTabs() {
        return uiTabs;
    }

    public synchronized FXMapTab getMapTab() {
        if(mapTab==null){
            mapTab = new FXMapTab(uiTabs);
        }
        return mapTab;
    }

    public synchronized DiguesTab getDiguesTab() {
        if(diguesTab==null){
            diguesTab = new DiguesTab(uiTabs);
        }
        return diguesTab;
    }

    public final synchronized void addTab(Tab tab){
        if (tab == null) return;
        if (!uiTabs.equals(tab.getTabPane())) {
            uiTabs.getTabs().add(tab);
        }
        uiTabs.getSelectionModel().select(tab);
    }

    /**
     * HACK : Add an item in progress monitor list, allowing to clear list of
     * tasks in error.
     * @param pm Progress monitor to hack.
     */
    private void initProgressMonitor(ProgressMonitor pm) {
//        for (final Node n : pm.getChildren()) {
//            if (n instanceof MenuButton) {
//                final MenuButton menu = (MenuButton)n;
//                Node graphic = menu.getGraphic();
//                if (graphic instanceof ImageView && ((ImageView)graphic).getImage() == ProgressMonitor.ICON_ERROR) {
//                    final MenuItem clearList = new MenuItem("Vider la liste");
//                    clearList.setOnAction(evt -> TaskManager.INSTANCE.getTasksInError().clear());
//                    clearList.setGraphic(new ImageView(GeotkFX.ICON_DELETE));
//                    menu.getItems().add(0, clearList);
//                    menu.getItems().add(1, new SeparatorMenuItem());
//                    break;
//                }
//            }
//        }
    }

    private enum Choice{REFERENCE, MODEL};
    private MenuItem toMenuItem(final Class clazz, final Choice typeOfSummary){
        final LabelMapper mapper = LabelMapper.get(clazz);
        final MenuItem item;
        if(typeOfSummary==Choice.REFERENCE){
            item = new MenuItem(clazz.getSimpleName()+" ("+mapper.mapClassName()+")");
        }
        else{
            item = new MenuItem(mapper.mapClassName());
        }

        final EventHandler<ActionEvent> handler;
        if(typeOfSummary==Choice.REFERENCE){
            handler = (ActionEvent event) -> {
                addTab(Injector.getSession().getOrCreateReferenceTypeTab(clazz));
            };
        }
        else{
            handler = (ActionEvent event) -> {
                addTab(Injector.getSession().getOrCreateDesignationTab(clazz));
            };
        }

        item.setOnAction(handler);
        return item;
    }

    /**
     * Property contenant le plugin actif.
     *
     * @return Le plugin activé dans l'interface.
     */
    public ObjectProperty<Plugin> activePluginProperty(){
        return activePlugin;
    }

    @FXML
    private void clearCache() {
        final Collection<AbstractSIRSRepository> repos = session.getModelRepositories();
        for(final AbstractSIRSRepository repo : repos){
            repo.clearCache();
        }
        // HACK-REDMINE-4408 : preview cache must be refreshed too
        session.getPreviews().clearCache();
    }

    /**
     * Génère le menu déroulant pour le plugin donné.
     *
     * @param plugin Plugin à montrer.
     * @return Une entrée de menu pour activer le plugin.
     */
    private MenuItem toMenuItem(final Plugin plugin) {
        final MenuItem item = new MenuItem(plugin.getTitle().toString());
        if (plugin.getImage() != null) {
            // Une image a été fournie pour le plugin, elle remplacera l'icône générale des modules.
            uiPluginsImg.setImage(plugin.getImage());
        } else {
            // chargement de l'image par défaut
            uiPluginsImg.setImage(new Image(this.getClass().getResourceAsStream("images/menu-modules.png")));
        }
        item.setOnAction(event -> activePlugin.set(plugin));
        return item;
    }

    /**
     * Crée un bouton pour représenter le sous menu d'un plugin.
     *
     * @param theme Le thème a afficher dans un bouton.
     * @return Un bouton dont le rôle est d'ouvrir le panneau associé au thème sur clic.
     */
    private ButtonBase toButton(final AbstractPluginsButtonTheme theme) {
        final ButtonBase button;
        if (theme.getSubThemes() != null && !theme.getSubThemes().isEmpty()) {
            button = new MenuButton(theme.getName());
            for (final Theme t : theme.getSubThemes()) {
                ((MenuButton) button).getItems().add(toMenuItem(t));
            }
        } else {
            button = new Button(theme.getName());
            button.setOnAction(new DisplayTheme(theme));
        }

        if (theme.getDescription() != null && !theme.getDescription().isEmpty()) {
            button.setTooltip(new Tooltip(theme.getDescription()));
        }
        if (theme.getImg() != null) {
            final ImageView imageView = new ImageView();
            imageView.setFitHeight(32);
            imageView.setFitWidth(32);
            imageView.setImage(theme.getImg());
            button.setGraphic(imageView);
        }
        // Button style
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setTextAlignment(TextAlignment.CENTER);
        button.setWrapText(true);
        button.setMinWidth(USE_PREF_SIZE);
        button.setMinHeight(USE_COMPUTED_SIZE);
        button.setPrefWidth(170);
        button.setPrefHeight(USE_COMPUTED_SIZE);
        button.setMaxWidth(USE_PREF_SIZE);
        button.setMaxHeight(Double.MAX_VALUE);
        button.setContentDisplay(ContentDisplay.TOP);
        button.setAlignment(Pos.CENTER);
        button.getStyleClass().add("buttonbar-button");

        return button;
    }

    /**
     * Créé un item de menu et son arborescence pour le thème choisi.
     *
     * @param theme Le thème à afficher dans un menu.
     * @return Un élément de meu dont le rôle est d'ouvrir le panneau associé au
     * thème lors d'un clic.
     */
    private MenuItem toMenuItem(final Theme theme) {
        final List<Theme> subs = theme.getSubThemes();
        final MenuItem item;
        // Atomic case
        if (subs.isEmpty()) {
            item = new MenuItem(theme.getName());
            item.setOnAction(new DisplayTheme(theme));
        // container case
        } else {
            item = new Menu(theme.getName());
            //action avec tous les sous-panneaux
            final MenuItem all = new MenuItem("Ouvrir l'ensemble");
            all.setGraphic(new ImageView(ICON_ALL));
            all.setOnAction(new DisplayTheme(theme));
            ((Menu) item).getItems().add(all);

            for (final Theme sub : subs) {
                ((Menu) item).getItems().add(toMenuItem(sub));
            }
        }

        return item;
    }

    @FXML
    void openMap(ActionEvent event) {
        getMapTab().show();
    }

    @FXML
    void openDigueTab(ActionEvent event) {
        getDiguesTab().show();
    }

    /**
     * Get or create search tab. If it has been previously closed, we reset it,
     * so user won't be bothered with an old request.
     */
    @FXML
    private void openSearchTab(ActionEvent event) {
        if (searchTab == null || !uiTabs.equals(searchTab.getTabPane())) {
            searchTab = new FXFreeTab(bundle.getString(BUNDLE_KEY_SEARCH), false);
            final FXSearchPane searchPane = new FXSearchPane();
            searchTab.setContent(searchPane);
            uiTabs.getTabs().add(searchTab);
        }
        uiTabs.getSelectionModel().select(searchTab);
    }

    @FXML
    void openCompte(ActionEvent event){
        if (session.getUtilisateur()!=null) {
            addTab(session.getOrCreateElementTab(session.getUtilisateur(), SIRS.CONSULTATION_PREDICATE));
        }
    }

    @FXML
    void openPref(ActionEvent event) {
        final Stage prefEditor = SIRS.getPreferenceEditor();
        prefEditor.show();
        prefEditor.requestFocus();
    }

    @FXML
    void exit(ActionEvent event) {
        System.exit(0);
    }

    /**
     *
     * @return Une fenêtre affichant les modèles de données du noyau et des modules chargés.
     * @throws IOException Si l'on ne peut accéder à un modèle de données.
     */
    public static Stage modelStage() throws IOException {
        Stage stage = MODEL_STAGE == null? null : MODEL_STAGE.get();
        if (stage == null) {

            final TabPane tabPane = new TabPane();
            for (final Plugin p : Plugins.getPlugins()) {
                final Optional<Image> opt = p.getModelImage();
                if (opt.isPresent()) {
                    final ImageView imageView = new ImageView(opt.get());
                    final Tab tab = new Tab(p.getTitle().toString(), new ScrollPane(imageView));
                    tabPane.getTabs().add(tab);
                }
            }

            stage = new Stage();

            stage.getIcons().add(SIRS.ICON);
            stage.setScene(new Scene(tabPane));
            stage.setTitle("Modèle");
            stage.setWidth(800);
            stage.setHeight(600);
        }

        return stage;
    }

    @FXML
    public void openModel() throws IOException {
        final Stage s = modelStage();
        s.show();
        s.requestFocus();
    }

    @FXML
    void deconnect(ActionEvent event) throws IOException{
        if (SIRS.getLauncher() != null) {
            SIRS.getLauncher().restart();
        } else {
            // Ne devrait pas arriver car on a toujours une instance de Stage à la création.
            SIRS.LOADER.showSplashStage();
        }
    }

    @FXML
    void changeUser(ActionEvent event) throws IOException{
        this.getScene().getWindow().hide();
        session.setUtilisateur(null);
        clearCache();
        session.getTaskManager().reset();
        SIRS.LOADER.showSplashStage();
    }

    @FXML
    private void checkReferences(){
        Injector.getSession().getTaskManager().submit(Injector.getSession().getReferenceChecker());
    }

    @FXML
    private void print() throws Exception {
        TaskManager.INSTANCE.submit("Impression", () -> {
            session.getPrintManager().printFocusedPrintable();
            return true;
        });
    }

    @FXML
    private void disorderPrint(){
        addTab(session.getOrCreatePrintTab(Session.PrintTab.DESORDRE, "Fiches détaillées de désordres"));
    }

    @FXML
    private void reseauFermePrint(){
        addTab(session.getOrCreatePrintTab(Session.PrintTab.RESEAU_FERME, "Fiches détaillées de réseaux hydrauliques fermés"));
    }

    @FXML
    private void ouvrageAssociePrint(){
        addTab(session.getOrCreatePrintTab(Session.PrintTab.OUVRAGE_ASSOCIE, "Fiches détaillées d'ouvrages associés"));
    }

    @FXML
    private void elementModelPrint(){
        addTab(session.getOrCreatePrintTab(Session.PrintTab.TEMPLATE, "Edition des modèles de fiche"));
    }

    @FXML
    private void reportPrint() {
        addTab(session.getOrCreatePrintTab(Session.PrintTab.REPORT, "Edition des modèles de rapport"));
    }

    @FXML
    public void showUserGuide() {
        try {
            SIRS.browseURL(new URL("http://sirs-digues.info/documents/"), "Guide utilisateur");
        } catch (MalformedURLException ex) {
            SirsCore.LOGGER.log(Level.WARNING, null, ex);
            GeotkFX.newExceptionDialog("L'URL demandée est invalide.", ex).show();
        }
    }

    @FXML
    public void openAppInfo() {
        final Stage infoStage = new Stage();
        infoStage.getIcons().add(SIRS.ICON);
        infoStage.setTitle("À propos");
        infoStage.initStyle(StageStyle.UTILITY);
        infoStage.setScene(new Scene(new FXAboutPane()));
        infoStage.setResizable(false);
        infoStage.show();
    }

    private class DisplayTheme implements EventHandler<ActionEvent> {

        private final Theme theme;

        public DisplayTheme(Theme theme) {
            this.theme = theme;
        }

        @Override
        public void handle(ActionEvent event) {
            final Tab result = Injector.getSession().getOrCreateThemeTab(theme);
            if(result==null) return;
            addTab(result);
        }
    }
}