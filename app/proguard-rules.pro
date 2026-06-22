# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable
-shrinkunusedprotofields

# HID Host on Android 10 is accessed through an API-gated reflection adapter. Keep only the
# platform profile members the adapter probes; do not keep app packages broadly.
-dontwarn android.bluetooth.BluetoothHidHost
-keepclassmembers class android.bluetooth.BluetoothHidHost {
    public boolean connect(android.bluetooth.BluetoothDevice);
    public boolean disconnect(android.bluetooth.BluetoothDevice);
    public java.util.List getConnectedDevices();
    public int getConnectionState(android.bluetooth.BluetoothDevice);
    public boolean setConnectionPolicy(android.bluetooth.BluetoothDevice,int);
    public boolean setPriority(android.bluetooth.BluetoothDevice,int);
}

-keepclassmembers class android.bluetooth.BluetoothProfile {
    public static final int HID_HOST;
}

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
