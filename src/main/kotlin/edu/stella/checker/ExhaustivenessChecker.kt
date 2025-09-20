package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.StellaCompileException
import edu.stella.core.TypeManager
import edu.stella.core.quote
import edu.stella.type.BoolTy
import edu.stella.type.ListTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.SumTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy
import edu.stella.type.VariantTy

private val Ty.finite: Boolean get() = when (this) {
    is UnitTy -> true
    is BoolTy -> true
    is NatTy -> true
    is TupleTy -> components.all(Ty::finite)
    is RecordTy -> components.unzip().second.all(Ty::finite)

    else -> TODO("am I over a finite domain?")
}

private sealed class Model<T : Ty>() : Iterable<Model<T>>, Iterator<Model<T>> {
    open val next: Model<T>? = null

    override fun hasNext(): Boolean = next != null
    override fun next(): Model<T> = next!!
    override fun iterator(): Iterator<Model<T>> = this

    companion object {
        fun<T : Ty> initial(ty: T): Model<T> = when(ty) {
            is BoolTy -> BoolModel.False
            is NatTy -> NatModel::Zero
            is TupleTy -> TupleModel(ty.components.map(::initial))
            is UnitTy -> UnitModel.Unit
            else -> throw Exception("How to process other types?")
        } as Model<T>
    }
}

private class WildcardModel<T : Ty> : Model<T>() {
//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = true
}

private class UnitModel : Model<UnitTy>() {
    companion object {
        val Unit = UnitModel()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return other is UnitModel
    }

//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when (pattern) {
//        is stellaParser.PatternUnitContext -> true
//        is stellaParser.PatternVarContext -> true
//        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
//        else -> false
//    }

    override fun hashCode(): Int = this::class.simpleName.hashCode()
}

private data class BoolModel(val value: Boolean) : Model<BoolTy>() {
    override val next: Model<BoolTy>?
        get() = if (!value) True else null

    companion object {
        val True = BoolModel(true)
        val False = BoolModel(false)
    }
}

private data class SumModel(val value: SumSide) : Model<SumTy>() {
    enum class SumSide {
        LEFT, RIGHT
    }

    override val next: Model<SumTy>?
        get() = if (value == SumSide.LEFT) SumModel(SumSide.RIGHT) else null
}

private data class VariantModel(val label: String, /*val value: Model<*>,*/ val ty: VariantTy) : Model<VariantTy>() {
    // assume that variant pattern contains only "variable" as a tag value (as declared in description in task doc)
    override val next: Model<VariantTy>?
        get() {
            val tags = ty.tags
            val nextIdx = ty.tags.indexOf(label) + 1
            if (nextIdx > tags.lastIndex) return null
            return VariantModel(ty.tags[nextIdx], /*value,*/ ty)
        }
}

private data class NatModel(val value: UInt) : Model<NatTy>() {
    override val next: Model<NatTy>?
        get() = NatModel(value + 1u)

//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
//        is stellaParser.PatternIntContext -> true
//        is stellaParser.PatternSuccContext -> true
//        is stellaParser.PatternVarContext -> true
//        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
//        else -> false
//    }

    companion object {
        val Zero = NatModel(0u)
        val One = NatModel(1u)
    }
}

private data class TupleModel(val values: List<Model<*>>) : Model<TupleTy>() {
    override val next: Model<TupleTy>?
        get() {
            val nextValues = mutableListOf<Model<*>>()
            for (v in values) {
                nextValues.add(v)
            }

            for (i in nextValues.indices) {
                nextValues[i].next?.let {
                    nextValues[i] = it
                    break
                }
                if (i == nextValues.indices.last) return null
            }
            return TupleModel(nextValues)
        }

//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
//        is stellaParser.PatternTupleContext -> true
//        is stellaParser.PatternVarContext -> true
//        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
//        else -> false
//    }
}

private abstract class ExhaustivenessSolver<T : Ty>(val of: T) {
    protected val models = mutableListOf<Model<T>>()

    operator fun plusAssign(m: Model<T>) {
        models.add(m)
    }

    abstract operator fun plusAssign(pattern: stellaParser.PatternContext)

    abstract val isExhaustive: Boolean

    abstract fun isValidPattern(pattern: stellaParser.PatternContext): Boolean
}

