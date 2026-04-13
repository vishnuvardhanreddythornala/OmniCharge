/**
 * Shared API Models — Single source of truth for common API interfaces.
 *
 * All services should import these from here instead of defining their own.
 */

/** Standard API response wrapper from all backend endpoints */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/** Paginated response from Spring Data Page endpoints */
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
