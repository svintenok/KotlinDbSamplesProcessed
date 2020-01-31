# KotlinDbSamplesProcessed

### BUILD

To build jar:

`./gradlew shadowJar`

### RUN

Get help:

`java -jar kotlinDbSamplesProcessed-all.jar --help`

Run script:

`java -jar kotlinDbSamplesProcessed-all.jar`

### Options

| Option |  Description  | Default value |
| --- |  ---  | --- |
| -n, ----insert_count INT | Batch samples insert count | 100 |
| --workers_count INT | Empty selects count before worker finished | 7 |
| --max_empty_attempts INT | Sample processing workers count | 7 |
