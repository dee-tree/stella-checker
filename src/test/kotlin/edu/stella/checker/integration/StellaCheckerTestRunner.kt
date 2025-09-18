package edu.stella.checker.integration

import edu.stella.checker.TypeCheckErrorKind
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.StellaCompiler
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import kotlin.test.Ignore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StellaCheckerTestRunner {

    @TestFactory
    @Ignore
    fun `stella tests all`() = TestDataSource().map { data ->
        data.toTest()
    }

    @Nested
    inner class `step 1` {
        @TestFactory
        fun `ok`() = TestDataSource(TestDataSource.step1(TestDataSource.passing())).map { data ->
            data.toTest()
        }

        @TestFactory
        fun `bad`() = TestDataSource(TestDataSource.step1(TestDataSource.failing())).map { data ->
            data.toTest()
        }
    }

    @Nested
    inner class `step 2` {
        @TestFactory
        fun `ok`() = TestDataSource(TestDataSource.step2(TestDataSource.passing())).map { data ->
            data.toTest()
        }

        @TestFactory
        fun `bad`() = TestDataSource(TestDataSource.step2(TestDataSource.failing())).map { data ->
            data.toTest()
        }
    }

    fun TestData.toTest(): DynamicTest = DynamicTest.dynamicTest(name) {
        run(expected, sourceCode) { name }
    }

    fun run(
        expected: TestResult,
        sourceCode: String,
        testName: () -> String = { -> "" }
    ) {
        val compiler = StellaCompiler(sourceCode)
        compiler.compile()

        checkResult(expected, compiler.diagEngine, testName)
    }

    private fun checkResult(
        expected: TestResult,
        actual: DiagnosticsEngine,
        testName: () -> String = { -> "" }
    ) {
        if (expected.isPassed) {
            assertFalse("Expected successful result for test `${testName()}`, but it has error") { actual.hasError }
            return
        }

        expected as TestResult.Failed
        assertTrue("Error (${expected.diag}) is expected as a result for test `${testName()}`, but it finished without error") { actual.hasError }
        assertTrue("Expected error `${expected.diag}` as a result for test `${testName()}`") { expected.diag in actual }
    }
}

sealed class TestResult(val isPassed: Boolean) {
    data object Passed : TestResult(isPassed = true)
    data class Failed(val diag: TypeCheckErrorKind) : TestResult(isPassed = false)
}