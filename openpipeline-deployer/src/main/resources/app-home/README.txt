jetty.cmd is the windows command to start jetty.

jettyservice is the file that gets copied into init.d on the Linux service. It runs
on startup. All it does is call app.home/jetty.sh

jetty.sh is the script that actually starts jetty.

To start and stop jetty manually, run:

sudo service jettyservice start/stop
