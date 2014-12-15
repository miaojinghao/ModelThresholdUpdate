package com.sharethis.adoptimization.clickthroughrate;

import cascading.operation.Filter;
import cascading.operation.Function;
import cascading.operation.Identity;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.scheme.Scheme;
import cascading.scheme.hadoop.TextDelimited;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.log4j.Logger;

import com.sharethis.adoptimization.common.AssigningRandomInt;
import com.sharethis.adoptimization.common.FilterOutDataNEInt;
import com.sharethis.adoptimization.common.RandomlyPickingKeyCategory;
import com.sharethis.adoptimization.common.RandomlyPickingKeyValue;

/**
 * This is the assembly to load the data.
 */

public class ModLoadingHourlyClick extends SubAssembly
{
	private static final Logger sLogger = Logger.getLogger(ModLoadingHourlyClick.class);
	private static final long serialVersionUID = 1L;
	private static String mDelimiter = "\t";
		
	public ModLoadingHourlyClick(Map<String, Tap> sources, String ctrFilePath, String pDateStr, Fields dFields, 
			Class[] types, Configuration config, String pipeName, String filePath, 
			Map<String, List<String>> setCatMap, Map<String, List<String>> crtvCatMap, Fields newCatFields) 
		throws IOException, ParseException, Exception
	{
		try{		

			Scheme dSourceScheme = new TextDelimited(dFields, false, mDelimiter, types);
            Pipe dAssembly = new Pipe(pipeName);
 
           	// Building the taps from the data files
            Tap cTap = new Hfs(dSourceScheme, filePath);
    		JobConf	jobConf = new JobConf();
    		if(cTap.resourceExists(jobConf)){
    			sLogger.info(filePath + " is used.");
				sources.put(pipeName, cTap);				
				
				// - Randomly picking user segment
				// - Randomly picking vertical
//				int uFieldInd = dFields.getPos("user_seg_list");
//				int vFieldInd = dFields.getPos("vertical_list");

//				Fields newFields = dFields.append(newKeyFields);
//				Function<?> pFunc = new RandomlyPickingKeyValue(newFields, dFields, uFieldInd, vFieldInd);
//				dAssembly = new Each(dAssembly, dFields, pFunc, Fields.RESULTS);	
				
				//Expanding the data for ShareThis adslot category and creative category.
				int setFieldInd = dFields.getPos("setting_id");
				int crtvFieldInd = dFields.getPos("creative_id");
				Function<?> cFunc = new RandomlyPickingKeyCategory(dFields.append(newCatFields), 
						dFields, setFieldInd, crtvFieldInd, setCatMap, crtvCatMap);
				dAssembly = new Each(dAssembly, dFields, cFunc, Fields.RESULTS);	
				
				setTails(dAssembly);            
    		}else{
    			sLogger.info("The data path: " + filePath + " does not exist.");				    				
            	setTails(new Pipe("No_Data", null));
    		}    				
		}catch(Exception ee){
			sLogger.info(ee.toString());
			throw new Exception(ee);
		}
	}
}