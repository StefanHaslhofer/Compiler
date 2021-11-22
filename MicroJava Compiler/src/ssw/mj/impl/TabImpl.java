package ssw.mj.impl;

import ssw.mj.Parser;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Struct;
import ssw.mj.symtab.Tab;

import java.util.Objects;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.symtab.Struct.Kind.Int;

public final class TabImpl extends Tab {

    /**
     * Set up "universe" (= predefined names).
     */
    public TabImpl(Parser p) {
        super(p);
        openScope();

        noObj = new Obj(Obj.Kind.Var, "$none", noType);

        insert(Obj.Kind.Type, "int", intType);
        insert(Obj.Kind.Type, "char", charType);

        insert(Obj.Kind.Con, "null", nullType);

        Obj chr = insert(Obj.Kind.Meth, "chr", charType);
        chr.nPars++;
        openScope();
        Obj i = insert(Obj.Kind.Var, "i", intType);
        i.level = 1;
        chr.locals = curScope.locals();
        closeScope();

        Obj ord = insert(Obj.Kind.Meth, "ord", intType);
        ord.nPars++;
        openScope();
        Obj ch = insert(Obj.Kind.Var, "ch", charType);
        ch.level = 1;
        ord.locals = curScope.locals();
        closeScope();

        Obj len = insert(Obj.Kind.Meth, "len", intType);
        len.nPars++;
        openScope();
        Obj arr = insert(Obj.Kind.Var, "arr", new StructImpl(noType));
        arr.level = 1;
        len.locals = curScope.locals();
        closeScope();
    }

    public void openScope() {
        curScope = new Scope(curScope);
        curLevel++;
    }

    public void closeScope() {
        curScope = curScope.outer();
        curLevel--;
    }

    public Obj insert(Obj.Kind kind, String name, Struct type) {
        if(name == null) {
            return noObj;
        }

        // report error if the found object is not null, which means that it is already declared
        if (curScope.findLocal(name) != null) {
            parser.error(DECL_NAME, name);
            return noObj;
        }

        Obj newObj = new Obj(kind, name, (StructImpl) type);

        if (kind == Obj.Kind.Var) {
            newObj.adr = curScope.nVars();
            newObj.level = curLevel;
        } else if (kind == Obj.Kind.Meth) {
            newObj.adr = curScope.nVars();
        }

        curScope.insert(newObj);
        return newObj;
    }

    public Obj find(String name) {
        Obj obj = curScope.findGlobal(name);
        if (obj == null) {
            parser.error(NOT_FOUND, name);
            return noObj;
        }

        return obj;
    }

    public Obj findField(String name, Struct type) {
        Obj obj = type.findField(name);

        if (obj == null) {
            parser.error(NO_FIELD, name);
            return noObj;
        }

        return obj;
    }
}
