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

Name:		ezbake-filesystem
Version:	%{ezbake_version}
Release:	%{ezbake_release}%{?dist}
Summary:	EzBake filesystem layout

BuildArch:	noarch
Group:		Development/Libraries
License:	ASL 2.0

Requires(pre): /usr/sbin/useradd


%description
This package provides some directories which are required by other EzBake
packages.


%build
mkdir -p %{buildroot}/etc/sysconfig/ezbake
mkdir -p %{buildroot}/opt/ezbake
mkdir -p %{buildroot}/var/lib/ezbake


%pre
# Attempt to add EzBake user. If it already exists, do nothing.
/usr/sbin/useradd -c "EzBake" -s /sbin/nologin -r -d /var/lib/ezbake \
    ezbake 2> /dev/null || :


%files
%attr(0750,ezbake,ezbake) %dir /etc/sysconfig/ezbake
%attr(0755,root,root) %dir /opt/ezbake
%attr(0700,ezbake,ezbake) %dir /var/lib/ezbake


%changelog
* Tue Jan 20 2015 2.1-SNAPSHOT
- initial commit
