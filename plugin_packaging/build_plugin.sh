#!/bin/bash

current=`pwd`
mkdir -p /tmp/rMinerSHARK/
cp -R ../src /tmp/rMinerSHARK
cp -R ../libs /tmp/rMinerSHARK
cp -R ../gradle /tmp/rMinerSHARK
cp ../gradlew /tmp/rMinerSHARK
cp ../build.gradle /tmp/rMinerSHARK
cp * /tmp/rMinerSHARK
cd /tmp/rMinerSHARK/

tar -cvf "$current/rMinerSHARK_plugin.tar" --exclude=*.tar --exclude=build_plugin.sh *
