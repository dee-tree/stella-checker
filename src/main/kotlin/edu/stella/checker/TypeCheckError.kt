package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.Diag
import edu.stella.core.quote
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.TokenStream

abstract class TypeCheckError(
    val error: TypeCheckErrorKind,
    val ctx: ParserRuleContext,
    val message: String
) : Diag(DiagKind.ERROR) {
    override fun toString(tokenStream: TokenStream): String {
        val lineSeparator = "-".repeat(80)
        return """$error: $message in
$lineSeparator
${tokenStream.getText(ctx)}
$lineSeparator
    """.trimMargin()
    }
}

class DiagMissingMain(ctx: stellaParser.ProgramContext) : TypeCheckError(
    TypeCheckErrorKind.ERROR_MISSING_MAIN, ctx, "main".quote() + " is missing")