package com.cbkii.btandroidts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductIdentityTest {

	@Test
	fun packageIdentityMatchesPrivilegedAllowlistTarget() {
		assertTrue(BuildConfig.APPLICATION_ID.startsWith("com.cbkii.btandroidts"))
	}

	@Test
	fun versionNameIsTs18ProductBaseline() {
		assertTrue("Version must be part of the TS18 18.0.x product baseline", BuildConfig.VERSION_NAME.startsWith("18.0."))
	}
}
