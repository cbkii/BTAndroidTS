package com.cbkii.btandroidts.presentation.feature_opp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.cbkii.btandroidts.MainActivity
import com.cbkii.btandroidts.data.opp.AndroidOppShareIntentParser
import com.cbkii.btandroidts.domain.peripheral.FileTransferController
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OppShareActivity : Activity(), KoinComponent {

	private val fileTransferController: FileTransferController by inject()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val result = AndroidOppShareIntentParser().parse(intent)
		result.getOrNull()?.let { request ->
			fileTransferController.delegateToStockOpp(this, request)
		}
		val launch = Intent(this, MainActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			putExtra(EXTRA_OPP_SHARE_ERROR, result.exceptionOrNull()?.message)
			result.getOrNull()?.let { request ->
				putParcelableArrayListExtra(
					EXTRA_OPP_SHARE_URIS,
					ArrayList<Uri>(request.items.mapNotNull { it.uri })
				)
				putExtra(EXTRA_OPP_SHARE_TEXT, request.items.firstOrNull { it.text != null }?.text)
				putExtra(EXTRA_OPP_SHARE_MIME, request.mimeType)
			}
		}
		startActivity(launch)
		finish()
	}

	companion object {
		const val EXTRA_OPP_SHARE_URIS = "com.cbkii.btandroidts.extra.OPP_SHARE_URIS"
		const val EXTRA_OPP_SHARE_TEXT = "com.cbkii.btandroidts.extra.OPP_SHARE_TEXT"
		const val EXTRA_OPP_SHARE_MIME = "com.cbkii.btandroidts.extra.OPP_SHARE_MIME"
		const val EXTRA_OPP_SHARE_ERROR = "com.cbkii.btandroidts.extra.OPP_SHARE_ERROR"
	}
}
