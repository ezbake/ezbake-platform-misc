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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import ezbakehelpers.ezconfigurationhelpers.application.EzBakeApplicationConfigurationHelper;
import ezbakehelpers.ezconfigurationhelpers.system.SystemConfigurationHelper;

import ezbake.util.AuditLoggerConfigurator;

import ezbake.base.thrift.EzBakeBaseThriftService;
import ezbake.common.openshift.OpenShiftUtil;
import ezbake.configuration.EzConfigurationLoader;
import ezbake.configuration.EzConfiguration;
import ezbake.configuration.EzConfigurationLoaderException;
import ezbake.configuration.DirectoryConfigurationLoader;
import ezbake.configuration.OpenShiftConfigurationLoader;
import ezbake.configuration.PropertiesConfigurationLoader;
import ezbake.configuration.constants.EzBakePropertyConstants;

import org.kohsuke.args4j.ClassParser;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import ezbake.thriftrunner.starters.OpenShiftStarter;
import ezbake.thriftrunner.starters.SimpleStarter;

public class ThriftRunner {
    public static void main(String[] args) throws Exception {
        ThriftStarter thriftStarter = null;
        if (OpenShiftUtil.inOpenShiftContainer()) {
            thriftStarter = new OpenShiftStarter();
        } else {
            thriftStarter = new SimpleStarter();
        }

        final CommonOptions commonOptions = parseArgs(args, thriftStarter);

        final File jarFile = Preconditions.checkNotNull(commonOptions.jarFile, "A jar file must be specified!");
        Preconditions.checkState(jarFile.exists() && jarFile.isFile(), jarFile + " does not exist or is not a file!");

        final Properties configuration = getMergedConfig(commonOptions);

        setupLogging(configuration, commonOptions.loggingConfig);

        thriftStarter.initialize();

        /* args4j for boolean  will set to true if we enable an option, in this case we want to disable something, hence
         the '!' */
        final EzBakeBaseThriftService service = getServiceFromJar(jarFile, thriftStarter, configuration);

        System.out.println("Thrift server starting on " + thriftStarter.getPrivateHostInfo());

        boolean serviceDiscoveryEnabled = !commonOptions.serviceDiscoveryDisabled;
        // Will start the server and block until the JVM is shut down, thus stopping the service, unregistering, etc.
        new EZBakeBaseThriftServiceRunner(service, thriftStarter, serviceDiscoveryEnabled).run();
    }

    private static CommonOptions parseArgs(String[] args, ThriftStarter thriftStarter) throws CmdLineException {
        final CommonOptions commonOptions = new CommonOptions();
        final CmdLineParser cmdLineParser = new CmdLineParser(commonOptions);

        if (args.length == 0) {
            cmdLineParser.printUsage(System.err);
            System.exit(-1);
        }

        final ClassParser cp = new ClassParser();
        cp.parse(thriftStarter, cmdLineParser);

        cmdLineParser.parseArgument(args);

        return commonOptions;
    }

    private static Properties getMergedConfig(CommonOptions commonOptions) {

        Properties props = new Properties();
        // Lets go override or add things from the command line
        if (commonOptions.props.size() > 0) {
            System.out.println("Properties to merge: " + commonOptions.props);
            props.putAll(commonOptions.props);
        }

        if (!Strings.isNullOrEmpty(commonOptions.applicationName)) {
            System.out.println("Updating application name with: " + commonOptions.applicationName);
            props.setProperty(EzBakePropertyConstants.EZBAKE_APPLICATION_NAME, commonOptions.applicationName);
        }

        if (!Strings.isNullOrEmpty(commonOptions.serviceName)) {
            System.out.println("Updating service name with: " + commonOptions.serviceName);
            props.setProperty(EzBakePropertyConstants.EZBAKE_SERVICE_NAME, commonOptions.serviceName);
        }

        if (!Strings.isNullOrEmpty(commonOptions.securityID)) {
            System.out.println("Updating security ID with: " + commonOptions.securityID);
            props.setProperty(EzBakePropertyConstants.EZBAKE_SECURITY_ID, commonOptions.securityID);
        }

        try {
            // create a new object and load the default resources
            List<EzConfigurationLoader> configurationLoaders = Lists.newArrayList();
            configurationLoaders.add(new DirectoryConfigurationLoader());
            configurationLoaders.add(new OpenShiftConfigurationLoader());

            for(Path p : commonOptions.additionalConfigurationDirs) {
                configurationLoaders.add(new DirectoryConfigurationLoader(p));
            }

            configurationLoaders.add(new PropertiesConfigurationLoader(props));
            EzConfigurationLoader [] loaders = new EzConfigurationLoader[configurationLoaders.size()];
            EzConfiguration ezConfiguration = new EzConfiguration(configurationLoaders.toArray(loaders));
            return ezConfiguration.getProperties();
        } catch(EzConfigurationLoaderException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setupLogging(Properties config, String loggingConfig) {
        try {
            final EzBakeApplicationConfigurationHelper appConfig = new EzBakeApplicationConfigurationHelper(config);
            final String applicationName = appConfig.getApplicationName();
            final String serviceName = appConfig.getServiceName();

            final SystemConfigurationHelper sysConf = new SystemConfigurationHelper(config);
            final String logFilePath = sysConf.getLogFilePath(applicationName, serviceName);

            final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset(); // override default configuration

            // Only log to a file outside of OpenShift containers
            if (!OpenShiftUtil.inOpenShiftContainer()) {
                context.putProperty("log_file", logFilePath);
                System.setProperty("log_file", logFilePath);
            }

            if (sysConf.shouldLogToStdOut() || OpenShiftUtil.inOpenShiftContainer()) {
                context.putProperty("log_stdout", "true");
                System.setProperty("log_stdout", "true");
            }

            if (applicationName == null) {
                context.putProperty("application_name", "NULL");
            } else {
                context.putProperty("application_name", applicationName);
            }

            context.putProperty("service_name", serviceName);

            URL configURL = null;
            if (loggingConfig != null) {
                Path configPath = Paths.get(loggingConfig);
                if (Files.exists(configPath)) {
                    try {
                        configURL = configPath.toFile().toURI().toURL();
                    } catch (MalformedURLException e) {
                        // do nothing
                    }
                }
            }

            ContextInitializer contextInitializer = new ContextInitializer(context);
            if (configURL != null) {
                System.out.println("Using logging configuration from: " + configURL.toString());
                contextInitializer.configureByResource(configURL);
            } else {
                System.out.println("Using default logging configuration");
                contextInitializer.autoConfig();
            }
            AuditLoggerConfigurator.setFilePath(logFilePath);
        } catch (final JoranException e) {
            // Maybe propagate not sure yet
        }
    }

    private static EzBakeBaseThriftService getServiceFromJar(File jarFile, ThriftStarter thriftStarter, Properties props) throws Exception {
        final URL url = jarFile.toURI().toURL();
        final URLClassLoader loader = new URLClassLoader(new URL[] {url});
        final URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
        method.setAccessible(true);
        method.invoke(sysLoader, url);

        final Class<? extends EzBakeBaseThriftService> serviceClass = thriftStarter.getServiceClass(loader);
        final EzBakeBaseThriftService service = serviceClass.newInstance();
        service.setConfigurationProperties(props);

        return service;
    }
}
