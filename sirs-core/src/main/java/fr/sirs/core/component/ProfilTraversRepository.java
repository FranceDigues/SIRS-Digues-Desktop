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


import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;

import fr.sirs.core.model.ProfilTravers;
import org.ektorp.CouchDbConnector;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.support.View;

/**
 * Outil g�rant les �changes avec la bdd CouchDB pour tous les objets ProfilTravers.
 *
 * @author Maxime Gavens (Geomatys)
 */
@View(name=ProfilTraversRepository.BY_DESIGNATION, map="function(doc) {if(doc['@class']=='fr.sirs.core.model.ProfilTravers') {emit(doc.designation, doc._id)}}")
@Component("fr.sirs.core.component.ProfilTraversRepository")
public class ProfilTraversRepository extends AbstractSIRSRepository<ProfilTravers> {

    public static final String BY_DESIGNATION = "byDesignation";

    @Autowired
    private ProfilTraversRepository ( CouchDbConnector db) {
        super(ProfilTravers.class, db);
        initStandardDesignDocument();
    }

    @Override
    public ProfilTravers create() {
        return InjectorCore.getBean(SessionCore.class).getElementCreator().createElement(ProfilTravers.class);
    }

    public List<ProfilTravers> getByDesignation(final String designation) {
        ArgumentChecks.ensureNonNull("Profil en travers designation", designation);
        return this.queryView(BY_DESIGNATION, designation);
    }
}

