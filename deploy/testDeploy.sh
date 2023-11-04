#!/bin/bash

# extract from instance user data
export PLACE=Dev
export BUILD=0
export BUCKET=tramchesternewdist
#export TFGMAPIKEY=$(grep TFGMAPIKEY "$userText" | cut -d = -f 2-)

export ARTIFACTSURL=s3://$BUCKET/dist/$BUILD

echo  Set up Web server Bucket: "$BUCKET" Build: "$BUILD" Url: "$ARTIFACTSURL" Env: "$PLACE"

mkdir -p server
cd server || (echo Could not cd to server && exit)

# fetch and install the package
target=tramchester-$BUILD
distUrl=$ARTIFACTSURL/$target.zip
dist=$(basename "$distUrl")

echo Get "$distUrl"
#wget -nv "$distUrl" -O "$dist"
aws s3 cp "$distUrl" "$dist" || exit
unzip "$dist" || (echo Could not unzip from "$dist" from "$distUrl" && exit)

# start
echo invoke start script
export RAIL_WSDL=$target/config/OpenLDBWS.wsdl
export RELEASE_NUMBER="$BUILD"
echo Start tramchester
export JAVA_OPTS="-Xmx1550m"
bash ./$target/bin/start.sh &

echo -s Finish Web bootstrap script for "$BUILD" and "$PLACE"
