#!/bin/bash

jobname=$1
buildNumber=$2
workdir=$3
orgStr=$4
assetName=$5
axwayUser=$6
axwayPasswd=$7
backendService=$8
frontendService=$9

groupId="axway.com"
artifactId="tbd"

echo "Now Starting to setup backend and frontend service definitions in API Manager"


ls -al ./apim

python --version
echo "curl --insecure --user ${axwayUser}:${axwayPasswd} -X POST -F file=@./apim/${backendService} -F fe-desc=@./apim/${frontendService} -F 'type=swagger' -F \"organizationId=${orgStr}\" -F 'uploadType=html5' -F 'integral=false' -F \"name=${assetName}_${buildNumber}\" https://${DEV}:${PORT}"
				

echo "Copying ENV settings"
mkdir /tmp/axway-mgr/env
cp $workdir/env/*.env /tmp/axway-mgr/env

echo "Copying Test scripts"
cp $workdir/testscripts/*.json /tmp/axway-mgr/env
#cd /tmp/axway-mgr

#tar -zcvf /tmp/${polname}_packed.tar.gz .
                
echo "Upload to Nexus: ${nexusServerURL}/deployment/${polname}_packed.tar.gz"
#curl -v -F "g=${groupId}" -F "a=${artifactId}" --user ${nexusUser}:${nexusPasswd} --upload-file /tmp/${polname}_packed.tar.gz ${nexusServerURL}/deployment/${polname}_packed.tar.gz


