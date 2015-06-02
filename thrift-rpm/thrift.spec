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

Name:           thrift
Version:        0.9.1
Release:        %{ezbake_release}%{?dist}
Summary:        Apache Thrift C++ shared library

Group:          Development/Libraries
License:        Apache License v2.0
URL:            https://thrift.apache.org
Source0:        http://archive.apache.org/dist/%{name}/%{version}/%{name}-%{version}.tar.gz
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

BuildRequires: bison
BuildRequires: flex
BuildRequires: gcc-c++

BuildRequires: boost-devel
BuildRequires: libevent-devel
BuildRequires: libstdc++-devel
BuildRequires: openssl-devel
BuildRequires: zlib-devel

%description
Apache Thrift C++ runtime libraries


%package devel
Summary: Apache Thrift C++ development package
Group: Development/Libraries
Requires: %{name}%{?_isa} = %{version}-%{release}
Requires: pkgconfig
Requires: boost-devel
%description devel
Apache Thrift C++ development package


%package python
Summary: Apache Thrift Python bindings
Group: Development/Libraries
Requires: %{name}%{?_isa} = %{version}-%{release}
Requires: python2
BuildRequires: python2-devel
%description python
Apache Thrift C++ Python bindings


%prep
%setup -q


%build
%configure \
    --with-cpp \
    --with-python \
    --without-qt4 \
    --without-c_glib \
    --without-csharp \
    --without-java \
    --without-erlang \
    --without-perl \
    --without-php \
    --without-php_extension \
    --without-ruby \
    --without-haskell \
    --without-go \
    --without-d \
    --without-tests

make %{?_smp_mflags}


%install
rm -rf $RPM_BUILD_ROOT
make install DESTDIR=$RPM_BUILD_ROOT
# Remove broken static libraries
rm $RPM_BUILD_ROOT%{_libdir}/*.a
rm $RPM_BUILD_ROOT%{_libdir}/*.la


%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root,-)
%{_libdir}/libthrift-%{version}.so
%{_libdir}/libthriftnb-%{version}.so
%{_libdir}/libthriftz-%{version}.so


%files devel
%defattr(-,root,root,-)
%{_bindir}/thrift
%{_includedir}/thrift
%{_libdir}/libthrift.so
%{_libdir}/libthriftnb.so
%{_libdir}/libthriftz.so
%{_libdir}/pkgconfig/thrift.pc
%{_libdir}/pkgconfig/thrift-nb.pc
%{_libdir}/pkgconfig/thrift-z.pc


%files python
%defattr(-,root,root,-)
%{python_sitearch}/%{name}
%{python_sitearch}/%{name}-%{version}-py2.6.egg-info


%post
/sbin/ldconfig


%postun
/sbin/ldconfig
