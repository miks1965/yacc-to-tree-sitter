import java.io.File

fun main(args: Array<String>) {
//    File("/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/output.txt").writeText("")
//    File("/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/optionals.txt").writeText("")

//    convert(
//        "/home/lereena/github/tree-sitter-pascalabcnet/full1904.txt",
//        "/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/output.txt"
//    )

    File("/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/withSemanticPrints.txt").writeText("")
    addSemanticPrintlns(
        "/home/lereena/github/tree-sitter-pascalabcnet/full1904.txt",
        "/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/withSemanticPrints.txt"
    )
}

fun addSemanticPrintlns(inputFile: String, outputFile: String) {
    var content = File(inputFile).readText()
    var output = StringBuilder()

    val rulesSectionStart = content.indexOf("%%")
    val rulesSectionEnd = content.lastIndexOf("%%")
    content = content.substring(rulesSectionStart + 2, rulesSectionEnd)

    val rules = content.split(';')
    for (rule in rules) {
        if (rule.trim().isEmpty())
            continue
        val splitRule = removeSemanticActions(rule.trim()).split(':')
        val ruleName = splitRule[0].trim()

        val ruleBranches = splitRule[1].split('|')

        if (ruleBranches.isEmpty())
            println("Rule $ruleName has no branches")

        val formedRule = if (ruleBranches.size == 1)
            "$ruleName  : ${ruleBranches[0]} { Console.WriteLine(\"$ruleName.(${ruleBranches[0].trim()})\"); }\n\t;\n"
        else {
            val thisRule = StringBuilder()
            thisRule.append("$ruleName  : ${ruleBranches[0].trim()} { Console.WriteLine(\"$ruleName.(${ruleBranches[0].trim()})\"); }\n")
            for (i in 1 until ruleBranches.size) {
                val branch = ruleBranches[i]
                thisRule.append("\t| ${branch.trim()} { Console.WriteLine(\"$ruleName.(${branch.trim()})\"); }\n")
            }
            thisRule.append("\t;\n")
            thisRule.toString()
        }

        output.append(formedRule)
    }

    File(outputFile).writeText(output.toString())
}

val optionals = mutableListOf<String>()

fun convert(inputFile: String, outputFile: String) {
    var content = File(inputFile).readText()
    var output = StringBuilder()

    val rulesSectionStart = content.indexOf("%%")
    val rulesSectionEnd = content.lastIndexOf("%%")
    content = content.substring(rulesSectionStart + 2, rulesSectionEnd)

    output.append(
        "module.exports = grammar({\n" +
                "    name: 'pascalabcnet',\n" +
                "\n" +
                "    rules: {"
    )

    val rules = content.split(';')
    for (rule in rules) {
        if (rule.trim().isEmpty())
            continue
        val splitRule = removeSemanticActions(rule.trim()).split(':')
        val ruleName = splitRule[0].trim()

        if (ruleName == "template_empty_param_list") {
            output.append("template_empty_param_list: \$ => /,+/,\n")
            continue
        }
        if (ruleName == "template_empty_param")
            continue

        val ruleBranches = splitRule[1].split('|')

        if (ruleBranches.isEmpty())
            println("Rule $ruleName has no branches")
        val formedRule = if (ruleBranches.size == 1)
            formOneBranchRule(ruleName, ruleBranches)
        else
            formManyBranchRule(ruleName, ruleBranches)

//        println(formedRule)
        output.append(formedRule)
    }

    output.append("}\n});")

    val result = postProcess(output.toString())

    File(outputFile).writeText(result)
}

fun postProcess(output: String): String {
    var newOutput = output
    for (optionalRule in optionals) {
        newOutput = newOutput.replace("$.$optionalRule,", "optional($.$optionalRule),")
    }

    newOutput = newOutput.replace("$.template_empty_param_list,", "optional(\$.template_empty_param_list),")

    return newOutput
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
    builder.append('\n')
    return builder.toString()
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

fun makeHeader(name: String): String {
    return "$name: $ => "
}

fun formManyBranchRule(ruleName: String, ruleBranches: List<String>): String {
    val builder = StringBuilder()
    builder.append(makeHeader(ruleName))

    val actuallyMoreThanOneBranch = ruleBranches.count { x -> x.trim().isNotEmpty() } > 1
    if (actuallyMoreThanOneBranch)
        builder.append("choice(\n")

    for (branch in ruleBranches) {
        if (branch.trim().isEmpty()) {
            optionals.add(ruleName)
            File("/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/optionals.txt").appendText("$ruleName\n")
        } else
            builder.append(processBranch(branch.trim().split(' ')))
    }

    if (actuallyMoreThanOneBranch)
        builder.append("),\n")

    return builder.toString()
}

fun removeSemanticActions(rule: String): String {
    return rule.replace("\\{(.|\\n)+?\\}", "").replace("\\{(.)+?\\}", "")
}