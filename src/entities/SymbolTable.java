package entities;

import SemanticAnalyzer.SemanticErrorType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class SymbolTable {

    // mapa: scope -> (nom símbol -> Symbol)
    private final Map<Integer, Map<String, Symbol>> table = new HashMap<>();

    public void addSymbol(String name, String type, int scope, int line, int column) {
        // Si el scope no existeix, el creem
        table.putIfAbsent(scope, new HashMap<>());

        // Comprovem si el símbol ja existeix en el scope actual
        if (table.get(scope).containsKey(name)) {
            throw new RuntimeException(
                    SemanticErrorType.SYMBOL_REDECLARED_IN_SCOPE.format(name, scope)
            );
        }

        // Afegim el nou símbol a la taula
        Symbol symbol = new Symbol(name, type, scope, line, column);
        table.get(scope).put(name, symbol);
    }

    /**
     * Retorna el símbol amb nom `name`, cercant des del scope donat cap a l'àmbit global (0).
     */
    public Symbol getSymbol(String name, int scope) {
        for (int s = scope; s >= 0; s--) {
            Map<String, Symbol> scopeTable = table.get(s);
            if (scopeTable != null && scopeTable.containsKey(name)) {
                return scopeTable.get(name);
            }
        }
        return null;
    }

    /**
     * Retorna els símbols definit en un scope concret. Si no n'hi ha, retorna un mapa buit.
     */
    public Map<String, Symbol> getScopeSymbols(int scope) {
        return Collections.unmodifiableMap(
                table.getOrDefault(scope, Collections.emptyMap())
        );
    }

    /**
     * Retorna tots els símbols de tots els scopes (aplanant).
     */
    public Collection<Symbol> getAllSymbols() {
        return table.values().stream()
                .flatMap(m -> m.values().stream())
                .toList();
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
