package certguard

/** Reads the TLS-serving hosts out of the service registry.
  *
  * The registry pages at 250 entries and hands back a `next` token; the last
  * page leaves it out.
  */
final class ServiceRegistryClient(baseUrl: String, http: HttpJson, pageSize: Int = 250)
    extends HostRegistry {

  private val root = baseUrl.stripSuffix("/")

  override def managedHosts(): Seq[ManagedHost] = {
    def page(token: Option[String], acc: Vector[ManagedHost]): Vector[ManagedHost] = {
      val uri = s"$root/v2/hosts?tls=true&limit=$pageSize" + token.fold("")(t => s"&next=$t")
      val body = http.get(uri)
      val hosts = body("hosts").arr.toVector.map(ServiceRegistryClient.host)
      body.obj.get("next").map(_.str).filter(_.nonEmpty) match {
        case Some(next) if !token.contains(next) => page(Some(next), acc ++ hosts)
        case _                                   => acc ++ hosts
      }
    }
    page(None, Vector.empty)
  }
}

object ServiceRegistryClient {

  private[certguard] def host(node: ujson.Value): ManagedHost =
    ManagedHost(
      name = node("hostname").str,
      owner = node.obj.get("owner").map(_.str).filter(_.nonEmpty).getOrElse("unassigned"),
      environment = node.obj.get("environment").map(_.str).getOrElse("unknown")
    )
}
