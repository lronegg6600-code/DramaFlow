import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8)
});

export const dramaSchema = z.object({
  title: z.string().min(1),
  shortDescription: z.string().min(1),
  longDescription: z.string().min(1),
  posterUrl: z.string().url(),
  coverUrl: z.string().url(),
  tags: z.string().default(""),
  region: z.string().min(2),
  language: z.string().min(2),
  publishStatus: z.enum(["draft", "published", "archived"]),
  isFeatured: z.boolean().default(false)
});

export const feedConfigSchema = z.object({
  region: z.string().min(2),
  language: z.string().min(2),
  draftPayload: z.string().min(2)
});
