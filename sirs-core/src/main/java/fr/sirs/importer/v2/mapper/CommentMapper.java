package fr.sirs.importer.v2.mapper;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import fr.sirs.core.model.AvecCommentaire;
import fr.sirs.importer.AccessDbImporterException;
import java.io.IOException;
import java.util.Optional;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class CommentMapper extends AbstractMapper<AvecCommentaire> {

    private static final String STRUCTURE_FIELD = "DESCRIPTION_";
    private static final String DEFAULT_FIELD = "COMMENTAIRE";

    private final String fieldName;

    private CommentMapper(final Table t, String fieldName) {
        super(t);
        this.fieldName = fieldName;
    }

    @Override
    public void map(Row input, AvecCommentaire output) throws IllegalStateException, IOException, AccessDbImporterException {
        String comment = input.getString(fieldName);
        if (comment != null) {
            output.setCommentaire(comment);
        }
    }

    public static class Spi implements MapperSpi<AvecCommentaire> {

        @Override
        public Optional<Mapper<AvecCommentaire>> configureInput(Table inputType) {
            for (final Column c : inputType.getColumns()) {
                if (c.getName().toUpperCase().startsWith(STRUCTURE_FIELD)
                        || c.getName().toUpperCase().startsWith(DEFAULT_FIELD)) {
                    return Optional.of(new CommentMapper(inputType, c.getName()));
                }
            }
            return Optional.empty();
        }

        @Override
        public Class<AvecCommentaire> getOutputClass() {
            return AvecCommentaire.class;
        }
    }
}
