#!/bin/bash

#########################################
#
# A script to release a tag on GIT for the project in which the current project resides.
#
# Folder structure should be : 
#
#	- My_GIT_project/
#		--> build/
#			--> rename.xml
#			--> releaseScript.sh
#		--> pom.xml
#		--> ...
#
#	TODO : Error management : revert rename.xml and delete temp release branch if a failure happen.
#
########################################



############## DEFAULT VALUES TO USE FOR RELEASE VERSION 								 ##############
############## WARNING : DO NOT AFFECT THOSE VARIABLES IN ANOTHER PLACE OF THIS SCRIPT ! ##############
defaultMinor=46
defaultMajor=2



# DEFINE VARIABLES TO STORE PARAMETERS
releaseMinor=
releaseMajor=

# RELEASE VERSION : COMPLETE STRING COMPOSED OF MAJOR AND MINOR variables. 
releaseVersion=

# The name of the file in which we put tag name (so an automatic script can checkout tag by doing cat lastTag.tmp|git checkout)
lastTagFile="lastTag.tmp"

# TODO : add a case to change SNAPSHOT version after release.
oldSnaphot=
newSnapshot=

# A prefix for the temporary release branch
releaseBranchPrefix="release-"

# Character sequence to replace with release version in rename.xml ant file.
renameToReplace="_RELEASE_VERSION_"

# Extension used to back up rename.xml file, so we can revert it to its original state at the end of the job.
backupExtension=".bak"

# Ant script which will replace version in pom.xml files.
renameScriptFile="rename.xml"

# Keep original branch to come back to it after release.
originalBranch=

# Original code from 'http://stackoverflow.com/questions/242538/unix-shell-script-find-out-which-directory-the-script-file-resides'
# Absolute path to this script, e.g. /home/user/bin/foo.sh
SCRIPT=$(readlink -f "$BASH_SOURCE")
# Absolute path this script is in, thus /home/user/bin
SCRIPT_PATH=$(dirname "$SCRIPT")

###################### FUNCTIONS ############################

# Print help on standard output.
function printHelp {
	echo "
	Create a release tag for the current project. Release number will be something like X.x where X is major release version, and x is minor.
	"
	echo  "	WARNING : No check is done to ensure the project is well-formed (no compilation, test, syntax check, etc.)
	"
	echo  "	Arguments (all optional) : "
	echo  "	-release.minor : Minor version to set on new release. If not set, a default value will be used and incremented at the end of the script."
	echo  "	-release.major : Major version to set on new release. If not set, a default value will be used BUT NOT INCREMENTED."
	#etc.
	echo "	-- help, -h : Print this help."
}

function execute {
	# We place ourself in the script directory. It MUST BE into the git project directory.
	cd $SCRIPT_PATH

	# Save original branch name to go back to it at the end of the release.
	originalBranch="$(git symbolic-ref --short HEAD)"
	echo "Original branch is $originalBranch"
	createTag
	incrementDefaultValues
}

function createTag {
	echo "Create temporary branch for release $releaseVersion"
	tmpReleaseBranch="$releaseBranchPrefix$releaseVersion"
	git branch $tmpReleaseBranch
	git checkout $tmpReleaseBranch

	echo "Prepare script for version update"
	sed -i$backupExtension "s/$renameToReplace/$releaseVersion/g" $renameScriptFile

	echo "Perform version update"
	ant -f $renameScriptFile

	echo "Revert version script to its original state."
 	mv -f $renameScriptFile$backupExtension $renameScriptFile 

	echo "Commit release versions"
	git commit -m "Project release $releaseVersion" -a

	echo "Create tag named $releaseVersion"
	git tag $releaseVersion $tmpReleaseBranch
	echo "$releaseVersion" > "$lastTagFile"

	echo "Remove temporary Release branch"
	git checkout $originalBranch
	git branch -D $tmpReleaseBranch
}

# Increment and rewrite default values at the end of the script, when release has been done.
function incrementDefaultValues {
	newReleaseMinor=$(($releaseMinor+1))
	sed -r -i$backupExtension "s/defaultMinor=[[:digit:]]+/defaultMinor=$newReleaseMinor/" $SCRIPT
	sed -r -i$backupExtension "s/defaultMajor=[[:digit:]]+/defaultMajor=$releaseMajor/" $SCRIPT

	# TODO : commit script update on master branch
	git commit -m "Prepare release script for next build." -a
}

########################### MAIN ############################# 

# ARGUMENT CHECK
while [ "$1" != "" ]; do
    case $1 in
	-release.minor )	shift
			releaseMinor=$1
			;;
	-release.major )	shift
			releaseMajor=$1
			;;
	# etc.
        -h | --help )	printHelp
                        exit
                        ;;
        * )             printHelp
                        exit 1
				;;
    esac
    shift
done


if [ -z "$releaseMinor" ]
	then 
	releaseMinor=$defaultMinor
fi

if [ -z "$releaseMajor" ]
	then 
	releaseMajor=$defaultMajor
fi

# A regular expression to check if given release versions are integers.
re='^[0-9]+$'

if ! [[ $releaseMinor =~ $re ]]
	then echo "ERROR: MINOR RELEASE VERSION IS NOT AN INTEGER. FOUND VALUE --> $releaseMinor" >&2; exit 1
fi

if ! [[ $releaseMajor =~ $re ]]
	then echo "ERROR: MAJOR RELEASE VERSION IS NOT AN INTEGER. FOUND VALUE --> $releaseMajor" >&2; exit 1
fi

releaseVersion="$releaseMajor.$releaseMinor"

# We must ensure that not any tag already exists with the same name.
tagDoublon="$(git tag -l|grep $releaseVersion)"

if [ ! -z "$tagDoublon" ]
	then echo "ERROR : A tag already exists for the following version : $releaseVersion" >&2; exit 1
fi

echo "Execute for version $releaseVersion"

# Make release
execute
