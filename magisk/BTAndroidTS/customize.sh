# BTAndroidTS Magisk Module Customization Script

# This script is sourced by the Magisk module installer.
# It runs in a standalone BusyBox ash shell.

ui_print "****************************************"
ui_print "*   BTAndroidTS Privileged Installer   *"
ui_print "****************************************"

# Check API level
if [ "$API" -lt 29 ]; then
  ui_print "! This module is designed for Android 10 (API 29) and above."
  ui_print "! Current API: $API"
fi

ui_print "- Installing BTAndroidTS as systemless privileged app"

# The Magisk installer automatically extracts the system folder and sets
# default permissions (0755 for dirs, 0644 for files).
# We explicitly set them here to be certain.

set_perm_recursive "$MODPATH/system/priv-app/BTAndroidTS" 0 0 0755 0644
set_perm "$MODPATH/system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml" 0 0 0644

ui_print "- Ensuring no stale files"
rm -f "$MODPATH/system/priv-app/BTAndroidTS/README.md"

ui_print "- Installation successful"
