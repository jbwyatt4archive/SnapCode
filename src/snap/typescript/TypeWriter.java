package snap.typescript;
import java.util.List;
import snap.javaparse.*;

/**
 * A class to convert Java to TypeScript.
 */
public class TypeWriter {

    // The StringBuffer
    StringBuffer      _sb = new StringBuffer();
    
    // The indent string
    String            _indentStr = "    ";
    
    // The indent level
    int               _indent;
    
    // Whether at line end
    boolean           _lineStart;

/**
 * Returns the string for JNode.
 */
public String getString(JNode aNode)
{
    _sb.setLength(0); _indent = 0;
    writeJNode(aNode);
    return _sb.toString();
}

/**
 * Writes a JNode.
 */
public void writeJNode(JNode aNode)
{
    if(aNode instanceof JFile) writeJFile((JFile)aNode);
    //else if(aNode instanceof JPackageDecl) writeJPackageDecl((JPackageDecl)aNode);
    else if(aNode instanceof JImportDecl) writeJImportDecl((JImportDecl)aNode);
    else if(aNode instanceof JClassDecl) writeJClassDecl((JClassDecl)aNode);
    else if(aNode instanceof JFieldDecl) writeJFieldDecl((JFieldDecl)aNode);
    else if(aNode instanceof JMethodDecl) writeJMethodDecl((JMethodDecl)aNode);
    else if(aNode instanceof JStmt) writeJStmt((JStmt)aNode);
    else if(aNode instanceof JExpr) writeJExpr((JExpr)aNode);
    else if(aNode instanceof JType) writeJType((JType)aNode);
    else if(aNode instanceof JVarDecl) writeJVarDecl((JVarDecl)aNode);
    else append("TSWriter: write" + aNode.getClass().getSimpleName() + " not implemented");
}

/**
 * Writes a list of JNodes joined by string.
 */
public void writeJNodesJoined(List <? extends JNode> theNodes, String aStr)
{
    JNode last = theNodes.size()>0? theNodes.get(theNodes.size()-1) : null;
    for(JNode node : theNodes) {
        writeJNode(node); if(node!=last) append(aStr); }
}

/**
 * Write a JFile.
 */
public void writeJFile(JFile aJFile)
{
    append("/* Generated from Java with SnapCode - http://www.reportmill.com */\n");
    
    String pname = aJFile.getPackageName();
    if(pname!=null)
        append("namespace ").append(pname).append(' ').append("{").endln().indent();
    
    // Append imports
    aJFile.getUnusedImports();
    for(JImportDecl imp : aJFile.getImportDecls())
        writeJImportDecl(imp);
    endln();
    
    // Append class decls
    writeJClassDecl(aJFile.getClassDecl());
    
    // Outdent and terminate namespace
    if(pname!=null)
        outdent().append('}').endln();
    
    // Write main method
    if(aJFile.getClassDecl().getSimpleName().equals("HelloWorld"))
        endln().append(aJFile.getClassDecl().getClassName()).append(".main(null);\n");
}

/**
 * Write a JImportDecl.
 */
public void writeJImportDecl(JImportDecl anImp)
{
    String iname = anImp.getName(); if(iname.startsWith("jsweet.")) return;
    iname = iname.replace(".function", ".__function");
    
    // Handle inclusive
    if(anImp.isInclusive()) {
        for(String cname : anImp.getFoundClassNames()) {
            append("import ").append(cname).append(" = ").append(iname).append('.').append(cname).append(';').endln(); }
    }

    // Fully specified    
    else {
        int ind = iname.lastIndexOf('.');
        String cname = iname.substring(ind+1);
        append("import ").append(cname).append(" = ").append(iname).append(';').endln();
    }
}

/**
 * Writes a JClassDecl.
 */
public void writeJClassDecl(JClassDecl aCDecl)
{
    // Get class name
    String cname = aCDecl.getSimpleName();
    boolean isInterface = aCDecl.isInterface();
    if(aCDecl.isAnonymousClass()) { 
        JClassDecl ec = aCDecl.getEnclosingClassDecl();
        cname = ec.getSimpleName() + '$' + cname;
    }
    
    // Add export call
    if(aCDecl.getFile().getPackageName()!=null)
        append("export ");

    // Append class label with modifiers: public class/interface XXX ...
    JModifiers mods = aCDecl.getModifiers();
    String mstr = mods!=null && mods.isAbstract()? "abstract " : "";
    append(mstr);
    append(isInterface? "interface " : "class ");
    append(cname).append(' ');
    
    // Append extends types
    List <JType> etypes = aCDecl.getExtendsTypes(); JType elast = etypes.size()>0? etypes.get(etypes.size()-1) : null;
    if(etypes.size()>0) append("extends ");
    for(JType etyp : etypes) {
        writeJType(etyp); if(etyp!=elast) append(", "); else append(' '); }
        
    // Append implements types
    List <JType> itypes = aCDecl.getImplementsTypes(); JType ilast = itypes.size()>0? itypes.get(itypes.size()-1): null;
    if(itypes.size()>0) append("implements ");
    for(JType ityp : itypes) {
        writeJType(ityp); if(ityp!=ilast) append(", "); else append(' '); }
    
    // Write class label close char
    append('{').endln().endln();
    indent();
    
    // Append fields
    JFieldDecl fdecls[] = aCDecl.getFieldDecls();
    for(JFieldDecl fd : fdecls) {
        writeJFieldDecl(fd); endln(); }
        
    // Append constructors
    JConstrDecl cndecls[] = aCDecl.getConstructorDecls();
    for(JConstrDecl cd : cndecls) {
        writeJConstrDecl(cd); endln(); }
        
    // Append methods
    JMethodDecl mdecls[] = aCDecl.getMethodDecls(), mlast = mdecls.length>0? mdecls[mdecls.length-1] : null;
    for(JMethodDecl md : mdecls) {
        writeJMethodDecl(md); if(md!=mlast) endln(); }
        
    // Terminate
    outdent();
    append('}').endln().endln();
    
    // If not interface, write Class/Interfaces stuff
    if(!isInterface) {
    
        // Write class
        String cpath = aCDecl.getClassName().replace('$','.');
        append(cname).append("[\"__class\"] = \"").append(cpath).append("\";").endln();
        
        // Write interfaces
        if(itypes.size()>0) {
            append(cname).append("[\"__interfaces\"] = [");
            for(JType ityp : itypes) { String cp = ityp.getClassName().replace('$','.');
                append('"').append(cp).append('"'); if(ityp!=ilast) append(","); }
            append("];").endln();
        }
    }
    
    // Append inner classes
    JClassDecl cdecls[] = aCDecl.getClassDecls();
    if(cdecls.length>0) {
        //System.out.println("Writing inner classes for " + aCDecl.getFile().getName() + " (" + cdecls.length + ')');
        endln().append("export namespace ").append(cname).append(" {").endln().endln().indent();
        for(JClassDecl cd : cdecls)
            writeJClassDecl(cd);
        outdent().append('}').endln();
    }
}

/**
 * Writes a JFieldDecl.
 */
public void writeJFieldDecl(JFieldDecl aFDecl)
{
    // Write modifiers
    JModifiers mods = aFDecl.getModifiers();
    writeJModifiers(mods);

    // Get first var decl
    JVarDecl vd = aFDecl.getVarDecls().get(0);
    
    // Write var decl and terminator
    writeJVarDecl(vd);
    append(';').endln();
}

/**
 * Writes a JConstrDecl.
 */
public void writeJConstrDecl(JConstrDecl aCDecl)
{
    // Write modifiers
    JModifiers mods = aCDecl.getModifiers();
    writeJModifiers(mods);

    // Write 'constructor' label and start char
    append("constructor(");
    
    // Write parameters
    List <JVarDecl> params = aCDecl.getParameters();
    JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : params) {
        writeJVarDecl(param); if(param!=last) append(", "); }
        
