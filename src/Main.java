// src/Main.java
import ParserAnalyzer.ParserAnalyzer;
import entities.Dictionary;
import entities.Grammar;
import entities.ParserTableBuilder;
import entities.Node;
import LexicalAnalyzer.LexicalAnalyzer;

public class Main {
    public static void main(String[] args) {
        try {
            // 1) Carga diccionario y gramática
            Dictionary dict = new Dictionary("resources/diccionari.json");
            Grammar grammar = new Grammar   ("resources/grammar.json");

            // 2) Construye la tabla LL(1)
            ParserTableBuilder builder = new ParserTableBuilder(dict, grammar);
            builder.buildParsingTable();

            // 3) Tokeniza el fichero de entrada
            LexicalAnalyzer lexer = new LexicalAnalyzer(dict);
            lexer.tokenize("resources/whatsappFile.txt");

            // 4) Parsea y obtiene el árbol
            ParserAnalyzer parser = new ParserAnalyzer(grammar, builder);
            Node root = parser.parse(lexer);

            System.out.println("¡Parseo completado sin errores!");
            printTree(root, 0);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void printTree(Node node, int level) {
        System.out.println("  ".repeat(level) + node);
        for (Node child : node.getChildren()) {
            printTree(child, level + 1);
        }
    }
}
