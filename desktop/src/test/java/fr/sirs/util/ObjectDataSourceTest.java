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

import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.Photo;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Samuel Andrés (Geomatys) <samuel.andres at geomatys.com>
 */
public class ObjectDataSourceTest {

    @Test
    public void test_forceArialFontFace() {
        final String comment = "<html dir=\"ltr\">"
                + "<head></head>"
                + "<body contenteditable=\"true\">"
                + "<p><font face=\"Segoe UI\">Crue ponctuelle sur certains cours d'eau.</font></p>"
                + "<p><font face=\"Segoe UI\">Certains ouvrages ont pu être mis en charge ponctuellement</font></p>"
                + "</body>"
                + "</html>";
        final String result = ObjectDataSource.forceArialFontFace(comment);

        final String expected = "<html dir=\"ltr\">"
                + "<head></head>"
                + "<body contenteditable=\"true\">"
                + "<p><font face=\"Arial\">Crue ponctuelle sur certains cours d'eau.</font></p>"
                + "<p><font face=\"Arial\">Certains ouvrages ont pu être mis en charge ponctuellement</font></p>"
                + "</body>"
                + "</html>";

        Assert.assertEquals(expected, result);
    }

    @Test
    public void test_observationComparator() {
        final List<Observation> obs = new ArrayList<>();
        final Observation o01 = new Observation();
        o01.setDesignation("o01");
        o01.setDate(null);
        obs.add(o01);
        final Observation o1 = new Observation();
        o1.setDesignation("o1");
        o1.setDate(LocalDate.now());
        obs.add(o1);
        final Observation o02 = new Observation();
        o02.setDesignation("o02");
        o02.setDate(null);
        obs.add(o02);
        final Observation o2 = new Observation();
        o2.setDesignation("o2");
        o2.setDate(LocalDate.now().minusDays(1l));
        obs.add(o2);
        final Observation o3 = new Observation();
        o3.setDesignation("o3");
        o3.setDate(LocalDate.now().plusDays(1l));
        obs.add(o3);

        obs.sort(SirsComparator.OBSERVATION_COMPARATOR);

        assertTrue(obs.indexOf(o3) == 0);
        assertTrue(obs.indexOf(o1) == 1);
        assertTrue(obs.indexOf(o2) == 2);
        assertTrue(obs.indexOf(o01) == 3 || obs.indexOf(o01) == 4);
        assertTrue(obs.indexOf(o02) == 3 || obs.indexOf(o02) == 4);
    }

    @Test
    public void test_photoComparator() {
        final List<Photo> obs = new ArrayList<>();
        final Photo o01 = new Photo();
        o01.setDesignation("o01");
        o01.setDate(null);
        obs.add(o01);
        final Photo o1 = new Photo();
        o1.setDesignation("o1");
        o1.setDate(LocalDate.now());
        obs.add(o1);
        final Photo o02 = new Photo();
        o02.setDesignation("o02");
        o02.setDate(null);
        obs.add(o02);
        final Photo o2 = new Photo();
        o2.setDesignation("o2");
        o2.setDate(LocalDate.now().minusDays(1l));
        obs.add(o2);
        final Photo o3 = new Photo();
        o3.setDesignation("o3");
        o3.setDate(LocalDate.now().plusDays(1l));
        obs.add(o3);

        obs.sort(SirsComparator.PHOTO_COMPARATOR);

        assertTrue(obs.indexOf(o3) == 0);
        assertTrue(obs.indexOf(o1) == 1);
        assertTrue(obs.indexOf(o2) == 2);
        assertTrue(obs.indexOf(o01) == 3 || obs.indexOf(o01) == 4);
        assertTrue(obs.indexOf(o02) == 3 || obs.indexOf(o02) == 4);
    }

