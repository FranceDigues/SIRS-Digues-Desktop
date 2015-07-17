package fr.sirs.plugin.document;

import fr.sirs.plugin.document.ui.DynamicDocumentsPane;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import javafx.scene.Parent;
import javafx.scene.image.Image;

/**
 * Gestion de la création de documents dynamiques.
 *
 * @author Cédric Briançon (Geomatys)
 */
public class DynamicDocumentTheme extends AbstractPluginsButtonTheme {
    private static final Image BUTTON_IMAGE = new Image(DocumentManagementTheme.class.getResourceAsStream("images/gen_etats.png"));

    public DynamicDocumentTheme() {
        super("Documents dynamiques", "Documents dynamiques", BUTTON_IMAGE);
    }

    @Override
    public Parent createPane() {
        return new DynamicDocumentsPane();
    }
}
