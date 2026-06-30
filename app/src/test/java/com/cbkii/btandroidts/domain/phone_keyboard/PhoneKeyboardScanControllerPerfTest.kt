package com.cbkii.btandroidts.domain.phone_keyboard

import org.junit.Test
import kotlin.system.measureTimeMillis

class PhoneKeyboardScanControllerPerfTest {
    @Test
    fun benchmarkMapCopy() {
        val currentCandidates = LinkedHashMap<String, String>()
        for (i in 1..100) {
            currentCandidates["address_$i"] = "Candidate $i"
        }

        val timeBaseline = measureTimeMillis {
            for (i in 1..50000) {
                val working = currentCandidates.toMutableMap()
                working["address_1"] = "New 1"
                currentCandidates.clear()
                currentCandidates.putAll(working)
            }
        }

        currentCandidates.clear()
        for (i in 1..100) {
            currentCandidates["address_$i"] = "Candidate $i"
        }

        val timeOptimized = measureTimeMillis {
            // Reusing a single map?
            for (i in 1..50000) {
                // In place update
                currentCandidates["address_1"] = "New 1"
            }
        }

        println("Baseline Map Copy: $timeBaseline ms")
        println("Optimized In-Place: $timeOptimized ms")
    }
}
