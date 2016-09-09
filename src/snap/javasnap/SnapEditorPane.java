package snap.javasnap;
import snap.gfx.*;
import snap.javaparse.*;
import snap.javatext.*;
import snap.view.*;

/**
 * This class manages a SnapEditor.
 */
public class SnapEditorPane extends ViewOwner {

    // The main editor UI
    SnapEditor          _editor;

    // A class for editing code
    JavaTextPane        _javaPane;
    
    // The pieces pane
    SupportPane         _supportPane;
    
    // The node path
    HBox                _nodePathBox;
    
    // The deepest part of current NodePath (which is SelectedPart, unless NodePath changed SelectedPart)
    SnapPart            _deepPart;

    // Whether to rebuild CodeArea
    boolean             _rebuild = true;
    
    // The SnapEditorPage - we only need this to switch to Java text
    SnapEditorPage      _snapPage;

/**
 * Creates a new SnapEditorPane for given JavaTextPane.
 */
public SnapEditorPane(JavaTextPane aJTP)
{
    _javaPane = aJTP;
    _supportPane = new SupportPane(); _supportPane._editorPane = this;
}

/**
 * Returns the SnapEditor.
 */
public SnapEditor getEditor()  { return _editor; }

/**
 * Returns the SnapJavaPane.
 */
public JavaTextPane getJavaTextPane()  { return _javaPane; }

/**
 * Returns the JavaTextView.
 */
public JavaTextView getJavaTextView()  { return _javaPane.getTextView(); }

/**
 * Returns the SupportPane.
 */
public SupportPane getSupportPane()  { return _supportPane; }

/**
 * Returns the selected part.
 */
public SnapPart getSelectedPart()  { return _editor.getSelectedPart(); }

/**
 * Sets the selected parts.
 */
public void setSelectedPart(SnapPart aPart)  { _editor.setSelectedPart(aPart); }

/**
 * Create UI.
 */
protected View createUI()
{
    // Get normal UI
    View toolBar = super.createUI(); //toolBar.setMaxHeight(28);
    
    // Create SnapEditor
    _editor = new SnapEditor(getJavaTextView());

    // Add to Editor.UI to ScrollView
    ScrollView sview = new ScrollView(_editor); sview.setGrowWidth(true);

    // Get SupportPane
    _supportPane = getSupportPane(); _supportPane.getUI().setPrefWidth(300);
    
    // Create SplitView, configure and return
    SplitView spane = new SplitView();
    spane.setChildren(sview, _supportPane.getUI());
    
    // Create NodePath and add to bottom
    _nodePathBox = new HBox(); _nodePathBox.setPadding(2,2,2,2);
    
    // Create BorderView with toolbar
    BorderView bview = new BorderView(); bview.setCenter(spane); bview.setTop(toolBar); bview.setBottom(_nodePathBox);
    return bview;
}

/**
 * Initialize UI.
 */
protected void initU()
{
    addKeyActionEvent("CutButton", "Shortcut+X");
    addKeyActionEvent("CopyButton", "Shortcut+C");
    addKeyActionEvent("PasteButton", "Shortcut+V");
    addKeyActionEvent("DeleteButton", "DELETE");
    addKeyActionEvent("DeleteButton", "BACKSPACE");
    addKeyActionEvent("UndoButton", "Shortcut+Z");
    addKeyActionEvent("RedoButton", "Shortcut+Shift+Z");
    addKeyActionEvent("Escape", "ESC");
}

/**
 * ResetUI.
 */
public void resetUI()
{
    if(_rebuild) { _editor.rebuildUI(); _rebuild = false; }
    rebuildNodePath();
}

/**
 * Respond to UI changes.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle NodePathLabel
    if(anEvent.equals("NodePathLabel")) {
        Label label = anEvent.getView(Label.class);
        SnapPart part = (SnapPart)label.getProp("SnapPart"), dpart = _deepPart;
        setSelectedPart(part);
        _deepPart = dpart;
    }
    
    // Handle SaveButton
    if(anEvent.equals("SaveButton")) getJavaTextPane().saveChanges();
    
    // Handle CutButton, CopyButton, PasteButton, Escape
    if(anEvent.equals("CutButton")) cut();
    if(anEvent.equals("CopyButton")) copy();
    if(anEvent.equals("PasteButton")) paste();
    if(anEvent.equals("DeleteButton")) delete();
    if(anEvent.equals("Escape")) escape();
    
    // Handle UndoButton, RedoButton
    if(anEvent.equals("UndoButton")) undo();
    if(anEvent.equals("RedoButton")) redo();
    
    // Handle JavaButton
    if(anEvent.equals("JavaButton")) _snapPage.openAsJavaText();
}

/**
 * Rebuilds the NodePathBox.
 */
void rebuildNodePath()
{
    // Clear path and get font
    _nodePathBox.removeChildren();
    
    // Iterate up from DeepPart and add parts
    for(SnapPart part=_deepPart, spart=getSelectedPart(); part!=null;) {
        Label label = new Label(part.getPartString()); label.setFont(Font.Arial12);
        label.setName("NodePathLabel"); label.setProp("SnapPart", part);
        if(part==spart) label.setFill(Color.LIGHTGRAY);
        _nodePathBox.addChild(label,0); label.setOwner(this); enableEvents(label, MouseClicked);
        part = part.getParent(); if(part==null) break;
        Label div = new Label(" \u2022 "); div.setFont(Font.Arial12);
        _nodePathBox.addChild(div,0);
    }
}

/**
 * Rebuilds the CodeArea UI later.
 */
protected void rebuildLater()  { _rebuild = true; resetLater(); }

/**
 * Sets the selected parts.
 */
public void updateSelectedPart(SnapPart aPart)
{
    _supportPane.rebuildUI();
    resetLater();
    _deepPart = aPart;
}

/**
 * Cut current selection to clipboard.
 */
public void cut()  { copy(); delete(); }

/**
 * Copy current selection to clipboard.
 */
public void copy()
{
    // Make sure statement is selected
    if(!(getSelectedPart() instanceof SnapPartStmt)) {
        SnapPartStmt stmt = (SnapPartStmt)getSelectedPart().getAncestor(SnapPartStmt.class); if(stmt==null) return;
        setSelectedPart(stmt);
    }
    
    // Do copy
    getJavaTextView().copy();
}

/**
 * Paste ClipBoard contents.
 */
public void paste()
{
    // Get Clipboard String and create node
    Clipboard cb = Clipboard.get(); if(!cb.hasString()) return;
    String str = cb.getString();
    JNode node = null;
    try { node = _supportPane._stmtParser.parseCustom(str, JNode.class); } catch(Exception e) { }
    if(node==null)
        try { node = _supportPane._exprParser.parseCustom(str, JNode.class); } catch(Exception e) { }
    
    // Get SelectedPart and drop node
    SnapPart spart = getSelectedPart();
    if(spart!=null && node!=null)
        spart.dropNode(node, spart.getWidth()/2, spart.getHeight());
}

/**
 * Delete current selection.
 */
public void delete()  { getJavaTextView().delete(); rebuildLater(); }

/**
 * Undo last change.
 */
public void undo()  { getJavaTextView().undo(); rebuildLater(); }

/**
 * Redo last undo.
 */
public void redo()  { getJavaTextView().redo(); rebuildLater(); }

/**
 * Escape.
 */
public void escape()  { SnapPart par = getSelectedPart().getParent(); if(par!=null) setSelectedPart(par); }

}