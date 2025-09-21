package edu.stella.checker.exhaustiveness

import edu.stella.type.BoolTy
import edu.stella.type.NatTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy

internal sealed class Model<T : Ty>(val of: T) : Iterable<Model<T>>, Iterator<Model<T>> {
    open val next: Model<T>? = null

    override fun hasNext(): Boolean = next != null
    override fun next(): Model<T> = next!!
    override fun iterator(): Iterator<Model<T>> = this

    open fun normalized(): Model<T> = this

    companion object {
        fun <T : Ty> initial(ty: T): Model<T> = when (ty) {
            is BoolTy -> BoolModel.False
            is NatTy -> NatModel.of(0u)
            is TupleTy -> TupleModel(ty.components.map(::initial), ty)
            is UnitTy -> UnitModel
            else -> throw Exception("How to process other types?")
        } as Model<T>
    }
}

internal sealed class FiniteModel<T : Ty>(of: T) : Model<T>(of)

internal class WildcardModel<T : Ty>(of: T) : Model<T>(of) {
//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = true
}

private object UnitModel : Model<UnitTy>(UnitTy()) {
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

private data class TupleModel(val values: List<Model<*>>, private val ty: TupleTy) : Model<TupleTy>(ty) {
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
            return TupleModel(nextValues, ty)
        }

//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
//        is stellaParser.PatternTupleContext -> true
//        is stellaParser.PatternVarContext -> true
//        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
//        else -> false
//    }
}


