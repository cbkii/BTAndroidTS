package com.cbkii.btandroidts.domain.peripheral

interface DiagnosticsExporter {
	fun availableTargets(): List<DiagnosticsExportTarget>
	suspend fun exportLocal(redactMacAddresses: Boolean = true): Result<DiagnosticsExportResult>
}

data class DiagnosticsExportTarget(
	val label: String,
	val path: String,
	val writable: Boolean,
	val evidence: EvidenceSource,
)

data class DiagnosticsExportResult(
	val path: String,
	val byteCount: Long,
	val lineCount: Int,
)
