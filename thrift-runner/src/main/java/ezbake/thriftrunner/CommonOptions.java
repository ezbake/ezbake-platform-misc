/*   Copyright (C) 2013-2014 Computer Sciences Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

 package ezbake.thriftrunner;

import com.google.common.collect.Lists;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

class CommonOptions {
    @Option(name = "-j", aliases = "--jar_file", usage = "the jar file containing the service to be ran",
            required = true)
    File jarFile = null;

    @Option(name = "-a", aliases = "--application_name", usage = "the name of the application")
    String applicationName = "";

    @Option(name = "-s", aliases = "--service_name", usage = "the name of the service to be exectued")
    String serviceName = null;

    @Option(name = "-x", aliases = "--security_id", usage = "the security id of the service or application")
    String securityID = "";

    @Option(name = "--disable-service-registration", usage="Use this flag when you want to disable automatic registration with service discovery")
    boolean serviceDiscoveryDisabled = false;

    List<Path> additionalConfigurationDirs = Lists.newArrayList();
    @Option(name = "-P", aliases = "--additional-config-dirs", metaVar = "dir1 dir2 dir2")
    void setAdditionalConfigurationDirectory(final String directory) throws CmdLineException {
        Path path = Paths.get(directory);
        if(!Files.isDirectory(path)) {
            throw new CmdLineException(path.toString() + " is not a directory!");
        }

        additionalConfigurationDirs.add(path);
    }


    Properties props = new Properties();

    @Option(name = "-D", metaVar = "<property>=<value>", usage = "use value for given property")
    void setProperty(final String property) throws CmdLineException {
        final String[] arr = property.split("=");
        if (arr.length != 2) {
            throw new CmdLineException("Properties must be specified in the form -D<property>=<value> instead of " + property);
        }

        props.setProperty(arr[0], arr[1]);
    }

    @Option(name="-l", aliases="--logging-config", usage="Path to the logging configuration file")
    String loggingConfig = "/opt/ezbake/thriftrunner/etc/logback.xml";
}
