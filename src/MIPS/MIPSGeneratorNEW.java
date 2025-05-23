package MIPS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MIPSGeneratorNEW {

    private Map<String, String> varRegisterMap;
    private int registerCounter;
    private int floatRegisterCounter;
    private Map<String, String> floatLabels;
    private FileWriter writer;

    private final Set<String> globalVars = new HashSet<>();

    public MIPSGeneratorNEW() {
        varRegisterMap = new HashMap<>();
        floatLabels = new HashMap<>();
        registerCounter = 0;
        floatRegisterCounter = 0;
    }

    public void generate(String tacFilePath, String mipsFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(tacFilePath));
             FileWriter w = new FileWriter(mipsFilePath)) {

            this.writer = w;
            String line;
            boolean insideFunction = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.endsWith(":")) {
                    insideFunction = true;
                } else if (!insideFunction && line.contains("=")) {
                    String var = line.split("=")[0].trim();
                    globalVars.add(var);
                }
                convertTacToMips(line);
            }

        } catch (Exception e) {
            System.err.println("Error during MIPS generation: " + e.getMessage());
        }
    }

    private void convertTacToMips(String line) {
        if (line.isEmpty()) return;

        if (line.endsWith(":")) {                   // FUNCTION
            handleLabel(line);
        } else if (line.startsWith("if ")) {        // CONDITIONAL
            handleConditionalJump(line);
        } else if (line.startsWith("goto ")) {      // JUMP TO LABEL
            handleGoto(line);
        } else if (line.startsWith("return ")) {    // RETURN VALUE
            handleReturn(line);
        } else if (line.contains("call")) {            // ASSIGNATION
            handleCall(line);
        } else if (line.contains("=")) {
            handleAssignment(line);
            // potser aqui controlar el call de la funcio en una assignacio, no se
        }
    }

    private void handleLabel(String line) {
        String label = line.substring(0, line.length());
        try {
            writer.write(label + "\n");  // només l’etiqueta
        } catch (IOException e) {
            System.err.println("Error writing label: " + e.getMessage());
        }
    }

    private void handleCall(String line) {
        try {
            String[] parts = line.split("=");
            String resultVar = null;
            String functionName;

            if (parts.length == 2) {
                resultVar = parts[0].trim();
                functionName = parts[1].replace("call", "").trim();
            } else {
                functionName = line.replace("call", "").trim();
            }

            writer.write("  # Save context before calling " + functionName + "\n");

            int regCount = 0;

            // Guardem $t0–$t9
            for (int i = 0; i <= 9; i++) {
                writer.write("  sw $t" + i + ", -" + (4 * (regCount + 1)) + "($sp)\n");
                regCount++;
            }

            // Guardem $ra
            writer.write("  sw $ra, -" + (4 * (regCount + 1)) + "($sp)\n");
            regCount++;

            // Guardem $a0 per si l'hem utilitzat
            writer.write("  sw $a0, -" + (4 * (regCount + 1)) + "($sp)\n");
            regCount++;

            // Reservem espai de cop
            writer.write("  addiu $sp, $sp, -" + (4 * regCount) + "\n");

            writer.write("  # Call function\n");
            writer.write("  jal " + functionName + "\n");

            writer.write("  # Restore context after call\n");

            // Recuperem $a0, $ra, $t0–$t9 (en ordre invers)
            int offset = 0;
            writer.write("  lw $a0, " + offset + "($sp)\n"); offset += 4;
            writer.write("  lw $ra, " + offset + "($sp)\n"); offset += 4;

            for (int i = 9; i >= 0; i--) {
                writer.write("  lw $t" + i + ", " + offset + "($sp)\n");
                offset += 4;
            }

            // Pugem $sp
            writer.write("  addiu $sp, $sp, " + (4 * regCount) + "\n");

            // Assignem el resultat
            if (resultVar != null) {
                String destReg = getRegister(resultVar);
                writer.write("  move " + destReg + ", $v0\n");
            }

        } catch (IOException e) {
            System.err.println("Error writing function call: " + e.getMessage());
        }
    }

    private void handleReturn(String line) {
        try {
            String[] parts = line.split(" ");
            if (parts.length == 2) {
                String value = parts[1];

                if (isFloat(value)) {
                    writer.write("  li.s $f0, " + value + "\n");
                } else if (isInteger(value)) {
                    writer.write("  li $v0, " + value + "\n");
                } else {
                    String reg = getRegister(value);
                    if (isFloatRegister(reg)) {
                        writer.write("  mov.s $f0, " + reg + "\n");
                    } else {
                        writer.write("  move $v0, " + reg + "\n");
                    }
                }
            }

            writer.write("  jr $ra\n");

        } catch (IOException e) {
            System.err.println("Error writing return: " + e.getMessage());
        }
    }

    private void handleGoto(String line) {
        try {
            // Ex: "goto etiqueta"
            String label = line.substring(5).trim();  // treu "goto "
            writer.write("  j " + label + "\n");
        } catch (IOException e) {
            System.err.println("Error writing goto: " + e.getMessage());
        }
    }


    private boolean isFloatRegister(String reg) {
        return reg.startsWith("$f");
    }


    private void handleConditionalJump(String line) {
        // TODO
    }

    private void handleAssignment(String line) {
        try {
            String[] parts = line.split("=");
            String left = parts[0].trim();   // t0
            String expr = parts[1].trim();   // pot ser 5, t1, t1 + t2, etc.

            String[] tokens = expr.split(" ");
            String destReg = getRegister(left);

            if (tokens.length == 1) {
                String value = tokens[0];

                if (isInteger(value)) {
                    writer.write("  li " + destReg + ", " + value + "\n");
                } else if (isFloat(value)) {
                    String freg = getFloatRegister(left);
                    writer.write("  li.s " + freg + ", " + value + "\n");
                } else {
                    String srcReg = getRegister(value);
                    writer.write("  move " + destReg + ", " + srcReg + "\n");
                }

            } else if (tokens.length == 3) {
                String op1 = tokens[0];
                String operator = tokens[1];
                String op2 = tokens[2];

                boolean isFloatOp = isFloat(op1) || isFloat(op2) || isFloatRegister(getRegister(op1)) || isFloatRegister(getRegister(op2));

                if (isFloatOp) {
                    String fregDest = getFloatRegister(left);
                    String freg1 = getFloatRegister(op1);
                    String freg2 = getFloatRegister(op2);

                    switch (operator) {
                        case "+": writer.write("  add.s " + fregDest + ", " + freg1 + ", " + freg2 + "\n"); break;
                        case "-": writer.write("  sub.s " + fregDest + ", " + freg1 + ", " + freg2 + "\n"); break;
                        case "*": writer.write("  mul.s " + fregDest + ", " + freg1 + ", " + freg2 + "\n"); break;
                        case "/": writer.write("  div.s " + fregDest + ", " + freg1 + ", " + freg2 + "\n"); break;
                    }

                } else {
                    String r1 = getRegister(op1);
                    String r2 = getRegister(op2);

                    switch (operator) {
                        case "+": writer.write("  add " + destReg + ", " + r1 + ", " + r2 + "\n"); break;
                        case "-": writer.write("  sub " + destReg + ", " + r1 + ", " + r2 + "\n"); break;
                        case "*": writer.write("  mul " + destReg + ", " + r1 + ", " + r2 + "\n"); break;
                        case "/": writer.write("  div " + r1 + ", " + r2 + "\n");  // resultat queda a lo/hi
                            writer.write("  mflo " + destReg + "\n"); break;
                    }
                }

            }

        } catch (IOException e) {
            System.err.println("Error writing assignment: " + e.getMessage());
        }
    }


    private String getRegister(String var) {
        if (!varRegisterMap.containsKey(var)) {
            if (globalVars.contains(var)) {
                String reg = "$s" + (registerCounter % 8);  // globals → $s0-$s7
                varRegisterMap.put(var, reg);
            } else {
                String reg = "$t" + (registerCounter % 10);
                varRegisterMap.put(var, reg);
            }
            registerCounter++;
        }
        return varRegisterMap.get(var);
    }

    private String getFloatRegister(String var) {
        if (!floatLabels.containsKey(var)) {
            String freg = "$f" + (floatRegisterCounter % 10);
            floatLabels.put(var, freg);
            floatRegisterCounter++;
        }
        return floatLabels.get(var);
    }



    private boolean isInteger(String s) {
        return s.matches("-?\\d+");
    }

    private boolean isFloat(String s) {
        return s.matches("-?\\d+\\.\\d+");
    }

    private boolean isChar(String s) {
        return s.matches("-?\\d+");
    }
}
