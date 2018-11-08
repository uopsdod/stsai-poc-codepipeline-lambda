package com.amazonaws.lambda.demo;

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
import com.google.gson.Gson;
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
        
        /** get input params **/
        JsonObject data = CodePipeline_job.get("data").getAsJsonObject();
        String UserParameters = data.get("actionConfiguration").getAsJsonObject().get("configuration").getAsJsonObject().get("UserParameters").getAsString();
        context.getLogger().log("stsai-poc-codepipeline-lambda UserParameters: " + UserParameters);
        
        /** process main process **/
        
        
        /** get job id **/
        String jobId = CodePipeline_job.get("id").getAsString();
        context.getLogger().log("stsai-poc-codepipeline-lambda jobId: " + jobId);
        
        /** report result back to CodePipeline **/
        if (UserParameters.contains("fail_mode")) {
        	/** notify a failed result back to CodePipeline **/
        	FailureDetails failureDetails = new FailureDetails().withMessage(gson.toJson(CodePipeline_job)).withType(FailureType.JobFailed).withExternalExecutionId("externalExecutionId");
        	PutJobFailureResultResult putJobFailureResultResult = codepipeline_client.putJobFailureResult(new PutJobFailureResultRequest().withJobId(jobId).withFailureDetails(failureDetails));
        	context.getLogger().log("stsai-poc-codepipeline-lambda putJobFailureResultResult: " + putJobFailureResultResult);
        }else {
            /** notify a successful result back to CodePipeline **/
            PutJobSuccessResultResult putJobSuccessResultResult = codepipeline_client.putJobSuccessResult(new PutJobSuccessResultRequest().withJobId(jobId));
            context.getLogger().log("stsai-poc-codepipeline-lambda putJobSuccessResultResult: " + putJobSuccessResultResult);       	
        }
        
        return "Hello from stsai-poc-codepipeline-lambda Lambda!";
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