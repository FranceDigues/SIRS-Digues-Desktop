from pyecore.resources import ResourceSet, URI, global_registry
from style.updateUserPreferenceStyles import update_style_from_data
import pyecore.ecore as Ecore  # We get a reference to the Ecore metamodel implementation.
import json
import sys
import glob
import os
import shutil
import re


#QGIS_PLUGIN_PROJECT_PATH = "/home/maximegavens/GEOMATYS/qgis-plugin-couchdb-project/qgis-plugin-couchdb/couchdb_importer"
QGIS_PLUGIN_PROJECT_PATH = os.environ.get('QGIS_PLUGIN_PROJECT_PATH')
#SIRS_MOBILE_PROJECT_PATH = "/home/maximegavens/GEOMATYS/sirsMobile-project/sirsmobilev2"
SIRS_MOBILE_PROJECT_PATH = os.environ.get('SIRS_MOBILE_PROJECT_PATH')
FORM_TEMPLATE_MOBILE_CLASS = [
    "AmenagementHydraulique",
    "PrestationAmenagementHydraulique",
    "StructureAmenagementHydraulique",
    "OrganeProtectionCollective",
    "DesordreDependance",
    "OuvrageAssocieAmenagementHydraulique",
    "Prestation"
]
NO_FORM_TEMPLATE_MOBILE_ATTRIBUTE = [
    "geometry",
    "date_debut",
    "date_fin",
    "dateMaj",
    "commentaire",
    "designation",
    "valid",
    "author"
]

all_positionable = []
containment_classes_from_positionable = set()


def unicodize(seg):
    if re.match(r'\\u[0-9a-f]{4}', seg):
        return seg.decode('unicode-escape')

    return seg.decode('utf-8')


class LabelLoader:
    def __init__(self, properties_paths=None):
        self.propertieDictionnary = {}
        self.classes = []
        if properties_paths is not None:
            for p in properties_paths:
                self.complete_dict(p)

    def complete_dict(self, path_to_property_files):
        propertiesFileName = glob.glob(path_to_property_files)

        # iterate on file properties
        for path in propertiesFileName:
            if os.path.isdir(path):
                continue
            filestr = path.split('/')[-1]
            clazz = filestr.split('.')[0]
            self.propertieDictionnary[clazz] = {}
            self.classes.append(clazz)

            with open(path, "r") as propertieFile:
                propertieFileStr = propertieFile.read()
                propertieFileStr = propertieFileStr.encode("UTF-8").decode("unicode_escape")

                rows = propertieFileStr.split('\n')[2:-1]
            for row in rows:
                r = row.split('=')
                self.propertieDictionnary[clazz][r[0]] = r[1]

    def to_label(self, clazz, attribute):
        try:
            return self.propertieDictionnary[clazz][attribute]
        except KeyError:
            return ""

    def load_user_preferences_from_labels(self):
        user_preferences = {}

        for clazz in self.propertieDictionnary:
            user_preferences[clazz] = {"attributes": {}}

            # Does not exist in propertie file
            user_preferences[clazz]["attributes"]["_id"] = True
            user_preferences[clazz]["attributes"]["_rev"] = True
            user_preferences[clazz]["attributes"]["@class"] = True

            for attribute in self.propertieDictionnary[clazz]:
                user_preferences[clazz]["attributes"][attribute] = True
        return user_preferences


def check_argument():
    copy2plugin = False
    copy2mobile = False
    printInfo = False
    argv = sys.argv

    #parse arguments
    if argv[-1] == "--help":
        with open("README.md", "r") as f:
            content = f.read()
            print(content)
        sys.exit(0)
    for i in range(len(argv)):
        if argv[i] == "buildEditionFormPilote.py":
            continue
        elif argv[i] == "--plugin":
            copy2plugin = True
        elif argv[i] == "--mobile":
            copy2mobile = True
        elif argv[i] == "--info":
            printInfo = True
        else:
            print("WARNING unknown argument " + argv[i])
    return copy2plugin, copy2mobile, printInfo