private class BoolExhaustivenessSolver() : ExhaustivenessSolver<BoolTy>(BoolTy()) {
    override val isExhaustive: Boolean
        get() {
            val values = mutableListOf<Model<BoolTy>>(BoolModel.False, BoolModel.True)
            for (m in models) {
                if (m is WildcardModel<BoolTy>) {
                    return true
                }
                values.remove(m)
                if (values.isEmpty()) return true
            }
            return false
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternTrueContext -> true
        is stellaParser.PatternFalseContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = when(pattern) {
        is stellaParser.PatternTrueContext -> this += BoolModel.True
        is stellaParser.PatternFalseContext -> this += BoolModel.False
        is stellaParser.PatternVarContext -> this += WildcardModel<BoolTy>()
        is stellaParser.ParenthesisedPatternContext -> this += pattern.pattern()
        else -> throw StellaCompileException("Unexpected pattern $pattern for Bool matcher")
    }
}

private class SumExhaustivenessSolver(ty: SumTy): ExhaustivenessSolver<SumTy>(ty) {
    override val isExhaustive: Boolean
        get() {
            val values = mutableListOf<Model<SumTy>>(
                SumModel(SumModel.SumSide.LEFT),
                SumModel(SumModel.SumSide.RIGHT)
            )
            for (m in models) {
                if (m is WildcardModel<SumTy>) {
                    return true
                }
                values.remove(m)
                if (values.isEmpty()) return true
            }
            return false
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternInlContext -> true
        is stellaParser.PatternInrContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = when(pattern) {
        is stellaParser.PatternInlContext -> this += SumModel(SumModel.SumSide.LEFT)
        is stellaParser.PatternInrContext -> this += SumModel(SumModel.SumSide.RIGHT)
        is stellaParser.PatternVarContext -> this += WildcardModel<SumTy>()
        is stellaParser.ParenthesisedPatternContext -> this += pattern.pattern()
        else -> throw StellaCompileException("Unexpected pattern $pattern for sum matcher")
    }
}

private class VariantExhaustivenessSolver(ty: VariantTy) : ExhaustivenessSolver<VariantTy>(ty) {
    override val isExhaustive: Boolean
        get() {
            val values = of.tags.mapTo(mutableListOf()) { VariantModel(it, of) }
            for (m in models) {
                if (m is WildcardModel<VariantTy>) return true
                values.remove(m)
                if (values.isEmpty()) return true
            }
            return false
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternVariantContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = when(pattern) {
        is stellaParser.PatternVariantContext -> this += VariantModel(pattern.label!!.text!!, of)
        is stellaParser.PatternVarContext -> this += WildcardModel<VariantTy>()
        is stellaParser.ParenthesisedPatternContext -> this += pattern.pattern()
        else -> throw StellaCompileException("Unexpected pattern $pattern for sum matcher")
    }
}

private class NatExhaustivenessSolver() : ExhaustivenessSolver<NatTy>(NatTy()) {

    override fun plusAssign(pattern: stellaParser.PatternContext) {
        // TODO: implement
    }

    override val isExhaustive: Boolean
        get() = true // TODO: implement

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternIntContext -> true
        is stellaParser.PatternSuccContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }
}

private class ListExhaustivenessSolver(ty: ListTy) : ExhaustivenessSolver<ListTy>(ty) {

    override fun plusAssign(pattern: stellaParser.PatternContext) {
        // TODO: implement
    }

    override val isExhaustive: Boolean
        get() = true // TODO: implement

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternListContext -> true
        is stellaParser.PatternConsContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }
}

class ExhaustivenessChecker(
    private val matching: stellaParser.MatchContext,
    private val types: TypeManager,
    private val diag: DiagnosticsEngine
) {

    fun check(): Boolean {
        val matchingTy = types[matching.expr()]
        val solver = when (val ty = matchingTy) {
            is BoolTy -> BoolExhaustivenessSolver()
            is NatTy -> NatExhaustivenessSolver()
            is SumTy -> SumExhaustivenessSolver(ty)
            is VariantTy -> VariantExhaustivenessSolver(ty)
            is ListTy -> ListExhaustivenessSolver(ty)
            else -> throw StellaCompileException("Unexpected matching over type ${ty.toString().quote()}")
        }

        matching.cases.forEach { cs ->
            val pattern = cs.pattern()
            if (!solver.isValidPattern(pattern)) {
                diag.diag(DiagUnexpectedPatternForType(pattern, matchingTy))
            } else {
                solver += pattern
            }
        }

        val exhaustive = solver.isExhaustive

        if (!exhaustive) {
//            diag.diag(DiagNonExhaustiveMatching(matching, required.map { it.toString() }, BoolTy()))
            diag.diag(DiagNonExhaustiveMatching(matching, BoolTy()))
        }

        return exhaustive
    }
}

