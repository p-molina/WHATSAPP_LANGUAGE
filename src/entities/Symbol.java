package entities;

/**
 * Representa una variable o identificador amb informació de tipus i ubicació.
 */
public class Symbol {
    private final String name;
    private final String type;
    private final int scope;
    private final int line;
    private final int column;

    /**
     * Construeix un símbol amb nom, tipus i informació de localització.
     *
     * @param name   Nom del símbol.
     * @param type   Tipus de dades del símbol.
     * @param scope  Àmbit numèric del símbol.
     * @param line   Línia on es troba.
     * @param column Columna on es troba.
     */
    public Symbol(String name, String type, int scope, int line, int column) {
        this.name   = name;
        this.type   = type;
        this.scope  = scope;
        this.line   = line;
        this.column = column;
    }

    /** @return Nom del símbol. */
    public String getName()   { return name; }
    /** @return Tipus de dades del símbol. */
    public String getType()   { return type; }
    /** @return Àmbit on es declara el símbol. */
    public int getScope()  { return scope; }
    /** @return Línia de declaració. */
    public int getLine()   { return line; }
    /** @return Columna de declaració. */
    public int getColumn() { return column; }

    @Override
    public String toString() {
        return String.format(
                "Symbol{name='%s', type='%s', scope='%s', line=%d, col=%d}",
                name, type, scope, line, column
        );
    }
}
