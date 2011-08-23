#!/bin/sh
#
# build Osm2GpsMid & GpsMid midlets for signing
#

numver=0.7.7
ver=$numver-map69
map=Berlin

ant clean
#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 

ant
cd Osm2GpsMid
ant clean
ant

for target in full editing blackberry minimal
do
  java -Xmx1024m -jar dist/Osm2GpsMid-$ver.jar --nogui --properties=mapdef/$map-$target
#  mv GpsMid-$ver.jar GpsMid-Generic-$target-$ver.jar
#  mv GpsMid-$ver.jad GpsMid-Generic-$target-$ver.jad
done

cd ..
cp -p Osm2GpsMid/dist/Osm2GpsMid-$ver.jar .

mkdir "Release $numver"
cp Osm2GpsMid/*-$ver.ja? README.mkd WHATSNEW.txt Osm2GpsMid-$ver.jar "Release $numver"

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish

cd Osm2GpsMid
ant clean
ant



cd ..
cp -p Osm2GpsMid/dist/Osm2GpsMid-$ver.jar "Release $numver/Osm2GpsMid-$ver-debug.jar"

chmod -R g+w "Release $numver"
