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

%global cartridgedir %{_libexecdir}/openshift/cartridges/logstash

Summary:        Embedded logstash support for OpenShift
Name:           openshift-origin-cartridge-logstash
Version:        0.0.2
Release:        1%{?dist}
Group:          Development/Languages
License:        ASL 2.0
URL:            ezbake-platform-misc/openshift-cartridges/logstash-cartridge
Source0:        logstash-cartridge.tar.gz

Requires:       logstash >= 1.4.2
Requires:       logstash-contrib  >= 1.4.2
Provides:       openshift-origin-cartridge-logstash
BuildArch:      noarch


%description
Logstash cartridge for openshift. (Cartridge Format V2)

%prep
%setup -q -n logstash-cartridge

%build
%__rm -f *.spec

%install
%__mkdir -p %{buildroot}%{cartridgedir}
%__cp -r * %{buildroot}%{cartridgedir}


%files
%dir %{cartridgedir}
%attr(0755,-,-) %{cartridgedir}/bin/
%{cartridgedir}/metadata

%changelog
* Tue Sep 16 2014 0.0.1
- Initial creation
