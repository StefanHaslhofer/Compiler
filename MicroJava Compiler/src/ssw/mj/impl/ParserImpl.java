package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;

import static ssw.mj.Errors.Message.TOKEN_EXPECTED;
import static ssw.mj.Token.Kind.ident;
import static ssw.mj.Token.Kind.program;

public final class ParserImpl extends Parser {

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
