#!/bin/bash
DST_DIR=spring-boot-starter-netty

#compile
pushd .
cd ..
mvn clean package -Dmaven.test.skip=true
popd

#package
rm -rf ${DST_DIR}
mkdir -p ${DST_DIR}/
cp -Rf ../target/lib ${DST_DIR}
cp -f ../target/*.jar ${DST_DIR}/
tar czf ${DST_DIR}.tar.gz ${DST_DIR}

#install onto local maven repo
mvn install:install-file -Dfile=${DST_DIR}/${DST_DIR}.jar -DpomFile=../pom.xml
