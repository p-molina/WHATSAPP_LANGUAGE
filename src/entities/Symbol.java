// src/entities/Symbol.java
package entities;

public class Symbol {
    private final String name;
    private final String type;
    private final int scope;
    private final int line;
    private final int column;

    public Symbol(String name, String type, int scope, int line, int column) {
        this.name   = name;
        this.type   = type;
        this.scope  = scope;
        this.line   = line;
        this.column = column;
    }

    public String getName()   { return name; }
    public String getType()   { return type; }
    public int getScope()  { return scope; }
    public int getLine()   { return line; }
    public int getColumn() { return column; }

    @Override
    public String toString() {
        return String.format(
                "Symbol{name='%s', type='%s', scope='%s', line=%d, col=%d}",
                name, type, scope, line, column
        );
    }
}