    // Write parameters close char
    append(") ");
    
    // Write statement block
    writeJStmtBlock(aCDecl.getBlock(), false);
}

/**
 * Writes a JMethodDecl.
 */
public void writeJMethodDecl(JMethodDecl aMDecl)
{
    // Write modifiers
    JModifiers mods = aMDecl.getModifiers();
    writeJModifiers(mods);

    // Write method name and args start char
    append(aMDecl.getName()).append("(");
    
    // Write parameters
    List <JVarDecl> params = aMDecl.getParameters();
    JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : params) {
        writeJVarDecl(param); if(param!=last) append(", "); }
        
    // Write parameters close char
    append(") ");
    
    // Write return type (if not empty/void)
    String tstr = getTypeString(aMDecl.getType());
    if(tstr.length()>0) append(": ").append(tstr).append(' ');
    
    // If interface or abstract, just write semi-colon
    if(aMDecl.getEnclosingClassDecl().isInterface() || mods!=null && mods.isAbstract())
        append(';').endln();
    
    // Otherwise, write method block
    else writeJStmtBlock(aMDecl.getBlock(), false);
}

/**
 * Writes a JVarDecl.
 */
public void writeJVarDecl(JVarDecl aVD)
{
    // Write name
    JExprId name = aVD.getId();
    writeJExprId(name); append(" : ");
    
    // Write type
    String tstr = getTypeString(aVD.getType());
    for(int i=0,iMax=aVD.getArrayCount();i<iMax;i++) tstr += "[]";
    append(tstr).append(' ');
    
    // Write initializer
    JExpr init = aVD.getInitializer();
    if(init!=null) {
        append("= ");
        writeJExpr(init);
    }
}

