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
package fr.sirs.theme;

import fr.sirs.Injector;
import fr.sirs.Session;
import fr.sirs.core.model.Contact;
import fr.sirs.core.model.Organisme;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SimpleFXEditMode;
import javafx.scene.Parent;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ContactsTheme extends Theme {

    public ContactsTheme() {
        super("Organismes et personnes physiques", Type.UNLOCALIZED);
    }

    @Override
    public Parent createPane() {
        final BorderPane uiCenter = new BorderPane();
        final TabPane tabPane = new TabPane();

        final Session session = Injector.getSession();
        final PojoTable tableContact = new PojoTable(session.getRepositoryForClass(Contact.class),"Personnes physiques", null);
        final PojoTable tableOrganisme = new PojoTable(session.getRepositoryForClass(Organisme.class),"Organismes", null);

        // HACK - REDMINE 8104 :
        // Tabs should not be closeable to avoid loosing them if closed.
        final Tab tabIntervenant = new Tab("Personnes physiques");
        tabIntervenant.setContent(createTabContent(tableContact));
        tabIntervenant.setClosable(false);
        final Tab tabOrganisme = new Tab("Organismes");
        tabOrganisme.setContent(createTabContent(tableOrganisme));
        tabOrganisme.setClosable(false);

        tabPane.getTabs().add(tabIntervenant);
        tabPane.getTabs().add(tabOrganisme);

        uiCenter.setCenter(tabPane);
        return tabPane;
    }

    public Parent createTabContent(final PojoTable table) {
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        table.editableProperty().bind(editMode.editionState());

        return new BorderPane(table, topPane, null, null, null);
    }
}
