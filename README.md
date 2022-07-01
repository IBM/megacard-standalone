# MegaCardStandalone

 Online credit card processing with integrated fraud checking as part of a Benchmark. Written in Java

It requires a SQL Database (DB2) with the schema defined in `schema.ddl`



## Requests

The application has one HTTP resource  to accept incomming requests: 

```
POST /MegaCardStandalone/MegaCard/Svc/Transfer
```

The request body is json formatted:

```json
{
    "merchantAcc": ${merchantAcc},
    "merchantToken": "${merchantToken}",
    "cardNumber": "${cardNumber}",
    "cvv": "${cvv}",
    "expirationDate": "${expirationDate}",
    "amount": ${amount},
    "transactionUuid": "${__UUID}",
    "method": "${method}",
}
```



## Response

If the transaction was successful the string `successful`is returned.



## Cofiguration

The app can be configured through a set of environment variables:

### Fraud Checking Basic

| Variable          | Description                                                  |
| ----------------- | ------------------------------------------------------------ |
| CHECK_FRAUD       | (true/false) Enable / Disable Fraud checking and quering the required history from the Database |
| ModelAdapterClass | Class which implements the FraudChecking. Needs to implement the `com.ibm.lozperf.mb.ModelAdapter` interface. The App comes with a number of Adapters in `com.ibm.lozperf.mb.modeladapter`:<br />`TFServingAdapter`, `TFJavaAdapter`, `TFJavaBatchingAdapter`, `DLCSplitModelBatchingAdapter` |

### Tensorflow Serving

| Varaible | Description                                                  |
| -------- | ------------------------------------------------------------ |
| TF_URL   | Url of the tensorflow model server to be used by `TFServingAdapter` |

### Tensorflow Java

| Variable | Description                                             |
| -------- | ------------------------------------------------------- |
| TF_MODEL | Path of the saved model to be loaded by Java Tensorflow |



### Batching

Model Adapters with `Batching` in the name use `com.ibm.lozperf.mb.batching.BatchCollector` to implement batching of fraud checking requests. The BatchCollector can be configured through the following Variables

| Variable        | Description                                                  |
| --------------- | ------------------------------------------------------------ |
| PREDICT_THREADS | Number of Threads to concurrently process the batches        |
| TARGET_BS       | Size of the batches the BatchCollector tries to archive. If no Predict thread is availible, the batches can grow brigger. |
| BATCH_TIMEOUT   | Maximum time to wait for additinal requests, after the first request received before processing of the batch is triggered as if `TARGET_BS` is reached. |

### DLC Model Adapters

The DLC doesn't implement string operations at the moment, so for the preprocessing part the Java Tensorflow is used. The `TF_MODEL` variable needs to point to a saved model implementing the preprocessing, like the `lafalce-preprocess` model. The main model (LSTM) is implemented in the DLC part.

## JMeter

`MegaCard_date.jmx`can be used to drive requests against the application.