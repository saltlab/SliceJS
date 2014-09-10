package com.slicejs.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.json.JSONObject;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.owasp.webscarab.model.Preferences;
import org.owasp.webscarab.plugin.Framework;
import org.owasp.webscarab.plugin.proxy.Proxy;

import com.slicejs.configuration.AliasAnalyzer;
import com.slicejs.configuration.FileLineNumber;
import com.slicejs.configuration.ProxyConfiguration;
import com.slicejs.configuration.TraceHelper;
import com.slicejs.core.trace.ArgumentRead;
import com.slicejs.core.trace.ArgumentWrite;
import com.slicejs.core.trace.PropertyRead;
import com.slicejs.core.trace.RWOperation;
import com.slicejs.core.trace.ReturnStatementValue;
import com.slicejs.core.trace.ReturnValueWrite;
import com.slicejs.core.trace.VariableRead;
import com.slicejs.core.trace.VariableWrite;
import com.slicejs.core.trace.VariableWriteAugmentAssign;
import com.slicejs.instrument.ProxyInstrumenter;
import com.slicejs.instrument.helpers.ControlMapper;
import com.slicejs.instrument.helpers.ParentFunctionFinder;
import com.slicejs.jsmodify.JSExecutionTracer;
import com.slicejs.jsmodify.JSModifyProxyPlugin;
import com.slicejs.units.IfStatement;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import com.crawljax.util.Helper;

public class SimpleExample2 {

	// Example arguments:       --server http://www.themaninblue.com/experiment/BunnyHunt/ --file bunnies.js --line 732 --variable positionY

	//--server http://www.themaninblue.com/experiment/BunnyHunt/ --file clouds.js --line 30 --variable cloud1

	// --server http://localhost:8080/test.html --file test.js --line 17 --variable original

	// --server http://localhost:8080/test.html --file test.js --line 22 --variable tt


	public static final String SERVER_PREFIX2 = "--server";
	public static final String SERVER_PREFIX1 = "--s";

	public static final String LINE_PREFIX2 = "--line";
	public static final String LINE_PREFIX1 = "--l";

	public static final String FILE_PREFIX2 = "--file";
	public static final String FILE_PREFIX1 = "--f";

	public static final String VAR_PREFIX2 = "--v";
	public static final String VAR_PREFIX1 = "--variable";
	
	private static ObjectMapper mapper2 = new ObjectMapper();

	private static boolean urlProvided = false;
	private static boolean varProvided = false;
	private static boolean lineProvided = false;
	private static boolean fileProvided = false;
	private static String URL = "";
	private static String VAR = "";
	private static int LINE;
	private static String FILE = "";

	private static String outputFolder = "";
	private static WebDriver driver;

	private ArrayList<FileLineNumber> theSlice = new ArrayList<FileLineNumber>();
	private ArrayList<RWOperation> expandedSlice = new ArrayList<RWOperation>();

