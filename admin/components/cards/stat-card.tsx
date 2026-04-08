export function StatCard({ title, value, hint }: { title: string; value: string | number; hint: string }) {
  return (
    <div className="panel p-5">
      <p className="text-sm text-muted">{title}</p>
      <p className="mt-3 text-3xl font-semibold">{value}</p>
      <p className="mt-2 text-xs text-muted">{hint}</p>
    </div>
  );
}
