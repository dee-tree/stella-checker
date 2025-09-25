package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.type.AnyTy
import edu.stella.type.BoolTy
import edu.stella.type.FunTy
import edu.stella.type.ListTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.SumTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy
import edu.stella.type.VariantTy
import edu.stella.type.asTy

class StellaTypeSynthesizer(
    private val delegate: SemaStage<Unit>
) :  SemaASTVisitor<Unit>(),  SemaStage<Unit> by delegate {
    override fun defaultResult() = Unit

    private val patternScope = mutableListOf<Ty?>()

    private fun withPatternScope(ty: Ty?, action: (ty: Ty?) -> Unit) {
        pushPatternScope(ty)
        action(ty)
        popPatternScope()
    }

    private fun pushPatternScope(ty: Ty?) {
        patternScope += ty
    }

    private fun popPatternScope() {
        patternScope.removeLast()
    }

    private val patternTyScope: Ty?
        get() = patternScope.lastOrNull()


    fun synthesize(program: stellaParser.ProgramContext) {
        program.accept(this)
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        types.learn(ctx, FunTy(ctx.returnType!!.asTy, ctx.paramDecls.map { param -> param.paramType!!.asTy }))
        super.visitDeclFun(ctx)
    }

    override fun visitParamDecl(ctx: stellaParser.ParamDeclContext) {
        types.learn(ctx, ctx.paramType!!.asTy)
        super.visitParamDecl(ctx)
    }

    override fun visitVar(ctx: stellaParser.VarContext) {
        super.visitVar(ctx)
        getTy(ctx.name!!.text!!, ctx)?.let { ty ->
            types.learn(ctx, ty)
        }
    }

    override fun visitConstFalse(ctx: stellaParser.ConstFalseContext) {
        types.learn(ctx, BoolTy())
        super.visitConstFalse(ctx)
    }

    override fun visitConstTrue(ctx: stellaParser.ConstTrueContext) {
        types.learn(ctx, BoolTy())
        super.visitConstTrue(ctx)
    }

    override fun visitConstInt(ctx: stellaParser.ConstIntContext) {
        types.learn(ctx, NatTy())
        super.visitConstInt(ctx)
    }

    override fun visitParenthesisedExpr(ctx: stellaParser.ParenthesisedExprContext) {
        super.visitParenthesisedExpr(ctx)
        types.getSynthesized(ctx.expr())?.let { ty ->
            types.learn(ctx, ty)
        }
    }

    override fun visitTerminatingSemicolon(ctx: stellaParser.TerminatingSemicolonContext) {
        super.visitTerminatingSemicolon(ctx)
        types.getSynthesized(ctx.expr())?.let { ty ->
            types.learn(ctx, ty)
        }
    }

    override fun visitIsZero(ctx: stellaParser.IsZeroContext) {
        super.visitIsZero(ctx)
        types.learn(ctx, BoolTy())
    }

    override fun visitSucc(ctx: stellaParser.SuccContext) {
        super.visitSucc(ctx)
        types.learn(ctx, NatTy())
    }

    override fun visitPred(ctx: stellaParser.PredContext) {
        super.visitPred(ctx)
        types.learn(ctx, NatTy())
    }

    override fun visitApplication(ctx: stellaParser.ApplicationContext) {
        super.visitApplication(ctx)

        val funcTy = types.getSynthesized(ctx.func!!) as? FunTy ?: return
        types.learn(ctx, funcTy.ret)
    }

    override fun visitAbstraction(ctx: stellaParser.AbstractionContext) {
        val paramTys = mutableListOf<Ty>()
        ctx.paramDecls.forEach { param ->
            val ty = param.paramType!!.asTy
            paramTys += ty
            types.learn(param, ty)
        }

        ctx.returnExpr!!.accept(this)

        types.getSynthesized(ctx.returnExpr!!)?.let { retTy ->
            types.learn(ctx, FunTy(retTy, paramTys))
        }
    }

    override fun visitMatch(ctx: stellaParser.MatchContext) {
        // no super call due to manual walk
        ctx.expr().accept(this)

        withPatternScope(types.getSynthesized(ctx.expr())) {
            ctx.cases.forEach { it.accept(this) }
            ctx.cases.firstOrNull()?.let { cs ->
                types.getSynthesized(cs.expr())?.let { ty ->
                    types.learn(ctx, ty)
                }
            }
        }
    }

    override fun visitPatternVar(ctx: stellaParser.PatternVarContext) {
        super.visitPatternVar(ctx)

        patternTyScope?.let { ty ->
            types.learn(ctx, ty)
        }
    }

    override fun visitPatternVariant(ctx: stellaParser.PatternVariantContext) {
        (patternTyScope as? VariantTy)?.let { variantTy ->
            types.learn(ctx, variantTy)

            if (ctx.pattern() == null) return

            withPatternScope(variantTy.of(ctx.label!!.text!!)) {
                super.visitPatternVariant(ctx)
            }
        }
    }

    override fun visitPatternInl(ctx: stellaParser.PatternInlContext) {
        (patternTyScope as? SumTy)?.let { sumTy ->
            types.learn(ctx, sumTy)

            withPatternScope(sumTy.left) {
                super.visitPatternInl(ctx)
            }
        }
    }

    override fun visitPatternInr(ctx: stellaParser.PatternInrContext) {
        (patternTyScope as? SumTy)?.let { sumTy ->
            types.learn(ctx, sumTy)

            withPatternScope(sumTy.right) {
                super.visitPatternInr(ctx)
            }
        }
    }

    override fun visitConstUnit(ctx: stellaParser.ConstUnitContext) {
        types.learn(ctx, UnitTy())
        super.visitConstUnit(ctx)
    }

    override fun visitTuple(ctx: stellaParser.TupleContext) {
        super.visitTuple(ctx)

        val tys = mutableListOf<Ty>()
        ctx.exprs.forEach { component ->
            val ty = types.getSynthesized(component) ?: return
            tys += ty
        }
        types.learn(ctx, TupleTy(tys))
    }

    override fun visitDotTuple(ctx: stellaParser.DotTupleContext) {
        super.visitDotTuple(ctx)

        (types.getSynthesized(ctx.expr()) as? TupleTy)?.let { tuTy ->
            tuTy.getComponentTyOrNull(ctx.index!!.text!!.toInt() - 1)?.let { componentTy ->
                types.learn(ctx, componentTy)
            }
        }
    }

    override fun visitDotRecord(ctx: stellaParser.DotRecordContext) {
        super.visitDotRecord(ctx)

        (types.getSynthesized(ctx.expr()) as? RecordTy)?.let { recTy ->
            recTy.getComponentTyOrNull(ctx.label!!.text!!)?.let { componentTy ->
                types.learn(ctx, componentTy)
            }
        }
    }

    override fun visitRecord(ctx: stellaParser.RecordContext) {
        super.visitRecord(ctx)

        val tys = mutableListOf<Pair<String, Ty>>()
        ctx.bindings.forEach { binding ->
            types.getSynthesized(binding.expr())?.let { ty ->
                tys += binding.name!!.text!! to ty
            } ?: return
        }

        types.learn(ctx, RecordTy(tys))
    }

    override fun visitTypeAsc(ctx: stellaParser.TypeAscContext) {
        super.visitTypeAsc(ctx)
        types.learn(ctx, ctx.stellatype().asTy)
    }

    override fun visitPatternBinding(ctx: stellaParser.PatternBindingContext) {
        ctx.expr().accept(this)
        ctx.pattern().accept(this)
        types.learn(ctx.pattern(), types.getSynthesized(ctx.expr()) ?: AnyTy)
    }

    override fun visitLet(ctx: stellaParser.LetContext) {
        ctx.patternBindings.forEach { binding ->
            binding.expr().accept(this)

            withPatternScope(types.getSynthesized(binding.expr())) {
                binding.pattern().accept(this)
            }
        }

        ctx.expr().accept(this)

        types.getSynthesized(ctx.expr())?.let { ty ->
            types.learn(ctx, ty)
        }
        // Do not call super.visitLet() since it is visited in manual order
    }

    override fun visitVariant(ctx: stellaParser.VariantContext) {
        super.visitVariant(ctx)
        return // no way to synthesize variant type
    }

    override fun visitIf(ctx: stellaParser.IfContext) {
        super.visitIf(ctx)

        val thenTy = types.getSynthesized(ctx.thenExpr!!) ?: return
        val elseTy = types.getSynthesized(ctx.elseExpr!!) ?: return

        if (!(thenTy same elseTy)) return
        types.learn(ctx, thenTy)
    }

    override fun visitConsList(ctx: stellaParser.ConsListContext) {
        super.visitConsList(ctx)

        val of = types.getSynthesized(ctx.head!!) ?: return //?: AnyTy
        types.learn(ctx, ListTy(of))
    }

    override fun visitTail(ctx: stellaParser.TailContext) {
        super.visitTail(ctx)

        val of = (types.getSynthesized(ctx.expr()) as? ListTy) ?: ListTy(AnyTy)
        types.learn(ctx, of)
    }

    override fun visitIsEmpty(ctx: stellaParser.IsEmptyContext) {
        super.visitIsEmpty(ctx)

        types.learn(ctx, BoolTy())
    }

    override fun visitList(ctx: stellaParser.ListContext) {
        super.visitList(ctx)
        val of = types.getSynthesized(ctx.exprs.firstOrNull() ?: return) ?: return
        types.learn(ctx, ListTy(of))
    }

    override fun visitPanic(ctx: stellaParser.PanicContext) {
        super.visitPanic(ctx)
    }

    override fun visitTryCatch(ctx: stellaParser.TryCatchContext) {
        super.visitTryCatch(ctx)

        val tryTy = types.getSynthesized(ctx.tryExpr!!) ?: return
        val fallbackTy = types.getSynthesized(ctx.fallbackExpr!!) ?: return

        if (!(tryTy same fallbackTy)) return
        types.learn(ctx, tryTy)
    }

    override fun visitFix(ctx: stellaParser.FixContext) {
        super.visitFix(ctx)

        (types.getSynthesized(ctx.expr()) as? FunTy)?.let { funTy ->
            types.learn(ctx, funTy.ret)
        }
    }
}