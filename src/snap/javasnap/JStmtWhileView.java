package snap.javasnap;
import snap.javakit.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtWhile.
 */
public class JStmtWhileView <JNODE extends JStmtWhile> extends JStmtView <JNODE> {

/**
 * Updates UI.
 */
protected void updateUI()
{
    // Do normal version
    super.updateUI();
    
    // Configure HBox
    HBox hbox = getHBox();
    
    // Creeate label and expr parts and add to HBox
    Label label = createLabel("while");
    JStmtWhile wstmt = getJNode(); JExpr cond = wstmt.getConditional();
    JExprView spart = new JExprEditor(); spart.setJNode(cond);
    hbox.setChildren(label, spart);
}    

}