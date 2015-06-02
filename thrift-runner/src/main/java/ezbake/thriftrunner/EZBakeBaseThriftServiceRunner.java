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

import static ezbake.thrift.ThriftUtils.getServerArgs;
import static ezbake.thrift.ThriftUtils.getSslServerSocket;
import static org.slf4j.LoggerFactory.getLogger;

import ezbake.base.thrift.EzBakeBaseThriftService;
import ezbake.common.properties.EzProperties;
import ezbakehelpers.ezconfigurationhelpers.application.EzBakeApplicationConfigurationHelper;
import ezbakehelpers.ezconfigurationhelpers.thrift.ThriftConfigurationHelper;
import ezbakehelpers.ezconfigurationhelpers.zookeeper.ZookeeperConfigurationHelper;

import java.net.InetSocketAddress;

import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.slf4j.Logger;

import com.google.common.net.HostAndPort;
import ezbake.ezdiscovery.ServiceDiscoveryClient;

public class EZBakeBaseThriftServiceRunner implements Runnable {
    private final Logger logger = getLogger(EZBakeBaseThriftServiceRunner.class);

    private final EzBakeBaseThriftService service;
    private final EzProperties ezProperties;
    private final HostAndPort publicHostInfo;
    private final HostAndPort privateHostInfo;
    private TServer server;
    private String zookeeperString;
    private String serviceName = null;
    private String applicationName = null;


    private boolean serviceDiscoveryEnabled = true;

    public EZBakeBaseThriftServiceRunner(EzBakeBaseThriftService service, ThriftStarter thriftStarter, boolean serviceDiscoveryEnabled) {
        this(service, thriftStarter.getPublicHostInfo(), thriftStarter.getPrivateHostInfo(), serviceDiscoveryEnabled);
    }

    public EZBakeBaseThriftServiceRunner(EzBakeBaseThriftService service, HostAndPort publicHostInfo,
            HostAndPort privateHostInfo, boolean serviceDiscoveryEnabled) {
        this.service = service;
        this.ezProperties = new EzProperties(this.service.getConfigurationProperties(), true);
        this.publicHostInfo = publicHostInfo;
        this.privateHostInfo = privateHostInfo;
        this.serviceDiscoveryEnabled = serviceDiscoveryEnabled;
    }

