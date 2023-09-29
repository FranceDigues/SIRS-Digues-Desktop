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
package fr.sirs.core.model;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Predicate;
import javafx.beans.property.ObjectProperty;
import org.apache.sis.measure.NumberRange;

/**
 * Spécifie un interval de validité temporelle, borné par une date de début et une
 * date de fin.
 *
 * @author Alexis Manin (Geomatys)
 */
public interface AvecBornesTemporelles extends AvecFinTemporelle{

    public ObjectProperty<LocalDate> date_debutProperty();

    public LocalDate getDate_debut();

    public void setDate_debut(LocalDate date_debut);

    public static boolean checkDatesIntersectRange(final LocalDate objDateDebut, final LocalDate objDateFin, final NumberRange dateRange) {
        final long debut    = objDateDebut == null ? 0 : objDateDebut.atTime(0, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
        final long fin      = objDateFin == null ? Long.MAX_VALUE : objDateFin.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();
        final NumberRange objDateRange = NumberRange.create(debut, true, fin, true);
        return dateRange.intersectsAny(objDateRange);
    }


    /**
     * Méthode d'archivage des tronçons de digue et des objets s'y référant dotés d'une validité temporelle (c'est à dire
     * implémentant l'interface {@link AvecBornesTemporelles}).
     *
     * La procédure d'archivage consiste à affecter une date de fin au tronçon de digue et aux objets archivables qui s'y
     * réfèrent, à condition qu'ils n'aient pas déjà une date de fin ou, éventuellement, que cette date de fin soit
     * postérieure à la date d'archivage. Cette dernière condition doit être volontairement activée.
     *
     * ------
     * On n'affecte une date de fin qu'aux objets qui n'en on pas déjà une SAUF si cette dernière est
     * postérieure à la date d'archivage. Cette condition provient du code initial récupéré de la fusion de
     * tronçons, avec une date d'archivage fixée à la veille de la date à laquelle est réalisée la fusion.
     * Mais quelle est la raison pratique de cette condition… ? Difficile à dire.
     *
     * Afin de pouvoir paramétrer ce comportement, la condition de postériorité de la date doit donc être
     * volontairement activée.
     *
     */
    public static final class ArchivePredicate implements Predicate<AvecBornesTemporelles> {

        private final LocalDate forceAfter;

        /**
         * @param forceAfter Date à partir de laquelle on force la réinitialisation de la date d'archivage. Si nulle, on
         * permet de n'archiver que les éléments non déjà archivés (ayant une date de fin nulle). Sinon, force en plus la
         * réinitialisation de la date de fin aux éléments dont la date d'archivate est postérieure à cette date.
         *
         */
        public ArchivePredicate(LocalDate forceAfter){
            this.forceAfter = forceAfter;
        }

        @Override
        public boolean test(AvecBornesTemporelles input) {
            final LocalDate endDate = input.getDate_fin();
            return endDate == null || (forceAfter!=null && endDate.isAfter(forceAfter));
        }
    }

    /**
     *
     */
    public static final class UnArchivePredicate implements Predicate<AvecBornesTemporelles> {

        private final LocalDate initialArchiveDate;

        public UnArchivePredicate(LocalDate initialArchiveDate){
            this.initialArchiveDate = initialArchiveDate;
        }

        @Override
        public boolean test(AvecBornesTemporelles dated) {
            final LocalDate date = dated.getDate_fin();
            return date != null && date.isEqual(initialArchiveDate);
        }
    }

    /**
     * Condition de mise à jour des éléments archivés : leur date de fin doit être nulle (c'est à dire qu'ils ne doivent
     * pas être archivés) ou égale à une date donnée.
     *
     * Pour la mise à jour de l'archivage d'un tronçon avec les objets qui le référencent.
     *
     * D'après la demande explicite de Jordan Perrin (SYM-1444), les objets dont la date de fin est identique à l'ancienne
     * date d'archivage du tronçon, voient alors leur nouvelle date de fin modifiée de manière à suivre celle du tronçon
     * (voir commentaire du 22/05/2017 15:10).
     */
    public static final class UpdateArchivePredicate implements Predicate<AvecBornesTemporelles> {

        private final LocalDate oldArchiveDate;

        public UpdateArchivePredicate(LocalDate oldArchiveDate){
            this.oldArchiveDate = oldArchiveDate;
        }

        @Override
        public boolean test(AvecBornesTemporelles dated) {
            return dated.getDate_fin()==null || dated.getDate_fin().isEqual(oldArchiveDate);
        }
    }

    /**
     * Check that given Element is within the date Range.
     */
    static final public class IntersectDateRange implements Predicate<AvecBornesTemporelles> {

        /**
         * The date range to use to filter the @{@link Prestation}
         */
        final NumberRange dateRange;

        public IntersectDateRange(final NumberRange dateRange) {
            this.dateRange = dateRange;
        }

        @Override
        public boolean test(AvecBornesTemporelles prestation) {
            final LocalDate objDateDebut    = prestation.getDate_debut();
            final LocalDate objDateFin      = prestation.getDate_fin();
            return checkDatesIntersectRange(objDateDebut, objDateFin, dateRange);
        }
    }
}
