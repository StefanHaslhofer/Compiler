package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Tab;

import static ssw.mj.Errors.Message.NO_VAL;
import static ssw.mj.Errors.Message.NO_VAR;

public final class CodeImpl extends Code {

    public CodeImpl(Parser p) {
        super(p);
    }

    // TODO Exercise 5 - 6: implementation of code generation
    void load(Operand x) {
        switch (x.kind) {
            case Con: loadConst(x.val); break;
            case Local:
                switch (x.adr) {
                    case 0: put(OpCode.load_0); break;
                    case 1: put(OpCode.load_1); break;
                    case 2: put(OpCode.load_2); break;
                    case 3: put(OpCode.load_3); break;
                    default: put(OpCode.load); put(x.adr); break;
                }
                break;
            case Static: put(OpCode.getstatic); put2(x.adr); break;
            case Stack: break; // nothing to do (already loaded)
            case Fld: put(OpCode.getfield); put2(x.adr); break;
            case Elem:
                if (x.type == Tab.charType) { put(OpCode.baload); }
                else { put(OpCode.aload); }
                break;
            default: parser.error(NO_VAL);
        }
        x.kind = Operand.Kind.Stack; // remember that value is now loaded
    }

    void assign(Operand x, Operand y) {
        load(y);
        switch (x.kind) {
            case Local:
                switch (x.adr) {
                    case 0: put(OpCode.store_0); break;
                    case 1: put(OpCode.store_1); break;
                    case 2: put(OpCode.store_2); break;
                    case 3: put(OpCode.store_3); break;
                    default: put(OpCode.store); put(x.adr); break;
                }
                break;
            case Static: put(OpCode.putstatic); put2(x.adr); break;
            case Fld: put(OpCode.putfield); put2(x.adr); break;
            case Elem:
                if (x.type == Tab.charType) { put(OpCode.bastore); }
                else { put(OpCode.astore); }
                break;
            default: parser.error(NO_VAR);
        }
    }

    void loadConst(int x) {

    }
}
