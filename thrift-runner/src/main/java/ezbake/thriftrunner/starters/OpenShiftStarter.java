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

package ezbake.thriftrunner.starters;

import java.io.IOException;

import org.kohsuke.args4j.Option;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;

import ezbake.thriftrunner.ThriftStarter;

public class OpenShiftStarter extends ThriftStarter {
    @Option(name = "--private_port", usage = "the private port which openshift will give us for internal use")
    private int privatePort = -1;

    @Option(name = "--private_host", usage = "the private host which openshift will give us for internal use")
    private String privateHostName = null;

    @Option(name = "--public_port", usage = "the public port which we will broadcast to ezDiscovery")
    private int publicPort = -1;

    @Option(name = "--public_host", usage = "the public hostname which we will broadcast to ezDiscovery")
    private String publicHostName = null;

    @Override
    public void initialize() throws IOException {
        Preconditions.checkState(!Strings.isNullOrEmpty(publicHostName), "public host name MUST be specified!");
        Preconditions.checkState(publicPort > 0, "public port MUST be higher then 0");
        Preconditions.checkState(!Strings.isNullOrEmpty(privateHostName), "private host name MUST be specified!");
        Preconditions.checkState(privatePort > 0, "private port MUST be higher then 0");
    }

    @Override
    public HostAndPort getPublicHostInfo() {
        return HostAndPort.fromParts(publicHostName, publicPort);
    }

    @Override
    public HostAndPort getPrivateHostInfo() {
        return HostAndPort.fromParts(privateHostName, privatePort);
    }
}
