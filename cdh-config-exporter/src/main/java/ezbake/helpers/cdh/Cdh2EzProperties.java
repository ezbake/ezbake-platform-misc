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

 package ezbake.helpers.cdh;
import com.cloudera.api.ClouderaManagerClientBuilder;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiCluster;
import com.cloudera.api.model.ApiClusterList;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.v3.ClustersResourceV3;
import com.cloudera.api.v3.RootResourceV3;
import com.cloudera.api.v3.ServicesResourceV3;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.*;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Creates properties from CDH to ezconfiguration properties.
 *
 * Usage for a main program.
 *
 * java -jar &lt;jarfilename&gt; -h cm-hostname -u admin -p adminpassword -o hdfs.properties
 *
 *
 */
public class Cdh2EzProperties implements Runnable {

    @Option(name = "-h", aliases = "--host", usage = "host of the service", required = true)
    String host;

    @Option(name = "-u", aliases = "--username", usage = "The username to log in as", required = true)
    String username;

    @Option(name = "-p", aliases = "--password", usage = "The password to log in as", required = true)
    String password;

    @Option(name = "-t", aliases = "--tls", usage = "Whether or not the connection should use TLS", required = false)
    boolean tls = false;

    @Option(name = "-v", aliases = "--verbose", usage = "Be verbose", required = false)
    boolean verbose = false;

    @Option(name = "-c", aliases = "--cluster-name", usage = "Name of the cluster to export the information. " +
            "Defaults to the first cluster returned by cdh", required = false)
    String clusterName = "";

    @Option(name = "-o", aliases = "--output", usage = "Filename to write the properties to. Defaults to stdout",
            required = false)
    String output = "-";

    @Override
    public void run() {
        RootResourceV3 apiRoot;
        if (!tls) {
          apiRoot = new ClouderaManagerClientBuilder()
                  .withHost(host)
                  .withUsernamePassword(username, password)
                  .build()
                  .getRootV3();
        } else {
          apiRoot = new ClouderaManagerClientBuilder()
                  .withHost(host)
                  .withUsernamePassword(username, password)
                  .withPort(7183)
                  .enableTLS()
                  .disableTlsCertValidation()
                  .disableTlsCnValidation()
                  .build()
                  .getRootV3();
        }
          
        ClustersResourceV3 clusterResource = apiRoot.getClustersResource();
        if ( Strings.isNullOrEmpty(clusterName) ) {
            ApiClusterList clusters = apiRoot.getClustersResource().readClusters(DataView.SUMMARY);
            List<ApiCluster> clusterList = clusters.getClusters();
            if ( clusterList == null || clusterList.isEmpty() ) {
                final CmdLineParser cmdLineParser = new CmdLineParser(this);
                cmdLineParser.printUsage(System.err);
                System.exit(-1);
            }
            clusterName = clusterList.get(0).getName();
        }
        if ( verbose ) System.err.println("Using cluster '" + clusterName + "'.");

        ServicesResourceV3 serviceResource = clusterResource.getServicesResource(clusterName);
        InputStreamDataSource configStream = serviceResource.getClientConfig("HDFS");
        try {
            Configuration configuration = getConfiguration(configStream);
            Properties properties = new Properties();
            for (Map.Entry<String, String> prop : configuration ) {
                properties.setProperty(prop.getKey(), prop.getValue());
            }

            if ( Strings.isNullOrEmpty(output) || output.equals("-") ) {
                properties.store(System.out, getFileComment());
            } else {
                try (OutputStream outputStream = new FileOutputStream(output, false)) {
                    properties.store(outputStream, getFileComment());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileComment() {
        return "Auto-generated by " + this.getClass().getName()
                + " at " + new DateTime().toString(ISODateTimeFormat.dateTime());
    }

    public Configuration getConfiguration(InputStreamDataSource configStream) throws IOException {
        Configuration configuration = new Configuration(false);
        try (ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(configStream.getInputStream())) {
            ZipArchiveEntry zipEntry = zipInputStream.getNextZipEntry();
            while (zipEntry != null) {
                String name = zipEntry.getName();
                if ( name.endsWith("core-site.xml") || name.endsWith("hdfs-site.xml") ) {
                    if ( verbose ) System.err.println("Reading \"" + name + "\" into Configuration.");
                    ByteArrayOutputStream boas = new ByteArrayOutputStream();
                    IOUtils.copy(zipInputStream, boas);
                    configuration.addResource(new ByteArrayInputStream(boas.toByteArray()), name);
                }
                zipEntry = zipInputStream.getNextZipEntry();
            }
        }
        return configuration;
    }

    /**
     * Sets the log configuration to log to stderr.
     *
     * Since we are writing the file to stdout, for proper redirection we should log to stderr, not the default stdout.
     */
    private static void logToStdErr() {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN),
                ConsoleAppender.SYSTEM_ERR));
    }

    public static void main(String[] args) throws CmdLineException {
        logToStdErr();
        Cdh2EzProperties program = new Cdh2EzProperties();
        final CmdLineParser cmdLineParser = new CmdLineParser(program);

        if (args.length == 0) {
            cmdLineParser.printUsage(System.err);
            System.exit(-1);
        }
        cmdLineParser.parseArgument(ImmutableList.copyOf(args));

        Logger.getRootLogger().setLevel(program.verbose ? Level.DEBUG : Level.ERROR);

        program.run();
    }

}