/**
 * Writes a JModifiers.
 */
public void writeJModifiers(JModifiers aMod)
{
    String str = getJModifierString(aMod);
    append(str);
}

/**
 * Returns a string for JModifier.
 */
public String getJModifierString(JModifiers aMod)
{
    String str = ""; if(aMod==null) return str;
    if(aMod.isPublic()) str += "public ";
    if(aMod.isStatic()) str += "static ";
    if(aMod.isAbstract()) str += "abstract ";
    return str;
}

/**
 * Writes a type.
 */
public void writeJType(JType aType)
{
    String str = getTypeString(aType); if(str.length()==0) return;
    append(str);
}

/**
 * Returns a type string.
 */
public String getTypeString(JType aType)
{
    // Get name
    String name = aType.getName();
    String cname = aType.getClassName();
    
    // Map core TypeScript types
    if(name.equals("void")) name = "";
    else if(name.equals("Object")) name = "any";
    else if(name.equals("String") || name.equals("char") || name.equals("Character")) name = "string";
    else if(aType.isNumberType()) name = "number";
    else if(name.equals("Boolean")) name = "boolean";
    else if(cname!=null && cname.startsWith("java.lang.")) name = "java.lang." + name;
    
    // Add array chars
    if(aType.isArrayType())
        name = name + "[]";
        
    // Return name
    return name;
}

/**
 * Writes a JStmt.
 */
