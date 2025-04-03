package LexicalAnalyzer;

public class Token {
    private String tokenName;
    private TokenType tokenType;
    private Integer lineNumber;

    public Token(String tokenName, Integer attributeValue) {
        this.tokenName = tokenName;
        this.attributeValue = attributeValue;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public Integer getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(Integer attributeValue) {
        this.attributeValue = attributeValue;
    }
}
