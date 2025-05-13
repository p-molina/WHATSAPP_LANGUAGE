package MIPS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;

public class MIPSGenerator {

    private final String tacFilePath;
    private final String mipsFilePath;

    public MIPSGenerator(String tacFilePath, String mipsFilePath) {
        this.tacFilePath = tacFilePath;
        this.mipsFilePath = mipsFilePath;

        File mipsFile = new File(mipsFilePath);
        if (!mipsFile.exists()) {
            try {
                mipsFile.createNewFile();
            } catch (Exception e) {
                System.err.println("Error creating MIPS file: " + e.getMessage());
            }
        }
    }

    public void generate() {
        try (BufferedReader br = new BufferedReader(new java.io.FileReader(tacFilePath));
             FileWriter writer = new FileWriter(mipsFilePath)) {

            String line;
            while ((line = br.readLine()) != null) {
                String mipsInstruction = convertTacToMips(line);
                writer.write(mipsInstruction + "\n");
                System.out.println("TAC->   " + line);
                System.out.println("MIPS->  " + mipsInstruction);
            }

        } catch (Exception e) {
            System.err.println("Error during MIPS generation: " + e.getMessage());
        }
    }

    private String convertTacToMips(String line) {
        line = line.trim();
        if (line.isEmpty()) return "";

        if (line.endsWith(":")) return line;

        if (line.startsWith("goto ")) {
            String label = line.substring(5).trim();
            return "j " + label;
        }

        if (line.startsWith("if ")) {
            String[] parts = line.split("\\s+");
            if (parts.length == 6 && "goto".equals(parts[4])) {
                String left = parts[1];
                String op = parts[2];
                String right = parts[3];
                String label = parts[5];

                String instr = switch (op) {
                    case "==" -> "beq";
                    case "!=" -> "bne";
                    case "<"  -> "blt";
                    case ">"  -> "bgt";
                    case "<=" -> "ble";
                    case ">=" -> "bge";
                    default -> "# Operador condicional no suportat: " + op;
                };

                return instr + " $" + left + ", $" + right + ", " + label;
            }
        }

        if (line.startsWith("call ")) {
            String funcName = line.substring(5).trim();
            return "jal " + funcName;
        }

        if (line.startsWith("return ")) {
            String[] parts = line.split("\\s+");
            if (parts.length == 2) {
                String value = parts[1];
                return "move $v0, $" + value + "\njr $ra";
            }
        }

        // Lectura d'array: t1 = x[2]
        if (line.matches("^[a-zA-Z0-9_]+ = [a-zA-Z0-9_]+\\[[0-9]+\\]$")) {
            String[] parts = line.split(" = ");
            String dest = parts[0];
            String arrayAccess = parts[1];
            String array = arrayAccess.substring(0, arrayAccess.indexOf("["));
            String index = arrayAccess.substring(arrayAccess.indexOf("[") + 1, arrayAccess.indexOf("]"));

            String offsetReg = "$t9";
            String addrReg = "$t8";

            return String.join("\n",
                    "li " + offsetReg + ", " + index,
                    "sll " + offsetReg + ", " + offsetReg + ", 2",
                    "la " + addrReg + ", " + array,
                    "add " + addrReg + ", " + addrReg + ", " + offsetReg,
                    "lw $" + dest + ", 0(" + addrReg + ")"
            );
        }

        // Escriptura d'array: x[2] = t1
        if (line.matches("^[a-zA-Z0-9_]+\\[[0-9]+\\] = [a-zA-Z0-9_]+$")) {
            String[] parts = line.split(" = ");
            String arrayAccess = parts[0];
            String value = parts[1];
            String array = arrayAccess.substring(0, arrayAccess.indexOf("["));
            String index = arrayAccess.substring(arrayAccess.indexOf("[") + 1, arrayAccess.indexOf("]"));

            String offsetReg = "$t9";
            String addrReg = "$t8";

            return String.join("\n",
                    "li " + offsetReg + ", " + index,
                    "sll " + offsetReg + ", " + offsetReg + ", 2",
                    "la " + addrReg + ", " + array,
                    "add " + addrReg + ", " + addrReg + ", " + offsetReg,
                    "sw $" + value + ", 0(" + addrReg + ")"
            );
        }

        // Operació aritmètica
        if (line.matches("^[a-zA-Z0-9_]+ = [a-zA-Z0-9_]+ [\\+\\-\\*/] [a-zA-Z0-9_]+$")) {
            String[] parts = line.split("\\s+");
            String dest = parts[0];
            String left = parts[2];
            String op = parts[3];
            String right = parts[4];

            String opInstr = switch (op) {
                case "+" -> "add";
                case "-" -> "sub";
                case "*" -> "mul";
                case "/" -> "div";
                default -> "# Operador aritmètic no suportat: " + op;
            };

            return opInstr + " $" + dest + ", $" + left + ", $" + right;
        }

        // Assignacions literals / moviments
        if (line.matches("^[a-zA-Z0-9_]+ = .+$")) {
            String[] parts = line.split("\\s+");
            if (parts.length < 3) return "# Malformació";
            String dest = parts[0];
            String src = parts[2];

            if (src.equals("$v0")) {
                return "move $" + dest + ", $v0";
            }

            if (src.matches("\\d+(\\.\\d+)?")) {
                return "li $" + dest + ", " + src;
            }

            if (src.matches("'.'")) {
                int ascii = (int) src.charAt(1);
                return "li $" + dest + ", " + ascii;
            }

            return "move $" + dest + ", $" + src;
        }

        return "# No reconegut: " + line;
    }
}
