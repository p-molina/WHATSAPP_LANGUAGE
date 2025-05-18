package MIPS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Generador de codi MIPS a partir de TAC (Three-Address Code).
 * <p>Aquesta classe llegeix un fitxer TAC, converteix cada línia
 * al corresponent codi MIPS i escriu el resultat en un fitxer de sortida.</p>
 * <p>Gestiona assignacions, salts, etiquetes, trucades a funcions i retorns,
 * així com l'assignació de registres temporals i de punt flotant.</p>
 */
public class MIPSGenerator {

    /** Mapatge de variables a registres enters ($s0–$s7). */
    private Map<String, String> varRegisterMap;
    /** Comptador per registres enters ($t0, $t1, ...) */
    private int registerCounter;
    /** Comptador per registres de coma flotant ($f0, $f1, ...) */
    private int floatRegisterCounter;
    /** Mapa per assignar registres a variables */
    private Map<String, String> floatLabels;
    /** Escrivent per al fitxer de sortida MIPS. */
    private FileWriter writer;
    /** Conjunt de variables globals (definides fora de funcions). */
    private final Set<String> globalVars = new HashSet<>();
    /** Mapatge de registres assignats per l'allocator intel·ligent. */
    private Map<String, String> assignedRegs;

    /**
     * Construeix un nou generador MIPS inicialitzant mapes i comptadors.
     */
    public MIPSGenerator() {
        varRegisterMap = new HashMap<>();
        floatLabels = new HashMap<>();
        registerCounter = 0;
        floatRegisterCounter = 0;
    }

    /**
     * Genera el codi MIPS a partir d'un fitxer TAC.
     *
     * @param tacFilePath  Ruta al fitxer d'entrada TAC.
     * @param mipsFilePath Ruta al fitxer de sortida MIPS.
     */
    public void generate(String tacFilePath, String mipsFilePath) {
        try {
            // Llegir tot el TAC per detectar variables globals (aquelles abans de la primera etiqueta)
            List<String> tacLines = Files.readAllLines(Paths.get(tacFilePath));
            boolean insideFunction = false;
            for (String line : tacLines) {
                line = line.trim();
                if (line.endsWith(":")) {
                    insideFunction = true;
                    break;
                } else if (line.contains("=")) {
                    String var = line.split("=")[0].trim();
                    if (var.startsWith("t")) {
                        globalVars.add(var);
                    }
                }
            }

            // Filtrar línies que només contenen codi de funcions (no globals)
            List<String> filteredTacLines = new ArrayList<>();
            insideFunction = false;
            for (String line : tacLines) {
                line = line.trim();
                if (line.endsWith(":")) insideFunction = true;
                if (insideFunction) filteredTacLines.add(line);
            }

            // Assignar registres temporals
            SmartRegisterAllocator allocator = new SmartRegisterAllocator();
            this.assignedRegs = allocator.allocate(filteredTacLines);

            // Obrir lector i escriptor de fitxers
            BufferedReader br = new BufferedReader(new FileReader(tacFilePath));
            FileWriter w = new FileWriter(mipsFilePath);
            this.writer = w;

            // Processar cada línia TAC i convertir-la a MIPS
            String line;
            while ((line = br.readLine()) != null) {
                convertTacToMips(line.trim());
            }

            writer.close();
        } catch (Exception e) {
            System.err.println("Error during MIPS generation: " + e.getMessage());
        }
    }

