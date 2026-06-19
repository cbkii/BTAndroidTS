#!/system/bin/sh

ui_print "BTAndroidTS privileged module"
ui_print "This module places BTAndroidTS as a priv-app and grants only BLUETOOTH_PRIVILEGED."
ui_print "It does not replace Bluetooth.apk, HAL, firmware, Topway services, or pairing databases."
ui_print "Create a file named disable in the module directory to skip boot-time reconcile scripts."
