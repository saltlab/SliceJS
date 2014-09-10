SliceJS
=======

SliceJS assists developers in identifying the control and data dependencies for a given JavaScript variable or line. Through a combination of automated selective code transformation and dynamic backwards slicing, SliceJS is able to link related JavaScript statements together, based on some initial slicing criteria. The selective instrumentation is based on a static analysis algorithm and allows our tool to mantain a low runtime overhead while collecting a more concise execution trace. Slicing has a multitude of uses and this tool in particular was recently used in our fault localization tool [wiki](http://salt.ece.ubc.ca/software/camellia/).

After exercising a target web application, SliceJS generates an interactive  visualization that allows users to replay an application's execution at the JavaScript source code level. This allows developers to deterministically debug their applications offline, once some desired behaviour has been captured.


More technical documentation to come on our [wiki](https://github.com/saltlab/slicejs/wiki).

## Using SliceJS

Currrently, the SliceJS project is designed to run from within the Eclipse IDE. Additionally, both [Mozilla Firefox](http://www.mozilla.org/en-US/firefox/new/) and [Apache Maven](http://maven.apache.org/download.cgi) will be needed to successfully run this project.

### Installation

To install, checkout the project from GitHub and import it into Eclipse as an existing Maven project (File > Import... > Maven > Existing Maven Projects). In order to do this you may need the [m2e plugin for Eclipse](http://eclipse.org/m2e/download/). This provides Maven integration for Eclipse and simplifies the handling of project dependencies. Please note that it may take a few minutes to compile SliceJS after the first import.

### Configuration

SliceJS has been tested with the photo gallery application [Phormer](http://p.horm.org/er/) and the [WSO2 Enterprise Store](http://wso2.com/products/enterprise-store/). These example applications contain some basic synchronous and asynchronous JavaScript events. To use SliceJS with the Phormer gallery application, download Phormer and deploy it locally using a personal webserver such as [MAMP](http://www.mamp.info/en/index.html). Then, run the SliceJS project via the [Main]() class with the following arguments (change server url based on your own configuration).


```
--server localhost:8888/?feat=slideshow --file phorm.js --line 274 --variable ss_cur
```

Doing so will create a new browser instance of FireFox, which will automatically navigate to the provided URL. From here you are free to exercise the application's underlying JavaScript code. To better utilize SliceJS with Phormer, please add a few photos to the application before running our tool. 

Camellia can be tested with the Enterprise Store in a similar manner. First, download the WSO2 application binary files from the above linked URL and then follow the provided instructions to deploy the application locally (running the appropriate script from within the (``bin``) folder).

In order to test your own web-application using Camellia, deploy the application and provide an initial slicing criteria to SliceJS in a similar fashion as shown above.

### Running the Tool 

The Jetty server must be started before running SliceJS. First, navigate to the root directory of SliceJS (where you checked-out SliceJS) and execute the following from your command-line (Terminal, etc.):

```
mvn jetty:run
```

If successful, a notification should appear confirming that the server is up-and-running  
(``[INFO] Started Jetty Server``). Next, run the SliceJS project as a Java application from Eclipse by setting [com.slicejs.core.Main]() as the Main class. 

Lastly, the outputted visualization can be viewed at the following address while the Jetty server is running:

```
http://localhost:8080/view.html
```

## Contributing

Your feedback is valued! Please use the [Issue tracker](https://github.com/saltlab/slicejs/issues) for reporting bugs or feature requests.
