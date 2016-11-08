/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.project.Project;
import snap.util.*;

/**
 * A class to represent a declaration of a Java Class, Method, Field or Constructor.
 */
public class JavaDecl implements Comparable<JavaDecl> {

    // The type
    Type           _type;
    
    // The class name of this declaration
    String         _cname;
    
    // The name of the declaration member
    String         _name;
    
    // The simple name of the declaration member
    String         _sname;
    
    // The type of the declaration member
    String         _tname;
    
    // A string description of arg types
    String         _argTypeNames[];
    
    // The modifier
    int            _modifier;
    
    // The package name
    String         _pname;
    
    // The VariableDecl
    JVarDecl       _vdecl;
    
    // Constants for type
    public enum Type { Class, Field, Constructor, Method, Package, VarDecl }
    
/**
 * Creates a new JavaDecl for class.
 */
public JavaDecl(Class aClass)
{
    _type = Type.Class; _cname = _name = getTypeName(aClass); _sname = aClass.getSimpleName();
    _modifier = aClass.getModifiers();
}

/**
 * Creates a new JavaDecl for field.
 */
public JavaDecl(Field aField)
{
    _type = Type.Field; _name = _sname = aField.getName(); _cname = aField.getDeclaringClass().getName();
    _tname = getTypeName(aField.getGenericType()); _modifier = aField.getModifiers();
}

/**
 * Creates a new JavaDecl for constructor.
 */
public JavaDecl(Constructor aConstr)
{
    _type = Type.Constructor; _name = _cname = _tname = aConstr.getName();
    _sname = aConstr.getDeclaringClass().getSimpleName();
    _argTypeNames = new String[aConstr.getParameterCount()];
    
    // Get GenericParameterTypes and names (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
    java.lang.reflect.Type ptypes[] = aConstr.getGenericParameterTypes();
    for(int i=0,iMax=aConstr.getParameterCount(); i<iMax; i++)
        _argTypeNames[i] = ptypes.length==iMax? getTypeName(ptypes[i]) : getTypeName(aConstr.getParameterTypes()[i]);
    _modifier = aConstr.getModifiers();
}

/**
 * Creates a new JavaDecl for method.
 */
public JavaDecl(Method aMethod)
{
    _type = Type.Method; _name = _sname = aMethod.getName(); _cname = aMethod.getDeclaringClass().getName();
    _argTypeNames = new String[aMethod.getParameterCount()];
    for(int i=0,iMax=aMethod.getParameterCount(); i<iMax; i++)
        _argTypeNames[i] = getTypeName(aMethod.getGenericParameterTypes()[i]);
    _tname = getTypeName(aMethod.getGenericReturnType()); _modifier = aMethod.getModifiers();
}

/**
 * Creates a new JavaDecl.
 */
public JavaDecl(String aClsName, String aMmbrName, String aTypeName, String theMmbrTypes[])
{
    // Handle Class
    if(aClsName!=null && aMmbrName==null && theMmbrTypes==null) {
        _type = Type.Class; _cname = _name = aClsName; _sname = getSimpleName(aClsName); }
    
    // Handle Field
    else if(aClsName!=null && aMmbrName!=null && theMmbrTypes==null) {
        _type = Type.Field; _cname = aClsName; _name = _sname = aMmbrName; _tname = aTypeName; }
    
    // Handle Constructor
    else if(aClsName!=null && (aMmbrName==null || aMmbrName.equals("<init>")) && theMmbrTypes!=null) {
        _type = Type.Constructor; _cname = _name = _tname = aClsName; _argTypeNames = theMmbrTypes;
        _sname = getSimpleName(aClsName);
    }
    
    // Handle Method
    else if(aClsName!=null && aMmbrName!=null && theMmbrTypes!=null) {
        _type = Type.Method; _cname = aClsName; _name = _sname = aMmbrName;
        _tname = aTypeName; _argTypeNames = theMmbrTypes;
    }
    
    // If anything else, throw exception
    else throw new RuntimeException("Unknown type for " + aClsName + ", " + aMmbrName + ", " + theMmbrTypes);
}

/**
 * Creates a new JavaDecl.
 */
public JavaDecl(Type aType, Object anObj)
{
    _type = aType;
    if(_type==Type.Package) {
        _pname = _name = (String)anObj; _sname = getSimpleName(_pname); }
    else if(_type==Type.VarDecl) {
        _vdecl = (JVarDecl)anObj; _sname = _name = _vdecl.getName(); _tname = _vdecl.getClassName(); }
    else throw new RuntimeException("Unknown type for " + aType + ", " + anObj);
}
        
/**
 * Returns the type.
 */
public Type getType()  { return _type; }

/**
 * Returns whether is a class reference.
 */
public boolean isClass()  { return _type==Type.Class; }

/**
 * Returns whether is a field reference.
 */
public boolean isField()  { return _type==Type.Field; }

/**
 * Returns whether is a constructor reference.
 */
public boolean isConstructor()  { return _type==Type.Constructor; }

/**
 * Returns whether is a method reference.
 */
public boolean isMethod()  { return _type==Type.Method; }

/**
 * Returns whether is a package reference.
 */
public boolean isPackage()  { return _type==Type.Package; }

/**
 * Returns whether is a variable declaration reference.
 */
public boolean isVarDecl()  { return _type==Type.VarDecl; }

/**
 * Returns the name.
 */
public String getName()  { return _name; }

/**
 * Returns the simple name.
 */
public String getSimpleName()  { return _sname; }

/**
 * Returns the class name.
 */
public String getClassName()  { return _cname; }

/**
 * Returns the class name.
 */
public String getClassSimpleName()  { return getSimpleName(_cname); }

/**
 * Returns the parent class name.
 */
public String getParentClassName()  { return _cname!=null? getParentClassName(_cname) : null; }

/**
 * Returns the top level class name.
 */
public String getRootClassName()  { return _cname!=null? getRootClassName(_cname) : null; }

/**
 * Returns whether class is member.
 */
public boolean isMemberClass()  { return _cname!=null? _cname.indexOf('$')>0 : false; }

/**
 * Returns the type name (for method or field).
 */
public String getTypeName()  { return _tname; }

/**
 * Returns the simple type name.
 */
public String getTypeSimpleName()  { return getSimpleName(_tname); }

/**
 * Returns the package name.
 */
public String getPackageName() { return _pname!=null? _pname : (_pname=getPackageName(_cname)); }

/**
 * Returns the variable declaration name.
 */
public JVarDecl getVarDecl() { assert(isVarDecl()); return _vdecl; }

/**
 * Returns a name suitable to describe declaration.
 */
public String getPrettyName()
{
    String name = _cname;
    if(_type==Type.Method || _type==Type.Field) name += '.' + _name;
    if(_type==Type.Method || _type==Type.Constructor) {
        String names[] = Arrays.copyOf(_argTypeNames, _argTypeNames.length);
        for(int i=0;i<names.length;i++) names[i] = getSimpleName(names[i]);
        name +=  '(' + StringUtils.join(names, ",") + ')';
    }
    if(_type==Type.Package) return _pname;
    if(_type==Type.VarDecl) return _name;
    return name;
}

/**
 * Returns a name unique for matching declarations.
 */
public String getMatchName()
{
    String name = _cname;
    if(_type==Type.Method || _type==Type.Field) name += '.' + _name;
    if(_type==Type.Method || _type==Type.Constructor) name +=  '(' + StringUtils.join(_argTypeNames, ",") + ')';
    if(_type==Type.Package) return _pname;
    if(_type==Type.VarDecl) return _name;
    return name;
}

/**
 * Returns the full name.
 */
public String getFullName()
{
    if(_fname!=null) return _fname;
    String name = getMatchName();
    if(_type==Type.Method || _type==Type.Field) name = _tname + " " + name;
    String mstr = Modifier.toString(_modifier); if(mstr.length()>0) name = mstr + " " + name;
    return _fname=name;
} String _fname;

/**
 * Returns a string representation of suggestion.
 */
public String getSuggestionString()
{
    StringBuffer sb = new StringBuffer(getSimpleName());
    switch(getType()) {
        case Constructor:
        case Method: {
            String names[] = Arrays.copyOf(_argTypeNames, _argTypeNames.length);
            for(int i=0;i<names.length;i++) names[i] = getSimpleName(names[i]);
            sb.append('(').append(StringUtils.join(names, ",")).append(')');
        }
        case VarDecl: case Field:
            if(getTypeName()!=null) sb.append(" : ").append(getTypeSimpleName());
            if(getClassName()!=null) sb.append(" - ").append(getClassSimpleName());
            break;
        case Class: sb.append(" - ").append(getParentClassName()); break;
        case Package: break;

        default:  throw new RuntimeException("Unsupported Type " + getType());
    }

    // Return string
    return sb.toString();
}

/**
 * Returns the string to use when inserting this suggestion into code.
 */
public String getReplaceString()
{
    switch(getType()) {
        case Class: return getSimpleName();
        case Constructor: return getPrettyName().replace(getParentClassName() + '.', "");
        case Method: return getPrettyName().replace(_cname + '.', "");
        case Package: {
            String name = getPackageName(); int index = name.lastIndexOf('.');
            return index>0? name.substring(index+1) : name;
        }
        default: return getName();
    }
}

/**
 * Returns the class or declaring class using the given project.
 */
public Class getDeclClass(Project aProj)
{
    ClassLoader cldr = aProj!=null? aProj.getClassLoader() : ClassLoader.getSystemClassLoader();
    return ClassUtils.getClass(_cname, cldr);
}

/**
 * Returns whether given declaration collides with this declaration.
 */
public boolean matches(Project aProj, JavaDecl aDecl)
{
    if(aDecl._type!=_type) return false;
    if(!aDecl._name.equals(_name)) return false;
    if(!Arrays.equals(aDecl._argTypeNames, _argTypeNames)) return false;
    
    // If field or method, see if declaring class matches
    if(isField() || isConstructor() || isMethod()) {
        Class c1 = getDeclClass(aProj), c2 = aDecl.getDeclClass(aProj);
        return c1==c2 || c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1);
    }
    
