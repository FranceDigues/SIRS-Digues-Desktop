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
import fr.sirs.core.model.EchelleLimnimetrique;
import fr.sirs.core.model.ObjetReseau;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.OuvrageParticulier;
import fr.sirs.core.model.OuvrageTelecomEnergie;
import fr.sirs.core.model.OuvrageVoirie;
import fr.sirs.core.model.Photo;
import fr.sirs.core.model.Prestation;
import fr.sirs.core.model.ReseauHydrauliqueCielOuvert;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.ReseauTelecomEnergie;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.model.VoieDigue;
import static fr.sirs.util.JRDomWriterDesordreSheet.OBSERVATION_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterDesordreSheet.PHOTO_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterDesordreSheet.PRESTATION_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterDesordreSheet.RESEAU_OUVRAGE_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterDesordreSheet.VOIRIE_TABLE_DATA_SOURCE;
import static fr.sirs.util.JRDomWriterDesordreSheet.IMAGE_DATA_SOURCE;
import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * Source de données de remplissage des fiches de désordre.
 * 
 * @author Samuel Andrés (Geomatys)
 */
public class DesordreDataSource extends ObjectDataSource<Desordre> {

    private static Logger LOGGER = Logger.getLogger(DesordreDataSource.class.getName());

    private final boolean printLocationInsert;

    public DesordreDataSource(Iterable<Desordre> iterable, final boolean printLocationInsert) {
        super(iterable);
        this.printLocationInsert = printLocationInsert;
    }

    public DesordreDataSource(final Iterable<Desordre> iterable, final Previews previewLabelRepository, final boolean printLocationInsert){
        super(iterable, previewLabelRepository);
        this.printLocationInsert = printLocationInsert;
    }

    public DesordreDataSource(final Iterable<Desordre> iterable, final Previews previewLabelRepository, final SirsStringConverter stringConverter, final boolean printLocationInsert){
        super(iterable, previewLabelRepository, stringConverter);
        this.printLocationInsert = printLocationInsert;
    }

    @Override
    public Object getFieldValue(final JRField jrf) throws JRException {

        final String name = jrf.getName();

        if(DIGUE_ID_FIELD.equals(name)){
            if(currentObject!=null&&currentObject.getLinearId()!=null){
                try{
                    final TronconDigue troncon = Injector.getSession().getRepositoryForClass(TronconDigue.class).get(currentObject.getLinearId());
                    if(troncon!=null&&troncon.getDigueId()!=null){
                        return parsePropertyValue(troncon.getDigueId(), Digue.class, String.class);
                    }
                } catch (IllegalArgumentException e){
                    // SYM-1735 : problème d'impression des fiches de désordres attachés à des berges lorsque le module berges n'est pas chargé
                    LOGGER.log(Level.INFO, "un problème a été rencontré lors de l'extraction de la digue d'une fiche", e);
                }
            }
            return null;
        }
        else if(PHOTO_DATA_SOURCE.equals(name)){
            final List<Photo> photos = new ArrayList<>();
            for(final Observation observation : currentObject.observations){
                if(observation.photos!=null && !observation.photos.isEmpty()){
                    photos.addAll(observation.photos);
                }
            }
            photos.sort(SirsComparator.PHOTO_COMPARATOR);
            return new ObjectDataSource<>(photos, previewRepository, stringConverter);
        }
        else if(OBSERVATION_TABLE_DATA_SOURCE.equals(name)){
            final ObservableList<Observation> observations = currentObject.getObservations();
            observations.sort(SirsComparator.OBSERVATION_COMPARATOR);
            return new ObjectDataSource<>(observations, previewRepository, stringConverter);
        }
        else if(PRESTATION_TABLE_DATA_SOURCE.equals(name)){
            final List<Prestation> prestationList = Injector.getSession().getRepositoryForClass(Prestation.class).get(currentObject.getPrestationIds());
            prestationList.sort(SirsComparator.ELEMENT_COMPARATOR);
            return new ObjectDataSource<>(prestationList, previewRepository, stringConverter);
        }
        else if(RESEAU_OUVRAGE_TABLE_DATA_SOURCE.equals(name)){

            final List<List<? extends ObjetReseau>> retrievedLists = new ArrayList();
            retrievedLists.add(Injector.getSession().getRepositoryForClass(EchelleLimnimetrique.class).get(currentObject.getEchelleLimnimetriqueIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(OuvrageParticulier.class).get(currentObject.getOuvrageParticulierIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(ReseauTelecomEnergie.class).get(currentObject.getReseauTelecomEnergieIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(OuvrageTelecomEnergie.class).get(currentObject.getOuvrageTelecomEnergieIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(OuvrageHydrauliqueAssocie.class).get(currentObject.getOuvrageHydrauliqueAssocieIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(ReseauHydrauliqueCielOuvert.class).get(currentObject.getReseauHydrauliqueCielOuvertIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(ReseauHydrauliqueFerme.class).get(currentObject.getReseauHydrauliqueFermeIds()));

            final List<ObjetReseau> reseauOuvrageList = new ArrayList<>();
            for(final List<? extends ObjetReseau> candidate : retrievedLists){
                if(candidate!=null && !candidate.isEmpty()){
                    reseauOuvrageList.addAll(candidate);
                }
            }

            reseauOuvrageList.sort(SirsComparator.ELEMENT_COMPARATOR);
            return new ObjectDataSource<>(reseauOuvrageList, previewRepository, stringConverter);
        }
        else if(VOIRIE_TABLE_DATA_SOURCE.equals(name)){

            final List<List<? extends ObjetReseau>> retrievedLists = new ArrayList();
            retrievedLists.add(Injector.getSession().getRepositoryForClass(OuvrageVoirie.class).get(currentObject.getOuvrageVoirieIds()));
            retrievedLists.add(Injector.getSession().getRepositoryForClass(VoieDigue.class).get(currentObject.getVoieDigueIds()));

            final List<ObjetReseau> voirieList = new ArrayList<>();
            for(final List candidate : retrievedLists){
                if(candidate!=null && !candidate.isEmpty()){
                    voirieList.addAll(candidate);
                }
            }

            voirieList.sort(SirsComparator.ELEMENT_COMPARATOR);
            return new ObjectDataSource<>(voirieList, previewRepository, stringConverter);
        } else if (IMAGE_DATA_SOURCE.equals(name)) {
            if (this.printLocationInsert) {
                final Image img = CorePlugin.takePictureOfElement(currentObject, new Dimension(1750, 1200));
                if (img != null) {
                    return img;
                } else {
                    return noImage();
                }
            } else {
                return null;
            }
        }
        else return super.getFieldValue(jrf);
    }
}
