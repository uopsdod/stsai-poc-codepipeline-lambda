package com.amazonaws.lambda.demo;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.codepipeline.AWSCodePipeline;
import com.amazonaws.services.codepipeline.AWSCodePipelineClientBuilder;
import com.amazonaws.services.codepipeline.model.FailureDetails;
import com.amazonaws.services.codepipeline.model.FailureType;
import com.amazonaws.services.codepipeline.model.PutJobFailureResultRequest;
import com.amazonaws.services.codepipeline.model.PutJobFailureResultResult;
import com.amazonaws.services.codepipeline.model.PutJobSuccessResultRequest;
import com.amazonaws.services.codepipeline.model.PutJobSuccessResultResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {
	
    AWSCodePipeline codepipeline_client = AWSCodePipelineClientBuilder.defaultClient();

    @Override
    public String handleRequest(Object input, Context context) {
        context.getLogger().log("stsai-poc-codepipeline-lambda Input: " + input);
        
        Gson gson = new Gson();
        JsonObject inputJsonObj = gson.toJsonTree(input).getAsJsonObject();
        context.getLogger().log("stsai-poc-codepipeline-lambda inputJsonObj: " + inputJsonObj);
        JsonObject CodePipeline_job = inputJsonObj.get("CodePipeline.job").getAsJsonObject();
        context.getLogger().log("stsai-poc-codepipeline-lambda CodePipeline_job: " + CodePipeline_job);

        /** get job id **/
        String jobId = CodePipeline_job.get("id").getAsString();
        context.getLogger().log("stsai-poc-codepipeline-lambda jobId: " + jobId);
        
        try {
	        
	        /** get input params - userParam **/
	        JsonObject data = CodePipeline_job.get("data").getAsJsonObject();
	        String UserParameters = data.get("actionConfiguration").getAsJsonObject().get("configuration").getAsJsonObject().get("UserParameters").getAsString();
	        context.getLogger().log("stsai-poc-codepipeline-lambda UserParameters: " + UserParameters);
	        
	        /** get input params - source uri **/
	        JsonArray inputArtifacts = data.get("inputArtifacts").getAsJsonArray();
	        
	        /** process main process **/
	        for (JsonElement inputArtifact : inputArtifacts) {
	            JsonObject inputArtifactJsonObj = inputArtifact.getAsJsonObject();
	            JsonObject locationJsonObj = inputArtifactJsonObj.get("location").getAsJsonObject();
	            String typeJsonStr = locationJsonObj.get("type").getAsString();
	            context.getLogger().log("stsai-poc-codepipeline-lambda typeJsonStr: " + typeJsonStr);
	            if("S3".equalsIgnoreCase(typeJsonStr)) {
	            	JsonObject s3LocationJsonObj = locationJsonObj.get("s3Location").getAsJsonObject();
	            	String bucketNameJsonStr = s3LocationJsonObj.get("bucketName").getAsString();
	            	String objectKeyJsonStr = s3LocationJsonObj.get("objectKey").getAsString();
	            	context.getLogger().log("stsai-poc-codepipeline-lambda bucketNameJsonStr: " + bucketNameJsonStr);
	            	context.getLogger().log("stsai-poc-codepipeline-lambda objectKeyJsonStr: " + objectKeyJsonStr);
	
	            	
	            	final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            		String OutputBucketNameJsonStr = bucketNameJsonStr+"-output";
            		createS3Bucket(OutputBucketNameJsonStr);
            		context.getLogger().log("stsai-poc-codepipeline-lambda OutputBucketNameJsonStr: " + OutputBucketNameJsonStr);
            		context.getLogger().log("stsai-poc-codepipeline-lambda objectKeyJsonStr: " + objectKeyJsonStr);
            		s3.copyObject(bucketNameJsonStr, objectKeyJsonStr, OutputBucketNameJsonStr, objectKeyJsonStr);
	            }
	        }
	        
	        
	        /** report result back to CodePipeline **/
	        if (UserParameters.contains("fail_mode")) {
	        	throw new RuntimeException("fail_mode");
	        }else {
	        	/** notify a successful result back to CodePipeline **/
	        	PutJobSuccessResultResult putJobSuccessResultResult = codepipeline_client.putJobSuccessResult(new PutJobSuccessResultRequest().withJobId(jobId));
	        	context.getLogger().log("stsai-poc-codepipeline-lambda putJobSuccessResultResult: " + putJobSuccessResultResult);	        	
	        }
        
        }catch(Exception e) {
        	/** notify a failed result back to CodePipeline **/
        	e.printStackTrace();
        	context.getLogger().log("stsai-poc-codepipeline-lambda e.getMessage(): " + e.getMessage());
        	putJobFailureResult(gson, CodePipeline_job, jobId, context);
        }
        
        
        return "Hello from stsai-poc-codepipeline-lambda Lambda!";
    }

    /** notify a failed result back to CodePipeline **/
    private void putJobFailureResult(Gson gson, JsonObject CodePipeline_job, String jobId, Context context) {
    	FailureDetails failureDetails = new FailureDetails().withMessage(gson.toJson(CodePipeline_job)).withType(FailureType.JobFailed).withExternalExecutionId("externalExecutionId");
    	PutJobFailureResultResult putJobFailureResultResult = codepipeline_client.putJobFailureResult(new PutJobFailureResultRequest().withJobId(jobId).withFailureDetails(failureDetails));
    	context.getLogger().log("stsai-poc-codepipeline-lambda putJobFailureResultResult: " + putJobFailureResultResult);
    }
    
    private void createS3Bucket(String bucket_name) {
    	final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
    	Bucket b = null;
    	if (s3.doesBucketExistV2(bucket_name)) {
    	    System.out.format("Bucket %s already exists.\n", bucket_name);
    	} else {
    	    try {
    	        b = s3.createBucket(bucket_name);
    	    } catch (AmazonS3Exception e) {
    	        System.err.println(e.getErrorMessage());
    	    }
    	}
    }
}

// Notify AWS CodePipeline of a failed job
//var putJobFailure = function(message) {
//  var params = {
//      jobId: jobId,
//      failureDetails: {
//          message: JSON.stringify(message),
//          type: 'JobFailed',
//          externalExecutionId: context.invokeid
//      }
//  };
//  codepipeline.putJobFailureResult(params, function(err, data) {
//      context.fail(message);      
//  });
//};        


//// Notify AWS CodePipeline of a successful job
//var putJobSuccess = function(message) {
//  var params = {
//      jobId: jobId
//  };
//  codepipeline.putJobSuccessResult(params, function(err, data) {
//      if(err) {
//          context.fail(err);      
//      } else {
//          context.succeed(message);      
//      }
//  });
//};       