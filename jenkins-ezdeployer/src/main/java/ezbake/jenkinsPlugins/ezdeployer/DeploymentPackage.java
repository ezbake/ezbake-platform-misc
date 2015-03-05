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

import hudson.FilePath;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the holder for a package. It contains the paths for the packages, manifest, and config files.
 * It also contains methods to retrieve the file associated with those paths.
 * Created by sstapleton on 2/5/14.
 */
public class DeploymentPackage implements Serializable {
    private final String manifestPath;
    private final String packagePath;
    private final String configPaths;//semicolon separated list

    public DeploymentPackage(String manifestPath, String packagePath, String configPaths) {
        this.manifestPath = manifestPath;
        this.packagePath = packagePath;
        this.configPaths = configPaths;
    }

    public String getManifestPath() {
        return manifestPath;
    }

    public File getManifestFile(FilePath workspacePath) {
        String path = getManifestPath();
        if (!path.startsWith("/"))
            path = "/" + path;
        path = workspacePath + path;
        return new File(path);
    }

    public String getPackagePath() {
        return packagePath;
    }

    public File getPackageFile(FilePath workspacePath) {
        String path = getPackagePath();
        if (!path.startsWith("/"))
            path = "/" + path;
        path = workspacePath + path;
        return new File(path);
    }

    public String getConfigPaths() {
        return configPaths;
    }

    public List<File> getConfigFiles(FilePath workspacePath) {
        List<File> configFiles = new ArrayList<File>();
        String[] pathsAsList = getConfigPaths().split(";");
        for (String configPath : pathsAsList) {
            if (configPath.trim().length() == 0)
                continue;
            if (!configPath.startsWith("/"))
                configPath = "/" + configPath.trim();
            configPath = workspacePath + configPath;
            configFiles.add(new File(configPath));
        }
        return configFiles;
    }

    public String toString() {
        String retVal = "Manifest Path: " + getManifestPath() + "\nPackage Path: " + getPackagePath();
        if (getConfigPaths().length() > 0) {
            retVal += "\nConfig Paths:";
            String[] pathsAsList = getConfigPaths().split(";");
            for (String path : pathsAsList) {
                retVal += "\n\t" + path;
            }
        }
        return retVal;
    }

}
