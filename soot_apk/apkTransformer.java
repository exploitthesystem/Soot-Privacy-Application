package myTransformer;

import java.io.*;
import java.util.*;

import soot.*;

public class TransformerMain{

	public static void main(String[] args){
		//prefer Android APK files// -src-prec apk
		Options.v().set_src_prec(Options.src_prec_apk);

		//output as APK, too//-f J
		Options.v().set_output_format(Options.output_format_dex);

		// resolve the PrintStream and System soot-classes
		Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
		Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);

		PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

		@Override
		protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
				final PatchingChain units = b.getUnits();		
				//important to use snapshotIterator here
				for(Iterator iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = iter.next();
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
							    SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void     println(java.lang.String)");                    
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
	}
}