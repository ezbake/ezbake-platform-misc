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

%global cartridgedir %{_libexecdir}/openshift/cartridges/java-thriftrunner
%global gitrev bd1bce9

Summary: Java thriftrunner cartridge
Name: openshift-origin-cartridge-java-thriftrunner
Version: 0.1.0
Release: 13.git.%{gitrev}%{?dist}
Group: Development/Languages
License: ASL 2.0
URL: ezbake-platform-misc/openshift-cartridges/java-thriftrunner-cartridge
# Fragment so that rpmbuild will get the right filename
Source0: java-thriftrunner-cartridge.tar.gz
Requires: rubygem(openshift-origin-node)
Requires: openshift-origin-node-util
Requires: java >= 1.6
Requires: python >= 2.6
Requires: python < 3
BuildArch: noarch

%description
Java thriftrunner OpenShift cartridge.


%prep
%setup -q -n java-thriftrunner-cartridge.git


%build
%__rm -f *.spec


%install
%__mkdir -p %{buildroot}%{cartridgedir}
%__cp -r * %{buildroot}%{cartridgedir}
sed -i -e '/^Source-Url:/d' %{buildroot}%{cartridgedir}/metadata/manifest.yml


%files
%dir %{cartridgedir}
%attr(0755,-,-) %{cartridgedir}/bin/
%{cartridgedir}/env/
%attr(0755,-,-) %{cartridgedir}/hooks/
%{cartridgedir}/lib/
%{cartridgedir}/metadata/
%{cartridgedir}/usr/
%doc %{cartridgedir}/README.md
%doc %{cartridgedir}/COPYRIGHT
%doc %{cartridgedir}/LICENSE


%changelog
* Fri Dec 13 2013 0.1.0-1
- Fork from openshift-origin-cartridge-diy.spec 1.16.3-1