    @Test
    public void test_elementComparator() {
        final List<Desordre> des = new ArrayList<>();

        final Desordre d11 = new Desordre();
        d11.setLinearId("lin2");
        d11.setDesignation(null);
        des.add(d11);
        final Desordre d12 = new Desordre();
        d12.setLinearId(null);
        d12.setDesignation(null);
        des.add(d12);
        final Desordre d13 = new Desordre();
        d13.setLinearId(null);
        d13.setDesignation("d13");
        des.add(d13);

        final Desordre d3 = new Desordre();
        d3.setLinearId("lin1");
        d3.setDesignation("d3");
        des.add(d3);
        final Desordre d4 = new Desordre();
        d4.setLinearId("lin2");
        d4.setDesignation("d4");
        des.add(d4);
        final Desordre d6 = new Desordre();
        d6.setLinearId("lin2");
        d6.setDesignation("d6");
        des.add(d6);
        final Desordre d7 = new Desordre();
        d7.setLinearId("lin1");
        d7.setDesignation("d7");
        des.add(d7);
        final Desordre d8 = new Desordre();
        d8.setLinearId("lin3");
        d8.setDesignation("d8");
        des.add(d8);
        final Desordre d9 = new Desordre();
        d9.setLinearId("lin1");
        d9.setDesignation("d9");
        des.add(d9);
        final Desordre d5 = new Desordre();
        d5.setLinearId("lin3");
        d5.setDesignation("d5");
        des.add(d5);
        final Desordre d1 = new Desordre();
        d1.setLinearId("lin3");
        d1.setDesignation("d1");
        des.add(d1);
        final Desordre d2 = new Desordre();
        d2.setLinearId("lin3");
        d2.setDesignation("d2");
        des.add(d2);
        final Desordre d10 = new Desordre();
        d10.setLinearId("lin3");
        d10.setDesignation("d10");
        des.add(d10);

        des.sort(SirsComparator.ELEMENT_COMPARATOR);

        assertTrue(des.indexOf(d1) == 0);
        assertTrue(des.indexOf(d10) == 1);
        assertTrue(des.indexOf(d13) == 2);
        assertTrue(des.indexOf(d2) == 3);
        assertTrue(des.indexOf(d3) == 4);
        assertTrue(des.indexOf(d4) == 5);
        assertTrue(des.indexOf(d5) == 6);
        assertTrue(des.indexOf(d6) == 7);
        assertTrue(des.indexOf(d7) == 8);

        assertTrue(des.indexOf(d8) == 9);
        assertTrue(des.indexOf(d9) == 10);
        assertTrue(des.indexOf(d11) == 11 || des.indexOf(d11) == 12);
        assertTrue(des.indexOf(d12) == 11 || des.indexOf(d12) == 12);
    }

