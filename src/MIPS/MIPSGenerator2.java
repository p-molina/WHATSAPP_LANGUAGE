package MIPS;

import java.io.*;
import java.util.*;

public class MIPSGenerator2 {

    private static final String[] TEMP_REGS = {
            "$t0","$t1","$t2","$t3","$t4",
            "$t5","$t6","$t7","$t8","$t9"
    };

    private final String tacFilePath;
    private final String mipsFilePath;
    private final Map<String,String> regMap = new LinkedHashMap<>();
    private int nextReg = 0;

    public MIPSGenerator2(String tacFilePath, String mipsFilePath) {
        this.tacFilePath = tacFilePath;
        this.mipsFilePath = mipsFilePath;

        /* Esborrar fitxer previ perquè no es vagi encadenant */
        try { new PrintWriter(mipsFilePath).close(); }
        catch (IOException e) { System.err.println("Error reseting MIPS file: " + e.getMessage()); }
    }

    public void generate() {
        try (BufferedReader br = new BufferedReader(new FileReader(tacFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String mips = convertTacToMips(line);
                writeMipsInstruction(mips);
                System.out.println("TAC  → " + line);
                System.out.println("MIPS → " + mips + "\n");
            }
        } catch (Exception e) {
            System.err.println("Error reading TAC file: " + e.getMessage());
        }
    }

    /* ---------- PART PRIVADA ---------- */

    private void writeMipsInstruction(String mipsInstruction) throws IOException {
        try (FileWriter fw = new FileWriter(mipsFilePath, true)) {
            fw.write(mipsInstruction + "\n");
        }
    }

    private String getReg(String var) {
        if (isNumeric(var)) {
            // immediats: carrega en un registre lliure temporal
            String r = freshTemp();
            return "li " + r + ", " + var + "\n\t" + r;
        }
        return regMap.computeIfAbsent(var, v -> freshTemp());
    }

    private String freshTemp() {
        if (nextReg >= TEMP_REGS.length)
            throw new RuntimeException("Exhaurits registres temporals! (implementa spilling)");
        return TEMP_REGS[nextReg++];
    }

    private static boolean isNumeric(String s) { return s.matches("-?\\d+"); }

    /**
     * Traducció TAC → MIPS molt simplificada
     */
    private String convertTacToMips(String line) {

        /* ---------- Etiquetes ---------- */
        if (line.endsWith(":")) {
            // L1:
            return line;
        }

        /* ---------- GOTO ---------- */
        if (line.startsWith("goto ")) {
            String target = line.substring(5).trim();
            return "j " + target;
        }

        /* ---------- IF cond GOTO ---------- */
        if (line.startsWith("if ")) {
            // Format: if x goto L1
            String[] p = line.split("\\s+");
            if (p.length == 4 && "goto".equals(p[2])) {
                String cond   = p[1];
                String target = p[3];

                String regOrLoad = getReg(cond);
                StringBuilder sb = new StringBuilder();
                // Si és immediat, regOrLoad conté 'li $tx, imm\n\t$tx'
                if (regOrLoad.contains("\n")) {
                    int idx = regOrLoad.lastIndexOf('\n');
                    sb.append(regOrLoad, 0, idx+1);  // la 'li'
                    regOrLoad = regOrLoad.substring(idx+1); // el registre
                }
                sb.append("bne ").append(regOrLoad).append(", $zero, ").append(target);
                return sb.toString();
            }
        }

        /* ---------- Assignacions & Operacions ---------- */
        // <res> = <op1> <op> <op2>
        // o bé <res> = <single>
        int eq = line.indexOf('=');
        if (eq == -1) throw new RuntimeException("Línia TAC desconeguda: " + line);

        String res = line.substring(0, eq).trim();
        String rhs = line.substring(eq+1).trim();

        StringBuilder out = new StringBuilder();
        String destReg = getReg(res);

        if (rhs.matches("[^\\s]+ [\\+\\-\\*/] [^\\s]+")) {
            String[] p = rhs.split("\\s+");
            String op1 = p[0], op = p[1], op2 = p[2];

            String r1 = getReg(op1);
            String r2 = getReg(op2);

            // Si r1 o r2 contenien un "li ...", cal afegir-lo abans
            extractLi(out, r1);
            extractLi(out, r2);

            r1 = lastToken(r1);
            r2 = lastToken(r2);

            switch (op) {
                case "+": out.append("add ").append(destReg).append(", ").append(r1).append(", ").append(r2); break;
                case "-": out.append("sub ").append(destReg).append(", ").append(r1).append(", ").append(r2); break;
                case "*": out.append("mul ").append(destReg).append(", ").append(r1).append(", ").append(r2); break;
                case "/":
                    out.append("div ").append(r1).append(", ").append(r2).append("\n\t");
                    out.append("mflo ").append(destReg);
                    break;
                default:  throw new RuntimeException("Operador no implementat: " + op);
            }
        } else {
            // <res> = <rhs> (mov immediat o entre registres)
            String regOrLoad = getReg(rhs);
            extractLi(out, regOrLoad);
            out.append("move ").append(destReg).append(", ").append(lastToken(regOrLoad));
        }
        return out.toString();
    }

    /* -- utilitats -- */
    private static void extractLi(StringBuilder out, String regOrLoad) {
        int idx = regOrLoad.indexOf('\n');
        if (idx != -1) { // contingut "li $tX,imm\n\t$tX"
            out.append(regOrLoad, 0, idx+1);
        }
    }
    private static String lastToken(String s) {
        int idx = s.lastIndexOf(' ');
        return idx == -1 ? s : s.substring(idx+1);
    }

    /* ---------- MAIN de prova ràpida ---------- */
    public static void main(String[] args) {
        new MIPSGenerator("test.tac", "out.s").generate();
    }
}
