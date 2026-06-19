#!/system/bin/sh
MODDIR=${0%/*}

if [ -f "$MODDIR/disable" ]; then
  exit 0
fi

# No partition writes, no Bluetooth stack restarts, no vendor service changes.
exit 0
