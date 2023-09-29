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

import com.sun.javafx.stage.StageHelper;
import static fr.sirs.core.SirsCore.*;
import static fr.sirs.core.SirsCore.DesordreFields.*;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.JRColumnParameter;
import fr.sirs.util.PrinterUtilities;
import fr.sirs.util.SirsStringConverter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.feature.Feature;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class PrintManager {

    static Node printButton;

    /**
     * Find first encountered printable element while browsing recursively given
     * nodes. Browsing is done "depth last".
     * @param nodes Nodes to find a printable object into.
     * @return First encountered printable, or null if we cannot find any.
     */
    private static Printable findPrintableChild(final Collection<Node> nodes) {
        if (nodes == null || nodes.isEmpty())
            return null;

        final List<Node> children = new ArrayList<>();
        for (final Node n : nodes) {
            if (n.isDisabled() || n.isMouseTransparent() || !n.isVisible()) {
                continue;
            }

            if (n instanceof Printable) {
                return (Printable) n;

            } else if (n instanceof Parent) {
                children.addAll(((Parent)n).getChildrenUnmodifiable());
            }
        }

        return findPrintableChild(children);
    }

    private static final ChangeListener<Scene> sceneListener = new ChangeListener<Scene>() {
        @Override
        public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
            if(oldValue!=null){
                oldValue.focusOwnerProperty().removeListener(focusOwnerListener);
            }
            if(newValue!=null){
                newValue.focusOwnerProperty().addListener(focusOwnerListener);
            }
        }
    };

    private static final ChangeListener<Node> focusOwnerListener = new ChangeListener<Node>() {
        @Override
        public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
            if (newValue == null) {
                printable.set(null);
                return;
            }

            // Do not update if user focused print button.
            if (printButton != null && newValue == printButton) {
                return;
            }

            // On regarde si un des enfants est imprimable.
            Printable p = null;
            if (newValue instanceof Printable) {
                p = (Printable) newValue;
            } else if (newValue instanceof Parent) {
                p = findPrintableChild(((Parent) newValue).getChildrenUnmodifiable());
            }

            // Si aucun enfant ne l'est on s'interesse aux parents du noeud selectionné
            if (p == null) {
                Node previous = newValue;
                Parent papa = newValue.getParent();
                while (p ==null && papa != null) {
                    if (papa instanceof Printable) {
                        p = (Printable) papa;
                    } else {
                        // Check other children of current parent.
                        final ObservableList<Node> otherChildren = FXCollections.observableArrayList(papa.getChildrenUnmodifiable());
                        otherChildren.remove(previous);
                        p = findPrintableChild(otherChildren);
                        previous = papa;
                        papa = papa.getParent();
                    }
                }
            }

            printable.set(p);
        }
    };

    private static Stage focusedStage = null;
    private static final ObjectProperty<Printable> printable = new SimpleObjectProperty<>();

    static {
        //on ecoute quel element a le focus pour savoir qui est imprimable
        StageHelper.getStages().addListener(new ListChangeListener<Stage>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Stage> c) {
                while(c.next()){
                    for(Stage s : c.getAddedSubList()){
                        s.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                            if(Boolean.TRUE.equals(newValue)) setFocusedStage(s);
                        });
                    }
                }
            }
        });
        for(Stage s : StageHelper.getStages()){
            if(s.isFocused()){
                setFocusedStage(s);
            }
        }
    }

    private static synchronized void setFocusedStage(Stage stage){
        if(focusedStage!=null){
            focusedStage.sceneProperty().removeListener(sceneListener);
            focusedStage.getScene().focusOwnerProperty().removeListener(focusOwnerListener);
        }
        focusedStage = stage;
        if(focusedStage!=null){
            focusedStage.sceneProperty().addListener(sceneListener);
            focusedStage.getScene().focusOwnerProperty().addListener(focusOwnerListener);
            focusOwnerListener.changed(null, null, focusedStage.getScene().getFocusOwner());
        }
    }

    /**
     * Récuperer l'element imprimable actif.
     *
     * @return Printable
     */
    public static ReadOnlyObjectProperty<Printable> printableProperty() {
        return printable;
    }

    public void printFocusedPrintable() throws Exception {
        final Printable tmpPrintable = PrintManager.printable.get();
        if(!tmpPrintable.print()){
            final Object candidate = tmpPrintable.getPrintableElements().get();
            print(candidate);
        }
    }

    public final void print(Object candidate) throws Exception {
        List<Element> elementsToPrint = null;
        FeatureCollection featuresToPrint = null;

        if(candidate instanceof Feature){
            featuresToPrint = FeatureStoreUtilities.collection((Feature)candidate);
        }else if(candidate instanceof Element){
            elementsToPrint = new ArrayList<>();
            elementsToPrint.add((Element)candidate);
        }else if(candidate instanceof FeatureCollection){
            featuresToPrint = (FeatureCollection) candidate;
        }else if(candidate instanceof List){
            elementsToPrint = (List)candidate;
        }

        if(elementsToPrint!=null){
            printElements(elementsToPrint);
        } else if(featuresToPrint!=null){
            printFeatures(featuresToPrint);
        }
    }

    private void printFeatures(FeatureCollection featuresToPrint) throws Exception {
            final List<String> avoidFields = new ArrayList<>();
            avoidFields.add(GEOMETRY_MODE_FIELD);
            final File fileToPrint = PrinterUtilities.print(avoidFields, featuresToPrint);
            SIRS.openFile(fileToPrint);
    }

    private void printElements(List<Element> elementsToPrint) throws Exception {
        final List<String> avoidFields = new ArrayList<>();
        avoidFields.add(GEOMETRY_FIELD);
        avoidFields.add(DOCUMENT_ID_FIELD);
        avoidFields.add(ID_FIELD);
        avoidFields.add(LONGITUDE_MIN_FIELD);
        avoidFields.add(LONGITUDE_MAX_FIELD);
        avoidFields.add(LATITUDE_MIN_FIELD);
        avoidFields.add(LATITUDE_MAX_FIELD);
        avoidFields.add(FOREIGN_PARENT_ID_FIELD);
        avoidFields.add(REVISION_FIELD);
        avoidFields.add(POSITION_DEBUT_FIELD);
        avoidFields.add(POSITION_FIN_FIELD);
        avoidFields.add(PARENT_FIELD);
        avoidFields.add(COUCH_DB_DOCUMENT_FIELD);
        avoidFields.add(GEOMETRY_MODE_FIELD);

        for(final Element element : elementsToPrint){
            if(element instanceof TronconDigue){
                if(!avoidFields.contains(BORNE_IDS_REFERENCE)) avoidFields.add(BORNE_IDS_REFERENCE);
            }
        }

        final File fileToPrint = PrinterUtilities.print(avoidFields, Injector.getSession().getPreviews(), new SirsStringConverter(), elementsToPrint);
        SIRS.openFile(fileToPrint);
    }

    /**
     * Génère un rapport PDF des désordres requis.
     *
     * @param desordres
     * @param printPhoto
     * @param printReseauOuvrage
     * @param printVoirie
     */
    public final void printDesordres(final List<Desordre> desordres,
                                     final boolean printPhoto,
                                     final boolean printReseauOuvrage,
                                     final boolean printVoirie,
                                     final boolean printLocationInsert,
                                     final boolean printPR,
                                     final boolean printXY,
                                     final boolean printBorne)
            throws Exception {

        final List<String> avoidDesordreFields = new ArrayList<>();
        avoidDesordreFields.add(GEOMETRY_FIELD);
        avoidDesordreFields.add(DOCUMENT_ID_FIELD);
        avoidDesordreFields.add(ID_FIELD);
        avoidDesordreFields.add(LONGITUDE_MIN_FIELD);
        avoidDesordreFields.add(LONGITUDE_MAX_FIELD);
        avoidDesordreFields.add(LATITUDE_MIN_FIELD);
        avoidDesordreFields.add(LATITUDE_MAX_FIELD);
        avoidDesordreFields.add(FOREIGN_PARENT_ID_FIELD);
        avoidDesordreFields.add(REVISION_FIELD);
        avoidDesordreFields.add(PARENT_FIELD);
        avoidDesordreFields.add(COUCH_DB_DOCUMENT_FIELD);
        avoidDesordreFields.add(OBSERVATIONS_REFERENCE);

        avoidDesordreFields.add(ECHELLE_LIMINIMETRIQUE_REFERENCE);
        avoidDesordreFields.add(OUVRAGE_PARTICULIER_REFERENCE);
        avoidDesordreFields.add(RESEAU_TELECOM_ENERGIE_REFERENCE);
        avoidDesordreFields.add(OUVRAGE_TELECOM_ENERGIE_REFERENCE);
        avoidDesordreFields.add(OUVRAGE_HYDRAULIQUE_REFERENCE);
        avoidDesordreFields.add(RESEAU_HYDRAULIQUE_FERME_REFERENCE);
        avoidDesordreFields.add(RESEAU_HYDRAULIQUE_CIEL_OUVERT_REFERENCE);
        avoidDesordreFields.add(OUVRAGE_VOIRIE_REFERENCE);
        avoidDesordreFields.add(VOIE_DIGUE_REFERENCE);
        avoidDesordreFields.add(PRESTATION_REFERENCE);

        avoidDesordreFields.add(VALID_FIELD);
        avoidDesordreFields.add(AUTHOR_FIELD);
        avoidDesordreFields.add(DATE_MAJ_FIELD);

        final List<JRColumnParameter> observationFields = new ArrayList<>();
        observationFields.add(new JRColumnParameter("date", .9f, true));
        observationFields.add(new JRColumnParameter("observateurId", .9f));
        observationFields.add(new JRColumnParameter("nombreDesordres",.7f));
        observationFields.add(new JRColumnParameter("urgenceId", .9f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationFields.add(new JRColumnParameter("evolution", 1.8f));
        observationFields.add(new JRColumnParameter("suiteApporterId", 1.2f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationFields.add(new JRColumnParameter("suite", 2.2f));

        final List<JRColumnParameter> prestationFields = new ArrayList<>();
        prestationFields.add(new JRColumnParameter("designation", .7f, true));
        prestationFields.add(new JRColumnParameter("libelle", 1.f));
        prestationFields.add(new JRColumnParameter("intervenantsIds", 1.f));
        prestationFields.add(new JRColumnParameter("date_debut", .7f));
        prestationFields.add(new JRColumnParameter("date_fin", .7f));
        prestationFields.add(new JRColumnParameter("commentaire", 2.5f));

        final List<JRColumnParameter> reseauFields = new ArrayList<>();
        reseauFields.add(new JRColumnParameter("designation"));
        reseauFields.add(new JRColumnParameter("libelle"));
        reseauFields.add(new JRColumnParameter("date_debut"));
        reseauFields.add(new JRColumnParameter("date_fin"));
        reseauFields.add(new JRColumnParameter("commentaire", 2.f));

        final File fileToPrint = PrinterUtilities.printDisorders(
                avoidDesordreFields,
                observationFields,
                prestationFields,
                reseauFields,
                Injector.getSession().getPreviews(),
                new SirsStringConverter(),
                desordres, printPhoto, printReseauOuvrage, printVoirie, printLocationInsert, printPR, printXY, printBorne);
        SIRS.openFile(fileToPrint);
    }

    /**
     * Génère un rapport PDF des réseaux hydrauliques fermés requis.
     *
     * @param reseauxFermes
     * @param printPhoto
     * @param printReseauOuvrage
     */
    public final void printReseaux(final List<ReseauHydrauliqueFerme> reseauxFermes,
                                   final boolean printPhoto,
                                   final boolean printReseauOuvrage,
                                   final boolean printLocationInsert,
                                   final boolean printPR,
                                   final boolean printXY,
                                   final boolean printBorne,
                                   boolean printObservationsSpec)
            throws Exception {

        final List<String> avoidReseauFields = new ArrayList<>();
        avoidReseauFields.add(GEOMETRY_FIELD);
        avoidReseauFields.add(DOCUMENT_ID_FIELD);
        avoidReseauFields.add(ID_FIELD);
        avoidReseauFields.add(LONGITUDE_MIN_FIELD);
        avoidReseauFields.add(LONGITUDE_MAX_FIELD);
        avoidReseauFields.add(LATITUDE_MIN_FIELD);
        avoidReseauFields.add(LATITUDE_MAX_FIELD);
        avoidReseauFields.add(FOREIGN_PARENT_ID_FIELD);
        avoidReseauFields.add(REVISION_FIELD);
        avoidReseauFields.add(PARENT_FIELD);
        avoidReseauFields.add(COUCH_DB_DOCUMENT_FIELD);
        avoidReseauFields.add(OBSERVATIONS_REFERENCE);

        avoidReseauFields.add(ECHELLE_LIMINIMETRIQUE_REFERENCE);
        avoidReseauFields.add(OUVRAGE_PARTICULIER_REFERENCE);
        avoidReseauFields.add(RESEAU_TELECOM_ENERGIE_REFERENCE);
        avoidReseauFields.add(OUVRAGE_TELECOM_ENERGIE_REFERENCE);
        avoidReseauFields.add(OUVRAGE_HYDRAULIQUE_REFERENCE);
        avoidReseauFields.add(RESEAU_HYDRAULIQUE_FERME_REFERENCE);
        avoidReseauFields.add(RESEAU_HYDRAULIQUE_CIEL_OUVERT_REFERENCE);
        avoidReseauFields.add(OUVRAGE_VOIRIE_REFERENCE);
        avoidReseauFields.add(VOIE_DIGUE_REFERENCE);
        avoidReseauFields.add(PRESTATION_REFERENCE);

        avoidReseauFields.add(VALID_FIELD);
        avoidReseauFields.add(AUTHOR_FIELD);
        avoidReseauFields.add(DATE_MAJ_FIELD);

        final List<JRColumnParameter> observationFields = new ArrayList<>();
        observationFields.add(new JRColumnParameter("date", 1.1f, true));
        observationFields.add(new JRColumnParameter("observateurId", .8f));
        observationFields.add(new JRColumnParameter("urgenceId", .9f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationFields.add(new JRColumnParameter("evolution", 2.f));
        observationFields.add(new JRColumnParameter("suiteApporterId", 1.2f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationFields.add(new JRColumnParameter("suite", 2.f));

        final List<JRColumnParameter> observationSpecFields = new ArrayList<>();
        observationSpecFields.add(new JRColumnParameter("date", 1.2f, true));
        observationSpecFields.add(new JRColumnParameter("etatOuvrageId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("etatOuvrageCom", 1.6f, "Commentaire"));
        observationSpecFields.add(new JRColumnParameter("etatAccessoireId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("etatAccessoireCom", 1.6f, "Commentaire"));
        observationSpecFields.add(new JRColumnParameter("etatGenieCivilId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("etatGenieCivilCom", 1.6f, "Commentaire"));
        observationSpecFields.add(new JRColumnParameter("manoeuvreOuvrageId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("manoeuvreOuvrageCom", 1.6f, "Commentaire"));

        final List<JRColumnParameter> reseauFields = new ArrayList<>();
        reseauFields.add(new JRColumnParameter("designation"));
        reseauFields.add(new JRColumnParameter("libelle"));
        reseauFields.add(new JRColumnParameter("date_debut"));
        reseauFields.add(new JRColumnParameter("date_fin"));
        reseauFields.add(new JRColumnParameter("commentaire", 2.f));

        final List<JRColumnParameter> desordreFields = new ArrayList<>();
        desordreFields.add(new JRColumnParameter("observationDate", true));
        desordreFields.add(new JRColumnParameter("observationDesignation", .7f));
        desordreFields.add(new JRColumnParameter("desordreDesignation", .7f));
        desordreFields.add(new JRColumnParameter("observationUrgence", .7f,  JRColumnParameter.DisplayPolicy.REFERENCE_CODE));
        desordreFields.add(new JRColumnParameter("desordreDescription", 2.5f));
        desordreFields.add(new JRColumnParameter("desordreEnded", .4f));

        final File fileToPrint = PrinterUtilities.printReseauFerme(avoidReseauFields,
                observationFields,
                observationSpecFields,
                reseauFields,
                desordreFields,
                Injector.getSession().getPreviews(),
                new SirsStringConverter(),
                reseauxFermes, printPhoto, printReseauOuvrage, printLocationInsert, printPR, printXY, printBorne, printObservationsSpec);
        SIRS.openFile(fileToPrint);
    }

    /**
     * Génère un rapport PDF des réseaux hydrauliques fermés requis.
     *
     * @param ouvrages
     * @param printPhoto
     * @param printReseauxFermes
     */
    public final void printOuvragesAssocies(final List<OuvrageHydrauliqueAssocie> ouvrages,
                                            final boolean printPhoto,
                                            final boolean printReseauxFermes,
                                            final boolean printLocationInsert,
                                            final boolean printPR,
                                            final boolean printXY,
                                            final boolean printBorne,
                                            boolean printObservationsSpec)
           throws Exception {

        final List<String> avoidReseauFields = new ArrayList<>();
        avoidReseauFields.add(GEOMETRY_FIELD);
        avoidReseauFields.add(DOCUMENT_ID_FIELD);
        avoidReseauFields.add(ID_FIELD);
        avoidReseauFields.add(LONGITUDE_MIN_FIELD);
        avoidReseauFields.add(LONGITUDE_MAX_FIELD);
        avoidReseauFields.add(LATITUDE_MIN_FIELD);
        avoidReseauFields.add(LATITUDE_MAX_FIELD);
        avoidReseauFields.add(FOREIGN_PARENT_ID_FIELD);
        avoidReseauFields.add(REVISION_FIELD);
        avoidReseauFields.add(PARENT_FIELD);
        avoidReseauFields.add(COUCH_DB_DOCUMENT_FIELD);
        avoidReseauFields.add(OBSERVATIONS_REFERENCE);

        avoidReseauFields.add(ECHELLE_LIMINIMETRIQUE_REFERENCE);
        avoidReseauFields.add(OUVRAGE_PARTICULIER_REFERENCE);
        avoidReseauFields.add(RESEAU_TELECOM_ENERGIE_REFERENCE);
        avoidReseauFields.add(OUVRAGE_TELECOM_ENERGIE_REFERENCE);
        avoidReseauFields.add(OUVRAGE_HYDRAULIQUE_REFERENCE);
        avoidReseauFields.add(RESEAU_HYDRAULIQUE_FERME_REFERENCE);
        avoidReseauFields.add(RESEAU_HYDRAULIQUE_CIEL_OUVERT_REFERENCE);
        avoidReseauFields.add(OUVRAGE_VOIRIE_REFERENCE);
        avoidReseauFields.add(VOIE_DIGUE_REFERENCE);
        avoidReseauFields.add(PRESTATION_REFERENCE);

        avoidReseauFields.add(VALID_FIELD);
        avoidReseauFields.add(AUTHOR_FIELD);
        avoidReseauFields.add(DATE_MAJ_FIELD);

        final List<JRColumnParameter> observationFields = new ArrayList<>();
        observationFields.add(new JRColumnParameter("date", 1.1f, true));
        observationFields.add(new JRColumnParameter("observateurId", .8f));
        observationFields.add(new JRColumnParameter("urgenceId", .9f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationFields.add(new JRColumnParameter("evolution", 2.f));
        observationFields.add(new JRColumnParameter("suiteApporterId", 1.2f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationFields.add(new JRColumnParameter("suite", 2.f));

        final List<JRColumnParameter> observationSpecFields = new ArrayList<>();
        observationSpecFields.add(new JRColumnParameter("date", 1.2f, true));
        observationSpecFields.add(new JRColumnParameter("etatOuvrageId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("etatOuvrageCom", 1.6f, "Commentaire"));
        observationSpecFields.add(new JRColumnParameter("etatAccessoireId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("etatAccessoireCom", 1.6f, "Commentaire"));
        observationSpecFields.add(new JRColumnParameter("etatGenieCivilId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("etatGenieCivilCom", 1.6f, "Commentaire"));
        observationSpecFields.add(new JRColumnParameter("manoeuvreOuvrageId", 1.1f, JRColumnParameter.DisplayPolicy.REFERENCE_LABEL_AND_CODE));
        observationSpecFields.add(new JRColumnParameter("manoeuvreOuvrageCom", 1.6f, "Commentaire"));

        final List<JRColumnParameter> reseauFields = new ArrayList<>();
        reseauFields.add(new JRColumnParameter("typeConduiteFermeeId", 2.3f, true));
        reseauFields.add(new JRColumnParameter("designation", .9f));
        reseauFields.add(new JRColumnParameter("libelle", .9f));
        reseauFields.add(new JRColumnParameter("date_debut"));
        reseauFields.add(new JRColumnParameter("date_fin"));
        reseauFields.add(new JRColumnParameter("commentaire", 2.6f));

        final List<JRColumnParameter> desordreFields = new ArrayList<>();
        desordreFields.add(new JRColumnParameter("observationDate", true));
        desordreFields.add(new JRColumnParameter("observationDesignation", .7f));
        desordreFields.add(new JRColumnParameter("desordreDesignation", .7f));
        desordreFields.add(new JRColumnParameter("observationUrgence", .7f,  JRColumnParameter.DisplayPolicy.REFERENCE_CODE));
        desordreFields.add(new JRColumnParameter("desordreDescription", 2.5f));
        desordreFields.add(new JRColumnParameter("desordreEnded", .4f));

        final File fileToPrint = PrinterUtilities.printOuvrageAssocie(avoidReseauFields,
                observationFields,
                observationSpecFields,
                reseauFields,
                desordreFields,
                Injector.getSession().getPreviews(),
                new SirsStringConverter(),
                ouvrages, printPhoto, printReseauxFermes, printLocationInsert, printPR, printXY, printBorne, printObservationsSpec);
        SIRS.openFile(fileToPrint);
    }
}