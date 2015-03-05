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


import com.google.common.io.Files;
import ezbakehelpers.ezconfigurationhelpers.application.EzBakeApplicationConfigurationHelper;
import ezbakehelpers.ezconfigurationhelpers.system.SystemConfigurationHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Properties;

public class LogsServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Properties configuration = BaseClient.getInstance().getConfiguration();

        SystemConfigurationHelper sysConfig = new SystemConfigurationHelper(configuration);
        EzBakeApplicationConfigurationHelper appConf = new EzBakeApplicationConfigurationHelper(configuration);

        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.println(Files.toString(new File(sysConfig.getLogFilePath(appConf.getApplicationName(),
                appConf.getServiceName())), Charset.defaultCharset()));

    }

}
