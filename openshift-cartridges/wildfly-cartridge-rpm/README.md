# OpenShift WildFly Cartridge

The community-supported WildFly cartridge repository contains an entire copy of WildFly. Rather than duplicating it here, we just provide an RPM spec file, which the community-supported version does not have.

Download the cartridge source (replace the git revision with the global variable `gitrev` from the spec):

    curl -L -O https://github.com/openshift-cartridges/openshift-wildfly-cartridge/archive/bf5dfcc.tar.gz

Either setup an rpmbuild tree yourself or download a script to do it for you:

    curl -O https://raw.githubusercontent.com/charlessimpson/dist-rpm/master/quickbuild

Call rpmbuild yourself or use the script you just downloaded:

    sh quickbuild openshift-origin-cartridge-wildfly.spec bf5dfcc.tar.gz

This should produce an RPM of the form:

    openshift-origin-cartridge-wildfly-0.1.0-1.git.bf5dfcc.el6.x86_64.rpm
