package one_block.libs.connection

import com.redescreen.sdk.connection.createMongoDatabase

val Mongo by lazy { createMongoDatabase("one-block") }
