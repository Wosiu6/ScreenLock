# Screen Lock Service

This application was written to detect user inactivity and put the screen to sleep when the external screen is connected as the default Android behaviour is not to do it (and there is no way to set it up in the options to my knowledge).

## How does it work

The application detects user clicks anywhere on the screen by setting a 0x0 layout that is being displayed over the screen at all times (when the service is running) and detects clicks outside it (so de facto all clicks).
The application then resets the timer. If the timer is not reset after the default screen timeout and the screen is connected to at least one external display, the screen will lock itself automatically.
