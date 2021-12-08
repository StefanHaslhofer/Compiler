package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Parser;
import ssw.mj.Scanner;
import ssw.mj.Token;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import java.util.EnumSet;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.Token.Kind.*;
import static ssw.mj.symtab.Tab.*;

public final class ParserImpl extends Parser {

    private final EnumSet<Token.Kind> firstFactor;
    private final EnumSet<Token.Kind> firstMulop;
    private final EnumSet<Token.Kind> firstRelop;
    private final EnumSet<Token.Kind> firstExpr;
    private final EnumSet<Token.Kind> firstAssignop;
    private final EnumSet<Token.Kind> firstStatement;
    private final EnumSet<Token.Kind> firstMethodDecl;

    private final EnumSet<Token.Kind> followMethodDecl;
    private final EnumSet<Token.Kind> followStatement;

    private final EnumSet<Token.Kind> recoverMethodDeclSet;
    private final EnumSet<Token.Kind> recoverDeclSet;
    private final EnumSet<Token.Kind> recoverStatementSet;

    private int errDist;
    private static final int ERR_DIST_THRESHOLD = 3;

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

        this.followMethodDecl = EnumSet.of(ident, void_, rbrace);
        this.followStatement = EnumSet.of(else_, rbrace);
        this.followStatement.addAll(firstStatement);

        this.recoverDeclSet = EnumSet.of(final_, class_, eof, lbrace);
        this.recoverMethodDeclSet = EnumSet.copyOf(followMethodDecl);
        this.recoverMethodDeclSet.add(eof);
        this.recoverStatementSet = EnumSet.of(if_, while_, break_, return_, read, print, semicolon, rbrace, else_, eof);
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

    @Override
    public void error(Errors.Message msg, Object... msgParams) {
        if (errDist >= ERR_DIST_THRESHOLD) {
            scanner.errors.error(la.line, la.col, msg, msgParams);
        }
        errDist = 0;
    }

    private void recoverDecl() {
        this.error(INVALID_DECL);
        for (; ; ) {
            if (recoverDeclSet.contains(sym)) {
                break;
            } else if (sym == ident && tab.find(t.str).type != noType) {
                break;
            }

            scan();
        }
        errDist = 0;
    }

    private void recoverMethodDecl() {
        this.error(METH_DECL);
        for (; ; ) {
            if (recoverMethodDeclSet.contains(sym)) {
                break;
            } else if (sym == ident && tab.find(t.str).type != noType) {
                break;
            }

            scan();
        }
        errDist = 0;
    }

    private void recoverStatement() {
        this.error(INVALID_STAT);
        while (!recoverStatementSet.contains(sym)) {
            scan();
        }
        errDist = 0;
    }

