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
package fr.sirs.theme;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import org.apache.sis.util.ArgumentChecks;

/**
 * Un thème permet de gerer un ensemble d'objet du meme type.
 * Un thème peut être sous divisé en sous-thème.
 * 
 * @author Johann Sorel
 */
public abstract class Theme {
    
    /**
     * Les thèmes sont classés en trois catégories (Type) :
     * - LOCALIZED : pour les thèmes d'objets rattachés aux tronçons et qui ont une représentation graphique.
     * - UNLOCALIZED : pour les thèmes sans relations ni représentation graphique.
     * - PLUGINS : regroupe les plugins additionnels chargés par l'utilisateur pour son application.
     */
    public static enum Type{
        LOCALIZED,
        UNLOCALIZED,
        PLUGINS
    }
    
    private final StringProperty name = new SimpleStringProperty("");
    private final Type type;
    private final List<Theme> subThemes = new ArrayList<>();

    /**
     * Constructeur.
     * 
     * @param name nom du thème
     * @param type type de thème
     */
    public Theme(String name, Type type) {
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("type", type);
        this.name.set(name);
        this.type = type;
    }
    
    /**
     * 
     * @return String, jamais nulle
     */
    public String getName(){
        return name.get();
    }

    /**
     * Récupérer le type du thème.
     * 
     * @return Type, jamais nulle.
     */
    public Type getType() {
        return type;
    }

    /**
     * Récupérer la liste des sous-thèmes.
     * 
     * @return Liste des sous-thème. jamais nulle
     */
    public List<Theme> getSubThemes() {
        return subThemes;
    }
        
    /**
     * Création d'un panneau d'interface utilisateur permettant la consultation
     * et l'édition des objets de ce thème.
     * 
     * @return Composant d'interface, jamais nulle.
     */
    public abstract Parent createPane();

    /**
     * Indicates if the theme pane must be cached or reloaded.
     * 
     * @return true if the pane must be cached.
     */
    public boolean isCached(){return true;}
    
    
    /**
     * Un listener sur la selection du theme. Ne fais rien par default, doit etre surchargé par les sous classes.
     * @return 
     */
    public ChangeListener<Boolean> getSelectedPropertyListener() {
        return (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            // ne fais rien par defaut.
        };
    }

    /**
     * Method to be implemented if action required when closing the Theme tab.
     * No called by default.
     * Modify method Session.getOrCreateThemeTab(Theme) to call this method when closing the tab for a specific Theme.
     */
    public void resetContent() {}

    /**
     * Method to be implemented if required to refresh the theme content.
     * Modify method Session.getOrCreateThemeTab(Theme) to call this method when selecting/unselecting the theme tab for a specific Theme.
     */
    public void refresh() {}
}
