package org.purl.wf4ever.astrotaverna.voutils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import java.util.Map;
import java.util.regex.Pattern;

import org.purl.wf4ever.astrotaverna.voutils.TemplateFillerActivityConfigurationBean;
import org.purl.wf4ever.astrotaverna.utils.MyUtils;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

import net.sf.taverna.t2.invocation.InvocationContext;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.processor.activity.AbstractAsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;

/**
 * 
 * @author Julian Garrido
 * @since    19 May 2011
 */
public class TemplateFillerActivity extends
		AbstractAsynchronousActivity<TemplateFillerActivityConfigurationBean>
		implements AsynchronousActivity<TemplateFillerActivityConfigurationBean> {

	/*
	 * Best practice: Keep port names as constants to avoid misspelling. This
	 * would not apply if port names are looked up dynamically from the service
	 * operation, like done for WSDL services.
	 */
	private static final String IN_FIRST_INPUT_TABLE = "voTable";
	private static final String IN_SECOND_INPUT = "template";
	private static final String IN_FILTER = "ColumnNameWithResultFileName";
	//private static final String IN_OUTPUT_TABLE_NAME = "outputFileNameIn";

	private static final String OUT_LIST = "list";
	private static final String OUT_REPORT = "report";
	
	private TemplateFillerActivityConfigurationBean configBean;

	@Override
	public void configure(TemplateFillerActivityConfigurationBean configBean)
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
		
		/*if(!(      configBean.getTypeOfFilter().compareTo("Column names")==0 
				|| configBean.getTypeOfFilter().compareTo("UCDs")==0)){
			throw new ActivityConfigurationException(
					"Invalid filter type for the tables");
		}*/
		
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
		addInput(IN_SECOND_INPUT, 0, true, null, String.class);
		addInput(IN_FILTER, 0, true, null, String.class);
		
		
		//if(configBean.getTypeOfInput().compareTo("File")==0){
		//	addInput(IN_OUTPUT_TABLE_NAME, 0, true, null, String.class);
		//}
		

		// Optional ports depending on configuration
		//if (configBean.getExampleString().equals("specialCase")) {
		//	// depth 1, ie. list of binary byte[] arrays
		//	addInput(IN_EXTRA_DATA, 1, true, null, byte[].class);
		//	addOutput(OUT_REPORT, 0);
		//}
		
		// Single value output port (depth 1)
		addOutput(OUT_LIST, 1);
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
			
			/*
			 * Check if the mandatory inputs are not null
			 */
			public boolean areMandatoryInputsNotNull(){
				boolean validStatus = true;
				try{
					if(inputs.get(IN_FIRST_INPUT_TABLE)==null
							|| inputs.get(IN_FILTER)==null
							|| inputs.get(IN_SECOND_INPUT)==null)
						validStatus = false;
					//else if(configBean.getTypeOfInput().compareTo("File")==0 
					//		&& inputs.get(IN_OUTPUT_TABLE_NAME)==null)
					//	validStatus = false;
				}catch(Exception ex){validStatus = false;}
				
				return validStatus;
			}
			
			public void run() {
				
				boolean callbackfails=false;
				File tmpInFile = null;
				
				if(areMandatoryInputsNotNull()){
					InvocationContext context = callback.getContext();
					ReferenceService referenceService = context.getReferenceService();
					// Resolve inputs 				
					String inputTable = (String) referenceService.renderIdentifier(inputs.get(IN_FIRST_INPUT_TABLE), String.class, context);
					String secondInput = (String) referenceService.renderIdentifier(inputs.get(IN_SECOND_INPUT), String.class, context);
					String filter = (String) referenceService.renderIdentifier(inputs.get(IN_FILTER), String.class, context);
					StarTable table = null;
					
					
					//String lastInput = (String) referenceService.renderIdentifier(inputs.get(IN_OUTPUT_TABLE_NAME), 
					//		String.class, context);
	
					//boolean optionalPorts = configBean.getTypeOfInput().compareTo("File")==0;
					
					//String outputTableName = null;
					//if(optionalPorts && inputs.containsKey(IN_OUTPUT_TABLE_NAME)){ //configBean.getNumberOfTables()==3
					//	outputTableName = (String) referenceService.renderIdentifier(inputs.get(IN_OUTPUT_TABLE_NAME), 
					//			String.class, context);
					//}
	
					
					if(configBean.getTypeOfInput().compareTo("File")==0){
						File file = new File(inputTable);
						if(!file.exists()){
							callback.fail("Input table file does not exist: "+ inputTable,new IOException());
							callbackfails = true;
						}else{
							try{
								table = loadVOTable(file);
							}catch (IOException ex) {
								callback.fail("Invalid service call while reading the table", ex);
								callbackfails = true;
							}							
						}
						
						file = new File(secondInput);
						if(!file.exists()){
							callback.fail("Template file does not exist: "+ secondInput,new IOException());
							callbackfails = true;
						}else{
							try{
								secondInput = MyUtils.readFileAsString(secondInput);
							}catch (IOException ex) {
								callback.fail("Invalid service call while reading the template", ex);
								callbackfails = true;
							}							
						}
					}
					
					if(configBean.getTypeOfInput().compareTo("URL")==0){
						try {
							URI exampleUri = new URI(inputTable);
							table = loadVOTable(exampleUri);
						} catch (URISyntaxException e) {
							callback.fail("Invalid URL: "+ inputTable,e);
							callbackfails = true;
						}catch (IOException e) {
							callback.fail("Invalid service call while reading the table", e);
							callbackfails = true;
						}
						
						File file = new File(secondInput);
						if(!file.exists()){
							callback.fail("Template file does not exist: "+ secondInput,new IOException());
							callbackfails = true;
						}else{
							try{
								secondInput = MyUtils.readFileAsString(secondInput);
							}catch (IOException ex) {
								callback.fail("Invalid service call while reading the template", ex);
								callbackfails = true;
							}							
						}
					}
					
					
					//prepare tmp input files if needed
					if(configBean.getTypeOfInput().compareTo("String")==0){
						try{
							tmpInFile = MyUtils.writeStringAsTmpFile(inputTable);
							tmpInFile.deleteOnExit();
							inputTable = tmpInFile.getAbsolutePath();
							try{
								table = loadVOTable(tmpInFile);
							}catch (IOException ex) {
								callback.fail("Invalid service call while reading the table", ex);
								callbackfails = true;
							}	
						}catch(Exception ex){
							callback.fail("It wasn't possible to create a temporary file",ex);
							callbackfails = true;
						}
					}
					
					if(table ==null || secondInput == null)
						callbackfails = true;
					
					//prepare tmp output files if needed
					/*if(configBean.getTypeOfInput().compareTo("String")==0
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
					*/
					
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
					
					
					
					if(!callbackfails){
						Map<String, Integer> mapping = new HashMap<String, Integer>();
						Vector<String> mappingPos = new Vector<String>();
						//get the column info
						int nCol = table.getColumnCount();
						for(int i =0; i< nCol; i++){
							ColumnInfo colInfo = table.getColumnInfo(i);
							mapping.put(colInfo.getName(), i);  //names to positions
						    mappingPos.add(colInfo.getName());  //positions to names
						   //mappingIds.put(colInfo.getName(), (String) colInfo.getAuxDatumValue(VOStarTable.ID_INFO, String.class));
						}
						
					    try{
					    	ArrayList<String> reportList = new ArrayList<String>();
							String report = "";
							RowSequence rseq = table.getRowSequence();
					        while ( rseq.next() ) {
					        	Object [] arrayCells = rseq.getRow();
					        	
					        	String template = secondInput;
					        	int k = 0;
					        	for (Object obj: arrayCells){
					        		//System.out.println("replace/"+"$"+mappingPos.get(k)+"$/"+obj.toString()+"/");
					        		template = template.replaceAll(Pattern.quote("$")+mappingPos.get(k)+Pattern.quote("$"), obj.toString());
					        		k++;
					        	}
					        	//System.out.println(configGalfit);
					        	//System.out.println("Config File: " +rseq.getCell(  (int) mapping.get(columnNameWithConfigFile)  ));
					        	String fileName = (String) rseq.getCell(mapping.get(filter).intValue()  );
					        	MyUtils.writeStringToAFile(fileName, template);
					        	//report += fileName + "\n";
					        	reportList.add(fileName);
					        }
					        rseq.close();
				        
							
							if(!callbackfails){
								// Register outputs
								Map<String, T2Reference> outputs = new HashMap<String, T2Reference>();
								String simpleoutput = report;//"simple-report";

									T2Reference simpleRef = referenceService.register(reportList, 1, true, context);
									outputs.put(OUT_LIST, simpleRef);
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
					    }catch(Exception ex){
							callback.fail("Exception during the process: \n"+ ex.getMessage());
							callbackfails = true;
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
	public TemplateFillerActivityConfigurationBean getConfiguration() {
		return this.configBean;
	}

	public StarTable loadVOTable( File source ) throws IOException {
	    return new StarTableFactory().makeStarTable( source.toString(), "votable" );
	}
	
	public StarTable loadVOTable( URI source ) throws IOException {
	    return new StarTableFactory().makeStarTable( source.toString(), "votable" );
	}
}
