package common.executor.model

import play.api.libs.concurrent.CustomExecutionContext

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * utility functions that acts as wrappers for scala.util methods
  * Base class with common methods for repositories
  */
trait BaseDomainRepository {
  /**
    * enable futures to be executed in parallel
    */
  protected def lift[T](futures: Seq[Future[T]])(implicit ec: CustomExecutionContext): Seq[Future[Try[T]]] =
    futures.map(_.map { Success(_) }.recover { case t => Failure(t) })

   /**
    *
     wait for all to complete either with SUCCESS or FAILURE
    */
  protected def waitAll[T](futures: Seq[Future[T]])(implicit ec: CustomExecutionContext): Future[Seq[Try[T]]] =
    Future.sequence(lift(futures))
}
