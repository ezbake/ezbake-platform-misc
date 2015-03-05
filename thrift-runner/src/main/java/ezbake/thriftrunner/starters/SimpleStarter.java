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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLClassLoader;

import ezbake.base.thrift.EzBakeBaseThriftService;
import org.kohsuke.args4j.Option;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;

import ezbake.thriftrunner.ThriftStarter;

import org.apache.commons.lang3.math.NumberUtils;

public class SimpleStarter extends ThriftStarter {

    @Option(name = "-p", aliases = "--port_number",
            usage = "the port to run on can be a range delimited by a ':' or a number (0 for random)")
    String portNumberOption = "0";

    @Option(name = "-c", aliases = "--class_name", usage = "the fully qualified class name of the service to start")
    String className = null;

    @Option(name = "-h", aliases = "--host_name", usage = "the name of the hose its being run on")
    String hostName = null;

    private int portNumber = -1;

    @Override
    public void initialize() throws IOException {
        if (hostName == null) {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        }
        getFreePort();

    }

    @Override
    public HostAndPort getPublicHostInfo() {
        return HostAndPort.fromParts(hostName, portNumber);
    }

    @Override
    public HostAndPort getPrivateHostInfo() {
        return HostAndPort.fromParts(hostName, portNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EzBakeBaseThriftService> getServiceClass(URLClassLoader loader) throws Exception {
        if (!Strings.isNullOrEmpty(className)) {
            return (Class<? extends EzBakeBaseThriftService>) loader.loadClass(className);
        } else {
            return getServiceClassUsingReflectionFromJar(loader);
        }
    }

    void getFreePort() throws IOException {
        final String[] minAndMaxPort = portNumberOption.split(":");
        int minPort = 0;
        int maxPort = 0;

        if (minAndMaxPort.length == 2) {
            minPort = NumberUtils.toInt(minAndMaxPort[0], 0);
            maxPort = NumberUtils.toInt(minAndMaxPort[1], 0);
        } else if (minAndMaxPort.length == 1) {
            minPort = NumberUtils.toInt(minAndMaxPort[0], 0);
        }

        if (minPort <= 0) {
            isFree(0);
            return;
        }

        if (maxPort <= minPort) {
            // default max port to minPort + 1
            maxPort = minPort + 1;
        }

        for (int port = minPort; port < maxPort; ++port) {
            if (isFree(port)) {
                return;
            }
        }

        if (portNumber == -1) {
            throw new RuntimeException("Could not find a free port!");
        }
    }

    boolean isFree(int port) {
        if (serverSocketIsFree(port)) {
            return clientSideSocketIsFree(port);
        }

        return false;
    }

    boolean clientSideSocketIsFree(int port) {
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(hostName, port);
        } catch (final IOException e) {
            return true;
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (final IOException e) {
                    // Should never happen
                }
            }
        }
        return false;
    }

    boolean serverSocketIsFree(int port) {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            this.portNumber = socket.getLocalPort();
            return true;
        } catch (final IOException e) {
            return false;
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (final IOException e) {
                // should never happen
            }
        }
    }
}