    /**
     * Converteix una única línia TAC al corresponent codi MIPS.
     *
     * @param line Línia de TAC a processar.
     */
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
        } else if (line.contains("call")) {         // ASSIGNATION
            handleCall(line);
        } else if (line.contains("=")) {
            handleAssignment(line);
        }
    }

    /**
     * Gestiona la conversió d'una etiqueta TAC a etiqueta MIPS.
     *
     * @param line Línia TAC que conté l'etiqueta (acaba en ':').
     */
    private void handleLabel(String line) {
        String label = line.substring(0, line.length());
        try {
            writer.write(label + "\n");
        } catch (IOException e) {
            System.err.println("Error writing label: " + e.getMessage());
        }
    }

    /**
     * Gestiona la trucada a funcions TAC i genera codi MIPS corresponent.
     *
     * @param line Línia TAC que comença amb 'call ' seguits del nom de la funció.
     */
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

            // Total de registres a guardar:
            // $t0–$t9 (10), $s0–$s4 (5), $ra (1), $a0 (1) → 17 registres * 4 bytes = 68
            int totalBytes = 17 * 4;

            // Reservar espai
            writer.write("  addiu $sp, $sp, -" + totalBytes + "\n");

            // Guardar $t0–$t9
            for (int i = 0; i <= 9; i++) {
                writer.write("  sw $t" + i + ", " + (i * 4) + "($sp)\n");
            }

            // Guardar $s0–$s4
            for (int i = 0; i <= 4; i++) {
                writer.write("  sw $s" + i + ", " + ((10 + i) * 4) + "($sp)\n");
            }

            // Guardar $ra i $a0
            writer.write("  sw $ra, " + (15 * 4) + "($sp)\n");
            writer.write("  sw $a0, " + (16 * 4) + "($sp)\n");

            // Crida
            writer.write("  # Call function\n");
            writer.write("  jal " + functionName + "\n");

            // Restaurar context
            writer.write("  # Restore context after call\n");

            // Recuperar $t0–$t9
            for (int i = 0; i <= 9; i++) {
                writer.write("  lw $t" + i + ", " + (i * 4) + "($sp)\n");
            }

            // Recuperar $s0–$s4
            for (int i = 0; i <= 4; i++) {
                writer.write("  lw $s" + i + ", " + ((10 + i) * 4) + "($sp)\n");
            }

            // Recuperar $ra i $a0
            writer.write("  lw $ra, " + (15 * 4) + "($sp)\n");
            writer.write("  lw $a0, " + (16 * 4) + "($sp)\n");

            // Alliberar espai
            writer.write("  addiu $sp, $sp, " + totalBytes + "\n");

            // Assignació de retorn (si escau)
            if (resultVar != null) {
                String destReg = getRegister(resultVar);
                writer.write("  move " + destReg + ", $v0\n");
            }

        } catch (IOException e) {
            System.err.println("Error writing function call: " + e.getMessage());
        }
    }

    /**
     * Gestiona la instrucció return TAC i escriu el codi MIPS corresponent.
     */
    private void handleReturn(String line) {
        try {
            String[] parts = line.split(" ");
            if (parts.length == 2 && !parts[1].equals("null")) {
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
            } else {
                // Return null o sense valor explícit
                writer.write("  li $v0, 0\n");
            }
            writer.write("  jr $ra\n");
        } catch (IOException e) {
            System.err.println("Error writing return: " + e.getMessage());
        }
    }

    /**
     * Gestiona salts incondicionals TAC i converteix-los a MIPS.
     *
     * @param line Línia TAC que comença amb 'goto '.
     */
    private void handleGoto(String line) {
        try {
            String label = line.substring(5).trim();
            writer.write("  j " + label + "\n");
        } catch (IOException e) {
            System.err.println("Error writing goto: " + e.getMessage());
        }
    }

    /**
     * Comprova si un registre és de punt flotant.
     *
     * @param reg Nom del registre.
     * @return true si és un registre de punt flotant, false altrament.
     */
    private boolean isFloatRegister(String reg) {
        return reg.startsWith("$f");
    }

    /**
     * Gestiona salts condicionals TAC i genera codi MIPS equivalent.
     *
     * @param line Línia TAC que conté 'if'.
     */
    private void handleConditionalJump(String line) {
        try {
            String[] parts = line.replace("if", "").trim().split("goto");
            String condVar = parts[0].trim();
            String label = parts[1].trim();
            String reg = getRegister(condVar);
            writer.write("  bne " + reg + ", $zero, " + label + "\n");
        } catch (IOException e) {
            System.err.println("Error writing conditional jump: " + e.getMessage());
        }
    }

    /**
     * Gestiona assignacions TAC i genera les instruccions MIPS corresponents.
     *
     * @param line Línia TAC amb un '='.
     */
    private void handleAssignment(String line) {
        try {
            String[] parts = line.split("=");
            String left = parts[0].trim();
            String expr = parts[1].trim();
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

                boolean isFloatOp = isFloat(op1) || isFloat(op2) ||
                        isFloatRegister(getRegister(op1)) || isFloatRegister(getRegister(op2));

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
                        case "/":
                            writer.write("  div " + r1 + ", " + r2 + "\n");
                            writer.write("  mflo " + destReg + "\n");
                            break;
                        case "<":
                            writer.write("  slt " + destReg + ", " + r1 + ", " + r2 + "\n");
                            break;
                        case ">":
                            writer.write("  slt " + destReg + ", " + r2 + ", " + r1 + "\n");
                            break;
                        case "<=":
                            writer.write("  slt $at, " + r2 + ", " + r1 + "\n");
                            writer.write("  xori " + destReg + ", $at, 1\n");
                            break;
                        case ">=":
                            writer.write("  slt $at, " + r1 + ", " + r2 + "\n");
                            writer.write("  xori " + destReg + ", $at, 1\n");
                            break;
                        case "==":
                            writer.write("  xor $at, " + r1 + ", " + r2 + "\n");
                            writer.write("  sltiu " + destReg + ", $at, 1\n");
                            break;
                        case "!=":
                            writer.write("  xor $at, " + r1 + ", " + r2 + "\n");
                            writer.write("  sltu " + destReg + ", $zero, $at\n");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error writing assignment: " + e.getMessage());
        }
    }

    /**
     * Obté o assigna un registre temporal per a una variable TAC.
     *
     * @param var Nom de variable TAC.
     * @return Nom del registre MIPS assignat.
     */
    private String getRegister(String var) {
        if (globalVars.contains(var)) {
            if (!varRegisterMap.containsKey(var)) {
                String reg = "$s" + (registerCounter % 8);
                varRegisterMap.put(var, reg);
                registerCounter++;
            }
            return varRegisterMap.get(var);
        }
        return assignedRegs.getOrDefault(var, "$zero");
    }

    /**
     * Obté o assigna un registre de punt flotant per a una variable TAC.
     *
     * @param var Nom de variable TAC.
     * @return Registre flotant assignat.
     */
    private String getFloatRegister(String var) {
        if (!floatLabels.containsKey(var)) {
            String freg = "$f" + (floatRegisterCounter % 10);
            floatLabels.put(var, freg);
            floatRegisterCounter++;
        }
        return floatLabels.get(var);
    }

    /**
     * Comprova si una cadena representa un enter.
     *
     * @param s Cadena a comprovar.
     * @return true si la cadena és un enter, false altrament.
     */
    private boolean isInteger(String s) {
        return s.matches("-?\\d+");
    }

    /**
     * Comprova si una cadena representa un nombre de punt flotant.
     *
     * @param s Cadena a comprovar.
     * @return true si és un flotant, false altrament.
     */
    private boolean isFloat(String s) {
        return s.matches("-?\\d+\\.\\d+");
    }

    /**
     * Comprova si una cadena representa un caràcter ASCII com a enter.
     *
     * @param s Cadena a comprovar.
     * @return true si s'ajusta al patró de caràcter, false altrament.
     */
    private boolean isChar(String s) {
        return s.matches("-?\\d+");
    }
}
