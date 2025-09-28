package edu.stella.type

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.ExtensionChecker

sealed interface Ty {
    val astType: stellaParser.StellatypeContext?
        get() = null

    val isFunction: Boolean get() = this is FunTy
    val isTuple: Boolean get() = this is TupleTy
    val isRecord: Boolean get() = this is RecordTy
    val isVariant: Boolean get() = this is VariantTy
    val isList: Boolean get() = this is ListTy
    val isBool: Boolean get() = this is BoolTy
    val isRef: Boolean get() = this is RefTy

    abstract override fun toString(): String
    infix fun same(other: Ty?) = same(other, deep = true)
    fun same(other: Ty?, deep: Boolean = true): Boolean
    infix fun subtypeOf(superTy: Ty?): Boolean

    companion object {
        private lateinit var extensions: ExtensionChecker
        fun initExtensions(ext: ExtensionChecker) {
            extensions = ext
        }

        val withStructuralSubtyping: Boolean
            get() = extensions.isStructuralSubtypingEnabled
    }
}

data class TopTy(val expr: stellaParser.ExprContext? = null) : Ty {
    override fun same(other: Ty?, deep: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun subtypeOf(superTy: Ty?): Boolean = superTy is TopTy || superTy is AnyTy

    override fun toString(): String = "Top"
}

data class BotTy(val expr: stellaParser.ExprContext? = null) : Ty {

    override fun same(other: Ty?, deep: Boolean): Boolean {
        TODO()
    }

    override fun subtypeOf(superTy: Ty?): Boolean = when (superTy) {
        null -> false
        is BadTy -> false
        is BotTy -> true
        else if Ty.withStructuralSubtyping -> true
        else -> false
    }

    override fun toString(): String = "Bot"
}

data class BadTy(val expr: stellaParser.ExprContext? = null) : Ty {
    override fun toString(): String = "BAD"

    override fun same(other: Ty?, deep: Boolean): Boolean = other is BadTy

    override fun subtypeOf(superTy: Ty?) = false
}

data object AnyTy : Ty {
    override fun same(other: Ty?, deep: Boolean): Boolean = other !is BadTy
    override fun toString(): String = "*"

    override fun subtypeOf(superTy: Ty?): Boolean = superTy !is BadTy
}

data class BoolTy(override val astType: stellaParser.TypeBoolContext? = null) : Ty {
    override fun toString(): String = "Bool"

    override fun same(other: Ty?, deep: Boolean): Boolean = other is BoolTy

    override fun subtypeOf(superTy: Ty?): Boolean = when (superTy) {
        is BoolTy -> true
        is TopTy if Ty.withStructuralSubtyping -> true
        else -> false
    }
}

data class NatTy(override val astType: stellaParser.TypeNatContext? = null) : Ty {
    override fun toString(): String = "Nat"

    override fun same(other: Ty?, deep: Boolean): Boolean = other is NatTy

    override fun subtypeOf(superTy: Ty?): Boolean = when (superTy) {
        is NatTy -> true
        is TopTy if Ty.withStructuralSubtyping -> true
        else -> false
    }
}

data class FunTy(
    val ret: Ty,
    val params: List<Ty>,
    override val astType: stellaParser.TypeFunContext? = null
) : Ty {
    override fun toString(): String = params.joinToString(", ", prefix = "(", postfix = ")") + " -> " + ret

    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is ErrorTy) return true
        if (other !is FunTy) return false
        if (!deep) return true

        val thisNorm = normalized
        val otherNorm = other.normalized

