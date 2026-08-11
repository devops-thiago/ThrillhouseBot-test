using RenewalService.Models;

namespace RenewalService.Data;

public interface ICustomerRepository
{
    /// <summary>
    /// Looks up a customer by id. Returns <c>null</c> when no matching
    /// customer exists -- for example, when a subscription row survives
    /// after the customer record it references has been deleted.
    /// </summary>
    Task<Customer?> FindByIdAsync(int customerId, CancellationToken ct = default);

    /// <summary>
    /// Looks up a customer by email address, used by the support lookup
    /// endpoint. Returns <c>null</c> when no match is found.
    /// </summary>
    Task<Customer?> FindByEmailAsync(string email, CancellationToken ct = default);
}
