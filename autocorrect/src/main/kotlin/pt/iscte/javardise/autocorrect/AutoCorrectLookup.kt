package pt.iscte.javardise.autocorrect

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import org.apache.commons.text.similarity.LevenshteinDistance
import pt.iscte.javardise.external.getOrNull
import kotlin.math.max

const val DEFAULT_THRESHOLD = .7

data class ScopeId(val id: String, val type: Type) {
    enum class Type {
        CLASS, METHOD, FIELD, VARIABLE
    }
}

class AutoCorrectLookup(val threshold: Double = DEFAULT_THRESHOLD) {

    // returns null if id is found in list
    fun match(id: String, list: Collection<String>): String? {
        if(list.isEmpty() || id in list)
            return null

        val rank = list.map { Pair(it, findSimilarity(id, it)) }.sortedBy { it.second }

        return if(rank.last().second >= threshold)
            rank.last().first
        else null
    }

    fun matchSub(id: String, list: Collection<String>): String? {
        if(list.isEmpty() || id in list)
            return null

        val rank = list
            .map {
                if(it.length < id.length)
                it
            else
                it.substring(id.indices)
            }
            .map { Pair(it, findSimilarity(id, it)) }
            .sortedBy { it.second }

        return if(rank.last().second >= threshold)
            rank.last().first
        else null
    }

    fun findOption(n: NameExpr): ScopeId? {
        val rank = n.findMethod()?.scope()
        ?.map { Pair(it, findSimilarity(it.id, n.nameAsString)) }
        ?.sortedBy {it.second }

        return if(rank.isNullOrEmpty())
            null
        else if(rank.last().first.id != n.nameAsString && rank.last().second >= threshold)
            rank.last().first
        else null
    }

    fun findSubOption(n: NameExpr, idToCompare: String): ScopeId? {
        val rank = n.findMethod()?.scope()
            ?.map { ScopeId(
                if(it.id.length < idToCompare.length)
                    it.id
                else
                    it.id.substring(idToCompare.indices), it.type) }
            ?.map { Pair(it, findSimilarity(it.id, idToCompare)) }
            ?.sortedBy {it.second }

        return if(rank.isNullOrEmpty())
            null
        else if(rank.last().first.id != idToCompare && rank.last().second >= threshold)
            rank.last().first
        else null
    }



    private fun findSimilarity(x: String, y: String): Double {
        val maxLength = max(x.length, y.length)
        return if (maxLength > 0) {
            1.0 - LevenshteinDistance().apply(x.lowercase(), y.lowercase()).toDouble() / maxLength
        } else 1.0
    }
}


fun Node.findClass(): ClassOrInterfaceDeclaration? =
    if (parentNode.getOrNull is ClassOrInterfaceDeclaration)
        parentNode.get() as ClassOrInterfaceDeclaration
    else if (parentNode.isPresent)
        parentNode.get().findClass()
    else
        null

fun Node.findMethod(): CallableDeclaration<*>? =
    if (parentNode.getOrNull is CallableDeclaration<*>)
        parentNode.get() as CallableDeclaration<*>
    else if (parentNode.isPresent)
        parentNode.get().findMethod()
    else
        null

fun CallableDeclaration<*>.scope(): List<ScopeId> {
    val list = mutableListOf<ScopeId>()
    (parentNode.getOrNull as? ClassOrInterfaceDeclaration)?.let { c ->
        list.addAll(c.methods.map { ScopeId(it.nameAsString, ScopeId.Type.METHOD) })
        list.addAll(c.fields.flatMap { it.variables }.map { ScopeId(it.nameAsString, ScopeId.Type.FIELD) })
    }

    parameters.mapTo(list) { ScopeId(it.nameAsString, ScopeId.Type.VARIABLE) }
    walk {
        if (it is VariableDeclarationExpr)
            list.addAll(it.variables.map { ScopeId(it.nameAsString, ScopeId.Type.VARIABLE) })
    }
    return list
}



