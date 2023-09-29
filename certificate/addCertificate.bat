@echo off

Rem -----------------------------------------
Rem Script used to add a certificate to the SIRS JRE cacerts file
Rem -----------------------------------------

Rem    Entrez le chemin pour accéder au projet SIRS ici
set pathToProject="C:\Program Files\SIRS2"


Rem    Ne pas éditer le reste
Rem    -----------------------------------------------------------------------

:CHECKPERMISSION
        openfiles >nul 2>&1
        IF %ERRORLEVEL% NEQ 0 (
                ECHO Erreur: Veuillez executer ce script en tant qu'administrateur.
                GOTO END
        )

:CHECKPROJECTPATH
Rem Ask the user to verify
set /p isCorrectPath="Confirmez le chemin d'acces au SIRS : %pathToProject% (O/[N])? "

if /I "%isCorrectPath%" EQU "N" (
    ECHO Veuillez mettre a jour le chemin d'acces directement dans le script %0.
    GOTO END
) else (
	IF /I "%isCorrectPath%" NEQ "O" (
		Rem Input not valid
		ECHO "Veuillez répondre oui (O) ou non (N)."
		GOTO CHECKPROJECTPATH
	)
)

set /p pathToCertificate="Entrez le chemin d'acces du certificat: "
set /p alias="Entrez un alias pour le certificat: "

Rem Path to cacerts file relative to the project folder.
set relativePathToCacerts=\runtime\lib\security\cacerts

keytool -importcert -noprompt -trustcacerts -alias %alias% -file %pathToCertificate% -keystore %pathToProject%%relativePathToCacerts% -storepass changeit

:END

pause
