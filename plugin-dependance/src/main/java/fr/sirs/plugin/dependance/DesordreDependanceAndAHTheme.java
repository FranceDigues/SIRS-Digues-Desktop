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
import fr.sirs.plugin.dependance.ui.AbstractDescriptionPane;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import fr.sirs.core.component.DesordreDependanceRepository;
import fr.sirs.core.model.DesordreDependance;
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
 * Elément d'ui pour générer le thème des désordres dépendances et AH.
 *
 * @author Gavens Maxime (Geomatys)
 */
public abstract class DesordreDependanceAndAHTheme extends AbstractPluginsButtonTheme {

    public DesordreDependanceAndAHTheme(String name, String description) {
        super(name, description, null);
    }

    @Override
    public Parent createPane() {
        final BorderPane borderPane = new AbstractDescriptionPane();
        return borderPane;
    }

    public static AbstractTheme.ThemeManager<DesordreDependance> generateThemeManager(String tabTitle){
        final ResourceBundle bundle = ResourceBundle.getBundle(DesordreDependance.class.getCanonicalName(), Locale.getDefault(), Thread.currentThread().getContextClassLoader());

        final Function<String, ObservableList<DesordreDependance>> extractor = (String id) -> {
            final List<DesordreDependance> result = ((DesordreDependanceRepository) Injector.getSession().getRepositoryForClass(DesordreDependance.class)).getByDependanceOrAHId(id);
            return FXCollections.observableList(result);
        };

        final Consumer<DesordreDependance> deletor = (DesordreDependance themeElement) -> {
            Injector.getSession().getRepositoryForClass(DesordreDependance.class).remove(themeElement);
        };

        return new AbstractTheme.ThemeManager<DesordreDependance>(bundle.getString(BUNDLE_KEY_CLASS), tabTitle, DesordreDependance.class, extractor, deletor);
    }
}
