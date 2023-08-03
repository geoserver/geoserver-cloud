#!/bin/sh

v1=1.2.0
echo Gettig current version...
v2=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

echo Comparing $v1 vs $v2...
echo Summary:

repo=geoservercloud
for i in `docker images|grep geoservercloud|grep "$v2 "|sort|cut -d" " -f1|sed -e "s/$repo\///g"`
do
  export image=$i
  echo "----------------------------"
  echo "* $image:"
  echo "\t\`$v1\`: $(trivy image --scanners vuln --vuln-type library --no-progress $repo/$image:$v1 | grep Total)" 
  echo "\t\`$v2\`: $(trivy image --scanners vuln --vuln-type library --no-progress $repo/$image:$v2 | grep Total)"
done

echo "$v2 library vulnerabilities"

echo writing html reports to $PWD/target
mkdir -p target
for i in `docker images|grep geoservercloud|grep "$v2 "|sort|cut -d" " -f1|sed -e "s/$repo\///g"`
do
  export image=$i
  export old=$image:$v1
  export new=$image:$v2
  trivy image --scanners vuln --vuln-type library --format template --template "@/usr/local/share/trivy/templates/html.tpl" -o target/$old.html $repo/$old
  trivy image --scanners vuln --vuln-type library --format template --template "@/usr/local/share/trivy/templates/html.tpl" -o target/$new.html $repo/$new
done