	public void main(String[] args) {
		try {
			int argType = -1;

			// Iterate through arguments
			for (String arg : args) {


				// Argument parsing
				switch (argType) {
				case 0:
					URL = arg;
					urlProvided = true;
					break;
				case 1:  
					VAR = arg;
					varProvided = true;
					break;
				case 2:  
					LINE = Integer.parseInt(arg);
					lineProvided = true;
					break;
				case 3:  
					FILE = arg;
					fileProvided = true;
					break;
				default:
					// Not an expected argument flag, ignore
					break;
				}
				argType = -1;

				// If previous argument was an argument flag, save the value
				argType = parse(arg);

			}

			if (urlProvided == false) {
				System.err.println("Invalid arguments. Please provide URL for target application as argument (E.g. --server http://localhost:8888/phormer331/index.php)");
				throw new IllegalArgumentException();
			} 

			if (fileProvided == false) {
				System.err.println("Invalid arguments. Please provide a filename for the slicing criteria for target application as argument (E.g. --file animate.js)");
				throw new IllegalArgumentException();
			} 

			if (lineProvided == false) {
				System.err.println("Invalid arguments. Please provide a line number for the slicing criteria (E.g. --line 8)");
				throw new IllegalArgumentException();
			} 

			if (varProvided == false) {
				System.err.println("Invalid arguments. Please provide the name of the variable for which we are computing a slice (E.g. --variable privateInt)");
				throw new IllegalArgumentException();
			} 

			outputFolder = Helper.addFolderSlashIfNeeded("clematis-output");

			JSExecutionTracer tracer = new JSExecutionTracer();
			tracer.setOutputFolder(outputFolder + "ftrace");
			tracer.preCrawling();

			// Create a new instance of the firefox driver
			FirefoxProfile profile = new FirefoxProfile();
			// Instantiate proxy components
			ProxyConfiguration prox = new ProxyConfiguration();

			// Modifier responsible for parsing Ast tree
			ProxyInstrumenter s = new ProxyInstrumenter();

			// Add necessary files from resources
			s.setFileNameToAttach("/slice.wrapper.js");
			s.setFileNameToAttach("/send.and.buffer.js");

			// Interface for Ast traversal
			JSModifyProxyPlugin p = new JSModifyProxyPlugin(s);
			p.excludeDefaults();

			// Set slicing criteria (as it was passed in by user)
			p.setTargetFile(FILE);
			p.setLineNo(LINE);
			p.setVariableName(VAR);

			Framework framework = new Framework();

			/* set listening port before creating the object to avoid warnings */
			Preferences.setPreference("Proxy.listeners", "127.0.0.1:" + prox.getPort());

			Proxy proxy = new Proxy(framework);

			/* add the plugins to the proxy */
			proxy.addPlugin(p);

			framework.setSession("FileSystem", new File("convo_model"), "");

			/* start the proxy */
			proxy.run();

			if (prox != null) {
				profile.setPreference("network.proxy.http", prox.getHostname());
				profile.setPreference("network.proxy.http_port", prox.getPort());
				profile.setPreference("network.proxy.type", prox.getType().toInt());
				/* use proxy for everything, including localhost */
				profile.setPreference("network.proxy.no_proxies_on", "");
			}

			driver = new FirefoxDriver(profile);
			WebDriverWait wait = new WebDriverWait(driver, 10);
			boolean sessionOver = false;
			
			driver.get(URL);

			while (!sessionOver) {
				// Wait until the user/tester has closed the browser

				try {
					waitForWindowClose(wait);

					// At this point the window was closed, no TimeoutException
					sessionOver = true;
				} catch (TimeoutException e) {
					// 10 seconds has elapsed and the window is still open
					sessionOver = false;
				} catch (WebDriverException wde) {
					wde.printStackTrace();
					sessionOver = false;
				}
			}

			tracer.postCrawling();

			ObjectMapper mapper = new ObjectMapper();
			// Register the module that serializes the Guava Multimap
			mapper.registerModule(new GuavaModule());

			Multimap<String, RWOperation> traceMap = mapper
					.<Multimap<String, RWOperation>> readValue(
							new File("clematis-output/ftrace/null"),
							new TypeReference<TreeMultimap<String, RWOperation>>() {
							});

			//Collection<RWOperation> properyReads = traceMap.get("PropertyRead");
			Collection<RWOperation> myOperations = traceMap.values();

			// Convert to ArrayList
			ArrayList<RWOperation> all = new ArrayList<RWOperation>();
			Iterator<RWOperation> it3 = myOperations.iterator();
			while (it3.hasNext()) {
				all.add(it3.next());
			}

			// Sort the ArrayList of read/write operations
			Comparator<RWOperation> comparator = new Comparator<RWOperation>() {
				public int compare(RWOperation c1, RWOperation c2) {
					return c1.getOrder() - c2.getOrder(); 
				}
			};
			Collections.sort(all, comparator); // use the comparator as much as u want

			RWOperation nextOp;
			Iterator<RWOperation> it1 = all.iterator();

			ArrayList<RWOperation> readsToBeSliced = new ArrayList<RWOperation>();
			ArrayList<RWOperation> readsCompleted = new ArrayList<RWOperation>();

			// First one:
			RWOperation begin = new RWOperation();
			RWOperation next;
			begin.setLineNo(LINE);
			begin.setVariable(VAR);

			readsToBeSliced.add(begin);

			// 1  -  Get all instances of slicing criteria (all the reads for positionX on line _, etc.)
			// 2  -  Get last write for that instance
			// 3  -  Get all dependencies for that last write
			// 4  - repeat 1 for each new dependency
			while (readsToBeSliced.size() > 0) {
				next = readsToBeSliced.get(0);
				it1 = all.iterator();

				while (it1.hasNext()) {
					nextOp = it1.next();

					if (nextOp.getLineNo() == next.getLineNo() && nextOp.getVariable().equals(next.getVariable()) && (nextOp instanceof VariableRead || nextOp instanceof PropertyRead)) {
						// Relevant [READ] found! --> nextOp

						computeBackwardSlice(null, nextOp, nextOp.getVariable(), all, true, ((VariableRead) nextOp).getDefiningFunction(), nextOp);



					}
				}
				readsCompleted.add(readsToBeSliced.remove(0));
			}

			ArrayList<String> vars = new ArrayList<String>();

			for (int d = 0; d < readsCompleted.size(); d++) {
				if (vars.indexOf(readsCompleted.get(d).getVariable()) == -1) {
					vars.add(readsCompleted.get(d).getVariable());
				}
			}

			//Save slice line numbers to file for visualization
			Helper.directoryCheck(p.getOutputFolder());
			Helper.directoryCheck(Helper.addFolderSlashIfNeeded(p.getOutputFolder() + "source"));
			Helper.directoryCheck(Helper.addFolderSlashIfNeeded(p.getOutputFolder() + "lines"));
			PrintStream oldOut = System.out;

			if (theSlice.size() > 0) {
				for (int a = 0; a < theSlice.size(); a++) {
					Helper.checkFolderForFile("src/main/webapp/lines" + theSlice.get(a).getFileName().replace(".js", ".txt"));
					PrintStream outputVisual = new PrintStream("src/main/webapp/lines" + theSlice.get(a).getFileName().replace(".js", ".txt"));
					System.setOut(outputVisual);
					int accessLine = LINE+1;
					System.out.println(theSlice.get(a).getLinesAsString()+accessLine+",");
				}

				String allFiles = "";
				String nextFileName = "";
				for (int a = 0; a < theSlice.size(); a++) {
					nextFileName = theSlice.get(a).getFileName();
					if(nextFileName.contains(".js")) {
						nextFileName = nextFileName.substring(0, nextFileName.lastIndexOf(".js"));
					}
					nextFileName = nextFileName.replaceAll("\\/", "");
					allFiles += nextFileName + " ";
				}
				allFiles = allFiles.substring(0, allFiles.lastIndexOf(" "));
				Helper.checkFolderForFile("src/main/webapp/" + "allFiles.txt");
				PrintStream outputVisual2 = new PrintStream("src/main/webapp/" + "allFiles.txt");
				System.setOut(outputVisual2);
				System.out.println(allFiles);
				System.setOut(oldOut);

			} else {
				System.out.println("Application not exercised enough! No slice produced.");
			}

			mapper2.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(
					Visibility.ANY));
			mapper2.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
			mapper2.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			// to allow coercion of JSON empty String ("") to null Object value:
			mapper2.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
			mapper2.enable(SerializationFeature.INDENT_OUTPUT);

			mapper2.writeValue(new File("src/main/webapp/lines/slicetrace.json"),
					expandedSlice);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}



