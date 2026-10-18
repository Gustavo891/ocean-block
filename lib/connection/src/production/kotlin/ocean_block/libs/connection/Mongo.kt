package ocean_block.libs.connection

import com.redescreen.sdk.connection.createMongoDatabase

val Mongo by lazy { createMongoDatabase("ocean-block") }
