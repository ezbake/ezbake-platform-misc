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

%global cartridgedir %{_libexecdir}/openshift/cartridges/wildfly
%global jbossver 8.1.0.Final
%global oldjbossver 8.0.0.Final
%global gitrev bf5dfcc
%global gitrevlong bf5dfcc92a06ce89f12b5dc07f3ecdc0f757aeed

Summary:       Provides WildFly support
Name:          openshift-origin-cartridge-wildfly
Version:       0.1.0
Release:       1.git.%{gitrev}%{?dist}
Group:         Development/Languages
License:       ASL 2.0
URL:           ezbake-platform-misc/openshift-cartridges/openshift-wildfly-cartridge
Source:        https://github.com/openshift-cartridges/openshift-wildfly-cartridge/archive/%{gitrev}.tar.gz
Requires:      rubygem(openshift-origin-node)
Requires:      openshift-origin-node-util
Requires:      lsof
Requires:      bc
Requires:      gawk
Requires:      java-1.7.0-openjdk
Requires:      java-1.7.0-openjdk-devel
%if 0%{?rhel}
Requires:      maven3
%endif
%if 0%{?fedora}
Requires:      maven
%endif
BuildRequires: jpackage-utils

%define __jar_repack 0

%description
Provides WildFly support to OpenShift. (Cartridge Format V2)


%prep
%setup -q -n openshift-wildfly-cartridge-%{gitrevlong}


%build


%install
%__mkdir -p %{buildroot}%{cartridgedir}
%__cp -r * %{buildroot}%{cartridgedir}


%files
%dir %{cartridgedir}
%attr(0755,-,-) %{cartridgedir}/bin/
%attr(0755,-,-) %{cartridgedir}/versions/8/bin/
%attr(0755,-,-) %{cartridgedir}/hooks/
%{cartridgedir}/env
%{cartridgedir}/metadata
%{cartridgedir}/versions/8/appclient
%{cartridgedir}/versions/8/docs
%{cartridgedir}/versions/8/domain
%{cartridgedir}/versions/8/jboss-modules.jar
%{cartridgedir}/versions/8/modules
%{cartridgedir}/versions/8/standalone
%{cartridgedir}/versions/8/template
%{cartridgedir}/versions/8/welcome-content
%doc %{cartridgedir}/README.md
%doc %{cartridgedir}/COPYRIGHT
%doc %{cartridgedir}/LICENSE
%doc %{cartridgedir}/versions/8/copyright.txt
%doc %{cartridgedir}/versions/8/LICENSE.txt
%doc %{cartridgedir}/versions/8/README.txt


%changelog
* Thu Jul 31 2014 0.1.0
- Fork from openshift-origin-cartridge-jbossas-1.26.4
