
### Description
Ce dossier contient le script buildEditionFormPilote.py qui sert à créer des fichiers de configuration qui servent à:
	- piloter la construction de certains composants dans SirsMobile
	- piloter la construction de modèles dans le plugin QGIS.
Ce script est placé dans SIRS (desktop) pour utiliser les diagrammes ecore et les fichier de propriétés.
Ce script est motivé pour faciliter les répercutions de mise à jour du modèle de SirsDesktop dans SirsMobile et le plugin QGIS.
Ce script crée:
 - Le fichier form-template-pilotes.ts: fichier typescript contient la variable formTemplatePilote.
 - Le fichier formTemplatePilote.json: fichier json contenant un ensemble d'objets de Sirs avec une description attributaire.
 - Le fichier user_preference_correspondence.json: fichier json qui va conserver les préférences utilisateurs dans le plugin QGIS.


##### SirsMobile
form-template-pilotes.ts contient l'objet formTemplatePilote (utilisé par le service formstemplate.service.ts).
formTemplatePilote est une "feuille de route" pour le composant générique base-form, qui produit une partie du formulaire d'édition
pour certains objets (actuellemement tous les objets ne sont pas concernés).
Actuellement les objets concernés sont:
	Prestation								(Core)
	AmenagementHydraulique					(Plugin-dependance)
	DesordreDependance						(Plugin-dependance)
	PrestationAmenagementHydraulique		(Plugin-dependance)
	StructureAmenagementHydraulique			(Plugin-dependance)
	OuvrageAssocieAmenagementHydraulique	(Plugin-dependance)
	OrganeProtectionCollective				(Plugin-dependance)

##### Plugin QGIS
formTemplatePilote.json et user_preference_correspondence.json sont des fichiers de configuration à la racine du plugin QGIS.
Specifiquement pour le plugin QGIS:
 - formTemplatePilote.json structure la génération des couches de données dans QGIS.
 - user_preference_correspondence.json va conserver:
 	- Le dernier paramétrage de l'interface du plugin QGIS (choix des objets, attributs...).
 	- Le style des couches dans QGIS (par type et par géométrie).
 	- Le CRS des couches dans GQIS (par type).


### Usage
##### Prérequis
/!\Ce script doit être lancé sur un système UNIX ou MacOS/!\
/!\Ce script doit être lancé depuis ce dossier/!\
/!\Le binaire d'execution doit être python version 3.6 ou plus/!\
/!\Installer le module pyecore (ex: `python -m pip install pyecore`)

##### Command
Commande:
	`python [--help|--info|--plugin|--mobile] buildEditionFormPilote.py`

 --help: Affiche cette documentation.
 --info: Affiche la liste des objets traités et la liste des objets "contenus" traités.
 --plugin : Copie formTemplatePilote.json et user_preference_correspondence.json dans le module Qgis 	(variable d'environnement à renseigner: QGIS_PLUGIN_PROJECT_PATH).
 --mobile : Copie le fichier form-template-pilotes.ts dans le projet SIRSMOBILEV2 			(variable d'environnement à renseigner: SIRS_MOBILE_PROJECT_PATH).

/*\Note importante: Dans SIRSMOBILEV2, l'ajout de nouveaux objets a motivé la création de ce système afin de rendre générique la création des
formulaires d'édition, ainsi seules quelques objets sont concernés par ce fonctionnement. Si le besoin change, on peut toujours modifier cette liste
en éditant la variable FORM_TEMPLATE_MOBILE_CLASS au début du script buildEditionFormPilote.py/*\

/*\Note importante: Dans SIRSMOBILEV2, le composant BaseForm n'affiche que la partie du formulaire d'édition propre à chaque objet, ainsi
certains attributs sont volontairement retirés dans le fichier form-template-pilotes.ts. Si le besoin change, on peut toujours modifier cette liste
en éditant la variable NO_FORM_TEMPLATE_MOBILE_ATTRIBUTE au début du script buildEditionFormPilote.py/*\