    @Test
    public void Test_lastObservation() {

        //---------------
        //Initialisation
        //---------------
        //Initialisation des observations du test
        final Observation obs01_dateNull_evolNull = new Observation();
        obs01_dateNull_evolNull.setDesignation(null);
        obs01_dateNull_evolNull.setDate(null);
        obs01_dateNull_evolNull.setEvolution(null);

        final Observation obs1_dateNow = new Observation();
        obs1_dateNow.setDesignation("o1");
        obs1_dateNow.setDate(LocalDate.now());
        obs1_dateNow.setEvolution("Evolution obs1_dateNow");

        final Observation obs02_dateNull = new Observation();
        obs02_dateNull.setDesignation("o02");
        obs02_dateNull.setDate(null);
        obs02_dateNull.setEvolution("Evolution obs02_dateNull");

        final Observation obs02_dateNotNull_evolNull = new Observation();
        obs02_dateNotNull_evolNull.setDesignation("o02");
        obs02_dateNotNull_evolNull.setDate(LocalDate.now());
        obs02_dateNotNull_evolNull.setEvolution(null);

        final Observation obs2_dateOldest = new Observation();
        obs2_dateOldest.setDesignation("o2");
        obs2_dateOldest.setDate(LocalDate.now().minusDays(1l));
        obs2_dateOldest.setEvolution("Evolution obs2_dateOldest");

        final Observation obs3_dateNewest = new Observation();
        obs3_dateNewest.setDesignation("o3");
        obs3_dateNewest.setDate(LocalDate.now().plusDays(1l));
        obs3_dateNewest.setEvolution("Evolution obs3_dateNewest");

        //Initialisation des ObservableListes du test:
        //    Liste d'entrée vide
        final ObservableList<Observation> listObsNull = null;
        final ObservableList<Observation> listObsVide = FXCollections.observableArrayList();

        //    Liste avec 3 ou 4 éléments, avec au moins un avec une date null (pour tester le tri).
        final ObservableList<Observation> listObs_4elts_1dateNull = FXCollections.observableArrayList();
        listObs_4elts_1dateNull.add(obs1_dateNow);
        listObs_4elts_1dateNull.add(obs2_dateOldest);
        listObs_4elts_1dateNull.add(obs02_dateNull);
        listObs_4elts_1dateNull.add(obs3_dateNewest);

        //    Liste avec des éléments dont toutes les dates sont nulles (pour tester la construction de message sans date)
        //    Note  : le tri des observations ne prend pas en compte les évolutions.
        final ObservableList<Observation> listObs_allDatesNull_1erEvolNull = FXCollections.observableArrayList();
        listObs_allDatesNull_1erEvolNull.add(obs01_dateNull_evolNull);
        listObs_allDatesNull_1erEvolNull.add(obs02_dateNull);

        final ObservableList<Observation> listObs_allDatesNull_1erEvolNotNull = FXCollections.observableArrayList();
        listObs_allDatesNull_1erEvolNotNull.add(obs02_dateNull);
        listObs_allDatesNull_1erEvolNotNull.add(obs01_dateNull_evolNull);

        //    Liste avec un seul élément, date présente mais évolution nulle.
        final ObservableList<Observation> list_1Obs_DatesNotNull_EvolNull = FXCollections.observableArrayList();
        list_1Obs_DatesNotNull_EvolNull.add(obs02_dateNotNull_evolNull);

        //    Liste avec un seul élément dont tous les champs sont nuls.
        final ObservableList<Observation> list_1Obs_DatesNull_EvolNull = FXCollections.observableArrayList();
        list_1Obs_DatesNull_EvolNull.add(obs01_dateNull_evolNull);

        //-------
        //Tests :
        //-------
        assertTrue(ObjectDataSource.lastObservation(listObsNull).equals("Ni commentaire ni observation.")); //    Liste d'entrée vide
        assertTrue(ObjectDataSource.lastObservation(listObsVide).equals("Ni commentaire ni observation."));

        //    Liste avec 3 ou 4 éléments, avec au moins un avec une date null (pour tester le tri).
        assertTrue(ObjectDataSource.lastObservation(listObs_4elts_1dateNull).equals(
                "Observation du " + obs3_dateNewest.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " : " + obs3_dateNewest.getEvolution()));

        //    Liste avec des éléments dont toutes les dates sont nulles (pour tester la construction de message sans date)
        //    Note  : le tri des observations ne prend pas en compte les évolutions.
        assertTrue(ObjectDataSource.lastObservation(listObs_allDatesNull_1erEvolNull).equals(
                "Ni commentaire ni observation."));
        assertTrue(ObjectDataSource.lastObservation(listObs_allDatesNull_1erEvolNotNull).equals(
                "Observation : " + obs02_dateNull.getEvolution()));

        //    Liste avec un seul élément, date présente mais évolution nulle.
        assertTrue(ObjectDataSource.lastObservation(list_1Obs_DatesNotNull_EvolNull).equals(
                "Observation du " + obs02_dateNotNull_evolNull.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " : pas d'évolution renseignée."));

        //    Liste avec un seul élément dont tous les champs sont nuls.
        assertTrue(ObjectDataSource.lastObservation(list_1Obs_DatesNull_EvolNull).equals("Ni commentaire ni observation."));

    }

}
