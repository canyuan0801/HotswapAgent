

package org.hotswap.agent.javassist.compiler;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMember;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtPrimitiveType;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.LocalVariableAttribute;
import org.hotswap.agent.javassist.bytecode.Opcode;
import org.hotswap.agent.javassist.compiler.ast.ASTList;
import org.hotswap.agent.javassist.compiler.ast.ASTree;
import org.hotswap.agent.javassist.compiler.ast.CallExpr;
import org.hotswap.agent.javassist.compiler.ast.Declarator;
import org.hotswap.agent.javassist.compiler.ast.Expr;
import org.hotswap.agent.javassist.compiler.ast.FieldDecl;
import org.hotswap.agent.javassist.compiler.ast.Member;
import org.hotswap.agent.javassist.compiler.ast.MethodDecl;
import org.hotswap.agent.javassist.compiler.ast.Stmnt;
import org.hotswap.agent.javassist.compiler.ast.Symbol;

public class Javac {
    JvstCodeGen gen;
    SymbolTable stable;
    private Bytecode bytecode;

    public static final String param0Name = "$0";
    public static final String resultVarName = "$_";
    public static final String proceedName = "$proceed";


    public Javac(CtClass thisClass) {
        this(new Bytecode(thisClass.getClassFile2().getConstPool(), 0, 0),
             thisClass);
    }


    public Javac(Bytecode b, CtClass thisClass) {
        gen = new JvstCodeGen(b, thisClass, thisClass.getClassPool());
        stable = new SymbolTable();
        bytecode = b;
    }


    public Bytecode getBytecode() { return bytecode; }


    public CtMember compile(String src) throws CompileError {
        Parser p = new Parser(new Lex(src));
        ASTList mem = p.parseMember1(stable);
        try {
            if (mem instanceof FieldDecl)
                return compileField((FieldDecl)mem);
            CtBehavior cb = compileMethod(p, (MethodDecl)mem);
            CtClass decl = cb.getDeclaringClass();
            cb.getMethodInfo2()
              .rebuildStackMapIf6(decl.getClassPool(),
                                  decl.getClassFile2());
            return cb;
        }
        catch (BadBytecode bb) {
            throw new CompileError(bb.getMessage());
        }
        catch (CannotCompileException e) {
            throw new CompileError(e.getMessage());
        }
    }

    public static class CtFieldWithInit extends CtField {
        private ASTree init;

        CtFieldWithInit(CtClass type, String name, CtClass declaring)
            throws CannotCompileException
        {
            super(type, name, declaring);
            init = null;
        }

        protected void setInit(ASTree i) { init = i; }

        @Override
        protected ASTree getInitAST() {
            return init;
        }
    }

    private CtField compileField(FieldDecl fd)
        throws CompileError, CannotCompileException
    {
        CtFieldWithInit f;
        Declarator d = fd.getDeclarator();
        f = new CtFieldWithInit(gen.resolver.lookupClass(d),
                                d.getVariable().get(), gen.getThisClass());
        f.setModifiers(MemberResolver.getModifiers(fd.getModifiers()));
        if (fd.getInit() != null)
            f.setInit(fd.getInit());

        return f;
    }

    private CtBehavior compileMethod(Parser p, MethodDecl md)
        throws CompileError
    {
        int mod = MemberResolver.getModifiers(md.getModifiers());
        CtClass[] plist = gen.makeParamList(md);
        CtClass[] tlist = gen.makeThrowsList(md);
        recordParams(plist, Modifier.isStatic(mod));
        md = p.parseMethod2(stable, md);
        try {
            if (md.isConstructor()) {
                CtConstructor cons = new CtConstructor(plist,
                                                   gen.getThisClass());
                cons.setModifiers(mod);
                md.accept(gen);
                cons.getMethodInfo().setCodeAttribute(
                                        bytecode.toCodeAttribute());
                cons.setExceptionTypes(tlist);
                return cons;
            }
            Declarator r = md.getReturn();
            CtClass rtype = gen.resolver.lookupClass(r);
            recordReturnType(rtype, false);
            CtMethod method = new CtMethod(rtype, r.getVariable().get(),
                                       plist, gen.getThisClass());
            method.setModifiers(mod);
            gen.setThisMethod(method);
            md.accept(gen);
            if (md.getBody() != null)
                method.getMethodInfo().setCodeAttribute(
                                    bytecode.toCodeAttribute());
            else
                method.setModifiers(mod | Modifier.ABSTRACT);

            method.setExceptionTypes(tlist);
            return method;
        }
        catch (NotFoundException e) {
            throw new CompileError(e.toString());
        }
    }


