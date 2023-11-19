#!/bin/bash

# AWS LINUX VERSION #################

logger -s Begin setup of tramchester server

export USERDATA=http://169.254.169.254/latest/user-data

userText=/tmp/userdata.txt
wget -nv $USERDATA -O "$userText"

export S3URL=https://s3-eu-west-1.amazonaws.com

# extract from instance user data
export PLACE=$(grep ENV "$userText" | cut -d = -f 2-)
export BUILD=$(grep BUILD "$userText" | cut -d = -f 2-)
export BUCKET=$(grep BUCKET "$userText" | cut -d = -f 2-)
export TFGMAPIKEY=$(grep TFGMAPIKEY "$userText" | cut -d = -f 2-)

export ARTIFACTSURL=$S3URL/$BUCKET/dist/$BUILD

if [ "$BUILD" == '' ]; then
        echo 'BUILD missing'
        exit;
fi
if [ "$BUCKET" == '' ]; then
        echo 'BUCKET missing'
        exit;
fi
if [ "$PLACE" == '' ]; then
        echo 'PLACE missing'
        exit;
fi

logger -s Set up Web server Bucket: "$BUCKET" Build: "$BUILD" Url: "$ARTIFACTSURL" Env: "$PLACE"

cd ~ec2-user || (logger Could not cd to ec2-user && exit)
mkdir -p server
cd server || (logger Could not cd to ec2-user/server && exit)

# fetch and install the package
target=tramchester-$BUILD
distUrl=$ARTIFACTSURL/$target.zip
dist=$(basename "$distUrl")

logger -s Get "$distUrl"
wget -nv "$distUrl" -O "$dist"
unzip "$dist" || (logger -s Could not unzip from "$dist" from "$distUrl" && exit)

# fix ownership
chown -R ec2-user .

# cloudwatch logs agent
logger -s set up amazon cloudwatch logs agent new
sed -i.orig "s/PREFIX/web_${PLACE}_${BUILD}/" $target/config/cloudwatch_agent.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:$target/config/cloudwatch_agent.json -s
logger -s cloud watch agent configured

# start
logger -s invoke start script
export RAIL_WSDL=$target/config/OpenLDBWS.wsdl
export RELEASE_NUMBER="$BUILD"
logger Start tramchester for $PLACE
export JAVA_OPTS="-Xmx1550m"
sudo -E -u ec2-user bash ./$target/bin/start.sh &

logger -s Finish Web bootstrap script for "$BUILD" and "$PLACE"
