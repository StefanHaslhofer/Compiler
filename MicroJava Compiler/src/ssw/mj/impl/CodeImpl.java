package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.codegen.Code;
import ssw.mj.codegen.Label;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import static ssw.mj.Errors.Message.NO_VAL;
import static ssw.mj.Errors.Message.NO_VAR;

public final class CodeImpl extends Code {

    public CodeImpl(Parser p) {
        super(p);
    }

    public void fjump(Operand x) {

    }

    public void tjump(Operand x) {

    }

    public void jump(Label l) {

    }

    public void load(Operand x) {
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

    public void assign(Operand x, Operand y, OpCode c) {
        // combine two operands via arithmetic operation if a opCode is given
        // otherwise simply load the second operand normal
        if(c != OpCode.nop) {
            load(y);
            put(c);
        } else {
            load(y);
        }
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

    public void loadConst(int x) {
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
                put4(x);
                break;
        }
    }

    public void storeConst(int x) {
        switch (x) {
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
                put(x);
                break;
        }
    }

    /*
     * arithmetic operations for non local fields by value
     */
    public  void arithmethicOpNonLocal(Operand x, int val, Code.OpCode c) {
        // save for kind for later as it will be changed by load(x)
        Operand.Kind k = x.kind;
        if (k == Operand.Kind.Elem) {
            put(Code.OpCode.dup2);
        } else if (k != Operand.Kind.Static){
            put(Code.OpCode.dup);
        }
        load(x);
        loadConst(val);
        put(c);

        if (k == Operand.Kind.Elem) {
            put(Code.OpCode.astore);
        } else if (k == Operand.Kind.Con || k == Operand.Kind.Local || k == Operand.Kind.Fld){
            put(OpCode.putfield);
            put2(x.adr);
        } else{
            put(OpCode.putstatic);
            put2(x.adr);
        }
    }

    /*
     * return true if a given operand is a variable, arrayelement or field
     */
    public boolean isAssignable(Operand x) {
        return x.kind == Operand.Kind.Local || x.kind == Operand.Kind.Elem ||
                x.kind == Operand.Kind.Static || x.kind == Operand.Kind.Fld;
    }

    /*
     * return true if a given can be read
     */
    public boolean isReadable(Operand x) {
        return x.type.kind == Struct.Kind.Int || x.type.kind == Struct.Kind.Char;
    }

    /*
     * increment or decrement non local field by value
     */
    public void addToLocal(Operand x, int val) {
        put(Code.OpCode.inc);
        put(x.adr);
        put(val);
    }
}
