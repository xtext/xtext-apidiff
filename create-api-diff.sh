#!/bin/bash

cd xtext-apidiff
./gradlew clean build
cd ..

if [ -z "$NEW_VERSION" ]; then
  # if not set in environment use default
  NEW_VERSION=2.20.0
fi

if [ -z "$OLD_VERSION" ]; then
  # if not set in environment use default
  OLD_VERSION=2.19.0
fi

echo "Diffing $NEW_VERSION against $OLD_VERSION"

if [ -f japicmp-ext.jar ];
then
  rm japicmp-ext.jar
fi
cp xtext-apidiff/build/libs/japicmp-ext.jar .

if [ ! -d eclipse ];
then
   echo "Downloading Eclipse"
   # Get basic Eclipse SDK
   curl -m 600 -sL 'http://download.eclipse.org/eclipse/downloads/drops4/R-4.12-201906051800/eclipse-SDK-4.12-linux-gtk-x86_64.tar.gz' --output 'eclipse-SDK-4.12-linux-gtk-x86_64.tar.gz' && tar -zxf eclipse-SDK-4.12-linux-gtk-x86_64.tar.gz
   rm eclipse-SDK-4.12-linux-gtk-x86_64.tar.gz
   # Install additional features: Xtext and dependent
   eclipse/eclipse -data eclipse/.director-ws -consolelog -noSplash -clean \
   -application org.eclipse.equinox.p2.director \
   -metadataRepository http://download.eclipse.org/modeling/tmf/xtext/updates/releases/2.19.0/,http://download.eclipse.org/releases/2019-09,http://download.eclipse.org/lsp4j/updates/releases/,https://download.eclipse.org/tools/orbit/downloads/2019-09 \
   -artifactRepository http://download.eclipse.org/modeling/tmf/xtext/updates/releases/2.19.0/,http://download.eclipse.org/releases/2019-09,http://download.eclipse.org/lsp4j/updates/releases/,https://download.eclipse.org/tools/orbit/downloads/2019-09 \
   -installIU org.eclipse.xtext.sdk.feature.group,org.eclipse.lsp4j.sdk.feature.group,org.eclipse.m2e.core,org.kohsuke.args4j \
   -destination eclipse
fi

if [ ! -d tmf-xtext-Update-2.17.0 ];
then
   echo "Downloading Xtext 2.17.0"
   curl -m 600 -sL 'download.eclipse.org/modeling/tmf/xtext/downloads/drops/2.17.0/R201903041445/tmf-xtext-Update-2.17.0.zip' --output 'tmf-xtext-Update-2.17.0.zip' && unzip -q tmf-xtext-Update-2.17.0.zip -d tmf-xtext-Update-2.17.0
   rm tmf-xtext-Update-2.17.0.zip
fi

if [ ! -d tmf-xtext-Update-2.17.1 ];
then
   echo "Downloading Xtext 2.17.1"
   curl -m 600 -sL 'download.eclipse.org/modeling/tmf/xtext/downloads/drops/2.17.1/R201904030733/tmf-xtext-Update-2.17.1.zip' --output 'tmf-xtext-Update-2.17.1.zip' && unzip -q tmf-xtext-Update-2.17.1.zip -d tmf-xtext-Update-2.17.1
   rm tmf-xtext-Update-2.17.1.zip
fi

if [ ! -d tmf-xtext-Update-2.18.0 ];
then
   echo "Downloading Xtext 2.18.0"
   curl -m 600 -sL 'download.eclipse.org/modeling/tmf/xtext/downloads/drops/2.18.0/R201906031516/tmf-xtext-Update-2.18.0.zip' --output 'tmf-xtext-Update-2.18.0.zip' && unzip -q tmf-xtext-Update-2.18.0.zip -d tmf-xtext-Update-2.18.0
   rm tmf-xtext-Update-2.18.0.zip
fi

if [ ! -d tmf-xtext-Update-2.19.0 ];
then
   echo "Downloading Xtext 2.19.0"
   curl -m 600 -sL 'download.eclipse.org/modeling/tmf/xtext/downloads/drops/2.19.0/R201909021322/tmf-xtext-Update-2.19.0.zip' --output 'tmf-xtext-Update-2.19.0.zip' && unzip -q tmf-xtext-Update-2.19.0.zip -d tmf-xtext-Update-2.19.0
   rm tmf-xtext-Update-2.19.0.zip
fi

if [ -d tmf-xtext-Update-2.20.0 ];
then
  rm -r tmf-xtext-Update-2.20.0
fi
echo "Downloading Xtext 2.20.0"
# TODO make this better and faster. am not sure if we can guess the nightly number. or ....
# ideally download official nightly
curl -m 600 --retry 5 -sL 'https://ci.eclipse.org/xtext/job/xtext-umbrella/job/master/lastStableBuild/artifact/build/org.eclipse.xtext.sdk.p2-repository-2.20.0-SNAPSHOT.zip' --output 'tmf-xtext-Update-2.20.0.zip' && unzip -q tmf-xtext-Update-2.20.0.zip -d tmf-xtext-Update-2.20.0
rm tmf-xtext-Update-2.20.0.zip

# update apicmp.properties
{
	echo "old.version=${OLD_VERSION}"
	echo "new.version=${NEW_VERSION}"
	echo "old.location=tmf-xtext-Update-${OLD_VERSION}/plugins/"
	echo "new.location=tmf-xtext-Update-${NEW_VERSION}/plugins/"
	echo "cpLocation=eclipse/plugins"

} > japicmp.properties

if [ -d output ];
then
  rm -r output
fi
echo "creating the actual diff"
java -jar japicmp-ext.jar