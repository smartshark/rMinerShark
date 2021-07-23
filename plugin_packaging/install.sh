#!/bin/sh
PLUGIN_PATH=$1

cd $PLUGIN_PATH

# Build jar file
./gradlew fatJar

cp build/libs/rMinerSHARK*.jar ./rMinerSHARK.jar

# remove signing
zip -d rMinerSHARK.jar 'META-INF/.SF' 'META-INF/.RSA' 'META-INF/*SF'
