export function MiniBar({ values }: { values: number[] }) {
  return (
    <div className="flex h-16 items-end gap-2">
      {values.map((value, index) => (
        <div
          key={index}
          className="flex-1 rounded-t-lg bg-accentSoft"
          // 2026-04-08:
          // 这里给柱子加一个最小高度，不然低值会直接贴平，看起来像“数据没渲染出来”。
          // 对后台概览这种弱可视化组件来说，先保证可读，再谈绝对精确。
          style={{ height: `${Math.max(12, value)}%` }}
        />
      ))}
    </div>
  );
}
