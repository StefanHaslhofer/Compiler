package ssw.mj.impl;

import ssw.mj.Scanner;
import ssw.mj.Token;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
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
                    nextCh();
                } else if (ch == '-') {
                    t.kind = mminus;
                    nextCh();
                } else {
                    t.kind = minus;
                }
                break;
            case '+':
                nextCh();
                if (ch == '=') {
                    t.kind = plusas;
                    nextCh();
                } else if (ch == '+') {
                    t.kind = pplus;
                    nextCh();
                } else {
                    t.kind = plus;
                }
                break;
            case '/':
                nextCh();
                if (ch == '*') {
                    skipComment(t);
                    t = next();
                } else if (ch == '=') {
                    t.kind = slashas;
                    nextCh();
                } else {
                    t.kind = slash;
                }
                break;
            case '*':
                nextCh();
                if (ch == '=') {
                    t.kind = timesas;
                    nextCh();
                } else {
                    t.kind = times;
                }
                break;
            case '%':
                nextCh();
                if (ch == '=') {
                    t.kind = remas;
                    nextCh();
                } else {
                    t.kind = rem;
                }
                break;
            case '=':
                nextCh();
                if (ch == '=') {
                    t.kind = eql;
                    nextCh();
                } else {
                    t.kind = assign;
                }
                break;
            case '!':
                nextCh();
                if (ch == '=') {
                    t.kind = neq;
                    nextCh();
                } else {
                    errors.error(t.line, t.col, INVALID_CHAR, '!');
                }
                break;
            case '<':
                nextCh();
                if (ch == '=') {
                    t.kind = leq;
                    nextCh();
                } else {
                    t.kind = lss;
                }
                break;
            case '>':
                nextCh();
                if (ch == '=') {
                    t.kind = geq;
                    nextCh();
                } else {
                    t.kind = gtr;
                }
                break;
            case '&':
                nextCh();
                if (ch == '&') {
                    t.kind = and;
                    nextCh();
                } else {
                    errors.error(t.line, t.col, INVALID_CHAR, '&');
                }
                break;
            case '|':
                nextCh();
                if (ch == '|') {
                    t.kind = or;
                    nextCh();
                } else {
                    errors.error(t.line, t.col, INVALID_CHAR, '|');
                }
                break;
            case '\'':
                readCharConst(t);
                break;
            case ';':
                t.kind = semicolon;
                nextCh();
                break;
            case ',':
                t.kind = comma;
                nextCh();
                break;
            case '#':
                t.kind = hash;
                nextCh();
                break;
            case '.':
                nextCh();
                if (ch == '.') {
                    nextCh();
                    if (ch == '.') {
                        t.kind = ppperiod;
                    } else {
                        t.kind = pperiod;
                    }
                    nextCh();
                } else {
                    t.kind = period;
                }
                break;
            case '(':
                t.kind = lpar;
                nextCh();
                break;
            case ')':
                t.kind = rpar;
                nextCh();
                break;
            case '{':
                t.kind = lbrace;
                nextCh();
                break;
            case '}':
                t.kind = rbrace;
                nextCh();
                break;
            case '[':
                t.kind = lbrack;
                nextCh();
                break;
            case ']':
                t.kind = rbrack;
                nextCh();
                break;
            case EOF:
                t.kind = eof;
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
        boolean endOfConst = false; // used to check if endOfConst is present in quotes
        nextCh(); // read next character

        if (ch == '\'') { // empty char const ''
            errors.error(t.line, t.col, EMPTY_CHARCONST);
            nextCh();
            endOfConst = true; // charConst ends immediately when empty
        } else if (ch == '\\') { // escape sequences
            nextCh(); // next ch musst be either r, n, \ or whitespace
            switch (ch) {
                case 'r':
                    t.val = '\r';
                    break;
                case 'n':
                    t.val = '\n';
                    break;
                case '\\':
                    t.val = '\\';
                    break;
                case ' ':
                    t.val = ' ';
                    break;
                case '\'': // missing quote will be handled later on
                    t.val = '\'';
                    break;
                default:
                    errors.error(t.line, t.col, UNDEFINED_ESCAPE, ch);
                    break;
            }
        } else if (ch == LF) { // newline in charconst
            errors.error(t.line, t.col, ILLEGAL_LINE_END);
            endOfConst = true; // charConst ends on line end
        } else if (ch == EOF) {
            errors.error(t.line, t.col, EOF_IN_CHAR);
            endOfConst = true; // charConst ends on end of file
        } else {
            t.val = ch; // set value if the character is valid
        }

        if (!endOfConst) { // an endOfConst also ends a charconst
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
        while ((ch >= 48 && ch <= 57) || (ch >= 65 && ch<= 90)
                || (ch >= 97 && ch <= 122) || ch == '_') {
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
        StringBuilder stringValue = new StringBuilder();

        while (Character.isDigit(ch)) {
            stringValue.append(ch);
            nextCh();
        }

        try {
            t.val = Integer.parseInt(stringValue.toString());
        } catch (NumberFormatException ex) {
            errors.error(t.line, t.col, BIG_NUM, stringValue.toString());
            t.val = 0;
        }

        t.kind = number;
    }

    /**
     * iterates over chars until end of comment is reached
     */
    private void skipComment(Token t) {
        int commentCount = 1;
        char lastCh = ' ';

        // iterate over comment block until comment counter is 0
        while (commentCount > 0) {
            if (ch == EOF) {
                t.kind = eof;
                errors.error(t.line, t.col, EOF_IN_COMMENT);
                return;
            } else {
                nextCh(); // skipt first '*' therefore call method at the start of the loop
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

        nextCh();
    }

    /**
     * Returns next character
     */
    private void nextCh() {
        try {
            if (ch != EOF) { // only incrment line and col if end of file is not reached
                ch = (char) in.read();

                if (ch == '\r') { // read next character if we come across CR
                    ch = (char) in.read();
                }

                if (ch == LF) {
                    line++; // increment line at newline
                    col = 0; // reset column at newline
                } else {
                    col++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