	static boolean waitForWindowClose(WebDriverWait w) throws TimeoutException {
		// Function to check if window has been closed

		w.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver d) {
				try {
					return d.getWindowHandles().size() < 1;
				} catch (Exception e) {
					return true;
				}
			}
		});
		return true;
	}

	public static boolean isAlertPresent()
	{
		// Selenium bug where all alerts must be closed before
		try {
			driver.switchTo().alert();
			return true;
		} catch (NoAlertPresentException Ex) {
			return false;
		}
	}

	public static String getOutputFolder() {
		return Helper.addFolderSlashIfNeeded(outputFolder);
	}

	private static int parse(String arg) throws IllegalArgumentException {
		if (arg.equals(SERVER_PREFIX1) || arg.equals(SERVER_PREFIX2)) {
			return 0;
		} else if (arg.equals(VAR_PREFIX2) || arg.equals(VAR_PREFIX1)) {
			return 1;
		} else if (arg.equals(LINE_PREFIX1) || arg.equals(LINE_PREFIX2)) {
			return 2;
		} else if (arg.equals(FILE_PREFIX1) || arg.equals(FILE_PREFIX2)) {
			return 3;
		}
		return -1;
	}


	private void computeBackwardSlice(RWOperation top, RWOperation bottom, String name, ArrayList<RWOperation> all, boolean contained, String definingFunction, RWOperation beginRead) {
		int i = (top == null ? 0 : all.indexOf(top));
		RWOperation next = null;
		RWOperation nestedTop, nestedBottom;
		boolean found = false;
	    // Partial slice
		ArrayList<RWOperation> partialSlice = new ArrayList<RWOperation>();
		System.out.println(beginRead);
		partialSlice.add(beginRead);

		if (name.equals("ss_cur")) {
			System.out.println(bottom.getOrder());
			System.out.println(bottom.getClass());
		}


		for (int j = all.indexOf(bottom); j >= i; j--) {
			next = all.get(j);

			/** 1 (ONE): Cut out function calls **/
			if (next instanceof ReturnStatementValue) {


				if (definingFunction.equals("global")) {
					// Keep looking for writes to this global variable (regardless of from which function)
					continue;
				} else if (!definingFunction.equals("global") && TraceHelper.isReadAsynchronous(j, definingFunction, all)) {
					// Jump to last instance of defining function and look for writes there.
					int bottomOfLastFn = TraceHelper.getEndOfLastFnInstance(j, definingFunction, all);
	                   writePartialSliceToDisk(partialSlice);
	                   highlightLine(beginRead, new ArrayList<RWOperation>());
					computeBackwardSlice(null, all.get(bottomOfLastFn-1), name, all, true, definingFunction, beginRead);
					return;
				} else {

					String alias = null;
					nestedTop = TraceHelper.getBeginningOfFunction((ReturnStatementValue) next, all);
					nestedBottom = next;

					// Skip the nested call when backwards slicing, it is taken care of in the above recursive call
					// j = index of ArgumentRead
					j = all.indexOf(nestedTop) + 1;

					// Check if the variable of interest is passed into a function by reference (could be altered there)
					for (int p = j; p >= i; p--) {
						// Possible for multiple references to variable passed in as separate arguments (but why?)
						if (all.get(p) instanceof ArgumentRead
								&& ((ArgumentRead) all.get(p)).getVariable().indexOf(name) == 0
								&& TraceHelper.isComplex(((ArgumentRead) all.get(p)).getValue())) {


							// Get the local name in the called function (argument name)
							for (int q = j; q <= all.indexOf(nestedBottom); q++) {
								if (all.get(q) instanceof ArgumentWrite
										&& ((ArgumentWrite) all.get(q)).getFunctionName().equals(((ArgumentRead) all.get(p)).getFunctionName())
										&& ((ArgumentWrite) all.get(q)).getArgumentNumber() == ((ArgumentRead) all.get(p)).getArgumentNumber()) {
									alias = ((ArgumentWrite) all.get(q)).getVariable();

									//TODO: implement 'computeForwardSlice'
									// Compute forward slice of reference/argument
									computeForwardSlice(all.get(q), nestedBottom, alias, all);

									if (all.get(q).getChildren().size() > 0) {
										// If the function call does change the object via reference, highlight the origin of the call:
										highlightLine(all.get(p), partialSlice);

										for (int o = q; o <= all.indexOf(nestedBottom); o++) {
											if (all.get(o).getSliceStatus() == true) {

												all.get(o).omitFromSlice();
												all.get(o).clearChildren();
												all.get(o).setParent(null);

												// Add line to slice
												highlightLine(all.get(o), partialSlice);


												if (all.get(o) instanceof VariableWrite) {
													// NEW, TEST THIS
													// Need to slice all the dependencies for this write (this update through alias)
													ArrayList<RWOperation> deps = null;
													try {
														deps = TraceHelper.getDataDependencies(all, (VariableWrite) all.get(o));
													} catch (Exception e) {
														e.printStackTrace();
													}
													for (int r = 0; r < deps.size(); r++) {
													    highlightLine(deps.get(r), partialSlice);
														computeBackwardSlice(null, deps.get(r), deps.get(r).getVariable(), all, true, ((VariableRead) deps.get(r)).getDefiningFunction(), deps.get(r));
													}
												}
											}
										}
									}




									break;

								}

							}
							if (alias != null) break; 

						}
					}
				}
				/** 2 (TWO): Return value from function is used in write-of-interest **/
			} else if (next instanceof ReturnValueWrite
					// Decide between indexOf and equals...below 'else if' uses equals.
					&& next.getVariable().indexOf(name) == 0) {		

				// Add line to slice
				highlightLine(next, partialSlice);

				// Need to backwards slice both the dependencies on the call line (arguments) AND the return statement from the function


				// Backwards slice on arguments and base object (if class method)
				ArrayList<RWOperation> deps = null;
				try {
					deps = TraceHelper.getDataDependencies(all, (ReturnValueWrite) next);
				} catch (Exception e) {
					e.printStackTrace();
				}
				for (int z = 0; z < deps.size(); z++) {
				    highlightLine(deps.get(z), partialSlice);
					computeBackwardSlice(null, all.get(all.indexOf(deps.get(z))-1), deps.get(z).getVariable(), all, true, ((VariableRead) deps.get(z)).getDefiningFunction(), deps.get(z));
				}


				// Compute backwards slice on return statement dependencies
				for (int r = all.indexOf(next); r >= i; r--) {
					if (all.get(r) instanceof ReturnStatementValue
							// Might need to change the below line, currently 'ReturnStatementValue' doesn't save the function name (Apr. 23)
							&& ((ReturnStatementValue) all.get(r)).getFunctionName().equals(((ReturnValueWrite) next).getFunctionName())) {
						nestedTop = TraceHelper.getBeginningOfFunction((ReturnStatementValue) all.get(r), all);

						// TODO: implement TraceHelper.getReturnDependencies
						ArrayList<RWOperation> rsDependencies = TraceHelper.getReturnDependencies(all, (ReturnStatementValue) all.get(r));

						for (int w = 0; w < rsDependencies.size(); w++) {
							// TODO: What if an argument influenced the return value? --> do we need to allow slicing to exit the function?
							// NOT being run yet
			                highlightLine(rsDependencies.get(w), partialSlice);
							computeBackwardSlice(nestedTop, all.get(r), rsDependencies.get(w).getVariable(), all, true, "global", rsDependencies.get(w));
						}
					}
				}
				/** 3 (OLD): Basic slicing **/
			} else if (next instanceof VariableWrite && ((VariableWrite) next).getVariable().equals(name)) {

				// UPWARDS
				if (next instanceof ArgumentWrite) {
					if (all.indexOf(next) == 0) {
						// Specialer case, argument read was not included (iterative instrumentaiton not run long enough)
						highlightLine(next, partialSlice);
					} else {
						// Special case linking arguments from call to declaration
						for (int q = all.indexOf(next)-1; q >= 0; q--) {
							if (all.get(q) instanceof ArgumentRead 
									&& ((ArgumentRead) all.get(q)).getArgumentNumber() == ((ArgumentWrite) next).getArgumentNumber()
									&& ((ArgumentRead) all.get(q)).getFunctionName().equals(((ArgumentWrite) next).getFunctionName())
									&& ((ArgumentRead) all.get(q)).getValue().equals(((ArgumentWrite) next).getValue())) {


								// Add line to slice
								highlightLine(next, partialSlice);
								highlightLine(all.get(q), partialSlice);

								// Allowed to look through the parent/calling function for argument slice
								// GOOD TO GO

								computeBackwardSlice(null, all.get(q-1), all.get(q).getVariable(), all, true, ((VariableRead) all.get(q)).getDefiningFunction(), all.get(q));


								// Break from looking from Argument read
								found = true;
								break;
							}
						}
						if (!found) {
							// The argument write was found (assignment of the variable as an arugment)...
							// But the passing of the value into the function was not found in trace...
							// Most probably the call was not instrumented (not found). Best effort, give up
							highlightLine(next, partialSlice);
							writePartialSliceToDisk(partialSlice);
							return;
						}
					}
				} else {
					// Regular hard write (not argument write)
					ArrayList<RWOperation> deps = null;
					try {
						deps = TraceHelper.getDataDependencies(all, (VariableWrite) next);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for (int z = 0; z < deps.size(); z++) {
						if (deps.get(z).getVariable().split("\\.").length > 1) {
	                          highlightLine(deps.get(z), partialSlice);

							computeBackwardSlice(null,
									all.get(all.indexOf(deps.get(z))-1),
									deps.get(z).getVariable().split("\\.")[0],
									all,
									true,
									((VariableRead) deps.get(z)).getDefiningFunction()
									, deps.get(z));
						} else {
                            highlightLine(deps.get(z), partialSlice);

							computeBackwardSlice(null,
									all.get(all.indexOf(deps.get(z))-1),
									deps.get(z).getVariable(),
									all,
									true,
									((VariableRead) deps.get(z)).getDefiningFunction(), 
									deps.get(z));
						}
					}

					if (!(next instanceof VariableWriteAugmentAssign)) {
						// Previous writes are IRRELEVANT if it was NOT an augmented assignment i.e. -=, +=
						found = true;


					} else {
						// Add the augment assign line to the slice and continue looking for previous assign (non-augment)

						// Add line to slice
						highlightLine(next, partialSlice);

						int parentIfId = ControlMapper.getIfId(next.getLineNo(), next.getFile());
						if (parentIfId != -1) {
							IfStatement parentIf = ControlMapper.getIf(parentIfId);;
							int ifLine = parentIf.getLine();

							// go backwards will last read for if
							ArrayList<RWOperation> controlDeps = TraceHelper.getConditionalReads(all, next, ifLine);
							Iterator<RWOperation> ctrlIt = controlDeps.iterator();
							RWOperation nextCtrl = null;
							while (ctrlIt.hasNext()) {
								nextCtrl = ctrlIt.next();

								// Add control/branches to slice
								highlightLine(nextCtrl, partialSlice);

								computeBackwardSlice(null,
										nextCtrl,
										nextCtrl.getVariable(),
										all,
										true,
										((VariableRead) nextCtrl).getDefiningFunction(),
										nextCtrl);

							}
						}
					}


					// August 10th, slice the control dependencies!!!
					if (next.getFile() == null) {
						System.out.println("[SimpleExample2 678]: null file!");
						System.out.println(next.getOrder());
					}
					int parentIfId = ControlMapper.getIfId(next.getLineNo(), next.getFile());

					if (parentIfId != -1) {
						IfStatement parentIf = ControlMapper.getIf(parentIfId);;
						int ifLine = parentIf.getLine();

						// go backwards will last read for if
						ArrayList<RWOperation> controlDeps = TraceHelper.getConditionalReads(all, next, ifLine);
						Iterator<RWOperation> ctrlIt = controlDeps.iterator();
						RWOperation nextCtrl = null;
						while (ctrlIt.hasNext()) {
							nextCtrl = ctrlIt.next();

							// Add control/branches to slice
							highlightLine(nextCtrl, partialSlice);

							computeBackwardSlice(null,
									nextCtrl,
									nextCtrl.getVariable(),
									all,
									true,
									((VariableRead) nextCtrl).getDefiningFunction(), 
									nextCtrl);

						}
					}
				}

				//Relevant <WRITE> found! --> 'searchingOp'
				// If the line is not part of the slice...add it
				if (found) {
					// Add line to slice
					highlightLine(next, partialSlice);
					break;
				}
				/** 3.2 (OLD): Basic slicing **/
			} else if (next instanceof VariableWrite && ((VariableWrite) next).getVariable().indexOf(name) == 0
					&& ((VariableWrite) next).getVariable().indexOf(".") != -1) {

				// Backwards slice the right side! (Apr. 23)
				ArrayList<RWOperation> deps = null;
				try {
					deps = TraceHelper.getDataDependencies(all, (VariableWrite) next);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (int z = 0; z < deps.size(); z++) {
					computeBackwardSlice(null, all.get(all.indexOf(deps.get(z))-1), deps.get(z).getVariable(), all, true, ((VariableRead) deps.get(z)).getDefiningFunction(), deps.get(z));
	                // Add reads related to the write..to the slice
	                highlightLine(deps.get(z), partialSlice);
				}



				// Add line to slice
				highlightLine(next, partialSlice);


				/** 4 (FOUR) : Alias Detection/analysis  **/
			} else if (next instanceof VariableWrite
					&& TraceHelper.isComplex(((VariableWrite) next).getValue())) {
				// variable write covers property write as of now
				ArrayList<RWOperation> dependencies = new ArrayList<RWOperation>();

				try {
					dependencies = TraceHelper.getDataDependencies(all, (VariableWrite) next);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


				// RHS Reading
				for (int b = 0; b < dependencies.size(); b++) {

					// If the variable read on the right side is the variable being CURRENTLY sliced, and its value is
					// non-primitive...possible new alias created (by reference) check for updates via this alias...
					// Makes the assumption that the RHS is only 1 other variable (a NAME or PROPERTYGET)...
					// Object.merge etc. outside the scope for now.
					if (dependencies.get(b) instanceof VariableRead
							&& TraceHelper.isComplex(((VariableRead) dependencies.get(b)).getValue())
							&& dependencies.get(b).getVariable().indexOf(name) == 0) {

						//	if (trace.get(i).getVariable().indexOf(".") == -1) {
						// Variable write

						AliasAnalyzer.getNextWrite(next, bottom, all);

						if (next.getChildren().size() > 0) {

							next.includeInSlice();

							// Clear kids
							for (int k = 0; k < next.getChildren().size(); k++) {
								next.getChildren().get(k).setParent(null);
							}
							next.clearChildren();

							// TODO: ADD TO SLICE ALL THE FLAGGED RWOOOOOOOOOOOOO
							for (int o = all.indexOf(next); o <= all.indexOf(bottom); o++) {
								if (all.get(o).getSliceStatus() == true) {
									all.get(o).omitFromSlice();

									// Add linet to slice
									highlightLine(all.get(o), partialSlice);

									if (all.get(o) instanceof VariableWrite) {
										// NEW, TEST THIS
										// Need to slice all the dependencies for this write (this update through alias)
										ArrayList<RWOperation> deps = null;
										try {
											deps = TraceHelper.getDataDependencies(all, (VariableWrite) all.get(o));
										} catch (Exception e) {
											e.printStackTrace();
										}
										for (int r = 0; r < deps.size(); r++) {
				                            highlightLine(deps.get(r), partialSlice);

											// CANT be computing slice from bottom
											computeBackwardSlice(null, all.get(all.indexOf(deps.get(r))-1), deps.get(r).getVariable(), all, true, ((VariableRead) deps.get(r)).getDefiningFunction(), deps.get(r));
										}
									}
								}
							}
						}

						// Not sure about this, does a backwards slice of alias (overkill?)

						// Don't need this, only need forward slice for aliases?

						//computeBackwardSlice(next, bottom, next.getVariable(), all, false);


						break;
					} else if (dependencies.get(b) instanceof PropertyRead
							&& dependencies.get(b).getVariable().indexOf(bottom.getVariable()) == 0) {
					}
				}
			} else if (next instanceof PropertyRead && next.getVariable().equals(name)) {
				int propIndex = -1;
				try {
					propIndex = TraceHelper.getAtomicIndex(all, (PropertyRead) next);
					VariableRead baseRead = (VariableRead) all.get(propIndex);

					if (baseRead.getValue().equals("[object XMLHttpRequest]") && (((PropertyRead) next).getProperty().equals("open")  ||   ((PropertyRead) next).getProperty().equals("send"))) {
						highlightLine(next, partialSlice);
						ArrayList<RWOperation> deps = TraceHelper.getDataDependenciesLoose(all, next);



						for (int r = 0; r < deps.size(); r++) {
		                      highlightLine(deps.get(r), partialSlice);

							computeBackwardSlice(null, deps.get(r), deps.get(r).getVariable(), all, true, ((VariableRead) deps.get(r)).getDefiningFunction(), deps.get(r));
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		writePartialSliceToDisk(partialSlice);
	}

	private void computeForwardSlice(RWOperation top, RWOperation bottom, String name, ArrayList<RWOperation> all) {
		// TODO:



		AliasAnalyzer.getNextWrite(top, bottom, all);



	}
	
	private void writePartialSliceToDisk (ArrayList<RWOperation> p) {
        mapper2.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(
                Visibility.ANY));
        mapper2.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper2.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // to allow coercion of JSON empty String ("") to null Object value:
        mapper2.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper2.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            mapper2.writeValue(new File("src/main/webapp/lines/"+p.get(0).getOrder()+".json"),
                    p);
        } catch (JsonGenerationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println(p);
            System.out.println(p.size());
        }
	}


	private void highlightLine(RWOperation o, ArrayList<RWOperation> p) {
	    p.add(o);

		// Add new file -> line number mapping if not already present
		if (TraceHelper.getFileLineMapping(o.getFile(), theSlice) == null) {
			theSlice.add(new FileLineNumber(o.getFile()));
		} 
		// Add line to existing mapping
		TraceHelper.getFileLineMapping(o.getFile(), theSlice).addLine(o.getLineNo());
		
		// Don't wan't duplicate operations in slice
		Iterator<RWOperation> opIt = expandedSlice.iterator();
		boolean found = false;
		while (opIt.hasNext()) {
			if (opIt.next().getOrder() == o.getOrder()) {
				found = true;
				break;
			}
		}
		if (!found) {
			expandedSlice.add(o);
		}
	}

}



