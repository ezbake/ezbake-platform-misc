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

%global cartridgedir %{_libexecdir}/openshift/cartridges/play-framework
%global gitrev 7bdafe3

Summary:       Play Framework cartridge
Name:          openshift-origin-cartridge-play-framework
Version:       0.1.0
Release:       1.git.%{gitrev}%{?dist}
Group:         Development/Languages
License:       Proprietary
URL:           ezbake-platform-misc/openshift-cartridges/play-framework-cartridge
# Fragment so that rpmbuild will get the right filename
Source:        play-framework-cartridge.tar.gz
Requires:      rubygem(openshift-origin-node)
Requires:      openshift-origin-node-util
Requires:      java >= 1.7
Requires:      /usr/bin/nohup
Requires:      /usr/bin/unzip
BuildArch:     noarch

%description
Play Framework OpenShift cartridge.

%prep
%setup -q -n play-framework-cartridge.git

%build
%__rm %{name}.spec
%__rm env/.gitkeep
%__rm hooks/.gitkeep

%install
%__mkdir -p %{buildroot}%{cartridgedir}
%__cp -r * %{buildroot}%{cartridgedir}
sed -i -e '/^Source-Url:/d' %{buildroot}%{cartridgedir}/metadata/manifest.yml

%files
%dir %{cartridgedir}
%attr(0755,-,-) %{cartridgedir}/bin/
%{cartridgedir}/configuration
%{cartridgedir}/env
%attr(0755,-,-) %{cartridgedir}/hooks/
%{cartridgedir}/metadata
%{cartridgedir}/usr
%doc %{cartridgedir}/README.md
%doc %{cartridgedir}/COPYRIGHT

%changelog
* Mon Jul 14 2014 0.1.0-1
- Fork from openshift-origin-cartridge-diy.spec 1.24.1-1
