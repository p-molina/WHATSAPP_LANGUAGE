// src/Testing/Test.java
package Testing;

public class Test {
    private final int id;
    private final String description;
    private final String filePath;

    public Test(int id, String description, String filePath) {
        this.id = id;
        this.description = description;
        this.filePath = filePath;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getFilePath() {
        return filePath;
    }
}
