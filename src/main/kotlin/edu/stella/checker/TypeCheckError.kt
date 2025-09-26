package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.Diag
import edu.stella.core.quote
import edu.stella.type.TupleTy
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

class DiagUnexpectedVariant(
    variant: stellaParser.VariantContext,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_VARIANT,
    variant.getParent() ?: variant,
    { "Expected an expression of type ${expectedTy.toString().quote()}, but got variant ${getText(variant).quote()}" }
)

class DiagUnexpectedList private constructor(
    list: stellaParser.ExprContext,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_LIST,
    list.getParent() ?: list,
    { "Expected an expression of type ${expectedTy.toString().quote()}, but got list ${getText(list).quote()}" }
) {
    constructor(list: stellaParser.ConsListContext, expectedTy: Ty) : this(list as stellaParser.ExprContext, expectedTy)
    constructor(list: stellaParser.ListContext, expectedTy: Ty) : this(list as stellaParser.ExprContext, expectedTy)
}

class DiagUnexpectedInjection private constructor(
    injection: stellaParser.ExprContext,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_INJECTION,
    injection.getParent() ?: injection,
    { "Expected an expression of type ${expectedTy.toString().quote()}, but got injection ${getText(injection).quote()}" }
) {
    constructor(injection: stellaParser.InlContext, expectedTy: Ty) : this(injection as stellaParser.ExprContext, expectedTy)
    constructor(injection: stellaParser.InrContext, expectedTy: Ty) : this(injection as stellaParser.ExprContext, expectedTy)
}

class DiagMissingRecordField(
    record: stellaParser.RecordContext,
    missing: String,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_MISSING_RECORD_FIELDS,
    record.getParent() ?: record,
    { "Missing field ${missing.quote()} in record ${getText(record).quote()} of expected type ${expectedTy.toString().quote()}" }
)

class DiagUnexpectedRecordField(
    record: stellaParser.RecordContext,
    unexpected: String,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_RECORD_FIELDS,
    record.getParent() ?: record,
    { "Unexpected field ${unexpected.quote()} in record ${getText(record).quote()} of expected type ${expectedTy.toString().quote()}" }
)

class DiagUnexpectedRecordFieldAccess(
    access: stellaParser.DotRecordContext,
    unexpected: String,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_FIELD_ACCESS,
    access.getParent() ?: access,
    { "Unexpected field ${unexpected.quote()} access ${getText(access).quote()} of record ${expectedTy.toString().quote()}" }
)

class DiagUnexpectedVariantLabel(
    variant: stellaParser.VariantContext,
    unexpected: String,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_VARIANT_LABEL,
    variant.getParent() ?: variant,
    { "Unexpected label ${unexpected.quote()} in variant ${getText(variant).quote()} of expected type ${expectedTy.toString().quote()}" }
)

class DiagMissingVariantData(
    variant: stellaParser.VariantContext,
    missingFieldValue: String,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_MISSING_DATA_FOR_LABEL,
    variant.getParent() ?: variant,
    { "Missing value for tag ${missingFieldValue.quote()} in variant ${getText(variant).quote()}" }
)

class DiagUnexpectedValueOfNullaryVariant(
    expr: stellaParser.ExprContext,
    tag: String,
    ty: Ty?,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL,
    expr.getParent() ?: expr,
    { "Unexpected value ${getText(expr).quote()} for nullary tag ${tag.quote()} in variant type ${ty.toString().quote()}" }
)

class DiagTupleIndexOutOfBounds(
    access: stellaParser.DotTupleContext,
    index: Int,
    expectedTy: TupleTy
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_TUPLE_INDEX_OUT_OF_BOUNDS,
    access.getParent() ?: access,
    { "Tuple item access ${index.toString().quote()} out of bound in ${getText(access).quote()} of expected type ${expectedTy.toString().quote()} (1..${expectedTy.components.indices.last + 1})" }
)

class DiagUnexpectedTupleLength(
    tuple: stellaParser.TupleContext,
    expectedTy: TupleTy
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_TUPLE_LENGTH,
    tuple.getParent() ?: tuple,
    { "Unexpected tuple ${getText(tuple).quote()} length (${tuple.exprs.size}) of expected type ${expectedTy.toString().quote()} (${expectedTy.components.size})" }
)

class DiagAmbiguousSumType(
    expr: stellaParser.ExprContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_AMBIGUOUS_SUM_TYPE,
    expr.getParent() ?: expr,
    { "Ambiguous sum-type in expression ${getText(expr).quote()}. Please, specify expected type explicitly" }
) {
    constructor(inl: stellaParser.InlContext) : this(inl as stellaParser.ExprContext)
    constructor(inr: stellaParser.InrContext) : this(inr as stellaParser.ExprContext)
}

