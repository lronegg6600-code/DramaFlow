"use client";

import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { StatCard } from "@/components/cards/stat-card";
import { MiniBar } from "@/components/charts/mini-bar";

export function DashboardOverview() {
  const query = useQuery({
    queryKey: ["dashboard"],
    queryFn: () => adminFetch<any>("/dashboard")
  });

  const stats = query.data ?? {
    dramaTotal: 0,
    publishedDramaTotal: 0,
    todayPurchaseSyncTotal: 0,
    todayRtdnEventTotal: 0,
    activeEntitlementTotal: 0,
    playback24hTotal: 0
  };

  // 2026-04-08:
  // 这里先保留成轻量总览区，不急着把所有监控指标一股脑塞进来。
  // 后台首页最重要的是让运营和排障同学一进来就能知道“系统是不是大体正常”，
  // 不是把 Prometheus 面板原封不动搬过来。

  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <StatCard title="剧目总数" value={stats.dramaTotal} hint="内容库中的全部剧目记录。" />
        <StatCard title="已发布剧目" value={stats.publishedDramaTotal} hint="当前对客户端可见的剧目数量。" />
        <StatCard title="今日购买同步" value={stats.todayPurchaseSyncTotal} hint="今天写入的计费同步事件数。" />
        <StatCard title="今日 RTDN 事件" value={stats.todayRtdnEventTotal} hint="今天接收的 RTDN 通知数。" />
        <StatCard title="生效中权益" value={stats.activeEntitlementTotal} hint="当前有效或处于宽限期的权益数量。" />
        <StatCard title="24 小时播放量" value={stats.playback24hTotal} hint="最近 24 小时各模式下的播放会话数。" />
      </div>
      <div className="panel grid gap-4 p-6 lg:grid-cols-[1fr,320px]">
        <div>
          <h3 className="text-lg font-semibold">近期运行概览</h3>
          <p className="mt-2 text-sm text-muted">这个面板预留给近期后台错误汇总、修复积压和首页推荐发布历史。</p>
        </div>
        <MiniBar values={[24, 62, 51, 76, 33, 58, 81]} />
      </div>
    </div>
  );
}
