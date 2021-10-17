package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Scanner;
import ssw.mj.Token;

import java.io.IOException;
import java.io.Reader;

import static ssw.mj.Errors.Message.INVALID_CHAR;
import static ssw.mj.Token.Kind.*;

public final class ScannerImpl extends Scanner {

    public ScannerImpl(Reader r) {
        super(r);
        line = 1;
        col = 0;
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
            case '/':
                nextCh();
                if (ch == '*') {
                    skipComment(t);
                    t = next();
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
     * calls nextCh until name is fully read and updates token
     */
    private void readName(Token t) {

    }

    /**
     * calls nextCh until number is fully read and updates token
     */
    private void readNumber(Token t) {

    }

    /**
     * iterates over chars until end of comment is reached
     */
    private void skipComment(Token t) {
        int commentCount = 1;
        char lastCh = ' ';

        // iterate over comment block until comment counter is 0
        for (; commentCount > 0; nextCh()) {
            if (ch == EOF) {
                t.kind = eof;
                errors.error(t.line, t.col, Errors.Message.EOF_IN_COMMENT);
                return;
            }


            if (lastCh == '*' && ch == '/') { // decrement counter if inner comment was closed
                commentCount--;
            } else if (lastCh == '/' && ch == '*') { // increment counter if inner comment was found
                commentCount++;
            } else {
                lastCh = ' ';
            }

            if (ch == '/') { // slash could be the start of a new comment
                lastCh = '/'; // safe possible comment start for next iteration
            } else if (ch == '*') { // a star could be the end of an inner comment
                lastCh = '*'; // safe possible comment end for next iteration
            }
        }
    }

    /**
     * Returns next character
     */
    private void nextCh() {
        try {
            ch = (char) in.read();
            if (ch == LF) {
                line++;
                col = 1;
            } else {
                col++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
