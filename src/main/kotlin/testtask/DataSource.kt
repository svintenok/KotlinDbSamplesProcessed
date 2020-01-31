package testtask

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.util.*

object DataSource {
    private val dataSource: HikariDataSource by lazy { hikariDs() }
    private val props = Properties()

    init {
        props.load(DataSource.javaClass.getResource("/application.properties").openStream())
    }

    fun getConnection(): Connection = dataSource.connection

    private fun hikariDs(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = props.getProperty("database.driver")
        config.jdbcUrl = props.getProperty("database.host")
        config.username = props.getProperty("database.user")
        config.password = props.getProperty("database.password")
        config.maximumPoolSize = 4
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_READ_COMMITTED"
        config.validate()

        return HikariDataSource(config)
    }
}