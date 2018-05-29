package common.executor

import akka.actor.ActorSystem
import javax.inject.Inject
import play.api.libs.concurrent.CustomExecutionContext

class WorkerDispatcherContext @Inject()(actorSystem: ActorSystem) extends CustomExecutionContext(actorSystem, "worker.dispatcher")
