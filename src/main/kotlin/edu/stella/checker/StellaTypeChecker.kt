package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.exhaustiveness.ExhaustivenessChecker
import edu.stella.checker.exhaustiveness.patternChecker
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.TypeManager
import edu.stella.type.AnyTy
import edu.stella.type.BadTy
import edu.stella.type.BoolTy
import edu.stella.type.FunTy
import edu.stella.type.ListTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.RefTy
import edu.stella.type.SumTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy
import edu.stella.type.VariantTy
import edu.stella.type.asTy

class StellaTypeChecker(
    private val program: stellaParser.ProgramContext,
    override val symbols: SymbolTable,
    override val types: TypeManager,
    override val diag: DiagnosticsEngine
) : SemaASTVisitor<Unit>() {
    private val extensions = ExtensionChecker(program)
    private var exceptionType: Ty? = null

    fun check() {
        StellaTypeSynthesizer(this).synthesize(program)
        program.accept(this)
        types.checkRemaining()
    }

    override fun defaultResult() = Unit

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        ctx.accept(MissingMainChecker(diag))
        ctx.accept(UndefinedVariableChecker(this))
        ctx.accept(NotATupleChecker(this))
        ctx.accept(NotARecordChecker(this))
        ctx.accept(NotAListChecker(this))

        super.visitProgram(ctx)
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        types.expect(ctx.returnExpr!!, ctx.returnType!!.asTy)
        super.visitDeclFun(ctx)
    }

    override fun visitIf(ctx: stellaParser.IfContext) {
        types.expect(ctx.condition!!, BoolTy())
        types.getExpectation(ctx)?.let { retTy ->
            types.expect(ctx.thenExpr!!, retTy)
            types.expect(ctx.elseExpr!!, retTy)
        } ?: run {
            types[ctx.thenExpr!!]?.let { ty ->
                types.expect(ctx.elseExpr!!, ty)
            }
        }

        super.visitIf(ctx)
    }

    override fun visitParenthesisedExpr(ctx: stellaParser.ParenthesisedExprContext) {
        types.getExpectation(ctx)?.let { ty ->
            types.expect(ctx.expr(), ty)
        }
    }

    override fun visitMatch(ctx: stellaParser.MatchContext) {
        types[ctx]?.let { retTy ->
            ctx.cases.forEach { cs ->
                types.expect(cs.expr(), retTy)
            }
        }

        if (ctx.cases.isEmpty()) {
            diag.diag(DiagEmptyMatching(ctx))
        }

        super.visitMatch(ctx)

        ExhaustivenessChecker(ctx, types, diag).check()
    }

    override fun visitAbstraction(ctx: stellaParser.AbstractionContext) {
        (types.getExpectation(ctx) as? FunTy)?.let { expected ->
            expected.params.zip(ctx.paramDecls).forEach { (expTy, param) ->
                types.expect(param, expTy)
            }

            types.expect(ctx.returnExpr!!, expected.ret)
        } ?: types.getExpectation(ctx)?.let {
            diag.diag(DiagUnexpectedLambda(ctx, it))
        }

        super.visitAbstraction(ctx)
    }

    override fun visitFix(ctx: stellaParser.FixContext) {
        types.getSynthesized(ctx.expr())?.let { ty ->
            if (!ty.isFunction) diag.diag(DiagNotAFunction(ctx, ty))
        }

        types.getExpectation(ctx)?.let { ty ->
            types.expect(ctx.expr(), FunTy(ty, listOf(ty)))
        } ?: types.getSynthesized(ctx)?.let { ty ->
            types.expect(ctx.expr(), FunTy(ty, listOf(ty)))
        }
        super.visitFix(ctx)
    }

    override fun visitNatRec(ctx: stellaParser.NatRecContext) {
        types.expect(ctx.n!!, NatTy())
        types.check(ctx.n!!)
        val element = types.getSynthesized(ctx.initial!!) ?: BadTy()
        types.expect(ctx.step!!, FunTy(FunTy(element, listOf(element)), listOf(NatTy())))

        super.visitNatRec(ctx)
    }

    override fun visitApplication(ctx: stellaParser.ApplicationContext) {
        types.getSynthesized(ctx.func!!)?.let { ty ->
            if (!ty.isFunction) diag.diag(DiagNotAFunction(ctx, ty))
        }

        val paramTys = mutableListOf<Ty>()
        (types.getSynthesized(ctx.func!!) as? FunTy)?.let { funTy ->
            ctx.args.zip(funTy.params).forEach { (arg, ty) ->
                types.expect(arg, ty)
                paramTys += ty
            }
        }

        (types.getSynthesized(ctx.func!!) as? FunTy)?.let { funTy ->
            types.expect(ctx, funTy.ret)
        }

        super.visitApplication(ctx)
    }

    override fun visitTuple(ctx: stellaParser.TupleContext) {
        (types.getExpectation(ctx) as? TupleTy)?.let { tupleTy ->
            if (ctx.exprs.size != tupleTy.components.size) {
                diag.diag(DiagUnexpectedTupleLength(ctx, tupleTy))
            }

            tupleTy.components.forEachIndexed { i, ty ->
                types.expect(ctx.exprs[i], ty)
            }
        }

        super.visitTuple(ctx)
    }

    override fun visitDotTuple(ctx: stellaParser.DotTupleContext) {
        super.visitDotTuple(ctx)

        val idx = ctx.index!!.text!!.toInt() - 1

        (types.getSynthesized(ctx.expr()) as? TupleTy)?.let { tupleTy ->
            if (idx !in tupleTy.components.indices) {
                diag.diag(DiagTupleIndexOutOfBounds(ctx, idx + 1, tupleTy))
            }
            if (tupleTy.components.size > idx) {
                types.expect(ctx, tupleTy.components[idx])
            }
        }
    }

    override fun visitRecord(ctx: stellaParser.RecordContext) {
        (types.getExpectation(ctx) as? RecordTy)?.let { ty ->
            val expectedLabels = ty.labels
            val actualLabels = (ctx.bindings.mapTo(hashSetOf()) { it.name!!.text!! }) as Set<String>
            for (label in expectedLabels) {
                if (label !in actualLabels) {
                    diag.diag(DiagMissingRecordField(ctx, label, ty))
                }
            }

            for (actualLabel in actualLabels) {
                if (actualLabel !in expectedLabels) {
                    diag.diag(DiagUnexpectedRecordField(ctx, actualLabel, ty))
                }
            }

            for (binding in ctx.bindings) {
                val (label, expr) = binding.name!!.text!! to binding.expr()

                ty.getComponentTyOrNull(label)?.let { componentTy ->
                    types.expect(expr, componentTy)
                }
            }

            ctx.bindings.groupBy { it.name!!.text!! }.forEach { label, repeat ->
                if (repeat.size > 1) diag.diag(DiagDuplicatingRecordField(ctx, label, ty))
            }
        }

        super.visitRecord(ctx)
    }

    override fun visitDotRecord(ctx: stellaParser.DotRecordContext) {
        val label = ctx.label!!.text!!
        (types.getSynthesized(ctx.expr()) as? RecordTy)?.let { recTy ->
            if (label !in recTy.labels) {
                diag.diag(DiagUnexpectedRecordFieldAccess(ctx, label, recTy))
            }
        }

        super.visitDotRecord(ctx)
    }

    override fun visitTypeRecord(ctx: stellaParser.TypeRecordContext) {
        ctx.fieldTypes.groupBy { it.label!!.text!! }.forEach { label, repeat ->
            if (repeat.size > 1) diag.diag(DiagDuplicatingRecordTypeField(ctx, label))
        }
        super.visitTypeRecord(ctx)
    }

    override fun visitVariant(ctx: stellaParser.VariantContext) {
        (types.getExpectation(ctx) as? VariantTy)?.let { ty ->
            val expectedTags = ty.tags
            val actualTag = ctx.label!!.text!!
            if (actualTag !in expectedTags) {
                diag.diag(DiagUnexpectedVariantLabel(ctx, actualTag, ty))
            }

            if (ctx.expr() == null && !ty.isNullary(actualTag)) {
                diag.diag(DiagMissingVariantData(ctx, actualTag))
            } else if (ctx.expr() != null && ty.isNullary(actualTag)) {
                diag.diag(DiagUnexpectedValueOfNullaryVariant(ctx.expr()!!, actualTag, ty))
            }

            if (ctx.expr() != null) {
                ty.of(actualTag)?.let { tagTy ->
                    types.expect(ctx.expr()!!, tagTy)
                }
            }
        }

        if (types.getSynthesized(ctx) == null && types.getExpectation(ctx) !is VariantTy) {
            diag.diag(DiagAmbiguousVariantType(ctx))
        }

        super.visitVariant(ctx)
    }

    override fun visitTypeVariant(ctx: stellaParser.TypeVariantContext) {
        ctx.fieldTypes.groupBy { it.label!!.text!! }.forEach { label, repeat ->
            if (repeat.size > 1) diag.diag(DiagDuplicatingVariantTypeField(ctx, label))
        }
        super.visitTypeVariant(ctx)
    }

    override fun visitSucc(ctx: stellaParser.SuccContext) {
        types.expect(ctx.expr(), NatTy())
        super.visitSucc(ctx)
    }

    override fun visitPred(ctx: stellaParser.PredContext) {
        types.expect(ctx.expr(), NatTy())
        super.visitPred(ctx)
    }

    override fun visitIsZero(ctx: stellaParser.IsZeroContext) {
        types.expect(ctx.expr(), NatTy())
        super.visitIsZero(ctx)
    }

    override fun visitConsList(ctx: stellaParser.ConsListContext) {
        val listTy = (types.getExpectation(ctx) as? ListTy) ?: ListTy(types.getSynthesized(ctx.head!!) ?: BadTy())
        types.expect(ctx.head!!, listTy.of)
        types.expect(ctx.tail!!, listTy)

        super.visitConsList(ctx)
    }

    override fun visitTail(ctx: stellaParser.TailContext) {
        types.check(ctx, deep = false)

        val listTy = (types.getExpectation(ctx) as? ListTy) ?: ListTy(AnyTy)
        types.expect(ctx.expr(), listTy)

        super.visitTail(ctx)
    }

    override fun visitHead(ctx: stellaParser.HeadContext) {
        super.visitHead(ctx)

        types.getExpectation(ctx)?.let { eTy ->
            types.expect(ctx.expr(), ListTy(eTy))
        } ?: types.expect(ctx.expr(), ListTy(AnyTy))

    }

    override fun visitList(ctx: stellaParser.ListContext) {
        if (types.getSynthesized(ctx) == null && types.getExpectation(ctx) !is ListTy) {
            diag.diag(DiagAmbiguousListType(ctx))
        }

        (types.getExpectation(ctx) as? ListTy)?.let { expectedList ->
            ctx.exprs.forEach { element ->
                types.expect(element, expectedList.of)
            }
        } ?: ctx.exprs.firstOrNull()?.let { first ->
            val firstTy = types.getSynthesized(first) ?: return@let
            ctx.exprs.forEach { element ->
                types.expect(element, firstTy)
            }
        }

        super.visitList(ctx)
    }

    override fun visitTypeAsc(ctx: stellaParser.TypeAscContext) {
        types.expect(ctx.expr(), ctx.stellatype().asTy)
        types.check(ctx)

        types.getExpectation(ctx)?.let { ty ->
            types.expect(ctx.expr(), ty)
        }

        super.visitTypeAsc(ctx)
    }

    override fun visitInl(ctx: stellaParser.InlContext) {
        (types.getExpectation(ctx) as? SumTy)?.let { sumTy ->
            types.expect(ctx.expr(), sumTy.left)
        }

        types.getExpectation(ctx)?.let { otherTy ->
            if (otherTy !is SumTy) {
                diag.diag(DiagUnexpectedInjection(ctx, otherTy))
            }
        }

        if (types.getSynthesized(ctx) == null && types.getExpectation(ctx) !is SumTy) {
            diag.diag(DiagAmbiguousSumType(ctx))
        }

        super.visitInl(ctx)
    }

    override fun visitInr(ctx: stellaParser.InrContext) {
        (types.getExpectation(ctx) as? SumTy)?.let { sumTy ->
            types.expect(ctx.expr(), sumTy.right)
        }

        types.getExpectation(ctx)?.let { otherTy ->
            if (otherTy !is SumTy) {
                diag.diag(DiagUnexpectedInjection(ctx, otherTy))
            }
        }

        if (types.getSynthesized(ctx) == null && types.getExpectation(ctx) !is SumTy) {
            diag.diag(DiagAmbiguousSumType(ctx))
        }

        super.visitInr(ctx)
    }

    override fun visitLet(ctx: stellaParser.LetContext) {
        types.getExpectation(ctx)?.let { ty ->
            types.expect(ctx.expr(), ty)
        }

        super.visitLet(ctx)
    }

    override fun visitSequence(ctx: stellaParser.SequenceContext) {
        types.check(ctx)
        types.expect(ctx.expr1!!, UnitTy())
        types.getExpectation(ctx)?.let { ty ->
            types.expect(ctx.expr2!!, ty)
        }
        super.visitSequence(ctx)
    }

    override fun visitDeclExceptionType(ctx: stellaParser.DeclExceptionTypeContext) {
        super.visitDeclExceptionType(ctx)

        if (!extensions.isExceptionTypeDeclarationEnabled) {
            diag.diag(DiagError.extensionMustBeEnabled(ctx, ExtensionChecker.Extensions.EXCEPTION_TYPE_DECLARATION))
        }

        exceptionType = ctx.exceptionType!!.asTy
    }

    override fun visitDeclExceptionVariant(ctx: stellaParser.DeclExceptionVariantContext) {
        super.visitDeclExceptionVariant(ctx)

        if (!extensions.isExceptionTypeVariantEnabled) {
            diag.diag(DiagError.extensionMustBeEnabled(ctx, ExtensionChecker.Extensions.EXCEPTION_TYPE_VARIANT))
        }

        exceptionType = ctx.variantType!!.asTy
    }

    override fun visitPanic(ctx: stellaParser.PanicContext) {
        super.visitPanic(ctx)

        if (!extensions.isPanicEnabled) {
            diag.diag(DiagError.extensionMustBeEnabled(ctx, ExtensionChecker.Extensions.PANIC))
        }

        if (types.getSynthesized(ctx) == null && types.getExpectation(ctx) == null) {
            diag.diag(DiagAmbiguousPanicType((ctx)))
        }
    }

    override fun visitThrow(ctx: stellaParser.ThrowContext) {
        super.visitThrow(ctx)

        if (exceptionType == null) {
            diag.diag(DiagExceptionTypeNotDeclared(ctx))
            return
        }

        if (types.getSynthesized(ctx) == null && types.getExpectation(ctx) == null) {
            diag.diag(DiagAmbiguousThrowType((ctx)))
        }

        types.expect(ctx.expr(), exceptionType!!)
    }

    override fun visitTryCatch(ctx: stellaParser.TryCatchContext) {
        super.visitTryCatch(ctx)

        if (exceptionType == null) {
            diag.diag(DiagExceptionTypeNotDeclared(ctx))
            return
        }

        val ty = types.getExpectation(ctx) ?: types[ctx.tryExpr!!] ?: return

        types.expect(ctx.tryExpr!!, ty)
        types.expect(ctx.fallbackExpr!!, ty)

        if (!exceptionType!!.patternChecker().isValidPattern(ctx.pattern())) {
            diag.diag(DiagUnexpectedPatternForType(ctx.pattern(), ty))
        }
    }

    override fun visitTryWith(ctx: stellaParser.TryWithContext) {
        super.visitTryWith(ctx)

        if (exceptionType == null) {
            diag.diag(DiagExceptionTypeNotDeclared(ctx))
            return
        }

        val ty = types.getExpectation(ctx) ?: types[ctx.tryExpr!!] ?: return

        types.expect(ctx.tryExpr!!, ty)
        types.expect(ctx.fallbackExpr!!, ty)
    }

    override fun visitConstMemory(ctx: stellaParser.ConstMemoryContext) {
        super.visitConstMemory(ctx)

        if (types.getSynthesized(ctx) == null && types.getExpectation(ctx) == null) {
            diag.diag(DiagAmbiguousReferenceType((ctx)))
        }

        types.getExpectation(ctx)?. let { ty ->
            if (!ty.isRef) diag.diag(DiagUnexpectedMemoryAddress(ctx, ty))
        }
    }

    override fun visitRef(ctx: stellaParser.RefContext) {
        super.visitRef(ctx)

        types.getExpectation(ctx)?.let { ty ->
            when {
                ty is RefTy -> types.expect(ctx.expr(), ty.of)
                else -> types.expect(ctx, RefTy(AnyTy))
            }
        }
    }

    override fun visitDeref(ctx: stellaParser.DerefContext) {
        super.visitDeref(ctx)

        types.getExpectation(ctx)?.let { ty ->
            types.expect(ctx.expr(), RefTy(ty))
        } ?: types.expect(ctx.expr(), RefTy(AnyTy))
    }

    override fun visitAssign(ctx: stellaParser.AssignContext) {
        super.visitAssign(ctx)

        types[ctx.rhs!!]?.let { ty ->
            types.expect(ctx.lhs!!, RefTy(ty))
        }
    }
}