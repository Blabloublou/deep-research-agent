package com.core.deepsearch.util

import com.core.deepsearch.db.tables.AgentsTable
import com.core.deepsearch.db.tables.ReportsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/** 
 * Database initialization.
 */
object DatabaseFactory {
    
    /**
     * Initialize the database connection and create tables if needed.
     */
    fun init(dbPath: String = "deep_research.db") {
        val driverClassName = "org.sqlite.JDBC"
        val jdbcURL = "jdbc:sqlite:$dbPath"
                
        val database = Database.connect(jdbcURL, driverClassName)
        
        transaction(database) {
            SchemaUtils.create(AgentsTable, ReportsTable)
        }
    }
}

