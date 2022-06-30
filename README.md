# MegaCardStandalone

 Online credit card processing with integrated fraud checking as part of a Benchmark. Written in Java

It requires a SQL Database (DB2) with the schema defined in `schema.ddl`



## Requests

The application has one HTTP resource  to accept incomming requests: 

```
POST /MegaCardStandalone/MegaCard/Svc/Transfer
```

The request body is json formatted:

```
{
    "merchantAcc": ${merchantAcc},
    "merchantToken": "${merchantToken}",
    "cardNumber": "${cardNumber}",
    "cvv": "${cvv}",
    "expirationDate": "${expirationDate}",
    "amount": ${amount},
    "transactionUuid": "${__UUID}",
    "method": "${method}",
    "timestamp": ${timestamp}
}
```

timestamp is optional and only used to make runs reproducable



## Response

If the transaction was successful the string `successful`is returned.



## Cofiguration

The app can be configured through a set of environment variables:

### Fraud Checking Basic

| Variable          | Description                                                  |
| ----------------- | ------------------------------------------------------------ |
| CHECK_FRAUD       | (true/false) Enable / Disable Fraud checking and quering the required history from the Database |
| ModelAdapterClass | Class which implements the FraudChecking. Needs to implement the `com.ibm.lozperf.mb.ModelAdapter` interface. The App comes with a number of Adapters in `com.ibm.lozperf.mb.modeladapter`:<br />`TFServingAdapter`, `TFServingBatchingAdapter`, `DLCModelBatchingMTPPAdapter` |

### Tensorflow Serving

| Varaible | Description                                                  |
| -------- | ------------------------------------------------------------ |
| TF_URL   | Url of the tensorflow model server to be used by `TFServingAdapter` |

### Batching

Model Adapters with `Batching` in the name use `com.ibm.lozperf.mb.batching.BatchCollector` to implement batching of fraud checking requests. The BatchCollector can be configured through the following Variables

| Variable        | Description                                                  |
| --------------- | ------------------------------------------------------------ |
| PREDICT_THREADS | Number of Threads to concurrently process the batches        |
| TARGET_BS       | Size of the batches the BatchCollector tries to archive. If no Predict thread is availible, the batches can grow brigger. |
| BATCH_TIMEOUT   | Maximum time to wait for additinal requests, after the first request received before processing of the batch is triggered as if `TARGET_BS` is reached. |

### DLC Model Adapters

The DLC doesn't implement string operations at the moment, so the strings first need to be mapped to an Integer Index. These mappings are read from CSV files using the `|`as the seperator. For example:

```
[UNK]|0
Money Transfer Company 1|1
Convenience Store Chain 1|2
Auto Toll Provider 1|3
Supermarket Chain 2|4
Wholesale Club 1|5
Convenience Store Chain 3|6
Gas Station Company 1|7
Supermarket Chain 3|8
Bookstore Company 1|9
```

The name of the mapping files are: `MCC.csv` `MerchantCity.csv`  `MerchantName.csv`  `MerchantState.csv`  `Zip.csv`

The directory of the mapping files is specified through the `STRING_MAP_DIR` variable.



## JMeter

`MegaCard_date.jmx`can be used to drive requests against the application.