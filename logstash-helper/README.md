# EzBake Logstash Helper

This is an RPM that installs an EzBake logstash helper script. The helper script can be used to
apply the logstash configuration for EzBake services and to start/stop the logstash process.

## Using

The logstash script contains functions, so you should source it in your script and call the functions on it

```
source logstash.sh
start_logstash common_services service_name /my/service/logstash
stop_logstash common_services logstash_test /my/service/logstash
```

## Building

There is a Vagrantfile in this directory for building the logstash-helper RPM on CentOS. rpmbuild does
not work very well on Mac OS X.

The build.sh script takes care of starting the vagrant VM and running the rpmbuild steps. Summarized for
convenience:

```
#!/bin/bash
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
```
