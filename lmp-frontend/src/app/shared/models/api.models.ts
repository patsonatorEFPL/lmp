/**
 * Enveloppe de réponse API standard du backend (ApiResponse Java).
 */
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

/**
 * Page Spring Data sérialisée (org.springframework.data.domain.Page).
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
