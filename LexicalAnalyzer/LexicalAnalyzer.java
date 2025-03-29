package LexicalAnalyzer;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class LexicalAnalyzer {
    private ArrayList<Token> streamOfTokens;

    /**
     * This class is responsible for reading a file and creating a stream of tokens from the words in the file.
     * It uses the Token class to represent each word as a token.
     */
    public LexicalAnalyzer() {
        streamOfTokens = new ArrayList<Token>();
    }

    /**
     * This method reads a file and creates a stream of tokens from the words in the file.
     *
     * @param filePath The path to the file to be read.
     * @return An ArrayList of Token objects representing the words in the file.
     */
    public void analizeFile(String filePath) {
        try {
            // Create a File object
            File myObj = new File(filePath);

            // Create a Scanner object
            Scanner myReader = new Scanner(myObj);

            // Read the file line by line
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();

                // Split the line into words
                String[] words = data.split(" ");

                // For each word, create a Token object and add it to the stream of Tokens
                for (String word : words) {
                    Token token = new Token(word);
                    streamOfTokens.add(token);
                }
            }

            // Close the Scanner
            myReader.close();

        } catch (FileNotFoundException e) {
            // Handle the exception
            System.out.println("ERROR: File not found.");
        }

    }

    /**
     * This method returns the next token from the stream of tokens.
     *
     * @return The next Token object from the stream of tokens.
     */
    public Token getNextToken() {
        if (!streamOfTokens.isEmpty()) {
            // Get the first token from the stream
            Token token = streamOfTokens.getFirst();

            // Remove the first token from the stream
            streamOfTokens.removeFirst();
            return token;
        } else {
            return null; // No more tokens
        }
    }

    /**
     * This method checks if there are more tokens in the stream.
     *
     * @return true if there are more tokens, false otherwise.
     */
    public boolean hasNextToken() {
        return !streamOfTokens.isEmpty();
    }

}