public void writeJStmt(JStmt aStmt)
{
    if(aStmt instanceof JStmtAssert) writeJStmtAssert((JStmtAssert)aStmt);
    else if(aStmt instanceof JStmtBlock) writeJStmtBlock((JStmtBlock)aStmt, false);
    else if(aStmt instanceof JStmtBreak) writeJStmtBreak((JStmtBreak)aStmt);
    else if(aStmt instanceof JStmtClassDecl) writeJStmtClassDecl((JStmtClassDecl)aStmt);
    else if(aStmt instanceof JStmtConstrCall) writeJStmtConstrCall((JStmtConstrCall)aStmt);
    else if(aStmt instanceof JStmtContinue) writeJStmtContinue((JStmtContinue)aStmt);
    else if(aStmt instanceof JStmtDo) writeJStmtDo((JStmtDo)aStmt);
    else if(aStmt instanceof JStmtEmpty) writeJStmtEmpty((JStmtEmpty)aStmt);
    else if(aStmt instanceof JStmtExpr) writeJStmtExpr((JStmtExpr)aStmt);
    else if(aStmt instanceof JStmtFor) writeJStmtFor((JStmtFor)aStmt);
    else if(aStmt instanceof JStmtIf) writeJStmtIf((JStmtIf)aStmt);
    else if(aStmt instanceof JStmtLabeled) writeJStmtLabeled((JStmtLabeled)aStmt);
    else if(aStmt instanceof JStmtReturn) writeJStmtReturn((JStmtReturn)aStmt);
    else if(aStmt instanceof JStmtSwitch) writeJStmtSwitch((JStmtSwitch)aStmt);
    else if(aStmt instanceof JStmtSynchronized) writeJStmtSynchronized((JStmtSynchronized)aStmt);
    else if(aStmt instanceof JStmtThrow) writeJStmtThrow((JStmtThrow)aStmt);
    else if(aStmt instanceof JStmtTry) writeJStmtTry((JStmtTry)aStmt);
    else if(aStmt instanceof JStmtVarDecl) writeJStmtVarDecl((JStmtVarDecl)aStmt);
    else if(aStmt instanceof JStmtWhile) writeJStmtWhile((JStmtWhile)aStmt);
    else throw new RuntimeException("TSWriter.writeJStmt: Unsupported statement " + aStmt.getClass());
    //else append(aStmt.getString()).endln();
}

/**
 * Writes a JStmtAssert.
 */
public void writeJStmtAssert(JStmtAssert aStmt)
{
    JExpr cond = aStmt.getConditional(), expr = aStmt.getExpr();
    append("if(!("); writeJExpr(cond); append(")) ");
    append("throw new Error(\"Assertion error\")");
    append(';').endln();
}

/**
 * Writes a JStmtBlock.
 */
public void writeJStmtBlock(JStmtBlock aBlock, boolean doSemicolon)
{
    // Write start and indent
    append('{').endln();
    indent();
    
    // Write statements
    if(aBlock!=null)
        for(JStmt stmt : aBlock.getStatements())
            writeJStmt(stmt);
        
    // Outdent and terminate
    outdent(); append(doSemicolon? "};" : "}").endln();
}

/**
 * Writes a JStmtBreak.
 */
public void writeJStmtBreak(JStmtBreak aStmt)
{
    append("break");
    JExpr label = aStmt.getLabel();
    if(label!=null) {
        append(' '); writeJExpr(label); }
    append(';').endln();
}

/**
 * Writes a JStmtClassDecl.
 */
public void writeJStmtClassDecl(JStmtClassDecl aStmt)
{
    JClassDecl cdecl = aStmt.getClassDecl();
    writeJClassDecl(cdecl);
}

/**
 * Writes a JStmtConstrCall.
 */
public void writeJStmtConstrCall(JStmtConstrCall aStmt)
{
    // Write label (this, super, Class.this, etc.) and open char
    List <JExprId> ids = aStmt.getIds();
    writeJNodesJoined(ids, ".");
    append('(');
    
    // Write args and close char
    List <JExpr> args = aStmt.getArgs();
    writeJNodesJoined(args, ", ");
    append(");").endln();
}

/**
 * Writes a JStmtContinue.
 */
public void writeJStmtContinue(JStmtContinue aStmt)
{
    append("continue");
    JExpr label = aStmt.getLabel();
    if(label!=null) {
        append(' '); writeJExpr(label); }
    append(';').endln();
}

/**
 * Writes a JStmtDo.
 */
public void writeJStmtDo(JStmtDo aStmt)
{
    append("do ");
    JStmt stmt = aStmt.getStatement();
    writeJStmt(stmt);
    JExpr cond = aStmt.getConditional();
    append("while(");
    writeJExpr(cond);
    append(");").endln();
}

/**
 * Writes a JStmtEmpty.
 */
