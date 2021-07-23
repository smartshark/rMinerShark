#!/bin/sh
PLUGIN_PATH=$1

COMMAND="java -jar $PLUGIN_PATH/rMinerSHARK.jar --project ${9} --db-hostname $2 --db-port $3 --db-database $4"

if [ ! -z ${5+x} ] && [ ${5} != "None" ]; then
	COMMAND="$COMMAND --db-user ${5}"
fi

if [ ! -z ${6+x} ] && [ ${6} != "None" ]; then
	COMMAND="$COMMAND --db-password ${6}"
fi

if [ ! -z ${7+x} ] && [ ${7} != "None" ]; then
	COMMAND="$COMMAND --db-authentication ${7}"
fi

if [ ! -z ${8+x} ] && [ ${8} != "None" ]; then
	COMMAND="$COMMAND -ssl"
fi

$COMMAND
