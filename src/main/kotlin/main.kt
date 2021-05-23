import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 3) {
        println("Please invoke with following arguments: languageName inputFile outputFile")
        exitProcess(0)
    }

    val languageName = args[0]
    val inputFile = args[1]
    val outputFile = args[2]

    val converted = convert(languageName, inputFile)
    File(outputFile).writeText(converted)
}

val optionals = mutableListOf<String>()

fun convert(languageName: String, inputFile: String): String {
    var content = File(inputFile).readText()
    val output = StringBuilder()

    val rulesSectionStart = content.indexOf("%%")
    val rulesSectionEnd = content.lastIndexOf("%%")
    content = content.substring(rulesSectionStart + 2, rulesSectionEnd)
    content = removeSemanticActions(content.trim())

    output.append(
        "module.exports = grammar({\n" +
                "    name: '$languageName',\n" +
                "\n" +
                "    rules: {"
    )

    val rules = content.split(';')
    for (rule in rules) {
        if (rule.trim().isEmpty())
            continue

        val splitRule = rule.trim().split(':')
        val ruleName = splitRule[0].trim()

        val ruleBranches = splitRule[1].split('|')

        if (ruleBranches.isEmpty())
            println("Rule $ruleName has no branches")
        val formedRule = if (ruleBranches.size == 1)
            formOneBranchRule(ruleName, ruleBranches)
        else
            formManyBranchRule(ruleName, ruleBranches)

        output.append(formedRule)
    }

    output.append("}\n});")

    return postProcess(output.toString())
}

fun formOneBranchRule(ruleName: String, ruleBranches: List<String>): String {
    val builder = StringBuilder()
    builder.append(makeHeader(ruleName))

    val branch = ruleBranches[0].trim().split(' ')
    if (branch.size == 1)
        builder.append("$." + branch[0] + ',')
    else {
        builder.append(processBranch(branch))
    }
    builder.append("\n")
    return builder.toString()
}

fun formManyBranchRule(ruleName: String, ruleBranches: List<String>): String {
    val builder = StringBuilder()
    builder.append(makeHeader(ruleName))

    val actuallyMoreThanOneBranch = ruleBranches.count { x -> x.trim().isNotEmpty() } > 1
    if (actuallyMoreThanOneBranch)
        builder.append("choice(\n")

    for (branch in ruleBranches) {
        if (branch.trim().isEmpty())
            optionals.add(ruleName)
        else
            builder.append(processBranch(branch.trim().split(' ')))
    }

    if (actuallyMoreThanOneBranch)
        builder.append("),\n\n")

    return builder.toString()
}

fun removeSemanticActions(rule: String): String {
    val semanticActionsRegex = Regex("\\{(.|\\n)+?}")
    val commentsRegex = Regex("(//.*?\\n|/\\*(.|\\n)*?\\*/)")

    var result = semanticActionsRegex.replace(rule, "")
    result = commentsRegex.replace(result, "")

    return result
}

fun makeHeader(name: String): String {
    return "$name: $ => "
}

fun processBranch(branch: List<String>): String {
    val builder = StringBuilder()
    if (branch.size > 1)
        builder.append("seq(\n")
    for (element in branch)
        builder.append("$.$element,\n")
    if (branch.size > 1)
        builder.append("),\n")
    return builder.toString()
}

fun postProcess(output: String): String {
    var newOutput = output
    for (optionalRule in optionals) {
        newOutput = newOutput.replace("$.$optionalRule,", "optional($.$optionalRule),")
    }

    return newOutput
}