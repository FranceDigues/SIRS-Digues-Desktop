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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Description d'un plugin. Les informations requises sont le nom et la version
 * du plugin (séparée en deux variables : version majeure et mineure). Une
 * description peut également apparaître pour faciliter l'identification du
 * module.
 * Le plugin définit également les versions de l'application pour lesquelles il
 * sera fonctionnel.
 * Une URL de téléchargement peut (et c'est fortement recommandé) être donnée
 * pour spécifier où récupérer le plugin. Par défaut, une URL de téléchargement
 * est construite. Elle dénote un chemin sur le serveur de plugins courant. Elle
 * pointe sur le fichier suivant :
 *
 * urlServeur/${nomPlugin}-${versionMajeure}.${versionMineure}-plugin-package.zip
 *
 * @author Johann Sorel (Geomatys)
 */
@SuppressWarnings("serial")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginInfo {

    private static final String ASSEMBLY_SUFFIX = "-plugin-package";

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty downloadURL = new SimpleStringProperty();
    private final IntegerProperty versionMajor = new SimpleIntegerProperty(2);
    private final IntegerProperty versionMinor = new SimpleIntegerProperty(43);
    private final IntegerProperty appVersionMin = new SimpleIntegerProperty(0);
    private final IntegerProperty appVersionMax = new SimpleIntegerProperty(0);

    public PluginInfo() {
    }

    public StringProperty nameProperty() {
       return name;
    }

    public String getName() {
        return this.name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getTitle() {
        return this.title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty descriptionProperty() {
       return description;
    }

    public String getDescription(){
        return this.description.get();
    }

    public void setDescription(String desc){
        this.description.set(desc);
    }

    public IntegerProperty versionMajorProperty() {
       return versionMajor;
    }

    public int getVersionMajor(){
        return this.versionMajor.get();
    }

    public void setVersionMajor(int version){
        this.versionMajor.set(version);
    }

    public IntegerProperty versionMinorProperty() {
       return versionMinor;
    }

    public int getVersionMinor(){
        return this.versionMinor.get();
    }

    public void setVersionMinor(int version){
        this.versionMinor.set(version);
    }

    public IntegerProperty appVersionMinProperty() {
        return appVersionMin;
    }

    public int getAppVersionMin(){
        return this.appVersionMin.get();
    }

    public void setAppVersionMin(int version){
        this.appVersionMin.set(version);
    }

    public IntegerProperty appVersionMaxProperty() {
        return appVersionMax;
    }

    public int getAppVersionMax(){
        return this.appVersionMax.get();
    }

    public void setAppVersionMax(int version){
        this.appVersionMax.set(version);
    }

    public String getDownloadURL() {
        return downloadURL.get();
    }

    public void setDownloadURL(final String dlUrl) {
        downloadURL.setValue(dlUrl);
    }

    @JsonIgnore
    public boolean isOlderOrSame(PluginInfo info) {
        if (info == null || !name.equals(info.name)) return false;
        return getVersionMajor() < info.getVersionMajor() ||
                (getVersionMajor() == info.getVersionMajor() &&
                 getVersionMinor() < info.getVersionMinor());
    }

    @JsonIgnore
    public URL bundleURL(URL serverURL) throws MalformedURLException {
        String dlURL = downloadURL.get();
        if (dlURL == null || dlURL.isEmpty()) {
            String serverUrl = serverURL.toExternalForm().replaceFirst("(?i)([^/]+\\.json)$", "");
            dlURL = serverUrl +"/"+name.get()+"-"+getVersionMajor()+"."+getVersionMinor()+ASSEMBLY_SUFFIX+".zip";
        }
        return new URL(dlURL);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginInfo other = (PluginInfo) obj;
        return this.getName().equalsIgnoreCase(other.getName())
                && this.getVersionMajor() == other.getVersionMajor()
                && this.getVersionMinor() == other.getVersionMinor();

    }

    @Override
    public String toString() {
        return "Module " + name.get() + ", " + title.get() +
                ((description.get() == null || description.get().isEmpty())?
                "" : "\nInformations : " + description.get()) +
                "\nVersion : " + versionMajor.get() + "." + versionMinor.get() +
                "\nVersions de l'application compatibles : de 0."+ appVersionMin.get() +" à 0."+ appVersionMax.get() +
                ((downloadURL.get() == null || downloadURL.get().isEmpty())?
                "" : "\nTéléchargement : " + downloadURL.get());
    }


}
