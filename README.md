StackMobIAPReceiptVerification
==============================

A custom server code method for StackMob (http://www.stackmob.com) that will verify that iOS in-app purchase receipts are valid.

After installing StackMobIAPReceiptVerification as custom server code on your StackMob installation, a new method called `verify_receipt` will become available to REST based services. When invoked, this method accepts a parameter called `receipt`, which should be a base 64 encoded iOS App Store receipt. The `receipt` parameter *must* be passed using POST. If it is passed as a GET argument, the method will exit with an error.

If the receipt is successfully verified, the method returns the receipt verification status and the transaction data that was stored in the receipt. If there is a problem with verification, an error will be returned.

Configuring the Project
-----------------------
Before building the project, you will need to uncomment the appropriate definition of `validationServerURL` in `VerifyReceipt.java`. During testing, uncomment the line that defines the URL to the sandbox server. When building for production, you will want to uncomment the line that defines the production server instead.

Building the Project
--------------------
Since this project will be mostly used by iOS developers who use Objective C, it may not be obvious how to build this java project. Detailed instructions are beyond the scope of this document, but you will need to have a JDK (http://www.oracle.com/technetwork/java/javase/downloads/index.html) installed on your development machine, as well as the Maven project management tool (http://maven.apache.org). Once those are installed, you can build the JAR that will need to be uploaded to StackMob from the terminal:

```
$ mvn clean
$ mvn package
```

Note: The first time you build the code, Maven will need to download several packages that this code depends on, so make sure you have a network connection. Once the dependencies are downloaded, you should be able to build off-line.

For more detailed information on building StackMob custom server code, please see the StackMob documentation: https://developer.preview.stackmob.com/tutorials/customcode/Build-and-Upload-Custom-Code-Example

Uploading to StackMob
---------------------
For detailed information on how to upload the JAR that contains your custom server code, please see the StackMob documentation: https://developer.preview.stackmob.com/tutorials/customcode/Build-and-Upload-Custom-Code-Example

Invocation
----------
Since this method verifies the authenticity of in-app purchase receipts from the iOS App Store, it seems reasonable to assume that it will be mostly invoked from iOS. To make invocation from iOS easier, you will probably want to use the StackMob iOS SDK (available from http://developer.stackmob.com). To invoke the `verify_receipt` method using the version 0.5.x series of StackMob iOS SDKs, use code like this in your project's `SKPaymentTransactionObserver`:

```objective-c
- (void) completeTransaction: (SKPaymentTransaction *)transaction {

    if(transaction.transactionState == SKPaymentTransactionStatePurchased) {

        // NOTE: -[NSData base64EncodedString] is implemented as a category, and is included in the StackMob iOS SDK.
        NSString *encodedReceipt = [transaction.transactionReceipt base64EncodedString];

        if(encodedReceipt != nil) {

            NSDictionary *arguments = [NSDictionary dictionaryWithObject:encodedReceipt forKey:@"receipt"];
            [[StackMob stackmob] post:verify_receipt withArguments:arguments andCallback:^(BOOL success, id result) {

                // NOTE: If there's an error, or if something goes wrong, just silently exit. As long as
                // 'finishTransaction' isn't called, the receipt will be processed eventually.
                if(success) {

                    //
                    // The receipt was valid.
                    //

                    // Get the product identifier of the purchased item.
                    NSString *productIdentifier = nil;
                    if([result isKindOfClass:[NSDictionary class]]) {

                        NSDictionary *dictionary = result;
                        productIdentifier = [dictionary objectForKey:@"product_id"];

                        // For a list of all other information in the returned NSDictionary, please see the
                        // "Verifying Store Receipts" section of Apple's In-App Purchase Programming Guide.
                        // There is a dictionary entry for each purchase info key returned by Apple's verification
                        // server.
                    }

                    // Now that it is known which product was purchased (from the product identifier), do whatever is
                    // required to fulfill the purchase, then remove the transaction from the payment queue.

                    [[SKPaymentQueue defaultQueue] finishTransaction: transaction];
                }
                else {

                    // Something went wrong
                    if([result isKindOfClass:[NSDictionary class]]) {

                        // If something went wrong on the server, this 'else' clause should be called. The returned
                        // NSDictionary will contain error messages that can be used for debugging.
                        NSLog(@"Receipt verification failed:");
                        NSDictionary *dictionary = result;
                        for (NSString *key in [dictionary allKeys]) {

                            if([[dictionary objectForKey:key] isKindOfClass:[NSString class]]) {

                                NSString *value = [dictionary objectForKey:key];
                                NSLog(@"  %@: %@", key, value);
                            }
                        }
                    }
                    else {

                        NSLog(@"Subscribe failed with an unknown error.");
                    }
                }
            }];
        }
    }
}
```

As of this writing, StackMob has recently introduced a newer version 1.x series of their iOS SDK which has much different syntax. I have not yet upgraded to this new series, and have not investigated how custom server code can be called from it. If you get this working with StackMob's version 1.x series iOS SDK, please send me some sample code so I can include it in this document.

Support
-------
I don't offer support for this code. If you have questions, the best place to get help is probably StackMob support.

Bugs
----
Please help me improve this code! If you have improvements or bug fixes, please fork this project and issue a pull request. Thanks in advance for your help.