    private void program() {
        errDist = ERR_DIST_THRESHOLD;

        check(program);
        check(ident);
        Obj prog = tab.insert(Obj.Kind.Prog, t.str, noType);

        tab.openScope();

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

        if (tab.curScope.locals().size() > MAX_GLOBALS) {
            error(TOO_MANY_GLOBALS);
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

        prog.locals = tab.curScope.locals();
        tab.closeScope();
    }

    private void constDecl() {
        check(final_);
        Struct type = type();
        check(ident);

        Obj constant = tab.insert(Obj.Kind.Con, t.str, type);
        check(assign);

        switch (sym) {
            case number:
                if (constant.type == intType) {
                    scan();
                    constant.val = t.val;
                } else {
                    error(CONST_TYPE);
                }
                break;
            case charConst:
                if (constant.type == charType) {
                    scan();
                    constant.val = t.val;
                } else {
                    error(CONST_TYPE);
                }
                break;
            default:
                this.error(CONST_DECL);
        }
        check(semicolon);
    }

    private void varDecl() {
        Struct type = type();
        check(ident);
        tab.insert(Obj.Kind.Var, t.str, type);

        while (sym == comma) {
            scan();
            check(ident);
            tab.insert(Obj.Kind.Var, t.str, type);
        }
        check(semicolon);
    }

    private void classDecl() {
        check(class_);
        check(ident);
        StructImpl clazz = new StructImpl(Struct.Kind.Class);
        tab.insert(Obj.Kind.Type, t.str, clazz);
        check(lbrace);

        tab.openScope();
        while (sym == ident) {
            varDecl();
        }

        if (tab.curScope.locals().size() > MAX_FIELDS) {
            error(TOO_MANY_FIELDS);
        } else {
            // set local variables and parameters
            clazz.fields = tab.curScope.locals();
        }

        check(rbrace);
        tab.closeScope();
    }

    private void methodDecl() {
        Struct type = noType;
        switch (sym) {
            case ident:
                type = type();
                break;
            case void_:
                scan();
                break;
            default:
                this.error(METH_DECL);
        }
        check(ident);
        String methodName = t.str;
        Obj meth = tab.insert(Obj.Kind.Meth, methodName, type);
        check(lpar);

        tab.openScope();
        if (sym == ident) {
            formPars(meth);
        }
        check(rpar);

        if (methodName.equals("main")) {
            code.mainpc = code.pc;
            if (meth.nPars > 0) {
                error(MAIN_WITH_PARAMS);
            }
            if (!meth.type.equals(noType)) {
                error(MAIN_NOT_VOID);
            }
        }

        while (sym == ident) {
            varDecl();
        }

        if (tab.curScope.locals().size() > MAX_LOCALS) {
            error(TOO_MANY_LOCALS);
        } else {
            // set local variables and parameters
            meth.locals = tab.curScope.locals();
            meth.adr = code.pc;
            code.put(Code.OpCode.enter);
            code.put(meth.nPars);
            code.put(tab.curScope.nVars());
        }

        block();
        tab.closeScope();

        if (meth.type == Tab.noType) {
            code.put(Code.OpCode.exit);
            code.put(Code.OpCode.return_);
        } else { // end of function reached without a return statement
            code.put(Code.OpCode.trap);
            code.put(1);
        }
    }

    private void formPars(Obj meth) {
        Struct type = type();
        check(ident);

        Obj var = tab.insert(Obj.Kind.Var, t.str, type);
        // increase number of parameters
        meth.nPars++;
        while (sym == comma) {
            scan();
            type = type();
            check(ident);
            var = tab.insert(Obj.Kind.Var, t.str, type);
            meth.nPars++;
        }

        if (sym == ppperiod) {
            scan();
            var.type = new StructImpl((StructImpl) type);
            meth.hasVarArg = true;
        }
    }

    private Struct type() {
        check(ident);
        Obj o = tab.find(t.str);
        if (o.kind != Obj.Kind.Type) {
            error(NO_TYPE);

        }
        StructImpl type = o.type;

        if (sym == lbrack) {
            scan();
            check(rbrack);
            type = new StructImpl(type);
        }

        return type;
    }

    private void block() {
        check(lbrace);

        for (; ; ) {
            if (firstStatement.contains(sym)) {
                statement();
            } else if (sym == rbrace || sym == eof) {
                break;
            } else {
                recoverStatement();
            }
        }
        check(rbrace);
    }

    private void statement() {
        Operand x = null;
        if (!firstStatement.contains(sym)) {
            recoverStatement();
        }
        switch (sym) {
            case ident:
                x = designator();
                if (firstAssignop.contains(sym)) {
                    assignop();
                    Operand y = expr();
                    if (y.type.assignableTo(x.type)) {
                        code.assign(x, y);
                    } else {
                        this.error(INCOMP_TYPES);
                    }
                } else if (sym == lpar) {
                    actpars();
                } else if (sym == pplus) {
                    scan();
                    // distinguish between local and global variables
                    if (x.kind != Operand.Kind.Local) {
                        code.addToNonLocal(x, 1);
                    } else {
                        code.addToLocal(x, 1);
                    }
                } else if (sym == mminus) {
                    scan();
                    // distinguish between local and global variables
                    if (x.kind != Operand.Kind.Local) {
                        code.addToNonLocal(x, -1);
                    } else {
                        code.addToLocal(x, -1);
                    }
                } else {
                    this.error(DESIGN_FOLLOW);
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
                x = expr();
                code.load(x);
                code.loadConst(String.valueOf(t.val).length());
                if (sym == comma) {
                    scan();
                    check(number);
                }
                check(rpar);
                check(semicolon);
                code.put(Code.OpCode.print);
                break;
            case lbrace:
                block();
                break;
            case semicolon:
                scan();
                break;
            default:
                this.error(INVALID_STAT);
        }
    }

    private void assignop() {
        if (firstAssignop.contains(sym)) {
            scan();
        } else {
            this.error(ASSIGN_OP);
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
            this.error(REL_OP);
        }
    }

    private Operand expr() {
        if (sym == minus) {
            scan();
        }

        Operand x = term();

        while (sym == plus || sym == minus) {
            Code.OpCode c = addop();
            code.load(x);
            Operand y = term();
            code.load(y);

            if (x.type != intType || y.type != intType) {
                this.error(NO_INT_OP);
            }

            code.put(c);
        }

        return x;
    }

    private Operand term() {
        Operand x = factor();

        while (firstMulop.contains(sym)) {
            Code.OpCode c = mulop();
            code.load(x);
            Operand y = factor();
            code.load(y);

            if (x.type != intType || y.type != intType) {
                this.error(NO_INT_OP);
            }

            code.put(c);
        }

        return x;
    }

    private Operand factor() {
        Operand x = null;
        switch (sym) {
            case ident:
                x = designator();
                if (sym == lpar) {
                    actpars();
                }
                break;
            case number:
                scan();
                x = new Operand(t.val);
                break;
            case charConst:
                scan();
                x = new Operand(t.val);
                x.type = charType;
                break;
            case new_:
                scan();
                check(ident);
                Obj obj = tab.find(t.str);
                StructImpl type = obj.type;

                if (sym == lbrack) {
                    scan();
                    Operand y = expr();
                    if (y.type != intType) {
                        this.error(ARRAY_SIZE);
                    }
                    code.load(y);
                    code.put(Code.OpCode.newarray);
                    // if char array allocate bytes otherwise words
                    if (type == Tab.charType) {
                        code.put(0);
                    } else {
                        code.put(1);
                    }
                    type = new StructImpl(type);
                    check(rbrack);
                } else {
                    if (obj.kind != Obj.Kind.Type || obj.type.kind != Struct.Kind.Class) {
                        this.error(NO_CLASS_TYPE);
                    }
                    code.put(Code.OpCode.new_);
                    code.put2(obj.type.nrFields());
                }

                x = new Operand(type);
                break;
            case lpar:
                scan();
                x = expr();
                check(rpar);
                break;
            default:
                x = new Operand(noType);
                this.error(INVALID_FACT);
        }

        return x;
    }

    private Operand designator() {
        check(ident);
        Operand x = new Operand(tab.find(t.str), this);
        while (sym == period || sym == lbrack) {
            if (sym == period) {
                scan();
                code.load(x);
                check(ident);

                Obj obj = tab.findField(t.str, x.type);
                x.kind = Operand.Kind.Fld;
                x.type = obj.type;
                x.adr = obj.adr;
            } else {
                scan();
                code.load(x);
                Operand y = expr();
                code.load(y);
                check(rbrack);

                x.kind = Operand.Kind.Elem;
                x.type = x.type.elemType;
            }
        }

        return x;
    }

    private Code.OpCode addop() {
        if (sym == plus) {
            scan();
            return Code.OpCode.add;
        } else if (sym == minus) {
            return Code.OpCode.sub;
        } else {
            this.error(ADD_OP);
            return Code.OpCode.nop;
        }
    }

    private Code.OpCode mulop() {
        switch (sym) {
            case times:
                scan();
                return Code.OpCode.mul;
            case slash:
                scan();
                return Code.OpCode.div;
            case rem:
                scan();
                return Code.OpCode.rem;
            default:
                this.error(MUL_OP);
                return Code.OpCode.nop;
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
            this.error(TOKEN_EXPECTED, expected);
        }
    }

}
