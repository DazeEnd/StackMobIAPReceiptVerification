 //  Created by Charles Perry on 11/14/12.
 //  Copyright (c) 2012 Leaf Hut Software.
 //
 //  Distributed under the MIT License:
 //
 //  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 //  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 //  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 //  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 //
 //  The above copyright notice and this permission notice shall be included in all copies or substantial portions
 //  of the Software.
 //
 //  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 //  TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 //  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 //  CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 //  IN THE SOFTWARE.

package com.leafhut.open_source;

import com.stackmob.core.ServiceNotActivatedException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.*;
import com.stackmob.sdkapi.http.HttpService;
import com.stackmob.sdkapi.http.exceptions.AccessDeniedException;
import com.stackmob.sdkapi.http.exceptions.TimeoutException;
import com.stackmob.sdkapi.http.request.PostRequest;
import com.stackmob.sdkapi.http.response.HttpResponse;

import org.json.JSONObject;
import org.json.JSONException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.*;

public class VerifyReceipt implements CustomCodeMethod {

  //
  // Define string constants
  //
  private static final String kReceiptParameter = "receipt";                    // Input parameter name
  private static final String kReceiptValidationKey =  "receipt-data";          // JSON key expected by Apple receipt validation server
  private static final String kValidationResponseStatusKey = "status";          // key in JSON response that indicates whether receipt is valid

  //
  // Define constants used for error reporting
  //
  private static final String kErrorCodeKey = "errorCode";                          // key for the numeric error code
  private static final String kErrorDescriptionKey = "errorDescription";            // key for the error description
  private static final String kExceptionNameKey = "exception";                      // key for the exception's class name
  private static final String kExceptionMessageKey = "exceptionMessage";            // key for the exception's message
  private static final String kFailureReasonKey = "failureReason";                  // key for the failure reason
  private static final String kFailingURLStringKey = "failingURLString";            // key for the URL that failed
  private static final String kRecoverySuggestionKey = "recoverySuggestion";        // key for suggestions on how to recover from the error

  private static final String kErrorCode402Description = "Payment Required";
  private static final String kErrorCode403Description = "Forbidden";
  private static final String kErrorCode500Description = "Internal Server Error";
  private static final String kErrorCode504Description = "Gateway Timeout";

  //
  // Define the URL of the server to use for receipt validation. *** Only uncomment one of them. ***
  //
  private static final String validationServerURL = "https://sandbox.itunes.apple.com/verifyReceipt";        // Sandbox Server
  // private static final String validationServerURL = "https://buy.itunes.apple.com/verifyReceipt";         // Production Server

  @Override
  public String getMethodName() {
    return "verify_receipt";
  }

  @Override
  public List<String> getParams() {

      // This custom code method accepts single parameter: a base 64 encoded App Store receipt sent via POST (not GET).
      // NOTE: If the receipt is not sent via POST, this method will generate an error and fail.
      return Arrays.asList(kReceiptParameter);
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {

      // Set up logger
      LoggerService logger = serviceProvider.getLoggerService(VerifyReceipt.class);


      String startMessage = "Processing receipt...";
      logger.info(startMessage);

      HttpService http;
      try {
          http = serviceProvider.getHttpService();
      } catch (ServiceNotActivatedException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          logger.error(exceptionLogMessage);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }

      if(http == null) {

          String failureReason = "HTTP Service is null.";
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }

      // Fetch parameters sent via POST
      String encodedReceipt = null;
      try {

          JSONObject jsonObj = new JSONObject(request.getBody());

          if (!jsonObj.isNull(kReceiptParameter)) {

              encodedReceipt = jsonObj.getString(kReceiptParameter);
          }

      } catch (JSONException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          String failureReason = "Invalid or missing parameter.";

          logger.error(exceptionLogMessage);
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }

      // Create JSON representation of receipt
      JSONObject validationBodyJSON = new JSONObject();
      try {
          validationBodyJSON.put(kReceiptValidationKey, encodedReceipt);
      } catch (JSONException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          String failureReason = "Could not create JSON for receipt validation server.";

          logger.error(exceptionLogMessage);
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }
      String validationBodyString = validationBodyJSON.toString();

      // create the HTTP request
      PostRequest req;
      try {
          req = new PostRequest(validationServerURL, validationBodyString);
      } catch (MalformedURLException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          String failureReason = "Invalid URL for receipt validation server.";

          logger.error(exceptionLogMessage);
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );
          errorMap.put(kFailingURLStringKey, validationServerURL);
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }

      // Send the request. This method call will not return until the server returns.
      // note that this method may throw AccessDeniedException if the URL is whitelisted or rate limited,
      // or TimeoutException if the server took too long to return
      HttpResponse response;
      try {
          response = http.post(req);
      } catch (AccessDeniedException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          String failureReason = "HTTP request refused by StackMob custom code environment.";
          String suggestionMessage = "Check rate limiting, whitelisting, and blacklisting in the StackMob custom code environment.";
          logger.error(exceptionLogMessage);
          logger.error(failureReason);
          logger.debug(suggestionMessage);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_FORBIDDEN);
          errorMap.put(kErrorDescriptionKey, kErrorCode403Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );
          errorMap.put(kFailureReasonKey, failureReason);
          errorMap.put(kRecoverySuggestionKey, suggestionMessage);

          return new ResponseToProcess(HttpURLConnection.HTTP_FORBIDDEN, errorMap);

      } catch (TimeoutException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          String failureReason = "HTTP request to receipt validation server timed out.";

          logger.error(exceptionLogMessage);
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
          errorMap.put(kErrorDescriptionKey, kErrorCode504Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, errorMap);
      }

