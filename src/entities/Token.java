package entities;

public class Token {
    private String type;
    private String lexeme;
    private int line;
    private int column;

    /**
     * Constructor for Token.
     *
     * @param type   The type of the token.
     * @param lexeme The lexeme of the token.
     * @param line   The line number where the token was found.
     * @param column The column number where the token was found.
     */
    public Token(String type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the type of the token.
     *
     * @return The type of the token.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the lexeme of the token.
     *
     * @return The lexeme of the token.
     */
    public String getLexeme() {
        return lexeme;
    }

    /**
     * Returns the line number where the token was found.
     *
     * @return The line number of the token.
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number where the token was found.
     *
     * @return The column number of the token.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns a string representation of the token.
     *
     * @return A string representation of the token.
     */
    @Override
    public String toString() {
        return "Token{" +
                "type='" + type + '\'' +
                ", lexeme='" + lexeme + '\'' +
                ", line=" + line +
                ", column=" + column +
                '}';
    }
}
