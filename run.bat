if param = start
	java -Xmx256m -server -Dapp.home=%CD% -cp lib/*; org.openpipeline.server.Server browser gui
else if param=stop
	kill pid
	or 