/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.util.*;
import snap.parse.Token;
import snap.util.*;

/**
 * The base class for all nodes of a JFile.
 */
public class JNode {

    // The name for this node (if it has one)
    String              _name;
    
    // The start/end tokens for this node
    Token               _startToken, _endToken;
    
    // The parent node
    JNode               _parent;

    // The list of child nodes
    List <JNode>        _children = Collections.EMPTY_LIST;
    
    // The declaration for this node
    JavaDecl            _decl;

/**
 * Returns the parent file node (root).
 */
public JFile getFile()  { return getParent(JFile.class); }

/**
 * Returns the node name, if it has one.
 */
public String getName()  { return _name!=null? _name : (_name=getNameImpl()); }

/**
 * Resolves the name, if possible.
 */
protected String getNameImpl()  { return null; }

/**
 * Returns the class loader used to resolve classes.
 */
public ClassLoader getClassLoader()  { return getFile().getClassLoader(); }

/**
 * Returns the class for given name.
 */
public Class getClassForName(String aName)
{
    ClassLoader cldr = getClassLoader();
    return ClassUtils.getClass(aName, cldr);
}

/**
 * Returns whether given class name is known.
 */
public boolean isKnownClassName(String aName)  { return getClassForName(aName)!=null; }

/**
 * Returns whether given class name is known.
 */
public boolean isKnownPackageName(String aName)
{
    // Bite me
    if(aName.startsWith("java") && aName.equals(aName.toLowerCase())) {
        if(aName.equals("java") || aName.startsWith("java.")) return true;
        if(aName.equals("javax") || aName.startsWith("javax.")) return true;
        if(aName.equals("javafx") || aName.startsWith("javafx.")) return true;
    }
    
    ClassLoader cldr = getClassLoader();
    String rname = aName.replace('.', '/');
    java.net.URL url = cldr.getResource(rname);
    return url!=null;
}

/**
 * Returns the enclosing class.
 */
public JClassDecl getEnclosingClassDecl()  { return getParent(JClassDecl.class); }

/**
 * Returns the enclosing method declaration, if in method.
 */
public JMethodDecl getEnclosingMethodDecl()  { return getParent(JMethodDecl.class); }

/**
 * Returns the enclosing member declaration, if in member.
 */
public JMemberDecl getEnclosingMemberDecl()  { return getParent(JMemberDecl.class); }

/**
 * Returns the class name for this node, if it has one.
 */
public String getClassName()
{
    JavaDecl decl = getDecl(); if(decl==null) return null;
    if(decl.isClass() || decl.isConstructor())
        return decl.getClassName();
    if(decl.isField() || decl.isMethod())
        return decl.getTypeName();
    if(decl.isVarDecl())
        return decl.getVarDecl().getClassName();
    return null;
}

/**
 * Returns the Class of this node, if it has one.
 */
public Class getJClass()
{
    String cname = getClassName(); if(cname==null) return null;
    Class cls = getClassForName(cname);
    if(cls==null)
        System.err.println("JNode.getJClass: Couldn't find class for name " + cname);
    return cls;
}

/**
 * Returns whether node is a declaration name (JClassDecl JMethodDecl, JFieldDecl, JVarDecl).
 */
public boolean isDecl()
{
    JExprId id = this instanceof JExprId? (JExprId)this : null; if(id==null) return false;
    JNode par = id.getParent();
    return par instanceof JMemberDecl || par instanceof JEnumConst || par instanceof JVarDecl;
}

/**
 * Tries to determine IdentiferType.
 */
public JavaDecl getDecl()  { return _decl!=null? _decl : (_decl=getDeclImpl()); }

/**
 * Returns a JavaDeclRef for a JNode.
 */
protected JavaDecl getDeclImpl()  { return null; }

/**
 * Returns the enclosing JavaDecl (JVarDecl, JConstrDecl, JMethodDecl or JClassDecl).
 */
public JavaDecl getEnclosingDecl()  { JNode n = getEnclosingDeclNode(); return n!=null? n.getDecl() : null; }

/**
 * Returns the enclosing JavaDecl (JVarDecl, JConstrDecl, JMethodDecl or JClassDecl).
 */
public JNode getEnclosingDeclNode()
{
    JNode node = getParent(JMemberDecl.class);
    if(node instanceof JFieldDecl && getParent(JVarDecl.class)!=null) node = getParent(JVarDecl.class);
    return node;
}

/**
 * Returns the start token of this node.
 */
public Token getStartToken()  { return _startToken; }

/**
 * Sets the start token of this node.
 */
public void setStartToken(Token aToken)  { _startToken = aToken; }

/**
 * Returns the start char index of this node.
 */
public int getStart()  { return _startToken!=null? _startToken.getInputStart() : 0; }

/**
 * Returns the end token of this node.
 */
public Token getEndToken()  { return _endToken; }

/**
 * Sets the end token of this node.
 */
public void setEndToken(Token aToken)  { _endToken = aToken; }

/**
 * Returns the end char index of this node.
 */
public int getEnd()  { return _endToken!=null? _endToken.getInputEnd() : 0; }

/**
 * Returns the line index of this node.
 */
public int getLineIndex()  { return _startToken.getLineIndex(); }

/**
 * Returns the parent node.
 */
public JNode getParent()  { return _parent; }

/**
 * Sets the parent node.
 */
public void setParent(JNode aParent)  { _parent = aParent; }

/**
 * Returns the parent node of given class.
 */
public <T> T getParent(Class<T> aClass)
{
    return _parent==null || aClass.isInstance(_parent)? (T)_parent : _parent.getParent(aClass);
}

/**
 * Returns the number of child nodes.
 */
public final int getChildCount()  { return getChildren().size(); }

/**
 * Returns the individual child node at given index.
 */
public final JNode getChild(int anIndex)  { return getChildren().get(anIndex); }

/**
 * Returns the array of child nodes.
 */
public List <JNode> getChildren()  { return _children; }

/**
 * Add child node to list.
 */
protected void addChild(JNode aNode)  { addChild(aNode, -1); }

/**
 * Add child node to list.
 */
protected void addChild(JNode aNode, int anIndex)
{
    if(aNode==null) return; if(anIndex<0) anIndex = _children.size();
    if(_children==Collections.EMPTY_LIST) _children = new ArrayList();
    _children.add(anIndex, aNode); aNode.setParent(this);
    if(getStartToken()==null || getStart()>aNode.getStart()) setStartToken(aNode.getStartToken());
    if(getEndToken()==null || getEnd()<aNode.getEnd()) setEndToken(aNode.getEndToken());
    //for(JNode n=this; n!=null; n=n.getParent()) n._string = null;
}

/**
 * Removes a child.
 */
protected int removeChild(JNode aNode)
{
    if(aNode==null) return -1;
    int index = ListUtils.indexOfId(_children, aNode);
    if(index>=0) _children.remove(index);
    //for(JNode n=this; n!=null; n=n.getParent()) n._string = null;
    return index;
}

/**
 * Replaces a given child with a new child - though this is mostly used to add.
 */
protected void replaceChild(JNode oNode, JNode nNode)
{
    int index = oNode!=null? removeChild(oNode) : -1;
    addChild(nNode, index);
}

/**
 * Returns whether statement has a block associated with it.
 */
public boolean isBlock()  { return false; }

/**
 * Returns the statement block.
 */
public JStmtBlock getBlock()  { return null; }

/**
 * Returns the statement block.
 */
public void setBlock(JStmtBlock aBlock)  { }

/**
 * Returns the node at given char index.
 */
public JNode getNodeAtCharIndex(int anIndex)
{
    // Iterate over nodes and recurse in to one in range (return top level node in range)
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { JNode node = getChild(i);
        if(node.getStart()<=anIndex && anIndex<=node.getEnd())
            return node.getNodeAtCharIndex(anIndex); }
    return this; // Return this node
}

/**
 * Returns the node at given char index.
 */
public JNode getNodeAtCharIndex(int aStart, int anEnd)
{
    // Iterate over nodes and recurse in to one in range (return top level node in range)
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { JNode node = getChild(i);
        if(node.getStart()<=aStart && anEnd<=node.getEnd())
            return node.getNodeAtCharIndex(aStart, anEnd); }
    return this; // Return this node
}

/**
 * Resolve a name.
 */
protected JavaDecl resolveName(JNode aNode)  { return _parent!=null? _parent.resolveName(aNode) : null; }

/**
 * Fills a given list with variables that start with given prefix.
 */
public List <JVarDecl> getVarDecls(String aPrefix, List <JVarDecl> theVariables)
{
    if(_parent!=null) _parent.getVarDecls(aPrefix, theVariables);
    return theVariables;
}

/**
 * Returns the node path.
 */
public List <JNode> getNodeParents()
{
    List <JNode> parents = new ArrayList();
    for(JNode parent=getParent(); parent!=null; parent=parent.getParent())
        parents.add(0, parent);
    return parents;
}

/**
 * Returns the node parent path, with separator.
 */
public String getNodePath(String aSep)
{
    List <JNode> parents = getNodeParents();
    StringBuffer sb = new StringBuffer();
    for(JNode parent : parents) sb.append(parent.getNodeString()).append(aSep);
    sb.append(getNodeString());
    return sb.toString();
}

/**
 * Returns the node name.
 */
public String getNodeString()  { return getClass().getSimpleName(); }

/**
 * Returns the string for this node (from Token.Tokenizer.getInput(Start,End)).
 */
public String getString()  { return new JavaWriter().getString(this); }

/**
 * Standard toString implementation.
 */
public String toString()
{
    StringBuffer sb = new StringBuffer(getClass().getSimpleName()).append(" { ");
    if(getFile()!=null) sb.append("File:").append(getFile().getName()).append(", ");
    sb.append("Line:").append(getLineIndex()).append(", Start:").append(getStart()).append(", End:").append(getEnd());
    if(getName()!=null && getName().length()>0) sb.append(", Name:").append(getName());
    sb.append(" } ");
    String str = getString(); int ind = str.indexOf('\n'); if(ind>0) str = str.substring(0,ind) + " ...";
    sb.append(str);
    return sb.toString();
}

}
