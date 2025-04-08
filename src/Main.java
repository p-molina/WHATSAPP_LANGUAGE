import ParserAnalyzer.ParserAnalyzer;
import entities.Dictionary;
import LexicalAnalyzer.LexicalAnalyzer;
import entities.Grammar;
import entities.Token;

public class Main {
    public static void main(String[] args) {
        try {
            //Leemos el diccionario y la gramatica de nuestro lenguaje
            Dictionary dict = new Dictionary("resources/diccionari.json");
            Grammar grammar = new Grammar("resources/grammar.json");

            LexicalAnalyzer lexer = new LexicalAnalyzer(dict);

            lexer.tokenize("resources/whatsappFile.txt");
            Token token;

            while ((token = lexer.getNextToken()) != null) {
                System.out.println(token);
            }

            ParserAnalyzer parserAnalyzer = new ParserAnalyzer(dict, grammar);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
