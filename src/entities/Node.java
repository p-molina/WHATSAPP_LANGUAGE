package entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un element de l'arbre sintàctic.
 * <p>Pot ser un no-terminal (symbol) o un terminal (token).</p>
 */
public class Node {
    private String symbol;
    private Token token;
    private final List<Node> children;
    private Node parent;

    /**
     * Construeix un node no-terminal amb un símbol.
     *
     * @param symbol Nom del no-terminal.
     */
    public Node(String symbol) {
        this.symbol = symbol;
        this.children = new ArrayList<>();
    }

    /**
     * Construeix un node terminal a partir d'un token.
     *
     * @param token Token associat al node.
     */
    public Node(Token token) {
        this.token = token;
        this.symbol = token.getType();
        this.children = new ArrayList<>();
    }

    /** Retorna el símbol o tipus del node. */
    public String getSymbol() { return symbol; }

    /** Retorna el token si és un node terminal. */
    public Token getToken() { return token; }

    /**
     * Assigna un token al node i actualitza el símbol.
     *
     * @param token Nou token del node.
     */
    public void setToken(Token token) {
        this.token = token;
        if (token != null) {
            this.symbol = token.getType();
        }
    }

    /** Retorna la llista de fills del node. */
    public List<Node> getChildren() { return children; }

    /**
     * Afegeix un node fill i ajusta el seu parent.
     *
     * @param child Node fill a afegir.
     */
    public void addChild(Node child) {
        child.setParent(this);
        children.add(child);
    }

    /** Retorna el node parent, o null si és l'arrel. */
    public Node getParent() { return parent; }

    /** Asigna el node parent. */
    public void setParent(Node parent) { this.parent = parent; }

    @Override
    public String toString() {
        if (token != null) {
            return "Terminal(" + token.getType() + ": " + token.getLexeme() + ")";
        } else {
            return "NonTerminal(" + symbol + ")";
        }
    }
}