
package fr.sirs.plugin.document.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.plugin.document.DocumentManagementTheme;
import fr.sirs.plugin.document.FileTreeItem;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.apache.sis.util.logging.Logging;

/**
 *
 * @author guilhem
 */
public class DocumentsPane extends GridPane {
    
    @FXML
    private Button importDocButton;

    @FXML
    private Button deleteDocButton;

    @FXML
    private Button setFolderButton;

    @FXML
    private TreeTableView<FileTreeItem> tree1;

    @FXML
    private Button addDocButton;

    @FXML
    private Button addFolderButton;

    @FXML
    private Button listButton;

    private static final Image ADDF_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/add_folder.png"));
    private static final Image ADDD_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/add_doc.png"));
    private static final Image IMP_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/import.png"));
    private static final Image DEL_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/remove.png"));
    private static final Image SET_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/set.png"));
    private static final Image LIST_BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/list.png"));
    
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");
    
    private static final Logger LOGGER = Logging.getLogger(DocumentsPane.class);
    
    public DocumentsPane() {
        SIRS.loadFXML(this, DocumentsPane.class);
        Injector.injectDependencies(this);
        
        addFolderButton.setGraphic(new ImageView(ADDF_BUTTON_IMAGE));
        importDocButton.setGraphic(new ImageView(IMP_BUTTON_IMAGE));
        deleteDocButton.setGraphic(new ImageView(DEL_BUTTON_IMAGE));
        setFolderButton.setGraphic(new ImageView(SET_BUTTON_IMAGE));
        addDocButton.setGraphic(new ImageView(ADDD_BUTTON_IMAGE));
        listButton.setGraphic(new ImageView(LIST_BUTTON_IMAGE));
        
        // Name column
        tree1.getColumns().get(0).setEditable(false);
        tree1.getColumns().get(0).setCellValueFactory(new Callback() {
            @Override
            public ObservableValue call(Object param) {
                final File f = (File) ((CellDataFeatures)param).getValue().getValue();
                return new SimpleStringProperty(f.getName());
            }
        });
        
        // Date column
        tree1.getColumns().get(1).setEditable(false);
        tree1.getColumns().get(1).setCellValueFactory(new Callback() {
            @Override
            public ObservableValue call(Object param) {
                final File f = (File) ((CellDataFeatures)param).getValue().getValue();
                synchronized (DATE_FORMATTER) {
                    return new SimpleStringProperty(DATE_FORMATTER.format(new Date(f.lastModified())));
                }
            }
        });
        
        // Size column
        tree1.getColumns().get(2).setEditable(false);
        tree1.getColumns().get(2).setCellValueFactory(new Callback() {
            @Override
            public ObservableValue call(Object param) {
                final File f = (File) ((CellDataFeatures)param).getValue().getValue();
                return new SimpleStringProperty(getStringSizeFile(f));
            }
        });
        
        // Inventory number column
        tree1.getColumns().get(3).setEditable(false);
        tree1.getColumns().get(3).setCellValueFactory(new Callback() {
            @Override
            public ObservableValue call(Object param) {
                final File f = (File) ((CellDataFeatures)param).getValue().getValue();
                return new SimpleStringProperty(getInventoryNumber(f));
            }
        });
        
        // class place column
        tree1.getColumns().get(4).setEditable(false);
        tree1.getColumns().get(4).setCellValueFactory(new Callback() {
            @Override
            public ObservableValue call(Object param) {
                final File f = (File) ((CellDataFeatures)param).getValue().getValue();
                return new SimpleStringProperty(getClassPlace(f));
            }
        });
        
        // do integrated column
        tree1.getColumns().get(5).setCellValueFactory(new Callback() {
            @Override
            public ObservableValue call(Object param) {
                final File f = (File) ((CellDataFeatures)param).getValue().getValue();
                return new SimpleObjectProperty(f);
            }
        });
        
        
        tree1.getColumns().get(5).setCellFactory(new Callback() {
            @Override
            public TreeTableCell call(Object param) {
                return new DOIntegatedCell();
            }
        });
        
        
        TreeItem root = new FileTreeItem(new File("/home/guilhem/Bureau/sym_doc_test"));
        tree1.setRoot(root);
        
    }
    
    public static String getInventoryNumber(final File f) {
        final Properties prop = getSirsProperties(f);
        return prop.getProperty(f.getName() + "_inventory_number", "");
    }
    
    public static String getClassPlace(final File f) {
        final Properties prop = getSirsProperties(f);
        return prop.getProperty(f.getName() + "_class_place", "");
    }
    
    public static Boolean getDOIntegrated(final File f) {
        final Properties prop = getSirsProperties(f);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_do_integrated", "false"));
    }
    
    public static void setDOIntegrated(final File f, boolean value) {
        final Properties prop   = getSirsProperties(f);
        prop.put(f.getName() + "_do_integrated", Boolean.toString(value));
        
        try {
            final File sirsPropFile = getSirsPropertiesFile(f);
            prop.store(new FileWriter(sirsPropFile), "");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Erro while accessing sirs properties file.", ex);
        }
    }
    
    public static File getSirsPropertiesFile(final File f) throws IOException {
        final File parent = f.getParentFile();
        if (parent != null) {
            final File sirsPropFile = new File(parent, "sirs.properties");
            if (!sirsPropFile.exists()) {
                sirsPropFile.createNewFile();
            }
            return sirsPropFile;
        }
        return null;
    }
    
    public static Properties getSirsProperties(final File f) {
        final Properties prop = new Properties();
        try {
            final File sirsPropFile = getSirsPropertiesFile(f);
            if (sirsPropFile != null) {
                prop.load(new FileReader(sirsPropFile));
            } 
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Erro while loading/creating sirs properties file.", ex);
        }
        return prop;
    }
    
    public static String getStringSizeFile(final File f) {
        final long size        = getFileSize(f);
        final DecimalFormat df = new DecimalFormat("0.0");
        final float sizeKb     = 1024.0f;
        final float sizeMo     = sizeKb * sizeKb;
        final float sizeGo     = sizeMo * sizeKb;
        final float sizeTerra  = sizeGo * sizeKb;

        if (size < sizeKb) {
            return df.format(size)          + " o";
        } else if (size < sizeMo) {
            return df.format(size / sizeKb) + " Ko";
        } else if (size < sizeGo) {
            return df.format(size / sizeMo) + " Mo";
        } else if (size < sizeTerra) {
            return df.format(size / sizeGo) + " Go";
        }
        return "";
    }
    
    public static long getFileSize(final File f) {
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
    
    private static class DOIntegatedCell extends TreeTableCell {

        private CheckBox box = new CheckBox();

        public DOIntegatedCell() {
            setGraphic(box);
            box.disableProperty().bind(editingProperty());
            box.selectedProperty().addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    File f = (File) getItem();
                    if (f != null) {
                        setDOIntegrated(f, newValue);
                    }
                }
            });
        }
        
        
        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            File f = (File) item;
            if (f == null || f.isDirectory()) {
                box.setVisible(false);
            } else {
                box.setVisible(true);
                box.setSelected(getDOIntegrated(f));
            }
        }
        
        
        
    }
}
