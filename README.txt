README.TXT

Quick Start
-----------

OpenPipeline requires Java 1.6 or higher.

Once it is installed and running, use your browser to navigate to
http://localhost:8080 to reach the admin application.


Windows Installation
--------------------

Run the installer. If the appropriate version of Java is installed, the system
will use it; otherwise the installer will provide an option to download it from
the OpenPipeline site.

Once the system is installed, you can run it by selecting "OpenPipeline" on the
start menu. You can also run it by calling "openpipeline.exe" on the command
line, or by running it directly in Java (see "Running OpenPipeline From The
Command Line" below).

You can install the system as a Windows service using "op_service.exe". Run
"op_service /install" to create a service that will start when Windows is
rebooted. Run "op_service /install-demand" to create a service that does not
start on reboot, but can be run on demand using the Service Manager. Use the
"/uninstall" switch to uninstall, "/start" to start the service, and "/stop" to
stop it.

Errors or other messages will be saved in log files in the /logs directory.


Linux/Solaris/Unix Installation
-------------------------------

Uncompress the .tar.gz file to a convenient directory. The directory contains a
startup script, "op_service", which allows you to start and stop the server.
Call "./op_service start" to start, and "./op_service stop" to stop. You can
also stop the server in the admin application using the "Stop Server" menu
option.

To run OpenPipeline as a daemon on Unix-like platforms, the start/stop script
has to be integrated into the boot sequence by the administrator. This is often
done by editing runlevels.

Errors or other messages will be saved in log files in the /logs directory.


Mac OS X Installation
---------------------

Click on the downloaded file and follow the instructions in the GUI installer.


Running OpenPipeline From The Command Line
------------------------------------------------

It is possible to start the server from the command line. On most
operating systems the command is similar:

java -Xmx256m -server -Dapp.home=openpipe-X.X.X -cp lib/*; org.openpipeline.server.Server browser gui

Note that this particular -cp command adds all jars in the /lib directory to the
classpath, but it does not look into subdirectories. If you have any jars
in subdirectories you'll have to add them to the classpath explicitly.
On Unix-type systems, separate the jars with colons instead of semicolons.

The -Xmx256m option tells Java to allocate 256mb to the search engine. Adjust
this number as appropriate.

The -server option tell the JVM to run in server mode. This yields better
performance.

The -Dapp.home option specifies the installation directory. Modify this
to point to the actual directory you're using.

The -cp option is the classpath. Be sure to include all .jar files in the /lib
directory.

org.openpipeline.server.Server is the main class for the server. The 
"browser" parameter tells the system to launch the default browser and navigate 
to the main page for the admin. Omit this parameter if the system is installed on a
remote machine. The "gui" parameter tells the system to start a small client-side
Java app with a small window with buttons for launching the browser, seeing the 
console, and stopping the app. Again, omit this parameter on a remote machine.

If you get the error "Error: no `server' JVM at ...", make sure that you have a
full JDK installed, not just the Java runtime, and that the path points to the
java command in the JDK directory. The -server parameter isn't essential, but
the system run significantly faster with it.


More Information
----------------

To get started developing your own applications, see the Developer's Guide and
the API documentation.

For installation or support questions, visit our site at http://www.openpipeline.org
