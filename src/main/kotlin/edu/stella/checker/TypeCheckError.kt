package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.Diag
import edu.stella.core.quote
import edu.stella.type.Ty
import org.antlr.v4.kotlinruntime.RuleContext
import org.antlr.v4.kotlinruntime.TokenStream

abstract class TypeCheckError(
    val error: TypeCheckErrorKind,
    val ctx: RuleContext,
    val message: TokenStream.() -> String
) : Diag(DiagKind.ERROR) {
    override fun toString(tokenStream: TokenStream): String {
        val lineSeparator = "-".repeat(80)
        return """$error: ${message(tokenStream)} in
$lineSeparator
${tokenStream.getText(ctx)}
$lineSeparator
    """.trimMargin()
    }
}

class DiagUnexpectedTypeForExpr(
    expr: RuleContext,
    expected: Ty,
    actual: Ty?
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION,
    expr.getParent() ?: expr,
    { "Expected type ${expected.toString().quote()} for expression ${getText(expr).quote()}, but got ${actual.toString().quote()}" }
)

class DiagMissingMain(ctx: stellaParser.ProgramContext) : TypeCheckError(
    TypeCheckErrorKind.ERROR_MISSING_MAIN, ctx, { "main".quote() + " is missing" })

class DiagUndefinedVariable(variable: stellaParser.VarContext, ctx: RuleContext) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNDEFINED_VARIABLE,
    ctx,
    { "variable ${variable.name!!.text!!.quote()} is undefined" }
)

class DiagNotAFunction private constructor(func: stellaParser.ExprContext, app: stellaParser.ExprContext, actualTy: Ty? = null) : TypeCheckError(
    TypeCheckErrorKind.ERROR_NOT_A_FUNCTION,
    app,
    { "Expected a function instead of ${getText(func).quote()} ($actualTy)" }
) {
    constructor(application: stellaParser.ApplicationContext, actualTy: Ty?) : this(application.func!!, application, actualTy)
    constructor(fix: stellaParser.FixContext, actualTy: Ty?) : this(fix.expr(), fix, actualTy)
}

class DiagNotATuple(tuple: stellaParser.ExprContext, actualTy: Ty?) : TypeCheckError(
    TypeCheckErrorKind.ERROR_NOT_A_TUPLE,
    tuple.getParent() ?: tuple,
    { "Expected a tuple instead of ${getText(tuple).quote()} (${actualTy})" }
)

class DiagNotARecord(record: stellaParser.ExprContext, actualTy: Ty?) : TypeCheckError(
    TypeCheckErrorKind.ERROR_NOT_A_RECORD,
    record.getParent() ?: record,
    { "Expected a record instead of ${getText(record).quote()} ($actualTy)" }
)

class DiagNotAList(list: stellaParser.ExprContext, actualTy: Ty?) : TypeCheckError(
    TypeCheckErrorKind.ERROR_NOT_A_LIST,
    list.getParent() ?: list,
    { "Expected a list instead of ${getText(list).quote()} ($actualTy)" }
)

class DiagUnexpectedLambda(lambda: stellaParser.ExprContext, expectedTy: Ty?) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_LAMBDA,
    lambda.getParent() ?: lambda,
    { "Expected an expression of type ${expectedTy?.toString()?.quote()}, but got lambda ${getText(lambda).quote()}" }
)

class DiagUnexpectedParameterType(
    parameter: stellaParser.ParamDeclContext,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_TYPE_FOR_PARAMETER,
    parameter.getParent() ?: parameter,
    { "Unexpected parameter ${parameter.name!!.text!!.quote()} type. Expected ${expectedTy.toString().quote()}, but got ${getText(parameter.paramType!!).quote()}" }
)

class DiagUnexpectedTuple(
    tuple: stellaParser.TupleContext,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_TUPLE,
    tuple.getParent() ?: tuple,
    { "Expected an expression of type ${expectedTy.toString().quote()}, but got tuple ${getText(tuple).quote()}" }
)

class DiagUnexpectedRecord(
    record: stellaParser.RecordContext,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_RECORD,
    record.getParent() ?: record,
    { "Expected an expression of type ${expectedTy.toString().quote()}, but got record ${getText(record).quote()}" }
)