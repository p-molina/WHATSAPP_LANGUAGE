package TAC;

import entities.Node;
import entities.Token;

import java.util.*;

public class TACGenerator {

    private final List<String> code = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private int tempCounter = 0;
    private int labelCounter = 0;

    private String newTemp() { return "t" + tempCounter++; }
    private String newLabel() { return "L" + labelCounter++; }

    private boolean awaitingArraySize = false;
    private Integer pendingArraySize = null;

    private static class ArrayCtx {
        final String name;
        final int declaredSize;
        final List<String> values = new ArrayList<>();
        ArrayCtx(String n, int sz) { name = n; declaredSize = sz; }
    }
    private ArrayCtx currentArray = null;

    private Node defaultRoot;
    public TACGenerator() {}
    public TACGenerator(Node root) { this.defaultRoot = root; }

    public List<String> generate() {
        if (defaultRoot == null) throw new IllegalStateException("Root not set");
        return generate(defaultRoot);
    }

    public List<String> generate(Node root) {
        code.clear(); stack.clear(); tempCounter = 0; labelCounter = 0;
        awaitingArraySize = false; pendingArraySize = null; currentArray = null;
        visit(root);
        return List.copyOf(code);
    }

    private void visit(Node n) {
        for (Node ch : n.getChildren()) visit(ch);

        Token tok = n.getToken();
        if (tok != null) handleToken(tok);
        else handleNonTerminal(n);
    }

    private void handleToken(Token tok) {
        String type = tok.getType();

        switch (type) {
            case "ARRAY" -> awaitingArraySize = true;
            case "INT_VALUE" -> {
                if (awaitingArraySize && pendingArraySize == null) {
                    pendingArraySize = Integer.parseInt(tok.getLexeme());
                    return;
                }
                literalToTemp(tok);
            }
            case "FLOAT_VALUE", "CHAR_VALUE" -> literalToTemp(tok);
            case "ID" -> {
                if (pendingArraySize != null) {
                    currentArray = new ArrayCtx(tok.getLexeme(), pendingArraySize);
                    awaitingArraySize = false; pendingArraySize = null;
                } else {
                    stack.push(tok.getLexeme());
                }
            }
            case "OPEN_CLAUDATOR", "CLOSE_CLAUDATOR" -> {
                if ("CLOSE_CLAUDATOR".equals(type) && currentArray != null && !currentArray.values.isEmpty()) {
                    generateArrayInit(currentArray);
                    currentArray = null;
                }
            }
        }
    }

    private void literalToTemp(Token tok) {
        String tmp = newTemp();
        code.add(tmp + " = " + tok.getLexeme());
        stack.push(tmp);
        if (currentArray != null) currentArray.values.add(tmp);
    }

    private void handleNonTerminal(Node n) {
        if (n.getChildren().isEmpty()) return;
        Token firstTok = n.getChildren().get(0).getToken();
        if (firstTok == null) return;
        String type = firstTok.getType();

        if ("EQUAL_ASSIGNATION".equals(type)) {
            if (stack.size() < 2) return;
            String rhs = stack.pop();
            String lhs = stack.pop();
            code.add(lhs + " = " + rhs);
            stack.push(lhs);
            return;
        }

        if (isOperator(type) || isComparison(type)) {
            if (n.getChildren().size() < 3) return;
            Node leftNode = n.getChildren().get(0);
            Node rightNode = n.getChildren().get(2);

            visit(leftNode);
            String left = stack.pop();
            visit(rightNode);
            String right = stack.pop();

            String tmp = newTemp();
            String op = isComparison(type) ? mapComparison(type) : map(type);
            code.add(tmp + " = " + left + ' ' + op + ' ' + right);
            stack.push(tmp);
            return;
        }


        if ("IF".equals(type)) {
            Node condNode = n.getChildren().get(2);
            visit(condNode);
            String tCond = stack.pop();

            String labelThen = newLabel();
            String labelEnd = newLabel();

            code.add("if " + tCond + " goto " + labelThen);
            if (n.getChildren().size() > 7) {
                Node elseNode = n.getChildren().get(7);
                visit(elseNode);
            }
            code.add("goto " + labelEnd);
            code.add(labelThen + ":");
            Node bodyIf = n.getChildren().get(5);
            visit(bodyIf);
            code.add(labelEnd + ":");
            return;
        }

        if ("BUCLE".equals(type)) {
            String labelCond = newLabel();
            String labelBody = newLabel();
            String labelExit = newLabel();

            code.add(labelCond + ":");

            Node condNode = n.getChildren().get(2);
            visit(condNode);
            String tCond = stack.pop();

            code.add("if " + tCond + " goto " + labelBody);
            code.add("goto " + labelExit);

            code.add(labelBody + ":");
            Node bodyNode = n.getChildren().get(5);
            visit(bodyNode);
            code.add("goto " + labelCond);

            code.add(labelExit + ":");
            return;
        }
    }

    private void generateArrayInit(ArrayCtx ctx) {
        int size = ctx.declaredSize > 0 ? ctx.declaredSize : ctx.values.size();
        String tSize = newTemp();
        code.add(tSize + " = " + size);
        code.add(ctx.name + " = alloc " + tSize);
        for (int i = 0; i < ctx.values.size(); i++) {
            code.add(ctx.name + "[" + i + "] = " + ctx.values.get(i));
        }
    }

    private static boolean isOperator(String t) {
        return Set.of("SUM", "MINUS", "MULTIPLY", "DIVISION").contains(t);
    }

    private static String map(String t) {
        return switch (t) {
            case "SUM" -> "+";
            case "MINUS" -> "-";
            case "MULTIPLY" -> "*";
            case "DIVISION" -> "/";
            default -> "?";
        };
    }

    private static boolean isComparison(String t) {
        return Set.of("EQUAL_COMPARATION", "DIFFERENT", "LOWER", "BIGGER", "LOWER_EQUAL", "BIGGER_EQUAL").contains(t);
    }

    private static String mapComparison(String t) {
        return switch (t) {
            case "EQUAL_COMPARATION" -> "==";
            case "DIFFERENT" -> "!=";
            case "LOWER" -> "<";
            case "BIGGER" -> ">";
            case "LOWER_EQUAL" -> "<=";
            case "BIGGER_EQUAL" -> ">=";
            default -> "?";
        };
    }
}
