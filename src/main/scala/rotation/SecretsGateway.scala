package rotation

/** HTTP transport for the secrets-manager REST API. */
trait SecretsGateway {

  /** Issues an authenticated GET against `path` and returns the raw body. */
  def getJson(path: String): String
}
