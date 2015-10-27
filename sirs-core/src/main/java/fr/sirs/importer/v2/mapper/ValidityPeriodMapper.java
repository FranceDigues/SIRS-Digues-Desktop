package fr.sirs.importer.v2.mapper;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.importer.AccessDbImporterException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class ValidityPeriodMapper extends AbstractMapper<AvecBornesTemporelles> {

    private static final String DEFAULT_START_FIELD = "DATE_DEBUT_VAL";
    private static final String DEFAULT_END_FIELD = "DATE_FIN_VAL";

    private static final String START_STRING = "DEBUT";
    private static final String END_STRING = "FIN";
    private static final String DATE_STRING = "DATE";

    private String startDateName;
    private String endDateName;

    private ValidityPeriodMapper(final Table t, final String startDateName, final String endDateName) {
        super(t);
        if (startDateName == null && endDateName == null)
            throw new IllegalArgumentException("No valid start or end date has been given yet.");
        this.startDateName = startDateName;
        this.endDateName = endDateName;
    }

    @Override
    public void map(Row input, AvecBornesTemporelles output) throws IllegalStateException, IOException, AccessDbImporterException {
        if (startDateName != null) {
            final Date start = input.getDate(startDateName);
            if (start != null) {
                output.setDate_debut(context.convertData(start, LocalDate.class));
            }
        }
        if (endDateName != null) {
            final Date end = input.getDate(endDateName);
            if (end != null) {
                output.setDate_fin(context.convertData(end, LocalDate.class));
            }
        }
    }

    @Component
    public static class Spi implements MapperSpi<AvecBornesTemporelles> {

        @Override
        public Optional<Mapper<AvecBornesTemporelles>> configureInput(Table inputType) {
            String startDateName = null;
            String endDateName = null;

            try {
                if (inputType.getColumn(DEFAULT_START_FIELD) != null) {
                    startDateName = DEFAULT_START_FIELD;
                }
            } catch (IllegalArgumentException e) {
                // No field with search name. We will try to use fallback case.
            }

            try {
                if (inputType.getColumn(DEFAULT_END_FIELD) != null) {
                    endDateName = DEFAULT_END_FIELD;
                }
            } catch (IllegalArgumentException e) {
                // No field with search name. We will try to use fallback case.
            }

            // Fallback.
            for (final Column c : inputType.getColumns()) {
                String cName = c.getName().toUpperCase();
                if (cName.contains(DATE_STRING)) {
                    if (startDateName == null && cName.contains(START_STRING)) {
                        startDateName = c.getName();
                    }  else if (endDateName == null && cName.contains(END_STRING)) {
                        endDateName = c.getName();
                    } else if (endDateName != null && startDateName != null) {
                        break;
                    }
                }
            }

            if (startDateName != null || endDateName != null) {
                return Optional.of(new ValidityPeriodMapper(inputType, startDateName, endDateName));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Class<AvecBornesTemporelles> getOutputClass() {
            return AvecBornesTemporelles.class;
        }
    }
}
