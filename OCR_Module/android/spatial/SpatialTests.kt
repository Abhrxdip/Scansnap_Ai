package com.smartvendor.ai.ocr.spatial

import kotlin.system.measureTimeMillis
import kotlin.random.Random

fun main() {
    println("--- RUNNING SPATIAL TESTS ---")
    
    val cases = mutableListOf<TestCase>()
    
    // CASE A: THUMS UP
    cases.add(TestCase(
        id = "CASE-A",
        lines = listOf(
            SpatialTextLine("THUMS", 10, 10, 60, 30),
            SpatialTextLine("UP", 10, 32, 40, 52)
        ),
        expected = "THUMS UP"
    ))
    
    // CASE B: TH UMS UP
    cases.add(TestCase(
        id = "CASE-B",
        lines = listOf(
            SpatialTextLine("TH", 10, 10, 30, 30),
            SpatialTextLine("UMS", 35, 10, 70, 30),
            SpatialTextLine("UP", 10, 32, 40, 52)
        ),
        expected = "TH UMS UP"
    ))
    
    // CASE C: JIM JAM
    cases.add(TestCase(
        id = "CASE-C",
        lines = listOf(
            SpatialTextLine("JIM", 10, 10, 40, 30),
            SpatialTextLine("JAM", 10, 32, 40, 52)
        ),
        expected = "JIM JAM"
    ))
    
    // CASE D: ORE O
    cases.add(TestCase(
        id = "CASE-D",
        lines = listOf(
            SpatialTextLine("ORE", 10, 10, 40, 30),
            SpatialTextLine("O", 10, 32, 20, 52)
        ),
        expected = "ORE O"
    ))
    
    // CASE E: Unrelated nearby lines (MRP)
    cases.add(TestCase(
        id = "CASE-E",
        lines = listOf(
            SpatialTextLine("THUMS", 10, 10, 60, 30),
            SpatialTextLine("UP", 10, 32, 40, 52),
            SpatialTextLine("NET WEIGHT 500 ML", 10, 100, 150, 115),
            SpatialTextLine("MRP Rs30", 10, 150, 80, 165)
        ),
        expected = "THUMS UP\nNET WEIGHT 500 ML\nMRP Rs30"
    ))
    
    var correct = 0
    var incorrect = 0
    
    for (case in cases) {
        val out = SpatialTextReconstructor.reconstructToString(case.lines)
        if (out == case.expected) {
            correct++
        } else {
            println("FAILED ${case.id}")
            println("EXPECTED: ${case.expected}")
            println("GOT: $out")
            incorrect++
        }
    }
    
    println("Basic Tests: $correct Correct, $incorrect Incorrect")
    
    // STRESS TEST
    val rng = Random(42)
    var crashes = 0
    var exceptions = 0
    val times = mutableListOf<Long>()
    
    for (i in 0 until 1000) {
        val numLines = rng.nextInt(1, 21)
        val lines = mutableListOf<SpatialTextLine>()
        for (j in 0 until numLines) {
            val left = rng.nextInt(0, 501)
            val top = rng.nextInt(0, 501)
            val w = rng.nextInt(20, 101)
            val h = rng.nextInt(10, 41)
            lines.add(SpatialTextLine("WORD\$j", left, top, left + w, top + h))
        }
        
        try {
            val start = System.nanoTime()
            SpatialTextReconstructor.reconstruct(lines)
            val dur = System.nanoTime() - start
            times.add(dur)
        } catch (e: Exception) {
            crashes++
            exceptions++
        }
    }
    
    val avgTime = if (times.isNotEmpty()) times.average() / 1_000_000.0 else 0.0
    val sortedTimes = times.sorted()
    val p50 = if (times.isNotEmpty()) sortedTimes[(sortedTimes.size * 0.5).toInt()] / 1_000_000.0 else 0.0
    val p95 = if (times.isNotEmpty()) sortedTimes[(sortedTimes.size * 0.95).toInt()] / 1_000_000.0 else 0.0
    val p99 = if (times.isNotEmpty()) sortedTimes[(sortedTimes.size * 0.99).toInt()] / 1_000_000.0 else 0.0
    
    println("Stress Cases: 1000 | Crashes: $crashes | Exceptions: $exceptions")
    println(String.format("Performance: Avg %.3f ms | P50 %.3f ms | P95 %.3f ms | P99 %.3f ms", avgTime, p50, p95, p99))
}

data class TestCase(
    val id: String,
    val lines: List<SpatialTextLine>,
    val expected: String
)
