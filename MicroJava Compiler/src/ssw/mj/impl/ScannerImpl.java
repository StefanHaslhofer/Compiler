package ssw.mj.impl;

import ssw.mj.Scanner;
import ssw.mj.Token;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;

public final class ScannerImpl extends Scanner {

    private final Map<String, Token.Kind> keyWords;
    private static final int ASCII_ZERO = 48; // integer value of '0' is 48
    private static final int MAX_INT_WITHOUT_LAST_POINT = 2147483640;

    public ScannerImpl(Reader r) {
        super(r);
        line = 1;
        col = 0;
        keyWords = new HashMap<>();
        keyWords.put("break", break_);
        keyWords.put("class", class_);
        keyWords.put("else", else_);
        keyWords.put("final", final_);
        keyWords.put("if", if_);
        keyWords.put("new", new_);
        keyWords.put("print", print);
        keyWords.put("read", read);
        keyWords.put("program", program);
        keyWords.put("return", return_);
        keyWords.put("void", void_);
        keyWords.put("while", while_);
        keyWords.put("hash", hash);
    }

    /**
     * Returns next token. To be used by parser.
     */
    @Override
    public Token next() {
        while (Character.isWhitespace(ch) || ch == '\t' || ch == LF || (col == 0 && ch == 0)) {
            nextCh(); // skip white space and tabulator
        }
        Token t = new Token(none, line, col);

        switch (ch) {
            //----- identifier or keyword
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
                readName(t); // distinguish between identifier and keyword
                break;
            //----- number
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                readNumber(t);
                break;
            case '-':
                nextCh();
                if (ch == '=') {
                    t.kind = minusas;
                } else if (ch == '-') {
                    t.kind = mminus;
                } else {
                    t.kind = minus;
                }
                break;
            case '\'':
                readCharConst(t);
                break;
            case '/':
                nextCh();
                if (ch == '*') {
                    skipComment(t);
                    t = next();
                } else {
                    t.kind = slash;
                }
                break;
            case ';':
                t.kind = semicolon;
                nextCh();
                break;
            case EOF:
                t.kind = eof;
                break;
            case '{':
                t.kind = lbrace;
                nextCh();
                break;
            case '}':
                t.kind = rbrace;
                nextCh();
                break;
            default:
                // fill ch and restart method if no character has been read yet
                error(t, INVALID_CHAR, ch);
                nextCh();
                break;
        }

        return t;
    }

    /**
     * reads a character constant
     */
    private void readCharConst(Token t) {
        boolean linebreak = false; // used to check if linebreak is present in quotes
        nextCh(); // read next character

        if (ch == '\'') { // empty char const ''
            errors.error(t.line, t.col, EMPTY_CHARCONST);
        } else if (ch == '\\') { // escape sequences
            nextCh(); // next ch musst be either r, n, \ or whitespace
            switch (ch) {
                case 'r':
                    t.kind = charConst;
                    t.val = '\r';
                    break;
                case 'n':
                    t.kind = charConst;
                    t.val = '\n';
                    break;
                case '\\':
                    t.kind = charConst;
                    t.val = '\\';
                    break;
                case ' ':
                    t.kind = charConst;
                    t.val = ' ';
                    break;
                default:
                    errors.error(t.line, t.col, UNDEFINED_ESCAPE, ch);
                    break;
            }
        } else if (ch == LF) { // newline in charconst
            errors.error(t.line, t.col, ILLEGAL_LINE_END);
            linebreak = true;
        } else {
            t.val = ch; // set value if the character is valid
        }

        if (!linebreak) { // a linebreak also ends a charconst
            nextCh();

            if (ch != '\'') { // charconst must end with "'"
                errors.error(t.line, t.col, MISSING_QUOTE);
                t.val = '\0';
            } else {
                nextCh();
            }
        }

        t.kind = charConst;
    }

    /**
     * return true if keyWords-map contains the method-parameter
     */
    private boolean isKeyWord(String word) {
        return this.keyWords.containsKey(word);
    }

    /**
     * calls nextCh until name is fully read and updates token
     */
    private void readName(Token t) {
        StringBuilder word = new StringBuilder();
        // iterate over character stream unti a whitespace is reached
        while (Character.isLetterOrDigit(ch) || ch == '_') {
            word.append(ch);
            nextCh();
        }

        if (isKeyWord(word.toString())) {
            t.kind = this.keyWords.get(word.toString());
        } else {
            t.kind = ident;
            t.str = word.toString();
        }
    }

    /**
     * calls nextCh until number is fully read and updates token
     */
    private void readNumber(Token t) {
        List<Integer> numbers = new ArrayList<>();
        StringBuilder stringValue = new StringBuilder();

        while (Character.isDigit(ch)) {
            numbers.add(ch - ASCII_ZERO); // numbers from 0 to 9 start ah 48, e.g. '0' - 48 = 0
            stringValue.append(ch);
            nextCh();
        }

        // if a number has more than 10 digits it is too large
        if (numbers.size() > 10) {
            errors.error(t.line, t.col, BIG_NUM, stringValue.toString());
        } else {
            for (int i = 0; i < numbers.size(); i++) {
                // if the number has exactle 10 digits and is bigger than a certain value
                // its last digit must be smaller than 8
                if (numbers.size() == 10 && i == numbers.size() - 1
                        && t.val >= MAX_INT_WITHOUT_LAST_POINT && numbers.get(i) >= 8) {
                    errors.error(t.line, t.col, BIG_NUM, stringValue.toString());
                    t.val = 0;
                } else {
                    // multiply number with 10 times the the inverted position in the list
                    // e.g. 123 = 1 * 10^2 + 2 * 10 + 3 * 1
                    t.val += numbers.get(i) * Math.pow(10, numbers.size() - 1.0 - i);
                }

            }
        }
        t.kind = number;
    }

    /**
     * iterates over chars until end of comment is reached
     */
    private void skipComment(Token t) {
        int commentCount = 1;
        char lastCh = ' ';
        nextCh(); // skip first '*'

        // iterate over comment block until comment counter is 0
        for (; commentCount > 0; nextCh()) {
            if (ch == EOF) {
                t.kind = eof;
                errors.error(t.line, t.col, EOF_IN_COMMENT);
                return;
            }

            if (lastCh == '*' && ch == '/') { // decrement counter if inner comment was closed
                commentCount--;
                if (commentCount > 0) {
                    nextCh(); // skip next character because a comment marker consists of 2 ('*' + '/')
                }

            } else if (lastCh == '/' && ch == '*') { // increment counter if inner comment was found
                commentCount++;
                nextCh();
            }

            lastCh = ch; // save last character in order to recognize comment begin and end (both are two chars)
        }
    }

    /**
     * Returns next character
     */
    private void nextCh() {
        try {
            ch = (char) in.read();

            if (ch == LF) {
                line++; // increment line at newline
                col = 0; // reset column at newline
            } else {
                col++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