public void writeJStmtEmpty(JStmtEmpty aStmt)
{
    append(';').endln();
}

/**
 * Writes a JStmtExpr.
 */
public void writeJStmtExpr(JStmtExpr aStmt)
{
    writeJExpr(aStmt.getExpr());
    append(';').endln();
}

/**
 * Writes a JStmtIf.
 */
public void writeJStmtIf(JStmtIf aStmt)
{
    // Write if(), conditional and statement
    append("if(");
    writeJExpr(aStmt.getConditional());
    append(") ");
    writeJStmt(aStmt.getStatement());
    
    // Write else
    if(aStmt.getElseStatement()!=null) {
        append("else ");
        writeJStmt(aStmt.getElseStatement());
    }
}

/**
 * Writes a JStmtFor.
 */
public void writeJStmtFor(JStmtFor aStmt)
{
    JStmtVarDecl init = aStmt.getInitDecl();
    JVarDecl initVD = init!=null? init.getVarDecls().get(0) : null;
    JExpr cond = aStmt.getConditional();
    
    append("for(");

    // Handle for(each)
    if(aStmt.isForEach()) {
        append("let ").append(initVD.getName()).append(" of ");
        writeJExpr(cond);
    }
    
    // Handle conventional for()
    else {
        if(initVD!=null) {
            if(initVD.getType()!=null) append("let ");
            writeJVarDecl(initVD);
        }
        append(';');
        if(cond!=null)
            writeJExpr(cond);
        append(';');
        List <JStmtExpr> updStmts = aStmt.getUpdateStmts();
        JStmtExpr last = updStmts.size()>0? updStmts.get(updStmts.size()-1) : null;
        for(JStmtExpr updStmt : updStmts) {
            writeJExpr(updStmt.getExpr()); if(updStmt!=last) append(", "); }
    }

    // Write for closing paren and statement    
    append(") ");
    JStmt stmt = aStmt.getStatement();
    if(stmt instanceof JStmtBlock)
        writeJStmt(stmt);
    else {
        endln().indent();
        writeJStmt(stmt);
        outdent();
    }
}

/**
 * Writes a JStmtLabeled.
 */
public void writeJStmtLabeled(JStmtLabeled aStmt)
{
    JExprId label = aStmt.getLabel();
    writeJExprId(label);
    append(':');
    JStmt stmt = aStmt.getStmt();
    if(stmt!=null) {
        append(' '); writeJStmt(stmt); }
    else endln();
}

/**
 * Writes a JStmtReturn.
 */
public void writeJStmtReturn(JStmtReturn aStmt)
{
    append("return");
    JExpr expr = aStmt.getExpr();
    if(expr!=null) { append(' '); writeJExpr(expr); }
    append(';').endln();
}

/**
 * Writes a JStmtSwitch.
 */
public void writeJStmtSwitch(JStmtSwitch aStmt)
{
    JExpr expr = aStmt.getExpr();
    append("switch("); writeJExpr(expr); append(") {").endln();
    indent();
    for(JStmtSwitch.SwitchLabel lbl : aStmt.getSwitchLabels())
        writeJStmtSwitchLabel(lbl);
    outdent();
    append('}').endln();
}

/**
 * Writes a JStmtSwitch.
 */
public void writeJStmtSwitchLabel(JStmtSwitch.SwitchLabel aSL)
{
    if(aSL.isDefault()) append("default: ");
    else { append("case "); writeJExpr(aSL.getExpr()); append(": "); }
    for(JStmt stmt : aSL.getStatements())
        writeJStmt(stmt);
}

/**
 * Writes a JStmtSynchronized.
 */
public void writeJStmtSynchronized(JStmtSynchronized aStmt)
{
    writeJStmtBlock(aStmt.getBlock(), true);
}

/**
 * Writes a JStmtThrow.
 */
public void writeJStmtThrow(JStmtThrow aStmt)
{
    append("throw ");
    writeJExpr(aStmt.getExpr());
    append(';').endln();
}

/**
 * Writes a JStmtTry.
 */
