/*
 * This file is part of SIRS-Digues 2.
 *
 *  Copyright (C) 2021, FRANCE-DIGUES,
 *
 *  SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any
 *  later version.
 *
 *  SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.dependance;

import fr.sirs.Injector;
import static fr.sirs.core.SirsCore.BUNDLE_KEY_CLASS;
import fr.sirs.core.model.AbstractAmenagementHydraulique;
import fr.sirs.plugin.dependance.ui.AbstractDescriptionPane;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import fr.sirs.core.component.AbstractAmenagementHydrauliqueRepository;
import fr.sirs.theme.AbstractTheme;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 * Couche d'abstraction des descriptions d'aménagement hydraulique
 *
 * @author Gavens Maxime (Geomatys)
 */
public abstract class AbstractDescriptionTheme extends AbstractPluginsButtonTheme {

    public AbstractDescriptionTheme(String name, String description) {
        super(name, description, null);
    }

    @Override
    public Parent createPane() {
        final BorderPane borderPane = new AbstractDescriptionPane();
        return borderPane;
    }

    public static AbstractTheme.ThemeManager<AbstractAmenagementHydraulique> generateThemeManager(String tabTitle, final Class themeClass){
        final ResourceBundle bundle = ResourceBundle.getBundle(themeClass.getCanonicalName(), Locale.getDefault(), Thread.currentThread().getContextClassLoader());

        final Function<String, ObservableList<AbstractAmenagementHydraulique>> extractor = (String ahId) -> {
            final List<AbstractAmenagementHydraulique> result = ((AbstractAmenagementHydrauliqueRepository) Injector.getSession().getRepositoryForClass(themeClass)).getByAmenagementHydrauliqueId(ahId);
            return FXCollections.observableList(result);
        };

        final Consumer<AbstractAmenagementHydraulique> deletor = (AbstractAmenagementHydraulique themeElement) -> {
            Injector.getSession().getRepositoryForClass(themeClass).remove(themeElement);
        };

        return new AbstractTheme.ThemeManager<>(bundle.getString(BUNDLE_KEY_CLASS), tabTitle, themeClass, extractor, deletor);
    }
}
