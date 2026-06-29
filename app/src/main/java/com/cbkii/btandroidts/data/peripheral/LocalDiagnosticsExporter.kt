package com.cbkii.btandroidts.data.peripheral

import android.content.Context
import android.os.Environment
import com.cbkii.btandroidts.domain.peripheral.DiagnosticsExportResult
import com.cbkii.btandroidts.domain.peripheral.DiagnosticsExportTarget
import com.cbkii.btandroidts.domain.peripheral.DiagnosticsExporter
import com.cbkii.btandroidts.domain.peripheral.EvidenceSource
import com.cbkii.btandroidts.domain.peripheral.Ts18DiagnosticsCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalDiagnosticsExporter(
	context: Context,
	private val collector: Ts18DiagnosticsCollector,
) : DiagnosticsExporter {

	private val appContext = context.applicationContext

	override fun availableTargets(): List<DiagnosticsExportTarget> = buildList {
		appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { dir ->
			add(
				DiagnosticsExportTarget(
					label = "App external diagnostics",
					path = dir.absolutePath,
					writable = dir.canWrite() || dir.mkdirs(),
					evidence = EvidenceSource.OBSERVED,
				)
			)
		}
		addAll(usbDiskTargets())
	}

	override suspend fun exportLocal(redactMacAddresses: Boolean): Result<DiagnosticsExportResult> =
		withContext(Dispatchers.IO) {
			runCatching {
				val report = withTimeout(COLLECTION_TIMEOUT_MILLIS) { collector.collect() }
				val outputDir = outputDirectory()
				outputDir.mkdirs()
				val outputFile = File(outputDir, "btandroidts-diagnostics-${timestamp()}.txt")
				val body = renderReport(report.lines, report.summary, redactMacAddresses)
				outputFile.writeText(body)
				DiagnosticsExportResult(
					path = outputFile.absolutePath,
					byteCount = outputFile.length(),
					lineCount = body.lineSequence().count(),
				)
			}
		}

	private fun outputDirectory(): File = File(appContext.filesDir, "diagnostics")

	private fun usbDiskTargets(): List<DiagnosticsExportTarget> {
		val storage = File("/storage")
		val volumes = storage.listFiles()
			.orEmpty()
			.filter { it.name.startsWith("usbdisk") }
			.sortedBy(File::getName)
		return volumes.map { volume ->
			val target = File(volume, "BTAndroidTS/diagnostics")
			val writable = if (target.exists()) target.canWrite() else volume.canWrite()
			DiagnosticsExportTarget(
				label = "TS18 USB ${volume.name}",
				path = target.absolutePath,
				writable = writable,
				evidence = EvidenceSource.REQUIRES_DEVICE_VALIDATION,
			)
		}
	}

	private fun renderReport(
		lines: List<String>,
		summary: String,
		redactMacAddresses: Boolean,
	): String {
		val bodyLines = buildList {
			add("BTAndroidTS diagnostics summary")
			add(summary)
			add("")
			add("Details")
			addAll(lines.take(MAX_REPORT_LINES))
		}
		val raw = bodyLines.joinToString(separator = "\n")
		val maybeRedacted = if (redactMacAddresses) {
			MAC_ADDRESS_PATTERN.replace(raw) { match ->
				"${match.groupValues[1]}:${match.groupValues[2]}:xx:xx:xx:${match.groupValues[6]}"
			}
		} else {
			raw
		}
		return maybeRedacted.take(MAX_REPORT_CHARS)
	}

	private fun timestamp(): String =
		SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

	private companion object {
		const val COLLECTION_TIMEOUT_MILLIS = 15_000L
		const val MAX_REPORT_LINES = 600
		const val MAX_REPORT_CHARS = 128 * 1024
		val MAC_ADDRESS_PATTERN = Regex("""\b([0-9A-Fa-f]{2}):([0-9A-Fa-f]{2}):([0-9A-Fa-f]{2}):([0-9A-Fa-f]{2}):([0-9A-Fa-f]{2}):([0-9A-Fa-f]{2})\b""")
	}
}