    return true;
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    if(anObj==this) return true;
    JavaDecl decl = anObj instanceof JavaDecl? (JavaDecl)anObj : null; if(decl==null) return false;
    if(decl._type!=_type) return false;
    if(decl._modifier!=_modifier) return false;
    if(!decl._name.equals(_name)) return false;
    if(!SnapUtils.equals(decl._cname,_cname)) return false;
    if(_tname!=null && !_tname.equals(decl._tname)) return false;
    if(!Arrays.equals(decl._argTypeNames, _argTypeNames)) return false;
    return true;
}

/**
 * Standard compareTo implementation.
 */
public int compareTo(JavaDecl aDecl)
{
    int t1 = _type.ordinal(), t2 = aDecl._type.ordinal();
    if(t1<t2) return -1; if(t2<t1) return 1;
    return getMatchName().compareTo(aDecl.getMatchName());
}

/**
 * Standard hashcode implementation.
 */
public int hashCode()  { return getFullName().hashCode(); }

/**
 * Standard toString implementation.
 */
public String toString()  { return getFullName(); }

/**
 * Returns reference nodes in given JNode that match given JavaDecl.
 */
public static void getMatches(Project aProj, JNode aNode, JavaDecl aDecl, List <JNode> theMatches)
{
    // If JType check name
    if(aNode instanceof JType || aNode instanceof JExprId) {
        JavaDecl decl = isPossibleMatch(aNode, aDecl)? aNode.getDecl() : null;
        if(decl!=null && decl.matches(aProj, aDecl))
            theMatches.add(aNode);
    }
 
    // Recurse
    for(JNode child : aNode.getChildren())
        getMatches(aProj, child, aDecl, theMatches);
}
    
