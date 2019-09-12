#!/bin/bash

polname=$1
poldir=$2
workdir=$3
groupId="axway.com"
artifactId="tbd"
file="./conf/apig.conf"

if [ -f "$file" ]
then
  echo "$file found."

  while IFS='=' read -r key value
  do
    key=$(echo $key | tr '.' '_')
    eval ${key}=\${value}
  done < "$file"

else
  echo "$file not found."
fi

#TEMP1 hard-coded values for testing
#polname=testpol
#poldir=/home/ec2-user/axwayfiles/workspace
#END

pkgNames=""
mkdir -p /tmp/axway
ary=($gitRepoNames)
for key in "${!ary[@]}"; do
        echo "REPO PATH[$key]: ${ary[$key]}";
        git clone http://$gitAccess@$gitServerURL/${ary[$key]} /tmp/axway/$gitPkgName$key
        pkgNames_temp=$(ls /tmp/axway/$gitPkgName$key)
        subary=($pkgNames_temp)
        echo "# of packages within folder: ${#subary[@]}"
        pkgLen=${#subary[@]}
        if [[ "$pkgLen" -gt "1" ]];then
                echo "Multiple packages exist!";
                for subkey in "${subary[@]}"; do
                        pkgNames+="/tmp/axway/$gitPkgName$key/$subkey "
                        echo "$subkey";
                done
        else
                echo "Single package to work with";
                pkgNames+="/tmp/axway/$gitPkgName$key/$(ls /tmp/axway/$gitPkgName$key) "
        fi

done
echo "# of Repo PATHs to work with: ${#ary[@]}"
echo "PKG names: $pkgNames" #|sed 's/,//g'

echo "Now Packing to generate .pol"

#echo "${axwayprojpack} --create --passphrase-none --name=$polname --type=pol --add $pkgNames  --projpass-none --dir=$poldir"
${axwayprojpack} --create --passphrase-none --name=$polname --type=pol --add $pkgNames  --projpass-none --dir=$poldir

echo "Copying ENV settings"
mkdir ${poldir}/env
cp $workdir/env/*.env ${poldir}/env

echo "Copying Test scripts"
cp $workdir/testscripts/*.json ${poldir}
cd ${poldir}

tar -zcvf /tmp/${polname}_packed.tar.gz .
                
echo "Upload to Nexus: ${nexusServerURL}/deployment/${polname}_packed.tar.gz"
curl -v -F "g=${groupId}" -F "a=${artifactId}" --user ${nexusUser}:${nexusPasswd} --upload-file /tmp/${polname}_packed.tar.gz ${nexusServerURL}/deployment/${polname}_packed.tar.gz

echo "Cleaning up git cloned pakages: /tmp/axway"
rm -rf /tmp/axway
