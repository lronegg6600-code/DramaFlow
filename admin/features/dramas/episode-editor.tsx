"use client";

import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { InlineNotice } from "@/components/feedback/inline-notice";
import { FormSection } from "@/components/forms/section";
import { adminFetch } from "@/lib/api/client";
import { useActionMutation } from "@/lib/query/use-action-mutation";

type EpisodeEditorValues = {
  episodeId: string;
  title: string;
  description: string;
  episodeNo: number;
  durationSeconds: number;
  previewSeconds: number;
  isPremium: boolean;
  publishStatus: string;
  sortOrder: number;
  streamKeyPlaceholder: string;
};

export function EpisodeEditor({ dramaId }: { dramaId: string }) {
  const router = useRouter();
  const form = useForm<EpisodeEditorValues>({
    defaultValues: {
      episodeId: "",
      title: "",
      description: "",
      episodeNo: 1,
      durationSeconds: 120,
      previewSeconds: 15,
      isPremium: false,
      publishStatus: "draft",
      sortOrder: 1,
      streamKeyPlaceholder: "dramaflow/demo.mp4"
    }
  });

  const saveEpisodeAction = useActionMutation({
    action: async (values: EpisodeEditorValues) => {
      // 2026-04-08:
      // 这里统一把 number 输入框的值显式转成数字，再发给后端。
      // 原因是浏览器表单控件哪怕用了 type=number，最终到提交边界时仍然可能混进字符串。
      // 把收口放在这里，可以避免“本地看着能填，接口层偶发类型漂移”的脏问题。
      const payload = {
        title: values.title,
        description: values.description,
        episodeNo: Number(values.episodeNo),
        durationSeconds: Number(values.durationSeconds),
        previewSeconds: Number(values.previewSeconds),
        isPremium: values.isPremium,
        publishStatus: values.publishStatus,
        sortOrder: Number(values.sortOrder),
        streamKeyPlaceholder: values.streamKeyPlaceholder
      };

      if (values.episodeId) {
        return adminFetch(`/episodes/${values.episodeId}`, { method: "PUT", body: JSON.stringify(payload) });
      }

      return adminFetch(`/dramas/${dramaId}/episodes`, { method: "POST", body: JSON.stringify(payload) });
    },
    successMessage: ({ variables }) => (variables.episodeId ? "剧集已更新。" : "剧集已创建。"),
    errorMessage: ({ error }) => (error instanceof Error ? error.message : "保存剧集失败，请稍后重试。"),
    invalidateKeys: [["drama-episodes", dramaId]],
    onSuccess: async (_data, variables) => {
      router.refresh();
      form.reset({ ...variables, episodeId: "" });
    }
  });

  async function onSubmit(values: EpisodeEditorValues) {
    await saveEpisodeAction.executeAction(values);
  }

  return (
    <FormSection title="剧集快速编辑" description="填写 `episodeId` 可更新已有剧集，不填则创建新剧集。">
      <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit(onSubmit)}>
        {saveEpisodeAction.notice ? <div className="md:col-span-2"><InlineNotice tone={saveEpisodeAction.notice.tone} message={saveEpisodeAction.notice.message} /></div> : null}
        <input className="input" placeholder="剧集 ID（更新时可填）" {...form.register("episodeId")} />
        <input className="input" placeholder="标题" {...form.register("title")} />
        <textarea className="input min-h-24 md:col-span-2" placeholder="描述" {...form.register("description")} />
        <input className="input" type="number" placeholder="集数" {...form.register("episodeNo", { valueAsNumber: true })} />
        <input className="input" type="number" placeholder="排序" {...form.register("sortOrder", { valueAsNumber: true })} />
        <input className="input" type="number" placeholder="时长（秒）" {...form.register("durationSeconds", { valueAsNumber: true })} />
        <input className="input" type="number" placeholder="试看时长（秒）" {...form.register("previewSeconds", { valueAsNumber: true })} />
        <input className="input" placeholder="流地址占位符" {...form.register("streamKeyPlaceholder")} />
        <select className="input" {...form.register("publishStatus")}>
          <option value="draft">草稿</option>
          <option value="published">已发布</option>
          <option value="archived">已归档</option>
        </select>
        <label className="flex items-center gap-2 rounded-xl border border-border px-3 py-2 text-sm">
          <input type="checkbox" {...form.register("isPremium")} />
          付费剧集
        </label>
        <div className="md:col-span-2">
          <button className="button-primary" disabled={saveEpisodeAction.isSubmitting} type="submit">
            {saveEpisodeAction.isSubmitting ? "保存中..." : "保存剧集"}
          </button>
        </div>
      </form>
    </FormSection>
  );
}
