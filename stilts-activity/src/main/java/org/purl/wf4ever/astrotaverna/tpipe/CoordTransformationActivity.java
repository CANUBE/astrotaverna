package org.purl.wf4ever.astrotaverna.tpipe;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.taverna.t2.invocation.InvocationContext;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.processor.activity.AbstractAsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;

import org.purl.wf4ever.astrotaverna.utils.MyUtils;
import org.purl.wf4ever.astrotaverna.utils.NoExitSecurityManager;

import uk.ac.starlink.ttools.Stilts;

/**
 * Activity configuration bean
 * @author Julian Garrido
 * @since    19 May 2011
 */
public class CoordTransformationActivity extends
		AbstractAsynchronousActivity<CoordTransformationActivityConfigurationBean>
		implements AsynchronousActivity<CoordTransformationActivityConfigurationBean> {

	/*
	 * Best practice: Keep port names as constants to avoid misspelling. This
	 * would not apply if port names are looked up dynamically from the service
	 * operation, like done for WSDL services.
	 */
	private static final String IN_FIRST_INPUT_TABLE = "voTable";
	private static final String IN_NAME_NEW_COL = "nameNewCol";
	private static final String IN_OUTPUT_TABLE_NAME = "outputFileNameIn";

	private static final String OUT_SIMPLE_OUTPUT = "outputTable";
	private static final String OUT_REPORT = "report";
	
	private CoordTransformationActivityConfigurationBean configBean;

	@Override
	public void configure(CoordTransformationActivityConfigurationBean configBean)
			throws ActivityConfigurationException {

		// Any pre-config sanity checks
		//if (!configBean.getTablefile1().exists()) {
		//	throw new ActivityConfigurationException(
		//			"Input table file 1 doesn't exist");
		//}
		
		if(!(      configBean.getTypeOfInput().compareTo("File")==0
				|| configBean.getTypeOfInput().compareTo("URL")==0
				|| configBean.getTypeOfInput().compareTo("String")==0)){
			throw new ActivityConfigurationException(
					"Invalid input type for the tables");
		}
		
		if(!isAllowedCoordenatesFunction(configBean.getTypeOfFilter())){
			throw new ActivityConfigurationException(
					"Invalid function name");
		}
		
		// Store for getConfiguration(), but you could also make
		// getConfiguration() return a new bean from other sources
		this.configBean = configBean;

		// OPTIONAL: 
		// Do any server-side lookups and configuration, like resolving WSDLs

		// myClient = new MyClient(configBean.getExampleUri());
		// this.service = myClient.getService(configBean.getExampleString());

		
		// REQUIRED: (Re)create input/output ports depending on configuration
		configurePorts();
	}

	protected void configurePorts() {
		// In case we are being reconfigured - remove existing ports first
		// to avoid duplicates
		removeInputs();
		removeOutputs();

		// FIXME: Replace with your input and output port definitions

		// Hard coded input port, expecting a single String
		//File name for the Input tables
		addInput(IN_FIRST_INPUT_TABLE, 0, true, null, String.class);
		addInput(IN_NAME_NEW_COL, 0, true, null, String.class);
		
		
		if(configBean.getTypeOfInput().compareTo("File")==0){
			addInput(IN_OUTPUT_TABLE_NAME, 0, true, null, String.class);
		}
		
		
		//coordenates parameters
		Vector<String> inParams = getNameParamsOfCoordFunctions(configBean.getTypeOfFilter());
		if(inParams!=null)
			for(String param : inParams){
				addInput(param, 0, true, null, String.class);
			}
		

		
		// Single value output port (depth 0)
		addOutput(OUT_SIMPLE_OUTPUT, 0);
		// Single value output port (depth 0)
		addOutput(OUT_REPORT, 0);

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executeAsynch(final Map<String, T2Reference> inputs,
			final AsynchronousActivityCallback callback) {
		// Don't execute service directly now, request to be run ask to be run
		// from thread pool and return asynchronously
		callback.requestRun(new Runnable() {
			
			public boolean areMandatoryInputsNotNull(){
				boolean validStatus = true;
				
				if(inputs.get(IN_FIRST_INPUT_TABLE)==null
						|| inputs.get(IN_NAME_NEW_COL)==null){
					validStatus = false;
				}else{
					if(configBean.getTypeOfInput().compareTo("File")==0 
						&& inputs.get(IN_OUTPUT_TABLE_NAME)==null){
						validStatus = false;
					} else{
						Vector<String> inParams = getNameParamsOfCoordFunctions(configBean.getTypeOfFilter());
						if(inParams!=null)
							for(String param : inParams){
								if(inputs.get(param)==null)
									validStatus = false;
							}
					}
				}
				
				return validStatus;
			}
			
			public void run() {
				Vector<String> inParams;
				Vector<String> paramValues;
				boolean callbackfails=false;
				File tmpInFile = null;
				File tmpOutFile = null;
				
				if(areMandatoryInputsNotNull()){
					InvocationContext context = callback.getContext();
					ReferenceService referenceService = context.getReferenceService();
					// Resolve inputs 				
					String inputTable = (String) referenceService.renderIdentifier(inputs.get(IN_FIRST_INPUT_TABLE), String.class, context);
					String nameNewCol = (String) referenceService.renderIdentifier(inputs.get(IN_NAME_NEW_COL), String.class, context);
					
					
					boolean optionalPorts = configBean.getTypeOfInput().compareTo("File")==0;
					
					String outputTableName = null;
					if(optionalPorts && inputs.containsKey(IN_OUTPUT_TABLE_NAME)){ //configBean.getNumberOfTables()==3
						outputTableName = (String) referenceService.renderIdentifier(inputs.get(IN_OUTPUT_TABLE_NAME), 
								String.class, context);
					}
					
					inParams = getNameParamsOfCoordFunctions(configBean.getTypeOfFilter());
					paramValues = new Vector<String>();
					if(inParams!=null && !inParams.isEmpty()){
						for(String param : inParams)
							if(inputs.containsKey(param))
								paramValues.add((String) referenceService.renderIdentifier(inputs.get(param), 
										String.class, context));
					}else{
						callback.fail("Lack of params in the coordenates function",new Exception());
						callbackfails = true;
					}
						
					
	
					//check correct input values
					
					if(configBean.getTypeOfInput().compareTo("File")==0){
						File file = new File(inputTable);
						if(!file.exists()){
							callback.fail("Input table file does not exist: "+ inputTable,new IOException());
							callbackfails = true;
						}
					}
					
					if(configBean.getTypeOfInput().compareTo("URL")==0){
						try {
							URI exampleUri = new URI(inputTable);
						} catch (URISyntaxException e) {
							callback.fail("Invalid URL: "+ inputTable,e);
							callbackfails = true;
						}
					}
					
					//check variable params of the function
					if(inParams.size()!=paramValues.size()){
						callback.fail("Expected number of parameters for the function: "+ inParams.size()+".\nReceived number of paramaters: "+ paramValues.size(),new Exception());
						callbackfails = true;
					}
					if(paramValues.size()>0){
						boolean nullvalues = false;
						for(int i = 0; i<paramValues.size() && !nullvalues;i++)
							if(paramValues.elementAt(i)==null || paramValues.elementAt(i).isEmpty()){
								nullvalues = true;
								callback.fail("Function parameters are empty",new Exception());
								callbackfails = true;
							}
					}
					
					
					//prepare tmp input files if needed
					if(configBean.getTypeOfInput().compareTo("String")==0){
						try{
							tmpInFile = MyUtils.writeStringAsTmpFile(inputTable);
							tmpInFile.deleteOnExit();
							inputTable = tmpInFile.getAbsolutePath();
						}catch(Exception ex){
							callback.fail("It wasn't possible to create a temporary file",ex);
							callbackfails = true;
						}
					}
					
					//prepare tmp output files if needed
					if(configBean.getTypeOfInput().compareTo("String")==0
							|| configBean.getTypeOfInput().compareTo("URL")==0){
						try{
							tmpOutFile = File.createTempFile("astro", null);
							tmpOutFile.deleteOnExit();
							outputTableName = tmpOutFile.getAbsolutePath();
						}catch(Exception ex){
							callback.fail("It wasn't possible to create a temporary file",ex);
							callbackfails = true;
						}
					}
					
					// Support our configuration-dependendent input
					//boolean optionalPorts = configBean.getExampleString().equals("specialCase"); 
					
					//List<byte[]> special = null;
					// We'll also allow IN_EXTRA_DATA to be optionally not provided
					//if (optionalPorts && inputs.containsKey(IN_EXTRA_DATA)) {
					//	// Resolve as a list of byte[]
					//	special = (List<byte[]>) referenceService.renderIdentifier(
					//			inputs.get(IN_EXTRA_DATA), byte[].class, context);
					//}
					
	
					// TODO: Do the actual service invocation
	//				try {
	//					results = this.service.invoke(firstInput, special)
	//				} catch (ServiceException ex) {
	//					callback.fail("Could not invoke Stilts service " + configBean.getExampleUri(),
	//							ex);
	//					// Make sure we don't call callback.receiveResult later 
	//					return;
	//				}
					
					//Performing the work: Stilts functinalities
					String [] parameters;
					
					if(!callbackfails){
						String  functionName;
						String commaSeparatedValues;
						
									
						commaSeparatedValues = MyUtils.toCommaSeparatedString(paramValues);
						functionName = ((Map<String, String>)CoordTransformationActivity.getFunctionsNameMap()).get(configBean.getTypeOfFilter());
						
						
						parameters = new String[6];
						parameters[0] = "tpipe";
						parameters[1] = "ifmt=votable";
						parameters[2] = "in="+inputTable;
						parameters[3] = "ofmt=votable";
						parameters[4] = "cmd=addcol "+ nameNewCol +" '(" + functionName + "("+ commaSeparatedValues +"))'";
						//System.out.println(parameters[4]);
						//parameters[4] = "cmd=addcol newCol '(raFK4toFK5radians(U, R))'";
						parameters[5] = "out="+outputTableName;
						
							
						SecurityManager securityBackup = System.getSecurityManager();
						System.setSecurityManager(new NoExitSecurityManager());
						
						try{
							System.setProperty("votable.strict", "false");
							Stilts.main(parameters);
						}catch(SecurityException ex){
							callback.fail("Invalid service call: check the input parameters", ex);
							callbackfails = true;
						}
					
						System.setSecurityManager(securityBackup);
						
						if(!callbackfails){
							// Register outputs
							Map<String, T2Reference> outputs = new HashMap<String, T2Reference>();
							String simpleValue = "";// //Name of the output file or result
							String simpleoutput = "simple-report";
							
							if(optionalPorts){ //case File
								simpleValue = outputTableName;
							}else if(configBean.getTypeOfInput().compareTo("URL")==0
										|| configBean.getTypeOfInput().compareTo("String")==0){
								
								try{
									simpleValue = MyUtils.readFileAsString(tmpOutFile.getAbsolutePath());
								}catch (Exception ex){
									callback.fail("It wasn't possible to read the result from a temporary file", ex);
									callbackfails = true;
								}
								
							}
			
							T2Reference simpleRef = referenceService.register(simpleValue, 0, true, context);
							outputs.put(OUT_SIMPLE_OUTPUT, simpleRef);
							T2Reference simpleRef2 = referenceService.register(simpleoutput,0, true, context); 
							outputs.put(OUT_REPORT, simpleRef2);
			
							// For list outputs, only need to register the top level list
							//List<String> moreValues = new ArrayList<String>();
							//moreValues.add("Value 1");
							//moreValues.add("Value 2");
							//T2Reference moreRef = referenceService.register(moreValues, 1, true, context);
							//outputs.put(OUT_MORE_OUTPUTS, moreRef);
			
							//if (optionalPorts) {
							//	// Populate our optional output port					
							//	// NOTE: Need to return output values for all defined output ports
							//	String report = "Everything OK";
							//	outputs.put(OUT_REPORT, referenceService.register(report,
							//			0, true, context));
							//}
							
							// return map of output data, with empty index array as this is
							// the only and final result (this index parameter is used if
							// pipelining output)
							callback.receiveResult(outputs, new int[0]);
						}
					}
				}else{ //End if isthereMandatoryInputs
					callback.fail("Mandatory inputs doesn't have any value");
					callbackfails = true;
				}
			}
		});
	}

	@Override
	public CoordTransformationActivityConfigurationBean getConfiguration() {
		return this.configBean;
	}

	/*
	 * Returns a Vector that contains the implemented functions
	 */
	public Vector<String> getListOfCoordenatesFunctions(){
		String [] array1parameters ={"radiansToDms", "radiansToHms", "dmsToRadians", "hmsToRadians", "hoursToRadians", "degreesToRadians", "radiansToDegrees"};
		String [] array2parameters ={"raFK4toFK5radians2", "decFK4toFK5radians2", "raFK5toFK4radians2", "decFK5toFK4radians2"};
		String [] array3parameters ={"raFK4toFK5Radians3", "decFK4toFK5Radians3", "raFK5toFK4Radians3", "decFK5toFK4Radians3"};
		String [] array4parameters ={"skyDistanceRadians"};
		
		Vector<String> vAll = new Vector (Arrays.asList(array1parameters));
		vAll.addAll(Arrays.asList(array2parameters));
		vAll.addAll(Arrays.asList(array3parameters));
		vAll.addAll(Arrays.asList(array4parameters));

		return vAll;
	}
	
	public boolean isAllowedCoordenatesFunction(String name){
		Vector<String> functions = this.getListOfCoordenatesFunctions();
		boolean found=false;
		Iterator<String> it = functions.iterator();
	
		while( it.hasNext() && !found ){
			if(it.next().compareTo(name)==0)
				found = true;
		}
		
		return found;
	}
	

	/*
	 * Returns an array that contains the inputs parameters name for each function.
	 */
	static Vector<String> getNameParamsOfCoordFunctions(String coordenatesFunction){
		Vector<String> params = new Vector<String>();
		
		
		if(coordenatesFunction.compareTo("radiansToDms")==0
				|| coordenatesFunction.compareTo("radiansToHms")==0
				|| coordenatesFunction.compareTo("hoursToRadians")==0
				|| coordenatesFunction.compareTo("degreesToRadians")==0
				|| coordenatesFunction.compareTo("radiansToDegrees")==0){
			
			params.add("nameColumn");
			
		}else if(coordenatesFunction.compareTo("dmsToRadians")==0
				|| coordenatesFunction.compareTo("hmsToRadians")==0){
			
			params.add("nameColumn");
			
		}else if(coordenatesFunction.compareTo("raFK4toFK5radians2")==0
				|| coordenatesFunction.compareTo("decFK4toFK5radians2")==0
				|| coordenatesFunction.compareTo("raFK5toFK4radians2")==0
				|| coordenatesFunction.compareTo("decFK5toFK4radians2")==0){

			params.add("nameRA");
			params.add("nameDEC");
			
		}else if(coordenatesFunction.compareTo("raFK4toFK5Radians3")==0
				|| coordenatesFunction.compareTo("decFK4toFK5Radians3")==0
				|| coordenatesFunction.compareTo("raFK5toFK4Radians3")==0
				|| coordenatesFunction.compareTo("decFK5toFK4Radians3")==0){
			
			params.add("nameRA");
			params.add("nameDEC");
			params.add("namebepoch");
			
		} else if(coordenatesFunction.compareTo("skyDistanceRadians")==0){
			
			params.add("nameRA1");
			params.add("nameDEC1");
			params.add("nameRA2");
			params.add("nameDEC2");
			
		} else {
			params.add("Value");
		}
			
		
		return params;
	}
	
	/*
	 * mapping between the name in the user interface and the real function name
	 */
	static Map<String, String> getFunctionsNameMap(){
		//<UI function name, real function name> 
		Map<String, String> mapping = new HashMap<String, String>();
		
		mapping.put("radiansToDms", "radiansToDms");
		mapping.put("radiansToHms", "radiansToHms");
		mapping.put("dmsToRadians", "dmsToRadians");
		mapping.put("hmsToRadians", "hmsToRadians");
		mapping.put("hoursToRadians", "hoursToRadians");
		mapping.put("degreesToRadians", "degreesToRadians");
		mapping.put("radiansToDegrees", "radiansToDegrees");
		
		mapping.put("raFK4toFK5radians2", "raFK4toFK5radians");
		mapping.put("decFK4toFK5radians2", "decFK4toFK5radians");
		mapping.put("raFK5toFK4radians2", "raFK5toFK4radians");
		mapping.put("decFK5toFK4radians2", "decFK5toFK4radians");
		
		mapping.put("raFK4toFK5radians3", "raFK4toFK5radians");
		mapping.put("decFK4toFK5radians3", "decFK4toFK5radians");
		mapping.put("raFK5toFK4radians3", "raFK5toFK4radians");
		mapping.put("decFK5toFK4radians3", "decFK5toFK4radians");
		
		mapping.put("skyDistanceRadians", "skyDistanceRadians");

		return mapping;
		
	}
	
	
/*	static Vector<Parameter<String, Object>> getNameParamsOfCoordFunctions(String coordenatesFunction){
		Vector<Parameter<String, Object>> params = new Vector<Parameter<String,Object>>();
		Parameter<String, Object> result;
		
		if(coordenatesFunction.compareTo("radiansToDms")==0
				|| coordenatesFunction.compareTo("radiansToHms")==0
				|| coordenatesFunction.compareTo("hoursToRadians")==0
				|| coordenatesFunction.compareTo("degreesToRadians")==0
				|| coordenatesFunction.compareTo("radiansToDegrees")==0){
			
			result = new Parameter<String, Object>("value", float.class);
			params.add(result);
			
		}else if(coordenatesFunction.compareTo("dmsToRadians")==0
				|| coordenatesFunction.compareTo("hmsToRadians")==0){
			
			result = new Parameter<String, Object>("value", String.class);
			params.add(result);
			
		}else if(coordenatesFunction.compareTo("raFK4toFK5radians2")==0
				|| coordenatesFunction.compareTo("decFK4toFK5radians2")==0
				|| coordenatesFunction.compareTo("raFK5toFK4radians2")==0
				|| coordenatesFunction.compareTo("decFK5toFK4radians2")==0){

			result = new Parameter("RA", float.class);
			params.add(result);
			result = new Parameter("DEC", float.class);
			params.add(result);
			
		}else if(coordenatesFunction.compareTo("raFK4toFK5radians3")==0
				|| coordenatesFunction.compareTo("decFK4toFK5radians3")==0
				|| coordenatesFunction.compareTo("raFK5toFK4radians3")==0
				|| coordenatesFunction.compareTo("decFK5toFK4radians3")==0){
			
			result = new Parameter("RA", float.class);
			params.add(result);
			result = new Parameter("DEC", float.class);
			params.add(result);
			result = new Parameter("bepoch", float.class);
			params.add(result);
			
		} if(coordenatesFunction.compareTo("skyDistanceRadians")==0){
			
			result = new Parameter("RA1", float.class);
			params.add(result);
			result = new Parameter("DEC1", float.class);
			params.add(result);
			result = new Parameter("RA2", float.class);
			params.add(result);
			result = new Parameter("DEC2", float.class);
			params.add(result);
			
		} else {
			result = new Parameter("value", String.class);
			params.add(result);
		}
			
		
		return params;
	}
	*/
	/*
	public static class Parameter<String,Object> {
	    private String name;
	    private Object className;
	    public Parameter(String name, Object className){
	        this.name = name;
	        this.className = className;
	    }

	    public String getName(){ return name; }
	    public Object getClassName(){ return className; }
	    public void setName(String name){ this.name = name; }
	    public void setClassName(Object className){ this.className = className; }
	}

	*/
	
	
}
