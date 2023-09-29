SIRS-DIGUES V2

Copyright (C) 2016, France-Digues
Licence GPLv3 

### Table des matières : 
* 1 - Créer un tag du projet
* 2 - Compiler un paquet executable (jar ou natif)
* 3 - Activer la détection de mise à jour
* 4 - Créer un plugin
* 5 - TroubleShot


### 1 - Créer un tag du projet

A la racine du projet se trouve un dossier "build". Dans ce dossier, vous trouverez un script "releaseScript.sh". Si vous l'éxecutez sans argument, il créera un tag pour la prochaine version mineure du projet. Vous pouvez changer la version à générer en passant les paramètres suivants : <br/>
-release.minor : version mineure à utiliser pour le tag (doit être un nombre). <br/>
-release.major : version majeure à utiliser pour le tag (doit être un nombre).

Pour une aide contextuelle concernant le script, utilisez l'argument -h.

Note : le script crée le tag en local, mais ne pousse rien. <br/>
Note2 : pour créer le tag, le script crée une branche temporaire qu'il efface une fois le tag généré avec succès. En cas d'erreur durant l'éxecution du script, il est possible que vous vous retrouviez avec une branche inutile sur votre dépôt local. <br/>
Note3 : Si vous devez mettre à jour des plugins, pensez à suivre la section dédiée dans le wiki avant le lancement du script. <br/>
Note4 : L'exécution du script de modification de la version dans les pom.xml nécessite l'installation d'Apache Ant.

### 2 - Compiler un paquet executable

Pour compiler un éxecutable de l'application, vous devez tout d'abord vous rendre dans le dossier "launcher". Depuis ce dossier, vous avez 2 choix : 

- Créer un jar executable via la commande "mvn jfx:jar". Cela créera un jar éxecutable permettant de lance l'application dans le repertoire "target/jfx/app". <br/>
- Créer un paquet natif pour votre OS, via la commande "mvn jfx:native". Cela créera un installeur dans le dossier "target/jfx/native". <br/>
Note Windows: Avant la création du paquet natif, installer 'Inno Setup', depuis un executable 'innosetup-*.exe' accessible sur https://jrsoftware.org/isdl.php. <br/>
Note : vous pouvez personnaliser les paramètres de la jvm pour l'application en modifiant le pom.xml de l'application. Dans la section configuration du plugin javafx-maven-plugin, vous pouvez ajouter des balises <jvmArg>votre parametre</jvmArgs>.

IMPORTANT : Si la license du logiciel ou les conditions d'utilisations évoluent, vous devez mettre à jour les fichiers présents dans launcher/src/main/deploy/additional.

### 3 - Activer la détection de mise à jour

Pour permettre à l'application SIRS-Digues de détecter si des mises à jour sont disponibles sur le serveur, il est nécessaire de publier un document JSON de la forme suivante : 

{
	"url":"http:/<host:port>/<path>", // URL vers la page de téléchargement de la mise à jour.
	"version":"X.x" // N° de version de la mise à jour..
} 

Avec ce docuement, l'application saura détecter une mise à jour du paquet, et rediriger l'utilisateur vers cette dernière.

Note : L'URL vers le JSON de mise à jour est editable dans les préférences de l'application, et la configuration par défaut est la suivante : "http://sirs-digues.info/wp-content/updates/plugins.json"

### 4 - Créer un plugin

Un archétype maven est fourni dans le projet SIRS. Ce squelette contient la structure de base à respecter pour les sources du projet, ainsi que le fichier de configuration de Maven (pom.xml) permettant de compiler le projet et de créer le paquet nécessaire à son intégration dans l’application.

Pour créer un nouveau projet de type plugin :

1 - compiler et déployer localement l’archétype de plugin (il n’est nécessaire de l’éxecuter qu’une fois pour une machine donnée). <br/>
 → Ouvrir une console. <br/>
 → Depuis les sources du projet SIRS, aller dans le dossier “plugins/pluginArchetype”. <br/>
 → Compiler le module en lançant la commande : “mvn clean install”. <br/>
 → Lancer la commande “mvn archetype:jar”. Cela va générer le modèle de projet. Il va   maintenant falloir le déployer localement pour pouvoir l’utiliser. <br/>
 → Lancer la commande “mvn archetype:update-local-catalog”. Cela permet de publier l’archétype de plugin localement, et de générer de nouveaux projets à partir de ce dernier.

2 - Créer un nouveau projet de type plugin : <br/>
 → Ouvrez une console <br/>
 → Placez-vous dans le dossier où vous voulez créer vos sources. <br/>
 → Executez la commande : “mvn archetype:generate -DarchetypeCatalog=local -DarchetypeGroupId=fr.sirs.plugins -DarchetypeArtifactId=sirs-plugin-archetype”. <br/>
Cela permet d’initialiser un nouveau projet construit comme le modèle choisi. Dans la commande, il vous sera demandé de spécifier plusieurs propriétés pour terminer la création de votre projet : <br/>
 → Les propriétés ‘groupId’, ‘artifactId’,  et ‘version’ sont respectivement le paquet, le nom et la version initiale que vous voulez donner à votre projet maven. <br/>
 → La propriété ‘package’ est le paquet Java à générer pour votre projet. Par défaut, c’est le même que celui de votre projet maven. <br/>
 → ‘sirs-version’ est la version de l’application SIRS2 compatible avec votre plugin.

Une fois toutes les propriétés spécifiées, maven vous demande confirmation. Confirmez, et la création de votre plugin est terminée.

