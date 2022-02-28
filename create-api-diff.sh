#!/bin/bash
# Use the build container for testing to avoid OS specific interpretation:
# docker run -it -v $(pwd):/xtext -w /xtext eclipsecbi/jiro-agent-centos-7 bash

declare -A VERSION_2_BUILDID
VERSION_2_BUILDID["2.26.0"]="R202202280901"
VERSION_2_BUILDID["2.25.0"]="R202103011429"
VERSION_2_BUILDID["2.24.0"]="R202011301016"

VERSIONS=(2.26.0 2.25.0 2.24.0 2.23.0 2.22.0 2.21.0 2.20.0 2.19.0 2.18.0 2.17.1 2.7.0)
BUILD_IDS=(R202202280901\
 R202103011429\
 R202011301016)
DEV_VERSION=$(curl -sS https://raw.githubusercontent.com/eclipse/xtext-core/master/gradle/versions.gradle |grep -Po "version = \'\K([^\']*)(?=\')" |sed 's/-SNAPSHOT//')

# The Eclipse release to use
# https://download.eclipse.org/eclipse/downloads/drops4/R-4.21-202109060500/download.php?dropFile=eclipse-SDK-4.21-linux-gtk-x86_64.tar.gz
ECLIPSE_RELEASE=2021-09
ECLIPSE_TARGZ_FILE=eclipse-SDK-4.21-linux-gtk-x86_64.tar.gz
ECLIPSE_TARGZ_DOWNLOAD_URL=http://download.eclipse.org/eclipse/downloads/drops4/R-4.21-202109060500/$ECLIPSE_TARGZ_FILE
ECLIPSE_XTEXT_VERSION=${VERSIONS[0]}



if [ -z "$NEW_VERSION" ]; then
  # if not set in environment use default
  NEW_VERSION=$DEV_VERSION
fi

if [ -z "$OLD_VERSION" ]; then
  # if not set in environment use default
  OLD_VERSION=${VERSIONS[0]}
fi

echo "Diffing $NEW_VERSION against $OLD_VERSION"
echo ""
echo "Current Xtext dev version is $DEV_VERSION"

if [ ! -d eclipse ]; then
   echo "Downloading Eclipse"
   # Get basic Eclipse SDK
   curl -m 1200 -sL $ECLIPSE_TARGZ_DOWNLOAD_URL --output $ECLIPSE_TARGZ_FILE && tar -zxf $ECLIPSE_TARGZ_FILE
   rm $ECLIPSE_TARGZ_FILE

   echo "Installing additional features: Xtext and dependent"
   eclipse/eclipse -data eclipse/.director-ws -consolelog -noSplash -clean \
   -application org.eclipse.equinox.p2.director \
   -metadataRepository http://download.eclipse.org/modeling/tmf/xtext/updates/releases/$ECLIPSE_XTEXT_VERSION,https://download.eclipse.org/modeling/emft/mwe/updates/releases,http://download.eclipse.org/releases/$ECLIPSE_RELEASE,https://download.eclipse.org/lsp4j/updates/releases/0.12.0/,https://download.eclipse.org/tools/orbit/downloads/$ECLIPSE_RELEASE \
   -artifactRepository http://download.eclipse.org/modeling/tmf/xtext/updates/releases/$ECLIPSE_XTEXT_VERSION,https://download.eclipse.org/modeling/emft/mwe/updates/releases,http://download.eclipse.org/releases/$ECLIPSE_RELEASE,https://download.eclipse.org/lsp4j/updates/releases/0.12.0/,https://download.eclipse.org/tools/orbit/downloads/$ECLIPSE_RELEASE \
   -installIU org.eclipse.xtext.sdk.feature.group,org.eclipse.lsp4j.sdk.feature.group,org.eclipse.m2e.core,org.eclipse.buildship.core,org.kohsuke.args4j \
   -destination eclipse
fi

download () {
   XTEXT_VERSION=$1
   DOWNLOAD_URL=$2
   ZIP_FILE=tmf-xtext-Update-$XTEXT_VERSION.zip

   if [ ! -d tmf-xtext-Update-$XTEXT_VERSION ]; then
      echo "Downloading Xtext $XTEXT_VERSION from $DOWNLOAD_URL"
      # check existence of file
      if [ $(curl -sS -I -o /dev/null -w '%{http_code}' $DOWNLOAD_URL) == 404 ]; then
         echo "Not found: $DOWNLOAD_URL"
         exit 1
      fi
      curl -m 1200 -sL $DOWNLOAD_URL --output $ZIP_FILE
      unzip -q $ZIP_FILE -d tmf-xtext-Update-$XTEXT_VERSION
      rm $ZIP_FILE
   fi
}

# Download latest Xtext build
if [ -d tmf-xtext-Update-$DEV_VERSION ]; then
  rm -r tmf-xtext-Update-$DEV_VERSION
fi
DOWNLOAD_URL=https://ci.eclipse.org/xtext/job/xtext-umbrella/job/master/lastStableBuild/artifact/build/org.eclipse.xtext.sdk.p2-repository-$DEV_VERSION-SNAPSHOT.zip
download $DEV_VERSION $DOWNLOAD_URL

# download NEW_VERSION and OLD_VERSION if not present
for ((idx=0; idx<${#BUILD_IDS[@]}; ++idx)); do
   VERSION=${VERSIONS[idx]}
   BUILD_ID=${BUILD_IDS[idx]}

   # only download relevant versions
   if [ $VERSION == $NEW_VERSION ] || [ $VERSION == $OLD_VERSION ]; then
      download $VERSION "https://download.eclipse.org/modeling/tmf/xtext/downloads/drops/$VERSION/$BUILD_ID/tmf-xtext-Update-$VERSION.zip"
   fi
done



echo ""
echo "Creating the actual diff"

# update apicmp.properties
{
	echo "old.version=${OLD_VERSION}"
	echo "new.version=${NEW_VERSION}"
	echo "old.location=tmf-xtext-Update-${OLD_VERSION}/plugins/"
	echo "new.location=tmf-xtext-Update-${NEW_VERSION}/plugins/"
	echo "cpLocation=eclipse/plugins"

} > japicmp.properties

rm -rf output
java -jar japicmp-ext.jar