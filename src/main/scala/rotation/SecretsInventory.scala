package rotation

/** Reads the credential inventory out of the secrets manager.
  *
  * The listing endpoint is paginated: it answers with at most `pageSize`
  * entries plus a `nextPageToken` that addresses the following page.
  */
class SecretsInventory(gateway: SecretsGateway, pageSize: Int) {

  def listSecrets(): Seq[Secret] = {
    val page = JsonCodec.parseSecretsPage(gateway.getJson("/v1/secrets?limit=" + pageSize))
    page.items
  }
}
