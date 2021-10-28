package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;

import java.util.EnumSet;

import static ssw.mj.Errors.Message.TOKEN_EXPECTED;
import static ssw.mj.Token.Kind.*;

public final class ParserImpl extends Parser {

    private final EnumSet<Token.Kind> firstFactor;
    private final EnumSet<Token.Kind> firstMulop;
    private final EnumSet<Token.Kind> firstRelop;
    private final EnumSet<Token.Kind> firstExpr;
    private final EnumSet<Token.Kind> firstAssignop;
    private final EnumSet<Token.Kind> firstStatement;
    private final EnumSet<Token.Kind> firstMethodDecl;

    // TODO Exercise 3 - 6: implementation of parser
    public ParserImpl(Scanner scanner) {
        super(scanner);

        this.firstFactor = EnumSet.of(ident, number, charConst, new_, lpar);
        this.firstMulop = EnumSet.of(times, slash, rem);
        this.firstRelop = EnumSet.of(eql, neq, leq, lss, geq, gtr);
        this.firstExpr = EnumSet.copyOf(firstFactor);
        this.firstExpr.add(minus);
        this.firstAssignop = EnumSet.of(assign, plusas, minusas, timesas, slashas, remas);
        this.firstStatement = EnumSet.of(ident, if_, while_, break_, return_, read, print, semicolon, lbrace);
        this.firstMethodDecl = EnumSet.of(ident, void_);
    }

    /**
     * Starts the analysis.
     */
    @Override
    public void parse() {
        scan();
        program();
        check(eof);
    }

    private void program() {
        check(program);
        check(ident);

        while (sym == final_ || sym == ident || sym == class_) {
            switch (sym) {
                case final_:
                    constDecl();
                    break;
                case ident:
                    varDecl();
                    break;
                default:
                    classDecl();
            }
        }
        check(lbrace);

        while(firstMethodDecl.contains(sym)) {
            methodDecl();
        }
        check(rbrace);
    }

    private void constDecl() {
        check(final_);
        type();
        check(ident);
        check(assign);

        switch (sym) {
            case number:
                scan();
                break;
            case charConst:
                scan();
                break;
            default:
                error(TOKEN_EXPECTED, number, charConst);
        }
        check(semicolon);
    }

    private void varDecl() {
        if (sym == ident) {
            type();
        }
        check(ident);

        while (sym == comma) {
            scan();
            check(ident);
        }
        check(semicolon);
    }

    private void classDecl() {
        check(class_);
        check(ident);
        check(lbrace);

        while (sym == ident) {
            varDecl();
        }
        check(rbrace);
    }

    private void methodDecl() {
        switch (sym) {
            case ident:
                type();
                break;
            case void_:
                scan();
                break;
            default:
                error(TOKEN_EXPECTED, ident, void_);
        }
        check(ident);
        check(lpar);

        if (sym == ident) {
            formPars();
        }
        check(rpar);

        while (sym == ident) {
            varDecl();
        }
        block();
    }

    private void formPars() {
        type();
        check(ident);

        while (sym == comma) {
            scan();
            type();
            check(ident);
        }

        if (sym == pperiod) {
            scan();
        }
    }

    private void type() {
        check(ident);

        if (sym == lbrack) {
            scan();
            check(rbrack);
        }
    }

    private void block() {
        check(lbrace);

        while (firstStatement.contains(sym)) {
            statement();
        }
        check(rbrace);
    }

    private void statement() {
        switch (sym) {
            case ident:
                designator();
                if (firstAssignop.contains(sym)) {
                    assignop();
                    expr();
                } else if (sym == lpar) {
                    actpars();
                } else if (sym == pplus) {
                    scan();
                } else if (sym == mminus) {
                    scan();
                } else {
                    error(TOKEN_EXPECTED, firstAssignop, lpar, pplus, mminus);
                }
                check(semicolon);
                break;
            case if_:
                scan();
                check(lpar);
                condition();
                check(rpar);
                statement();
                if (sym == else_) {
                    scan();
                    statement();
                }
                break;
            case while_:
                scan();
                check(lpar);
                condition();
                check(rpar);
                statement();
                break;
            case break_:
                scan();
                check(semicolon);
                break;
            case return_:
                scan();
                if (firstExpr.contains(sym)) {
                    expr();
                }
                check(semicolon);
                break;
            case read:
                scan();
                check(lpar);
                designator();
                check(rpar);
                check(semicolon);
                break;
            case print:
                scan();
                check(lpar);
                expr();
                if (sym == comma) {
                    scan();
                    check(number);
                }
                check(rpar);
                check(semicolon);
                break;
            case lbrace:
                block();
                break;
            case semicolon:
                scan();
                break;
            default:
                error(TOKEN_EXPECTED, firstStatement);
        }
    }

    private void assignop() {
        if (firstAssignop.contains(sym)) {
            scan();
        }
    }

    private void actpars() {
        check(lpar);

        if (firstExpr.contains(sym)) {
            expr();
            while (sym == comma) {
                scan();
                expr();
            }
        }

        if (sym == hash) {
            scan();
        }
        check(rpar);
    }

    private void varargs() {
        check(hash);
        check(number);

        if (firstExpr.contains(sym)) {
            expr();
            while (sym == comma) {
                scan();
                expr();
            }
        }
    }

    private void condition() {
        condTerm();

        while (sym == or) {
            scan();
            condTerm();
        }
    }

    private void condTerm() {
        condFact();

        while (sym == and) {
            scan();
            condFact();
        }
    }

    private void condFact() {
        expr();
        relop();
        expr();
    }

    private void relop() {
        if (firstRelop.contains(sym)) {
            scan();
        } else {
            error(TOKEN_EXPECTED, firstRelop);
        }
    }

    private void expr() {
        if (sym == minus) {
            scan();
        }

        term();

        while (sym == plus || sym == minus) {
            addop();
            term();
        }
    }

    private void term() {
        factor();

        while (firstMulop.contains(sym)) {
            mulop();
            factor();
        }
    }

    private void factor() {
        switch (sym) {
            case ident:
                designator();
                if (sym == lpar) {
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
                if (sym == lbrack) {
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

        while (sym == period || sym == lbrack) {
            if (sym == period) {
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
        if (sym == plus || sym == minus) {
            scan();
        } else {
            error(TOKEN_EXPECTED, plus, minus);
        }
    }

    private void mulop() {
        if (sym == times || sym == slash || sym == rem) {
            scan();
        } else {
            error(TOKEN_EXPECTED, times, slash, rem);
        }
    }

    /**
     * puts the lookahead token in current one and scans the next
     */
    private void scan() {
        t = la;
        la = scanner.next();
        sym = la.kind;
    }

    /**
     * validates if the kind of the lookahead token is equal to the expected kind
     */
    private void check(Token.Kind expected) {
        if (sym == expected) {
            scan();
        } else {
            error(TOKEN_EXPECTED, expected);
        }
    }

}
