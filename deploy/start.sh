#!/bin/bash

target=tramchester-1.0
logger Start ./$target/bin/tramchester

until ./$target/bin/tramchester server ./$target/config/local.yml 1> /dev/null; do
    logger ERROR stopped
    sleep 15
    logger ERROR Restarting
done