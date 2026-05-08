#!/bin/bash
# Use the build container for testing to avoid OS specific interpretation:
# docker run -it -v $(pwd):/xtext -w /xtext eclipsecbi/jiro-agent-centos-8 bash

set -Eeuo pipefail

LOG_DIR=${LOG_DIR:-logs}
mkdir -p "$LOG_DIR"

declare -A VERSION_2_BUILDID
VERSION_2_BUILDID["2.42.0"]=""
VERSION_2_BUILDID["2.41.0"]=""
VERSION_2_BUILDID["2.40.0"]=""
VERSION_2_BUILDID["2.39.0"]=""
VERSION_2_BUILDID["2.38.0"]=""
VERSION_2_BUILDID["2.37.0"]=""
VERSION_2_BUILDID["2.36.0"]=""
VERSION_2_BUILDID["2.35.0"]=""
VERSION_2_BUILDID["2.34.0"]=""
VERSION_2_BUILDID["2.33.0"]=""
VERSION_2_BUILDID["2.32.0"]=""
VERSION_2_BUILDID["2.31.0"]=""
VERSION_2_BUILDID["2.30.0"]="R202302271344"
VERSION_2_BUILDID["2.29.0"]="R202211211054"
VERSION_2_BUILDID["2.28.0"]="R202208290555"
VERSION_2_BUILDID["2.27.0"]="R202205300508"
VERSION_2_BUILDID["2.26.0"]="R202202280901"
VERSION_2_BUILDID["2.25.0"]="R202103011429"
VERSION_2_BUILDID["2.24.0"]="R202011301016"

VERSIONS=(2.42.0 2.41.0 2.40.0 2.39.0 2.38.0 2.37.0 2.36.0 2.35.0 2.34.0 2.33.0 2.32.0 2.31.0 2.30.0 2.29.0 2.28.0 2.27.0 2.26.0 2.25.0 2.24.0 2.23.0 2.22.0 2.21.0 2.20.0 2.19.0 2.18.0 2.17.1 2.7.0)

