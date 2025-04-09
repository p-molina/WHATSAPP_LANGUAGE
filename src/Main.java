import ParserAnalyzer.ParserAnalyzer;
import entities.Dictionary;
import LexicalAnalyzer.LexicalAnalyzer;
import entities.Grammar;
import entities.Token;
import entities.Node;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            Dictionary dict = new Dictionary("resources/diccionari.json");
            Grammar grammar = new Grammar("resources/grammar.json");

            LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
            lexer.tokenize("resources/whatsappFile.txt");

            List<Token> tokens = lexer.getTokens();

            System.out.println("=== TOKENS ===");
            for (Token t : tokens) {
                System.out.println(t);
            }

            ParserAnalyzer parserAnalyzer = new ParserAnalyzer(dict, grammar);

            Map<String, Map<String, List<String>>> table = parserAnalyzer.getParsingTable();

            System.out.println("\n=== TABLA DE PARSING ===");
            for (String nonTerminal : table.keySet()) {
                System.out.println("No Terminal: " + nonTerminal);
                Map<String, List<String>> row = table.get(nonTerminal);

                for (String terminal : row.keySet()) {
                    List<String> production = row.get(terminal);
                    String productionStr = String.join(" ", production);
                    System.out.println("  Con lookahead='" + terminal + "' => "
                            + nonTerminal + " ::= " + productionStr);
                }
                System.out.println();
            }


            Node root = parserAnalyzer.parse(tokens);

            System.out.println("\n=== ÁRBOL SINTÁCTICO ===");
            printTree(root, "");

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void printTree(Node node, String indent) {
        System.out.println(indent + node);
        for (Node child : node.getChildren()) {
            printTree(child, indent + "  ");
        }
    }
}
