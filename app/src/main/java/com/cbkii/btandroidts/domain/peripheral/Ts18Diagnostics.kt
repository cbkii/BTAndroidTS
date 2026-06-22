package com.cbkii.btandroidts.domain.peripheral

data class Ts18DiagnosticsReport(
	val generatedAtMillis: Long,
	val summary: String,
	val lines: List<String>,
)

interface Ts18DiagnosticsCollector {
	suspend fun collect(): Ts18DiagnosticsReport
}
