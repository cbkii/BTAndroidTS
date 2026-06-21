package com.cbkii.btandroidts.data.opp

import android.content.Intent
import android.net.Uri
import com.cbkii.btandroidts.domain.peripheral.OppShareItem
import com.cbkii.btandroidts.domain.peripheral.OppShareRequest

class AndroidOppShareIntentParser {

	@Suppress("DEPRECATION")
	fun parse(intent: Intent): Result<OppShareRequest> = runCatching {
		val mimeType = intent.type
		val items = when (intent.action) {
			Intent.ACTION_SEND -> {
				val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
				val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
				buildList {
					if (uri != null) add(OppShareItem(uri = uri, mimeType = mimeType))
					if (uri == null && !text.isNullOrBlank()) {
						add(OppShareItem(text = text, mimeType = mimeType ?: "text/plain"))
					}
				}
			}
			Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
				.orEmpty()
				.filterNotNull()
				.map { uri -> OppShareItem(uri = uri, mimeType = mimeType) }
			else -> throw IllegalArgumentException("Unsupported share action: ${intent.action}")
		}.distinct()

		if (items.isEmpty()) throw IllegalArgumentException("Share intent did not include stream URIs or text")

		OppShareRequest(
			items = items,
			mimeType = mimeType,
		)
	}
}
