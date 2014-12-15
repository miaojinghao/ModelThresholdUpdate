package com.sharethis.adoptimization.campaigndata;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;


/**
 * main class to run the computation of the price-volume.
 * The command line to run: hadoop jar PriceOptRunner.jar com.sharethis.adoptimization.clickthroughrate.HourlyGeneratingCTRTaskMain
 */


public class HourlyGeneratingDataTaskMain
{
	private static Logger sLogger = Logger.getLogger(HourlyGeneratingDataTaskMain.class);
		
    public static void main(String[] args) throws Exception {
		sLogger.info("Starting the task to generate the ctr ...\n");
		Configuration config = new Configuration();
        Class<?> jobClass = Class.forName("com.sharethis.adoptimization.campaigndata.HourlyGeneratingDataTask");
        Tool tool = (Tool) jobClass.newInstance();
        sLogger.info("Found class name : " + jobClass.getName());
        int res = ToolRunner.run(config, tool, args);
        System.exit(res);
		sLogger.info("Ended the task to generate the ctr!\n");
    }
}