def collect_model_and_properties_paths():
    plugins_without_aot_cot = ['dependance', 'lit', 'carto', 'berge', 'reglementary', 'vegetation']
    ecore_paths = []
    properties_paths = []

    #core paths
    ecore_paths.append("../sirs-core/model/sirs.ecore")
    properties_paths.append("../sirs-core/src/main/resources/fr/sirs/core/model/*")
    #plugin aot-cot paths
    ecore_paths.append("../plugin-aot-cot/model/aot_cot.ecore")
    properties_paths.append("../plugin-aot-cot/src/main/resources/fr/sirs/core/model/*")
    #other plugins paths
    for plugin in plugins_without_aot_cot:
        ecore_paths.append("../plugin-" + plugin + "/model/" + plugin + ".ecore")
        properties_paths.append("../plugin-" + plugin + "/src/main/resources/fr/sirs/core/model/*")
    return ecore_paths, properties_paths

class WrapAttribute(object):
    def __init__(self, clazz, attribute: Ecore.EAttribute, labels):
        self.name = attribute.name
        self.type = attribute.eType.name
        try:
            self.label = labels.to_label(clazz, attribute.name)
        except KeyError:
            self.label = ""
        self.reference = False
        self.min = 0 if attribute.eType.name == "EFloat" or attribute.eType.name == "EInt" else None


class WrapReference(object):
    def __init__(self, clazz, reference: Ecore.EReference, labels):
        self.name = reference.name
        self.type = reference.eType.name
        try:
            self.label = labels.to_label(clazz, reference.name)
        except KeyError:
            self.label = ""
        self.reference = True
        self.multiple = reference.upper
        self.containment = reference.containment


def build_preferences(formTemplatePilote, preferences):
    for clazz in formTemplatePilote:
        preferences[clazz] = {"attributes": {}}
        for attribute in formTemplatePilote[clazz]:
            preferences[clazz]["attributes"][attribute] = True


def is_child_of(classifier: Ecore.EClassifier, parentName):
    if classifier.name == parentName:
        return True
    if type(classifier) == Ecore.EClass or type(classifier) == Ecore.EProxy:
        super_types: Ecore.EOrderedSet = classifier.eAllSuperTypes()
        for st in super_types:
            if is_child_of(st, parentName):
                return True
    return False


def find_containment_classes(classifier: Ecore.EClassifier):
    if type(classifier) == Ecore.EClass or type(classifier) == Ecore.EProxy:
        super_types: Ecore.EOrderedSet = classifier.eAllSuperTypes()
        for st in super_types:
            for super_c in find_containment_classes(st):
                yield super_c
    content = list(classifier.eAllContents())
    for obj in content:
        try:
            if obj.containment:
                yield obj.eType.name
        except AttributeError:
            pass


def transform(classifier: Ecore.EClassifier, result, clazz, ll):
    if type(classifier) == Ecore.EClass or type(classifier) == Ecore.EProxy:
        super_types: Ecore.EOrderedSet = classifier.eAllSuperTypes()
        for st in super_types:
            transform(st, result, clazz, ll)
        content = list(classifier.eAllContents())
        for obj in content:
            if type(obj) == Ecore.EAttribute:
                a = WrapAttribute(clazz, obj, ll)
                result[clazz][a.name] = a.__dict__
            if type(obj) == Ecore.EReference:
                r = WrapReference(clazz, obj, ll)
                result[clazz][r.name] = r.__dict__


def filter_by_class(object_pilote, classes):
    return {k: v for k, v in object_pilote.items() if k in classes}


def filter_by_attribute(object_pilote, without_those_attributes):
    return {k: {k2: v2 for k2, v2 in v.items() if k2 not in without_those_attributes} for k, v in object_pilote.items()}


def build_form_template_for_mobile(original_pilot):
    filtered_by_class = filter_by_class(original_pilot, FORM_TEMPLATE_MOBILE_CLASS)
    filtered_by_attribute = filter_by_attribute(filtered_by_class, NO_FORM_TEMPLATE_MOBILE_ATTRIBUTE)
    #don't touch the header indentation here!
    header = '''/* eslint-disable no-console */
/**
 * Object used by the component BaseForm, a generic component for building edit forms.
 *
 * /!\I'm a script generated by SIRS DESKTOP! Take a look at the folder 'SirsMobileAndPluginQgis' within the desktop project.
 */
export const formTemplatePilote = '''

    with open("form-template-pilote.ts", "w") as ftp:
        dump = json.dumps(filtered_by_attribute, indent=4, ensure_ascii=False)
        ftp.write(header + dump)
        print("INFO form-template-pilote.ts imprimé avec succés")


