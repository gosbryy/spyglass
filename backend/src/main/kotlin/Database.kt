package gosbry

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {

    val dbUrl = environment.config.property("spyglass.db.url").getString()
    val dbUser = environment.config.property("spyglass.db.user").getString()
    val dbPassword = environment.config.property("spyglass.db.password").getString()

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        this.password = dbPassword
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(hikariConfig)

    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()

    Database.connect(dataSource)
    monitor.subscribe(ApplicationStopped) { _ ->
        dataSource.close()
    }
}
