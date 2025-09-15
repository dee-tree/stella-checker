package edu.stella.checker

import edu.stella.core.StellaCompiler
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SanityTest {
    @Test
    fun testOk() {
        val compiler = StellaCompiler("""
language core;

extend with #structural-patterns;

fn main(n : Nat) -> Nat {
  return match true {
    	true => 0
      | false => 0
   }
}
        """.trimIndent())
        compiler.compile()
        assertFalse { compiler.diagEngine.hasError }
    }

    @Test
    fun testBad() {
        val compiler = StellaCompiler("""
language core;

fn main(n : Nat) -> Bool {
    return fn(i : Nat) { return i }
}
        """.trimIndent())
        compiler.compile()
        assertTrue { compiler.diagEngine.hasError }
    }
}