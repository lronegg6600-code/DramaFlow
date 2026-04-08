export default function SettingsPage() {
  return (
    <section className="panel p-6">
      <h2 className="text-xl font-semibold">设置</h2>
      <p className="mt-2 text-sm text-muted">
        这里用于管理后台功能开关、允许来源以及后续运维保护项。仅开发环境可用的操作，在生产环境中应保持关闭。
      </p>
    </section>
  );
}
