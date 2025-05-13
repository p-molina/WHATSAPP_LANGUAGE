import Testing.TestExecute;
import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import entities.Dictionary;
import entities.Grammar;
import entities.Node;
import entities.ParserTableBuilder;
import LexicalAnalyzer.LexicalAnalyzer;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            Dictionary dict    = new Dictionary("resources/diccionari.json");
            Grammar    grammar = new Grammar("resources/grammar.json");
            ParserTableBuilder builder = new ParserTableBuilder(dict, grammar);
            builder.buildParsingTable();

            LexicalAnalyzer lexer   = new LexicalAnalyzer(dict);
            ParserAnalyzer  parser  = new ParserAnalyzer(grammar, builder);

            boolean runTests = false;
            // Cambiar esto al final para que sea por un parametro de entrada
            String  fileToParse = "resources/code.wsp";
            for (String arg : args) {
                if ("-test".equals(arg)) {
                    runTests = true;
                } else {
                    fileToParse = arg;
                }
            }

            if (runTests) {
                TestExecute tests = new TestExecute(lexer, parser);
                tests.runAll();
            } else {
                if (fileToParse == null) {
                    System.err.println("Uso:");
                    System.err.println("  java Main -test               # Para correr todos los tests");
                    System.err.println("  java Main <archivo.wsp>       # Para parsear un único archivo");
                    System.exit(1);
                }

                // Limpia estado del lexer antes de tokenizar
                lexer.tokenize(fileToParse);
                Node root = parser.parse(lexer);
                printTree(root, "", true);
                new SemanticAnalyzer(root).analyze();
            }

        } catch (Exception e) {
            System.err.println("Error durante la ejecución:");
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void printTree(Node node, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node);
        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTree(children.get(i),
                    prefix + (isTail ? "    " : "│   "),
                    i == children.size() - 1);
        }
    }
}
