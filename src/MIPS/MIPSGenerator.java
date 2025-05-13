package MIPS;

import java.io.BufferedReader;
import java.io.File;

public class MIPSGenerator {

    private final String tacFilePath;
    private final String mipsFilePath;

    public MIPSGenerator(String tacFilePath, String mipsFilePath) {
        this.tacFilePath = tacFilePath;
        this.mipsFilePath = mipsFilePath;

        File mipsFile = new File(mipsFilePath);
        if (!mipsFile.exists()) {
            try { mipsFile.createNewFile(); }
            catch (Exception e) { System.err.println("Error creating MIPS file: " + e.getMessage()); }
        }
    }

    public void generate() {
        try (BufferedReader br = new BufferedReader(new java.io.FileReader(tacFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String mipsInstruction = convertTacToMips(line);
                writeMipsInstruction(mipsInstruction);
                System.out.println("TAC->   " + line);
                System.out.println("MIPS->  " + mipsInstruction);
            }
        } catch (Exception e) {
            System.err.println("Error reading TAC file: " + e.getMessage());
        }
    }

    private void writeMipsInstruction(String mipsInstruction) {
        try (java.io.FileWriter writer = new java.io.FileWriter(mipsFilePath, true)) {
            writer.write(mipsInstruction + "\n");
        } catch (Exception e) {
            System.err.println("Error writing MIPS file: " + e.getMessage());
        }
    }

    private String convertTacToMips(String line) {
        StringBuilder mipsInstruction = new StringBuilder();


        // Hi haura un TAC amb la següent estructura:
        // <result> = <operand1> <operator> <operand2>
        String[] parts = line.split(" ");







        return "";
    }

}
