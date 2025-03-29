package LexicalAnalyzer;

public class Token {
    String token;

    /**
     * This class represents a token in the lexical analyzer.
     * It contains a string that represents the token.
     *
     * @param token The string that represents the token.
     */
    public Token(String token) {
        this.token = token;
    }

    /**
     * This method returns the string that represents the token.
     *
     * @return The string that represents the token.
     */
    public String getToken() {
        return token;
    }

}
