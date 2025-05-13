package TAC;

import entities.Node;
import entities.Token;

import java.util.*;

/**
 * TACGenerator actualitzat per a bucles i funcions
 */
public class TACGenerator {

    // ---------- Estat intern ----------
    private final List<String> code = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private int tempCounter = 0;
    private int labelCounter = 0;

    private String newTemp()  { return "t" + (tempCounter++); }
    private String newLabel() { return "L" + (labelCounter++); }

    // ---------- Suport arrays ----------
    private boolean awaitingArraySize = false;
    private Integer pendingArraySize = null;

    private static class ArrayCtx {
        final String name;
        final int declaredSize;
        final List<String> values = new ArrayList<>();
        ArrayCtx(String n, int sz) { name = n; declaredSize = sz; }
    }
    private ArrayCtx currentArray = null;

    // ---------- Entrada ----------
    private Node defaultRoot;
    public TACGenerator() {}
    public TACGenerator(Node root) { this.defaultRoot = root; }

    public List<String> generate() {
        if (defaultRoot == null) throw new IllegalStateException("Root not set");
        return generate(defaultRoot);
    }

    public List<String> generate(Node root) {
        code.clear();
        stack.clear();
        tempCounter = 0;
        labelCounter = 0;
        awaitingArraySize = false;
        pendingArraySize = null;
        currentArray = null;

        visit(root);
        return List.copyOf(code);
    }

    // ============================================================
    // Visita DFS amb maneig de funcions i bucles
    // ============================================================
    private void visit(Node n) {
        List<Node> children = n.getChildren();
        Token firstChildTok = null;
        if (!children.isEmpty()) firstChildTok = children.get(0).getToken();

        // ===== Funció (proc) =====
        if (firstChildTok != null && "NUM".equals(firstChildTok.getType())
                && children.size() > 3 && children.get(2).getToken() != null
                && "JAJAJ".equals(children.get(2).getToken().getType())) {
            // Declaració de funció: children[1]=ID, [3]=body
            String funcName = children.get(1).getToken().getLexeme();
            code.add(funcName + ":");
            stack.clear();  // buidem stack per seguretat
            visit(children.get(3));
            return;
        }

        // ===== Bucle (while) =====
        if (firstChildTok != null && "BUCLE".equals(firstChildTok.getType())) {
            handleWhile(n);
            return;
        }

        // Visita normal dels fills
        for (Node ch : children) {
            visit(ch);
        }

        // Després, terminals i no-terminals
        Token tok = n.getToken();
        if (tok != null) handleToken(tok);
        else handleNonTerminal(n);
    }

    // ---------- Maneig robust del while ----------
    private void handleWhile(Node n) {
        List<Node> children = n.getChildren();
        if (children.size() < 6) {
            for (Node ch : children) visit(ch);
            return;
        }
        Node condNode = children.get(2);
        Node bodyNode = children.get(5);

        String labelCond = newLabel();
        String labelBody = newLabel();
        String labelExit = newLabel();

        code.add(labelCond + ":");
        visit(condNode);
        if (stack.isEmpty()) throw new IllegalStateException("Condició del bucle no va generar cap valor");
        String tCond = stack.pop();
        code.add("if " + tCond + " goto " + labelBody);
        code.add("goto " + labelExit);

        code.add(labelBody + ":");
        visit(bodyNode);
        code.add("goto " + labelCond);

        code.add(labelExit + ":");
    }

    // ---------- Terminals ----------
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
                    awaitingArraySize = false;
                    pendingArraySize = null;
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

    // ---------- No terminals ----------
    private void handleNonTerminal(Node n) {
        int nChildren = n.getChildren().size();
        if (nChildren == 0) return;

        Token firstTok = n.getChildren().get(0).getToken();

        /* RETURN / XINPUM */
        if (firstTok != null) {
            String ft = firstTok.getType();
            if ("XINPUM".equals(ft) || "RETURN".equals(ft)) {
                String retVal = stack.isEmpty() ? null : stack.pop();
                code.add(retVal == null ? "return" : "return " + retVal);
                stack.clear();
                return;
            }
        }

        /* Assignacions i operacions */
        if (firstTok != null) {
            String rootType = firstTok.getType();
            if ("EQUAL_ASSIGNATION".equals(rootType)) {
                String rhs = stack.pop();
                String lhs = stack.pop();
                code.add(lhs + " = " + rhs);
                stack.push(lhs);
                return;
            }
            if (isOperator(rootType) || isComparison(rootType)) {
                String right = stack.pop();
                String left  = stack.pop();
                String tmp   = newTemp();
                String op    = isComparison(rootType) ? mapComparison(rootType) : map(rootType);
                code.add(tmp + " = " + left + ' ' + op + ' ' + right);
                stack.push(tmp);
                return;
            }
        }

        if (nChildren == 3) {
            Node opNode = n.getChildren().get(1);
            Token opTok = opNode.getToken();
            if (opTok != null) {
                String t = opTok.getType();
                if ("EQUAL_ASSIGNATION".equals(t)) {
                    String rhs = stack.pop();
                    String lhs = stack.pop();
                    code.add(lhs + " = " + rhs);
                    stack.push(lhs);
                    return;
                }
                if (isOperator(t) || isComparison(t)) {
                    String right = stack.pop();
                    String left  = stack.pop();
                    String tmp   = newTemp();
                    String op    = isComparison(t) ? mapComparison(t) : map(t);
                    code.add(tmp + " = " + left + ' ' + op + ' ' + right);
                    stack.push(tmp);
                    return;
                }
            }
        }

        /* IF */
        if (firstTok != null && "IF".equals(firstTok.getType())) {
            String tCond = stack.pop();
            String labelThen = newLabel();
            String labelEnd  = newLabel();

            code.add("if " + tCond + " goto " + labelThen);
            if (nChildren > 7) {
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
    }

    // ---------- Arrays ----------
    private void generateArrayInit(ArrayCtx ctx) {
        int size = ctx.declaredSize > 0 ? ctx.declaredSize : ctx.values.size();
        String tSize = newTemp();
        code.add(tSize + " = " + size);
        code.add(ctx.name + " = alloc " + tSize);
        for (int i = 0; i < ctx.values.size(); i++) {
            code.add(ctx.name + "[" + i + "] = " + ctx.values.get(i));
        }
    }

    // ---------- Helpers ----------
    private static boolean isOperator(String t) {
        return Set.of("SUM", "MINUS", "MULTIPLY", "DIVISION").contains(t);
    }
    private static boolean isComparison(String t) {
        return Set.of("EQUAL_COMPARATION", "DIFFERENT", "LOWER", "BIGGER", "LOWER_EQUAL", "BIGGER_EQUAL").contains(t);
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
