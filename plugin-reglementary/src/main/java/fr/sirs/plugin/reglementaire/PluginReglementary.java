/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.reglementaire;

import fr.sirs.Injector;
import fr.sirs.Plugin;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.EtapeObligationReglementaireRepository;
import fr.sirs.core.component.ObligationReglementaireRepository;
import fr.sirs.core.model.EtapeObligationReglementaire;
import fr.sirs.core.model.ObligationReglementaire;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.RefEcheanceRappelObligationReglementaire;
import fr.sirs.core.model.RefTypeObligationReglementaire;
import fr.sirs.ui.AlertItem;
import fr.sirs.ui.AlertManager;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.scene.image.Image;

/**
 * Plugin correspondant au module réglementaire, permettant de gérer des documents de suivis.
 *
 * @author Cédric Briançon (Geomatys)
 */
public class PluginReglementary extends Plugin {
    private static final String NAME = "plugin-reglementary";
    public static final String NODE_PREFERENCE_NAME = "ReglementaryPlugin";
    private static final String TITLE = "Module réglementaire";

    public PluginReglementary() {
        name = NAME;
        loadingMessage.set("module réglementaire");
        themes.add(new DocumentsTheme());
        themes.add(new StatesGeneratorTheme());
        themes.add(new RegistreTheme());
    }

    /**
     * Chargement du plugin et vérification des alertes possibles à afficher.
     *
     * @throws Exception
     */
    @Override
    public void load() throws Exception {
        getConfiguration();
        showAlerts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getTitle() {
        return TITLE;
    }

    /**
     * Pas d'image pour ce plugin, image par défaut.
     *
     * @return {@code null}
     */
    @Override
    public Image getImage() {
        return null;
    }

    /**
     * Récupère les alertes à afficher pour l'utilisateur, selon les dates fournies dans les étapes d'obligations
     * réglementaires et leurs échéances de rappel.
     */
    public static void showAlerts() {
        final Set<AlertItem> alerts = new HashSet<>();

        final EtapeObligationReglementaireRepository eorr = Injector.getBean(EtapeObligationReglementaireRepository.class);
        final List<EtapeObligationReglementaire> etapes = eorr.getAll();
        if (etapes.isEmpty()) {
            AlertManager.getInstance().addAlerts(alerts);
            return;
        }

        final ObligationReglementaireRepository orr = Injector.getBean(ObligationReglementaireRepository.class);
        final AbstractSIRSRepository<RefEcheanceRappelObligationReglementaire> repoEcheanceRappel =
                Injector.getSession().getRepositoryForClass(RefEcheanceRappelObligationReglementaire.class);
        final AbstractSIRSRepository<RefTypeObligationReglementaire> repoTypeObl =
                Injector.getSession().getRepositoryForClass(RefTypeObligationReglementaire.class);
        final LocalDate now = LocalDate.now();

        for (final EtapeObligationReglementaire etape : etapes) {
            final LocalDate oblDate = etape.getDateRealisation() != null ? etape.getDateRealisation() :
                    etape.getDateEcheance();
            if (oblDate == null) {
                continue;
            }

            if (etape.getEcheanceId() == null) {
                continue;
            }

            final RefEcheanceRappelObligationReglementaire echeance = repoEcheanceRappel.get(etape.getEcheanceId());

            // Compare la date actuelle avec la date d'échéance de l'obligation et le temps avant la date d'échéance
            // pour afficher l'alerte. Exemple : une obligation au 1er juillet avec un rappel 3 mois avant,
            // l'alerte sera affichée si la date du lancement de l'application est comprise dans cette période.
            if (oblDate.minusMonths(echeance.getNbMois()).compareTo(now) <= 0 && oblDate.compareTo(now) >= 0) {
                // Construction du texte à afficher sur le calendrier
                final StringBuilder sb = new StringBuilder();
                if (etape.getObligationReglementaireId() == null) {
                    continue;
                }
                final ObligationReglementaire obligation = orr.get(etape.getObligationReglementaireId());
                if (obligation.getTypeId() != null) {
                    sb.append(repoTypeObl.get(obligation.getTypeId()).getAbrege()).append(" - ");
                }
                if (obligation.getSystemeEndiguementId() != null) {
                    final Preview previewSE = Injector.getSession().getPreviews().get(obligation.getSystemeEndiguementId());
                    sb.append(previewSE.getLibelle()).append(" - ");
                }
                sb.append(obligation.getAnnee());
                alerts.add(new AlertItem(sb.toString(), oblDate, etape));
            }
        }

        AlertManager.getInstance().addAlerts(alerts);
    }

    @Override
    public Optional<Image> getModelImage() throws IOException {
        final Image image;

        try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("fr/sirs/reglementaryModel.png")) {
            image = new Image(in);
        }
        return Optional.of(image);
    }
}
