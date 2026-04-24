import java.nio.file.*;
import java.util.*;

public class Lexer {

    // token types
    enum TokenType {
        ID, CT_INT, CT_REAL, CT_CHAR, CT_STRING,

        BREAK, CHAR, DOUBLE, ELSE, FOR, IF, INT, RETURN, STRUCT, VOID, WHILE,

        COMMA, SEMICOLON, LPAR, RPAR, LBRACKET, RBRACKET, LACC, RACC,

        ADD, SUB, MUL, DIV, DOT,
        AND, OR, NOT,
        ASSIGN, EQUAL, NOTEQ,
        LESS, LESSEQ, GREATER, GREATEREQ,

        END
    }

    // the token
    static class Token {
        TokenType code;
        String text;
        Double realVal;
        Long intVal;
        int line;

        Token(TokenType code, int line) {
            this.code = code;
            this.line = line;
        }

        public String toString() {
            String attr = "";

            if (text != null) attr = " (" + text + ")";
            else if (intVal != null) attr = " (" + intVal + ")";
            else if (realVal != null) attr = " (" + realVal + ")";

            return code + attr + " [line " + line + "]";
        }
    }

    // lexer
    private String input;
    private int pos = 0;
    private int line = 1;

    private List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("break", TokenType.BREAK);
        keywords.put("char", TokenType.CHAR);
        keywords.put("double", TokenType.DOUBLE);
        keywords.put("else", TokenType.ELSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("if", TokenType.IF);
        keywords.put("int", TokenType.INT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("struct", TokenType.STRUCT);
        keywords.put("void", TokenType.VOID);
        keywords.put("while", TokenType.WHILE);
    }

    public Lexer(String input) {
        this.input = input + "\0";
    }

    private char current() {
        return input.charAt(pos);
    }

    private char next() {
        pos++;
        return current();
    }

    private char peek() {
        if (pos + 1 >= input.length()) return '\0';
        return input.charAt(pos + 1);
    }

    private Token add(TokenType type) {
        Token t = new Token(type, line);
        tokens.add(t);
        return t;
    }

    //FSM
    public List<Token> tokenize() {

        int state = 0;
        int start = pos;

        while (true) {
            char ch = current();

            switch (state) {

                // initale state
                case 0:
                    if (ch == '\0') {
                        add(TokenType.END);
                        return tokens;
                    }

                    if (ch == ' ' || ch == '\t' || ch == '\r') {
                        next();
                    } else if (ch == '\n') {
                        line++;
                        next();
                    }

                    // id
                    else if (Character.isLetter(ch) || ch == '_') {
                        start = pos;
                        next();
                        state = 1;
                    }

                    // number
                    else if (Character.isDigit(ch)) {
                        start = pos;
                        next();
                        state = 3;
                    }

                    // STRING
                    else if (ch == '"') {
                        next();
                        start = pos;
                        state = 10;
                    }

                    // CHAR
                    else if (ch == '\'') {
                        next();
                        state = 20;
                    }

                    // COMMENT //
                    else if (ch == '/' && peek() == '/') {
                        while (ch != '\n' && ch != '\0') ch = next();
                    }

                    // OPERATORS
                    else if (ch == '=') {
                        next();
                        state = 30;
                    } else if (ch == '!') {
                        next();
                        state = 33;
                    } else if (ch == '<') {
                        next();
                        state = 36;
                    } else if (ch == '>') {
                        next();
                        state = 39;
                    } else if (ch == '&') {
                        next();
                        state = 42;
                    } else if (ch == '|') {
                        next();
                        state = 45;
                    }

                    // SIMPLE TOKENS
                    else {
                        switch (ch) {
                            case '+': add(TokenType.ADD); break;
                            case '-': add(TokenType.SUB); break;
                            case '*': add(TokenType.MUL); break;
                            case '/': add(TokenType.DIV); break;
                            case '.': add(TokenType.DOT); break;
                            case ',': add(TokenType.COMMA); break;
                            case ';': add(TokenType.SEMICOLON); break;
                            case '(': add(TokenType.LPAR); break;
                            case ')': add(TokenType.RPAR); break;
                            case '[': add(TokenType.LBRACKET); break;
                            case ']': add(TokenType.RBRACKET); break;
                            case '{': add(TokenType.LACC); break;
                            case '}': add(TokenType.RACC); break;
                            default: error(ch);
                        }
                        next();
                    }
                    break;

                // IDENTIFIER
                case 1:
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        next();
                    } else {
                        String word = input.substring(start, pos);
                        TokenType type = keywords.getOrDefault(word, TokenType.ID);

                        Token tk = add(type);
                        if (type == TokenType.ID) tk.text = word;

                        state = 0;
                    }
                    break;

                // INTEGER / REAL
                case 3:
                    if (Character.isDigit(ch)) {
                        next();
                    } else if (ch == '.') {
                        next();
                        state = 4;
                    } else {
                        String num = input.substring(start, pos);
                        Token tk = add(TokenType.CT_INT);
                        tk.intVal = Long.parseLong(num);
                        state = 0;
                    }
                    break;

                case 4:
                    if (Character.isDigit(ch)) {
                        next();
                    } else {
                        String num = input.substring(start, pos);
                        Token tk = add(TokenType.CT_REAL);
                        tk.realVal = Double.parseDouble(num);
                        state = 0;
                    }
                    break;

                // STRING
                case 10:
                    if (ch == '"') {
                        String str = input.substring(start, pos);
                        Token tk = add(TokenType.CT_STRING);
                        tk.text = str;
                        next();
                        state = 0;
                    } else if (ch == '\0') {
                        throw new RuntimeException("Unclosed string at line " + line);
                    } else {
                        if (ch == '\n') line++;
                        next();
                    }
                    break;

                // CHAR
                case 20:
                    char val = ch;
                    next();
                    if (current() != '\'') error(current());
                    next();

                    Token tk = add(TokenType.CT_CHAR);
                    tk.text = "'" + val + "'";

                    state = 0;
                    break;

                // =
                case 30:
                    if (ch == '=') {
                        next();
                        add(TokenType.EQUAL);
                    } else {
                        add(TokenType.ASSIGN);
                    }
                    state = 0;
                    break;

                // !
                case 33:
                    if (ch == '=') {
                        next();
                        add(TokenType.NOTEQ);
                    } else {
                        add(TokenType.NOT);
                    }
                    state = 0;
                    break;

                // <
                case 36:
                    if (ch == '=') {
                        next();
                        add(TokenType.LESSEQ);
                    } else {
                        add(TokenType.LESS);
                    }
                    state = 0;
                    break;

                // >
                case 39:
                    if (ch == '=') {
                        next();
                        add(TokenType.GREATEREQ);
                    } else {
                        add(TokenType.GREATER);
                    }
                    state = 0;
                    break;

                // &&
                case 42:
                    if (ch == '&') {
                        next();
                        add(TokenType.AND);
                    } else error(ch);
                    state = 0;
                    break;

                // ||
                case 45:
                    if (ch == '|') {
                        next();
                        add(TokenType.OR);
                    } else error(ch);
                    state = 0;
                    break;
            }
        }
    }

    private void error(char ch) {
        throw new RuntimeException("Invalid character '" + ch + "' at line " + line);
    }

    // MAIN
    public static void main(String[] args) {
        try {
            String fileName = "/Users/eyabenrhouma/Desktop/CT/tests/5.c";

            String content = Files.readString(Path.of(fileName));

            Lexer lexer = new Lexer(content);
            List<Token> tokens = lexer.tokenize();

            System.out.println("===== TOKENS =====");
            for (Token t : tokens) {
                System.out.println(t);
            }
            System.out.println("===== END =====");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }
}