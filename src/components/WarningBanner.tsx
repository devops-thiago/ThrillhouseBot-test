interface WarningBannerProps {
  count: number;
}

export function WarningBanner({ count }: WarningBannerProps) {
  if (count === 0) return null;
  return (
    <div role="alert" className="warning-banner">
      {count} {count === 1 ? 'entry' : 'entries'} could not be verified and may be incomplete.
    </div>
  );
}
