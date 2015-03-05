#!/bin/bash
#   Copyright (C) 2013-2014 Computer Sciences Corporation
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.


function build_rpm() {
    spec_file=$1
    rpmbuild --quiet --define="_topdir $PWD/rpmbuild" -bb rpmbuild/SPECS/$spec_file
}

echo "Creating RPM build directories"
mkdir -p rpmbuild/{BUILD,BUILDROOT,RPMS,SOURCES,SPECS,SRPMS}

echo "Creating/Downloading source tarballs"
git archive \
    --format=tar \
    --prefix java-thriftrunner-cartridge.git/ \
    HEAD:openshift-cartridges/java-thriftrunner-cartridge/ | \
    gzip > rpmbuild/SOURCES/java-thriftrunner-cartridge.tar.gz

git archive \
    --format=tar \
    --prefix logstash-cartridge/ \
    HEAD:openshift-cartridges/logstash-cartridge/ | \
    gzip > rpmbuild/SOURCES/logstash-cartridge.tar.gz

git archive \
    --format=tar \
    --prefix play-framework-cartridge.git/ \
    HEAD:openshift-cartridges/play-framework-cartridge/ | \
    gzip > rpmbuild/SOURCES/play-framework-cartridge.tar.gz

git archive \
    --format=tar \
    --prefix ezbake-logstash-helper.git/ \
    HEAD:logstash-helper/ | \
    gzip > rpmbuild/SOURCES/ezbake-logstash-helper.tar.gz

wildfly_gitrev=$(grep -E '^%global gitrev ' openshift-cartridges/wildfly-cartridge-rpm/openshift-origin-cartridge-wildfly.spec | cut -d' ' -f3)
if [ ! -f rpmbuild/SOURCES/$wildfly_gitrev.tar.gz ]; then
    wget https://github.com/openshift-cartridges/openshift-wildfly-cartridge/archive/$wildfly_gitrev.tar.gz \
        -O rpmbuild/SOURCES/$wildfly_gitrev.tar.gz
fi

THRIFT_VERSION="0.9.1"
thrift_tarball=thrift-$THRIFT_VERSION.tar.gz
if [ ! -f rpmbuild/SOURCES/$thrift_tarball ]; then
    wget https://archive.apache.org/dist/thrift/$THRIFT_VERSION/$thrift_tarball \
        -O rpmbuild/SOURCES/$thrift_tarball
fi

echo "Copying spec files to RPM build directory"
cp openshift-cartridges/java-thriftrunner-cartridge/openshift-origin-cartridge-java-thriftrunner.spec \
    rpmbuild/SPECS/openshift-origin-cartridge-java-thriftrunner.spec

cp openshift-cartridges/logstash-cartridge/openshift-origin-cartridge-logstash.spec \
    rpmbuild/SPECS/openshift-origin-cartridge-logstash.spec

cp openshift-cartridges/play-framework-cartridge/openshift-origin-cartridge-play-framework.spec \
    rpmbuild/SPECS/openshift-origin-cartridge-play-framework.spec

cp openshift-cartridges/wildfly-cartridge-rpm/openshift-origin-cartridge-wildfly.spec \
    rpmbuild/SPECS/openshift-origin-cartridge-wildfly.spec

cp thrift-rpm/thrift.spec rpmbuild/SPECS/thrift.spec

cp logstash-helper/ezbake_logstash_helper.spec rpmbuild/SPECS/ezbake_logstash_helper.spec

echo "Modifying spec files"

sed -i \
    -e "s/^\%global gitrev [0-9a-f]*$/\%global gitrev $(git rev-parse --short HEAD)/" \
    -e "s/Release: [0-9]*\./Release: $(git show -s --format="%ct" HEAD)\./" \
    rpmbuild/SPECS/openshift-origin-cartridge-java-thriftrunner.spec

sed -r -i \
    -e "s/^\%global gitrev [0-9a-f]*$/\%global gitrev $(git rev-parse --short HEAD)/" \
    -e "s/Release:[[:space:]]*[0-9]+\./Release: $(git show -s --format="%ct" HEAD)\./" \
    rpmbuild/SPECS/openshift-origin-cartridge-play-framework.spec

sed -r -i \
    -e "s/Release:[[:space:]]*[0-9]+\./Release: $(git show -s --format="%ct" HEAD)\./" \
    rpmbuild/SPECS/openshift-origin-cartridge-wildfly.spec

sed -i \
    -e "s/^\%global gitrev [0-9a-f]*$/\%global gitrev $(git rev-parse --short HEAD)/" \
    -e "s/Release: [0-9]*\./Release: $(git show -s --format="%ct" HEAD)\./" \
    rpmbuild/SPECS/ezbake_logstash_helper.spec


echo "Building RPMs"
build_rpm openshift-origin-cartridge-java-thriftrunner.spec
build_rpm openshift-origin-cartridge-logstash.spec
build_rpm openshift-origin-cartridge-play-framework.spec
build_rpm openshift-origin-cartridge-wildfly.spec
build_rpm thrift.spec
build_rpm ezbake_logstash_helper.spec
