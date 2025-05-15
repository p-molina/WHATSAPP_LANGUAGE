package MIPS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class MIPSGeneratorNEW {

    private Map<String, String> varRegisterMap;
    private int registerCounter;
    private int floatRegisterCounter;
    private Map<String, String> floatLabels;
    private FileWriter writer;

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

            // Escriu capçalera de codi MIPS
            writer.write(".text\n.globl main\nmain:\n");

            String line;
            while ((line = br.readLine()) != null) {
                convertTacToMips(line.trim());
            }

            // Escriu constants float a la secció .data
            if (!floatLabels.isEmpty()) {
                writer.write("\n.data\n");
                for (Map.Entry<String, String> entry : floatLabels.entrySet()) {
                    writer.write(entry.getKey() + ": .float " + entry.getValue() + "\n");
                }
            }

        } catch (Exception e) {
            System.err.println("Error during MIPS generation: " + e.getMessage());
        }
    }

    private void convertTacToMips(String line) {
        if (line.isEmpty()) return;

        if (line.endsWith(":")) {
            handleLabel(line);
        } else if (line.startsWith("if ")) {
            handleConditionalJump(line);
        } else if (line.startsWith("goto ")) {
            handleGoto(line);
        } else if (line.startsWith("return ")) {
            handleReturn(line);
        } else if (line.contains("=")) {
            handleAssignment(line);
        }
    }

    private void handleLabel(String line) {
        try {
            writer.write(line + "\n");  // Ex: L0:
        } catch (Exception e) {
            System.err.println("Error in handleLabel: " + e.getMessage());
        }
    }

    private void handleGoto(String line) {
        try {
            // Ex: goto L1
            String[] parts = line.split("\\s+");
            if (parts.length != 2) return;
            String label = parts[1];
            writer.write("j " + label + "\n");
        } catch (Exception e) {
            System.err.println("Error in handleGoto: " + e.getMessage());
        }
    }

    private void handleConditionalJump(String line) {
        try {
            // Ex: if t1 goto L1
            String[] parts = line.split("\\s+");
            if (parts.length != 4) return;

            String conditionVar = parts[1];
            String label = parts[3];
            String reg = getRegister(conditionVar);

            writer.write("bne " + reg + ", $zero, " + label + "\n");
        } catch (Exception e) {
            System.err.println("Error in handleConditionalJump: " + e.getMessage());
        }
    }

    private void handleReturn(String line) {
        try {
            // Ex: return t5
            String[] parts = line.split("\\s+");
            if (parts.length != 2) return;

            String returnVar = parts[1];
            String reg = getRegister(returnVar);

            writer.write("move $v0, " + reg + "\n");
            writer.write("jr $ra\n");
        } catch (Exception e) {
            System.err.println("Error in handleReturn: " + e.getMessage());
        }
    }


    private void handleAssignment(String line) {
        try {
            String[] parts = line.split("=");
            if (parts.length != 2) return;

            String dest = parts[0].trim();
            String value = parts[1].trim();
            String reg = getRegister(dest);

            if (isInteger(value)) {
                writer.write("li " + reg + ", " + value + "\n");
            } else if (isFloat(value)) {
                String label = "flt_" + dest;
                floatLabels.put(label, value);
                String floatReg = "$f" + (floatRegisterCounter % 10);
                floatRegisterCounter++;
                writer.write("lwc1 " + floatReg + ", " + label + "\n");
            }

        } catch (Exception e) {
            System.err.println("Error in handleAssignment: " + e.getMessage());
        }
    }

    private String getRegister(String var) {
        if (!varRegisterMap.containsKey(var)) {
            String reg = "$t" + (registerCounter % 10);
            varRegisterMap.put(var, reg);
            registerCounter++;
        }
        return varRegisterMap.get(var);
    }

    private boolean isInteger(String s) {
        return s.matches("-?\\d+");
    }

    private boolean isFloat(String s) {
        return s.matches("-?\\d+\\.\\d+");
    }
}
