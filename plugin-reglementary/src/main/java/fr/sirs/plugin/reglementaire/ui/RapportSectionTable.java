
package fr.sirs.plugin.reglementaire.ui;

import fr.sirs.core.model.Element;
import fr.sirs.core.model.PhotoChoice;
import fr.sirs.core.model.RapportSectionObligationReglementaire;
import fr.sirs.core.model.SectionType;
import fr.sirs.theme.ui.PojoTable;
import javafx.scene.control.TableColumn;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class RapportSectionTable extends PojoTable {

    public RapportSectionTable() {
        super(RapportSectionObligationReglementaire.class, "Eléments du modèle");
        editableProperty().set(true);
        detaillableProperty().set(false);
        fichableProperty().set(false);
        importPointProperty().set(false);
        commentAndPhotoProperty().set(false);
        searchVisibleProperty().set(false);
        exportVisibleProperty().set(false);
        ficheModeVisibleProperty().set(false);
        filterVisibleProperty().set(false);
        openEditorOnNewProperty().set(false);


        for(TableColumn col : getColumns()){
            if(col instanceof PropertyColumn && ((PropertyColumn)col).getReference()!=null){
                col.setEditable(true);
            }
        }
    }

    @Override
    protected Element createPojo() {
        final RapportSectionObligationReglementaire section = (RapportSectionObligationReglementaire) super.createPojo();
        section.setPhotoChoice(PhotoChoice.NONE);
        section.setType(SectionType.FORM);
        return section;
    }

}
