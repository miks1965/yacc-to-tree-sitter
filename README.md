# yacc-to-tree-sitter
Converter from yacc to tree-sitter grammar format

Usage:

```
java -jar yacc-to-tree-sitter.jar languageName inputFile.y outputFile.js
```

- Ignores semantic actions.
- Ignores comments (after `//` and between `/*` and `*/`).
- Marks rules with empty productions as `optional` in places of usage.

Please note that this tool doesn't format generated code. For the readability of the resulting grammar, you need to use JavaScript formatter.
