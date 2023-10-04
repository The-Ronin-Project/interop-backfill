package com.projectronin.interop.backfill.server

import org.ktorm.database.Database
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@SpringBootApplication
@ComponentScan("com.projectronin.interop.backfill.server")
class BackfillServer {
    @Bean
    fun database(dataSource: DataSource): Database = Database.connectWithSpringSupport(dataSource)

    @Bean
    fun txManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)
}

fun main(args: Array<String>) {
    runApplication<BackfillServer>(*args)
}
