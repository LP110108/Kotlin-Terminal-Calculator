package calculator

import java.math.BigInteger
import kotlin.math.pow

enum class Error(val message: String) {
    UNKNOWN_COMMAND("Unknown command"),
    INVALID_EXPRESSION("Invalid expression"),
    INVALID_IDENTIFIER("Invalid identifier"),
    INVALID_ASSIGNMENT("Invalid assignment"),
    UNKNOWN_VARIABLE("Unknown variable")
}

class Calculator {
    companion object {
        val PRECEDENCES = mapOf(
            "(" to -2, ")" to -1, "+" to 0, "-" to 0, "*" to 1, "/" to 1, "^" to 2
        )
    }

    private val variables = mutableMapOf<String, BigInteger>()
    private var error: Error? = null
    private var loop = true

    fun run() {
        while (loop) {
            val input = readLine()!!
            if (input.startsWith("/")) processCommand(input)
            else processExpression(input)
            prStringError()
        }
    }

    private fun isNumeric(s: String): Boolean = Regex("-?\\d+").matches(s)

    private fun isAlpha(s: String): Boolean = Regex("-?[A-Za-z]+").matches(s)

    private fun sanitize(input: String) = Regex("\\(|\\)|\\^|/|\\*|=|[+-]+|[A-Za-z]+|\\d+").findAll(input)
        .map { " ${it.groupValues[0]} " }.joinToString(" ")
        .trim().replace(Regex(" +"), " ")
        .replace(Regex("^-"), "0 -").replace(Regex("^\\+"), "0 +")
        .replace("( -", "( 0 -").replace("( +", "( 0 +")
        .replace("= -", "= 0 -").replace("= +", "= 0 +")
        .split(" ").map { el ->
            if (!Regex("[+-]+").matches(el)) el
            else if (el.count { it == '-' } % 2 == 0) "+" else "-"
        }

    private fun assign(expression: List<String>) {
        if (!Regex("[A-Za-z]+").matches(expression[0])) {
            error = Error.INVALID_IDENTIFIER
            throw Exception()
        }
        try {
            variables[expression[0]] = eval(expression.drop(2))
        } catch (e: Exception) {
            error = Error.INVALID_ASSIGNMENT
        }
    }

    private fun convert(expression: String): BigInteger {
        if (Regex("[A-Za-z]+").matches(expression))
            if (variables[expression] != null) return variables[expression]!!
            else {
                error = Error.UNKNOWN_VARIABLE
                throw Exception()
            }
        return expression.toBigInteger()
    }

    private fun infixToPostfix(input: List<String>): List<String> {
        val stack = mutableListOf<String>()
        val result = mutableListOf<String>()
        input.forEach {
            if (isNumeric(it) || isAlpha(it)) result.add(it)
            else if (stack.isEmpty() || stack.last() == "(" || it == "(") stack.add(it)
            else if (PRECEDENCES[stack.last()]!! < PRECEDENCES[it]!!) stack.add(it)
            else if (PRECEDENCES[stack.last()]!! >= PRECEDENCES[it]!!) {
                while (stack.isNotEmpty() && PRECEDENCES[stack.last()]!! >= PRECEDENCES[it]!!)
                    result.add(stack.removeLast())
                if (it == ")") stack.removeLast()
                else stack.add(it)
            }
        }
        while (stack.isNotEmpty()) result.add(stack.removeLast())
        return result
    }

    private fun eval(expression: List<String>): BigInteger {
        val result = mutableListOf<BigInteger>()
        infixToPostfix(expression).forEach {
            if (isNumeric(it) || isAlpha(it)) {
                result.add(convert(it))
                return@forEach
            }
            val (b, a) = Pair(result.removeLast(), result.removeLast())
            when (it) {
                "+" -> result.add(a + b)
                "-" -> result.add(a - b)
                "*" -> result.add(a * b)
                "/" -> result.add(a / b)
                "^" -> result.add((a.toDouble().pow(b.toInt()).toInt()).toBigInteger())
            }
        }
        return result.last()
    }

    private fun processExpression(input: String) {
        if (input.isEmpty()) return
        try {
            if (input.contains("=")) assign(sanitize(input))
            else println(eval(sanitize(input)))
        } catch (e: Exception) {
            error = error ?: Error.INVALID_EXPRESSION
        }
    }

    private fun processCommand(input: String) {
        when (input) {
            "/exit" -> exit()
            "/help" -> help()
            else -> error = Error.UNKNOWN_COMMAND
        }
    }

    private fun prStringError() {
        if (error != null) println(error!!.message)
        error = null
    }

    private fun help() {
        println("The program calculates the sum of numbers")
    }

    private fun exit() {
        println("Bye!")
        loop = false
    }
}

fun main() {
    val calculator = Calculator()
    calculator.run()
}