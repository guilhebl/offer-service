package product.marketplace.amazon

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.{Calendar, TimeZone}

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Singleton}
import org.apache.commons.codec.binary.Base64

import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashMap

trait AmazonRequestHelper {
  def sign(endpoint : String, awsAccessKeyId : String, awsSecretKey : String, params: HashMap[String, String]): String
}

@Singleton
class AmazonRequestHelperImpl @Inject() extends AmazonRequestHelper {

  /**
    * All strings are handled as UTF-8
    */
  private val UTF8_CHARSET = "UTF-8"

  /**
    * The HMAC algorithm required by Amazon
    */
  private val HMAC_SHA256_ALGORITHM = "HmacSHA256"

  /**
    * This is the URI for the service, don't change unless you really know
    * what you're doing.
    */
  private val REQUEST_URI = "/onca/xml"

  /**
    * The sample uses HTTP GET to fetch the response. If you changed the sample
    * to use HTTP POST instead, change the value below to POST.
    */
  private val REQUEST_METHOD = "GET"

  override def sign(endpoint : String, awsAccessKeyId : String, awsSecretKey : String, params: HashMap[String, String]): String = {

    // 1. init crypto key
    val secretyKeyBytes : Array[Byte] = awsSecretKey.getBytes(UTF8_CHARSET)
    val secretKeySpec = new SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM)
    val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
    mac.init(secretKeySpec)

    // 2. sign request
    // Let's add the AWSAccessKeyId and Timestamp parameters to the request.
    params("AWSAccessKeyId") = awsAccessKeyId
    params("Timestamp") = timestamp

    // The parameters need to be processed in lexicographical order, so we'll
    // use a TreeMap implementation for that.
    val sortedParamMap = TreeMap(params.toArray:_*)

    // get the canonical form the query string
    val canonicalQS = canonicalize(sortedParamMap)

    // create the string upon which the signature is calculated
    val toSign = REQUEST_METHOD + "\n" + endpoint + "\n" + REQUEST_URI + "\n" + canonicalQS

    // get the signature
    val hmacStr = hmac(mac, toSign)
    val sig = percentEncodeRfc3986(hmacStr)

    // construct the URL
    val url = "http://" + endpoint + REQUEST_URI + "?" + canonicalQS + "&Signature=" + sig.replaceAll("%0D%0A", "")
    url
  }

  /**
    * Compute the HMAC.
    *
    * @param stringToSign String to compute the HMAC over.
    * @return base64-encoded hmac value.
    */
  private def hmac(mac: Mac, stringToSign: String): String = {
    try {
      val data = stringToSign.getBytes(UTF8_CHARSET)
      val rawHmac = mac.doFinal(data)
      val encoder = new Base64()
      val signature = new String(encoder.encode(rawHmac))
      signature
    } catch {
      case e: UnsupportedEncodingException =>
        throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e)
      case _ : Throwable => ""
    }
  }

  private def canonicalize(sortedParamMap : TreeMap[String, String]) : String = {
    if (sortedParamMap.isEmpty) {
      return ""
    }

    val buffer = new StringBuilder()

    sortedParamMap.foreach(x => {
      buffer ++= percentEncodeRfc3986(x._1)
      buffer ++= "="
      buffer ++= percentEncodeRfc3986(x._2)
      buffer ++= "&"
    })

    // remove last &
    buffer.dropRight(1).toString
  }

  /**
    * Percent-encode values according the RFC 3986. The built-in Java
    * URLEncoder does not encode according to the RFC, so we make the
    * extra replacements.
    *
    * @param s decoded string
    * @return encoded string per RFC 3986
    */
  private def percentEncodeRfc3986(s: String) = {
    try {
      URLEncoder.encode(s, UTF8_CHARSET).replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
    } catch {
      case _ : Throwable => s
    }
  }

  /**
    * Generate a ISO-8601 format timestamp as required by Amazon.
    *
    * @return ISO-8601 format timestamp.
    */
  private def timestamp = {
    val cal = Calendar.getInstance
    val dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dfm.setTimeZone(TimeZone.getTimeZone("GMT"))
    val timestamp = dfm.format(cal.getTime)
    timestamp
  }
}
