#!/bin/bash

target=tramchester-"$RELEASE_NUMBER"
logger -s Start ./$target/bin/tramchester for "$PLACE" and $target

LOGFILE=/home/ec2-user/server/logs/tramchester_local.log

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

logger -s Config is $CONFIG

while true; do
    ./$target/bin/tramchester server $CONFIG 1> /dev/null
    logger ERROR tramchester Stopped
    echo tramchester stopped
    if [ -f $LOGFILE ]; then
      logger tramchester last 5 lines of $LOGFILE
      tail -5 $LOGFILE | logger
    fi
    sleep 15
    logger ERROR tramchester Restarting
    echo tramchester Restarting
done

logger ERROR start script exited