      if(response == null) {

          String failureReason = "Response from receipt validation server is null.";
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }


      // Parse the response from the server
      JSONObject serverResponseJSON;

      try {
          serverResponseJSON = new JSONObject(response.getBody());
      } catch (JSONException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          String failureReason = "Could not parse JSON response from receipt validation server.";

          logger.error(exceptionLogMessage);
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }


      int validationStatus = -1;
      try {
          if (!serverResponseJSON.isNull(kValidationResponseStatusKey)) {

              validationStatus = serverResponseJSON.getInt(kValidationResponseStatusKey);
          }
      } catch (JSONException e) {

          String exceptionLogMessage = e.getClass().getName() + ": " + e.getMessage();
          String failureReason = "Missing or invalid status code from receipt validation server.";

          logger.error(exceptionLogMessage);
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_INTERNAL_ERROR);
          errorMap.put(kErrorDescriptionKey, kErrorCode500Description);
          errorMap.put(kExceptionNameKey, e.getClass().getName() );
          errorMap.put(kExceptionMessageKey, e.getMessage() );
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMap);
      }

      // Take action based on receipt validation
      if(validationStatus == 0) {

          //
          // Receipt is valid
          //
          // This is where you could take any server-side actions that were required to fulfill the purchase.
          // See the StackMob custom code documentation for more details:
          // https://developer.preview.stackmob.com/tutorials/custom%20code
          //

      }
      else {

          //
          // Receipt is invalid
          //

          String failureReason = "Invalid receipt.";
          logger.error(failureReason);

          Map<String, Object> errorMap = new HashMap<String, Object>();
          errorMap.put(kErrorCodeKey, HttpURLConnection.HTTP_PAYMENT_REQUIRED);
          errorMap.put(kErrorDescriptionKey, kErrorCode402Description);
          errorMap.put(kFailureReasonKey, failureReason);

          return new ResponseToProcess(HttpURLConnection.HTTP_PAYMENT_REQUIRED, errorMap);
      }

      // Send human-readable server response to calling client
      // Note: The parsing below is brittle (depends on there never being more than two layers of JSON).
      //       This probably should be generalized using recursion.
      Map<String, Object> returnMap = new HashMap<String, Object>();

      Iterator<?> keys = serverResponseJSON.keys();
      while( keys.hasNext() ){
          String key = (String)keys.next();
          try {
              if(serverResponseJSON.get(key) instanceof JSONObject) {

                  JSONObject nestedJSON = (JSONObject)serverResponseJSON.get(key);

                  Iterator<?> nestedKeys = nestedJSON.keys();
                  while( nestedKeys.hasNext() ) {

                      String nestedKey = (String)nestedKeys.next();
                      Object nestedValue = nestedJSON.get(nestedKey);
                      returnMap.put(nestedKey, nestedValue.toString());

                  }
              }
              else {

                  Object value = serverResponseJSON.get(key);
                  returnMap.put(key, value.toString());
              }

          } catch (JSONException e) {
              logger.debug(e.getMessage());
              e.printStackTrace();
          }
      }

      String finishMessage = "Receipt is valid.";
      logger.info(finishMessage);
      return new ResponseToProcess(HttpURLConnection.HTTP_OK, returnMap);
  }

}
