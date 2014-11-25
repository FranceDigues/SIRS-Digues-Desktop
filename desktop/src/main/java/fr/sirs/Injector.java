
package fr.sirs;

import fr.sirs.core.component.DocumentChangeEmiter;
import fr.sirs.index.SearchEngine;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 *
 * @author Olivier Nouguier (Géomatys)
 * @author Samuel Andrés (Géomatys)
 */
@Component
public class Injector implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;

    }

    public static void injectDependencies(Object o) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(o);
    }


    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
        
    }
    
    public static Session getSession(){
        return getBean(Session.class);
    }
    
    public static DocumentChangeEmiter getDocumentChangeEmiter(){
        return getBean(DocumentChangeEmiter.class);
    }
    
    public static SearchEngine getSearchEngine(){
        return getBean(SearchEngine.class);
    }
    
}
