package com.cbkii.btandroidts.domain.exceptions

class BluetoothConnectException(errorCode: Int, mError: String) :
	Exception("Cannot connect to bluetooth code:$errorCode message:$mError")