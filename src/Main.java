import ParserAnalyzer.ParserAnalyzer;
import SemanticAnalyzer.SemanticAnalyzer;
import SemanticAnalyzer.TACGenerator;
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
            lexer.tokenize("testing/test1.wsp");

            ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);
            Node root = parser.parse(lexer);
            printTree(root, "", true);

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(root);
            semanticAnalyzer.analyze();

            TACGenerator tacGen = new TACGenerator(root);
            List<String> tac = tacGen.generate(root);
            tac.forEach(System.out::println);

        } catch (Exception e) {
            System.err.println("Error durante el parseo:");
            e.printStackTrace();
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
