package entities;

import java.util.List;

public class Node {
    private Token father;
    private List<Node> children;

    public Node(Token token) {}

    public Token getFather() {
        return father;
    }

    public void setFather(Token father) {
        this.father = father;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

}
