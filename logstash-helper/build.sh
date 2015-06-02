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


PROJECT=ezbake-logstash-helper
GIT_COMMIT=$(git rev-parse HEAD)
GIT_ARCHIVE="${PROJECT}-${GIT_COMMIT}.tar.gz"

if [ ! -f "${GIT_ARCHIVE}" ]; then
    # generate the tarball
    tar -s,^,ezbake-logstash-helper.git/, -czf "${GIT_ARCHIVE}" *
fi

vagrant up
vagrant ssh <<EOF
cd /vagrant
cp "${GIT_ARCHIVE}" ~/rpmbuild/SOURCES/
rpmbuild -bb ezbake_logstash_helper.spec
mv ~/rpmbuild/RPMS/noarch/ezbake-logstash-helper*.rpm .
EOF
