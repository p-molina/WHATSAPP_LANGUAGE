import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import SemanticAnalyzer.SemanticAnalyzerDEBUG;
import entities.Dictionary;
import entities.Grammar;
import entities.Node;
import entities.ParserTableBuilder;
import LexicalAnalyzer.LexicalAnalyzer;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Dictionary dict = new Dictionary("resources/diccionari.json");
            Grammar grammar = new Grammar("resources/grammar.json");

            ParserTableBuilder builder = new ParserTableBuilder(dict, grammar);
            builder.buildParsingTable();

            LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
            lexer.tokenize("testing/test2.wsp");

            ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);
            Node root = parser.parse(lexer);
            printTree(root, "", true);

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(root);
            semanticAnalyzer.analyze();

            SemanticAnalyzerDEBUG semanticAnalyzerD = new SemanticAnalyzerDEBUG(root);
            semanticAnalyzerD.analyze();

        } catch (RuntimeException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printTree(Node node, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node);
        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean last = (i == children.size() - 1);
            printTree(children.get(i), prefix + (isTail ? "    " : "│   "), last);
        }
    }
}
