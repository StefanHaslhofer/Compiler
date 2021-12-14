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
import java.util.Iterator;

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

    private Obj curMethod = null;

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
            } else if (sym == ident && t.kind == semicolon) {
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
        if (code.mainpc == -1) {
            this.error(METH_NOT_FOUND, "main");
        }
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
        code.dataSize++;

        while (sym == comma) {
            scan();
            check(ident);
            tab.insert(Obj.Kind.Var, t.str, type);
            code.dataSize++;
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
        curMethod = tab.insert(Obj.Kind.Meth, methodName, type);
        check(lpar);

        tab.openScope();
        if (sym == ident) {
            formPars(curMethod);
        }
        check(rpar);

        if (methodName.equals("main")) {
            code.mainpc = code.pc;
            if (curMethod.nPars > 0) {
                error(MAIN_WITH_PARAMS);
            }
            if (!curMethod.type.equals(noType)) {
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
            curMethod.locals = tab.curScope.locals();
            curMethod.adr = code.pc;
            code.put(Code.OpCode.enter);
            code.put(curMethod.nPars);
            code.put(tab.curScope.nVars());
        }

        block();
        tab.closeScope();

        if (curMethod.type == Tab.noType) {
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
                    Code.OpCode c = assignop();

                    // check if it is possible to perform an arithmetic operation with the designator
                    if (c != Code.OpCode.nop) {
                        if (!code.isAssignable(x)) {
                            this.error(NO_VAR);
                        }
                        // duplicate variable on stack if we want to perform an arithmetic operation on stack
                        if (x.kind == Operand.Kind.Elem) {
                            code.put(Code.OpCode.dup2);
                            code.put(Code.OpCode.aload);
                        } else {
                            code.put(Code.OpCode.dup);
                        }
                    }

                    Operand y = expr();

                    // both operands have to be of type int if the opCode != "="
                    if (c != Code.OpCode.nop && (x.type != intType || y.type != intType)) {
                        this.error(NO_INT_OP);
                    }

                    if (y.type.assignableTo(x.type)) {
                        code.assign(x, y, c);
                    } else {
                        this.error(INCOMP_TYPES);
                    }
                } else if (sym == lpar) {
                    actpars(x);
                } else if (sym == pplus) {
                    if (x.type != intType) {
                        this.error(NO_INT);
                    }

                    if (!code.isAssignable(x)) {
                        this.error(NO_VAR);
                    }
                    // distinguish between local and global variables
                    if (x.kind != Operand.Kind.Local) {
                        code.arithmethicOpNonLocal(x, 1, Code.OpCode.add);
                    } else {
                        code.addToLocal(x, 1);
                    }
                    scan();
                } else if (sym == mminus) {
                    if (x.type != intType) {
                        this.error(NO_INT);
                    }
                    if (!code.isAssignable(x)) {
                        this.error(NO_VAR);
                    }
                    // distinguish between local and global variables
                    if (x.kind != Operand.Kind.Local) {
                        code.arithmethicOpNonLocal(x, -1, Code.OpCode.add);
                    } else {
                        code.addToLocal(x, -1);
                    }


                    scan();
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
                    x = expr();
                    code.load(x);
                    if (curMethod.type == noType) {
                        this.error(RETURN_VOID);
                    } else if (!x.type.assignableTo(curMethod.type)) {
                        this.error(RETURN_TYPE);
                    }
                } else if (curMethod.type != noType) {
                    this.error(RETURN_NO_VAL);
                }
                check(semicolon);
                code.put(Code.OpCode.exit);
                code.put(Code.OpCode.return_);
                break;
            case read:
                scan();
                check(lpar);
                x = designator();
                if (!code.isReadable(x)) {
                    this.error(READ_VALUE);
                }
                check(rpar);
                check(semicolon);
                code.load(x);
                if (x.type.kind == Struct.Kind.Int) {
                    code.put(Code.OpCode.read);
                } else if (x.type.kind == Struct.Kind.Char) {
                    code.put(Code.OpCode.bread);
                }
                code.storeConst(x.adr);
                break;
            case print:
                scan();
                check(lpar);
                x = expr();
                code.load(x);

                if (sym == comma) {
                    scan();
                    check(number);
                    code.loadConst(t.val);
                } else {
                    code.loadConst(1);
                }

                if (x.type == intType) {
                    code.put(Code.OpCode.print);
                } else if (x.type == charType) {
                    code.put(Code.OpCode.bprint);
                } else {
                    this.error(PRINT_VALUE);
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
                this.error(INVALID_STAT);
        }
    }

    /*
     * return nop if assign and error case otherwise return the matching opCode
     */
    private Code.OpCode assignop() {
        switch (sym) {
            case assign: scan();
                return Code.OpCode.nop;
            case plusas: scan();
                return Code.OpCode.add;
            case minusas: scan();
                return Code.OpCode.sub;
            case timesas: scan();
                return Code.OpCode.mul;
            case slashas: scan();
                return Code.OpCode.div;
            case remas: scan();
                return Code.OpCode.rem;
            default: this.error(ASSIGN_OP);
                return Code.OpCode.nop;
        }
    }

    private void actpars(Operand x) {
        check(lpar);

        if (x.kind != Operand.Kind.Meth) {
            this.error(NO_METH);
            x.obj = tab.noObj;
        }

        if (x.obj == tab.lenObj)
            code.put(Code.OpCode.arraylength);
        else if (x.obj != tab.ordObj && x.obj != tab.chrObj) {
            code.put(Code.OpCode.call);
            code.put2(x.adr - (code.pc - 1));
        }

        x.kind = Operand.Kind.Stack;
        int nPars = 0;
        Iterator<Obj> it = x.obj.locals.iterator();

        if (firstExpr.contains(sym)) {
            Operand y = expr();
            nPars++;
            if (it.hasNext() && !y.type.assignableTo(it.next().type)) {
                this.error(PARAM_TYPE);
            }
            if (x.obj == tab.chrObj && y.type.kind != Struct.Kind.Int
                    || x.obj == tab.ordObj && y.type.kind != Struct.Kind.Char
                    || x.obj == tab.lenObj && y.type.kind != Struct.Kind.Arr) {
                this.error(PARAM_TYPE);
            }

            while (sym == comma) {
                scan();
                y = expr();

                if (it.hasNext() && !y.type.assignableTo(it.next().type)) {
                    this.error(INCOMP_TYPES);
                }
                nPars++;
            }
        }

        // check if the method doesn´t have varargs
        if (nPars < x.obj.nPars && !x.obj.hasVarArg) {
            this.error(LESS_ACTUAL_PARAMS);
        } else if (nPars > x.obj.nPars) {
            this.error(MORE_ACTUAL_PARAMS);
        }

        if (sym == hash) {
            varargs();
        }
        check(rpar);
    }

    private void varargs() {
        check(hash);
        check(number);

        int expectedVarargs = t.val;
        int actualVarargs = 0;

        for (; ; ) {
            if (firstExpr.contains(sym)) {
                expr();
                actualVarargs++;
            } else if (sym == comma) {
                scan();
            } else {
                break;
            }
        }

        // check if number of expected varargs matches the actually present ones
        if (actualVarargs < expectedVarargs) {
            this.error(LESS_ACTUAL_VARARGS);
        } else if (actualVarargs > expectedVarargs) {
            this.error(MORE_ACTUAL_VARARGS);
        }
    }

    private Operand condition() {
        Operand x = condTerm();

        while (sym == or) {
            code.tjump(x);
            scan();
            x.fLabel.here();
            Operand y = condTerm();
            x.fLabel = y.fLabel;
            x.op = y.op;
        }

        return x;
    }

    private Operand condTerm() {
        Operand x = condFact();

        while (sym == and) {
            code.fjump(x);
            scan();
            Operand y = condFact();
            x.op = y.op;
        }

        return x;
    }

    private Operand condFact() {
        Operand x = expr();
        Code.CompOp c = relop();
        Operand y = expr();
        // check for compatibility
        if (!x.type.compatibleWith(y.type)) {
            this.error(INCOMP_TYPES);
        } else if ((y.type.isRefType() || x.type.isRefType())
                && (c != Code.CompOp.ne && c != Code.CompOp.eq)) {
            this.error(EQ_CHECK); // assure that arrays and classes are only checked for (in)equality
        }

        return new Operand(c, code);
    }

    private Code.CompOp relop() {
        switch (sym) {
            case eql:
                scan();
                return Code.CompOp.eq;
            case neq:
                scan();
                return Code.CompOp.ne;
            case leq:
                scan();
                return Code.CompOp.le;
            case lss:
                scan();
                return Code.CompOp.lt;
            case geq:
                scan();
                return Code.CompOp.ge;
            case gtr:
                scan();
                return Code.CompOp.gt;
            default:
                this.error(REL_OP);
                return null;
        }
    }

    private Operand expr() {
        // multiply term value with neg in order to negate it if there is "-" in front
        boolean isNeg = false;
        if (sym == minus) {
            scan();
            isNeg = true;
        }

        Operand x = term();

        if (isNeg) {
            if (x.type != intType) {
                this.error(NO_INT_OP);
            }

            // we can use val only for con otherwise we have to manually call neg
            if (x.kind == Operand.Kind.Con) {
                x.val = -x.val;
            } else {
                code.load(x);
                code.put(Code.OpCode.neg);
            }
        }

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
                    if (x.type == noType) {
                        this.error(INVALID_CALL);
                    }
                    actpars(x);
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

                if (obj.kind != Obj.Kind.Type) {
                    this.error(NO_TYPE);
                }

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
                    if (obj.type.kind != Struct.Kind.Class) {
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
                if (x.type.kind != Struct.Kind.Class) {
                    this.error(NO_CLASS);
                }
                scan();
                code.load(x);
                check(ident);

                Obj obj = tab.findField(t.str, x.type);
                x.kind = Operand.Kind.Fld;
                x.type = obj.type;
                x.adr = obj.adr;
            } else {
                code.load(x);
                scan();
                Operand y = expr();

                if (x.type.kind != Struct.Kind.Arr) {
                    this.error(NO_ARRAY);
                }
                if (y.type != intType) {
                    this.error(ARRAY_INDEX);
                }

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
            scan();
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
