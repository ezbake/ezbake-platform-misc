Building Instructions:
Run "mvn package"
This will deploy the hpi file into the target directory.

To run a local instance of Jenkins with plugin
(Note you will still need to set up a Thrift Client and an EZ Deployer instance to test locally)
run "mvn org.jenkins-ci.tools:maven-hpi-plugin:run"

Install/Use Instructions:

go to Manage Jenkins

Then click Manage Plugins
Go to the "Advanced" tab

Choose file in upload plugin section (This will be the hpi file built from "mvn package")

------------------

Reset Jenkins

------------------

Go back to manage Jenkins

Click configure system

You should see a section in here called "EZ DEPLOYER"
Under this section there will be a field called: "EZ Deployer Instance Name"
It should be defaulted to "ezdeployer"
If the instance of EZ Deployer is different for this server change it here.
There is another field called "Zoo Keeper Connection Key" this is the key that is used to connect to zookeeper.

If you update either of these you also need to go back to the Build config and click save again.  Other wise the build will
not get the new Jenkins config info.
------------------

Go to the desired build

click configure

in the post build step you should see "EZ DEPLOYER" as an option select that

The paths for the files should start at the workspace level.
ie: /app/for/war.war (*this will get prepended with the path to the workspace)

If you have any config files you can add that to the "Config paths" section in a ';' separated list
ie: /app/configs/config1.conf;/app/other/configs/config2.txt

------------------
After setting this up, whenever you run a build it will automatically deploy the packages if the build finished
successfully.  If there is an error in side of deploying it set the build to unstable and log the exception.  If a
deployment other than the first one fails all previous deployment packages may still have been deployed if there wer
no errors in those.
