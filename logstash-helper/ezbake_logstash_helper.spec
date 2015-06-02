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

%include %{_rpmconfigdir}/macros.ezbake

%define prefix  /opt
%define base_dir ezbake/ezbake-logstash-helper

Summary:        Logstash support for EzBake services
Name:           ezbake-logstash-helper
Version:        %{ezbake_version}
Release:        %{ezbake_release}%{?dist}
Group:          Development/Languages
License:        Apache 2.0
URL:            ezbake-platform-misc/logstash-helper
Source0:        ezbake-logstash-helper.tar.gz
Prefix:         %{prefix}

Requires:       logstash >= 1.4.2
Requires:       logstash-contrib  >= 1.4.2
Provides:       ezbake-logstash-helper
BuildArch:      noarch
BuildArch:      noarch


%description
Logstash helpers for EzBake services.

%prep
%setup -q -n ezbake-logstash-helper.git

%build
%__rm -f *.spec

%install
%__mkdir -p %{buildroot}%{prefix}/%{base_dir}
%__cp logstash.sh %{buildroot}%{prefix}/%{base_dir}

%clean
rm -rf %{buildroot}

%files
%defattr(0640,ezbake,ezbake,0750)
%dir %{prefix}/%{base_dir}
%attr(0750,-,-) %{prefix}/%{base_dir}/logstash.sh
