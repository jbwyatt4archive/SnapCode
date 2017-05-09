package snap.app;
import java.io.File;
import java.util.*;
import snap.debug.*;
import snap.javakit.JavaData;
import snap.javakit.JavaDecl;
import snap.project.*;
import snap.util.*;
import snap.view.DialogBox;
import snap.web.*;

/**
 * A class to launch Snap apps.
 */
public class AppLauncher {

    // The run config
    RunConfig         _config;

    // The file to make an applet for
    WebURL            _url;
    
    // The Project
    Project           _proj;
    
    // The last executed file
    static WebFile    _lastRunFile;
    
/**
 * Returns the WebURL.
 */
public WebURL getURL()  { return _url; }

/**
 * Returns the URL String.
 */
public String getURLString()  { return getURL().getString(); }

/**
 * Returns the app args.
 */
public String getAppArgs()  { return _config!=null? _config.getAppArgs() : null; }

/**
 * Runs the provided file for given run mode.
 */
public void runFile(AppPane anAppPane, RunConfig aConfig, WebFile aFile, boolean isDebug)
{
    // Have AppPane save files
    anAppPane.saveFiles();
    
    // Get file
    WebFile file = aFile, bfile;
    
    // Try to replace file with project file
    _proj = Project.get(file); if(_proj==null) { System.err.println("AppLauncher: not project file: " + file); return; }
    if(file.getType().equals("java")) bfile = _proj.getClassFile(file);
    else bfile = _proj.getBuildFile(file.getPath(), false, file.isDir());
    if(bfile!=null) file = bfile;
    
    // Set URL
    _url = file.getURL();
    _config = aConfig;
    
    // Set last run file
    _lastRunFile = aFile;
    
    // If TeaVM (links against jar and activates TVViewEnv), generate tea files and open in browser
    if(isTeaVM(_proj, aFile)) {
        runTea(anAppPane); return; }
        
    // If HTML
    if(isTeaHTML(_proj, aFile)) {
        runTeaHTML(aFile); return; }
    
    // Run/debug file
    if(isDebug) debugApp(anAppPane);
    else runApp(anAppPane);
}

/**
 * Runs the provided file as straight app.
 */
void runApp(AppPane anAppPane)
{
    // Get run command as string array
    List <String> commands = getCommand();
    String command[] = commands.toArray(new String[commands.size()]);
    
    // Print run command to console
    System.err.println(ListUtils.joinStrings(ListUtils.newList((Object[])command), " "));
    
    // Create RunApp and exec
    RunApp proc = new RunApp(getURL(), command);
    anAppPane.getProcPane().execProc(proc);
}

/**
 * Runs the provided file as straight app.
 */
void debugApp(AppPane anAppPane)
{
    // Get run command as string array (minus actual run)
    List <String> commands = getDebugCommand();
    String command[] = commands.toArray(new String[commands.size()]);
    
    // Print run command to console
    System.err.println("debug " + ListUtils.joinStrings(ListUtils.newList((Object[])command), " "));
    
    // Create DebugApp and add project breakpoints
    DebugApp proc = new DebugApp(getURL(), command);
    for(Breakpoint bp : _proj.getBreakpoints())
        proc.addBreakpoint(bp);
        
    // Add app to process pane and exec
    anAppPane.getProcPane().execProc(proc);
}

/**
 * Returns an array of args.
 */
protected List <String> getCommand()
{
    // Get basic run command and add to list
    List <String> commands = new ArrayList();
    String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    commands.add(java);
    
    // Get Class path and add to list
    String cpaths[] = _proj.getProjectSet().getClassPaths(), cpathsNtv[] = FilePathUtils.getNativePaths(cpaths);
    String cpath = FilePathUtils.getJoinedPath(cpathsNtv);
    commands.add("-cp"); commands.add(cpath);

    // If using Snap Runtime, add main class, otherwise ...
    //if(_proj.getUseSnapRuntime() || !getURLString().endsWith(".class")) {
    //    commands.add("snap.swing.SnapApp"); commands.add("file"); commands.add(getURLString()); } else
    
    // Add class name
    commands.add(_proj.getClassName(getURL().getFile()));
    
    // Add App Args
    if(getAppArgs()!=null && getAppArgs().length()>0)
        commands.add(getAppArgs());
    
    // Return commands
    return commands;
}

/**
 * Returns an array of args.
 */
protected List <String> getDebugCommand()  { List <String> cmd = getCommand(); cmd.remove(0); return cmd; }

/**
 * Returns whether this is TeaVM launch.
 */
public boolean isTeaVM(Project aProj, WebFile aFile)
{
    // If class path doesn't include TeaVM jar, return false
    String cpath = FilePathUtils.getJoinedPath(aProj.getProjectSet().getLibPaths());
    if(!cpath.contains("teavm-")) return false;
    
    // If main class contains TVViewEnv, return true
    Set <JavaDecl> decls = JavaData.get(aFile).getRefs(); for(JavaDecl decl : decls) {
        if(decl.getName().startsWith("snaptea.TV"))
            return true; }
    return false;
}

/**
 * Runs the provided file as straight app.
 */
void runTea(AppPane anAppPane)
{
    // Update Tea files
    updateTeaFiles();
    
    // Get run command as string array
    List <String> commands = getTeaCommand();
    String command[] = commands.toArray(new String[commands.size()]);
    
    // Print run command to console
    System.err.println(ListUtils.joinStrings(ListUtils.newList((Object[])command), " "));
    
    // Create RunApp
    RunApp proc = new RunApp(getURL(), command);
    anAppPane.getProcPane().execProc(proc);
    proc.addListener(new RunApp.AppAdapter() {
        public void appExited(RunApp ra) { teaExited(); }});
}

/**
 * Returns an array of args.
 */
protected List <String> getTeaCommand()
{
    // Get basic run command and add to list
    List <String> commands = new ArrayList();
    String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    commands.add(java);
    
    // Get Class path and add to list
    String cpaths[] = _proj.getProjectSet().getClassPaths(); //cpaths = ArrayUtils.add(cpaths, "/Temp/teavm/*");
    String cpathsNtv[] = FilePathUtils.getNativePaths(cpaths);
    String cpath = FilePathUtils.getJoinedPath(cpathsNtv); cpath = cpath.replace("teavm-jso-0.4.3.jar", "*");
    commands.add("-cp"); commands.add(cpath);
    
    // Add runner and class name
    commands.add("org.teavm.cli.TeaVMRunner");
    commands.add(_proj.getClassName(getURL().getFile()));
    
    // Add output dir
    String bpath = _proj.getClassPath().getBuildPathAbsolute() + "tea";
    String bpathNtv = FilePathUtils.getNativePath(bpath);
    commands.add("-d"); commands.add(bpathNtv);
    
    // Add other options
    //commands.add("-S"); commands.add("-D");
    commands.add("-G"); commands.add("-g");
    
    // Add App Args
    if(getAppArgs()!=null && getAppArgs().length()>0)
        commands.add(getAppArgs());
    
    // Return commands
    return commands;
}

/**
 * Called when tea Exited.
 */
public void teaExited()
{
    WebURL html = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea/index.html");
    WebFile htmlFile = html.getFile();
    if(htmlFile==null) htmlFile = getHTMLIndexFile();
    snap.gfx.GFXEnv.getEnv().openFile(htmlFile.getStandardFile());
}

/**
 * Updates tea vm files.
 */
private void updateTeaFiles()
{
    updateTeaFiles(_proj.getSourceDir());
    for(Project proj : _proj.getProjects()) {
        WebFile file = proj.getSourceDir();
        if(proj.getName().equals("SnapKit")) {
            updateTeaFiles(proj.getFile("/src/snap/util/XMLParser.txt"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/JTokens.txt"));
        }
        else updateTeaFiles(file);
    }
}

/**
 * Updates tea vm files.
 */
private void updateTeaFiles(WebFile aFile)
{
    // If directory, just recurse
    if(aFile.isDir()) {
        String name = aFile.getName(); if(name.equals("bin") || name.equals("tea")) return;
        for(WebFile file : aFile.getFiles()) updateTeaFiles(file);
    }
    
    // Otherwise get tea build file and see if it needs to be updated
    else if(isResourceFile(aFile)) {
        String path = aFile.getPath(); if(path.startsWith("/src/")) path = path.substring(4);
        WebURL url = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea" + path);
        WebFile tfile = url.getFile();
        if(tfile==null || aFile.getLastModifiedTime()>tfile.getLastModifiedTime()) {
            System.out.println("Updating Tea Resource File: " + url.getPath());
            if(tfile==null) tfile = url.createFile(false);
            tfile.setBytes(aFile.getBytes());
            tfile.save();
        }
    }
}

/**
 * Returns whether given file is a resource file.
 */
public boolean isResourceFile(WebFile aFile)  { return !aFile.getType().equals("java"); }

/**
 * Returns whether this is TeaVM launch.
 */
public boolean isTeaHTML(Project aProj, WebFile aFile)
{
    // If class path doesn't include TeaVM jar, return false
    String cpath = FilePathUtils.getJoinedPath(aProj.getProjectSet().getLibPaths());
    if(!cpath.contains("teavm-")) return false;
    
    // If main class contains TVViewEnv, return true
    return aFile.getType().equals("html") || aFile.getType().equals("snp");
}

/**
 * Runs the provided file as straight app.
 */
void runTeaHTML(WebFile aFile)
{
    // If snp file, offer to create HTML
    if(aFile.getType().equals("snp")) {
        String htmlFilename = aFile.getSimpleName() + ".html";
        WebFile htmlFile = aFile.getParent().getFile(htmlFilename);
        if(htmlFile==null)
            if(!DialogBox.showConfirmDialog(null, "Create HTML file", "Create HTML file?")) return;
        aFile = createHTMLFile(aFile);
    }
    
    WebURL turl = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea");
    WebFile tdir = turl.getFile();
    if(tdir==null) { }
    
    // Update Tea files
    updateTeaFiles();
    
    WebURL html = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea/" + aFile.getName());
    WebFile htmlFile = html.getFile();
    snap.gfx.GFXEnv.getEnv().openFile(htmlFile);
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile getHTMLIndexFile()
{
    WebURL html = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea/index.html");
    WebFile hfile = html.getFile(); if(hfile!=null) return hfile;
    hfile = html.createFile(false);

    StringBuffer sb = new StringBuffer();
    sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>SnapTea</title>\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"runtime.js\"></script>\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"classes.js\"></script>\n");
    sb.append("</head>\n<body onload=\"main()\">\n"); sb.append("</body>\n</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile createHTMLFile(WebFile aFile)
{
    String hpath = aFile.getParent().getDirPath() + aFile.getSimpleName() + ".html";
    WebFile hfile = aFile.getSite().createFile(hpath, false);

    StringBuffer sb = new StringBuffer();
    sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>SnapTea</title>\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"runtime.js\"></script>\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"classes.js\"></script>\n");
    sb.append("</head>\n<body onload=\"load()\">\n");
    sb.append("<script type=\"text/javascript\">\n");
    sb.append("    function load() { main(\"" + aFile.getName() + "\"); }\n");
    sb.append("</script>\n"); sb.append("</body>\n</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

/**
 * Returns the last run file.
 */
public static WebFile getLastRunFile()  { return _lastRunFile; }

}