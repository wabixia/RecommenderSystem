package com.atguigu.staticstics

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession


/**
 * Movie数据集， 数据集字段通过^分割
 *
 * 151                                电影ID
 * Rob Roy (1995)                     电影的名称
 * In the highland .....              电影的描述
 * 139 minutes                        电影的时长
 * Augest 26,1997                     电影的发行日期
 * 1995                               电影的拍摄日期
 * English                            电影的类型
 * Action|Drama|ROmance|War           电影的类型
 * Liam Neeson|Jessica Lange.....     电影的演员
 * Michael Caton-Jones*               电影的导演
 * tag1|tag2|tag3                     电影的标签
 */
case class Movie(val mid: Int, val name: String, val descri: String, val timelong: String, val issue: String,
                 val shoot: String, val language: String, val genres: String, val actors: String, val directors: String)

/**
 * Rating数据集， 用户对于电影的评分数据
 * 1，         用户的ID
 * 31,         电影的ID
 * 2.5,        用户对于电影的评分
 * 1260759144  用户对于电影评分的时间
 */

case class Rating(val uid: Int, val mid: Int, val score: Double, val timestamp: Int)

/**
 * MongoDB的连接配置
 *
 * @param uri MongoDB的连接
 * @param db  MongoDB要操作的数据库
 */
case class MongoConfig(val uri: String, val db: String)


object Statistics {

  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_MOVIE_COLLECTION = "Movie"

  //统计的表的名称
  val RATE_MORE_MOVIES = "RateMoreMovies"
  val RATE_MORE_RECENTLY_MOVIES = "RateMoreRecentlyMovies"
  val AVERAGE_MOVIES = "AverageMovies"
  val GENRES_TOP_MOVIES = "GenresTopMovies"

  //入口方法
  def main(args: Array[String]): Unit = {

    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://MovieR:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建SparkConf配置
    val sparkConf = new SparkConf().setAppName("StattisticsRecommender").setMaster(config("spark.cores"))

    //创建SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //加入隐式转换
    import spark.implicits._
    //数据加载进来
    val ratingDF = spark
      .read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Rating]
      .toDF()

    val movieDF = spark
      .read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Movie]
      .toDF()

    //统计所有历史数据中每个电影的评分数
//数据结构 -> mid, count

    val rateMoreMoviesDF = spark.sql("select mid, count(mid) as count from ratings group by mid")

    rateMoreMoviesDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection", RATE_MORE_MOVIES)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
    //统计以月为单位每个电影的评分数
    //数据结构 -> mid,count,time

    //创建一个日期格式化工具
val simpleDateFormat = new SimpleDateFormat("yyyyMM")

    //注册一个UDF函数， 用于将timestamp转换成年月格式
    spark.udf.register("changeDate",(x:Int) => simpleDateFormat.format(new Date(x*1000L)).toInt)

    //统计每种电影类型中评分最高的10个电影

    //关闭spark
  }

}
