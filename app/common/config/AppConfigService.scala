package common.config

import java.util.Base64

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject._
import play.api.Logger

trait AppConfigService {
  def properties: Map[String,String]
  def buildImgUrl(url:Option[String]): String
  def buildImgUrlExternal(url:Option[String], proxyRequired:Boolean): String
}

@Singleton
class AppConfigServiceImpl @Inject() extends AppConfigService {
  
  private val logger = Logger(getClass)  
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
  
  def encryptAES(s:String): String = {
   try {
       val key = properties("privateKeyAES")
       val aesKey = new SecretKeySpec(key.getBytes(), "AES")
       val cipher = Cipher.getInstance("AES")
  
       cipher.init(Cipher.ENCRYPT_MODE, aesKey)
       val encrypted = cipher.doFinal(s.getBytes());
       val r = Base64.getEncoder.encode(encrypted)
       r.toString
     } catch {
       case e: Exception => 
         logger.trace("error while generating SHA-256 hash" + e.getMessage)
         ""       
       case _: Exception => 
         logger.trace("error while generating SHA-256 hash")
         ""
     }
	}

  def decryptAES(s:String): String = {        
   try { 
       val key = properties("privateKeyAES")
       val aesKey = new SecretKeySpec(key.getBytes(), "AES")
       val cipher = Cipher.getInstance("AES")
       
       cipher.init(Cipher.DECRYPT_MODE, aesKey);
       val decrypted = new String(cipher.doFinal(Base64.getDecoder.decode(s)));
       decrypted     
     } catch {
       case e: Exception => 
         logger.trace("error while generating SHA-256 hash" + e.getMessage)
         ""       
       case _: Exception => 
         logger.trace("error while generating SHA-256 hash")
         ""
     }
	}

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
   */
  def buildImgUrlExternal(url:Option[String], proxyRequired:Boolean): String = {    
    url match {      
      case Some(v) => if (proxyRequired) {
        val hash = encryptAES(v)        
        val localURLSecure = proxyHostname + "?hash=" + hash;
        localURLSecure
      } else {
        changeToHttpsUrl(v) // case when provider has an https available service just switch to that if not already https
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