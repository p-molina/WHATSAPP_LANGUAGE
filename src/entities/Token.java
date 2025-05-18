package entities;

/**
 * Token representa una unitat lèxica amb tipus, lexema i posició.
 */
public class Token {
    private String type;
    private String lexeme;
    private int line;
    private int column;

    /**
     * Construeix un token amb informació bàsica.
     *
     * @param type   Tipus de token.
     * @param lexeme Lexema reconegut.
     * @param line   Línia on es troba.
     * @param column Columna on es troba.
     */
    public Token(String type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    /**
     * Retorna el tipus del token.
     *
     * @return Tipus del token.
     */
    public String getType() {
        return type;
    }

    /**
     * Retorna el lexema del token.
     *
     * @return Lexema del token.
     */
    public String getLexeme() {
        return lexeme;
    }

    /**
     * Retorna el número de línia a la que està el token.
     *
     * @return Número de línia del token.
     */
    public int getLine() {
        return line;
    }

    /**
     * Retorna el número de columna a la que està el token.
     *
     * @return Número de columna.
     */
    public int getColumn() {
        return column;
    }

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
