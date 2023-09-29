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
package fr.sirs.core.authentication;

import fr.sirs.core.SirsCore;
import fr.sirs.core.authentication.AuthenticationWallet.Entry;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.elasticsearch.common.io.Streams;

/**
 * An authenticator which will prompt a JavaFX dialog to query password from user.
 *
 * @author Alexis Manin (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 */
public class SIRSAuthenticator extends Authenticator {

    private final AuthenticationWallet wallet = AuthenticationWallet.getDefault();

    /**
     * Keep reference of checked entries, because if login information is wrong,
     * we'll know it and will prompt user.
     */
    private static final Map<String, Entry> ENTRIES_TO_CHECK = new ConcurrentHashMap<>();


    /**
     * Récupération d'un mots de passe:
     * Soit depuis AuthenticationWallet. 
     * le demande à l'utilisateur ou teste celui déjà indiqué par l'utilisateur 
     * via la Map ENTRIES_TO_CHECK. 
     * En cas de succès, la méthode à l'origine de la demande de l'identification
     * doit l'indiquer à cette classe à partir de la méthode statique :
     * {@link #validEntry(java.net.URL)} afin de sauvegarder l'entrée valide.
     * 
     * Todo : autoriser plusieurs authentification pour une même base?
     * 
     * @return 
     */
    @Override
    protected synchronized PasswordAuthentication getPasswordAuthentication() {
        // First, we retrieve target service information and check its integrity.
        String host = getRequestingHost();
        int port = getRequestingPort();
        final URL url = getRequestingURL();
        if (host == null || host.isEmpty()) {
            if (url == null)
                throw new IllegalStateException("Neither host nor valid URL has been provided for authentication check");
            else
                host = url.getHost();
        }

        if (port < 0 && url != null) {
            port = (url.getPort() < 0)? url.getDefaultPort() : url.getPort();
        }

        String serviceId = AuthenticationWallet.toServiceId(host, port);
        AuthenticationWallet.Entry entry = wallet == null? null : wallet.get(host, port);

        /*
//         * HACK : Apache HttpClient (used by Ektorp) will call this method on
//         * each query, which means we cannot determine if it is performing a
//         * fail&retry. As java.net methods give us the query URL, we can adopt
//         * different behavior for thee two components.
//         */

        final Entry checkedEntry =  ENTRIES_TO_CHECK.get(serviceId);
        final boolean nullEntryToCheck = checkedEntry == null;
        
        if ( (entry != null) &&   
                (nullEntryToCheck || 
                    (  ((!checkedEntry.checked)  && !(checkedEntry.equals(entry))) 
                    && (checkedEntry.checked && ( (checkedEntry.equals(entry))))
                    )
                ) 
            ){
//           
        // We've got login from wallet, and it has not been rejected yet.
                // A priori l'attribut checked n'est plus nécessaire (car on ne sauvegarde que des identifiants
                // valides. Je le conserve cependant au cas où l'identifiant et mots de passe
                // sont changés. Les identifiants sauvegardés sont alors obsolètes.
                if (!nullEntryToCheck && !checkedEntry.checked) {
                    ENTRIES_TO_CHECK.put(serviceId, entry);
                    checkedEntry.checked=true;
                }
                return new PasswordAuthentication(entry.login, (entry.password == null) ? new char[0] : entry.password.toCharArray());

        // New or invalid entry case.
        } else {
            
            Map.Entry<String, String> login = askForLogin(entry == null? null : entry.login, entry == null? null : entry.password);
            if (login == null || login.getKey() == null) {
                return null;
            } else {
                entry = new AuthenticationWallet.Entry(host, port, login.getKey(), login.getValue());
                ENTRIES_TO_CHECK.put(serviceId, entry);
                return new PasswordAuthentication(login.getKey(), login.getValue() == null? new char[0] : login.getValue().toCharArray());
            }
        }
    }
    
