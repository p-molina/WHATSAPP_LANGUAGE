package LexicalAnalyzer;

public class Symbol {
    private String name;
    private String type;
    private String scope;
    private int line;
    private int column;

    public Symbol(String name, String type, int line, int column) {
        this.name = name;
        this.type = type;
        this.line = line;
        this.column = column;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }


    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", line=" + line +
                ", column=" + column +
                '}';
    }
}
