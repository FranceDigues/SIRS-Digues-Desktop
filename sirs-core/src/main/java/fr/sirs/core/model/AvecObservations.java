

package fr.sirs.core.model;

import fr.sirs.util.SirsComparator;
import org.apache.sis.measure.Range;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface AvecObservations extends  Element {

    List<? extends AbstractObservation> getObservations();

    default Optional<? extends AbstractObservation> getLastObservation() {
        List<? extends AbstractObservation> observations = getObservations();
        if (observations == null) return Optional.empty();
        return getObservations().stream()
                .min(SirsComparator.OBSERVATION_COMPARATOR);
    }

    /**
     * Filtre les éléments par la date de leur derniere observation.
     * L'élément est sélectionné si sa dernière observation a été créé à une date comprise dans l'interval donné.
     */
    final class LastObservationPredicate implements Predicate<AvecObservations> {

        final Range<LocalDate> selectedRange;

        /**
         * /!\ si aucune date de début ou date de fin n'est renseignée
         * alors selectedRange est initialisé à null.
         */
        public LastObservationPredicate(final LocalDate start, final LocalDate end) {
            //final LocalDate start = uiOptionDebutLastObservation.getValue();
            //final LocalDate end = uiOptionFinLastObservation.getValue();
            if (start == null && end == null) {
                selectedRange = null;
            } else {
                selectedRange = new Range<>(LocalDate.class, start == null? LocalDate.MIN : start, true, end == null? LocalDate.MAX : end, true);
            }
        }

        @Override
        public boolean test(AvecObservations t) {
            final List<? extends AbstractObservation> observations = t.getObservations();

            if ((observations!=null) && (observations.size() > 0)) {
                final AbstractObservation lastObservation = Collections.min(observations, SirsComparator.OBSERVATION_COMPARATOR);
                final LocalDate ld = lastObservation.getDate();

                if (ld != null) {
                    return selectedRange == null || selectedRange.contains(ld);
                }
            }
            return selectedRange == null;
        }
    }

    /**
    * Check that the most recent observation defined on given réseau has an
    * {@link ObservationReseauHydrauliqueFerme#getUrgenceId() } compatible with user choice.
    * If user has not chosen any urgence, all réseau are accepted.
    */
    final class UrgencePredicate implements Predicate<AvecObservations> {

        final Set<String> acceptedIds;

        public UrgencePredicate(final Set<String> acceptedIds) {
            this.acceptedIds = acceptedIds;
        }

        @Override
        public boolean test(final AvecObservations t) {
            if (acceptedIds.isEmpty())
                return true;
            if (t instanceof ReseauHydrauliqueFerme) {
                return t.getLastObservation()
                        .map(obs -> (ObservationReseauHydrauliqueFerme) obs)
                        .map(obs -> obs.getUrgenceId() != null && acceptedIds.contains(obs.getUrgenceId()))
                        .orElse(false);
            } else if (t instanceof OuvrageHydrauliqueAssocie) {
                return t.getLastObservation()
                        .map(obs -> (ObservationOuvrageHydrauliqueAssocie) obs)
                        .map(obs -> obs.getUrgenceId() != null && acceptedIds.contains(obs.getUrgenceId()))
                        .orElse(false);
            } else if (t instanceof Desordre) {
                return t.getLastObservation()
                        .map(obs -> (Observation) obs)
                        .map(obs -> obs.getUrgenceId() != null && acceptedIds.contains(obs.getUrgenceId()))
                        .orElse(false);
            } else throw new IllegalStateException("This Element's observation has no urgence level");
        }
    }
}