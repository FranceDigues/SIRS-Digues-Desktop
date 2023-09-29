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
import java.util.Arrays;
import java.util.List;
import javafx.collections.ObservableList;

/**
 * Enum visant à accompagné la mise à jour dans le modèle ecore de référence 1-n
 * en référence n - n.
 *
 * Avant cette mise à jour, les instances de la classe associée au 1 de la
 * relation porte l'information des n éléments qui lui sont liés.
 * Cette information est portée par un attribut {@link ObservableList<String>}.
 * Après la mise à jour, les 2 instances des 2 classes liées doivent porter
 * l'information.
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public enum Upgrades1NtoNNSupported {

    DESORDRE(2, 23, Desordre.class,  Arrays.asList(
                                        new ClassAndItsGetter(VoieDigue.class, Desordre.class),
                                        new ClassAndItsGetter(OuvrageVoirie.class, Desordre.class),
                                        new ClassAndItsGetter(ReseauHydrauliqueFerme.class, Desordre.class),
                                        new ClassAndItsGetter(OuvrageHydrauliqueAssocie.class, Desordre.class),
                                        new ClassAndItsGetter(ReseauTelecomEnergie.class, Desordre.class),
                                        new ClassAndItsGetter(OuvrageTelecomEnergie.class, Desordre.class),
                                        new ClassAndItsGetter(ReseauHydrauliqueCielOuvert.class, Desordre.class),
                                        new ClassAndItsGetter(OuvrageParticulier.class, Desordre.class),
                                        new ClassAndItsGetter(EchelleLimnimetrique.class, Desordre.class)
    ));


    final int upgradeMajorVersion;
    final int upgradeMinorVersion;
    final Class linkSide1;
    final List<ClassAndItsGetter> linkSidesN;


    private Upgrades1NtoNNSupported(final int major, final int minor, final Class clazz, final List<ClassAndItsGetter> listLinks1N){
        this.upgradeMajorVersion = major;
        this.upgradeMinorVersion = minor;
        this.linkSide1 = clazz;
        this.linkSidesN = listLinks1N;
    }

}
