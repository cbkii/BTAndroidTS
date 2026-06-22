#!/system/bin/sh
MODDIR=${0%/*}
LOGDIR=/data/local/tmp/btandroidts
LOGFILE="$LOGDIR/module-service.log"

if [ -f "$MODDIR/disable" ]; then
  exit 0
fi

mkdir -p "$LOGDIR" 2>/dev/null
{
  echo "BTAndroidTS service reconcile start $(date)"
  pm path com.cbkii.btandroidts 2>/dev/null || true
  dumpsys package com.cbkii.btandroidts 2>/dev/null | grep -E "BLUETOOTH_PRIVILEGED|userId|pkgFlags" || true
  echo "BTAndroidTS service reconcile end $(date)"
} >> "$LOGFILE" 2>&1

exit 0
