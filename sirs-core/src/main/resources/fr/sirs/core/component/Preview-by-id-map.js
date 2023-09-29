/* global doc, key */

function(doc) {
    // On transmet le document, puis on va parcourir l'ensemble de ses clefs pour trouver les élements contenus.
    if (doc['@class'] && doc._id !== "$sirs") {
        emitDocument(doc);
        var objectKeys = Object.keys(doc);
        for (key in objectKeys) {
            // Si on a un attribut de type tableau ou objet, on recherche un element ou une collection d'élements à l'interieur.
            var attr = doc[objectKeys[key]];
            if (Array.isArray(attr)) {
                parseTab(doc, attr);
            }
            else if (typeof attr === "object") {
                parseObject(doc, attr);
            }
        }
    }

    /**
     * Si le contenu de field correspond à l'ID recherché, pour un document.
     */
    function emitDocument(object) {
        if (object['@class'] && object._id !== "$sirs") {
            var label;
            if (object.nom)
                label = object.nom;
            else if (object.libelle)
                label = object.libelle;
            else if (object.login)
                label = object.login;
            emit(object._id, {docId: object._id, docClass: object['@class'], elementId: object._id, elementClass: object['@class'], author: object.author, valid: object.valid, designation: object.designation, libelle: label, date_fin: object.date_fin});
        }
    }

    /**
     * Si le contenu de field correspond à l'ID recherché, pour un élément non document.
     */
    function emitInnerElement(docu, object) {
        if (object['@class'] && object._id !== "$sirs") {
            var label;
            if (object.nom)
                label = object.nom;
            else if (object.libelle)
                label = object.libelle;
            else if (object.login)
                label = object.login;
            emit(object.id, {docId: docu._id, docClass: docu['@class'], elementId: object.id, elementClass: object['@class'], author: object.author, valid: object.valid, designation: object.designation, libelle: label});
        }
    }

    /**
     * Analyse un tableau, avec l'ID de son élément le plus proche.
     */
    function parseTab(docu, tab) {
        for (var i = 0; i < tab.length; i++) {
            if (Array.isArray(tab[i])) {
                parseTab(docu, tab[i]);
            }
            else if (typeof tab[i] === "object") {
                parseObject(docu, tab[i]);
            }
        }
    }

    /**
     * Analyse un élément.
     */
    function parseObject(docu, object) {
        if (object['@class'] && object._id !== "$sirs") {
            // On transmet le document, puis on va parcourir l'ensemble de ses clefs pour trouver les élements contenus.
            emitInnerElement(docu, object);
            var objectKeys = Object.keys(object);
            for (key1 in objectKeys) {
                // Si c'est un objet, il faut procéder récursivement
                var attr = object[objectKeys[key1]];
                if (Array.isArray(attr)) {
                    parseTab(docu, attr);
                }
                else if (typeof attr === "object") {
                    parseObject(docu, attr);
                }
            }
        }
    }
}