        if (!thisNorm.ret.same(otherNorm.ret, deep)) return false
        if (thisNorm.params.size != otherNorm.params.size) return false
        thisNorm.params.zip(otherNorm.params).forEach { (a, b) ->
            if (!a.same(b, deep)) return false
        }
        return true
    }

    override fun subtypeOf(superTy: Ty?): Boolean {
        if (superTy == null) return false
        if (superTy is TopTy && Ty.withStructuralSubtyping) return true
        if (superTy !is FunTy) return false

        val thisNorm = normalized
        val otherNorm = normalized

        if (thisNorm.ret subtypeOf otherNorm.ret) return false // covariance
        if (thisNorm.params.size != otherNorm.params.size) return false
        thisNorm.params.zip(otherNorm.params).forEach { (thisP, otherP) ->
            if (!(otherP subtypeOf thisP)) return false // contravariance
        }
        return true
    }

    private val normalized: FunTy // a, b, c -> d  === a, b -> (c -> d) === a -> (b -> (c -> d))
        get() = when {
            params.size > 1 -> FunTy(FunTy(ret, listOf(params.first())).normalized, params.subList(0, params.size - 1), astType).normalized
            else -> this
        }
//        get() = when (ret) {
//            is FunTy -> FunTy(ret.ret, params + ret.params, astType)
//            else -> this
//        }
}

data class TupleTy(
    val components: List<Ty>,
    override val astType: stellaParser.TypeTupleContext? = null
) : Ty {
    override fun toString(): String = components.joinToString(", ", prefix = "(", postfix = ")")

    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is ErrorTy) return true
        if (other !is TupleTy) return false
        if (!deep) return true
        if (components.size != other.components.size) return false
        components.zip(other.components).forEach { (a, b) ->
            if (!a.same(b, deep)) return false
        }
        return true
    }

    override fun subtypeOf(superTy: Ty?): Boolean {
        if (superTy == null) return false
        if (superTy is TopTy && Ty.withStructuralSubtyping) return true
        if (superTy !is TupleTy) return false

        if (components.size != superTy.components.size) return false

        components.zip(superTy.components).forEach { (thisC, superC) ->
            if (!(thisC subtypeOf superC)) return false
        }
        return true
    }

    fun getComponentTyOrNull(i: Int): Ty? = when {
        i < 0 -> null
        i > components.lastIndex -> null
        else -> components[i]
    }
}

data class RecordTy(
    val components: List<Pair<String, Ty>>,
    override val astType: stellaParser.TypeRecordContext? = null
) : Ty {
    override fun toString(): String = components.joinToString(", ", prefix = "{", postfix = "}") { component ->
        "${component.first}: ${component.second}"
    }

    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is ErrorTy) return true
        if (other !is RecordTy) return false
        if (!deep) return true
        if (components.size != other.components.size) return false
        components.zip(other.components).forEach { (a, b) ->
            val (an, at) = a
            val (bn, bt) = b
            if (an != bn) return false
            if (!at.same(bt, deep)) return false
        }
        return true
    }

    override fun subtypeOf(superTy: Ty?): Boolean {
        if (superTy == null) return false
        if (superTy is TopTy && Ty.withStructuralSubtyping) return true
        if (superTy !is RecordTy) return false

        if (components.size != superTy.components.size && !Ty.withStructuralSubtyping) return false
        if (components.size < superTy.components.size && Ty.withStructuralSubtyping) return false

        if (Ty.withStructuralSubtyping) {
            superTy.components.forEach { (n, t) ->
                if (!(t subtypeOf getComponentTyOrNull(n))) return false
            }
        } else {
            components.zip(superTy.components).forEach { (thisC, superC) ->
                val (thisN, thisT) = thisC
                val (superN, superT) = superC
                if (thisN != superN) return false
                if (!(thisT subtypeOf superT)) return false
            }
        }
        return true
    }

    val labels: Set<String>
        get() = components.map { (label, _) -> label }.toSet()

    fun getComponentTyOrNull(label: String): Ty? {
        for ((recLabel, ty) in components) {
            if (label == recLabel) return ty
        }
        return null
    }
}

