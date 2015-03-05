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

package ezbake.endpoint.web;

import ezbake.base.thrift.EzBakeBaseService;
import ezbake.common.openshift.OpenShiftUtil;
import ezbake.thrift.ThriftUtils;

import java.util.Properties;


public class BaseClient {
    private static EzBakeBaseService.Client client;
    private Properties configuration;

    private static class BaseClientHolder {
        private static final BaseClient INSTANCE = new BaseClient();
    }

    public void setup(Properties configuration) throws Exception {
        this.configuration = configuration;

    }

    public static BaseClient getInstance() {
        return BaseClientHolder.INSTANCE;
    }

    public boolean ping() throws Exception {
        try {
            client = ThriftUtils.getClient(EzBakeBaseService.Client.class, OpenShiftUtil.getThriftPrivateInfo(), configuration);
            return client.ping();
        } finally {
            ThriftUtils.quietlyClose(client);
        }
    }

    public Properties getConfiguration() {
        return configuration;
    }
}
