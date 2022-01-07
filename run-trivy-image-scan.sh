#!/bin/sh

v1=1.0-RC5
echo Gettig current version...
v2=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

echo Comparing $v1 vs $v2...
echo Summary:

for i in `docker images|grep geoservercloud|grep "$v2 "|sort|cut -d" " -f1`
do
  export image=$i
  echo "* $image:"
  echo "\t\`$v1\`": $(trivy image --vuln-type library --no-progress --light $image:$v1 | grep Total) 
  echo "\t\`$v2\`": $(trivy image --vuln-type library --no-progress --light $image:$v2 | grep Total)
done

echo $v2 library vulnerabilities
for i in `docker images|grep geoservercloud|grep "$v2 "|sort|cut -d" " -f1`
do
  export image=$i
  echo "--------------------------------------------------"
  echo "$image:"
  trivy image --vuln-type library --no-progress -s "HIGH,CRITICAL" $image:$v2 |grep -v INFO
done
