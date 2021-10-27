package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;

import java.util.EnumSet;

import static ssw.mj.Errors.Message.TOKEN_EXPECTED;
import static ssw.mj.Token.Kind.*;

public final class ParserImpl extends Parser {

    private final EnumSet<Token.Kind> firstFactor = EnumSet.of(ident, number, charConst, new_, lpar);
    private final EnumSet<Token.Kind> firstMulop = EnumSet.of(times, slash, rem);

    // TODO Exercise 3 - 6: implementation of parser
    public ParserImpl(Scanner scanner) {
        super(scanner);
    }

    /**
     * Starts the analysis.
     */
    @Override
    public void parse() {
        // TODO
    }

    private void program() {
        check(program);
        check(ident);
        while() {

        }
    }

    private void actpars() {

    }

    private void expr() {
        if (sym == minus) {
            scan();
        }
        term();
        while(sym == plus || sym == minus) {
            addop();
            term();
        }
    }

    private void term() {
        factor();
        while(firstMulop.contains(sym)) {
            mulop();
            factor();
        }
    }

    private void factor() {
        switch (sym) {
            case ident:
                designator();
                if(sym == lpar) {
                    actpars();
                }
                break;
            case number:
                scan();
                break;
            case charConst:
                scan();
                break;
            case new_:
                scan();
                check(ident);
                if(sym == lbrack) {
                    scan();
                    expr();
                    check(rbrack);
                }
                break;
            case lpar:
                scan();
                expr();
                check(rpar);
                break;
            default:
                error(TOKEN_EXPECTED, firstFactor);
        }
    }

    private void designator() {
        check(ident);
        while(sym == period || sym == lbrack) {
            if(sym == period) {
                scan();
                check(ident);
            } else {
                scan();
                expr();
                check(rbrack);
            }
        }
    }

    private void addop() {
        if(sym == plus || sym == minus) {
            scan();
        } else {
            error(TOKEN_EXPECTED, plus, minus);
        }
    }

    private void mulop() {
        if(sym == times || sym == slash || sym == rem) {
            scan();
        } else {
            error(TOKEN_EXPECTED, times, slash, rem);
        }
    }

    /**
     * puts the lookahead token in current one and scans the next
     */
    private void scan () {
        t = la;
        la = scanner.next();
        sym = la.kind;
    }

    /**
     * validates if the kind of the lookahead token is equal to the expected kind
     */
    private void check (Token.Kind expected) {
        if (sym == expected) {
            scan();
        } else {
            error(TOKEN_EXPECTED, expected);
        }
    }

}
