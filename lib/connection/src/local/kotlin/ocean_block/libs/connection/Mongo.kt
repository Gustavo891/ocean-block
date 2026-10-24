package ocean_block.libs.connection

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoDatabase
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.Conventions
import org.bson.codecs.pojo.PojoCodecProvider

val Mongo by lazy { createMongoDatabase() }

fun createMongoDatabase(): MongoDatabase {
    val connectionString = System.getenv("OCEANBLOCK_MONGO_URI")?.takeIf { it.isNotBlank() } ?: "mongodb://localhost:27017/"

    val pojoCodecProvider = PojoCodecProvider.builder()
        .automatic(true)
        .conventions(
            listOf(
                Conventions.ANNOTATION_CONVENTION,
                Conventions.CLASS_AND_PROPERTY_CONVENTION,
                Conventions.SET_PRIVATE_FIELDS_CONVENTION
            )
        )
        .build()

    val settings = MongoClientSettings.builder()
        .codecRegistry(
            CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(pojoCodecProvider)
            )
        )
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .applyConnectionString(ConnectionString(connectionString))
        .build()

    return MongoClient.create(settings).getDatabase("ocean-block")
}
