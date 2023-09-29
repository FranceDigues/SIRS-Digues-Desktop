##################################################
# Script used to add a certificate to the SIRS JRE cacerts file
#################################################

# Entrez le chemin pour accéder au projet SIRS ici
pathToProject=/opt/SIRS2

# Ne pas éditer le reste
# -----------------------------------------------------------------------

while true; do
    read -p "Confirmer le chemin d'accès au SIRS : $pathToProject (O/[N] " yn
    case $yn in
        [Oo]* ) break;;
        [Nn]* ) {
          echo "Veuillez mettre à jour le chemin d'accès directement dans le script $0"
          exit
        };;
        * ) echo "Veuillez répondre oui (O) ou non (N).";;
    esac
done

read -p "Entrez le chemin d'acces du certificat:  " pathToCertificate

read -p "Entrez un alias pour le certificat:  " alias

# Path to cacerts file relative to the project folder.
relativePathToCacerts=/runtime/lib/security/cacerts

sudo keytool -importcert -noprompt -trustcacerts -alias $alias -file $pathToCertificate -keystore $pathToProject$relativePathToCacerts -storepass changeit
