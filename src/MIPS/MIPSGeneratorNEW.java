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
            String line;
            while ((line = br.readLine()) != null) {
                convertTacToMips(line.trim());
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
        } else if (line.contains("=")) {            // ASSIGNATION
            handleAssignment(line);
        } else if (line.contains("call")) {
            // potser aqui controlar el call de la funcio en una assignacio, no se
        }
    }

    private void handleLabel(String line) {
        // TODO
    }

    private void handleGoto(String line) {
        // TODO
    }

    private void handleConditionalJump(String line) {
        // TODO
    }

    private void handleReturn(String line) {
        // TODO
    }


    private void handleAssignment(String line) {
        // TODO
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

    private boolean isChar(String s) {
        return s.matches("-?\\d+");
    }
}
