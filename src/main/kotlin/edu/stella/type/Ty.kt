package edu.stella.type

import com.strumenta.antlrkotlin.parsers.generated.stellaParser

sealed interface Ty {
    val astType: stellaParser.StellatypeContext?
        get() = null

    val isFunction: Boolean get() = this is FunTy
    val isTuple: Boolean get() = this is TupleTy
    val isRecord: Boolean get() = this is RecordTy
    val isList: Boolean get() = this is ListTy
    val isBool: Boolean get() = this is BoolTy

    abstract override fun toString(): String
    infix fun same(other: Ty?) = same(other, deep = true)
    fun same(other: Ty?, deep: Boolean = true): Boolean
}

data class BadTy(val expr: stellaParser.ExprContext? = null) : Ty {
    override fun toString(): String = "BAD"

    override fun same(other: Ty?, deep: Boolean): Boolean = other is BadTy
}

data class BoolTy(override val astType: stellaParser.TypeBoolContext? = null) : Ty {
    override fun toString(): String = "Bool"

    override fun same(other: Ty?, deep: Boolean): Boolean = other is BoolTy
}

data class NatTy(override val astType: stellaParser.TypeNatContext? = null) : Ty {
    override fun toString(): String = "Nat"

    override fun same(other: Ty?, deep: Boolean): Boolean = other is NatTy
}

data class FunTy(
    val ret: Ty,
    val params: List<Ty>,
    override val astType: stellaParser.TypeFunContext? = null
) : Ty {
    override fun toString(): String = params.joinToString(", ", prefix = "(", postfix = ")") + " -> " + ret

    override fun same(other: Ty?, deep: Boolean): Boolean {
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
        if (other !is TupleTy) return false
        if (!deep) return true
        if (components.size != other.components.size) return false
        components.zip(other.components).forEach { (a, b) ->
            if (!a.same(b, deep)) return false
        }
        return true
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
    val components: List<Pair<String, Ty>>,
    override val astType: stellaParser.TypeVariantContext? = null
) : Ty {
    override fun toString(): String = components.joinToString(", ", prefix = "<", postfix = ">") { component ->
        "${component.first}: ${component.second}"
    }

    override fun same(other: Ty?, deep: Boolean): Boolean {
        if (other !is VariantTy) return false
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

    val tags: List<String>
        get() = components.map { it.first }
}

data class ListTy(
    val of: Ty,
    override val astType: stellaParser.TypeListContext? = null
) : Ty {
    override fun toString(): String = "[$of]"

    override fun same(other: Ty?, deep: Boolean): Boolean {
        if (other !is ListTy) return false
        if (!deep) return true
        return of.same(other.of, deep)
    }
}

data class UnitTy(
    override val astType: stellaParser.TypeUnitContext? = null
) : Ty {
    override fun toString(): String = "Unit"

    override fun same(other: Ty?, deep: Boolean): Boolean {
        return other is UnitTy
    }
}

data class SumTy(
    val left: Ty,
    val right: Ty,
    override val astType: stellaParser.TypeSumContext? = null
) : Ty {
    override fun toString(): String = "$left + $right"

    override fun same(other: Ty?, deep: Boolean): Boolean {
        if (other !is SumTy) return false
        if (!deep) return true
        if (!(left same other.left)) return false
        if (!(right same other.right)) return false
        return true
    }
}