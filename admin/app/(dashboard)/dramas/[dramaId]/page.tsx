import { DramaDetail } from "@/features/dramas/drama-detail";

export default async function DramaDetailPage({ params }: { params: Promise<{ dramaId: string }> }) {
  const { dramaId } = await params;
  return <DramaDetail dramaId={dramaId} />;
}
