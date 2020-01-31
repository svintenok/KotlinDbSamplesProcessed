package testtask

const val SAMPLES_TABLE = "samples"
const val PROCESSED_SAMPLES_TABLE = "processed_samples"

const val INSERT_SAMPLES_SQL = "INSERT INTO $SAMPLES_TABLE (creation_timestamp, sample) VALUES (?, ?)"
const val DELETE_SAMPLES_SQL = "DELETE FROM $SAMPLES_TABLE"
const val DELETE_PROCESSED_SAMPLES_SQL = "DELETE FROM $PROCESSED_SAMPLES_TABLE"
const val SELECT_SAMPLES_FOR_PROCESS_SQL = "SELECT * FROM $SAMPLES_TABLE WHERE id > ?"
const val PROCESSED_SAMPLES_INSERT_SQL = "INSERT INTO $PROCESSED_SAMPLES_TABLE (creation_timestamp, sample_id, thread_id) VALUES (?, ?, ?)"
const val STATS_PROCESSED_SAMPLES_SQL = "SELECT thread_id, COUNT(sample_id) AS processed_count, MAX(sample) as max_sample \n" +
        "FROM processed_samples\n" +
        "JOIN samples ON processed_samples.sample_id = samples.id\n" +
        "GROUP BY processed_samples.thread_id"