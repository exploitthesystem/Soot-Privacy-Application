import java.util.Iterator;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Set;


import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;


public class AndroidInstrument {
	
	public static void main(String[] args) {
		
		//prefer Android APK files// -src-prec apk
		Options.v().set_src_prec(Options.src_prec_apk);
		
		//output as APK, too//-f J
		Options.v().set_output_format(Options.output_format_dex);
		
        // resolve the PrintStream and System soot-classes
		Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);

        // resolve classes for the master list we want to add
        Scene.v().addBasicClass("java.util.Set",SootClass.SIGNATURES);


        // create a new class to store global tag list
        //Scene.v().addBasicClass()

        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

			@Override
			protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
								
				final PatchingChain<Unit> units = b.getUnits();


				Local tmpSetRef = addTmpSetRef(b); // add the local (defined further down) into the body

				// add path list objectto the chain
				// tried addLast, that didn't work, now trying this
				units.add(Jimple.v().newAssignStmt( 
						tmpSetRef, Jimple.v().newNewExpr(RefType.v("java.util.Set")))); 

				
				Unit tmpu = null;


				// Moved this down here so we can get the dump after tmpSetRef was inserted into the chain
				//    but still problems with it inserting many copies, maybe something to do with not using
				//    a snapshot iterator?
				// Dump the BriefUnitGraph to a file
				UnitGraph graph = new BriefUnitGraph(b);

				try (FileWriter fileWriter = new FileWriter("BriefUnitGraph.dump",true)){
				    PrintWriter out = new PrintWriter(fileWriter);
					out.print(graph.toString());
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				//TODO: need to find entry point for application (usually MainActivity)
				// except we can generalize and find entrypoint this way:
				// https://github.com/0-14N/soot-infoflow-android/blob/master/src/soot/jimple/infoflow/android/manifest/ProcessManifest.java
				// in entry point, need to insert "forbidden" list as a static
				// in entry point, need to declare static variable in (MainActivity.onCreate()) to keep track of accessed "forbidden path" units
				// 
				
				//important to use snapshotIterator here
				for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {

					final Unit u = iter.next();

					// represent the private resource we want to monitor and
					// (except we care about when it's accessed rather than created)
					String iface_unit_pattern = "EditText";
					// the iterface where this information could leave the device
					String priv_unit_pattern = "writeRXCharacteristic";

					// get all the preds of priv_unit_pattern until encounter iface_unit_pattern
					// insert tags BEFORE each unit, except last unit, which should get the check
					if (u.toString().matches("(.*)"+priv_unit_pattern+"(.*)")) {
						System.out.println(u.toString());
						System.out.println("******Printing Path*******");
						tmpu = u;
						do {
							try {
								tmpu = graph.getPredsOf(tmpu).get(0);
								System.out.println(tmpu.toString());
								// this is where the tag should be added
							}
							catch (Exception e) { 
								break;
							}
						} while (!(tmpu.toString().matches("(.*)"+iface_unit_pattern+"(.*)")));
						System.out.println("******Done*******");

					}	

					u.apply(new AbstractStmtSwitch() {
						
						public void caseInvokeStmt(InvokeStmt stmt) {
							InvokeExpr invokeExpr = stmt.getInvokeExpr();


							if(invokeExpr.getMethod().getName().equals("onDraw")) {

								Local tmpRef = addTmpRef(b);
								Local tmpString = addTmpString(b);
								
								  // insert "tmpRef = java.lang.System.out;" 
						        units.insertBefore(Jimple.v().newAssignStmt( 
						                      tmpRef, Jimple.v().newStaticFieldRef( 
						                      Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);

						        // insert "tmpLong = 'HELLO';" 
						        units.insertBefore(Jimple.v().newAssignStmt(tmpString, 
						                      StringConstant.v("HELLO")), u);
						        
						        // insert "tmpRef.println(tmpString);" 
						        SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");                    
						        units.insertBefore(Jimple.v().newInvokeStmt(
						                      Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), tmpString)), u);
						        
						        //check that we did not mess up the Jimple
						        b.validate();
							}
						}
						
					});
				}
			}


		}));
		
		soot.Main.main(args);
	}

    private static Local addTmpSetRef(Body body)
    {
        Local tmpSetRef = Jimple.v().newLocal("$pathSet", RefType.v("java.util.Set"));
        body.getLocals().add(tmpSetRef);
        return tmpSetRef;
    }

    private static Local addTmpRef(Body body)
    {
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        body.getLocals().add(tmpRef);
        return tmpRef;
    }
    
    private static Local addTmpString(Body body)
    {
        Local tmpString = Jimple.v().newLocal("tmpString", RefType.v("java.lang.String")); 
        body.getLocals().add(tmpString);
        return tmpString;
    }
}