    /**
     * Méthode à appeler lorsqu'une recette réussie.
     * 
     * on entre l'entrée validée dans la "wallet" avec ses attributs
     * (checked) mis à jour. Puis on la retire de la Map.
     * 
     * Idéalement, il faudrait faire évoluer cette méthode pour n'entrer que l'entrée
     * associée à la requête.
     * 
     * @param couchdbUrl : the url associated with the entries to validate.
     */
    public static void validEntry(URL couchdbUrl) {
        if (couchdbUrl == null) {
            throw new IllegalStateException("Neither host nor valid URL has been provided for authentication validation");
        }
        String serviceId = AuthenticationWallet.toServiceId(couchdbUrl);
        
        if (ENTRIES_TO_CHECK != null && ENTRIES_TO_CHECK.containsKey(serviceId)) {
            final AuthenticationWallet wallet = AuthenticationWallet.getDefault();
            if (wallet != null) {
                wallet.put(ENTRIES_TO_CHECK.get(serviceId));
                ENTRIES_TO_CHECK.remove(serviceId);
            }
        }
    }

    /**
     * Display a dialog to ask user a login and password to allow connection to queried service.
     * @param defaultUser Default login to show in login input. Can be null.
     * @param defaultPass default password to fill password input with. Can be null.
     * @return An entry whose key is login and value is password typed by user. Null if user cancelled dialog.
     */
    public Map.Entry<String, String> askForLogin(final String defaultUser, final String defaultPass) {
        final Task<Map.Entry<String, String>> askLogin = new Task() {

            @Override
            protected Object call() throws Exception {
                final TextField userInput = new TextField(defaultUser);
                final PasswordField passInput = new PasswordField();
                passInput.setText(defaultPass);

                final GridPane gPane = new GridPane();
                gPane.add(new Label("Login : "), 0, 0);
                gPane.add(userInput, 1, 0);
                gPane.add(new Label("Mot de passe : "), 0, 1);
                gPane.add(passInput, 1, 1);

                final StringBuilder headerText = new StringBuilder().append("Identifiants requis pour ");
                if (RequestorType.PROXY.equals(getRequestorType())) {
                    headerText.append("le Proxy ");
                } else if (getRequestingPort() == 5984) {
                    headerText.append("CouchDB");
                } else {
                    headerText.append("le service");
                }
                headerText.append(" :\n");
                final String host = getRequestingHost();
                if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
                    headerText.append("Service local");
                } else {
                    headerText.append(getRequestingSite());
                }
                if (getRequestingPort() >= 0) {
                    headerText.append(", port ").append(getRequestingPort());
                }

                final Alert question = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.CANCEL, ButtonType.OK);
                question.getDialogPane().setContent(gPane);
                question.setResizable(true);
                question.setTitle("Authentification requise");
                question.setHeaderText(headerText.toString());

                Optional<ButtonType> result = question.showAndWait();
                if (result.isPresent()) {
                    if (result.get().equals(ButtonType.OK)) {
                        return new AbstractMap.SimpleEntry<>(userInput.getText(), passInput.getText());
                    } else if (result.get().equals(ButtonType.CANCEL)) {
                        System.exit(0);
                    }
                }
                return null;
            }
        };

        if (Platform.isFxApplicationThread()) {
            askLogin.run();
        } else {
            Platform.runLater(askLogin);
        }

        try {
            return askLogin.get();
        } catch (InterruptedException | ExecutionException ex) {
            SirsCore.LOGGER.log(Level.WARNING, "Authentication prompt failed for service : "+ getRequestingSite().toString(), ex);
            return null;
        }
    }

    /**
     * Temporary method to retrieve apikey query parameter.
     * @return apikey
     * @throws IOException
     */
    public static String getThunderForestApiKey() throws IOException {
        final String apiKeyPath = "/fr/sirs/core/authentication/apikey";
        List<String> lines = Streams.readAllLines(SIRSAuthenticator.class.getResourceAsStream(apiKeyPath));
        if (lines.size() != 1) throw new IOException("apiKey file has the wrong format");
        return PasswordDeserializer.decode(lines.get(0));
    }
}
