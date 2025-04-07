import entities.Dictionary;
import LexicalAnalyzer.LexicalAnalyzer;
import entities.Token;

public class Main {
    public static void main(String[] args) {
        try {
            Dictionary dict = new Dictionary("resources/diccionari.json");
            LexicalAnalyzer lexer = new LexicalAnalyzer(dict);

            lexer.tokenize("resources/whatsappFile.txt");
            Token token;
            while ((token = lexer.getNextToken()) != null) {
                System.out.println(token);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