    @Override
    public final void run() {
        EzBakeApplicationConfigurationHelper appConf = new EzBakeApplicationConfigurationHelper(ezProperties);
        serviceName = appConf.getServiceName();
        applicationName = appConf.getApplicationName();

        if (StringUtils.isBlank(serviceName)) {
            final String errMsg = "Service name must be specified";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if (StringUtils.isBlank(applicationName)) {
            logger.debug("No application name specified");
        }

        logger.info("Using the following configuration for Thrift service {}: {}", serviceName,
                ezProperties);

        final ZookeeperConfigurationHelper zookeeperConf = new ZookeeperConfigurationHelper(ezProperties);
        zookeeperString = zookeeperConf.getZookeeperConnectionString();
        if (StringUtils.isBlank(zookeeperString)) {
            final String errMsg = "No Zookeeper available for service discovery use";
            logger.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        if(serviceDiscoveryEnabled) {
            // Register with service discovery
            final ServiceDiscoveryClient serviceDiscovery = new ServiceDiscoveryClient(zookeeperString);
            try {
                registerEndpointWithServiceDiscovery(serviceDiscovery);
                registerSecurityIDWithServiceDiscovery(serviceDiscovery, appConf.getSecurityID());
            } catch (final Exception e) {
                final String errMsg = "Error registering with service discovery";
                logger.error(errMsg, e);
                throw new RuntimeException(errMsg, e);
            } finally {
                serviceDiscovery.close();
            }
        }

        // Add shutdown hook to unregister
        unregisterOnShutdown();

        // Create and start the server
        try {
            createServer();
        } catch (final Exception e) {
            final String errMsg = "Error creating server";
            logger.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }

        server.serve();
    }

    public void stop() {
        logger.info("Stopping service {}", serviceName);

        if (service != null) {
            service.shutdown();
        }

        if (server != null && server.isServing()) {
            server.stop();
        }

        if(serviceDiscoveryEnabled) {
            final ServiceDiscoveryClient serviceDiscovery = new ServiceDiscoveryClient(zookeeperString);
            try {
                unregisterEndPointWithServiceDiscovery(serviceDiscovery);
            } catch (final Exception e) {
                logger.error("Could not unregister service from service discovery", e);
            } finally {
                serviceDiscovery.close();
            }
        }
    }

    private void registerEndpointWithServiceDiscovery(ServiceDiscoveryClient serviceDiscovery) throws Exception {
        final String publicHost = publicHostInfo.toString();
        if (StringUtils.isBlank(applicationName) || applicationName.equals("common_services")) {
            serviceDiscovery.registerEndpoint(serviceName, publicHost);
        } else {
            serviceDiscovery.registerEndpoint(applicationName, serviceName, publicHost);
        }
    }

    private void registerSecurityIDWithServiceDiscovery(ServiceDiscoveryClient serviceDiscovery, String securityID)
            throws Exception {
        if (StringUtils.isBlank(securityID)) {
            logger.warn("Got a blank security ID");
            return;
        }

        if (StringUtils.isBlank(applicationName) || applicationName.equals("common_services")) {
            serviceDiscovery.setSecurityIdForCommonService(serviceName, securityID);
        } else {
            serviceDiscovery.setSecurityIdForApplication(applicationName, securityID);
        }
    }

    private void unregisterOnShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stop();
                } catch (final Exception e) {
                    // Just log the error and close. Endpoint will hang around with no service running
                    logger.error("Error unregistering endpoint. Proceeding to close the service discovery client", e);
                }
            }
        }));
    }

    private void unregisterEndPointWithServiceDiscovery(ServiceDiscoveryClient serviceDiscovery) throws Exception {
        final String hostInfo = publicHostInfo.toString();
        if (StringUtils.isBlank(applicationName) || applicationName.equals("common_services")) {
            serviceDiscovery.unregisterEndpoint(serviceName, hostInfo);
        } else {
            serviceDiscovery.unregisterEndpoint(applicationName, serviceName, hostInfo);
        }
    }

    private void createServer() throws Exception {
        final ThriftConfigurationHelper tc = new ThriftConfigurationHelper(ezProperties);
        final boolean useTFramedTransport = ezProperties.getBoolean("tframe.transport", false);

        final TProcessor processor = service.getThriftProcessor();

        switch (tc.getServerMode()) {
            case Simple:
            {
                final TServer.Args serverArgs = (TServer.Args) getServerArgs(
                        getTransport(tc.useSSL()), ezProperties).processor(processor);

                if (useTFramedTransport) {
                    serverArgs.transportFactory(new TFramedTransport.Factory());
                }

                this.server = new TSimpleServer(serverArgs);
                break;
            }
            case ThreadedPool:
            {
                final TThreadPoolServer.Args serverArgs = (TThreadPoolServer.Args) getServerArgs(
                        getTransport(tc.useSSL()), ezProperties).processor(processor);

                if (useTFramedTransport) {
                    serverArgs.transportFactory(new TFramedTransport.Factory());
                }

                this.server = new TThreadPoolServer(serverArgs);
                break;
            }
            case HsHa:
            {
                final InetSocketAddress socketAddress =
                        new InetSocketAddress(privateHostInfo.getHostText(), privateHostInfo.getPort());
                final TNonblockingServerSocket socket = new TNonblockingServerSocket(socketAddress);
                final THsHaServer.Args serverArgs = new THsHaServer.Args(socket);
                serverArgs.processor(processor);
                serverArgs.inputProtocolFactory(new TCompactProtocol.Factory());
                serverArgs.outputProtocolFactory(new TCompactProtocol.Factory());
                this.server = new THsHaServer(serverArgs);
                break;
            }
        }

        logger.info("{} has started on {}", tc.getServerMode(), privateHostInfo);
        logger.info("Detected Processor was {}", processor.getClass());
    }

    private TServerTransport getTransport(boolean useSSL)
            throws Exception {
        final InetSocketAddress socketAddress =
                new InetSocketAddress(privateHostInfo.getHostText(), privateHostInfo.getPort());

        TServerTransport transport;
        if (useSSL) {
            transport = getSslServerSocket(socketAddress, ezProperties);
            logger.info("Communication protected by SSL");
        } else {
            transport = new TServerSocket(socketAddress);
        }

        return transport;
    }
}
