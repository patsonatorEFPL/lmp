export interface BlogPost {
  id: string;
  title: string;
  slug: string;
  excerpt: string;
  content: string;
  coverImage?: string;
  metaKeywords?: string;
  authorName?: string;
  published: boolean;
  publishedAt?: string;
  createdAt: string;
  updatedAt?: string;
}
