package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.TypeManager
import edu.stella.type.BadTy
import edu.stella.type.BoolTy
import edu.stella.type.FunTy
import edu.stella.type.ListTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.SumTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.VariantTy
import edu.stella.type.asTy

class StellaTypeChecker(
    private val program: stellaParser.ProgramContext,
    override val symbols: SymbolTable,
    override val types: TypeManager,
    override val diag: DiagnosticsEngine
) : SemaASTVisitor<Unit>() {

    fun check() {
        StellaTypeSynthesizer(this).synthesize(program)
        program.accept(this)
        types.checkRemaining()
    }

    override fun defaultResult() = Unit

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        ctx.accept(MissingMainChecker(diag))
        ctx.accept(UndefinedVariableChecker(this))
        ctx.accept(NotFunctionApplicationChecker(this))
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
        }
        super.visitIf(ctx)
    }

    override fun visitMatch(ctx: stellaParser.MatchContext) {
        types.getExpectation(ctx)?.let { retTy ->
            ctx.cases.forEach { cs ->
                types.expect(cs.expr(), retTy)
            }
        }

        if (ctx.cases.isEmpty()) {
            diag.diag(DiagEmptyMatching(ctx))
        }

        ExhaustivenessChecker(ctx, types, diag).check()

        super.visitMatch(ctx)
    }

    override fun visitAbstraction(ctx: stellaParser.AbstractionContext) {
        types.check(ctx, deep = false) {
            DiagUnexpectedLambda(ctx, types.getExpectation(ctx))
        }

        (types.getExpectation(ctx) as? FunTy)?.let { expected ->
            expected.params.zip(ctx.paramDecls).forEach { (expTy, param) ->
                types.expect(param, expTy)
                types.check(param) { DiagUnexpectedParameterType(param, expTy) }
            }

            types.expect(ctx.returnExpr!!, expected.ret)
        }

        super.visitAbstraction(ctx)
    }

    override fun visitNatRec(ctx: stellaParser.NatRecContext) {
        types.expect(ctx.n!!, NatTy())
        types.check(ctx.n!!)
        val element = types[ctx.initial!!] ?: BadTy()
        types.expect(ctx.step!!, FunTy(FunTy(element, listOf(element)), listOf(NatTy())))

        super.visitNatRec(ctx)
    }

    override fun visitApplication(ctx: stellaParser.ApplicationContext) {
        val paramTys = mutableListOf<Ty>()
        (types[ctx.func!!] as? FunTy)?.let { funTy ->
            ctx.args.zip(funTy.params).forEach { (arg, ty) ->
                types.expect(arg, ty)
                paramTys += ty
            }
        }

        (types.getExpectation(ctx) ?: types[ctx]) ?.let { retTy ->
            types.expect(ctx.func!!, FunTy(retTy, paramTys))
        }

        super.visitApplication(ctx)
    }

    override fun visitTuple(ctx: stellaParser.TupleContext) {
        types.check(ctx, deep = false) {
            DiagUnexpectedTuple(ctx, types.getExpectation(ctx) ?: BadTy())
        }

        (types.getExpectation(ctx) as? TupleTy)?.let { tupleTy ->
            if (ctx.exprs.size != tupleTy.components.size) {
                diag.diag(DiagUnexpectedTupleLength(ctx, tupleTy))
            }
        }

        super.visitTuple(ctx)
    }

    override fun visitDotTuple(ctx: stellaParser.DotTupleContext) {
        val idx = ctx.index!!.text!!.toInt() - 1

        (types[ctx.expr()] as? TupleTy)?.let { tupleTy ->
            if (idx !in tupleTy.components.indices) {
                diag.diag(DiagTupleIndexOutOfBounds(ctx, idx + 1, tupleTy))
            }
        }

        super.visitDotTuple(ctx)
    }

    override fun visitRecord(ctx: stellaParser.RecordContext) {
        types.check(ctx, deep = false) {
            DiagUnexpectedRecord(ctx, types.getExpectation(ctx) ?: BadTy())
        }

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
        (types[ctx.expr()] as? RecordTy)?.let { recTy ->
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
        types.check(ctx, deep = false) {
            DiagUnexpectedVariant(ctx, types.getExpectation(ctx) ?: BadTy())
        }

        (types.getExpectation(ctx) as? VariantTy)?.let { ty ->
            val expectedTags = ty.tags
            val actualTag = ctx.label!!.text!!
            if (actualTag !in expectedTags) {
                diag.diag(DiagUnexpectedVariantLabel(ctx, actualTag, ty))
            }
        }

        if (types.getExpectation(ctx) == null || types.getExpectation(ctx) !is VariantTy) {
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
        types.expect(ctx, BoolTy())
        super.visitIsZero(ctx)
    }

    override fun visitConsList(ctx: stellaParser.ConsListContext) {
        types.check(ctx, deep = false) {
            DiagUnexpectedList(ctx, types.getExpectation(ctx) ?: BadTy())
        }

        val listTy = (types.getExpectation(ctx) as? ListTy) ?: ListTy(BadTy())
        types.expect(ctx.tail!!, listTy)

        super.visitConsList(ctx)
    }

    override fun visitList(ctx: stellaParser.ListContext) {
        types.check(ctx, deep = false) {
            DiagUnexpectedList(ctx, types.getExpectation(ctx) ?: BadTy())
        }

        (types.getExpectation(ctx) as? ListTy)?.let { expectedList ->
            ctx.exprs.forEach { element ->
                types.expect(element, expectedList.of)
            }
        }

        types.getExpectation(ctx)?.let { ty ->
            if (ty !is ListTy && types[ctx] == null) {
                diag.diag(DiagUnexpectedList(ctx, ty))
            }
        }

        if (types[ctx] == null) {
            diag.diag(DiagAmbiguousListType(ctx))
        }

        super.visitList(ctx)
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

        if (types[ctx] == null) {
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

        if (types[ctx] == null) {
            diag.diag(DiagAmbiguousSumType(ctx))
        }

        super.visitInr(ctx)
    }

}