/**
 * Returns reference nodes in given JNode that match given JavaDecl.
 */
public static void getRefMatches(Project aProj, JNode aNode, JavaDecl aDecl, List <JNode> theMatches)
{
    // If JType check name
    if(aNode instanceof JType || aNode instanceof JExprId) {
        if(isPossibleMatch(aNode, aDecl) && !aNode.isDecl()) {
            JavaDecl decl = aNode.getDecl();
            if(decl!=null && decl.matches(aProj, aDecl) && aNode.getParent(JImportDecl.class)==null)
                theMatches.add(aNode);
        }
    }
 
    // Recurse
    for(JNode child : aNode.getChildren())
        getRefMatches(aProj, child, aDecl, theMatches);
}
    
/**
 * Returns declaration nodes in given JNode that match given JavaDecl.
 */
public static JNode getDeclMatch(Project aProj, JNode aNode, JavaDecl aDecl)
{
    List <JNode> matches = new ArrayList(); getDeclMatches(aProj, aNode, aDecl, matches);
    return matches.size()>0? matches.get(0) : null;
}

/**
 * Returns declaration nodes in given JNode that match given JavaDecl.
 */
public static void getDeclMatches(Project aProj, JNode aNode, JavaDecl aDecl, List <JNode> theMatches)
{
    // If JType check name
    if(aNode instanceof JType || aNode instanceof JExprId) {
        JavaDecl decl = aNode.isDecl() && isPossibleMatch(aNode, aDecl)? aNode.getDecl() : null;
        if(decl!=null && decl.matches(aProj, aDecl))
            theMatches.add(aNode);
    }
 
    // Recurse
    for(JNode child : aNode.getChildren())
        getDeclMatches(aProj, child, aDecl, theMatches);
}
    
