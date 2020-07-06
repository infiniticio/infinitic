function truthy(value: string): boolean {
  return value === "1" || value === "true";
}

export const useApiMocks = truthy(process.env.VUE_APP_USE_API_MOCKS);
