#!/bin/bash

target=tramchester-"$RELEASE_NUMBER"
logger -s Start ./$target/bin/tramchester for "$PLACE" and $target

configFile=local.yml

# disabled for now, makes start up too slow on smaller instances
#if [ "$PLACE" == 'UAT' ]; then
#  configFile=gm.yml
#fi

# live data from tfgm servers only if prod, requires nat gateway in the environment, see config files
if [ "$PLACE" == 'ProdBlue' ] || [ "$PLACE" == 'ProdGreen' ]; then
    export LIVEDATA_URL="https://api.tfgm.com/odata/Metrolinks"
    logger tramchester enable live data download from $LIVEDATA_URL
fi

CONFIG=./$target/config/$configFile
LOGFILE=./logs/tramchester_local.log

logger -s Config is $CONFIG

while true; do
    ./$target/bin/tramchester server $CONFIG 1> /dev/null
    logger -s ERROR tramchester Stopped
    if [ -f $LOGFILE ]; then
      logger -s tramchester last 25 lines of $LOGFILE
      tail -25 $LOGFILE | logger -s
    fi
    sleep 15
    logger -s ERROR tramchester Restarting
done

logger -s ERROR start script exited