def print_information(positionables, containment_classes):
    print("INFO All generated positionable classes of the plugin Qgis")
    for p in positionables:
        print("\t" + p)
    print("INFO All generated containment classes from positionable classes")
    containment_classes_list = list(containment_classes)
    containment_classes_list.sort()
    for cc in containment_classes_list:
        print("\t" + cc)


def copy_to_plugin():
    try:
        shutil.copy("formTemplatePilote.json", QGIS_PLUGIN_PROJECT_PATH)
        shutil.copy("user_preference_correspondence.json", QGIS_PLUGIN_PROJECT_PATH)
        print("INFO formTemplatePilote.json et user_preference_correspondence.json copiés avec succés dans " + QGIS_PLUGIN_PROJECT_PATH)
    except TypeError or FileNotFoundError:
        print("ERROR Impossible de copier les fichiers générés vers le plugin QGIS. Vérifiez que la variable QGIS_PLUGIN_PROJECT_PATH est un chemin valide.")


def copy_to_mobile():
    try:
        path = SIRS_MOBILE_PROJECT_PATH
        if path[-1] == "/":
            path = path[:-1]
        path = path + "/src/app/utils"
        shutil.copy("form-template-pilote.ts", path)
        print("INFO form-template-pilote.ts copiés avec succés dans " + path)
    except TypeError or FileNotFoundError:
        print("ERROR Impossible de copier le fichier form-template-pilote.ts dans le projet Sirs mobile. Vérifiez que la variable SIRS_MOBILE_PROJECT_PATH est un chemin valide.")


def engine(formTemplatePilote, resourceSet, ecore_paths, properties_paths):
    for i in range(len(ecore_paths)):
        #paths
        path_to_ecore = ecore_paths[i]
        path_to_properties = properties_paths[i]
        #ecore diagrams
        resource = resourceSet.get_resource(URI(path_to_ecore))
        graphMMRoot = resource.contents[0]  # We get the root (an EPackage here)
        #labels
        ll = LabelLoader()
        ll.complete_dict(path_to_properties)

        for clazz in ll.classes:
            #retrieve the corresponding classifier from the graph
            classifier: Ecore.EClassifier = graphMMRoot.getEClassifier(clazz)
            if classifier is None:
                print(f"WARNING The class {clazz} has a file .properties but its model is not found in {path_to_ecore}")
                continue
            #complete the formTemplatePilote object
            formTemplatePilote[clazz] = {}
            transform(classifier, formTemplatePilote, clazz, ll)
            #check if the current class is positionable (implement WithGeometrie and not abstract)
            if is_child_of(classifier, "AvecGeometrie") and not classifier.abstract:
                all_positionable.append(clazz)
                for c in find_containment_classes(classifier):
                    containment_classes_from_positionable.add(c)


def print_files(formTemplatePilote, preferences):
    with open("formTemplatePilote.json", "w") as ftp:
        dump = json.dumps(formTemplatePilote, indent=2, ensure_ascii=False)
        ftp.write(dump)
        print("INFO formTemplatePilote.json imprimé avec succés")
    with open("user_preference_correspondence.json", "w") as lc:
        dump = json.dumps(preferences, indent=2, ensure_ascii=False)
        lc.write(dump)
        print("INFO user_preference_correspondence.json imprimé avec succés")

    # Construction et impression du fichier mobile
    build_form_template_for_mobile(formTemplatePilote)


if __name__ == "__main__":
    # Récupération des arguments
    copy2plugin, copy2mobile, printInfo = check_argument()

    # Initialisation
    formTemplatePilote = {}
    preferences = {}
    global_registry[Ecore.nsURI] = Ecore  # We load the Ecore metamodel first
    rset = ResourceSet()

    # Collecte des chemins vers les modeles et les fichiers de propriétés
    ecore_paths, properties_paths = collect_model_and_properties_paths()

    # Construction de l'objet formTemplatePilote
    engine(formTemplatePilote, rset, ecore_paths, properties_paths)

    # Construction du ficher user_preference_correspondance.json
    build_preferences(formTemplatePilote, preferences)
    update_style_from_data(preferences)

    # Impression des fichiers de sortie
    print_files(formTemplatePilote, preferences)

    # Copie des fichiers de sortie
    if copy2plugin:
        copy_to_plugin()
    if copy2mobile:
        copy_to_mobile()

    # Impression des informations
    if printInfo:
        print_information(all_positionable, containment_classes_from_positionable)