public void writeJStmtTry(JStmtTry aStmt)
{
    JStmtBlock tryBlock = aStmt.getTryBlock();
    List <JStmtTry.CatchBlock> catchBlocks = aStmt.getCatchBlocks();
    JStmtBlock finBlock = aStmt.getFinallyBlock();

    // Write try block
    append("try "); writeJStmtBlock(tryBlock, false);
        
    // Write catch blocks
    //for(JStmtTry.CatchBlock cb : catchBlocks) {
    if(catchBlocks.size()>0) { JStmtTry.CatchBlock cb = catchBlocks.get(0);
        append("catch(").append(cb.getParameter().getName()).append(") ");
        writeJStmtBlock(cb.getBlock(), false);
    }
    
    // Write finally block
    if(finBlock!=null) {
        append("finally "); writeJStmtBlock(finBlock, false); }
}

/**
 * Writes a JStmtVarDecl.
 */
public void writeJStmtVarDecl(JStmtVarDecl aStmt)
{
    for(JVarDecl vd : aStmt.getVarDecls()) {
        append("let ");
        writeJVarDecl(vd);
        append(';').endln();
    }
}

/**
 * Writes a JStmtWhile.
 */
public void writeJStmtWhile(JStmtWhile aStmt)
{
    // Write "while", conditional and statement
    append("while(");
    writeJExpr(aStmt.getConditional());
    append(") ");
    writeJStmt(aStmt.getStmt());
}

/**
 * Writes a JExpr.
 */
public void writeJExpr(JExpr aExpr)
{
    if(aExpr instanceof JExprAlloc) writeJExprAlloc((JExprAlloc)aExpr);
    else if(aExpr instanceof JExprArrayIndex) writeJExprArrayIndex((JExprArrayIndex)aExpr);
    else if(aExpr instanceof JExprChain) writeJExprChain((JExprChain)aExpr);
    else if(aExpr instanceof JExpr.CastExpr) writeJExprCast((JExpr.CastExpr)aExpr);
    else if(aExpr instanceof JExprId) writeJExprId((JExprId)aExpr);
    else if(aExpr instanceof JExpr.InstanceOfExpr) writeJExprInstanceOf((JExpr.InstanceOfExpr)aExpr);
    else if(aExpr instanceof JExprLambda) writeJExprLambda((JExprLambda)aExpr);
    else if(aExpr instanceof JExprLiteral) writeJExprLiteral((JExprLiteral)aExpr);
    else if(aExpr instanceof JExprMath) writeJExprMath((JExprMath)aExpr);
    else if(aExpr instanceof JExprMethodCall) writeJExprMethodCall((JExprMethodCall)aExpr);
    else if(aExpr instanceof JExprMethodRef) writeJExprMethodRef((JExprMethodRef)aExpr);
    else if(aExpr instanceof JExprType) writeJExprType((JExprType)aExpr);
    else throw new RuntimeException("TSWriter.writeJExpr: Unsupported expression " + aExpr.getClass());
    //else append(aExpr.getString());
}

/**
 * Writes a JExprAlloc.
 */
public void writeJExprAlloc(JExprAlloc aExpr)
{
    // Append 'new' keyword, type and parameter list start char
    JType typ = aExpr.getType();
    append("new "); writeJType(typ);
    append('(');
    
    // Append args
    List <JExpr> args = aExpr.getArgs(); JExpr last = args.size()>0? args.get(args.size()-1) : null;
    for(JExpr arg : aExpr.getArgs()) {
        writeJExpr(arg); if(arg!=last) append(", "); }
        
    // Append close char
    append(')');
    
    // Append ClassDecl
    if(aExpr.getClassDecl()!=null) {
        System.err.println("Need to write ClassDecl for " + aExpr.getClassDecl().getClassName());
    }
}

/**
 * Writes a JExprArrayIndex.
 */
public void writeJExprArrayIndex(JExprArrayIndex aExpr)
{
    JExpr expr = aExpr.getArrayExpr();
    writeJExpr(expr);
    JExpr iexpr = aExpr.getIndexExpr();
    append('['); writeJExpr(iexpr); append(']');
}

