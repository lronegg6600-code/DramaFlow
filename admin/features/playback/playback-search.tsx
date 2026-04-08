"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DataTable } from "@/components/tables/data-table";

type PlaybackSessionRow = {
  sessionId: string;
  userId: string;
  episodeId: string;
  playbackMode: string;
  expiresAt: string;
  sessionStatus: string;
};

type PlaybackSessionListPayload = {
  items: PlaybackSessionRow[];
};

export function PlaybackSearch() {
  const query = useQuery({
    queryKey: ["playback-sessions"],
    queryFn: () => adminFetch<PlaybackSessionListPayload>("/playback/sessions")
  });

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">播放诊断</h2>
      <DataTable
        emptyLabel={query.isLoading ? "正在加载播放会话..." : "未找到播放会话。"}
        rows={query.data?.items ?? []}
        columns={[
          { key: "sessionId", header: "会话 ID" },
          { key: "userId", header: "用户 ID" },
          { key: "episodeId", header: "剧集 ID" },
          { key: "playbackMode", header: "模式" },
          { key: "expiresAt", header: "过期时间" },
          {
            key: "sessionStatus",
            header: "状态",
            render: (row) => <span className="badge">{row.sessionStatus}</span>
          }
        ]}
      />
    </div>
  );
}
