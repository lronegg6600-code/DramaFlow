"use client";

import React from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DataTable } from "@/components/tables/data-table";

type DramaListPayload = {
  items: Array<{
    id: string;
    title: string;
    region: string;
    language: string;
    publishStatus: string;
    isFeatured: boolean;
  }>;
};

export function DramaList() {
  const { data, isLoading } = useQuery({
    queryKey: ["dramas"],
    queryFn: () => adminFetch<DramaListPayload>("/dramas")
  });

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">剧目库</h2>
          <p className="text-sm text-muted">
            搜索、筛选并发布供 content-service 和 feed-service 使用的剧目信息。
          </p>
        </div>
        <Link className="button-primary" href="/dramas/new">
          新建剧目
        </Link>
      </div>
      <DataTable
        emptyLabel={isLoading ? "正在加载剧目..." : "暂无剧目。"}
        rows={data?.items ?? []}
        getRowKey={(row) => row.id}
        columns={[
          {
            key: "title",
            header: "标题",
            render: (row) => (
              <Link className="font-medium text-accent" href={`/dramas/${row.id}`}>
                {row.title}
              </Link>
            )
          },
          { key: "region", header: "地区" },
          { key: "language", header: "语言" },
          {
            key: "publishStatus",
            header: "状态",
            render: (row) => <span className="badge">{row.publishStatus}</span>
          },
          {
            key: "isFeatured",
            header: "推荐",
            render: (row) => (row.isFeatured ? "是" : "否")
          }
        ]}
      />
    </section>
  );
}