data class VariantTy(
    val components: List<Pair<String, Ty?>>,
    override val astType: stellaParser.TypeVariantContext? = null
) : Ty {
    override fun toString(): String = components.joinToString(", ", prefix = "<", postfix = ">") { component ->
        when (component.second) {
            null -> component.first
            else -> "${component.first}: ${component.second}"
        }
    }

    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is ErrorTy) return true
        if (other !is VariantTy) return false
        if (!deep) return true
        if (components.size != other.components.size) return false
        components.zip(other.components).forEach { (a, b) ->
            val (an, at) = a
            val (bn, bt) = b
            if (an != bn) return false
            if (at?.same(bt, deep) != true && (at != null || at != bt)) return false
        }
        return true
    }

    override fun subtypeOf(superTy: Ty?): Boolean {
        if (superTy == null) return false
        if (superTy is TopTy && Ty.withStructuralSubtyping) return true
        if (superTy !is VariantTy) return false

        if (components.size != superTy.components.size && !Ty.withStructuralSubtyping) return false
        if (components.size > superTy.components.size && Ty.withStructuralSubtyping) return false

        if (Ty.withStructuralSubtyping) {
            components.forEach { (n, t) ->
                if (n !in superTy.tags) return false
                if (of(n) == null && superTy.of(n) != null) return false
                if (of(n)?.subtypeOf(superTy.of(n)) == false) return false
            }
        } else {
            components.zip(superTy.components).forEach { (thisC, superC) ->
                val (thisN, thisT) = thisC
                val (superN, superT) = superC
                if (thisN != superN) return false
                if (thisT?.subtypeOf(superT) != true && (thisT != null || thisT != superT)) return false
            }
        }
        return true
    }

    val tags: List<String>
        get() = components.map { it.first }

    fun of(tag: String): Ty? = components.firstOrNull { it.first == tag }?.second

    fun isNullary(tag: String) = of(tag) == null
}

data class ListTy(
    val of: Ty,
    override val astType: stellaParser.TypeListContext? = null
) : Ty {
    override fun toString(): String = "[$of]"

    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is ErrorTy) return true
        if (other !is ListTy) return false
        if (!deep) return true
        return of.same(other.of, deep)
    }

    override fun subtypeOf(superTy: Ty?): Boolean {
        if (superTy == null) return false
        if (superTy is TopTy && Ty.withStructuralSubtyping) return true
        if (superTy !is ListTy) return false

        return of subtypeOf superTy.of
    }
}

data class UnitTy(
    override val astType: stellaParser.TypeUnitContext? = null
) : Ty {
    override fun toString(): String = "Unit"

    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is ErrorTy) return true
        return other is UnitTy
    }

    override fun subtypeOf(superTy: Ty?): Boolean = when (superTy) {
        null -> false
        is UnitTy -> true
        is TopTy if Ty.withStructuralSubtyping -> true
        else -> false
    }
}

data class SumTy(
    val left: Ty,
    val right: Ty,
    override val astType: stellaParser.TypeSumContext? = null
) : Ty {
    override fun toString(): String = "$left + $right"

    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is ErrorTy) return true
        if (other !is SumTy) return false
        if (!deep) return true
        if (!(left same other.left)) return false
        if (!(right same other.right)) return false
        return true
    }

    override fun subtypeOf(superTy: Ty?): Boolean {
        if (superTy == null) return false
        if (superTy is TopTy && Ty.withStructuralSubtyping) return true
        if (superTy !is SumTy) return false

        if (!(left subtypeOf superTy.left)) return false
        if (!(right subtypeOf superTy.right)) return false
        return true
    }
}

data class RefTy(val of: Ty) : Ty {
    override fun toString(): String = "&$of"

    override fun same(other: Ty?, deep: Boolean): Boolean {
        if (other !is RefTy) return false
        if (!deep) return true
        return of same other.of
    }

    override fun subtypeOf(superTy: Ty?): Boolean {
        if (superTy == null) return false
        if (superTy is TopTy && Ty.withStructuralSubtyping) return true
        if (superTy !is RefTy) return false

        return of subtypeOf superTy.of && superTy.of subtypeOf of
    }
}

//data class ErrorTy(override val astType: stellaParser.TypeBoolContext? = null) : Ty {
//    override fun toString(): String = "Error"
//
//    override fun same(other: Ty?, deep: Boolean): Boolean {
//        if (other is BadTy) return false
//        return true
//    }
//}