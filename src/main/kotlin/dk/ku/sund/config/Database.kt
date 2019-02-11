package dk.ku.sund.config

import com.cars.framework.secrets.DockerSecrets

val databaseConfig =
    try {
        DockerSecrets.loadFromFile("database.properties")
    } catch (error: Throwable) {
        mapOf(
            "hostname" to "localhost",
            "username" to "root",
            "password" to "",
            "authSource" to "admin",
            "db" to "activity"
        )
    }
