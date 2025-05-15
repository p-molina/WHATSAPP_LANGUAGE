package MIPS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class MIPSGeneratorNEW {

    public MIPSGeneratorNEW() { }

    public void generate(String tacFilePath, String mipsFilePath) {

        try (BufferedReader br = new BufferedReader(new FileReader(tacFilePath));
             FileWriter writer = new FileWriter(mipsFilePath)) {

            String line;
            while ((line = br.readLine()) != null) {
                String mipsInstruction = convertTacToMips(line);
                writer.write(mipsInstruction + "\n");
            }

        } catch (Exception e) {
            System.err.println("Error during MIPS generation: " + e.getMessage());
        }
    }

    private String convertTacToMips(String line) {
        line = line.trim();

        return line;
    }
}
