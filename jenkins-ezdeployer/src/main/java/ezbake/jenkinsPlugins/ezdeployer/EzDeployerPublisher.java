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

package ezbake.jenkinsPlugins.ezdeployer;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ezbake.base.thrift.EzSecurityToken;
import ezbake.configuration.DirectoryConfigurationLoader;
import ezbake.configuration.EzConfiguration;
import ezbake.deployer.utilities.PackageDeployer;
import ezbake.security.client.EzbakeSecurityClient;
import ezbake.services.deploy.thrift.EzBakeServiceDeployer;
import ezbake.thrift.ThriftClientPool;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.remoting.Callable;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains all the logic for performing an ezdeployer build from Jenkins
 * Created by sstapleton on 2/4/14.
 */
@SuppressWarnings("unused")
public class EzDeployerPublisher extends Recorder implements Serializable {
    public static String DISPLAY_NAME = "EZ DEPLOYER";

    private final java.util.List<DeploymentPackage> packages = new ArrayList<DeploymentPackage>();
    private final String ezBakeInstanceName;
    private final String propFile;

    @DataBoundConstructor
    public EzDeployerPublisher(String ezBakeInstanceNamed, String propFile) {
        this.ezBakeInstanceName = ezBakeInstanceNamed;
        this.propFile = propFile;
    }

    public java.util.List<DeploymentPackage> getPackages() {
        return packages;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        if (build.getResult() == Result.FAILURE) {
            listener.getLogger().println("The build failed to build properly.  " +
                    "Cannot attempt to deploy packages");
            return true;
        }
        try {
            final BuildListener fListener = listener;
            final FilePath workspace = build.getWorkspace();

            //wrap deployment in callable so that it will be called in the scope of the box it is on.
            Callable<Boolean, IOException> task = new MasterToSlaveCallable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    ThriftClientPool pool = null;
                    EzBakeServiceDeployer.Client client = null;
                    try {
                        Path configFilePath = new File(getPropFile()).toPath();
                        DirectoryConfigurationLoader directoryConfigurationLoader = new DirectoryConfigurationLoader(configFilePath);
                        EzConfiguration config = new EzConfiguration(directoryConfigurationLoader);
                        EzbakeSecurityClient securityClient = new EzbakeSecurityClient(config.getProperties());

                        pool = new ThriftClientPool(config.getProperties());
                        client = pool.getClient(getEzBakeInstanceName(), EzBakeServiceDeployer.Client.class);
                        Boolean retVal = Boolean.TRUE;
                        for (DeploymentPackage dp : getPackages()) {
                            fListener.getLogger().println("Deploying: " + dp.toString());
                            try {
                                File mf = dp.getManifestFile(workspace);
                                if (!mf.exists() || !mf.isFile()) {
                                    fListener.getLogger().println("Build set to unstable, manifest file not found");
                                    retVal = Boolean.FALSE;
                                    continue;
                                }
                                fListener.getLogger().println("Manifest File: " + mf.toURI());
                                File pf = dp.getPackageFile(workspace);
                                if (!pf.exists() || !pf.isFile()) {
                                    fListener.getLogger().println("Build set to unstable, package file not found");
                                    retVal = Boolean.FALSE;
                                    continue;
                                }
                                fListener.getLogger().println("Package File: " + pf.toURI());
                                List<File> cfs = dp.getConfigFiles(workspace);

                                try {
                                    fListener.getLogger().println("Trying to undeploy...");
                                    EzSecurityToken ezSecurityToken = securityClient.fetchAppToken();
                                    PackageDeployer.undeployPackage(client, mf, ezSecurityToken);
                                    fListener.getLogger().println("Undeployed successfully");
                                } catch (Exception ex) {
                                    fListener.getLogger().println("Failed to undeploy.  Maybe there was nothing to undeploy: " +
                                            ex.getMessage());
                                }
                                fListener.getLogger().println("Deploying...");
                                EzSecurityToken ezSecurityToken = securityClient.fetchAppToken();
                                PackageDeployer.deployPackage(client, pf, mf, cfs, null, ezSecurityToken);
                                fListener.getLogger().println("Deployed successfully");

                            } catch (Exception ex) {
                                fListener.getLogger().println(Throwables.getStackTraceAsString(ex));
                                retVal = Boolean.FALSE;
                            }
                        }
                        return retVal;
                    } catch (Exception e) {
                        fListener.getLogger().println(Throwables.getStackTraceAsString(e));
                        return Boolean.FALSE;
                    } finally {
                        if (pool != null) {
                            pool.returnToPool(client);
                            pool.close();
                        }
                    }
                }
            };

            if (!launcher.getChannel().call(task)) {
                build.setResult(Result.UNSTABLE);
            }

        } catch (Exception e) {
            listener.getLogger().println(Throwables.getStackTraceAsString(e));
            build.setResult(Result.UNSTABLE);
            return true;
        }
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getEzBakeInstanceName() {
        return ezBakeInstanceName;
    }

    public String getPropFile() {
        return propFile;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Publisher> implements Serializable {
        private static final String DEFAULT_INSTANCE_NAME = "ezdeployer";
        private String ezDeployerInstanceName;
        private String propFile;


        public DescriptorImpl() {
            super(EzDeployerPublisher.class);
            load();
        }

        protected DescriptorImpl(Class<? extends Publisher> clazz) {
            super(clazz);
        }

        @Override
        public String getDisplayName() {
            return EzDeployerPublisher.DISPLAY_NAME;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) {
            EzDeployerPublisher db = new EzDeployerPublisher(getEzDeployerInstanceName(), getPropFile());
            Gson gson = new GsonBuilder().create();
            JSONArray pkgs = formData.optJSONArray("packages");

            if (pkgs != null) {
                for (Object obj : formData.getJSONArray("packages")) {
                    DeploymentPackage pack = gson.fromJson(obj.toString(), DeploymentPackage.class);
                    db.getPackages().add(pack);
                }
            } else {
                JSONObject pkg = formData.optJSONObject("packages");
                if (pkg != null) {
                    DeploymentPackage pack = gson.fromJson(pkg.toString(), DeploymentPackage.class);
                    db.getPackages().add(pack);
                }
            }
            return db;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            //Sets the ezbake instance name from the jenkins config, or uses a default if none was given.
            String instanceName = formData.getString("instanceName");
            if (instanceName == null || instanceName.length() == 0) {
                instanceName = DEFAULT_INSTANCE_NAME;
            }
            setEzDeployerInstanceName(instanceName);

            //Sets the properties file that will be used by EZConfiguration.
            setPropFile(formData.getString("propFile"));

            save();
            return super.configure(req, formData);
        }

        public String getEzDeployerInstanceName() {
            return ezDeployerInstanceName;
        }

        public void setEzDeployerInstanceName(String ezDeployerInstanceName) {
            this.ezDeployerInstanceName = ezDeployerInstanceName;
        }

        public void setPropFile(String propFile) {
            this.propFile = propFile;
        }

        public String getPropFile() {
            return propFile;
        }
    }
}
