package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;

import java.util.EnumSet;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;

public final class ParserImpl extends Parser {

    private final EnumSet<Token.Kind> firstFactor;
    private final EnumSet<Token.Kind> firstMulop;
    private final EnumSet<Token.Kind> firstRelop;
    private final EnumSet<Token.Kind> firstExpr;
    private final EnumSet<Token.Kind> firstAssignop;
    private final EnumSet<Token.Kind> firstStatement;
    private final EnumSet<Token.Kind> firstMethodDecl;

    private final EnumSet<Token.Kind> followMethodDecl;
    private final EnumSet<Token.Kind> recoverMethodDeclSet;

    private final EnumSet<Token.Kind> recoverDeclSet;

    private static int errDist = 3;
    public static int errors = 0;

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

        this.followMethodDecl = EnumSet.copyOf(firstMethodDecl);
        this.followMethodDecl.add(rbrace);

        this.recoverDeclSet = EnumSet.of(final_, class_, eof, lbrace, ident);
        this.recoverMethodDeclSet = EnumSet.copyOf(followMethodDecl);
        this.recoverMethodDeclSet.add(eof);
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

    public void error(Errors.Message msg, Object... msgParams) {
        if (errDist >= 3) {
            scanner.errors.error(la.line, la.col, msg, msgParams);
            errors++;
        }
        errDist = 0;
    }

    private void recoverDecl() {
        this.error(INVALID_DECL);
        while (!recoverDeclSet.contains(sym)) {
            scan();
        }
    }

    private void recoverMethodDecl() {
        this.error(METH_DECL);
        while (!recoverMethodDeclSet.contains(sym)) {
            scan();
        }
    }

    private void program() {
        check(program);
        check(ident);

        for (; ; ) {
            if (sym == final_) {
                constDecl();
            } else if (sym == ident) {
                varDecl();
            } else if (sym == class_) {
                classDecl();
            } else if (sym == lbrace || sym == eof) {
                break;
            } else {
                recoverDecl();
            }
        }
        check(lbrace);

        for (; ; ) {
            if (firstMethodDecl.contains(sym)) {
                methodDecl();
            } else if (sym == rbrace || sym == eof) {
                break;
            } else {
                recoverMethodDecl();
            }
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
                super.error(CONST_DECL);
        }
        check(semicolon);
    }

    private void varDecl() {
        type();
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
                super.error(METH_DECL);
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

        if (sym == ppperiod) {
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
                    super.error(DESIGN_FOLLOW);
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
                super.error(INVALID_STAT);
        }
    }

    private void assignop() {
        if (firstAssignop.contains(sym)) {
            scan();
        } else {
            super.error(ASSIGN_OP);
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
            varargs();
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
            super.error(REL_OP);
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
                super.error(INVALID_FACT);
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
            super.error(ADD_OP);
        }
    }

    private void mulop() {
        if (sym == times || sym == slash || sym == rem) {
            scan();
        } else {
            super.error(MUL_OP);
        }
    }

    /**
     * puts the lookahead token in current one and scans the next
     */
    private void scan() {
        t = la;
        la = scanner.next();
        sym = la.kind;

        errDist++;
    }

    /**
     * validates if the kind of the lookahead token is equal to the expected kind
     */
    private void check(Token.Kind expected) {
        if (sym == expected) {
            scan();
        } else {
            super.error(TOKEN_EXPECTED, expected);
        }
    }

}
