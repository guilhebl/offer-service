package common.config

import common.encrypt.EncryptUtil._
import javax.inject._

trait AppConfigService {
  def properties: Map[String,String]
  def buildImgUrl(url:Option[String]): String
  def buildImgUrlExternal(url:Option[String], proxyRequired:Boolean): String
}

@Singleton
class AppConfigServiceImpl @Inject() extends AppConfigService {
  
  private val propertiesMap: Map[String, String] = readProperties()
  
  private def readProperties(): Map[String, String] = {
    import java.io.FileInputStream
    import java.util.Properties

    import scala.collection.JavaConverters._

    val prop = new Properties()
    prop.load(new FileInputStream(System.getProperty("user.dir") + "/conf/app/app-config.properties"))
    prop.asScala.toMap
  }
  
  def properties: Map[String,String] = propertiesMap  
  def hostname:String = properties("protocol") + properties("host") + "/"  
  def imgFolderUri:String = hostname + "assets/images/"
  def proxyHostname:String = properties("protocol") + properties("proxyHost") + "/"	

  /**
   * builds img from local assets folder
   */
  def buildImgUrl(url:Option[String]): String = {    
    url match {      
      case Some(v) => imgFolderUri + v;
      case _ => imgFolderUri + "image-placeholder.png"
    }
  }

  /**
   * builds an img source from an external server should proxy if http is used to avoid security warns (MIXED MODE)
   * in case when provider has an https available service just switch to that if not already https
   */
  def buildImgUrlExternal(url:Option[String], proxyRequired:Boolean): String = {
    url match {      
      case Some(urlString) => if (proxyRequired) {
        val enc = encryptAES(properties("privateKeyAES"), urlString)
        val localURLSecure = s"proxyHostname?hash=$enc"
        localURLSecure
      } else {
        changeToHttpsUrl(urlString)
      }
      case _ => imgFolderUri + "image-placeholder.png"
    }
  }
  
	/**
	 * replaces a non-SSL http URL with https url, otherwise just returns the given string if already under https format
	 * @param url
	 */
  def changeToHttpsUrl(url:String): String = {
		if (url.indexOf("http://") != -1) return url.replace("http://", "https://")
		url    
  }

}