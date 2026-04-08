"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DramaForm } from "./drama-form";
import { EpisodeEditor } from "./episode-editor";
import { DataTable } from "@/components/tables/data-table";

type EpisodeRow = {
  episodeNo: number;
  title: string;
  previewSeconds: number;
  isPremium: boolean;
  publishStatus: string;
};

export function DramaDetail({ dramaId }: { dramaId: string }) {
  const dramaQuery = useQuery({
    queryKey: ["drama", dramaId],
    queryFn: () => adminFetch<any>(`/dramas/${dramaId}`)
  });
  const episodesQuery = useQuery({
    queryKey: ["drama-episodes", dramaId],
    queryFn: () => adminFetch<{ items: EpisodeRow[] }>(`/dramas/${dramaId}/episodes`)
  });

  if (dramaQuery.isLoading) {
    return <div className="panel p-6">正在加载剧目详情...</div>;
  }

  return (
    <div className="space-y-6">
      <DramaForm dramaId={dramaId} initialValues={dramaQuery.data} />
      <section className="space-y-3">
        <h3 className="text-lg font-semibold">剧集</h3>
        <DataTable
          emptyLabel={episodesQuery.isLoading ? "正在加载剧集..." : "该剧目下暂无剧集。"}
          rows={episodesQuery.data?.items ?? []}
          columns={[
            { key: "episodeNo", header: "集数" },
            { key: "title", header: "标题" },
            { key: "previewSeconds", header: "试看秒数" },
            { key: "isPremium", header: "付费", render: (row) => (row.isPremium ? "是" : "否") },
            {
              key: "publishStatus",
              header: "状态",
              render: (row) => <span className="badge">{row.publishStatus}</span>
            }
          ]}
        />
      </section>
      <EpisodeEditor dramaId={dramaId} />
    </div>
  );
}
