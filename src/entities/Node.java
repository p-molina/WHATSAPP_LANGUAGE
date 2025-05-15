package entities;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private String symbol;
    private Token token;
    private List<Node> children;
    private Node parent;


    public Node(String symbol) {
        this.symbol = symbol;
        this.children = new ArrayList<>();
    }

    public Node(Token token) {
        this.token = token;
        this.symbol = token.getType();
        this.children = new ArrayList<>();
    }

    public String getSymbol() { return symbol; }
    public Token getToken() { return token; }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setToken(Token token) {
        this.token = token;
        if (token != null) {
            this.symbol = token.getType();
        }
    }

    public List<Node> getChildren() { return children; }
    public void addChild(Node child) {
        child.setParent(this);
        children.add(child);
    }

    public Node getParent() { return parent; }
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