if [ -z "${DEV_VERSION:-}" ]; then
   echo "Using fallback to retrieve DEV_VERSION"
   DEV_VERSION=$(curl -sS https://raw.githubusercontent.com/eclipse/xtext/main/pom.xml|grep -Po "([0-9]+\.[0-9]+\.[0-9]+)-SNAPSHOT" |sed 's/-SNAPSHOT//')
fi

# The Eclipse release to use
# https://download.eclipse.org/eclipse/downloads/drops4/R-4.31-202402290520/download.php?dropFile=eclipse-SDK-4.31-linux-gtk-x86_64.tar.gz
ECLIPSE_RELEASE=2024-03
ECLIPSE_TARGZ_FILE=eclipse-SDK-4.31-linux-gtk-x86_64.tar.gz
ECLIPSE_TARGZ_DOWNLOAD_URL=https://download.eclipse.org/eclipse/downloads/drops4/R-4.31-202402290520/$ECLIPSE_TARGZ_FILE
ECLIPSE_XTEXT_VERSION=${VERSIONS[0]}



if [ -z "${NEW_VERSION:-}" ]; then
  # if not set in environment use default
  NEW_VERSION=$DEV_VERSION
fi

if [ -z "${OLD_VERSION:-}" ]; then
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
   -metadataRepository https://download.eclipse.org/modeling/tmf/xtext/updates/releases/$ECLIPSE_XTEXT_VERSION,https://download.eclipse.org/modeling/emft/mwe/updates/releases/2.25.0,https://download.eclipse.org/releases/$ECLIPSE_RELEASE,https://download.eclipse.org/lsp4j/updates/releases/1.0.0/,https://download.eclipse.org/tools/orbit/downloads/$ECLIPSE_RELEASE \
   -artifactRepository https://download.eclipse.org/modeling/tmf/xtext/updates/releases/$ECLIPSE_XTEXT_VERSION,https://download.eclipse.org/modeling/emft/mwe/updates/releases/2.25.0,https://download.eclipse.org/releases/$ECLIPSE_RELEASE,https://download.eclipse.org/lsp4j/updates/releases/1.0.0/,https://download.eclipse.org/tools/orbit/downloads/$ECLIPSE_RELEASE \
   -installIU org.eclipse.xtext.sdk.feature.group,org.eclipse.lsp4j.sdk.feature.group,org.eclipse.m2e.core,org.eclipse.buildship.core,org.kohsuke.args4j,org.eclipse.draw2d \
   -destination eclipse
fi

download () {
    local XTEXT_VERSION=$1
    local DOWNLOAD_URL=$2
    local ZIP_FILE=tmf-xtext-Update-$XTEXT_VERSION.zip

    # For DEV version, check if already copied by Jenkins copyartifact plugin
    if [[ "$XTEXT_VERSION" == "$DEV_VERSION" ]]; then
        local pre_copied="org.eclipse.xtext.p2repository-${XTEXT_VERSION}-SNAPSHOT.zip"
        if [ -f "$pre_copied" ]; then
            echo "Using pre-copied artifact: $pre_copied"
            unzip -oq "$pre_copied" -d "tmf-xtext-Update-${XTEXT_VERSION}"
            return 0
        fi
    fi

    if [ ! -d tmf-xtext-Update-$XTEXT_VERSION ]; then
        echo "Downloading Xtext $XTEXT_VERSION from $DOWNLOAD_URL"
        local DOWNLOAD_LOG_PREFIX="$LOG_DIR/download-$XTEXT_VERSION"
        local HTTP_CODE=$(curl -m 1200 -sS -L \
            -D "$DOWNLOAD_LOG_PREFIX.headers" \
            -o "$ZIP_FILE" \
            -w '%{http_code}' \
            "$DOWNLOAD_URL")
        {
            echo "url=$DOWNLOAD_URL"
            echo "http_code=$HTTP_CODE"
        } > "$DOWNLOAD_LOG_PREFIX.meta"
        if [ "$HTTP_CODE" != "200" ]; then
            echo "Unexpected HTTP $HTTP_CODE: $DOWNLOAD_URL"
            echo "Response headers were written to $DOWNLOAD_LOG_PREFIX.headers"
            echo "Response body starts with:"
            sed -n '1,40p' "$ZIP_FILE"
            exit 1
        fi
        if ! unzip -t $ZIP_FILE > /dev/null 2>&1; then
            echo "Downloaded file is not a valid zip: $DOWNLOAD_URL"
            rm -f $ZIP_FILE
            exit 1
        fi
        unzip -oq $ZIP_FILE -d tmf-xtext-Update-$XTEXT_VERSION
        rm $ZIP_FILE
    fi
}

# Download latest Xtext build
if [ -d tmf-xtext-Update-$DEV_VERSION ]; then
  rm -r tmf-xtext-Update-$DEV_VERSION
fi
DOWNLOAD_URL=https://ci.eclipse.org/xtext/job/xtext/job/main/lastStableBuild/artifact/build/org.eclipse.xtext.p2repository-$DEV_VERSION-SNAPSHOT.zip
download $DEV_VERSION $DOWNLOAD_URL

# download NEW_VERSION and OLD_VERSION if not present
for VERSION in "${VERSIONS[@]}"; do
   # only download relevant versions
   if [ "$VERSION" == "$NEW_VERSION" ] || [ "$VERSION" == "$OLD_VERSION" ]; then
      # Skip DEV_VERSION as it's handled above
      if [ "$VERSION" == "$DEV_VERSION" ]; then
         continue
      fi

      BUILD_ID=${VERSION_2_BUILDID[$VERSION]:-}
      if [ -z "$BUILD_ID" ]; then
         download "$VERSION" "https://download.eclipse.org/modeling/tmf/xtext/downloads/drops/$VERSION/tmf-xtext-Update-$VERSION.zip"
      else
         download "$VERSION" "https://download.eclipse.org/modeling/tmf/xtext/downloads/drops/$VERSION/$BUILD_ID/tmf-xtext-Update-$VERSION.zip"
      fi
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

echo "using japicmp.properties"
cat japicmp.properties
echo ""
echo "calling java -Xmx2G -jar japicmp-ext.jar"

rm -rf output
java -Xmx2G -jar japicmp-ext.jar
