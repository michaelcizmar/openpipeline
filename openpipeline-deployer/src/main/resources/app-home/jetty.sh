#!/bin/sh

#Enginez Startup script.

usage()
{
    echo "Usage: ${0##*/} {start|stop|restart|status}"
    exit 1
}

# If no commandline parameter is provided, call usage method.
[ $# -gt 0 ] || usage



#Set Variables
JETTY_USER=ec2-user
JETTY_PORT=8080
APP_HOME=/home/ec2-user/app-home
CLASSPATH=${APP_HOME}/lib/*

#NOTE: exec command replaces jetty.sh process image with the java process image.
JETTY_CMD=(exec java -Xmx512M -server -Djetty.port=$JETTY_PORT -Djetty.home=\"$APP_HOME\" -Dapp.home=\"$APP_HOME\" -cp \"$CLASSPATH\" $app_launcher$)

##################################################
# Some utility functions
##################################################
findDirectory()
{
  local L OP=$1
  shift
  for L in "$@"; do
    [ "$OP" "$L" ] || continue 
    printf %s "$L"
    break
  done 
}

running()
{
  local PID=$(cat "$1" 2>/dev/null) || return 1
  kill -0 "$PID" 2>/dev/null
}

#####################################################
# Find a location for the pid file
#####################################################
if [ -z "$JETTY_RUN" ] 
then
  JETTY_RUN=$(findDirectory -w /var/run /usr/var/run /tmp)
fi

#####################################################
# Find a PID for the pid file
#####################################################
if [ -z "$JETTY_PID" ] 
then
  JETTY_PID="$JETTY_RUN/jetty.pid"
fi

####################
# Start Jetty Server
####################

start(){
    echo -n "Jetty Server: "

    if (( NO_START )); then 
      echo "Not starting jetty - NO_START=1";
      exit
    fi
	
	# Check if jetty server is already running.
	if [ -f "$JETTY_PID" ]
	then
	if running $JETTY_PID
	then
	  echo "Already Running!"
	  exit 1
	else
	  # dead pid file - remove
	  rm -f "$JETTY_PID"
	fi
	fi

	# If jetty user is defined, switch this user before starting the process.
	if [ "$JETTY_USER" ] 
	then
	touch "$JETTY_PID"
	chown "$JETTY_USER" "$JETTY_PID"
	su -l "$JETTY_USER" -c "
	  ${JETTY_CMD[@]} &
	  disown \$!
	  echo \$! > '$JETTY_PID'"
	else
	
	#Default command to start up jetty server
	"${JETTY_CMD[@]}" &
	disown $!
	echo $! > "$JETTY_PID"
	fi

	
	# Get Jetty pid
	PID=$(cat "$JETTY_PID" 2>/dev/null)

	echo "STARTED at `date`. (pid=$PID)" 	
}

##################
#Stop Jetty Server
##################

stop(){
	echo -n "Jetty Server: "

	#Kill jetty using pid.

	PID=$(cat "$JETTY_PID" 2>/dev/null)
	kill "$PID" 2>/dev/null

	TIMEOUT=30
	while running $JETTY_PID; do
	if (( TIMEOUT-- == 0 )); then
	  kill -KILL "$PID" 2>/dev/null
	fi

	sleep 1
	done

	rm -f "$JETTY_PID"
	echo "STOPPED"
}

################################
# Display status of Jetty Server
################################

status(){
	echo -n "Jetty Server: "
	if [ -f "$JETTY_PID" ]
	then
		if running "$JETTY_PID"
	      then
	      		PID=$(cat "$JETTY_PID" 2>/dev/null)
      			echo "RUNNING! (pid=$PID)"
		      exit 1
	      else
      			# dead pid file - remove
		       rm -f "$JETTY_PID"
		       echo "STOPPED"
	      fi
	else
		echo "STOPPED"
    fi
}

case "$1" in
  start)
	start
	;;
  stop)
	stop
	;;
  restart)
	stop
	start
	;;

  status)
    status
    ;;
  *)
	
  usage
    ;;
esac

exit 0