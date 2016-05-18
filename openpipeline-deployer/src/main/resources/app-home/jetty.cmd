:: OpenPipeline startup script
:: Execute "jetty" (not jetty.cmd) on the commandline to run this script.
@ECHO OFF

:: Set APP_HOME below. For example c:\app-home
SET APP_HOME=

:: Check if APP_HOME is set or not.
IF DEFINED APP_HOME GOTO :CHECK_APP_HOME
ECHO. 
ECHO ERROR: APP_HOME is not set. Open "jetty.cmd" in a text editor and set APP_HOME variable.
GOTO:EOF

:CHECK_APP_HOME
IF EXIST %APP_HOME% GOTO :RUN_JETTY
ECHO.
ECHO ERROR: APP_HOME=%APP_HOME% directory does not exist.
GOTO:EOF
:END

:RUN_JETTY
ECHO.
ECHO Jetty Server: Starting...
SET CLASSPATH="%APP_HOME%\lib\*"
java -Xmx512M -server -Djetty.home="%APP_HOME%" -Dapp.home=%APP_HOME% -cp %CLASSPATH% $app_launcher$
:END