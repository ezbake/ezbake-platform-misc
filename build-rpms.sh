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
    local spec_file=$1
    local ezbake_version=$2
    local is_release_build=$3
    local git_file_last_changed=$4

    rpmbuild \
        --quiet \
        --define="_topdir $PWD/rpmbuild" \
        --define="ezbake_version $ezbake_version" \
        --define="ezbake_release_build $is_release_build" \
        --define="git_file_last_changed $git_file_last_changed" \
        -bb \
        rpmbuild/SPECS/$spec_file
}

if [ ! -f /usr/lib/rpm/macros.ezbake ]; then
    echo "ERROR: The ezbake-rpm-tools package must be installed." >&2
    exit 1
fi

echo "Getting Maven project version information"
mvn_proj_version=$(mvn -q \
                  --non-recursive \
                  org.codehaus.mojo:exec-maven-plugin:1.3.1:exec \
                  -Dexec.executable="echo" \
                  -Dexec.args='${project.version}')

ezbake_version=${mvn_proj_version%%-SNAPSHOT}
if [[ $mvn_proj_version = *-SNAPSHOT ]]; then
    is_release_build=0
else
    is_release_build=1
fi

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

wildfly_spec=openshift-cartridges/wildfly-cartridge-rpm/openshift-origin-cartridge-wildfly.spec
wildfly_upstream_tarball_uri_prefix=$(grep "Source:" $wildfly_spec | awk '{print $2}' | xargs dirname)
wildfly_upstream_tarball=$(grep -E "%global upstream_gitrev " $wildfly_spec | awk '{print $3}').tar.gz
if [ ! -f rpmbuild/SOURCES/$wildfly_upstream_tarball ]; then
    wget $wildfly_upstream_tarball_uri_prefix/$wildfly_upstream_tarball \
        -O rpmbuild/SOURCES/$wildfly_upstream_tarball
fi

thrift_version=$(grep "Version:" thrift-rpm/thrift.spec | awk '{print $2}')
thrift_tarball=thrift-$thrift_version.tar.gz
if [ ! -f rpmbuild/SOURCES/$thrift_tarball ]; then
    wget https://archive.apache.org/dist/thrift/$thrift_version/$thrift_tarball \
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

cp filesystem-rpm/ezbake-filesystem.spec rpmbuild/SPECS/ezbake-filesystem.spec

echo "Building RPMs"

build_rpm \
    openshift-origin-cartridge-java-thriftrunner.spec \
    $ezbake_version \
    $is_release_build \
    openshift-cartridges/java-thriftrunner-cartridge

build_rpm \
    openshift-origin-cartridge-logstash.spec \
    $ezbake_version \
    $is_release_build \
    openshift-cartridges/logstash-cartridge

build_rpm \
    openshift-origin-cartridge-play-framework.spec \
    $ezbake_version \
    $is_release_build \
    openshift-cartridges/play-framework-cartridge

build_rpm \
    openshift-origin-cartridge-wildfly.spec \
    $ezbake_version \
    $is_release_build \
    openshift-cartridges/wildfly-cartridge-rpm

build_rpm thrift.spec $ezbake_version $is_release_build thrift-rpm
build_rpm ezbake_logstash_helper.spec $ezbake_version $is_release_build logstash-helper
build_rpm ezbake-filesystem.spec $ezbake_version $is_release_build filesystem-rpm
