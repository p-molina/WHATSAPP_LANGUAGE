package entities;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private final Map<Integer, Map<String, Symbol>> table = new HashMap<>();

    public void addSymbol(String name, String type, int scope, int line, int column) {
        // Si el scope no existeix, el creem
        table.putIfAbsent(scope, new HashMap<>());

        // Comprovem si el símbol ja existeix en el scope actual
        if (table.get(scope).containsKey(name)) {
            throw new RuntimeException(SemanticErrorType.SYMBOL_REDECLARED_IN_SCOPE.format(name, scope));
        }

        // Afegim el nou símbol a la taula
        Symbol symbol = new Symbol(name, type, scope, line, column);
        table.get(scope).put(name, symbol);
    }

    public Symbol getSymbol(String name, int scope) {
        // Cerca del scope actual cap a globals (0)
        for (int s = scope; s >= 0; s--) {
            Map<String, Symbol> scopeTable = table.get(s);
            if (scopeTable != null && scopeTable.containsKey(name)) {
                return scopeTable.get(name);
            }
        }
        return null;
    }

    public void printTable() {
        System.out.println("=== Taula de símbols ===");
        for (int scope : table.keySet()) {
            for (Symbol sym : table.get(scope).values()) {
                System.out.println("\t" + sym);
            }
        }
    }
}