/**
 * Writes a JExpr.CastExpr.
 */
public void writeJExprCast(JExpr.CastExpr aExpr)
{
    append('(').append('<');
    writeJType(aExpr.getType());
    append('>');
    writeJExpr(aExpr.getExpr());
    append(')');
}

/**
 * Writes a JExprChain.
 */
public void writeJExprChain(JExprChain aExpr)
{
    // Write component expressions
    List <JExpr> exprs = aExpr.getExpressions(); JExpr last = exprs.get(exprs.size()-1);
    for(JExpr exp : exprs) {
        writeJExpr(exp); if(exp!=last) append('.'); }
}

/**
 * Writes a JExprId.
 */
public void writeJExprId(JExprId aExpr)
{
    // If id is field or method reference and orphan, append "this." (or simple class name if static)
    if(aExpr.isFieldRef() && aExpr.getParentExpr()==null) { JavaDecl decl = aExpr.getDecl();
        if(decl.isStatic()) {
            if(!decl.getClassName().startsWith("jsweet.")) append(decl.getClassSimpleName()).append('.'); }
        else append("this.");
    }
    if(aExpr.isMethodCall() && aExpr.getMethodCall().getParentExpr()==null) { JavaDecl decl = aExpr.getDecl();
        if(decl.isStatic()) append(decl.getClassSimpleName()).append('.');
        else append("this.");
    }
    
    // Append id
    String str = aExpr.getName();
    if(str.equals("in")) str = "__in";
    else if(str.equals("function")) str = "__function";
    append(str);
}

/**
 * Writes a JExpr.InstanceOfExpr.
 */
public void writeJExprInstanceOf(JExpr.InstanceOfExpr aExpr)
{
    // Get the type and type string
    JExpr expr = aExpr.getExpr();
    JType typ = aExpr.getType();
    String tstr = getTypeString(typ);
    
    // Handle primitive types
    if(tstr.equals("number") || tstr.equals("string") || tstr.equals("boolean")) {
        append("typeof "); writeJExpr(expr); append(" === \'").append(tstr).append('\'');
        return;
    }
    
    // Get expr and write: expr!=null && expr instanceof
    writeJExpr(expr); append("!=null && ");
    writeJExpr(expr); append(" instanceof ");
    
    // Get write type
    writeJType(typ);
}

/**
 * Writes a JExprLambda.
 */
public void writeJExprLambda(JExprLambda aExpr)
{
    // Write parameters
    append('(');
    List <JVarDecl> params = aExpr.getParams(); JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : aExpr.getParams()) {
        append(param.getName()); if(param!=last) append(','); }
    append(')');
    
    // Write fat arrow
    append(" => ");
    
    // Write expression or statement block
    if(aExpr.getExpr()!=null)
        writeJExpr(aExpr.getExpr());
    else writeJStmtBlock(aExpr.getBlock(), false);
}

/**
 * Writes a JExprLiteral.
 */
public void writeJExprLiteral(JExprLiteral aExpr)
{
    String str = aExpr.getValueString();
    JExprLiteral.LiteralType typ = aExpr.getLiteralType();
    if(typ==JExprLiteral.LiteralType.Long || typ==JExprLiteral.LiteralType.Float ||
        typ==JExprLiteral.LiteralType.Double) {
        char c = str.charAt(str.length()-1);
        if(Character.isLetter(c)) str = str.substring(0, str.length()-1);
    }
    append(str);
}

/**
 * Writes a JExprMath.
 */
