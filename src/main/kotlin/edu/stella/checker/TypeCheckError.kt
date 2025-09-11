package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.Diag
import edu.stella.core.quote
import org.antlr.v4.kotlinruntime.RuleContext
import org.antlr.v4.kotlinruntime.TokenStream

abstract class TypeCheckError(
    val error: TypeCheckErrorKind,
    val ctx: RuleContext,
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

class DiagUndefinedVariable(variable: stellaParser.VarContext, ctx: RuleContext) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNDEFINED_VARIABLE,
    ctx,
    "variable ${variable.name!!.text!!.quote()} is undefined"
)

class DiagNotAFunction private constructor(func: stellaParser.ExprContext, app: stellaParser.ExprContext) : TypeCheckError(
    TypeCheckErrorKind.ERROR_NOT_A_FUNCTION,
    app,
    "Callee ${func.text.quote()} is not a function"
) {
    constructor(application: stellaParser.ApplicationContext) : this(application.func!!, application)
    constructor(fix: stellaParser.FixContext) : this(fix.expr(), fix)
}

class DiagNotATuple(tuple: stellaParser.ExprContext) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNDEFINED_VARIABLE,
    tuple.getParent() ?: tuple,
    "${tuple.text.quote()} is not a tuple"
)
