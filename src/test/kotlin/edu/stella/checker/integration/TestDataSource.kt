package edu.stella.checker.integration

import edu.stella.checker.TypeCheckErrorKind
import java.io.File
import kotlin.io.path.Path
import kotlin.runCatching

class TestDataSource(
    val filter: (expected: TestResult) -> Boolean = { true }
) : Iterable<TestData>, Iterator<TestData> {

    private val testDataPath = Path("./src/test/resources/integration")
    private val files = testDataPath.toFile().walk().iterator()

    private var next: Pair<File, TestResult>? = null

    init {
        nextFile()
    }

    override fun iterator(): Iterator<TestData> = this

    override fun hasNext(): Boolean = next != null

    override fun next(): TestData = next?.toTestData()?.also {
        next = null
        nextFile()
    } ?: throw IllegalStateException("No tests remaining")

    private fun Pair<File, TestResult>.toTestData() = TestData(
        first.name,
        first.readText(),
        second
    )

    private fun nextFile() {
        for (file in files) {
            if (!file.isFile) continue

            val testResult = when (file.parentFile.name) {
                "ok" -> TestResult.Passed
                else -> {
                    val grand = file.parentFile.parentFile
                    when (val errorName = grand.name) {
                        "bad" if TypeCheckErrorKind.values().none { it.name == file.parentFile.name } -> {
                            System.err.println("No test `${file.parentFile.name}` found!")
                            continue
                        }
                        "bad" -> TestResult.Failed(TypeCheckErrorKind.valueOf(file.parentFile.name))
                        else -> continue
                    }
                }
            }

            if (!filter(testResult)) continue

            next = file to testResult
            return
        }
    }

    companion object {
        fun passing(prefilter: (TestResult) -> Boolean = { true }) = { it: TestResult -> prefilter(it) && it.isPassed }
        fun step1(prefilter: (TestResult) -> Boolean = { true }) = { it: TestResult -> prefilter(it) && (passing()(it) || (it as TestResult.Failed).diag.isStep1) }
        fun step2(prefilter: (TestResult) -> Boolean = { true }) = { it: TestResult -> prefilter(it) && (passing()(it) || (it as TestResult.Failed).diag.isStep2) }

        fun failing(prefilter: (TestResult) -> Boolean = { true }) = { it: TestResult -> prefilter(it) && !it.isPassed }

    }
}