/** Returns whether node is a possible match. */
private static boolean isPossibleMatch(JNode aNode, JavaDecl aDecl)
{
    if(aNode instanceof JType) { JType type = (JType)aNode;
        if(type.getSimpleName().equals(aDecl.getSimpleName()))
            return true; }
    else if(aNode instanceof JExprId) { JExprId id = (JExprId)aNode;
        if(id.getName().equals(aDecl.getSimpleName()))
            return true; }
    return false;
}

/** Returns the class name, converting primitive arrays to 'int[]' instead of '[I'. */
private String getTypeName(java.lang.reflect.Type aType)
{
    if(aType instanceof Class)
        return getTypeName((Class)aType);
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        return getTypeName(gat.getGenericComponentType()) + "[]"; }
    if(aType instanceof ParameterizedType)
        return getTypeName(((ParameterizedType)aType).getRawType());
    if(aType instanceof TypeVariable)
        return getTypeName(((TypeVariable)aType).getBounds()[0]);
    if(aType instanceof WildcardType) { WildcardType wc = (WildcardType)aType;
        if(wc.getLowerBounds().length>0)
            return getTypeName(wc.getLowerBounds()[0]);
        return getTypeName(wc.getUpperBounds()[0]);
    }
    throw new RuntimeException("JavaDecl: Can't get Type name from type: " + aType);
}

/** Returns the class name, converting primitive arrays to 'int[]' instead of '[I'. */
private String getTypeName(Class aClass)
{
    if(!aClass.isArray()) return aClass.getName();
    return getTypeName(aClass.getComponentType()) + "[]";
}

/** Returns a simple class name. */
private String getSimpleName(String cname)
{
    int i = cname.lastIndexOf('$'); if(i<0) i = cname.lastIndexOf('.'); if(i>0) cname = cname.substring(i+1);
    return cname;
}

/** Returns the parent class name. */
private String getParentClassName(String cname)
{
   int i = cname.lastIndexOf('$'); if(i<0) i = cname.lastIndexOf('.'); if(i>0) cname = cname.substring(0,i);
   return cname;
}

/** Returns the top level class name. */
private String getRootClassName(String cname)
{
   int i = cname.indexOf('$'); if(i>0) cname = cname.substring(0,i);
   return cname;
}

/** Returns a package name for a class name. */
private String getPackageName(String cname)
{
    String name = getClassName(); int i = name.lastIndexOf('.'); name = i>0? name.substring(0,i) : "";
    return name;
}

}