public void writeJExprMath(JExprMath aExpr)
{
    JExprMath.Op op = aExpr.getOp();
    
    // Handle basic binary operations
    if(aExpr.getOperandCount()==2) {
        JExpr exp0 = aExpr.getOperand(0), exp1 = aExpr.getOperand(1);
        writeJExpr(exp0);
        append(' ').append(JExprMath.getOpString(op)).append(' ');
        writeJExpr(exp1);
    }
    
    // Handle conditional
    else if(op==JExprMath.Op.Conditional) {
        JExpr exp0 = aExpr.getOperand(0), exp1 = aExpr.getOperand(1), exp2 = aExpr.getOperand(2);
        writeJExpr(exp0);
        append("? ");
        writeJExpr(exp1);
        append(" : ");
        writeJExpr(exp2);
    }
    
    // Handle unary pre
    else if(op==JExprMath.Op.Negate || op==JExprMath.Op.Not ||
        op==JExprMath.Op.PreDecrement || op==JExprMath.Op.PreIncrement) {
        JExpr exp0 = aExpr.getOperand(0);
        append(JExprMath.getOpString(op));
        writeJExpr(exp0);
    }
    
    // Handle unary post
    else if(op==JExprMath.Op.PostDecrement || op==JExprMath.Op.PostIncrement) {
        JExpr exp0 = aExpr.getOperand(0);
        append(JExprMath.getOpString(op));
        writeJExpr(exp0);
    }
    
    else throw new RuntimeException("TSWriter.writeJExprMath: Unsupported op: " + op);
}

/**
 * Writes a JExprMethodCall.
 */
public void writeJExprMethodCall(JExprMethodCall aExpr)
{
    // If special, just return
    if(writeJExprMethodCallSpecial(aExpr)) return;
    
    // Append name and open char
    JExprId id = aExpr.getId(); String name = id.getName();
    writeJExprId(id); append('(');
    
    // Append args
    List <JExpr> args = aExpr.getArgs(); JExpr last = args.size()>0? args.get(args.size()-1) : null;
    for(JExpr arg : aExpr.getArgs()) {
        writeJExpr(arg); if(arg!=last) append(", "); }
        
    // Append close char
    append(')');
}

/**
 * Writes a JExprMethodCall.
 */
public boolean writeJExprMethodCallSpecial(JExprMethodCall aExpr)
{
    JExprId id = aExpr.getId(); String name = id.getName();
    
    // Handle jsweet.util.Globals.union()
    if(name.equals("union")) {
        JavaDecl decl = id.getDecl(); if(decl==null || !decl.getClassName().startsWith("jsweet.")) return false;
        JExpr arg = aExpr.getArgs().get(0);
        append("(<any>"); writeJExpr(arg); append(')');
        return true;
    }
    
    // Handle jsweet.util.Globals.function()
    if(name.equals("function")) {
        JavaDecl decl = id.getDecl(); if(decl==null || !decl.getClassName().startsWith("jsweet.")) return false;
        JExpr arg = aExpr.getArgs().get(0);
        writeJExpr(arg);
        return true;
    }
    
    return false;
}

/**
 * Writes a JExprMethodRef.
 */
public void writeJExprMethodRef(JExprMethodRef aExpr)
{
    System.out.println("TSWriter: Need to write method ref: " + aExpr.getFile().getClassName());
}

/**
 * Writes a JExprType.
 */
public void writeJExprType(JExprType aExpr)
{
    JType typ = aExpr.getType();
    writeJType(typ);
}

/**
 * Append String.
 */
public TypeWriter append(String aStr)  { cd(); _sb.append(aStr); return this; }

/**
 * Append char.
 */
public TypeWriter append(char aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append Int.
 */
public TypeWriter append(int aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append Double.
 */
public TypeWriter append(double aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append newline.
 */
public TypeWriter endln()  { _sb.append('\n'); _lineStart = true; return this; }

/**
 * Append indent.
 */
public TypeWriter indent()  { _indent++; return this; }

/**
 * Append indent.
 */
public TypeWriter outdent()  { _indent--; return this; }

/**
 * Append indent.
 */
public TypeWriter appendIndent()  { for(int i=0;i<_indent;i++) _sb.append(_indentStr); return this; }

/**
 * Checks for indent.
 */
protected void cd()  { if(_lineStart) appendIndent(); _lineStart = false; }

}