package TAC;

import entities.Node;
import entities.Token;
import java.util.*;

/**
 * Genera codi TAC (tres adreces) a partir d'un AST de Node/Token
 */
public class TACGenerator {
    private final List<String> code = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private int tempCounter = 0;
    private int labelCounter = 0;

    /**
     * Genera TAC a partir de l'arbre sintàctic donat
     */
    public List<String> generate(Node root) {
        if (root == null) throw new IllegalArgumentException("AST root no pot ser null");
        code.clear();
        stack.clear();
        tempCounter = 0;
        labelCounter = 0;

        visit(root);
        return List.copyOf(code);
    }

    /**
     * Recórrer l'AST en profunditat i generar TAC
     */
    private void visit(Node n) {
        List<Node> children = n.getChildren();
        Token firstTok = children.isEmpty() ? null : children.get(0).getToken();
        if (firstTok != null
                && "INT".equals(firstTok.getType())
                && children.size() > 3
                && children.get(2).getToken().getType().equals("OPEN_CLAUDATOR")) {
            // 1) Etiqueta de funció
            String funcName = children.get(1).getToken().getLexeme();
            code.add(funcName + ":");
            // 2) Reinicia temporals per començar de t0 dins de la funció
            tempCounter = 0;
            // 3) Processa només el BODY (a children.get(3))
            visit(children.get(3));
            return;
        }
        // Funcions: "NUM ID JAJAJ" prefix
        if (firstTok != null && "NUM".equals(firstTok.getType())
                && children.size() > 2
                && children.get(1).getToken() != null
                && "JAJAJ".equals(children.get(2).getToken().getType())) {
            String funcName = children.get(1).getToken().getLexeme();
            code.add(funcName + ":");
            visit(children.get(3));  // cos de la funció
            return;
        }

        // While: prefix "BUCLE"
        if (firstTok != null && "BUCLE".equals(firstTok.getType())) {
            handleWhile(n);
            return;
        }

        // If: prefix "IF"
        if (firstTok != null && "IF".equals(firstTok.getType())) {
            handleIf(n);
            return;
        }

        // Recorre fills
        for (Node child : children) {
            visit(child);
        }

        // Processa node actual
        Token tok = n.getToken();
        if (tok != null) {
            handleToken(tok);
        } else {
            handleNonTerminal(n);
        }
    }

    /**
     * Genera codi TAC per a un bucle while
     */
    private void handleWhile(Node n) {
        String Lstart = newLabel();
        String Lend = newLabel();
        code.add(Lstart + ":");
        // condició a children[2]
        visit(n.getChildren().get(2));
        String condTmp = stack.pop();
        code.add("if_false " + condTmp + " goto " + Lend);
        // cos a children[5]
        visit(n.getChildren().get(5));
        code.add("goto " + Lstart);
        code.add(Lend + ":");
    }

    /**
     * Genera codi TAC per a un if
     */
    private void handleIf(Node n) {
        // condició a children[2]
        visit(n.getChildren().get(2));
        String condTmp = stack.pop();
        String Lthen = newLabel(), Lend = newLabel();
        code.add("if " + condTmp + " goto " + Lthen);
        // else o salta al end
        // si no hi ha else, children.size()<8
        if (n.getChildren().size() > 7) {
            // té else i cos if dins children[5]
            visit(n.getChildren().get(5));
            code.add("goto " + Lend);
            code.add(Lthen + ":");
            visit(n.getChildren().get(7));
        } else {
            code.add("goto " + Lend);
            code.add(Lthen + ":");
            visit(n.getChildren().get(5));
        }
        code.add(Lend + ":");
    }

    /**
     * Processa terminals: literals i ID
     */
    private void handleToken(Token tok) {
        switch (tok.getType()) {
            case "INT_VALUE", "FLOAT_VALUE", "CHAR_VALUE" -> {
                String t = newTemp();
                code.add(t + " = " + tok.getLexeme());
                stack.push(t);
            }
            case "ID" -> stack.push(tok.getLexeme());
        }
    }

    /**
     * Processa no-terminals: assignacions, operacions, returns
     */
    private void handleNonTerminal(Node n) {
        List<Node> ch = n.getChildren();
        if (ch.isEmpty()) return;
        Token first = ch.get(0).getToken();
        if (first == null) return;
        String ft = first.getType();

        // RETURN
        if ("RETURN".equals(ft) || "XINPUM".equals(ft)) {
            String val = stack.isEmpty() ? null : stack.pop();
            code.add(val == null ? "return" : "return " + val);
            return;
        }
        // Assignació
        if ("EQUAL_ASSIGNATION".equals(ft)) {
            String rhs = stack.pop();
            String lhs = stack.pop();
            code.add(lhs + " = " + rhs);
            stack.push(lhs);
            return;
        }
        // Operacions aritmètiques
        if (isOperator(ft)) {
            String right = stack.pop();
            String left = stack.pop();
            String tmp = newTemp();
            String op = map(ft);
            code.add(tmp + " = " + left + " " + op + " " + right);
            stack.push(tmp);
        }
    }

    private String newTemp() {
        return "t" + (tempCounter++);
    }

    private String newLabel() {
        return "L" + (labelCounter++);
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
}
