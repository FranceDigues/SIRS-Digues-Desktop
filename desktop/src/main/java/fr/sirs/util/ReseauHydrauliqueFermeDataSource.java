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
package fr.sirs.util;

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import static fr.sirs.core.SirsCore.DIGUE_ID_FIELD;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.ObjetReseau;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.ObservationReseauHydrauliqueFerme;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.Photo;
import fr.sirs.core.model.RefSecurite;
import fr.sirs.core.model.ReseauHydrauliqueCielOuvert;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.StationPompage;
import fr.sirs.core.model.TronconDigue;
import static fr.sirs.util.JRDomWriterDesordreSheet.IMAGE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterReseauFermeSheet.DESORDRE_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterReseauFermeSheet.OBSERVATION_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterReseauFermeSheet.OBSERVATION_SPEC_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterReseauFermeSheet.PHOTO_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterReseauFermeSheet.RESEAU_OUVRAGE_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterReseauFermeSheet.SECURITE_ID_FIELD;
import java.awt.Dimension;
import java.awt.Image;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * Source de données de remplissage des fiches de réseaux hydrauliques fermés.
 *
 * @author Samuel Andrés (Geomatys)
 */
public class ReseauHydrauliqueFermeDataSource extends ObjectDataSource<ReseauHydrauliqueFerme> {

    private static Logger LOGGER = Logger.getLogger(ReseauHydrauliqueFermeDataSource.class.getName());

    private final boolean printLocationInsert;

    /**
     * Groupe par désignation de désordre et classe par date décroissante à l'intérieur de chaque groupe.
     * Inutilisé (était utilisé pour ordonner les lignes du tableau des observations/désordres).
     */
    static final Comparator<JRDesordreTableRow> DESORDRE_RESEAU_COMPARATOR = (JRDesordreTableRow d1, JRDesordreTableRow d2)->{
        final LocalDate date1 = d1.getObservationDate();
        final String des1 = d1.getDesordreDesignation();
        final LocalDate date2 = d2.getObservationDate();
        final String des2 = d2.getDesordreDesignation();
        if(des1==null && des2==null) return 0;
        else if(des1==null || des2==null) return (des1==null) ? -2 : 2; // On regroupe par désignation de désordre.
        else {
            final int compareTo = des1.compareTo(des2);
            if (compareTo!=0) return compareTo;
            else if(date1==null && date2==null) return 0;
            else if(date1==null || date2==null) return (date1==null) ? 1 : -1;
            else return -date1.compareTo(date2); // Par date décroissante : la plus récente en tête.
        }
    };

    public ReseauHydrauliqueFermeDataSource(Iterable<ReseauHydrauliqueFerme> iterable, final boolean printLocationInsert) {
        super(iterable);
        this.printLocationInsert = printLocationInsert;
    }

    public ReseauHydrauliqueFermeDataSource(final Iterable<ReseauHydrauliqueFerme> iterable, final Previews previewLabelRepository, final boolean printLocationInsert){
        super(iterable, previewLabelRepository);
        this.printLocationInsert = printLocationInsert;
    }

    public ReseauHydrauliqueFermeDataSource(final Iterable<ReseauHydrauliqueFerme> iterable, final Previews previewLabelRepository, final SirsStringConverter stringConverter, final boolean printLocationInsert){
        super(iterable, previewLabelRepository, stringConverter);
        this.printLocationInsert = printLocationInsert;
    }

    @Override
    public Object getFieldValue(final JRField jrf) throws JRException {

        final String name = jrf.getName();

        if(DIGUE_ID_FIELD.equals(name)){
            if(currentObject!=null&&currentObject.getLinearId()!=null){
                final TronconDigue troncon = Injector.getSession().getRepositoryForClass(TronconDigue.class).get(currentObject.getLinearId());
                if(troncon!=null&&troncon.getDigueId()!=null){
                    return parsePropertyValue(troncon.getDigueId(), Digue.class, String.class);
                }
            }
            return null;
        }
        else if(PHOTO_DATA_SOURCE.equals(name)){
            final List<Photo> photos = new ArrayList<>();
            for(final ObservationReseauHydrauliqueFerme observation : currentObject.getObservations()){
                if(observation.getPhotos()!=null && !observation.getPhotos().isEmpty()){
                    photos.addAll(observation.getPhotos());
                }
            }
            if(currentObject.getPhotos()!=null && !currentObject.getPhotos().isEmpty()){
                photos.addAll(currentObject.getPhotos());
            }
            photos.sort(SirsComparator.PHOTO_COMPARATOR);
            return new ObjectDataSource<>(photos, previewRepository, stringConverter);
        }
        else if(OBSERVATION_TABLE_DATA_SOURCE.equals(name) || OBSERVATION_SPEC_TABLE_DATA_SOURCE.equals(name)){
            final ObservableList<ObservationReseauHydrauliqueFerme> observations = currentObject.getObservations();
            observations.sort(SirsComparator.OBSERVATION_COMPARATOR);
            return new ObjectDataSource<>(observations, previewRepository, stringConverter);
        }
        else if(RESEAU_OUVRAGE_TABLE_DATA_SOURCE.equals(name)){
            
            final List<List<? extends ObjetReseau>> retrievedLists = new ArrayList();
            retrievedLists.add(Injector.getSession().getRepositoryForClass(OuvrageHydrauliqueAssocie.class).get(currentObject.getOuvrageHydrauliqueAssocieIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(ReseauHydrauliqueCielOuvert.class).get(currentObject.getReseauHydrauliqueCielOuvertIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(StationPompage.class).get(currentObject.getStationPompageIds()));

            final List<ObjetReseau> reseauOuvrageList = new ArrayList<>();
            for(final List<? extends ObjetReseau> candidate : retrievedLists){
                if(candidate!=null && !candidate.isEmpty()){
                    reseauOuvrageList.addAll(candidate);
                }
            }
            
            reseauOuvrageList.sort(SirsComparator.ELEMENT_COMPARATOR);
            return new ObjectDataSource<>(reseauOuvrageList, previewRepository, stringConverter);
        }
        else if(DESORDRE_TABLE_DATA_SOURCE.equals(name)){
            final List<JRDesordreTableRow> desordreRows = new ArrayList<>();
            for(final Desordre des : Injector.getSession().getRepositoryForClass(Desordre.class).getAll()){
                if(des.getReseauHydrauliqueFermeIds().contains(currentObject.getId())){
                    final List<Observation> observations = des.getObservations();
                    for(final Observation obs : observations){
                        desordreRows.add(new JRDesordreTableRow(obs.getDate(), des.getDesignation(), obs.getDesignation(), obs.getUrgenceId(), des.getCommentaire(), des.getDate_fin()!=null));
                    }
                }
            }
            Collections.sort(desordreRows);
            return new ObjectDataSource<>(desordreRows, previewRepository, stringConverter);
        } else if (IMAGE_DATA_SOURCE.equals(name)) {
            if (this.printLocationInsert) {
                final Image img = CorePlugin.takePictureOfElement(currentObject, new Dimension(1750, 1080));
                if (img != null) {
                    return img;
                } else {
                    return noImage();
                }
            } else {
                return null;
            }
        } else if (SECURITE_ID_FIELD.equals(name)) {
            if(currentObject != null && currentObject.getSecuriteId() != null){
                return parsePropertyValue(currentObject.getSecuriteId(), RefSecurite.class, String.class);
            }
            return null;
        }
        else return super.getFieldValue(jrf);
    }
}
