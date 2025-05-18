package entities;

import SemanticAnalyzer.SemanticErrorType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Gestiona els símbols en diferents àmbits (scopes) i evita redeclaracions.
 */
public class SymbolTable {
    private final Map<Integer, Map<String, Symbol>> table = new HashMap<>();

    /**
     * Cerca un símbol pel seu nom en l'àmbit més profund.
     *
     * @param name Nom del símbol a cercar.
     * @return El símbol trobat o null si no existeix.
     */
    public Symbol lookup(String name) {
        // Busquem des del scope més profund cap al global
        return getSymbol(name, getMaxScope());
    }

    private int getMaxScope() {
        return table.keySet().stream().max(Integer::compareTo).orElse(0);
    }

    /**
     * Afegeix un nou símbol a la taula en un àmbit concret.
     *
     * @param name   Nom del símbol.
     * @param type   Tipus de dades.
     * @param scope  Àmbit numèric.
     * @param line   Línia de declaració.
     * @param column Columna de declaració.
     * @throws RuntimeException Si ja existeix en aquest àmbit.
     */
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
     * Retorna un símbol cercant des d'un àmbit específic fins al global.
     *
     * @param name  Nom del símbol.
     * @param scope Àmbit inicial de cerca.
     * @return El símbol trobat o null.
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
     * Retorna els símbols d'un àmbit concret.
     *
     * @param scope Àmbit de consulta.
     * @return Mapa de noms a símbols.
     */
    public Map<String, Symbol> getScopeSymbols(int scope) {
        return Collections.unmodifiableMap(
                table.getOrDefault(scope, Collections.emptyMap())
        );
    }

    /**
     * Retorna tots els símbols de tots els àmbits aplanats.
     *
     * @return Col·lecció de tots els símbols.
     */
    public Collection<Symbol> getAllSymbols() {
        return table.values().stream()
                .flatMap(m -> m.values().stream())
                .toList();
    }

    /**
     * Mostra per consola la taula de símbols.
     */
    public void printTable() {
        String separator = "+-----------+--------+-----------+------+--------+";
        String header    = "| Name      | Scope  | TYPE      | LINE | COLUMN |";

        System.out.println(separator);
        System.out.println(header);
        System.out.println(separator);

        for (int scope : table.keySet()) {
            for (Symbol sym : table.get(scope).values()) {
                System.out.printf("| %-9s | %-6d | %-9s | %-4d | %-6d |\n",
                        sym.getName(),
                        sym.getScope(),
                        sym.getType(),
                        sym.getLine(),
                        sym.getColumn()
                );
            }
        }

        System.out.println(separator);
    }

}
