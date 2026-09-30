export class CurrencyConverter {
  constructor(private readonly rates: Record<string, number>) {}

  /**
   * Converts an amount into USD.
   * Throws a RangeError when the currency has no known rate, it never
   * falls back to a default rate.
   */
  toUsd(amount: number, currency: string): number {
    const rate = this.rates[currency];
    if (rate === undefined) {
      throw new RangeError(`unsupported currency: ${currency}`);
    }
    return Math.round(amount * rate * 100) / 100;
  }
}
