package entities;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private final Map<String, Symbol> table = new HashMap<>();

    public void addSymbol(String name, String type, int scope, int line, int column) {
        if (table.containsKey(name)) {
            System.err.printf(
                    "Warning: símbolo '%s' ya declarado en este ámbito (%s:%d,%d)%n",
                    name, scope, table.get(name).getLine(), table.get(name).getColumn()
            );
        } else {
            table.put(name, new Symbol(name, type, scope, line, column));
        }
    }

    public Symbol getSymbol(String name) {
        return table.get(name);
    }

    public void printTable() {
        System.out.println("=== Tabla de símbolos ===");
        table.values().forEach(s -> System.out.println("  " + s));
    }

    public boolean contains(String name) {
        return table.containsKey(name);
    }
}
