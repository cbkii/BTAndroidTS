package com.cbkii.btandroidts.domain.exceptions

class BLEIndicationOrNotifyRunningException :
	Exception("Some of characteristics are using notify and indication turn it off to continue")