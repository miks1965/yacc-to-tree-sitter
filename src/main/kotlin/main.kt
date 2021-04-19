import java.io.File

fun main(args: Array<String>) {
    File("/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/output.txt").writeText("")
    File("/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/optionals.txt").writeText("")
    convert("/home/lereena/github/tree-sitter-pascalabcnet/full1904.txt",
        "/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/output.txt")
}

fun convert(inputFile: String, outputFile: String) {
    var content = File(inputFile).readText()

    val rulesSectionStart = content.indexOf("%%")
    val rulesSectionEnd = content.lastIndexOf("%%")
    content = content.substring(rulesSectionStart + 2, rulesSectionEnd)

    val rules = content.split(';')
    for (rule in rules) {
        val splitRule = removeSemanticActions(rule.trim()).split(':')
        val ruleName = splitRule[0].trim()
        val ruleBranches = splitRule[1].split('|')

        if (ruleBranches.isEmpty())
            println("Rule $ruleName has no branches")
        val formedRule = if (ruleBranches.size == 1)
            formOneBranchRule(ruleName, ruleBranches)
        else
            formManyBranchRule(ruleName, ruleBranches)

        println(formedRule)
        File(outputFile).appendText(formedRule)
    }
}

fun formOneBranchRule(ruleName: String, ruleBranches: List<String>): String {
    val builder = StringBuilder()
    builder.append(makeHeader(ruleName) + "$.")

    val branch = ruleBranches[0].trim().split(' ')
    if (branch.size == 1)
        builder.append(branch[0] + ',')
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
    builder.append("choice(\n")

    for (branch in ruleBranches) {
        if (branch.trim().isEmpty())
            File("/home/lereena/IdeaProjects/yacc-to-tree-sitter/src/optionals.txt").appendText("$ruleName\n")
        else
            builder.append(processBranch(branch.trim().split(' ')))
    }

    builder.append("),\n")
    return builder.toString()
}

fun removeSemanticActions(rule: String): String {
    return rule.replace("\\{(.|\\n)+?\\}", "").replace("\\{(.)+?\\}", "")
}