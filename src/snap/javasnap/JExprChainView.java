package snap.javasnap;
import java.util.*;
import snap.javakit.*;
import snap.view.*;

/**
 * SnapPartExpr subclass for JExprChain.
 */
public class JExprChainView <JNODE extends JExprChain> extends JExprView <JNODE> {

/**
 * Updates UI.
 */
protected void updateUI()
{
    // Do normal version
    super.updateUI();
    
    // Get/configure HBox
    HBox hbox = getHBox(); hbox.setPadding(0,0,0,0);
    
    // Create/add views for child expressions
    for(JNodeView child : getJNodeViews()) hbox.addChild(child);
    getJNodeView(0).setSeg(Seg.First);
    getJNodeViewLast().setSeg(Seg.Last);
}

/**
 * Override to create children.
 */
protected List <JNodeView> createJNodeViews()
{
    // Iterate over expression chain children, create expression views and add to list
    JExprChain echain = getJNode(); List children = new ArrayList();
    for(JExpr exp : echain.getExpressions()) {
        JExprView eview = createView(exp); eview.setGrowWidth(true);
        children.add(eview);
    }
    
    // Return expression views
    return children;
}

}