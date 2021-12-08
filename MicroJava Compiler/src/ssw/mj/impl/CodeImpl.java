package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Struct;
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
            case Con:
                loadConst(x.val);
                break;
            case Local:
                switch (x.adr) {
                    case 0:
                        put(OpCode.load_0);
                        break;
                    case 1:
                        put(OpCode.load_1);
                        break;
                    case 2:
                        put(OpCode.load_2);
                        break;
                    case 3:
                        put(OpCode.load_3);
                        break;
                    default:
                        put(OpCode.load);
                        put4(x.adr);
                        break;
                }
                break;
            case Static:
                put(OpCode.getstatic);
                put2(x.adr);
                break;
            case Stack:
                break; // nothing to do (already loaded)
            case Fld:
                put(OpCode.getfield);
                put2(x.adr);
                break;
            case Elem:
                if (x.type == Tab.charType) {
                    put(OpCode.baload);
                } else {
                    put(OpCode.aload);
                }
                break;
            default:
                parser.error(NO_VAL);
        }
        x.kind = Operand.Kind.Stack; // remember that value is now loaded
    }

    void assign(Operand x, Operand y) {
        load(y);
        switch (x.kind) {
            case Local:
                switch (x.adr) {
                    case 0:
                        put(OpCode.store_0);
                        break;
                    case 1:
                        put(OpCode.store_1);
                        break;
                    case 2:
                        put(OpCode.store_2);
                        break;
                    case 3:
                        put(OpCode.store_3);
                        break;
                    default:
                        put(OpCode.store);
                        put(x.adr);
                        break;
                }
                break;
            case Static:
                put(OpCode.putstatic);
                put2(x.adr);
                break;
            case Fld:
                put(OpCode.putfield);
                put2(x.adr);
                break;
            case Elem:
                if (x.type == Tab.charType) {
                    put(OpCode.bastore);
                } else {
                    put(OpCode.astore);
                }
                break;
            default:
                parser.error(NO_VAR);
        }
    }

    void loadConst(int x) {
        switch (x) {
            case -1:
                put(OpCode.const_m1);
                break;
            case 0:
                put(OpCode.const_0);
                break;
            case 1:
                put(OpCode.const_1);
                break;
            case 2:
                put(OpCode.const_2);
                break;
            case 3:
                put(OpCode.const_3);
                break;
            case 4:
                put(OpCode.const_4);
                break;
            case 5:
                put(OpCode.const_5);
                break;
            default:
                put(OpCode.const_);
                put(x);
                break;
        }
    }

    /*
     * increment or decrement non local field by value
     */
    void addToNonLocal(Operand x, int val) {
        if (x.type.kind == Struct.Kind.Arr) {
            put(Code.OpCode.dup2);
        } else {
            put(Code.OpCode.dup);
        }
        load(x);
        loadConst(val);
        put(Code.OpCode.add);

        if (x.type.kind == Struct.Kind.Arr) {
            put(Code.OpCode.astore);
        } else {
            put(OpCode.putfield);
            put2(x.adr);
        }
    }

    /*
     * increment or decrement non local field by value
     */
    void addToLocal(Operand x, int val) {
        load(x);
        put(Code.OpCode.inc);
        loadConst(val);
    }
}
