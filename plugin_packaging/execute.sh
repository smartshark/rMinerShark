#!/bin/sh

PLUGIN_PATH=$1
NEW_UUID=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
RAMDISK_PATH="/dev/shm/$NEW_UUID"

# in case of slurm we use the provided jobid and path
if [ ! -z "$SLURM_JOB_ID" ]; then
    module load openjdk/11.0.2
    RAMDISK_PATH="/dev/shm/jobs/$SLURM_JOB_ID"
fi

COMMAND="java -jar $PLUGIN_PATH/rMinerSHARK.jar --project ${9} --db-hostname $2 --db-port $3 --db-database $4 -f $RAMDISK_PATH"

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

if [ ! -z ${10+x} ] && [ ${10} != "None" ]; then
	COMMAND="$COMMAND --folder ${10}"
fi

$COMMAND
