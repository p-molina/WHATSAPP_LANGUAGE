import LexicalAnalyzer.LexicalAnalyzer;

public class main {

    public static void main(String[] args) {

        // Create a LexicalAnalyzer object
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();

        // Read the file and create a stream of tokens
        lexicalAnalyzer.analizeFile("data/whatsappFile.txt");

        // TODO: Implement the logic to process the tokens
        // Use the getNextToken() method to retrieve tokens one by one

    }

}
