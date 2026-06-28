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
		assertEquals("18.0.1", BuildConfig.VERSION_NAME)
	}
}
