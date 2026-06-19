package com.cbkii.btandroidts.data.opp

import android.content.Intent
import android.net.Uri
import com.cbkii.btandroidts.domain.peripheral.OppShareItem
import com.cbkii.btandroidts.domain.peripheral.OppShareRequest

class AndroidOppShareIntentParser {

	@Suppress("DEPRECATION")
	fun parse(intent: Intent): Result<OppShareRequest> {
		val mimeType = intent.type
		val uris = when (intent.action) {
			Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
			Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
			else -> return Result.failure(IllegalArgumentException("Unsupported share action: ${intent.action}"))
		}.distinct()

		if (uris.isEmpty()) return Result.failure(IllegalArgumentException("Share intent did not include stream URIs"))

		return Result.success(
			OppShareRequest(
				items = uris.map { uri -> OppShareItem(uri = uri, mimeType = mimeType) },
				mimeType = mimeType,
			)
		)
	}
}
