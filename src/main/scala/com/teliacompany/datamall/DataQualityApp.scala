// spark-shell -master yarn --conf spark.ui.port=4044
package com.teliacompany.datamall

import com.teliacompany.datamall._
import org.apache.spark.{SparkConf, SparkContext}

import com.amazon.deequ.{VerificationSuite, VerificationResult}
import com.amazon.deequ.VerificationResult.checkResultsAsDataFrame
import com.amazon.deequ.checks.{Check, CheckLevel}

object DataQualityApp {
    def main(args: Array[String]) = {
        val conf = new SparkConf().setAppName("Hoad HDFS").setMaster("yarn-client")
        val sc = new SparkContext(conf)
        val sqlc = new org.apache.spark.sql.SQLContext(sc) 
        // DataFrame and toDF support
        import org.apache.spark.sql.DataFrame
        val session = sqlc.sparkSession
        import session.sqlContext.implicits._
        
        val dataset = sqlc.read.parquet(args(0))

        val result: VerificationResult = { 
        VerificationSuite()
            .onData(dataset)
            .addCheck(
                Check(CheckLevel.Error, "Data Validation Check")
                    .hasCompleteness("customer_id", _ >= 0.90) // At least 90% rows have customer_id defined
                    .isUnique("review_id")
                    .isNonNegative("total_votes") 
                    .hasStandardDeviation("helpful_votes", _ < 3.0)
                    .hasEntropy("helpful_votes", _ < 2.0)
                    .hasCorrelation("helpful_votes", "total_votes", _ >= 0.8)
                    )
        .run()
        }
        val output = checkResultsAsDataFrame(session, result)
        println("+++ Results")
        output.show()
        output.write.parquet(args(1), classOf[org.apache.hadoop.io.compress.SnappyCodec])

        sc.stop()
    }

    def load(path: String, sc: SparkContext) = {
        val ls = HdfsUtils.getAllFiles(path, sc)
        println("+++ Files list: ")
        println(ls)
    }
}

