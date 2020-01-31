package testtask

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import mu.KotlinLogging
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import kotlin.concurrent.thread

data class ProcessedMaxId(
    var id: Int
)

private val logger = KotlinLogging.logger {}

class App : CliktCommand(name = "samples-test") {

    private val n: Int by option("-n", "--insert_count",
        help = "Batch samples insert count").int().default(100)

    private val maxEmptyAttempts: Int by option("--max_empty_attempts",
        help = "Empty selects count before worker finished").int().default(7)

    private val workersCount: Int by option("--workers_count",
        help = "Sample processing workers count").int().default(7)

    override fun run() {
        // insert test rows to samples table
        thread(start = true) { insertSamples(n) }

        // start workers for samples processing
        val processedMaxId = ProcessedMaxId(-1) // shared value of max processed sample id
        val workers = mutableListOf<Thread>()

        (1..workersCount).forEach { _ ->
            val thread = Thread { processSamples(processedMaxId, maxEmptyAttempts) }.apply { start() }
            workers.add(thread)
        }

        // wait until all workers finished
        workers.forEach { it.join() }

        logStats()
        clearTables()
    }
}

fun main(args: Array<String>) = App().main(args)

fun insertSamples(n: Int = 100) {
    val connection: Connection = DataSource.getConnection()
    val ps = connection.prepareStatement(INSERT_SAMPLES_SQL)
    for (i in 1..n) {
        for (j in 1..10) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()))
            ps.setInt(2, (0..(10 * n)).random())
            ps.addBatch()
        }
        ps.executeBatch()
        connection.commit()
    }
    connection.close()
}

fun processSamples(processedMaxId: ProcessedMaxId, maxEmptyAttempts: Int = 7) {
    var emptyAttempts = 0

    val threadId = Thread.currentThread().id
    val samplesToProcess = mutableListOf<Int>()

    while (emptyAttempts < maxEmptyAttempts) {
        val connection: Connection = DataSource.getConnection()
        val selectPs = connection.prepareStatement(SELECT_SAMPLES_FOR_PROCESS_SQL)
        val insertPs = connection.prepareStatement(PROCESSED_SAMPLES_INSERT_SQL)

        synchronized(processedMaxId) {
            selectPs.setInt(1, processedMaxId.id)
            val rs = selectPs.executeQuery()

            emptyAttempts = if (rs.isBeforeFirst) 0 else emptyAttempts + 1

            while (rs.next()) { samplesToProcess.add(rs.getInt("id")) }

            if (emptyAttempts == 0) {
                processedMaxId.id = samplesToProcess.last()
            }
        }

        if (samplesToProcess.size >= 10 || emptyAttempts == maxEmptyAttempts) {
            samplesToProcess.forEach {
                insertPs.setTimestamp(1, Timestamp.from(Instant.now()))
                insertPs.setInt(2, it)
                insertPs.setInt(3, threadId.toInt())
                insertPs.addBatch()
            }
            insertPs.executeBatch()
            connection.commit()
            samplesToProcess.clear()
        }
        connection.close()
        Thread.sleep(10)
    }
}

fun clearTables() {
    with(DataSource.getConnection()) {
        createStatement().execute(DELETE_PROCESSED_SAMPLES_SQL)
        createStatement().execute(DELETE_SAMPLES_SQL)
        commit()
        close()
    }
}

fun logStats() {
    with(DataSource.getConnection()) {
        val rs = createStatement().executeQuery(STATS_PROCESSED_SAMPLES_SQL)
        while (rs.next()) {
            val threadId = rs.getInt("thread_id")
            val sampleCount = rs.getInt("processed_count")
            val maxsample = rs.getInt("max_sample")

            logger.info{ "THREAD $threadId: PROCESSED $sampleCount SAMPLES WITH MAX SAMPLE = $maxsample" }
        }
        close()
    }
}