class DiagAmbiguousVariantType(
    expr: stellaParser.VariantContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_AMBIGUOUS_VARIANT_TYPE,
    expr.getParent() ?: expr,
    { "Ambiguous variant type in expression ${getText(expr).quote()}. Please, specify expected type explicitly" }
)

class DiagAmbiguousListType(
    list: stellaParser.ListContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_AMBIGUOUS_LIST,
    list.getParent() ?: list,
    { "Ambiguous list type in expression ${getText(list).quote()}. Please, specify expected type explicitly" }
)

class DiagEmptyMatching(
    matching: stellaParser.MatchContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_ILLEGAL_EMPTY_MATCHING,
    matching.getParent() ?: matching,
    { "Unexpected ${"match".quote()} without cases" }
)

class DiagNonExhaustiveMatching(
    matching: stellaParser.MatchContext,
    uncovered: List<String>,
    ofType: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_NONEXHAUSTIVE_MATCH_PATTERNS,
    matching.getParent() ?: matching,
    {
        val uncoveredForm = if (uncovered.size > 1) "are" else "is"
        val uncoveredList = uncovered.joinToString(", ", prefix = "[", postfix = "]") { it.quote() }
        val uncoveredText = if (uncovered.isEmpty()) "" else "$uncoveredList $uncoveredForm uncovered"
        "Non-exhaustive matching over ${ofType.toString().quote()} type: $uncoveredText" }
) {
    constructor(matching: stellaParser.MatchContext, ofType: Ty) : this(matching, emptyList(), ofType)
}

class DiagUnexpectedPatternForType(
    pattern: stellaParser.PatternContext,
    ofType: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_PATTERN_FOR_TYPE,
    pattern.getParent() ?: pattern,
    { "Unexpected pattern ${getText(pattern).quote()} for type ${ofType.toString().quote()}" }
)

class DiagDuplicatingRecordField(
    record: stellaParser.RecordContext,
    field: String,
    expectedTy: Ty
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_DUPLICATE_RECORD_FIELDS,
    record.getParent() ?: record,
    { "Duplicated field ${field.quote()} in record ${getText(record).quote()} of expected type ${expectedTy.toString().quote()}" }
)

class DiagDuplicatingRecordTypeField(
    record: stellaParser.TypeRecordContext,
    field: String,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_DUPLICATE_RECORD_TYPE_FIELDS,
    record.getParent() ?: record,
    { "Duplicated field ${field.quote()} in record type ${getText(record).quote()}" }
)

class DiagDuplicatingVariantTypeField(
    variant: stellaParser.TypeVariantContext,
    field: String,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_DUPLICATE_VARIANT_TYPE_FIELDS,
    variant.getParent() ?: variant,
    { "Duplicated field ${field.quote()} in variant type ${getText(variant).quote()}" }
)

class DiagExceptionTypeNotDeclared(
    expr: stellaParser.ExprContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_EXCEPTION_TYPE_NOT_DECLARED,
    expr.getParent() ?: expr,
    { "Exception in ${getText(expr).quote()} without its type declaration" }
)

class DiagAmbiguousThrowType(
    throwExpr: stellaParser.ThrowContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_AMBIGUOUS_THROW_TYPE,
    throwExpr.getParent() ?: throwExpr,
    { "Ambiguous throw type in expression ${getText(throwExpr).quote()}. Please, specify expected type explicitly" }
)

class DiagAmbiguousPanicType(
    panic: stellaParser.PanicContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_AMBIGUOUS_PANIC_TYPE,
    panic.getParent() ?: panic,
    { "Ambiguous panic type in expression ${getText(panic).quote()}. Please, specify expected type explicitly" }
)

class DiagAmbiguousReferenceType(
    ref: stellaParser.ExprContext,
) : TypeCheckError(
    TypeCheckErrorKind.ERROR_AMBIGUOUS_REFERENCE_TYPE,
    ref.getParent() ?: ref,
    { "Ambiguous reference type in expression ${getText(ref).quote()}. Please, specify expected type explicitly" }
)

class DiagUnexpectedMemoryAddress(expr: stellaParser.ConstMemoryContext, expectedTy: Ty?) : TypeCheckError(
    TypeCheckErrorKind.ERROR_UNEXPECTED_MEMORY_ADDRESS ,
    expr.getParent() ?: expr,
    { "Expected an expression of type ${expectedTy?.toString()?.quote()}, but got memory address ${getText(expr).quote()}" }
)

class DiagError(
    node: RuleContext,
    message: TokenStream.() -> String
) : TypeCheckError(
    TypeCheckErrorKind.ERROR,
    node,
    { message() }
) {
    companion object {
        fun extensionMustBeEnabled(node: RuleContext, ext: ExtensionChecker.Extensions): DiagError {
            return DiagError(node) { "Extension ${ext.ext.quote()} must be enabled" }
        }
    }
}