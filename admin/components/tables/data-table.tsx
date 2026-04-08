import React from "react";

type Column<T> = {
  key: keyof T | string;
  header: string;
  render?: (row: T) => React.ReactNode;
};

export function DataTable<T extends Record<string, unknown>>({
  columns,
  rows,
  emptyLabel,
  getRowKey
}: {
  columns: Column<T>[];
  rows: T[];
  emptyLabel: string;
  getRowKey?: (row: T, index: number) => React.Key;
}) {
  return (
    <div className="panel overflow-hidden">
      <table className="min-w-full divide-y divide-border text-sm">
        <thead className="bg-slate-50">
          <tr>
            {columns.map((column) => (
              <th key={String(column.key)} className="px-4 py-3 text-left font-medium text-muted">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border bg-white">
          {rows.length === 0 ? (
            <tr>
              <td className="px-4 py-8 text-center text-muted" colSpan={columns.length}>
                {emptyLabel}
              </td>
            </tr>
          ) : (
            rows.map((row, index) => (
              <tr key={getRowKey ? getRowKey(row, index) : index}>
                {columns.map((column) => (
                  <td key={String(column.key)} className="px-4 py-3 align-top">
                    {column.render ? column.render(row) : String(row[column.key as keyof T] ?? "")}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
