package TAC;

import entities.Node;
import entities.Token;

import java.util.*;

/**
 * Three‑Address‑Code (TAC) generator for WhatsApp‑Lang (v2 ‑ arrays fixes)
 * -----------------------------------------------------------------------
 * Cobertura:
 *   • Literals, identificadors
 *   • Operadors binaris + − * /
 *   • Assignacions "ID -> expr"
 *   • <b>Arrays unidimensionals (declaració i llista d'inicialització)</b>
 *
 * ---- Notes sobre l'AST ----
 *   textaco de 3 num arrr jajaj 1 i 2 i 3 jejej
 *   └─ token sequence: ARRAY DE INT_VALUE TIPUS_BASE ID OPEN_CLAUDATOR ...
 *   El token «ARRAY» (type="ARRAY") sempre apareix només en declaracions
 *   d'array; les claus «jajaj / jejej» també s'utilitzen per a cossos de
 *   funció, així que <i>no podem</i> confiar només en OPEN/CLOSE_CLAUDATOR.
 *   Solució: fer-ho en 3 fases sincronitzades amb els tokens ARRAY, INT_VALUE
 *   (mida) i ID (nom).
 */
public class TACGenerator {

    /* ---------- state ---------- */
    private final List<String> code = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();      // eval‑stack

    private int tempCounter = 0;

    /* ------------ array helpers ------------ */
    private boolean awaitingArraySize = false;   // hem vist "ARRAY", esperem INT_VALUE
    private Integer pendingArraySize = null;     // mida recollida

    // quan tenim mida i ID → creem un context i esperem la llista de valors
    private static class ArrayCtx {
        final String name;
        final int declaredSize;            // 0 si no hem trobat INT_VALUE (seguretat)
        final List<String> values = new ArrayList<>();
        ArrayCtx(String n, int sz) { name = n; declaredSize = sz; }
    }
    private ArrayCtx currentArray = null;   // context d'array que s'està inicialitzant

    /* ---------- public API ---------- */
    private Node defaultRoot;
    public TACGenerator() {}
    public TACGenerator(Node root) { this.defaultRoot = root; }

    public List<String> generate() {
        if (defaultRoot == null) throw new IllegalStateException("Root not set");
        return generate(defaultRoot);
    }

    public List<String> generate(Node root) {
        code.clear(); stack.clear(); tempCounter = 0;
        awaitingArraySize = false; pendingArraySize = null; currentArray = null;

        visit(root);
        return List.copyOf(code);
    }

    /* ---------- traversal ---------- */
    private void visit(Node n) {
        // post‑order
        for (Node ch : n.getChildren()) visit(ch);

        Token tok = n.getToken();
        if (tok != null) handleToken(tok);
        else handleNonTerminal(n);
    }

    /* ---------- tokens ---------- */
    private void handleToken(Token tok) {
        String type = tok.getType();

        switch (type) {
            /* ----- array declaration handshake ----- */
            case "ARRAY" -> awaitingArraySize = true;   // next INT_VALUE will be size
            case "INT_VALUE" -> {
                if (awaitingArraySize && pendingArraySize == null) {
                    pendingArraySize = Integer.parseInt(tok.getLexeme());
                    // no stack push; size handled at ID step
                    return;
                }
                // literal INT (not array‑size context)
                literalToTemp(tok);
            }
            case "FLOAT_VALUE", "CHAR_VALUE" -> literalToTemp(tok);

            /* -------- identifier -------- */
            case "ID" -> {
                // If we have pendingArraySize set, this ID is the array name
                if (pendingArraySize != null) {
                    currentArray = new ArrayCtx(tok.getLexeme(), pendingArraySize);
                    awaitingArraySize = false; pendingArraySize = null;
                    // At this point we haven't allocated yet; we'll wait until
                    // we hit the OPEN_CLAUDATOR that starts INIT_ARRAY.
                } else {
                    stack.push(tok.getLexeme());
                }
            }

            /* ---- claudators for INIT_ARRAY ---- */
            case "OPEN_CLAUDATOR" -> {
                // If just started an array declaration, values will follow
                if (currentArray != null && currentArray.values.isEmpty()) {
                    // nothing special yet
                }
            }
            case "CLOSE_CLAUDATOR" -> {
                if (currentArray != null && !currentArray.values.isEmpty()) {
                    // Close array init → generate alloc + stores
                    generateArrayInit(currentArray);
                    currentArray = null;   // done
                }
            }
            /* ---- operators treated elsewhere ---- */
            case "SUM", "MINUS", "MULTIPLY", "DIVISION", "EQUAL_ASSIGNATION" -> {
                // handled in non‑terminal pattern section after children
            }
            default -> {
                // any other token types ignored for TAC purposes
            }
        }
    }

    private void literalToTemp(Token tok) {
        String tmp = newTemp();
        code.add(tmp + " = " + tok.getLexeme());
        stack.push(tmp);
        if (currentArray != null) {
            currentArray.values.add(tmp);
        }
    }

    /* ---------- non‑terminal patterns ---------- */
    private void handleNonTerminal(Node n) {
        if (n.getChildren().isEmpty()) return;

        Token firstTok = n.getChildren().get(0).getToken();
        if (firstTok == null) return;
        String type = firstTok.getType();

        /* Assignació */
        if ("EQUAL_ASSIGNATION".equals(type)) {
            if (stack.size() < 2) return;
            String rhs = stack.pop();
            String lhs = stack.pop();
            code.add(lhs + " = " + rhs);
            stack.push(lhs);
            return;
        }

        /* Operadors binaris */
        if (isOperator(type)) {
            if (stack.size() < 2) return;
            String right = stack.pop();
            String left  = stack.pop();
            String tmp = newTemp();
            code.add(tmp + " = " + left + ' ' + map(type) + ' ' + right);
            stack.push(tmp);
        }
    }

    /* ---------- utils ---------- */
    private void generateArrayInit(ArrayCtx ctx) {
        // tSize = ctx.values.size();  (use declared size if given >0)
        int size = ctx.declaredSize > 0 ? ctx.declaredSize : ctx.values.size();
        String tSize = newTemp();
        code.add(tSize + " = " + size);
        code.add(ctx.name + " = alloc " + tSize);

        for (int i = 0; i < ctx.values.size(); i++) {
            code.add(ctx.name + "[" + i + "] = " + ctx.values.get(i));
        }
    }

    private static boolean isOperator(String t) {
        return "SUM".equals(t) || "MINUS".equals(t) ||
                "MULTIPLY".equals(t) || "DIVISION".equals(t);
    }

    private static String map(String t) {
        return switch (t) {
            case "SUM"      -> "+";
            case "MINUS"    -> "-";
            case "MULTIPLY" -> "*";
            case "DIVISION" -> "/";
            default -> "?";
        };
    }

    private String newTemp() { return "t" + tempCounter++; }
}
