package snap.javasnap;
import java.util.*;
import snap.gfx.*;
import snap.javakit.*;
import snap.view.*;

/**
 * A View subclass to display a JNode.
 */
public class JNodeView <JNODE extends JNode> extends JNodeViewBase {

    // The JNode
    JNODE                 _jnode;
    
    // The children node owners
    List <JNodeView>      _jnodeViews;
    
    // The current drag over node
    static JNodeViewBase  _dragOver;
    
    // Constants for colors
    public static Color PieceColor = Color.get("#4C67d6");
    public static Color BlockStmtColor = Color.get("#8f56e3");
    public static Color MemberDeclColor = Color.get("#f0a822");
    public static Color ClassDeclColor = MemberDeclColor; //Color.get("#27B31E");

/**
 * Creates a new JNodeView.
 */
public JNodeView()  { }

/**
 * Creates a new JNodeView for given JNode.
 */
public JNodeView(JNODE aJN)  { setJNode(aJN); }

/**
 * Returns the JNode.
 */
public JNODE getJNode()  { return _jnode; }

/**
 * Sets the JNode.
 */
public void setJNode(JNODE aJNode)
{
    _jnode = aJNode;
    updateUI();
}

/**
 * Updates the UI.
 */
protected void updateUI()
{
    // Add child UI
    if(isBlock()) {
        VBox vbox = getVBox();
        for(JNodeView child : getJNodeViews())
            vbox.addChild(child);
        vbox.setMinHeight(vbox.getChildCount()==0? 30 : -1);
    }

    if(_jnode.getFile()==null) return;
    enableEvents(DragEvents);
}

/**
 * Returns the SnapEditor.
 */
public SnapEditor getEditor()  { return getJNodeViewParent()!=null? getJNodeViewParent().getEditor() : null; }

/**
 * Returns whether NodePane is a block.
 */
public boolean isBlock()  { return getJNode().isBlock(); }

/**
 * Returns the parent.
 */
public JNodeView getJNodeViewParent()  { return getParent(JNodeView.class); }

/**
 * Returns the number of children.
 */
public int getJNodeViewCount()  { return _jnodeViews!=null? _jnodeViews.size() : 0; }

/**
 * Returns the individual child.
 */
public JNodeView getJNodeView(int anIndex)  { return _jnodeViews.get(anIndex); }

/**
 * Returns the individual child.
 */
public JNodeView getJNodeViewLast()  { int cc = getJNodeViewCount(); return cc>0? _jnodeViews.get(cc-1) : null; }

/**
 * Returns the children.
 */
public List <JNodeView> getJNodeViews()  { return _jnodeViews!=null? _jnodeViews : (_jnodeViews=createJNodeViews()); }

/**
 * Creates the children.
 */
protected List <JNodeView> createJNodeViews()
{
    if(!isBlock()) return Collections.EMPTY_LIST;
    
    List <JNodeView> children = new ArrayList();
    JNode node = getJNode();
    if(node.getBlock()!=null) for(JStmt stmt : node.getBlock().getStatements()) {
        JNodeView mcnp = createView(stmt); if(mcnp==null) continue;
        children.add(mcnp);
    }
    return children;
}

/**
 * Returns a string describing the part.
 */
public String getPartString()  { return getJNode().getNodeString(); }

/**
 * Creates a statement label.
 */
protected Label createLabel(String aString)
{
    Label label = new Label(aString); label.setPadding(2,4,2,0);
    label.setFont(new Font("Arial Bold", 12)); label.setTextFill(Color.WHITE);
    return label;
}

/**
 * Creates a statement textfield.
 */
protected TextField createTextField(String aString)
{
    TextField tfield = new TextField(); tfield.setText(aString);
    tfield.setFont(new Font("Arial", 11)); tfield.setAlign(Pos.CENTER);
    tfield.setColumnCount(0); tfield.setMinWidth(36); tfield.setPrefHeight(18); tfield.setPadding(2,6,2,6);
    return tfield;
}

/**
 * ProcessEvent.
 */
protected void processEvent(ViewEvent anEvent)
{
    if(anEvent.isDragEvent()) handleDragEvent(anEvent);
    else super.processEvent(anEvent);
}

/**
 * Responds to drag events.
 */
protected void handleDragEvent(ViewEvent anEvent)
{
    // Handle DragEvent: Accept drag event
    //DragEvent de = anEvent.getEvent(DragEvent.class); de.acceptTransferModes(TransferMode.COPY); de.consume();
    anEvent.acceptDrag(); anEvent.consume();
    
    // Handle DragEnter, DragOver, DragExit: Apply/clear effect from DragEffectNode
    if(anEvent.isDragEnter() || anEvent.isDragOver() && _dragOver!=this) {
        if(_dragOver!=null) _dragOver.setUnderDrag(false); setUnderDrag(true); _dragOver = this; }
    if(anEvent.isDragExit()) { setUnderDrag(false); if(_dragOver==this) _dragOver = null; }
    
    // Handle DragDropEvent
    if(anEvent.isDragDropEvent() && SupportPane._dragSP!=null) {
        if(_dragOver!=null) _dragOver.setUnderDrag(false); _dragOver = null;
        dropNode(SupportPane._dragSP.getJNode(), anEvent.getX(), anEvent.getY());
        anEvent.dropComplete(); //de.setDropCompleted(true);
    }
}

/**
 * Drops a node at center of part.
 */
protected void dropNode(JNode aJNode) { dropNode(aJNode, getWidth()/2, getHeight()/2); }

/**
 * Drops a node.
 */
protected void dropNode(JNode aNode, double anX, double aY)
{
    System.out.println("Cannot add node to " + getClass().getSimpleName());
}
    
/**
 * Standard toString implementation.
 */
public String toString()  { return getJNode().toString(); }

/**
 * Returns the SnapPart of a node.
 */
public static JNodeView getJNodeView(View aView)
{
    if(aView instanceof JNodeView) return (JNodeView)aView;
    return aView.getParent(JNodeView.class);
}

/**
 * Creates a SnapPart for a JNode.
 */
public static JNodeView createView(JNode aNode)
{
    JNodeView np = null;
    if(aNode instanceof JFile) np = new JFileView();
    else if(aNode instanceof JMemberDecl) np = JMemberDeclView.createView(aNode);
    else if(aNode instanceof JStmt) np = JStmtView.createView(aNode);
    else if(aNode instanceof JExpr) np = JExprView.createView(aNode);
    else if(aNode instanceof JType) np = new JTypeView();
    if(np==null) return null;
    np.setJNode(aNode);
    return np;
}

}