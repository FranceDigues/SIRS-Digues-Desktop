/**
 *
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

package fr.sirs.migration.upgrade.v2and23;

import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.EchelleLimnimetrique;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.OuvrageParticulier;
import fr.sirs.core.model.OuvrageTelecomEnergie;
import fr.sirs.core.model.OuvrageVoirie;
import fr.sirs.core.model.ReseauHydrauliqueCielOuvert;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.ReseauTelecomEnergie;
import fr.sirs.core.model.VoieDigue;
import static fr.sirs.migration.upgrade.v2and23.Upgrades1NtoNNSupported.DESORDRE;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test de l'énum utilisé pour mettre à jour des relations 1-N en relation N-N
 * dans le modèle ecore.
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class Upgrades1NtoNNSupportedTest {

//    DESORDRE(2, 23, Desordre.class,  Arrays.asList(
//                                        new ClassAndItsGetter(VoieDigue.class, Desordre.class),
//                                        new ClassAndItsGetter(OuvrageVoirie.class, Desordre.class),
//                                        new ClassAndItsGetter(ReseauHydrauliqueFerme.class, Desordre.class),
//                                        new ClassAndItsGetter(OuvrageHydrauliqueAssocie.class, Desordre.class),
//                                        new ClassAndItsGetter(ReseauTelecomEnergie.class, Desordre.class),
//                                        new ClassAndItsGetter(OuvrageTelecomEnergie.class, Desordre.class),
//                                        new ClassAndItsGetter(ReseauHydrauliqueCielOuvert.class, Desordre.class),
//                                        new ClassAndItsGetter(OuvrageParticulier.class, Desordre.class),
//                                        new ClassAndItsGetter(EchelleLimnimetrique.class, Desordre.class)
//    ));

    /**
     * Test as all getters are found for upgrade from an X.XX version to 2.23 version of SIRS application
     * where X.XX {@literal <} 2.23.
     */
    @Test
    public void VerifyMethod() {

        boolean voieDigue = false, ouvrageVoirie = false,reseauHydrauliqueFerme =false,
                ouvrageHydrauliqueAssocie = false, reseauTelecomEnergie = false, ouvrageTelecomEnergie =false,
                reseauHydrauliqueCielOuvert = false, ouvrageParticulier = false, echelleLimnimetrique =false;

        String nameMethod;

        for (ClassAndItsGetter classeAndGetter : DESORDRE.linkSidesN) {
            Assert.assertTrue(classeAndGetter.fromClass == Desordre.class);

            if (classeAndGetter.clazz ==  VoieDigue.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getVoieDigueIds"));
                voieDigue = true;

            } else if (classeAndGetter.clazz ==  OuvrageVoirie.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getOuvrageVoirieIds"));
                ouvrageVoirie= true;

            } else if (classeAndGetter.clazz ==  ReseauHydrauliqueFerme.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getReseauHydrauliqueFermeIds"));
                reseauHydrauliqueFerme = true;

            } else if (classeAndGetter.clazz ==  OuvrageHydrauliqueAssocie.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getOuvrageHydrauliqueAssocieIds"));
                ouvrageHydrauliqueAssocie = true;

            } else if (classeAndGetter.clazz ==  ReseauTelecomEnergie.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getReseauTelecomEnergieIds"));
                reseauTelecomEnergie= true;

            } else if (classeAndGetter.clazz ==  OuvrageTelecomEnergie.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getOuvrageTelecomEnergieIds"));
                ouvrageTelecomEnergie = true;

            } else if (classeAndGetter.clazz ==  ReseauHydrauliqueCielOuvert.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getReseauHydrauliqueCielOuvertIds"));
                reseauHydrauliqueCielOuvert = true;

            } else if (classeAndGetter.clazz ==  OuvrageParticulier.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getOuvrageParticulierIds"));
                ouvrageParticulier= true;

            } else if (classeAndGetter.clazz ==  EchelleLimnimetrique.class){
                nameMethod = classeAndGetter.getter.getName();
                Assert.assertTrue(nameMethod != null && nameMethod.equals("getEchelleLimnimetriqueIds"));
                echelleLimnimetrique = true;

            }

        }

        Assert.assertTrue(voieDigue && ouvrageVoirie && reseauHydrauliqueFerme
                && ouvrageHydrauliqueAssocie && reseauTelecomEnergie && ouvrageTelecomEnergie
                && reseauHydrauliqueCielOuvert && ouvrageParticulier && echelleLimnimetrique);

    }




}
