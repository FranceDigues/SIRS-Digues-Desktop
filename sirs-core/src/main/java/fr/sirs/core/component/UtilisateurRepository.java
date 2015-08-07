package fr.sirs.core.component;

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCoreRuntimeException;
import static fr.sirs.core.component.UtilisateurRepository.BY_LOGIN;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.core.model.Role;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import fr.sirs.core.model.Utilisateur;
import java.util.Collections;
import java.util.List;


@View(name = BY_LOGIN, map = "function(doc) {if(doc['@class']=='fr.sirs.core.model.Utilisateur') {emit(doc.login, doc._id)}}")
@Component("fr.sirs.core.component.UtilisateurRepository")
public class UtilisateurRepository extends AbstractSIRSRepository<Utilisateur>{

    public static final String BY_LOGIN = "byLogin";

    public static final Utilisateur GUEST_USER = ElementCreator.createAnonymValidElement(Utilisateur.class);
    static {
        GUEST_USER.setRole(Role.GUEST);
    }

    @Autowired
    private UtilisateurRepository ( CouchDbConnector db) {
       super(Utilisateur.class, db);
       initStandardDesignDocument();
   }

    @Override
    public Utilisateur create(){
        final SessionCore session = InjectorCore.getBean(SessionCore.class);
        if(session!=null){
            final ElementCreator elementCreator = session.getElementCreator();
            return elementCreator.createElement(Utilisateur.class);
        } else {
            throw new SirsCoreRuntimeException("Pas de session courante");
        }
    }

    public List<Utilisateur> getByLogin(final String login) {
        if (login == null || login.isEmpty())
            return Collections.singletonList(GUEST_USER);
        return this.queryView(BY_LOGIN, login);
    }

}