    public Bytecode compileBody(CtBehavior method, String src)
        throws CompileError
    {
        try {
            int mod = method.getModifiers();
            recordParams(method.getParameterTypes(), Modifier.isStatic(mod));

            CtClass rtype;
            if (method instanceof CtMethod) {
                gen.setThisMethod((CtMethod)method);
                rtype = ((CtMethod)method).getReturnType();
            }
            else
                rtype = CtClass.voidType;

            recordReturnType(rtype, false);
            boolean isVoid = rtype == CtClass.voidType;

            if (src == null)
                makeDefaultBody(bytecode, rtype);
            else {
                Parser p = new Parser(new Lex(src));
                SymbolTable stb = new SymbolTable(stable);
                Stmnt s = p.parseStatement(stb);
                if (p.hasMore())
                    throw new CompileError(
                        "the method/constructor body must be surrounded by {}");

                boolean callSuper = false;
                if (method instanceof CtConstructor)
                    callSuper = !((CtConstructor)method).isClassInitializer();

                gen.atMethodBody(s, callSuper, isVoid);
            }

            return bytecode;
        }
        catch (NotFoundException e) {
            throw new CompileError(e.toString());
        }
    }

    private static void makeDefaultBody(Bytecode b, CtClass type) {
        int op;
        int value;
        if (type instanceof CtPrimitiveType) {
            CtPrimitiveType pt = (CtPrimitiveType)type;
            op = pt.getReturnOp();
            if (op == Opcode.DRETURN)
                value = Opcode.DCONST_0;
            else if (op == Opcode.FRETURN)
                value = Opcode.FCONST_0;
            else if (op == Opcode.LRETURN)
                value = Opcode.LCONST_0;
            else if (op == Opcode.RETURN)
                value = Opcode.NOP;
            else
                value = Opcode.ICONST_0;
        }
        else {
            op = Opcode.ARETURN;
            value = Opcode.ACONST_NULL;
        }

        if (value != Opcode.NOP)
            b.addOpcode(value);

        b.addOpcode(op);
    }


    public boolean recordLocalVariables(CodeAttribute ca, int pc)
        throws CompileError
    {
        LocalVariableAttribute va
            = (LocalVariableAttribute)
              ca.getAttribute(LocalVariableAttribute.tag);
        if (va == null)
            return false;

        int n = va.tableLength();
        for (int i = 0; i < n; ++i) {
            int start = va.startPc(i);
            int len = va.codeLength(i);
            if (start <= pc && pc < start + len)
                gen.recordVariable(va.descriptor(i), va.variableName(i),
                                   va.index(i), stable);
        }

        return true;
    }


    public boolean recordParamNames(CodeAttribute ca, int numOfLocalVars)
        throws CompileError
    {
        LocalVariableAttribute va
            = (LocalVariableAttribute)
              ca.getAttribute(LocalVariableAttribute.tag);
        if (va == null)
            return false;

        int n = va.tableLength();
        for (int i = 0; i < n; ++i) {
            int index = va.index(i);
            if (index < numOfLocalVars)
                gen.recordVariable(va.descriptor(i), va.variableName(i),
                                   index, stable);
        }

        return true;
    }



    public int recordParams(CtClass[] params, boolean isStatic)
        throws CompileError
    {
        return gen.recordParams(params, isStatic, "$", "$args", "$$", stable);
    }


    public int recordParams(String target, CtClass[] params,
                             boolean use0, int varNo, boolean isStatic)
        throws CompileError
    {
        return gen.recordParams(params, isStatic, "$", "$args", "$$",
                                use0, varNo, target, stable);
    }


    public void setMaxLocals(int max) {
        gen.setMaxLocals(max);
    }


