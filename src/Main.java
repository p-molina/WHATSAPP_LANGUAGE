import SemanticAnalyzer.SampleTreeBuilder;
import SemanticAnalyzer.SemanticAnalyzer;
import entities.Dictionary;
import LexicalAnalyzer.LexicalAnalyzer;
import entities.Node;
import entities.Token;

public class Main {
    public static void main(String[] args) {
        try {
            // Lexical analysis
            System.out.println("\n-------------------------------------");
            System.out.println("Starting lexical analysis...");
            Dictionary dict = new Dictionary("resources/diccionari.json");
            LexicalAnalyzer lexer = new LexicalAnalyzer(dict);

            lexer.tokenize("resources/whatsappFile.txt");
            Token token;
            while ((token = lexer.getNextToken()) != null) {
                System.out.println(token);
            }
            System.out.println("Lexical analysis passed!");
            System.out.println("-------------------------------------\n");


            //Sintactic analysis (simulation)
            System.out.println("\n-------------------------------------");
            System.out.println("Starting syntactic analysis...");
            Node root = SampleTreeBuilder.createSampleTree();
            System.out.println("Syntactic analysis passed!");
            System.out.println("-------------------------------------\n");


            // Semantic analysis
            System.out.println("\n-------------------------------------");
            System.out.println("Starting semantic analysis...");
            SemanticAnalyzer analyzer = new SemanticAnalyzer(root);
            analyzer.analyze();
            System.out.println("Semantic analysis passed!");
            System.out.println("-------------------------------------\n");


        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
