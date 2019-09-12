#!/bin/bash

polname=$1
workdir=$2
uname=$3
password=$4
env=$5
file="./conf/apig.conf"

if [ -f "$file" ]
then
  echo "$file found."

  while IFS='=' read -r key value
  do
    key=$(echo $key | tr '.' '_')
    eval ${key}=\${value}
  done < "$file"

  echo "Group: ${destGroupName}"
  echo "Server: ${destServerName}"

else
  echo "$file not found."
fi

if [ "$env" == "DEV" ]
then
	env2conn=${DEV}
	envFile=${DEVenvFile}
elif [ "$env" == "UAT" ]
then
	env2conn=${UAT}
	envFile=${UATenvFile}
elif [ "$env" == "PROD" ]
then
	env2conn=${PRD}
	envFile=${PRDenvFile}
else
  echo "Unknown $env setting."
  env2conn="Unknown"
  envFile="Unknown"
fi

echo "Connection to $env2conn"
IFS='_' read -r -a ary <<< "$polname"

#ping -c 2 $env2conn

echo "${axwayprojdeploy} --dir=$workdir --passphrase-none --name=${ary[0]}_${ary[1]} --type=pol --deploy-to --host-name=$env2conn --port=443 --user-name=$uname --password=$password --group-name=\"${destGroupName}\" --includes \"${destServerName}\" --apply-env=$workdir/env/$envFile"

${axwayprojdeploy} --dir=$workdir --passphrase-none --name=${ary[0]}_${ary[1]} --type=pol --deploy-to --host-name=$env2conn --port=443 --user-name=$uname --password=$password --group-name="${destGroupName}" --includes "${destServerName}"  --apply-env=$workdir/env/$envFile

