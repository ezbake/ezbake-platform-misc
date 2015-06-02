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

package ezbake.utils.encrypted.properties.base;

import ezbake.common.properties.EzProperties;
import ezbake.common.security.SharedSecretTextCryptoProvider;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Properties;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;




public abstract class BaseApp {
    @Option(name="-s", aliases="--secret", usage="The secret that we will use to encrypt/decrypt the properties",
            required=true)
    protected String secret;

    @Option(name="-o", aliases="--output-file", usage="The file to write out to")
    protected String outputFile = null;

    @Option(name="-i", aliases="--input-file", usage="The input file to read from")
    protected String inputFile = null;

    Properties props = new Properties();
    @Option(name="-p", metaVar="<property>=<value>", usage="use value for given property")
    void setProperty(final String property) throws CmdLineException {
        int index = property.indexOf('=');
        if(index == -1) {
            throw new RuntimeException("Properties must be in form of <key>=<value>");
        }

        props.setProperty(property.substring(0, index), property.substring(index+1, property.length()));
    }

    public void execute(String [] args) throws Exception {
        CmdLineParser cmdLineParser = new CmdLineParser(this);
        if(args.length == 0) {
            cmdLineParser.printUsage(System.err);
            System.exit(-1);
        }

        cmdLineParser.parseArgument(args);

        if(secret == null || secret.isEmpty()) {
            throw new RuntimeException("A secret must be specified to encrypt and decrypt the properties");
        }

        if(inputFile != null && !inputFile.isEmpty()) {
            Properties propsLoadedFromFile = new Properties();
            propsLoadedFromFile.load(new FileReader(inputFile));
            props.putAll(propsLoadedFromFile);
        }

        if(props.isEmpty()) {
            throw new RuntimeException("No input given, either specify a -i or -p");
        }

        EzProperties ezProperties = new EzProperties();
        ezProperties.setTextCryptoProvider(new SharedSecretTextCryptoProvider(secret));

        for(String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            doCryptoMethod(ezProperties, key, value);
        }

        if(outputFile == null) {
            for(String property : ezProperties.stringPropertyNames()) {
                System.out.println(property + "=" + ezProperties.get(property));
            }
        } else {
            PrintWriter pw = new PrintWriter(outputFile);
            ezProperties.store(pw, "");
            pw.close();
        }

    }

    public abstract void doCryptoMethod(EzProperties ezProps, String key, String value);

}
