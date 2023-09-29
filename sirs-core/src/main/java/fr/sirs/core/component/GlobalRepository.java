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
package fr.sirs.core.component;

import fr.sirs.core.model.Element;
import java.util.List;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An repository whose role is to create global views useful for all repositories.
 * For exemple, instead of creating an "all" view for each repository, we can use
 * global repository to to make a single view whose keys are repository model class.
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
@Views({
    @View(name=GlobalRepository.BY_CLASS_AND_LINEAR_VIEW, map="function(doc) {if(doc['@class']) {emit([doc['@class'], doc.linearId], {" +
"            id: doc._id," +
"            rev: doc._rev," +
"            designation: doc.designation," +
"            libelle: doc.libelle," +
"            geometry: doc.geometry" +
"        })}}"),
    @View(name=GlobalRepository.CONFLICTING_REVISIONS, map="function(doc) {if(doc._conflicts) {emit(doc._id, doc._conflicts)}}")
})
public class GlobalRepository extends CouchDbRepositorySupport<Element> {

    protected static final String BY_CLASS_AND_LINEAR_VIEW = "byClassAndLinear";
    protected static final String CONFLICTING_REVISIONS = "conflictingRevisions";

    @Autowired
    private GlobalRepository(CouchDbConnector db) {
        super(Element.class, db);
        initStandardDesignDocument();
    }

    protected <T> ViewQuery createByClassQuery(Class<T> type) {
        final ComplexKey startKey = ComplexKey.of(type.getCanonicalName());
        final ComplexKey endKey = ComplexKey.of(type.getCanonicalName(), ComplexKey.emptyObject());
        return createQuery(BY_CLASS_AND_LINEAR_VIEW)
                .startKey(startKey)
                .endKey(endKey)
                .includeDocs(true);
    }

    protected <T> ViewQuery createByLinearIdQuery(Class<T> type, final String linearId) {
        return createQuery(BY_CLASS_AND_LINEAR_VIEW)
                .key(ComplexKey.of(type == null? ComplexKey.emptyObject() : type.getCanonicalName(), linearId))
                .includeDocs(true);
    }

    <T> List<T> getAllForClass(Class<T> type) {
        return db.queryView(createByClassQuery(type), type);
    }

    <T> List<T> getByLinearId(Class<T> type, final String linearId) {
        return db.queryView(createByLinearIdQuery(type, linearId), type);
    }

    List getConflictingRevisions(final String id) {
        return db.queryView(createQuery(CONFLICTING_REVISIONS).key(id), List.class);
    }
}
