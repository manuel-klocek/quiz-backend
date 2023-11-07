package school.it.helper

import com.mongodb.client.MongoDatabase
import org.json.JSONObject
import org.litote.kmongo.KMongo

object GetEnvVars {
    val getMongoDbAsDb: MongoDatabase = try {
        val vcapServices = System.getenv("VCAP_SERVICES")
        val jsonObject = JSONObject(vcapServices)
        val mongodbCredentials = jsonObject.getJSONArray("stackit-mongodb").getJSONObject(0).getJSONObject("credentials")
        val connectionString = mongodbCredentials.getString("uri")
        val client = KMongo.createClient(connectionString)
        client.getDatabase("defaultDB")
    } catch (ex: Exception) {
        KMongo.createClient().getDatabase("Quiz")
    }
}