    public int recordReturnType(CtClass type, boolean useResultVar)
        throws CompileError
    {
        gen.recordType(type);
        return gen.recordReturnType(type, "$r",
                        (useResultVar ? resultVarName : null), stable);
    }


    public void recordType(CtClass t) {
        gen.recordType(t);
    }


    public int recordVariable(CtClass type, String name)
        throws CompileError
    {
        return gen.recordVariable(type, name, stable);
    }


    public void recordProceed(String target, String method)
        throws CompileError
    {
        Parser p = new Parser(new Lex(target));
        final ASTree texpr = p.parseExpression(stable);
        final String m = method;

        ProceedHandler h = new ProceedHandler() {
                @Override
                public void doit(JvstCodeGen gen, Bytecode b, ASTList args)
                    throws CompileError
                {
                    ASTree expr = new Member(m);
                    if (texpr != null)
                        expr = Expr.make('.', texpr, expr);

                    expr = CallExpr.makeCall(expr, args);
                    gen.compileExpr(expr);
                    gen.addNullIfVoid();
                }

                @Override
                public void setReturnType(JvstTypeChecker check, ASTList args)
                    throws CompileError
                {
                    ASTree expr = new Member(m);
                    if (texpr != null)
                        expr = Expr.make('.', texpr, expr);

                    expr = CallExpr.makeCall(expr, args);
                    expr.accept(check);
                    check.addNullIfVoid();
                }
            };

        gen.setProceedHandler(h, proceedName);
    }


    public void recordStaticProceed(String targetClass, String method)
        throws CompileError
    {
        final String c = targetClass;
        final String m = method;

        ProceedHandler h = new ProceedHandler() {
                @Override
                public void doit(JvstCodeGen gen, Bytecode b, ASTList args)
                    throws CompileError
                {
                    Expr expr = Expr.make(TokenId.MEMBER,
                                          new Symbol(c), new Member(m));
                    expr = CallExpr.makeCall(expr, args);
                    gen.compileExpr(expr);
                    gen.addNullIfVoid();
                }

                @Override
                public void setReturnType(JvstTypeChecker check, ASTList args)
                    throws CompileError
                {
                    Expr expr = Expr.make(TokenId.MEMBER,
                                          new Symbol(c), new Member(m));
                    expr = CallExpr.makeCall(expr, args);
                    expr.accept(check);
                    check.addNullIfVoid();
                }
            };

        gen.setProceedHandler(h, proceedName);
    }


    public void recordSpecialProceed(String target, final String classname,
                                     final String methodname, final String descriptor,
                                     final int methodIndex)
        throws CompileError
    {
        Parser p = new Parser(new Lex(target));
        final ASTree texpr = p.parseExpression(stable);

        ProceedHandler h = new ProceedHandler() {
                @Override
                public void doit(JvstCodeGen gen, Bytecode b, ASTList args)
                    throws CompileError
                {
                    gen.compileInvokeSpecial(texpr, methodIndex, descriptor, args);
                }

                @Override
                public void setReturnType(JvstTypeChecker c, ASTList args)
                    throws CompileError
                {
                    c.compileInvokeSpecial(texpr, classname, methodname, descriptor, args);
                }

            };

        gen.setProceedHandler(h, proceedName);
    }


    public void recordProceed(ProceedHandler h) {
        gen.setProceedHandler(h, proceedName);
    }


    public void compileStmnt(String src) throws CompileError {
        Parser p = new Parser(new Lex(src));
        SymbolTable stb = new SymbolTable(stable);
        while (p.hasMore()) {
            Stmnt s = p.parseStatement(stb);
            if (s != null)
                s.accept(gen);
        }
    }


    public void compileExpr(String src) throws CompileError {
        ASTree e = parseExpr(src, stable);
        compileExpr(e);
    }


    public static ASTree parseExpr(String src, SymbolTable st)
        throws CompileError
    {
        Parser p = new Parser(new Lex(src));
        return p.parseExpression(st);
    }


    public void compileExpr(ASTree e) throws CompileError {
        if (e != null)
            gen.compileExpr(e);
    }
}
