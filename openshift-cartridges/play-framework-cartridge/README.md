# Play Framework Cartridge

This cartridge provides support for binary [Play Framework](http://www.playframework.com/) applications. Applications should be packaged using `play dist` or `activator dist`.

Because the application is delivered already compiled and packaged into a zip file, the cartridge unzips the file, and start the application with the following properties defined:

* `http.port=${OPENSHIFT_PLAY_FRAMEWORK_HTTP_PORT}`
* `http.address=${OPENSHIFT_PLAY_FRAMEWORK_PRIVATE_IP}`
* `application.secret=${OPENSHIFT_SECRET_TOKEN}`
* `pidfile.path=${OPENSHIFT_HOMEDIR}/app-root/runtime/play-framework.pid`
