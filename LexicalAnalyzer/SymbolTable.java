package LexicalAnalyzer;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, Symbol> table;
    public SymbolTable() {
        table = new HashMap<>();
    }
    public void addSymbol(String name, String type, String scope, int line, int column) {
        if (!table.containsKey(name)) {
            Symbol symbol = new Symbol(name, type, scope, line, column);
            table.put(name, symbol);
        } else {
            System.out.println("Warning: El símbolo " + name + " ya está definido.");
        }
    }
    public Symbol getSymbol(String name) {
        return table.get(name);
    }
    //Cuando nos encontremos con un token tipo ID, hay que añadirlo en la tabla de simbolos, y tambien ir comprobando el contenido de la tabla cuando sea necesario, esto tenemos que hacerlo en el analizador lexico o en el parser segun decidamos
    public void printTable() {
        for (Symbol symbol : table.values()) {
            System.out.println(symbol);
        }
    }
}
